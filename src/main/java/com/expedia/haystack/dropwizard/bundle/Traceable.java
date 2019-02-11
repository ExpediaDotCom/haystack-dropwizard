package com.expedia.haystack.dropwizard.bundle;

import com.expedia.haystack.dropwizard.configuration.TracerFactory;

public interface Traceable {
    TracerFactory getTracerFactory();
}