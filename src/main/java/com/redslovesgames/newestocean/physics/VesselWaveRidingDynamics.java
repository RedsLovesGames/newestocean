package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;

/**
 * Adds cheap wave-riding forces on top of the shared buoyancy solution.
 * Surfing uses the existing surface velocity; planing uses vessel speed,
 * hull contact, and a profile-specific planing factor.
 */
public final class VesselWaveRidingDynamics {
    private static final double MIN_CONTACT = 0.10;
    private static final double SURF_MIN_WATER_SPEED = 0.60;
    private static final double SURF_MIN_CARRY_SPEED = 0.15;
    private static final double SURF_MIN_ALIGNMENT = 0.65;
    private static final double SURF_ACCEL_SCALE = 0.80;
    private static final double MAX_SURF_ACCEL = 2.50;
    private static final double PLANING_START_SPEED = 2.0;
    private static final double PLANING_FULL_SPEED = 6.0;
    private static final double PLANING_ACCEL = 3.0;
    private static final double MAX_PLANING_ACCEL = 2.75;

    private VesselWaveRidingDynamics() {
    }

    public static Result resolve(Input input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        if (input.contactFraction() < MIN_CONTACT) {
            return Result.ZERO;
        }

        Vec3 forward = new Vec3(input.forward().x(), 0.0, input.forward().z()).normalize();
        if (forward.lengthSquared() < 0.99) {
            return Result.ZERO;
        }

        Vec3 vesselHorizontal = new Vec3(input.vesselVelocity().x(), 0.0, input.vesselVelocity().z());
        Vec3 waterHorizontal = new Vec3(input.waterVelocity().x(), 0.0, input.waterVelocity().z());
        double forwardSpeed = Math.max(0.0, vesselHorizontal.dot(forward));
        double waterAlong = waterHorizontal.dot(forward);
        double waterSpeed = waterHorizontal.length();
        double alignment = waterSpeed < 1.0e-9 ? 0.0 : waterHorizontal.multiply(1.0 / waterSpeed).dot(forward);
        double carrySpeed = waterAlong - forwardSpeed;

        boolean surfing = input.contactFraction() <= 0.90
            && waterAlong >= SURF_MIN_WATER_SPEED
            && carrySpeed >= SURF_MIN_CARRY_SPEED
            && alignment >= SURF_MIN_ALIGNMENT;

        double surfAcceleration = surfing
            ? Math.min(MAX_SURF_ACCEL, carrySpeed * SURF_ACCEL_SCALE)
            : 0.0;
        Vec3 surfForce = forward.multiply(input.mass() * surfAcceleration);

        double speed01 = clamp01(
            (forwardSpeed - PLANING_START_SPEED) / (PLANING_FULL_SPEED - PLANING_START_SPEED)
        );
        double support = smoothstep(clamp01((input.contactFraction() - MIN_CONTACT) / 0.45));
        double factor = clamp01(input.planingFactor());
        double planingAcceleration = Math.min(
            MAX_PLANING_ACCEL,
            PLANING_ACCEL * speed01 * speed01 * factor * factor * support
        );
        double planingLift = input.mass() * planingAcceleration;
        boolean planing = planingLift > input.mass() * 0.05 && forwardSpeed > PLANING_START_SPEED;

        Vec3 force = surfForce.add(new Vec3(0.0, planingLift, 0.0));
        return new Result(force, surfing, planing, planingLift, surfAcceleration, forwardSpeed);
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public record Input(
        double contactFraction,
        Vec3 vesselVelocity,
        Vec3 waterVelocity,
        Vec3 forward,
        double beam,
        double length,
        double mass,
        double planingFactor
    ) {
        public Input {
            if (vesselVelocity == null || waterVelocity == null || forward == null) {
                throw new IllegalArgumentException("velocity and forward vectors cannot be null");
            }
            if (!Double.isFinite(contactFraction) || contactFraction < 0.0 || contactFraction > 1.0) {
                throw new IllegalArgumentException("contactFraction must be between 0 and 1");
            }
            if (!Double.isFinite(beam) || !Double.isFinite(length) || !Double.isFinite(mass)
                || beam <= 0.0 || length <= 0.0 || mass <= 0.0) {
                throw new IllegalArgumentException("beam, length, and mass must be finite and positive");
            }
            if (!Double.isFinite(planingFactor) || planingFactor < 0.0 || planingFactor > 1.0) {
                throw new IllegalArgumentException("planingFactor must be between 0 and 1");
            }
        }
    }

    public record Result(
        Vec3 force,
        boolean surfing,
        boolean planing,
        double planingLift,
        double surfAcceleration,
        double forwardSpeed
    ) {
        static final Result ZERO = new Result(Vec3.ZERO, false, false, 0.0, 0.0, 0.0);

        public Result {
            if (force == null) {
                throw new IllegalArgumentException("force cannot be null");
            }
        }
    }
}
