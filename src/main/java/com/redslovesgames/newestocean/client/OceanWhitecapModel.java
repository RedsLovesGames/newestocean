package com.redslovesgames.newestocean.client;

/** Pure visual whitecap model shared by the GPU path and CPU fallback. */
public final class OceanWhitecapModel {
    private OceanWhitecapModel() {
    }

    public static double intensity(
        double slopeMagnitude,
        double crestCurvature,
        double rainGradient,
        double thunderGradient,
        OceanQuality quality,
        double breakup
    ) {
        if (quality == null) {
            throw new IllegalArgumentException("quality is required");
        }
        if (!Double.isFinite(slopeMagnitude)
            || !Double.isFinite(crestCurvature)
            || !Double.isFinite(rainGradient)
            || !Double.isFinite(thunderGradient)
            || !Double.isFinite(breakup)) {
            throw new IllegalArgumentException("whitecap inputs must be finite");
        }

        double slope = smoothstep(0.24, 0.78, Math.max(0.0, slopeMagnitude));
        double crest = smoothstep(0.035, 0.30, Math.max(0.0, crestCurvature));
        double formation = slope * (0.45 + 0.55 * crest);
        double storm = stormStrength(rainGradient, thunderGradient);
        double stormGain = 0.55 + 0.75 * storm;
        double value = formation * stormGain * qualityScale(quality) * clamp01(breakup);
        return clamp01(value);
    }

    public static double stormStrength(double rainGradient, double thunderGradient) {
        if (!Double.isFinite(rainGradient) || !Double.isFinite(thunderGradient)) {
            throw new IllegalArgumentException("weather inputs must be finite");
        }
        double rain = clamp01(rainGradient);
        double thunder = clamp01(thunderGradient);
        return clamp01((rain * 0.55 + thunder * 0.90) / 1.45);
    }

    public static double qualityScale(OceanQuality quality) {
        if (quality == null) {
            throw new IllegalArgumentException("quality is required");
        }
        return switch (quality) {
            case POTATO -> 0.35;
            case LOW -> 0.55;
            case MEDIUM -> 0.75;
            case HIGH -> 0.90;
            case ULTRA -> 1.00;
        };
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
