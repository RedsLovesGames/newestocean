package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderCompatibilityOverlayContractTest {
    @Test
    void wakesAndShorelineBypassCustomShadersInCompatibilityMode() throws IOException {
        String wake = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/VesselWakeRenderer.java"
        ));
        String shore = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/ShorelineRenderer.java"
        ));
        String ocean = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java"
        ));

        assertTrue(wake.contains("compatibility.allowCustomShaders() && VesselWakeShader.available()"));
        assertTrue(wake.contains("compatibility.visualWaveComponents"));
        assertTrue(wake.contains("compatibility.wakeMultiplier()"));

        assertTrue(shore.contains("compatibility.allowCustomShaders() && ShorelineGpuShader.available()"));
        assertTrue(shore.contains("compatibility.visualWaveComponents"));
        assertTrue(shore.contains("compatibility.shorelineMultiplier()"));

        assertTrue(ocean.contains("ShorelineRenderer.render"));
        assertTrue(ocean.contains("VesselWakeRenderer.render"));
        assertTrue(ocean.contains("compatibility"));
    }
}
