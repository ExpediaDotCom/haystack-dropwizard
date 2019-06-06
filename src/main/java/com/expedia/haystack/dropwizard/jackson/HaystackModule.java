package com.expedia.haystack.dropwizard.jackson;

import com.expedia.www.haystack.client.idgenerators.IdGenerator;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class HaystackModule extends SimpleModule {

    public HaystackModule() {
        this(new IdGeneratorDeserializer());
    }

    public HaystackModule(IdGeneratorDeserializer idGeneratorDeserializer) {
        addDeserializer(IdGenerator.class, idGeneratorDeserializer);
    }
}
