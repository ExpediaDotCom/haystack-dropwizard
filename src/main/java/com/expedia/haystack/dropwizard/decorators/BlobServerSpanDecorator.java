/*
 * Copyright 2020 Expedia, Inc.
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

package com.expedia.haystack.dropwizard.decorators;

import com.expedia.blobs.core.BlobContext;
import com.expedia.blobs.core.BlobType;
import com.expedia.blobs.core.BlobWriter;
import com.expedia.blobs.core.BlobsFactory;
import com.expedia.haystack.blobs.SpanBlobContext;
import com.expedia.haystack.dropwizard.bundle.Blobable;
import com.expedia.haystack.dropwizard.configuration.BlobContent;
import com.expedia.haystack.dropwizard.configuration.BlobFactory;
import io.opentracing.Span;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import static com.expedia.haystack.dropwizard.decorators.BlobHelper.writeBlob;

public class BlobServerSpanDecorator implements ServerSpanDecorator {
    private final static Logger LOGGER = LoggerFactory.getLogger(BlobServerSpanDecorator.class);
    private final boolean isEnabled;
    private final BlobsFactory<BlobContext> factory;
    private final Blobable blobable;

    public BlobServerSpanDecorator(final BlobFactory blobs) {
        Validate.notNull(blobs);

        this.isEnabled = blobs.isEnabled();
        this.blobable = blobs.getBlobable();
        this.factory = blobs.factory();
    }

    @Override
    public void decorateRequest(final ContainerRequestContext requestContext,
                                final Span span) {
        if (!isEnabled || !blobable.isServerRequestValidForBlob(requestContext)) return;

        final SpanBlobContext blobContext = new SpanBlobContext((com.expedia.www.haystack.client.Span) span);
        final BlobWriter writer = factory.create(blobContext);
        try {
            final BlobContent blob = blobable.extractBlobFromServerRequest(requestContext);
            writeBlob(writer, blob, BlobType.REQUEST);
        } catch (Exception e) {
            LOGGER.error("Fail to read server request for writing as blob in span", e);
        }
    }

    @Override
    public void decorateResponse(final ContainerResponseContext responseContext,
                                 final Span span) {
        if (!isEnabled || !blobable.isServerResponseValidForBlob(responseContext)) return;

        final SpanBlobContext blobContext = new SpanBlobContext((com.expedia.www.haystack.client.Span) span);
        final BlobWriter writer = factory.create(blobContext);

        try {
            final BlobContent blob = blobable.extractBlobFromServerResponse(responseContext);
            writeBlob(writer, blob, BlobType.RESPONSE);
        } catch (Exception e) {
            LOGGER.error("Fail to read server response for writing as blob in span", e);
        }
    }
}