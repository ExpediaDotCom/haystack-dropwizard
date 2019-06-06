package com.expedia.haystack.dropwizard.jackson;

import com.expedia.www.haystack.client.idgenerators.IdGenerator;
import com.expedia.www.haystack.client.idgenerators.LongIdGenerator;
import com.expedia.www.haystack.client.idgenerators.RandomUUIDGenerator;
import com.expedia.www.haystack.client.idgenerators.TimeBasedUUIDGenerator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IdGeneratorDeserializerTest {

    private final ObjectMapper objectMapper = Jackson
            .newObjectMapper()
            .registerModule(new SimpleModule().addDeserializer(IdGenerator.class, new IdGeneratorDeserializer()));

    @Test
    public void canDeserializeLongIdGenerator() throws IOException {
        TestConfiguration testConfiguration = objectMapper
                .readValue("{ \"idGenerator\": \"long\" }", TestConfiguration.class);

        assertTrue(testConfiguration.idGenerator instanceof LongIdGenerator);
    }

    @Test
    public void canDeserializeRandomUUIDGenerator() throws IOException {
        TestConfiguration testConfiguration = objectMapper
                .readValue("{ \"idGenerator\": \"randomUUID\" }", TestConfiguration.class);

        assertTrue(testConfiguration.idGenerator instanceof RandomUUIDGenerator);
    }

    @Test
    public void canDeserializeTimeBasedUUIDGenerator() throws IOException {
        TestConfiguration testConfiguration = objectMapper
                .readValue("{ \"idGenerator\": \"timeBasedUUID\" }", TestConfiguration.class);

        assertTrue(testConfiguration.idGenerator instanceof TimeBasedUUIDGenerator);
    }

    @Test(expected = InvalidFormatException.class)
    public void serialiseUnrecognisedIdGenerator() throws IOException {
        objectMapper
                .readValue("{ \"idGenerator\": \"foo\" }", TestConfiguration.class);
    }

    @Test
    public void attemptToDeserializeNullIdGenerator() throws IOException {
        TestConfiguration testConfiguration = objectMapper
                .readValue("{ \"idGenerator\": null }", TestConfiguration.class);

        assertNull(testConfiguration.idGenerator);
    }

    @Test
    public void canRegisterCustomIdGenerator() throws IOException {
        ObjectMapper objectMapper = Jackson
                .newObjectMapper()
                .registerModule(
                        new SimpleModule()
                                .addDeserializer(
                                        IdGenerator.class,
                                        new IdGeneratorDeserializer()
                                                .register("incrementing", new IncrementingIdGenerator())));

        TestConfiguration testConfiguration = objectMapper
                .readValue("{ \"idGenerator\": \"incrementing\" }", TestConfiguration.class);

        assertTrue(testConfiguration.idGenerator instanceof IncrementingIdGenerator);
    }

    public static class IncrementingIdGenerator implements IdGenerator {

        private AtomicLong value = new AtomicLong(1);

        @Override
        public Object generate() {
            return value.getAndIncrement();
        }
    }

    public static class TestConfiguration {

        private final IdGenerator idGenerator;

        public TestConfiguration(@JsonProperty("idGenerator") IdGenerator idGenerator) {
            this.idGenerator = idGenerator;
        }
    }
}
