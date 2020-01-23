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

import com.expedia.blobs.core.BlobType;
import com.expedia.blobs.core.BlobWriter;
import com.expedia.blobs.core.ContentType;
import com.expedia.haystack.dropwizard.configuration.BlobContent;
import com.expedia.haystack.dropwizard.configuration.BlobFactory;
import com.expedia.www.haystack.client.Span;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class BlobClientSpanDecoratorTest {
    private final static String data = "{\"name\": \"Alice\"}";

    @Mock
    Span span;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    BlobFactory blobs;

    @Mock
    ClientRequestContext requestCtx;

    @Mock
    ClientResponseContext responseCtx;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void decorateClientRequest() throws Exception {
        final boolean writerInvoked[] = new boolean[1];
        when(blobs.isEnabled()).thenReturn(true);
        when(blobs.factory().create(Matchers.any())).thenReturn(newBlobWriter(writerInvoked, BlobType.REQUEST));
        when(blobs.getBlobable().isClientRequestValidForBlob(requestCtx)).thenReturn(true);
        when(blobs.getBlobable().extractBlobFromClientRequest(requestCtx)).thenReturn(
                new BlobContent(data.getBytes("utf-8"), ContentType.JSON));
        final BlobClientSpanDecorator decorator = new BlobClientSpanDecorator(blobs);
        decorator.decorateRequest(requestCtx, span);
        assertThat(writerInvoked[0]).isTrue();
    }

    @Test
    public void decorateClientResponse() throws Exception {
        final boolean writerInvoked[] = new boolean[1];
        when(blobs.isEnabled()).thenReturn(true);
        when(blobs.factory().create(Matchers.any())).thenReturn(newBlobWriter(writerInvoked, BlobType.RESPONSE));
        when(blobs.getBlobable().isClientResponseValidForBlob(responseCtx)).thenReturn(true);
        when(blobs.getBlobable().extractBlobFromClientResponse(responseCtx)).thenReturn(
                new BlobContent(data.getBytes("utf-8"), ContentType.JSON));

        final BlobClientSpanDecorator decorator = new BlobClientSpanDecorator(blobs);

        decorator.decorateResponse(responseCtx, span);
        assertThat(writerInvoked[0]).isTrue();
    }

    @Test
    public void skipDecorateClientResponse() throws Exception {
        final boolean writerInvoked[] = new boolean[1];
        when(blobs.isEnabled()).thenReturn(true);
        when(blobs.factory().create(Matchers.any())).thenReturn(newBlobWriter(writerInvoked, BlobType.RESPONSE));
        when(blobs.getBlobable().isClientResponseValidForBlob(responseCtx)).thenReturn(false);
        final BlobClientSpanDecorator decorator = new BlobClientSpanDecorator(blobs);

        decorator.decorateResponse(responseCtx, span);
        assertThat(writerInvoked[0]).isFalse();
    }

    @Test
    public void skipDecorateClientRequest() throws Exception {
        final boolean writerInvoked[] = new boolean[1];
        when(blobs.isEnabled()).thenReturn(true);
        when(blobs.factory().create(Matchers.any())).thenReturn(newBlobWriter(writerInvoked, BlobType.RESPONSE));
        when(blobs.getBlobable().isClientRequestValidForBlob(requestCtx)).thenReturn(false);
        final BlobClientSpanDecorator decorator = new BlobClientSpanDecorator(blobs);
        decorator.decorateRequest(requestCtx, span);
        assertThat(writerInvoked[0]).isFalse();
    }

    private static BlobWriter newBlobWriter(final boolean writerInvoked[], final BlobType type) {
        return (blobType, contentType, dataStream, metadata) -> {
            writerInvoked[0] = true;
            assertThat(blobType).isEqualTo(type);
            assertThat(contentType).isEqualTo(ContentType.JSON);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            dataStream.accept(outStream);
            assertThat(new String(outStream.toByteArray())).isEqualTo(data);
        };
    }
}
