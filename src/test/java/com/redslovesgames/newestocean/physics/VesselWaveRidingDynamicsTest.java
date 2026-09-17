package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselWaveRidingDynamicsTest {
    private static final Vec3 FORWARD = new Vec3(0.0, 0.0, 1.0);
    private static final Vec3 DOWNHILL_FORWARD = new Vec3(0.0, 0.98, 0.20).normalize();
    private static final Vec3 DOWNHILL_BACKWARD = new Vec3(0.0, 0.98, -0.20).normalize();

    @Test
    void downhillWaveFaceProducesForwardSurfingForceAtRealisticWaveVelocity() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            new VesselWaveRidingDynamics.Input(
                0.65,
                new Vec3(0.0, 0.0, 2.5),
                new Vec3(0.0, 0.0, 0.15),
                DOWNHILL_FORWARD,
                FORWARD,
                2.0,
                4.0,
                2.0,
                0.8
            )
        );

        assertTrue(result.surfing());
        assertTrue(result.force().z() > 0.0);
        assertEquals(0.0, result.force().x(), 1.0e-12);
    }

    @Test
    void climbingOppositeWaveFaceDoesNotCreateSurfingBoost() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            new VesselWaveRidingDynamics.Input(
                0.70,
                new Vec3(0.0, 0.0, 2.5),
                new Vec3(0.0, 0.0, -0.15),
                DOWNHILL_BACKWARD,
                FORWARD,
                2.0,
                4.0,
                2.0,
                0.8
            )
        );

        assertFalse(result.surfing());
        assertTrue(result.force().z() <= 0.0 || Math.abs(result.force().z()) < 1.0e-12);
    }

    @Test
    void lowSpeedHullDoesNotPlane() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            new VesselWaveRidingDynamics.Input(
                0.80,
                new Vec3(0.0, 0.0, 0.7),
                Vec3.ZERO,
                Vec3.UP,
                FORWARD,
                2.0,
                4.0,
                2.0,
                1.0
            )
        );

        assertFalse(result.planing());
        assertEquals(0.0, result.planingLift(), 1.0e-12);
    }

    @Test
    void fastLightHullGetsPlaningLift() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            new VesselWaveRidingDynamics.Input(
                0.55,
                new Vec3(0.0, 0.0, 4.0),
                Vec3.ZERO,
                Vec3.UP,
                FORWARD,
                1.5,
                3.0,
                1.2,
                1.0
            )
        );

        assertTrue(result.planing());
        assertTrue(result.planingLift() > 0.0);
        assertTrue(result.force().y() > 0.0);
    }

    @Test
    void lowPlaningFactorStronglyReducesLift() {
        VesselWaveRidingDynamics.Result light = VesselWaveRidingDynamics.resolve(
            new VesselWaveRidingDynamics.Input(
                0.55,
                new Vec3(0.0, 0.0, 4.0),
                Vec3.ZERO,
                Vec3.UP,
                FORWARD,
                2.0,
                6.0,
                2.0,
                1.0
            )
        );
        VesselWaveRidingDynamics.Result heavy = VesselWaveRidingDynamics.resolve(
            new VesselWaveRidingDynamics.Input(
                0.55,
                new Vec3(0.0, 0.0, 4.0),
                Vec3.ZERO,
                Vec3.UP,
                FORWARD,
                5.0,
                14.0,
                10.0,
                0.2
            )
        );

        assertTrue(light.planingLift() > heavy.planingLift());
    }

    @Test
    void nearlyDryHullGetsNoSurfingOrPlaningForce() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            new VesselWaveRidingDynamics.Input(
                0.04,
                new Vec3(0.0, 0.0, 5.0),
                new Vec3(0.0, 0.0, 0.2),
                DOWNHILL_FORWARD,
                FORWARD,
                2.0,
                4.0,
                2.0,
                1.0
            )
        );

        assertFalse(result.surfing());
        assertFalse(result.planing());
        assertEquals(Vec3.ZERO, result.force());
    }
}
