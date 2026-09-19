package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanSurface;

/**
 * Samples the shared ocean surface at four hull points and derives a stable visual pitch/roll target.
 * This is intentionally independent from Minecraft rendering classes so every vessel integration can reuse it.
 */
public final class OceanVesselPose {
    private OceanVesselPose() {
    }

    public static Angles sample(
        OceanSurface surface,
        OceanConditions conditions,
        double timeSeconds,
        Vec3 center,
        double yawRadians,
        double width,
        double length
    ) {
        if (surface == null || conditions == null || center == null) {
            throw new IllegalArgumentException("surface, conditions, and center cannot be null");
        }
        if (!Double.isFinite(width) || width <= 0.0 || !Double.isFinite(length) || length <= 0.0) {
            throw new IllegalArgumentException("width and length must be finite and positive");
        }

        double sin = Math.sin(yawRadians);
        double cos = Math.cos(yawRadians);
        Vec3 right = new Vec3(cos, 0.0, -sin);
        Vec3 forward = new Vec3(sin, 0.0, cos);

        double halfWidth = width * 0.5;
        double halfLength = length * 0.5;

        double front = height(surface, conditions, timeSeconds, center.add(forward.multiply(halfLength)));
        double back = height(surface, conditions, timeSeconds, center.add(forward.multiply(-halfLength)));
        double rightHeight = height(surface, conditions, timeSeconds, center.add(right.multiply(halfWidth)));
        double leftHeight = height(surface, conditions, timeSeconds, center.add(right.multiply(-halfWidth)));

        double pitch = Math.atan2(front - back, length);
        double roll = -Math.atan2(rightHeight - leftHeight, width);
        return new Angles(pitch, roll);
    }

    private static double height(
        OceanSurface surface,
        OceanConditions conditions,
        double timeSeconds,
        Vec3 point
    ) {
        return surface.sample(point.x(), point.z(), timeSeconds, conditions).height();
    }

    public record Angles(double pitchRadians, double rollRadians) {
        public Angles {
            if (!Double.isFinite(pitchRadians) || !Double.isFinite(rollRadians)) {
                throw new IllegalArgumentException("angles must be finite");
            }
        }
    }
}
