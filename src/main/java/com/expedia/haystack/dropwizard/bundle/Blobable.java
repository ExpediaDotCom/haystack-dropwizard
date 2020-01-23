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

package com.expedia.haystack.dropwizard.bundle;

import com.expedia.blobs.core.ContentType;
import com.expedia.haystack.dropwizard.configuration.BlobContent;
import com.expedia.haystack.dropwizard.decorators.BlobHelper;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public interface Blobable {
    /**
     * should request received on the server should be logged as a blob
     * @param req ContainerRequestContext
     * @return boolean
     */
    default boolean isServerRequestValidForBlob(ContainerRequestContext req) {
        return true;
    }

    /**
     * should response sent by the server should be logged as a blob
     * @param resp ContainerResponseContext
     * @return boolean
     */
    default boolean isServerResponseValidForBlob(ContainerResponseContext resp) {
        return true;
    }

    /**
     * should the request sent by client be logged as a blob
     * @param req ClientRequestContext
     * @return boolean
     */
    default boolean isClientRequestValidForBlob(ClientRequestContext req) {
        return true;
    }

    /**
     * should the request received by client be logged as a blob
     * @param resp ClientResponseContext
     * @return boolean
     */
    default boolean isClientResponseValidForBlob(ClientResponseContext resp) {
        return true;
    }

    /**
     * in case you want to override the behavior of extracting the blob from server request, you can override this func
     * @param req ContainerRequestContext
     * @return BlobContent
     * @throws Exception
     */
    default BlobContent extractBlobFromServerRequest(ContainerRequestContext req) throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final String requestContentType = req.getMediaType() == null ? MediaType.TEXT_PLAIN : req.getMediaType().toString();
        IOUtils.copy(req.getEntityStream(), os);
        final byte[] requestBytes = os.toByteArray();

        // reset the entity stream
        req.setEntityStream(new ByteArrayInputStream(requestBytes));
        return new BlobContent(requestBytes, requestContentType.equals(MediaType.APPLICATION_JSON) ?
                ContentType.JSON : ContentType.from(requestContentType));
    }

    /**
     * in case you want to override the behavior of extracting the blob from server response, you can override this func
     * @param resp ContainerResponseContext
     * @return BlobContent
     * @throws Exception
     */
    default BlobContent extractBlobFromServerResponse(ContainerResponseContext resp) throws Exception {
        final String contentType = resp.getMediaType() == null ? MediaType.TEXT_PLAIN : resp.getMediaType().toString();
        return BlobHelper.extract(resp.getEntity(), resp.getEntityClass(), contentType);
    }

    /**
     * in case you want to override the behavior of extracting the blob from client request, you can override this func
     * @param req ClientRequestContext
     * @return BlobContent
     * @throws Exception
     */
    default BlobContent extractBlobFromClientRequest(ClientRequestContext req) throws Exception {
        final String contentType = req.getMediaType() == null ? MediaType.TEXT_PLAIN : req.getMediaType().toString();
        return BlobHelper.extract(req.getEntity(), req.getEntityClass(), contentType);
    }

    /**
     * in case you want to override the behavior of extracting the blob from client response, you can override this func
     * @param resp ClientResponseContext
     * @return BlobContent
     * @throws Exception
     */
    default BlobContent extractBlobFromClientResponse(ClientResponseContext resp) throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final ContentType contentType = resp.getMediaType() == null ? ContentType.from(MediaType.TEXT_PLAIN) :
                ContentType.from(resp.getMediaType().toString());

        IOUtils.copy(resp.getEntityStream(), os);
        byte[] responseBytes = os.toByteArray();
        // reset the entity stream
        resp.setEntityStream(new ByteArrayInputStream(responseBytes));
        return new BlobContent(responseBytes, contentType);
    }
}