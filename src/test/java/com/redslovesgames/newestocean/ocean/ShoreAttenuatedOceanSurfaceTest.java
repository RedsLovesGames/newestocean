package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoreAttenuatedOceanSurfaceTest {
    private static final Vec3 BASE_NORMAL = new Vec3(0.6, 0.8, 0.0);
    private static final Vec3 BASE_DISPLACEMENT = new Vec3(1.0, 2.0, 3.0);
    private static final Vec3 ORBITAL_VELOCITY = new Vec3(4.0, 5.0, 6.0);

    @Test
    void shoreRemovesWaveMotionButPreservesTideAndBaseCurrent() {
        OceanConditions conditions = new OceanConditions(1.0, 63.25, new Vec3(0.4, 0.0, -0.3));
        ShoreAttenuatedOceanSurface surface = new ShoreAttenuatedOceanSurface(baseSurface(), (x, z) -> 0.0);

        OceanSurface.SurfaceSample sample = surface.sample(4.0, 9.0, 3.0, conditions);

        assertEquals(conditions.tideOffset(), sample.height(), 1.0e-12);
        assertEquals(Vec3.ZERO, sample.displacement());
        assertEquals(Vec3.UP, sample.normal());
        assertEquals(conditions.current(), sample.surfaceVelocity());
    }

    @Test
    void halfwayToOffshoreScalesDisplacementAndOrbitalVelocity() {
        OceanConditions conditions = new OceanConditions(1.0, 10.0, new Vec3(0.4, 0.0, -0.3));
        ShoreAttenuatedOceanSurface surface = new ShoreAttenuatedOceanSurface(baseSurface(), (x, z) -> 7.5);

        OceanSurface.SurfaceSample sample = surface.sample(4.0, 9.0, 3.0, conditions);
        double factor = 0.5;

        assertEquals(BASE_DISPLACEMENT.multiply(factor), sample.displacement());
        assertEquals(conditions.tideOffset() + BASE_DISPLACEMENT.y() * factor, sample.height(), 1.0e-12);
        assertEquals(conditions.current().add(ORBITAL_VELOCITY.multiply(factor)), sample.surfaceVelocity());

        Vec3 expectedNormal = Vec3.UP.multiply(1.0 - factor).add(BASE_NORMAL.multiply(factor)).normalize();
        assertEquals(expectedNormal.x(), sample.normal().x(), 1.0e-12);
        assertEquals(expectedNormal.y(), sample.normal().y(), 1.0e-12);
        assertEquals(expectedNormal.z(), sample.normal().z(), 1.0e-12);
    }

    @Test
    void fifteenBlocksAndBeyondPreserveUnderlyingSurfaceExactly() {
        OceanConditions conditions = new OceanConditions(1.0, 10.0, new Vec3(0.4, 0.0, -0.3));
        OceanSurface base = baseSurface();
        OceanSurface.SurfaceSample expected = base.sample(4.0, 9.0, 3.0, conditions);

        assertEquals(expected, new ShoreAttenuatedOceanSurface(base, (x, z) -> 15.0)
            .sample(4.0, 9.0, 3.0, conditions));
        assertEquals(expected, new ShoreAttenuatedOceanSurface(base, (x, z) -> 100.0)
            .sample(4.0, 9.0, 3.0, conditions));
    }

    private static OceanSurface baseSurface() {
        return (x, z, time, conditions) -> new OceanSurface.SurfaceSample(
            conditions.tideOffset() + BASE_DISPLACEMENT.y(),
            BASE_NORMAL,
            conditions.current().add(ORBITAL_VELOCITY),
            BASE_DISPLACEMENT
        );
    }
}
