package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanCoverageMaskTest {
    @Test
    void probesEachCellOnceWhenMaskIsBuilt() {
        OceanMeshPlanner.Plan plan = OceanMeshPlanner.plan(OceanQuality.POTATO, 0.0, 0.0);
        AtomicInteger probes = new AtomicInteger();

        OceanCoverageMask mask = OceanCoverageMask.build(plan, (x, z) -> {
            probes.incrementAndGet();
            return x >= 0.0;
        });

        int cellsPerSide = plan.verticesPerSide() - 1;
        assertEquals(cellsPerSide * cellsPerSide, probes.get());
        assertEquals(cellsPerSide * cellsPerSide, mask.cellCount());
    }

    @Test
    void remembersWaterAndLandWithoutReprobing() {
        OceanMeshPlanner.Plan plan = OceanMeshPlanner.plan(OceanQuality.POTATO, 0.0, 0.0);
        OceanCoverageMask mask = OceanCoverageMask.build(plan, (x, z) -> x > 0.0 && z > 0.0);

        int last = plan.verticesPerSide() - 2;
        assertFalse(mask.isWater(0, 0));
        assertTrue(mask.isWater(last, last));
    }
}
