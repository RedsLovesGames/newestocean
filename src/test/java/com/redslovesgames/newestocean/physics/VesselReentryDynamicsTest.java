package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselReentryDynamicsTest {
    @Test
    void firstContactRampsWaterForceInsteadOfApplyingFullSpike() {
        VesselReentryDynamics.Result result = VesselReentryDynamics.resolve(
            new VesselReentryDynamics.Input(
                VesselMotionController.Mode.RECONTACT,
                0.15,
                -2.5,
                new Vec3(2.0, 16.0, 1.0),
                new Vec3(8.0, 0.0, -6.0),
                0.10,
                -0.08,
                Vec3.ZERO,
                1.5,
                4.0,
                6.0
            )
        );

        assertTrue(result.wettingFactor() > 0.0 && result.wettingFactor() < 1.0);
        assertTrue(result.force().y() > 0.0);
        assertTrue(result.force().y() < 16.0);
    }

    @Test
    void hardReentryForceIsCapped() {
        VesselReentryDynamics.Result result = VesselReentryDynamics.resolve(
            new VesselReentryDynamics.Input(
                VesselMotionController.Mode.RECONTACT,
                0.45,
                -8.0,
                new Vec3(0.0, 500.0, 0.0),
                Vec3.ZERO,
                0.0,
                0.0,
                Vec3.ZERO,
                2.0,
                5.0,
                8.0
            )
        );

        assertTrue(result.force().y() <= 32.0 + 1.0e-9);
        assertTrue(result.impactSeverity() > 0.9);
    }

    @Test
    void largerHullGetsLowerAngularAccelerationForSameTorque() {
        VesselReentryDynamics.Result small = VesselReentryDynamics.resolve(
            new VesselReentryDynamics.Input(
                VesselMotionController.Mode.DISPLACEMENT,
                0.8,
                0.0,
                Vec3.ZERO,
                new Vec3(12.0, 0.0, 12.0),
                0.20,
                -0.20,
                Vec3.ZERO,
                1.4,
                2.0,
                2.0
            )
        );
        VesselReentryDynamics.Result large = VesselReentryDynamics.resolve(
            new VesselReentryDynamics.Input(
                VesselMotionController.Mode.DISPLACEMENT,
                0.8,
                0.0,
                Vec3.ZERO,
                new Vec3(12.0, 0.0, 12.0),
                0.20,
                -0.20,
                Vec3.ZERO,
                5.0,
                14.0,
                10.0
            )
        );

        assertTrue(large.angularAcceleration().length() < small.angularAcceleration().length());
    }

    @Test
    void restoringTorqueOpposesPitchAndRollError() {
        VesselReentryDynamics.Result result = VesselReentryDynamics.resolve(
            new VesselReentryDynamics.Input(
                VesselMotionController.Mode.DISPLACEMENT,
                0.9,
                0.0,
                Vec3.ZERO,
                Vec3.ZERO,
                0.25,
                -0.30,
                Vec3.ZERO,
                3.0,
                8.0,
                7.0
            )
        );

        assertTrue(result.torque().x() < 0.0);
        assertTrue(result.torque().z() > 0.0);
    }

    @Test
    void torqueSpikeIsClampedToHullScaledLimit() {
        VesselReentryDynamics.Result result = VesselReentryDynamics.resolve(
            new VesselReentryDynamics.Input(
                VesselMotionController.Mode.RECONTACT,
                0.30,
                -5.0,
                Vec3.ZERO,
                new Vec3(10000.0, 0.0, -10000.0),
                0.0,
                0.0,
                Vec3.ZERO,
                4.0,
                10.0,
                9.0
            )
        );

        assertTrue(result.torque().length() <= result.torqueLimit() + 1.0e-9);
    }

    @Test
    void fullySupportedDisplacementDoesNotRampNormalForce() {
        Vec3 force = new Vec3(1.0, 8.0, -2.0);
        VesselReentryDynamics.Result result = VesselReentryDynamics.resolve(
            new VesselReentryDynamics.Input(
                VesselMotionController.Mode.DISPLACEMENT,
                0.8,
                0.0,
                force,
                Vec3.ZERO,
                0.0,
                0.0,
                Vec3.ZERO,
                2.0,
                4.0,
                4.0
            )
        );

        assertEquals(force, result.force());
        assertEquals(1.0, result.wettingFactor(), 1.0e-12);
    }
}
