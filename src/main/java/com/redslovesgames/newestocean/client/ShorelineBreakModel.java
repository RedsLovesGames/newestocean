package com.redslovesgames.newestocean.client;

/** Pure visual shoreline influence and breaker math. */
public final class ShorelineBreakModel {
    private ShorelineBreakModel() {
    }

    public static int searchRadius(OceanQuality quality) {
        if (quality == null) {
            throw new IllegalArgumentException("quality is required");
        }
        return switch (quality) {
            case POTATO -> 4;
            case LOW -> 6;
            case MEDIUM -> 8;
            case HIGH -> 10;
            case ULTRA -> 12;
        };
    }

    public static double qualityScale(OceanQuality quality) {
        if (quality == null) {
            throw new IllegalArgumentException("quality is required");
        }
        return switch (quality) {
            case POTATO -> 0.45;
            case LOW -> 0.60;
            case MEDIUM -> 0.78;
            case HIGH -> 0.90;
            case ULTRA -> 1.00;
        };
    }

    public static double shoreInfluence(double depthBlocks, double distanceBlocks, double searchRadiusBlocks) {
        requireFinite(depthBlocks, distanceBlocks, searchRadiusBlocks);
        if (depthBlocks < 0.0 || distanceBlocks < 0.0 || searchRadiusBlocks <= 0.0) {
            throw new IllegalArgumentException("shoreline influence inputs are out of range");
        }
        double shallow = clamp01(1.0 - (depthBlocks - 1.0) / 9.0);
        double proximity = clamp01(1.0 - distanceBlocks / searchRadiusBlocks);
        return clamp01(shallow * proximity);
    }

    public static double breakerIntensity(
        ShorelineSample sample,
        double incomingAlignment,
        double waveEnergy,
        double stormStrength,
        OceanQuality quality,
        double breakup
    ) {
        if (sample == null || quality == null) {
            throw new IllegalArgumentException("sample and quality are required");
        }
        requireFinite(incomingAlignment, waveEnergy, stormStrength, breakup);

        double incoming = clamp01(incomingAlignment);
        double energy = clamp01(waveEnergy);
        double storm = clamp01(stormStrength);
        double breakupScale = clamp01(breakup);
        if (incoming <= 0.0 || sample.shoreInfluence() <= 0.0) {
            return 0.0;
        }

        double formation = sample.shoreInfluence() * incoming * (0.35 + 0.65 * energy);
        return clamp01(
            formation
                * (0.75 + 0.45 * storm)
                * qualityScale(quality)
                * breakupScale
        );
    }

    private static void requireFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("shoreline values must be finite");
            }
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
