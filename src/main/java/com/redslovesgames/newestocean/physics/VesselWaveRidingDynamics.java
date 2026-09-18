package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;

/**
 * Adds bounded three-dimensional wave-riding forces on top of the shared buoyancy solution.
 * Surfing follows the moving local wave face, planing support follows the actual surface normal,
 * lateral orbital motion can push across the hull, and rough cross-seas can remove momentum.
 */
public final class VesselWaveRidingDynamics {
    private static final double MIN_CONTACT = 0.10;
    private static final double SURF_MAX_CONTACT = 0.90;
    private static final double SURF_MIN_FORWARD_SPEED = 0.60;
    private static final double SURF_MIN_SLOPE = 0.035;
    private static final double SURF_MIN_ALIGNMENT = 0.45;
    private static final double SURF_GRAVITY_SCALE = 0.42;
    private static final double SURF_WAVE_PUSH_SCALE = 0.20;
    private static final double MAX_SURF_ACCEL = 2.50;

    private static final double PLANING_START_SPEED = 2.0;
    private static final double PLANING_FULL_SPEED = 6.0;
    private static final double PLANING_ACCEL = 3.0;
    private static final double MAX_PLANING_ACCEL = 2.75;
    private static final double NORMAL_WAVE_ASSIST_SCALE = 0.10;
    private static final double MAX_NORMAL_WAVE_ASSIST = 0.15;

    private static final double LATERAL_COUPLING = 0.55;
    private static final double MAX_LATERAL_ACCEL = 1.50;

    private static final double GLIDE_START_SPEED = 3.0;
    private static final double GLIDE_FULL_SPEED = 7.0;
    private static final double MAX_DRAG_RELIEF = 0.65;
    private static final double MAX_DRAG_COMP_ACCEL = 2.0;

    private static final double CROSS_WAVE_PLANING_PENALTY = 0.55;
    private static final double ROUGH_SEA_SCALE = 0.65;
    private static final double MAX_ROUGH_SEA_ACCEL = 2.25;

    private VesselWaveRidingDynamics() {
    }

    public static Result resolve(Input input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        if (input.contactFraction() < MIN_CONTACT) {
            return Result.ZERO;
        }

        Vec3 forward = horizontal(input.forward()).normalize();
        if (forward.lengthSquared() < 0.99) {
            return Result.ZERO;
        }
        Vec3 right = new Vec3(forward.z(), 0.0, -forward.x());

        Vec3 vesselHorizontal = horizontal(input.vesselVelocity());
        Vec3 waterHorizontal = horizontal(input.waterVelocity());
        double forwardSpeed = Math.max(0.0, vesselHorizontal.dot(forward));

        Vec3 normal = input.surfaceNormal().normalize();
        if (normal.lengthSquared() < 0.99) {
            normal = Vec3.UP;
        } else if (normal.y() < 0.0) {
            normal = normal.multiply(-1.0);
        }

        Vec3 horizontalNormal = horizontal(normal);
        double slope = horizontalNormal.length();
        Vec3 downhillDirection = slope < 1.0e-9
            ? Vec3.ZERO
            : horizontalNormal.multiply(1.0 / slope);
        double downhillAlignment = downhillDirection.dot(forward);
        double crossSlope = Math.abs(downhillDirection.dot(right));
        double crossEfficiency = clamp01(1.0 - CROSS_WAVE_PLANING_PENALTY * crossSlope);

        double forwardWaveVelocity = Math.max(0.0, waterHorizontal.dot(forward));
        boolean surfing = input.contactFraction() <= SURF_MAX_CONTACT
            && forwardSpeed >= SURF_MIN_FORWARD_SPEED
            && slope >= SURF_MIN_SLOPE
            && downhillAlignment >= SURF_MIN_ALIGNMENT;

        Vec3 surfForce = Vec3.ZERO;
        double surfAcceleration = 0.0;
        if (surfing) {
            double slopeAcceleration = 9.81 * slope * SURF_GRAVITY_SCALE;
            double wavePushAcceleration = forwardWaveVelocity * SURF_WAVE_PUSH_SCALE;
            Vec3 surfAccelerationVector = downhillDirection.multiply(slopeAcceleration)
                .add(forward.multiply(wavePushAcceleration))
                .clampLength(MAX_SURF_ACCEL);
            surfAcceleration = surfAccelerationVector.length();
            surfForce = surfAccelerationVector.multiply(input.mass());
        }

        double speed01 = clamp01(
            (forwardSpeed - PLANING_START_SPEED) / (PLANING_FULL_SPEED - PLANING_START_SPEED)
        );
        double contactSupport = smoothstep(clamp01((input.contactFraction() - MIN_CONTACT) / 0.45));
        double factor = clamp01(input.planingFactor());
        double normalWaveVelocity = input.waterVelocity().dot(normal);
        double waveAssist = clamp(
            normalWaveVelocity * NORMAL_WAVE_ASSIST_SCALE,
            -MAX_NORMAL_WAVE_ASSIST,
            MAX_NORMAL_WAVE_ASSIST
        );
        double planingAcceleration = Math.min(
            MAX_PLANING_ACCEL,
            PLANING_ACCEL
                * speed01 * speed01
                * factor * factor
                * contactSupport
                * crossEfficiency
                * (1.0 + waveAssist)
        );
        planingAcceleration = Math.max(0.0, planingAcceleration);
        Vec3 planingSupport = normal.multiply(input.mass() * planingAcceleration);
        boolean planing = planingSupport.length() > input.mass() * 0.05
            && forwardSpeed > PLANING_START_SPEED;

        double lateralWaterVelocity = input.waterVelocity().dot(right);
        double lateralAcceleration = clamp(
            lateralWaterVelocity * LATERAL_COUPLING * input.contactFraction(),
            -MAX_LATERAL_ACCEL,
            MAX_LATERAL_ACCEL
        );
        Vec3 lateralForce = right.multiply(input.mass() * lateralAcceleration);

        double glideSpeed01 = clamp01(
            (forwardSpeed - GLIDE_START_SPEED) / (GLIDE_FULL_SPEED - GLIDE_START_SPEED)
        );
        double drierContact = clamp01((0.90 - input.contactFraction()) / 0.65);
        double dragReliefFraction = MAX_DRAG_RELIEF
            * smoothstep(glideSpeed01)
            * (0.35 + 0.65 * drierContact)
            * factor
            * crossEfficiency;
        dragReliefFraction = clamp(dragReliefFraction, 0.0, MAX_DRAG_RELIEF);

        double relativeForwardSpeed = Math.max(
            0.0,
            vesselHorizontal.subtract(waterHorizontal).dot(forward)
        );
        double dragCompAcceleration = Math.min(
            MAX_DRAG_COMP_ACCEL,
            input.horizontalDrag()
                * relativeForwardSpeed
                * input.contactFraction()
                * dragReliefFraction
        );

        double roughness = slope * (0.25 + 0.75 * crossSlope);
        double roughSeaAcceleration = Math.min(
            MAX_ROUGH_SEA_ACCEL,
            forwardSpeed * roughness * ROUGH_SEA_SCALE * input.contactFraction()
        );

        Vec3 forwardAdjustment = forward.multiply(
            input.mass() * (dragCompAcceleration - roughSeaAcceleration)
        );
        Vec3 force = surfForce
            .add(planingSupport)
            .add(lateralForce)
            .add(forwardAdjustment);

        return new Result(
            force,
            surfing,
            planing,
            planingSupport,
            surfAcceleration,
            lateralAcceleration,
            dragReliefFraction,
            roughSeaAcceleration,
            forwardSpeed
        );
    }

    private static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x(), 0.0, vector.z());
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
        double contactFraction,
        Vec3 vesselVelocity,
        Vec3 waterVelocity,
        Vec3 surfaceNormal,
        Vec3 forward,
        double beam,
        double length,
        double mass,
        double planingFactor,
        double horizontalDrag
    ) {
        public Input(
            double contactFraction,
            Vec3 vesselVelocity,
            Vec3 waterVelocity,
            Vec3 surfaceNormal,
            Vec3 forward,
            double beam,
            double length,
            double mass,
            double planingFactor
        ) {
            this(
                contactFraction,
                vesselVelocity,
                waterVelocity,
                surfaceNormal,
                forward,
                beam,
                length,
                mass,
                planingFactor,
                1.2
            );
        }

        public Input {
            if (vesselVelocity == null || waterVelocity == null || surfaceNormal == null || forward == null) {
                throw new IllegalArgumentException("velocity, surfaceNormal, and forward vectors cannot be null");
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
            if (!Double.isFinite(horizontalDrag) || horizontalDrag < 0.0) {
                throw new IllegalArgumentException("horizontalDrag must be finite and non-negative");
            }
        }
    }

    public record Result(
        Vec3 force,
        boolean surfing,
        boolean planing,
        Vec3 planingSupport,
        double surfAcceleration,
        double lateralAcceleration,
        double dragReliefFraction,
        double roughSeaAcceleration,
        double forwardSpeed
    ) {
        static final Result ZERO = new Result(
            Vec3.ZERO,
            false,
            false,
            Vec3.ZERO,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0
        );

        public Result {
            if (force == null || planingSupport == null) {
                throw new IllegalArgumentException("force and planingSupport cannot be null");
            }
        }

        /** Upward world-Y component retained for existing integrations and diagnostics. */
        public double planingLift() {
            return planingSupport.y();
        }
    }
}
