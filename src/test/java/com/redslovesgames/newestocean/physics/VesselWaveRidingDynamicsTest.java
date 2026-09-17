package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselWaveRidingDynamicsTest {
    private static final Vec3 FORWARD = new Vec3(0.0, 0.0, 1.0);

    @Test
    void followingSwellProducesForwardSurfingForce() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            new VesselWaveRidingDynamics.Input(
                0.65,
                new Vec3(0.0, 0.0, 1.4),
                new Vec3(0.0, 0.0, 2.4),
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
    void opposingWaterDoesNotCreateSurfingBoost() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            new VesselWaveRidingDynamics.Input(
                0.70,
                new Vec3(0.0, 0.0, 1.5),
                new Vec3(0.0, 0.0, -2.0),
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
                new Vec3(0.0, 0.0, 3.0),
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
