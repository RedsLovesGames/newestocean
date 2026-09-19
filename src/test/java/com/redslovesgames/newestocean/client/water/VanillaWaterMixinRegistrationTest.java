package com.redslovesgames.newestocean.client.water;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaWaterMixinRegistrationTest {
    @Test
    void vanillaShaderProgramMixinIsRegisteredAsClientOnlyMixin() throws IOException {
        String json = resourceText("/newestocean.mixins.json");

        assertTrue(json.contains("\"client\""));
        assertTrue(json.contains("\"client.ShaderProgramMixin\""));
    }

    private static String resourceText(String path) throws IOException {
        try (InputStream stream = VanillaWaterMixinRegistrationTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("missing test resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
