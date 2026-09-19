package com.redslovesgames.newestocean.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Pure JSON codec for the client-only ocean settings. */
public final class OceanClientConfigCodec {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private OceanClientConfigCodec() {
    }

    public static String encode(OceanClientConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        return GSON.toJson(config.copy().sanitize());
    }

    public static OceanClientConfig decode(String json) {
        if (json == null || json.isBlank()) {
            return OceanClientConfig.defaults();
        }
        try {
            OceanClientConfig decoded = GSON.fromJson(json, OceanClientConfig.class);
            return decoded == null ? OceanClientConfig.defaults() : decoded.sanitize();
        } catch (RuntimeException error) {
            return OceanClientConfig.defaults();
        }
    }
}