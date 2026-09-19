package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShorelineIntegrationContractTest {
    @Test
    void mainRendererBuildsCachesResetsAndDrawsShorelineBeforeWakes() throws IOException {
        String renderer = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java"
        ));
        String client = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/NewestOceanClient.java"
        ));

        assertTrue(renderer.contains("private static ShorelineField shoreline"));
        assertTrue(renderer.contains("ShorelineAnalyzer.build"));
        assertTrue(renderer.contains("shoreline = null"));
        assertTrue(renderer.contains("coverageBuiltAtTick"));
        assertTrue(renderer.contains("ShorelineRenderer.render"));
        assertTrue(client.contains("ShorelineGpuShader.register"));

        int shorelineDraw = renderer.indexOf("ShorelineRenderer.render");
        int wakeDraw = renderer.indexOf("VesselWakeRenderer.render");
        assertTrue(shorelineDraw >= 0 && wakeDraw > shorelineDraw);
    }
}
