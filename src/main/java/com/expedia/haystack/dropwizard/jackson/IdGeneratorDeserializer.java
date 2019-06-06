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
package com.expedia.haystack.dropwizard.jackson;

import com.expedia.www.haystack.client.idgenerators.IdGenerator;
import com.expedia.www.haystack.client.idgenerators.LongIdGenerator;
import com.expedia.www.haystack.client.idgenerators.RandomUUIDGenerator;
import com.expedia.www.haystack.client.idgenerators.TimeBasedUUIDGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Basic deserializer for {@link IdGenerator} and its subtypes.
 *
 * The deserializer is initially constructed with the 3 currently supported IdGenerators:
 * <ul>
 *     <li>long - ${@link LongIdGenerator}</li>
 *     <li>randomUUID - ${@link RandomUUIDGenerator}</li>
 *     <li>timeBasedUUID - ${@link TimeBasedUUIDGenerator}</li>
 * </ul>
 *
 * @author amcghie
 * @since 0.2.4
 */
public class IdGeneratorDeserializer extends JsonDeserializer<IdGenerator> {

    private Map<String, IdGenerator> registry;

    public IdGeneratorDeserializer() {
        this(ImmutableMap.of(
                "long", new LongIdGenerator(),
                "randomUUID", new RandomUUIDGenerator(),
                "timeBasedUUID", new TimeBasedUUIDGenerator()
        ));
    }

    public IdGeneratorDeserializer(Map<String, IdGenerator> registry) {
        this.registry = new HashMap<>(registry);
    }

    /**
     * Register a custom {@link IdGenerator}
     * @param name
     * @param idGenerator
     * @return
     */
    public IdGeneratorDeserializer register(String name, IdGenerator idGenerator) {
        registry.put(name, idGenerator);
        return this;
    }

    @Override
    public IdGenerator deserialize(JsonParser jp,
                                   DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonToken currentToken = jp.getCurrentToken();

        if (currentToken.equals(JsonToken.VALUE_STRING)) {
            String text = jp.getText().trim();

            return Optional
                    .ofNullable(registry.get(text))
                    .orElseThrow(unknownIdGeneratorSpecified(ctxt, text));

        } else if (currentToken.equals(JsonToken.VALUE_NULL)) {
            return getNullValue(ctxt);
        }

        return (IdGenerator) ctxt.handleUnexpectedToken(IdGenerator.class, jp);
    }

    private Supplier<JsonMappingException> unknownIdGeneratorSpecified(DeserializationContext ctxt, String text) {
        return () -> ctxt.weirdStringException(
                text,
                IdGenerator.class,
                String.format(
                        "Only %s values supported",
                        registry
                                .keySet()
                                .stream()
                                .collect(Collectors.joining(", ", "\"", "\""))
                )
        );
    }
}
