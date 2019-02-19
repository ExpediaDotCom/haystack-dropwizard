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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collections;

import javax.validation.Validator;

import com.codahale.metrics.MetricRegistry;
import com.expedia.haystack.dropwizard.configuration.DropwizardMetricsFactory;
import com.expedia.haystack.dropwizard.configuration.MetricsFactory;
import com.expedia.haystack.dropwizard.configuration.TracerFactory;
import com.expedia.www.haystack.client.Tracer;
import com.expedia.www.haystack.client.metrics.Metrics;
import com.expedia.www.haystack.client.metrics.MetricsRegistry;
import com.expedia.www.haystack.client.metrics.dropwizard.DropwizardMetricsRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

import io.dropwizard.Configuration;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Environment;
import static org.mockito.Mockito.*;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

public class HaystackTracerBundleTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Environment environment;

    @Mock
    JerseyEnvironment jerseyEnvironment;

    @Mock
    Tracer tracer;

    @Mock
    TracerFactory tracerFactory;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateClientTracingFeature() throws Exception {
        final HaystackTracerBundle<Traceable> haystackTracerBundle = new HaystackTracerBundle<>();

        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(jerseyEnvironment.getProperty(Tracer.class.getName())).thenReturn(tracer);

        final ClientTracingFeature clientTracingFeature = haystackTracerBundle.clientTracingFeature(environment);
        assertThat(clientTracingFeature.getClass().getName().equals(ClientTracingFeature.class.getName()));
    }

    @Test
    public void testRun() throws Exception {
        final HaystackTracerBundle<Traceable> haystackTracerBundle = new HaystackTracerBundle<>();
        Traceable traceable = mock(Traceable.class, RETURNS_DEEP_STUBS);

        when(traceable.getTracerFactory().build(environment)).thenReturn(tracer);

        haystackTracerBundle.run(traceable, environment);
        verify(environment.servlets(), atLeastOnce()).addFilter(eq("SpanFinishingFilter"), isA(SpanFinishingFilter.class));
        verify(environment.jersey(), atLeastOnce()).property(io.opentracing.Tracer.class.getName(), tracer);
    }
}
