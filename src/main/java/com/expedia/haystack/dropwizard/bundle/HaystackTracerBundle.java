/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.haystack.dropwizard.bundle;

import com.expedia.haystack.dropwizard.configuration.BlobFactory;
import com.expedia.haystack.dropwizard.decorators.BlobClientSpanDecorator;
import com.expedia.haystack.dropwizard.decorators.BlobServerSpanDecorator;
import com.expedia.haystack.dropwizard.jackson.HaystackModule;
import com.expedia.haystack.dropwizard.jackson.IdGeneratorDeserializer;
import com.expedia.www.haystack.client.idgenerators.IdGenerator;
import com.fasterxml.jackson.databind.Module;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import io.opentracing.noop.NoopTracerFactory;
import org.apache.commons.lang3.Validate;

import javax.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

public class HaystackTracerBundle<T extends Traceable> implements ConfiguredBundle<T> {

    private final Module haystackModule;
    private final Function<Tracer, ServerTracingDynamicFeature.Builder> serverTracingBuilder;

    public HaystackTracerBundle() {
        this(new HaystackModule(), ServerTracingDynamicFeature.Builder::new);
    }

    /**
     * Allow for registration of a customized {@link IdGeneratorDeserializer} and/or
     * {@link ServerTracingDynamicFeature.Builder}.
     * <p>
     * <p>Customizing the {@link IdGeneratorDeserializer} is necessary if you've written your own {@link IdGenerator}.</p>
     * <p>Customizing the {@link ServerTracingDynamicFeature.Builder} is necessary if you want to use anything other than
     * the standard spanDecorators, serializationSpanDecorators, skipPattern etc</p>
     *
     * @param haystackModule       a customized {@link HaystackModule}
     * @param serverTracingBuilder a customized {@link ServerTracingDynamicFeature.Builder}
     */
    public HaystackTracerBundle(Module haystackModule,
                                Function<Tracer, ServerTracingDynamicFeature.Builder> serverTracingBuilder) {
        this.haystackModule = haystackModule;
        this.serverTracingBuilder = serverTracingBuilder;
    }

    @Override
    public void run(T traceable, Environment environment) {
        Validate.notNull(traceable);
        Validate.notNull(environment);

        final Tracer tracer = traceable.getTracerFactory().build(environment);

        final ServerTracingDynamicFeature tracingDynamicFeature = serverTracingBuilder
                .apply(tracer)
                .withDecorators(
                        Arrays.asList(
                                ServerSpanDecorator.STANDARD_TAGS,
                                new BlobServerSpanDecorator(traceable.getBlobFactory())))
                .withTraceSerialization(false).build();
        environment.jersey().register(tracingDynamicFeature);

        environment.servlets()
                .addFilter("SpanFinishingFilter", new SpanFinishingFilter(tracer))
                .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        environment.jersey().property(Tracer.class.getName() + ".blobs", traceable.getBlobFactory());
        environment.jersey().property(Tracer.class.getName(), tracer);
    }

    @Override
    public void initialize(Bootstrap bootstrap) {
        bootstrap
                .getObjectMapper()
                .registerModule(haystackModule);
    }

    public ClientTracingFeature clientTracingFeature(final Environment environment) {
        Tracer tracer = environment.jersey().getProperty(Tracer.class.getName());
        if (tracer == null) {
            tracer = NoopTracerFactory.create();
        }

        final BlobFactory blobs = environment.jersey().getProperty(Tracer.class.getName() + ".blobs");
        final List<ClientSpanDecorator> decorators = new ArrayList<>(2);
        decorators.add(ClientSpanDecorator.STANDARD_TAGS);
        if (blobs.isEnabled()) {
            decorators.add(new BlobClientSpanDecorator(blobs));
        }

        return new ClientTracingFeature.Builder(tracer)
                .withDecorators(decorators)
                .withTraceSerialization(false)
                .build();
    }
}