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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class BlobHelper {
    private final static Logger LOGGER = LoggerFactory.getLogger(BlobHelper.class);
    private final static ObjectMapper mapper = new ObjectMapper();

    public static byte[] objectToByteArray(Object obj) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        return bos.toByteArray();
    }

    public static byte[] toJson(Object obj) throws IOException {
        return mapper.writeValueAsBytes(obj);
    }

    public static void writeBlob(final BlobWriter blobWriter,
                                 final BlobContent blob,
                                 final BlobType blobType) {
        blobWriter.write(
                blobType,
                blob.getContentType(),
                (outputStream) -> {
                    try {
                        outputStream.write(blob.getData());
                    } catch (IOException e) {
                        LOGGER.error("Exception occurred while writing data to stream for preparing blob", e);
                    }
                },
                metadata -> {
                }
        );
    }

    public static BlobContent extract(final Object entity, Class<?> entityClazz, String contenType) throws Exception {
        if (entityClazz.getCanonicalName().equalsIgnoreCase("java.lang.string")) {
            return new BlobContent(entity.toString().getBytes("utf-8"), ContentType.from(contenType));
        } else if(entityClazz.getCanonicalName().equalsIgnoreCase("byte[]")) {
            return new BlobContent((byte[])entity, ContentType.from(contenType));
        } else if (contenType.equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
            return new BlobContent(BlobHelper.toJson(entity), ContentType.JSON);
        } else {
            return new BlobContent(objectToByteArray(entity), ContentType.from(contenType));
        }
    }
}