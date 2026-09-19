package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShorelineShaderContractTest {
    @Test
    void shorelineShaderCarriesTerrainMetricsIntoDirectionalBreaking() throws IOException {
        String vertex = Files.readString(Path.of(
            "src/main/resources/assets/newestocean/shaders/core/shoreline_break.vsh"
        ));
        String fragment = Files.readString(Path.of(
            "src/main/resources/assets/newestocean/shaders/core/shoreline_break.fsh"
        ));
        String json = Files.readString(Path.of(
            "src/main/resources/assets/newestocean/shaders/core/shoreline_break.json"
        ));

        assertTrue(vertex.contains("in vec4 Color"));
        assertTrue(vertex.contains("ShorelineWaveA0"));
        assertTrue(vertex.contains("shoreDirection"));
        assertTrue(vertex.contains("incoming"));
        assertTrue(vertex.contains("shoal"));
        assertTrue(vertex.contains("shoreBreaker"));
        assertTrue(fragment.contains("shoreBreaker"));
        assertTrue(fragment.contains("discard"));
        assertTrue(fragment.contains("shoreWash"));
        assertTrue(json.contains("\"Color\""));
        assertTrue(json.contains("ShorelineStormStrength"));
        assertTrue(json.contains("ShorelineQuality"));
    }
}
