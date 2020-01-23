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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


public class BlobServerSpanDecoratorTest {

    private final static String request_data = "{\"name\": \"Alice\"}";
    private final static String response_data = "{\"name\": \"Blob\"}";

    @Mock
    Span span;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    BlobFactory blobs;

    @Mock
    ContainerRequestContext requestCtx;

    @Mock
    ContainerResponseContext responseCtx;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void decorateServerRequest() throws Exception {
        final int[] writerInvoked = new int[] { 0 };
        final BlobWriter blobWriter = newBlobWriter(writerInvoked);

        when(blobs.isEnabled()).thenReturn(true);
        when(blobs.factory().create(Matchers.any())).thenReturn(blobWriter);

        when(blobs.getBlobable().isServerRequestValidForBlob(requestCtx)).thenReturn(true);
        when(blobs.getBlobable().extractBlobFromServerRequest(requestCtx)).thenReturn(
                new BlobContent(request_data.getBytes("utf-8"), ContentType.JSON));

        when(blobs.getBlobable().isServerResponseValidForBlob(responseCtx)).thenReturn(true);
        when(blobs.getBlobable().extractBlobFromServerResponse(responseCtx)).thenReturn(
                new BlobContent(response_data.getBytes("utf-8"), ContentType.JSON));

        final BlobServerSpanDecorator decorator = new BlobServerSpanDecorator(blobs);
        decorator.decorateRequest(requestCtx, span);
        decorator.decorateResponse(responseCtx, span);
        assertThat(writerInvoked[0]).isEqualTo(2);
    }


    @Test
    public void skipDecorateServerRequest() throws Exception {
        final int[] writerInvoked = new int[] { 0 };
        final BlobWriter blobWriter = newBlobWriter(writerInvoked);

        when(blobs.isEnabled()).thenReturn(true);
        when(blobs.factory().create(Matchers.any())).thenReturn(blobWriter);
        when(blobs.getBlobable().isServerRequestValidForBlob(requestCtx)).thenReturn(false);
        when(blobs.getBlobable().isServerResponseValidForBlob(responseCtx)).thenReturn(false);

        final BlobServerSpanDecorator decorator = new BlobServerSpanDecorator(blobs);
        decorator.decorateRequest(requestCtx, span);
        decorator.decorateResponse(responseCtx, span);
        assertThat(writerInvoked[0]).isEqualTo(0);
    }

    private static BlobWriter newBlobWriter(final int[] writerInvoked) {
        return (blobType, contentType, dataStream, metadata) -> {
            assertThat(contentType).isEqualTo(ContentType.JSON);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            dataStream.accept(outStream);

            if(blobType.equals(BlobType.REQUEST)) {
                writerInvoked[0] = writerInvoked[0] + 1;
                assertThat(new String(outStream.toByteArray())).isEqualTo(request_data);
            } else if (blobType.equals(BlobType.RESPONSE)) {
                writerInvoked[0] = writerInvoked[0] + 1;
                assertThat(new String(outStream.toByteArray())).isEqualTo(response_data);
            }
        };
    }
}
