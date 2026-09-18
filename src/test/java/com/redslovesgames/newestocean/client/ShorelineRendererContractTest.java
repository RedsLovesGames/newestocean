package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShorelineRendererContractTest {
    @Test
    void shorelineRendererUsesCachedFieldGpuPathAndCpuFallback() throws IOException {
        String renderer = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/ShorelineRenderer.java"
        ));

        assertTrue(renderer.contains("ShorelineField"));
        assertTrue(renderer.contains("ShorelineGpuShader.available"));
        assertTrue(renderer.contains("ShorelineBreakModel.breakerIntensity"));
        assertTrue(renderer.contains("NewestOcean.clientOcean().sample"));
        assertTrue(renderer.contains("frame.plan().visualWaveComponents()"));
        assertTrue(renderer.contains("OceanRenderCoordinates.relative"));
        assertTrue(renderer.contains("RenderSystem.depthMask(false)"));
    }
}
