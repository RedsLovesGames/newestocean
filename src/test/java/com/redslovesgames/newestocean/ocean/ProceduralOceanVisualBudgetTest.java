package com.redslovesgames.newestocean.ocean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ProceduralOceanVisualBudgetTest {
    @Test
    void fullVisualBudgetMatchesPhysicalSample() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(90210L);
        OceanConditions conditions = OceanConditions.weather(0.4, 0.2, 63.0, com.redslovesgames.newestocean.math.Vec3.ZERO);

        OceanSurface.SurfaceSample physical = ocean.sample(12.5, -34.25, 18.75, conditions);
        OceanSurface.SurfaceSample visual = ocean.sample(12.5, -34.25, 18.75, conditions, ocean.componentCount());

        assertEquals(physical, visual);
    }

    @Test
    void lowerVisualBudgetDropsFineWaveComponents() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(90210L);
        OceanConditions conditions = OceanConditions.weather(0.4, 0.2, 63.0, com.redslovesgames.newestocean.math.Vec3.ZERO);

        OceanSurface.SurfaceSample twoWaves = ocean.sample(12.5, -34.25, 18.75, conditions, 2);
        OceanSurface.SurfaceSample sixWaves = ocean.sample(12.5, -34.25, 18.75, conditions, 6);

        assertNotEquals(twoWaves.height(), sixWaves.height());
    }
}
