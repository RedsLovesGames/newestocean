package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OceanVesselPoseTest {
    private static final OceanSurface SLOPED_SURFACE = (x, z, time, conditions) -> {
        double height = conditions.tideOffset() + 0.10 * x + 0.20 * z;
        Vec3 normal = new Vec3(-0.10, 1.0, -0.20).normalize();
        return new OceanSurface.SurfaceSample(height, normal, Vec3.ZERO, Vec3.ZERO);
    };

    @Test
    void pitchAndRollFollowSurfaceSlope() {
        OceanVesselPose.Angles angles = OceanVesselPose.sample(
            SLOPED_SURFACE,
            new OceanConditions(1.0, 63.0, Vec3.ZERO),
            0.0,
            new Vec3(0.0, 63.0, 0.0),
            0.0,
            2.0,
            4.0
        );

        assertEquals(Math.atan2(0.8, 4.0), angles.pitchRadians(), 1.0e-9);
        assertEquals(-Math.atan2(0.2, 2.0), angles.rollRadians(), 1.0e-9);
    }
}
