package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OceanLodPlannerConfigTest {
    @Test
    void renderDistanceScaleChangesOnlyVisualOuterRadius() {
        OceanLodPlanner.Plan half = OceanLodPlanner.plan(OceanQuality.LOW, 0.0, 0.0, 0.5);
        OceanLodPlanner.Plan normal = OceanLodPlanner.plan(OceanQuality.LOW, 0.0, 0.0, 1.0);
        OceanLodPlanner.Plan doubleDistance = OceanLodPlanner.plan(OceanQuality.LOW, 0.0, 0.0, 2.0);

        assertEquals(20, outerRadius(half));
        assertEquals(40, outerRadius(normal));
        assertEquals(80, outerRadius(doubleDistance));
        assertEquals(OceanQuality.LOW.visualWaveComponents(), half.visualWaveComponents());
        assertEquals(OceanQuality.LOW.visualWaveComponents(), doubleDistance.visualWaveComponents());
    }

    private static int outerRadius(OceanLodPlanner.Plan plan) {
        return plan.rings().get(plan.rings().size() - 1).outerRadiusBlocks();
    }
}