package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanGerstnerShaderContractTest {
    @Test
    void oceanVertexShaderUsesGerstnerTangentsForNormal() throws IOException {
        String vertex = resource("/assets/newestocean/shaders/core/ocean_surface.vsh");

        assertTrue(vertex.contains("tangentX"));
        assertTrue(vertex.contains("tangentZ"));
        assertTrue(vertex.contains("cross(tangentZ, tangentX)"));
        assertFalse(vertex.contains("normalize(vec3(-slope.x, 1.0, -slope.y))"));
    }

    private static String resource(String path) throws IOException {
        try (InputStream stream = OceanGerstnerShaderContractTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
