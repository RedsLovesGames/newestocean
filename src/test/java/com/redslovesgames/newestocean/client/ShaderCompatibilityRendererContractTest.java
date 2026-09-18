package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderCompatibilityRendererContractTest {
    @Test
    void oceanRendererUsesCentralCompatibilitySnapshot() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java"
        ));

        assertTrue(source.contains("ShaderCompatibility.current()"));
        assertTrue(source.contains("compatibility.skipWorldRender()"));
        assertTrue(source.contains("compatibility.allowCustomShaders()"));
        assertTrue(source.contains("compatibility.visualWaveComponents"));
        assertTrue(source.contains("compatibility.oceanBaseAlpha()"));
        assertTrue(source.contains("compatibility.whitecapMultiplier()"));
    }
}
