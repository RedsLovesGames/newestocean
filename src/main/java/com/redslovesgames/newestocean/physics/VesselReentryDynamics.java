package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;

/**
 * Re-entry force shaping and passive angular stability shared by all vessel adapters.
 * It keeps first water contact progressive, caps slam spikes, and makes large hulls rotate slowly.
 */
public final class VesselReentryDynamics {
    private static final double FULL_WET_CONTACT = 0.55;
    private static final double FORCE_LIMIT_PER_MASS = 4.0;
    private static final double RESTORE_STRENGTH = 3.0;
    private static final double ANGULAR_DAMPING = 1.4;

    private VesselReentryDynamics() {
    }

    public static Result resolve(Input input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }

        double wettingFactor = input.mode() == VesselMotionController.Mode.RECONTACT
            ? smoothstep(clamp01(input.contactFraction() / FULL_WET_CONTACT))
            : 1.0;

        double impactSeverity = clamp01((-input.relativeVerticalVelocity() - 1.0) / 7.0);
        Vec3 shapedForce = input.rawForce().multiply(wettingFactor);
        double verticalLimit = input.mass() * FORCE_LIMIT_PER_MASS;
        shapedForce = new Vec3(
            shapedForce.x(),
            clamp(shapedForce.y(), -verticalLimit, verticalLimit),
            shapedForce.z()
        );

        Vec3 restoring = new Vec3(
            -input.pitchErrorRadians() * input.mass() * input.length() * RESTORE_STRENGTH,
            0.0,
            -input.rollErrorRadians() * input.mass() * input.beam() * RESTORE_STRENGTH
        );
        Vec3 damping = input.angularVelocity().multiply(-input.mass() * ANGULAR_DAMPING);
        Vec3 torque = input.rawTorque().multiply(wettingFactor).add(restoring).add(damping);

        double torqueLimit = input.mass() * (2.0 + 0.6 * Math.max(input.beam(), input.length()));
        torque = torque.clampLength(torqueLimit);

        double inertia = input.mass()
            * (input.beam() * input.beam() + input.length() * input.length())
            / 12.0;
        Vec3 angularAcceleration = torque.multiply(1.0 / Math.max(0.01, inertia));

        return new Result(
            shapedForce,
            torque,
            angularAcceleration,
            wettingFactor,
            impactSeverity,
            torqueLimit
        );
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Input(
        VesselMotionController.Mode mode,
        double contactFraction,
        double relativeVerticalVelocity,
        Vec3 rawForce,
        Vec3 rawTorque,
        double pitchErrorRadians,
        double rollErrorRadians,
        Vec3 angularVelocity,
        double beam,
        double length,
        double mass
    ) {
        public Input {
            if (mode == null || rawForce == null || rawTorque == null || angularVelocity == null) {
                throw new IllegalArgumentException("input values cannot be null");
            }
            if (!Double.isFinite(contactFraction) || contactFraction < 0.0 || contactFraction > 1.0) {
                throw new IllegalArgumentException("contactFraction must be between 0 and 1");
            }
            if (!Double.isFinite(relativeVerticalVelocity)
                || !Double.isFinite(pitchErrorRadians)
                || !Double.isFinite(rollErrorRadians)
                || !Double.isFinite(beam)
                || !Double.isFinite(length)
                || !Double.isFinite(mass)
                || beam <= 0.0 || length <= 0.0 || mass <= 0.0) {
                throw new IllegalArgumentException("re-entry inputs must be finite and dimensions/mass positive");
            }
        }
    }

    public record Result(
        Vec3 force,
        Vec3 torque,
        Vec3 angularAcceleration,
        double wettingFactor,
        double impactSeverity,
        double torqueLimit
    ) {
        public Result {
            if (force == null || torque == null || angularAcceleration == null) {
                throw new IllegalArgumentException("result vectors cannot be null");
            }
        }
    }
}
