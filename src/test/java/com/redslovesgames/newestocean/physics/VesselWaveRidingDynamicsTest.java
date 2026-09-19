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
            input(0.65, new Vec3(0.0, 0.0, 2.5), new Vec3(0.0, 0.0, 0.15), DOWNHILL_FORWARD, 0.8)
        );

        assertTrue(result.surfing());
        assertTrue(result.force().z() > 0.0);
        assertEquals(0.0, result.force().x(), 1.0e-12);
    }

    @Test
    void climbingOppositeWaveFaceDoesNotCreateSurfingBoost() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            input(0.70, new Vec3(0.0, 0.0, 2.5), new Vec3(0.0, 0.0, -0.15), DOWNHILL_BACKWARD, 0.8)
        );

        assertFalse(result.surfing());
        assertTrue(result.force().z() <= 0.0 || Math.abs(result.force().z()) < 1.0e-12);
    }

    @Test
    void lowSpeedHullDoesNotPlane() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            input(0.80, new Vec3(0.0, 0.0, 0.7), Vec3.ZERO, Vec3.UP, 1.0)
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
            input(0.04, new Vec3(0.0, 0.0, 5.0), new Vec3(0.0, 0.0, 0.2), DOWNHILL_FORWARD, 1.0)
        );

        assertFalse(result.surfing());
        assertFalse(result.planing());
        assertEquals(Vec3.ZERO, result.force());
    }

    @Test
    void planingSupportFollowsTheLocalTiltedSurfaceNormal() {
        Vec3 normal = new Vec3(0.30, 0.95, 0.10).normalize();
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            input(0.55, new Vec3(0.0, 0.0, 4.5), Vec3.ZERO, normal, 1.0)
        );

        assertTrue(result.planing());
        assertTrue(result.planingSupport().length() > 0.0);
        assertTrue(result.planingSupport().normalize().dot(normal) > 0.999999);
        assertTrue(result.planingSupport().x() > 0.0);
    }

    @Test
    void lateralOrbitalVelocityPushesSidewaysAndReversesWithTheWave() {
        VesselWaveRidingDynamics.Result positive = VesselWaveRidingDynamics.resolve(
            input(0.60, new Vec3(0.0, 0.0, 3.0), new Vec3(1.5, 0.0, 0.0), Vec3.UP, 0.8)
        );
        VesselWaveRidingDynamics.Result negative = VesselWaveRidingDynamics.resolve(
            input(0.60, new Vec3(0.0, 0.0, 3.0), new Vec3(-1.5, 0.0, 0.0), Vec3.UP, 0.8)
        );

        assertTrue(positive.lateralAcceleration() > 0.0);
        assertTrue(positive.force().x() > 0.0);
        assertTrue(negative.lateralAcceleration() < 0.0);
        assertTrue(negative.force().x() < 0.0);
    }

    @Test
    void lateralOrbitalCouplingIsCapped() {
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            input(0.80, new Vec3(0.0, 0.0, 4.0), new Vec3(100.0, 0.0, 0.0), Vec3.UP, 1.0)
        );

        assertTrue(Math.abs(result.lateralAcceleration()) <= 2.0);
    }

    @Test
    void fasterDrierPlaningGetsMoreBoundedDragRelief() {
        VesselWaveRidingDynamics.Result slowWet = VesselWaveRidingDynamics.resolve(
            input(0.85, new Vec3(0.0, 0.0, 2.4), Vec3.ZERO, Vec3.UP, 1.0)
        );
        VesselWaveRidingDynamics.Result fastDry = VesselWaveRidingDynamics.resolve(
            input(0.45, new Vec3(0.0, 0.0, 6.5), Vec3.ZERO, Vec3.UP, 1.0)
        );

        assertTrue(fastDry.dragReliefFraction() > slowWet.dragReliefFraction());
        assertTrue(fastDry.dragReliefFraction() > 0.0);
        assertTrue(fastDry.dragReliefFraction() < 1.0);
    }

    @Test
    void crossWaveSlopeReducesPlaningAndGlidingEfficiency() {
        VesselWaveRidingDynamics.Result flat = VesselWaveRidingDynamics.resolve(
            input(0.55, new Vec3(0.0, 0.0, 5.0), Vec3.ZERO, Vec3.UP, 1.0)
        );
        Vec3 crossWave = new Vec3(0.45, 0.893, 0.0).normalize();
        VesselWaveRidingDynamics.Result crossed = VesselWaveRidingDynamics.resolve(
            input(0.55, new Vec3(0.0, 0.0, 5.0), Vec3.ZERO, crossWave, 1.0)
        );

        assertTrue(crossed.planingSupport().length() < flat.planingSupport().length());
        assertTrue(crossed.dragReliefFraction() < flat.dragReliefFraction());
    }

    @Test
    void roughCrossWaveCanReduceForwardMomentumInsteadOfBecomingFreeAcceleration() {
        Vec3 steepCrossWave = new Vec3(0.65, 0.760, 0.0).normalize();
        VesselWaveRidingDynamics.Result result = VesselWaveRidingDynamics.resolve(
            input(0.70, new Vec3(0.0, 0.0, 5.0), Vec3.ZERO, steepCrossWave, 0.30)
        );

        assertTrue(result.roughSeaAcceleration() > 0.0);
        assertTrue(result.force().z() < 0.0);
    }

    private static VesselWaveRidingDynamics.Input input(
        double contact,
        Vec3 vesselVelocity,
        Vec3 waterVelocity,
        Vec3 normal,
        double planingFactor
    ) {
        return new VesselWaveRidingDynamics.Input(
            contact,
            vesselVelocity,
            waterVelocity,
            normal,
            FORWARD,
            2.0,
            4.0,
            2.0,
            planingFactor
        );
    }
}
