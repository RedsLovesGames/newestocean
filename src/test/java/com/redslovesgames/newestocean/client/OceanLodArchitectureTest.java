package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanLodArchitectureTest {
    @Test
    void highQualityUsesDenseNearRingAndProgressivelyCoarserOuterRings() {
        OceanLodPlanner.Plan plan = OceanLodPlanner.plan(OceanQuality.HIGH, 13.4, -8.2);

        assertTrue(plan.rings().size() >= 3);
        assertEquals(0, plan.rings().getFirst().innerRadiusBlocks());
        for (int i = 1; i < plan.rings().size(); i++) {
            OceanLodPlanner.Ring previous = plan.rings().get(i - 1);
            OceanLodPlanner.Ring current = plan.rings().get(i);
            assertEquals(previous.outerRadiusBlocks(), current.innerRadiusBlocks());
            assertTrue(current.gridStepBlocks() > previous.gridStepBlocks());
        }
        assertEquals(OceanQuality.HIGH.renderRadiusBlocks(), plan.rings().getLast().outerRadiusBlocks());
    }

    @Test
    void cameraOriginSnapsToCoarsestGridForStableTopologyPlacement() {
        OceanLodPlanner.Plan first = OceanLodPlanner.plan(OceanQuality.HIGH, 10.1, 10.1);
        OceanLodPlanner.Plan second = OceanLodPlanner.plan(OceanQuality.HIGH, 11.9, 11.9);

        assertEquals(first.originX(), second.originX());
        assertEquals(first.originZ(), second.originZ());
    }

    @Test
    void topologyCacheReusesGeometryForSameQualityRegardlessOfCameraPosition() {
        OceanLodTopology.Cache cache = new OceanLodTopology.Cache();
        OceanLodTopology first = cache.get(OceanLodPlanner.plan(OceanQuality.MEDIUM, 0.0, 0.0));
        OceanLodTopology second = cache.get(OceanLodPlanner.plan(OceanQuality.MEDIUM, 64.0, -32.0));
        OceanLodTopology ultra = cache.get(OceanLodPlanner.plan(OceanQuality.ULTRA, 0.0, 0.0));

        assertSame(first, second);
        assertNotSame(first, ultra);
        assertTrue(first.vertexCount() > 0);
        assertTrue(first.triangleCount() > 0);
    }

    @Test
    void lodTopologyUsesFarFewerVerticesThanUniformFinestGrid() {
        OceanLodPlanner.Plan plan = OceanLodPlanner.plan(OceanQuality.ULTRA, 0.0, 0.0);
        OceanLodTopology topology = OceanLodTopology.build(plan);

        int radius = OceanQuality.ULTRA.renderRadiusBlocks();
        int finestStep = OceanQuality.ULTRA.gridStepBlocks();
        int uniformSide = radius * 2 / finestStep + 1;
        int uniformVertices = uniformSide * uniformSide;

        assertTrue(topology.vertexCount() < uniformVertices * 0.55);
    }

    @Test
    void coverageMaskCanRemoveLandCellsWithoutRebuildingTopology() {
        OceanLodPlanner.Plan plan = OceanLodPlanner.plan(OceanQuality.MEDIUM, 0.0, 0.0);
        OceanLodTopology topology = OceanLodTopology.build(plan);
        OceanLodCoverageMask mask = OceanLodCoverageMask.build(plan, topology, (x, z) -> x >= 0.0);

        int[] waterIndices = mask.waterIndices(topology);
        assertTrue(waterIndices.length > 0);
        assertTrue(waterIndices.length < topology.indices().length);
        assertEquals(0, waterIndices.length % 3);
    }

    @Test
    void allQualityTiersStayInsidePhaseSevenTopologyBudget() {
        for (OceanQuality quality : OceanQuality.values()) {
            OceanLodTopology topology = OceanLodTopology.build(OceanLodPlanner.plan(quality, 0.0, 0.0));
            assertTrue(topology.vertexCount() <= 18_000, quality + " exceeded LOD vertex budget");
        }
    }
}
