package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimitedOceanSurfaceTest {
    @Test
    void samplesExactlyTheConfiguredLeadingComponents() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(24680L);
        LimitedOceanSurface limited = new LimitedOceanSurface(ocean, OceanSpectrum.PHYSICS_COMPONENTS);
        OceanConditions conditions = new OceanConditions(1.25, 63.0, new Vec3(0.04, 0.0, -0.02));

        OceanSurface.SurfaceSample expected = ocean.sample(18.25, -44.75, 12.5, conditions, OceanSpectrum.PHYSICS_COMPONENTS);
        OceanSurface.SurfaceSample actual = limited.sample(18.25, -44.75, 12.5, conditions);

        assertEquals(expected, actual);
        assertEquals(OceanSpectrum.PHYSICS_COMPONENTS, limited.componentLimit());
    }

    @Test
    void rejectsLimitsOutsideTheAvailableSpectrum() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(123L);

        assertThrows(IllegalArgumentException.class, () -> new LimitedOceanSurface(ocean, 0));
        assertThrows(IllegalArgumentException.class, () -> new LimitedOceanSurface(ocean, ocean.componentCount() + 1));
    }
}
