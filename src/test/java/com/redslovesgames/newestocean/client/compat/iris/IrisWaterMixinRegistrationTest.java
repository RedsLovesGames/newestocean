package com.redslovesgames.newestocean.client.compat.iris;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IrisWaterMixinRegistrationTest {
    @Test
    void irisMixinsAreRegisteredAsClientCompatibilityMixins() throws IOException {
        String json = resourceText("/newestocean.mixins.json");

        assertTrue(json.contains("\"compat.iris.IrisTransformPatcherMixin\""));
        assertTrue(json.contains("\"compat.iris.IrisSodiumShaderMixin\""));
    }

    @Test
    void runtimeVersionGateAcceptsOnlyPinnedBetaFamily() {
        assertTrue(IrisWaterRuntimeBridge.supportsVersion("1.8.14-beta.1+1.21.1"));
        assertTrue(IrisWaterRuntimeBridge.supportsVersion("1.8.14-beta.1+mc1.21.1"));
        assertFalse(IrisWaterRuntimeBridge.supportsVersion("1.8.14"));
        assertFalse(IrisWaterRuntimeBridge.supportsVersion("1.8.15"));
        assertFalse(IrisWaterRuntimeBridge.supportsVersion("1.9.0"));
    }

    private static String resourceText(String path) throws IOException {
        try (InputStream stream = IrisWaterMixinRegistrationTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("missing test resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
