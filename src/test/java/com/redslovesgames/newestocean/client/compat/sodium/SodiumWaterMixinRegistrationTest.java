package com.redslovesgames.newestocean.client.compat.sodium;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SodiumWaterMixinRegistrationTest {
    @Test
    void sodiumShaderMixinsAreRegisteredAsClientMixins() throws IOException {
        String json = resourceText("/newestocean.mixins.json");

        assertTrue(json.contains("\"compat.sodium.SodiumShaderLoaderMixin\""));
        assertTrue(json.contains("\"compat.sodium.SodiumShaderChunkRendererMixin\""));
    }

    private static String resourceText(String path) throws IOException {
        try (InputStream stream = SodiumWaterMixinRegistrationTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("missing test resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
