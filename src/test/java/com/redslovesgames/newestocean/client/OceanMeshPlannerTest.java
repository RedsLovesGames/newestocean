package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanMeshPlannerTest {
    @Test
    void potatoMeshIsTinyComparedWithUltra() {
        OceanMeshPlanner.Plan potato = OceanMeshPlanner.plan(OceanQuality.POTATO, 13.2, -9.7);
        OceanMeshPlanner.Plan ultra = OceanMeshPlanner.plan(OceanQuality.ULTRA, 13.2, -9.7);

        assertTrue(potato.vertexCount() < 300);
        assertTrue(ultra.vertexCount() > potato.vertexCount() * 50);
        assertEquals(4, potato.visualWaveComponents());
        assertEquals(24, ultra.visualWaveComponents());
    }

    @Test
    void meshOriginSnapsToGridSoSmallCameraMotionDoesNotForceRebuild() {
        OceanMeshPlanner.Plan first = OceanMeshPlanner.plan(OceanQuality.LOW, 10.1, 18.1);
        OceanMeshPlanner.Plan second = OceanMeshPlanner.plan(OceanQuality.LOW, 11.8, 19.9);

        assertEquals(first.originX(), second.originX());
        assertEquals(first.originZ(), second.originZ());
    }

    @Test
    void planProducesConsistentTriangleCount() {
        OceanMeshPlanner.Plan plan = OceanMeshPlanner.plan(OceanQuality.MEDIUM, 0.0, 0.0);
        int side = plan.verticesPerSide();

        assertEquals(side * side, plan.vertexCount());
        assertEquals((side - 1) * (side - 1) * 2, plan.triangleCount());
    }

    @Test
    void eachQualityTierStaysInsideAReasonableCpuMeshBudget() {
        for (OceanQuality quality : OceanQuality.values()) {
            OceanMeshPlanner.Plan plan = OceanMeshPlanner.plan(quality, 0.0, 0.0);
            assertTrue(plan.vertexCount() <= 40_000, quality + " exceeded mesh budget");
        }
    }
}
