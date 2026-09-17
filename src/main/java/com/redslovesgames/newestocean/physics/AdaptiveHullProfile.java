package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a cheap buoyancy approximation from a vessel's approximate beam and length.
 *
 * <p>The goal is not to reconstruct the visual hull. It is to give larger vessels enough
 * longitudinal samples to follow swells smoothly without making physics cost scale with model
 * complexity.</p>
 */
public final class AdaptiveHullProfile {
    private static final double MIN_BEAM = 1.0;
    private static final double MAX_BEAM = 8.0;
    private static final double MIN_LENGTH = 1.5;
    private static final double MAX_LENGTH = 24.0;

    private AdaptiveHullProfile() {
    }

    public static Profile fromDimensions(double beam, double length) {
        double safeBeam = clampFinite(beam, MIN_BEAM, MAX_BEAM, MIN_BEAM);
        double safeLength = clampFinite(length, MIN_LENGTH, MAX_LENGTH, MIN_LENGTH);
        if (safeBeam > safeLength) {
            double swap = safeBeam;
            safeBeam = safeLength;
            safeLength = swap;
        }

        double area = safeBeam * safeLength;
        int pointCount = choosePointCount(area, safeLength);
        int rows = pointCount / 2;
        double halfBeam = safeBeam * 0.38;
        double halfSampleLength = safeLength * 0.42;

        List<VesselPhysics.BuoyancyPoint> points = new ArrayList<>(pointCount);
        for (int row = 0; row < rows; row++) {
            double rowFraction = rows == 1 ? 0.0 : (double) row / (rows - 1);
            double z = lerp(-halfSampleLength, halfSampleLength, rowFraction);
            points.add(new VesselPhysics.BuoyancyPoint(new Vec3(-halfBeam, 0.0, z), 1.0));
            points.add(new VesselPhysics.BuoyancyPoint(new Vec3(halfBeam, 0.0, z), 1.0));
        }

        double size01 = clamp01((safeLength - MIN_LENGTH) / (MAX_LENGTH - MIN_LENGTH));
        double mass = clamp(0.8 + area * 0.12, 1.0, 14.0);
        double maxSubmersionDepth = lerp(0.65, 1.45, size01);
        double verticalDamping = lerp(3.0, 4.2, size01);
        double horizontalDrag = lerp(1.15, 0.72, size01);
        double normalInfluence = lerp(0.18, 0.12, size01);
        double maxAcceleration = lerp(24.0, 10.0, size01);

        VesselPhysics.Parameters parameters = new VesselPhysics.Parameters(
            mass,
            12.0,
            verticalDamping,
            horizontalDrag,
            maxSubmersionDepth,
            normalInfluence,
            maxAcceleration
        );
        return new Profile(points, parameters, safeBeam, safeLength);
    }

    private static int choosePointCount(double area, double length) {
        if (length >= 12.0 || area >= 48.0) {
            return 16;
        }
        if (length >= 7.0 || area >= 24.0) {
            return 12;
        }
        if (length >= 4.0 || area >= 8.0) {
            return 8;
        }
        return 4;
    }

    private static double clampFinite(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? clamp(value, min, max) : fallback;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public record Profile(
        List<VesselPhysics.BuoyancyPoint> points,
        VesselPhysics.Parameters parameters,
        double beam,
        double length
    ) {
        public Profile {
            points = List.copyOf(points);
            if (points.isEmpty() || parameters == null) {
                throw new IllegalArgumentException("profile requires buoyancy points and parameters");
            }
        }
    }
}
