package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;

public record OceanConditions(double waveScale, double tideOffset, Vec3 current) {
    public static final OceanConditions CALM = new OceanConditions(1.0, 0.0, Vec3.ZERO);

    public OceanConditions {
        if (!Double.isFinite(waveScale) || waveScale < 0.0) {
            throw new IllegalArgumentException("waveScale must be finite and non-negative");
        }
        if (!Double.isFinite(tideOffset)) {
            throw new IllegalArgumentException("tideOffset must be finite");
        }
        if (current == null) {
            throw new IllegalArgumentException("current cannot be null");
        }
    }

    public static OceanConditions weather(double rainGradient, double thunderGradient, double tideOffset, Vec3 current) {
        double rain = clamp01(rainGradient);
        double thunder = clamp01(thunderGradient);
        double scale = 0.65 + rain * 0.55 + thunder * 0.90;
        return new OceanConditions(scale, tideOffset, current);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
