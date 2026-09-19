package com.redslovesgames.newestocean.client.compat.iris;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IrisNoPackFallbackContractTest {
    @Test
    void noPackFallbackRunsOnlyWhenIrisIsPresentWithoutAnActiveShaderpack() {
        assertTrue(IrisWaterRuntimeBridge.shouldUseNoPackFallback(true, false));
        assertFalse(IrisWaterRuntimeBridge.shouldUseNoPackFallback(false, false));
        assertFalse(IrisWaterRuntimeBridge.shouldUseNoPackFallback(true, true));
    }

    @Test
    void irisOwnedNoPackSodiumHooksAreRegistered() throws IOException {
        String json = resourceText("/newestocean.mixins.json");

        assertTrue(json.contains("\"compat.iris.IrisNoPackShaderLoaderMixin\""));
        assertTrue(json.contains("\"compat.iris.IrisNoPackShaderChunkRendererMixin\""));
    }

    private static String resourceText(String path) throws IOException {
        try (InputStream stream = IrisNoPackFallbackContractTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("missing test resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
