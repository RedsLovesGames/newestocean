package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanRenderStateContractTest {
    @Test
    void sharedRenderStateRestoresDepthBlendAndCull() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanRenderState.java"
        ));

        assertTrue(source.contains("drawSurface"));
        assertTrue(source.contains("drawTwoSided"));
        assertTrue(source.contains("try"));
        assertTrue(source.contains("finally"));
        assertTrue(source.contains("RenderSystem.depthMask(false)"));
        assertTrue(source.contains("RenderSystem.depthMask(true)"));
        assertTrue(source.contains("RenderSystem.enableBlend()"));
        assertTrue(source.contains("RenderSystem.disableBlend()"));
        assertTrue(source.contains("RenderSystem.disableCull()"));
        assertTrue(source.contains("RenderSystem.enableCull()"));
    }

    @Test
    void renderersUseSharedStateHelper() throws IOException {
        String ocean = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java"
        ));
        String wakes = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/VesselWakeRenderer.java"
        ));
        String shoreline = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/ShorelineRenderer.java"
        ));

        assertTrue(ocean.contains("OceanRenderState.drawSurface"));
        assertTrue(wakes.contains("OceanRenderState.drawTwoSided"));
        assertTrue(shoreline.contains("OceanRenderState.drawTwoSided"));
    }
}
