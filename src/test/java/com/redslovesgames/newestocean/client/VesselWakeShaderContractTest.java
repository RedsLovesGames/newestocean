package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselWakeShaderContractTest {
    @Test
    void wakeShaderOwnsWaveConformityAndProceduralFoam() throws IOException {
        String shaderJava = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/VesselWakeShader.java"
        ));
        String vertex = Files.readString(Path.of(
            "src/main/resources/assets/newestocean/shaders/core/vessel_wake.vsh"
        ));
        String fragment = Files.readString(Path.of(
            "src/main/resources/assets/newestocean/shaders/core/vessel_wake.fsh"
        ));
        String definition = Files.readString(Path.of(
            "src/main/resources/assets/newestocean/shaders/core/vessel_wake.json"
        ));

        assertTrue(shaderJava.contains("OceanGpuWaveData.from"));
        assertTrue(shaderJava.contains("WakeWaveA"));
        assertTrue(shaderJava.contains("WakeCameraXZ"));
        assertTrue(vertex.contains("WakeWaterHeight"));
        assertTrue(vertex.contains("WakeWaveScale"));
        assertTrue(vertex.contains("WakeWaveCount"));
        assertTrue(vertex.contains("horizontalAmount"));
        assertTrue(fragment.contains("proceduralBreakup"));
        assertTrue(fragment.contains("wakeEdge"));
        assertTrue(fragment.contains("wakeStrength"));
        assertTrue(definition.contains("newestocean:vessel_wake"));
    }
}
