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

package com.expedia.haystack.dropwizard.configuration;

import com.expedia.blobs.core.BlobContext;
import com.expedia.blobs.core.BlobStore;
import com.expedia.blobs.core.BlobsFactory;
import com.expedia.blobs.core.predicates.BlobsRateLimiter;
import com.expedia.blobs.stores.io.FileStore;
import com.expedia.haystack.agent.blobs.client.AgentClient;
import com.expedia.haystack.dropwizard.bundle.Blobable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.util.function.Predicate;

public class BlobFactory {
    private final static Blobable DEFAULT_BLOBABLE = new Blobable() {};

    private Store store;

    private boolean enabled;

    private double ratePerSec = -1;

    @JsonIgnore
    private Blobable blobable = DEFAULT_BLOBABLE;

    @JsonProperty
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JsonProperty
    public void setStore(Store store) {
        this.store = store;
    }

    @JsonProperty
    public void setRatePerSec(double ratePerSec) {
        this.ratePerSec = ratePerSec;
    }

    private BlobsFactory<BlobContext> blobFactory;

    public Blobable getBlobable() {
        return this.blobable;
    }

    @JsonIgnore
    public void setBlobable(Blobable blobable) {
        this.blobable = blobable;
    }

    public BlobsFactory<BlobContext> factory() {
        if (!enabled) return null;

        if (blobFactory != null) return blobFactory;

        synchronized (this) {
            if (blobFactory != null) return blobFactory;
            final Predicate<BlobContext> predicate = ratePerSec >= 0 ? new BlobsRateLimiter<>(ratePerSec) : t -> true;
            blobFactory = new BlobsFactory<>(blobStore(), predicate);
        }
        return blobFactory;
    }

    private BlobStore blobStore() {
        switch (store.name.toLowerCase()) {
            case "file": {
                String userDirectory = System.getProperty("user.dir");
                String directoryPath = userDirectory.concat("/blobs");
                File directory = new File(directoryPath);
                if (!directory.exists()) {
                    directory.mkdir();
                }
                return new FileStore.Builder(directory).build();
            }
            case "agent": {
                return new AgentClient.Builder(store.host, store.port).build();
            }
            default:
                throw new UnsupportedOperationException("blob store type " + store.name + " is not supported");
        }
    }

    public class Store {
        private String name;
        private String host;
        private int port;

        @JsonProperty
        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty
        public void setHost(String host) {
            this.host = host;
        }

        @JsonProperty
        public void setPort(int port) {
            this.port = port;
        }
    }
}
