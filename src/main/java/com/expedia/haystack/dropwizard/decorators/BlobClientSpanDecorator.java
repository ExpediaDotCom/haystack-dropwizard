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
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;

import static com.expedia.haystack.dropwizard.decorators.BlobHelper.writeBlob;

public class BlobClientSpanDecorator implements ClientSpanDecorator {
    private final Logger LOGGER = LoggerFactory.getLogger(BlobClientSpanDecorator.class);

    private final BlobsFactory<BlobContext> factory;
    private final boolean isEnabled;
    private final Blobable blobable;

    public BlobClientSpanDecorator(final BlobFactory blobs) {
        this.blobable = blobs.getBlobable();
        this.isEnabled = blobs.isEnabled();
        this.factory = blobs.factory();
    }

    @Override
    public void decorateRequest(final ClientRequestContext requestContext,
                                final Span span) {
        if (!isEnabled || !blobable.isClientRequestValidForBlob(requestContext)) return;

        final SpanBlobContext blobContext = new SpanBlobContext((com.expedia.www.haystack.client.Span) span);
        final BlobWriter writer = factory.create(blobContext);
        try {
            final BlobContent blob = blobable.extractBlobFromClientRequest(requestContext);
            writeBlob(writer, blob, BlobType.REQUEST);
        } catch (Exception e) {
            LOGGER.error("Fail to read client request for writing as blob in span", e);
        }
    }

    @Override
    public void decorateResponse(final ClientResponseContext responseContext,
                                 final Span span) {
        if (!isEnabled || !blobable.isClientResponseValidForBlob(responseContext)) return;

        final SpanBlobContext blobContext = new SpanBlobContext((com.expedia.www.haystack.client.Span) span);
        final BlobWriter writer = factory.create(blobContext);
        try {
            final BlobContent blob = blobable.extractBlobFromClientResponse(responseContext);
            writeBlob(writer, blob, BlobType.RESPONSE);
        } catch (Exception e) {
            LOGGER.error("Fail to read client response for writing as blob in span", e);
        }
    }
}