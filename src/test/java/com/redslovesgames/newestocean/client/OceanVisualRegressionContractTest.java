package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanVisualRegressionContractTest {
    @Test
    void topologyCacheDoesNotReuseDifferentRenderDistancePlans() {
        OceanLodPlanner.Plan normalPlan = OceanLodPlanner.plan(OceanQuality.MEDIUM, 0.0, 0.0, 1.0);
        OceanLodPlanner.Plan doubledPlan = OceanLodPlanner.plan(OceanQuality.MEDIUM, 0.0, 0.0, 2.0);
        OceanLodTopology.Cache cache = new OceanLodTopology.Cache();

        OceanLodTopology normal = cache.get(normalPlan);
        OceanLodTopology doubled = cache.get(doubledPlan);

        assertEquals(OceanLodTopology.build(normalPlan).cellCount(), normal.cellCount());
        assertEquals(OceanLodTopology.build(doubledPlan).cellCount(), doubled.cellCount());
        assertNotEquals(normal.cellCount(), doubled.cellCount());
    }

    @Test
    void gpuDirectDrawAppliesCameraViewMatrixAtAfterTranslucent() throws IOException {
        String renderer = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java"
        ));
        String state = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanRenderState.java"
        ));

        assertTrue(renderer.contains("WorldRenderEvents.AFTER_TRANSLUCENT.register"));
        assertTrue(renderer.contains("OceanRenderState.drawSurface(builder, cameraMatrix)"));
        assertTrue(state.contains("RenderSystem.getModelViewStack()"));
        assertTrue(state.contains("RenderSystem.applyModelViewMatrix()"));
    }

    @Test
    void oceanSurfaceFadesBeforeFiniteLodBoundary() throws IOException {
        String renderer = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java"
        ));
        String vertexShader = Files.readString(Path.of(
            "src/main/resources/assets/newestocean/shaders/core/ocean_surface.vsh"
        ));
        String fragmentShader = Files.readString(Path.of(
            "src/main/resources/assets/newestocean/shaders/core/ocean_surface.fsh"
        ));

        assertTrue(renderer.contains("OceanSurfaceEdgeFade.factor"));
        assertTrue(vertexShader.contains("out float oceanEdgeFade"));
        assertTrue(fragmentShader.contains("in float oceanEdgeFade"));
        assertTrue(fragmentShader.contains("* oceanEdgeFade"));
    }
}
