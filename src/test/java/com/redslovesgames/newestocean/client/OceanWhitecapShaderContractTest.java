package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanWhitecapShaderContractTest {
    @Test
    void oceanShaderDeclaresAndCarriesWhitecapData() throws IOException {
        String vertex = resource("/assets/newestocean/shaders/core/ocean_surface.vsh");
        String fragment = resource("/assets/newestocean/shaders/core/ocean_surface.fsh");
        String definition = resource("/assets/newestocean/shaders/core/ocean_surface.json");

        assertTrue(vertex.contains("OceanFoamQuality"));
        assertTrue(vertex.contains("OceanStormStrength"));
        assertTrue(vertex.contains("crestCurvature"));
        assertTrue(vertex.contains("out float oceanFoam"));

        assertTrue(fragment.contains("in float oceanFoam"));
        assertTrue(fragment.contains("mix(baseColor"));

        assertTrue(definition.contains("\"OceanFoamQuality\""));
        assertTrue(definition.contains("\"OceanStormStrength\""));
    }

    private static String resource(String path) throws IOException {
        try (InputStream stream = OceanWhitecapShaderContractTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
