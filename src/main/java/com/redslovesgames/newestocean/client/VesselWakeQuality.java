package com.redslovesgames.newestocean.client;

/** Visual-only Phase 13 wake tracking budgets. */
public final class VesselWakeQuality {
    private VesselWakeQuality() {
    }

    public static Budget budget(OceanQuality quality) {
        if (quality == null) {
            throw new IllegalArgumentException("quality is required");
        }
        return switch (quality) {
            case POTATO -> new Budget(4, 48.0);
            case LOW -> new Budget(8, 64.0);
            case MEDIUM -> new Budget(16, 96.0);
            case HIGH -> new Budget(24, 128.0);
            case ULTRA -> new Budget(32, 160.0);
        };
    }

    public record Budget(int maxVessels, double trackingRadiusBlocks) {
        public Budget {
            if (maxVessels < 1 || !Double.isFinite(trackingRadiusBlocks) || trackingRadiusBlocks <= 0.0) {
                throw new IllegalArgumentException("wake budget values must be positive and finite");
            }
        }
    }
}
