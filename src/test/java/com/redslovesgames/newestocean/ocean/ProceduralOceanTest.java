package com.redslovesgames.newestocean.ocean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProceduralOceanTest {
    @Test
    void sameSeedProducesIdenticalOcean() {
        ProceduralOcean first = ProceduralOcean.createDefault(123456789L);
        ProceduralOcean second = ProceduralOcean.createDefault(123456789L);

        assertEquals(first.components(), second.components());

        OceanSurface.SurfaceSample a = first.sample(42.25, -18.5, 73.125, OceanConditions.CALM);
        OceanSurface.SurfaceSample b = second.sample(42.25, -18.5, 73.125, OceanConditions.CALM);
        assertEquals(a, b);
    }

    @Test
    void differentSeedsProduceDifferentWaveFields() {
        ProceduralOcean first = ProceduralOcean.createDefault(1L);
        ProceduralOcean second = ProceduralOcean.createDefault(2L);
        assertNotEquals(first.components(), second.components());
    }

    @Test
    void samplesStayFiniteAndNormalsStayNormalized() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(77L);
        OceanConditions storm = OceanConditions.weather(1.0, 1.0, 0.35, com.redslovesgames.newestocean.math.Vec3.ZERO);

        for (int i = 0; i < 500; i++) {
            double x = i * 13.37 - 2000.0;
            double z = i * -7.91 + 800.0;
            double time = i * 0.125;
            OceanSurface.SurfaceSample sample = ocean.sample(x, z, time, storm);

            assertTrue(Double.isFinite(sample.height()));
            assertTrue(Double.isFinite(sample.surfaceVelocity().x()));
            assertTrue(Double.isFinite(sample.surfaceVelocity().y()));
            assertTrue(Double.isFinite(sample.surfaceVelocity().z()));
            assertEquals(1.0, sample.normal().length(), 1.0e-9);
        }
    }
}
