package com.redslovesgames.newestocean.client;

/**
 * Tiny hysteresis controller that protects frame rate by moving only the visual quality tier.
 * It intentionally reacts slowly so transient spikes do not make the ocean visibly pop between tiers.
 */
public final class AdaptiveQualityController {
    private static final int SLOW_FRAMES_TO_DOWNGRADE = 120;
    private static final int FAST_FRAMES_TO_UPGRADE = 300;

    private final double targetFrameMs;
    private OceanQuality quality;
    private int slowFrames;
    private int fastFrames;

    public AdaptiveQualityController(OceanQuality initialQuality, double targetFrameMs) {
        if (initialQuality == null) {
            throw new IllegalArgumentException("initialQuality cannot be null");
        }
        if (!Double.isFinite(targetFrameMs) || targetFrameMs <= 0.0) {
            throw new IllegalArgumentException("targetFrameMs must be finite and positive");
        }
        this.quality = initialQuality;
        this.targetFrameMs = targetFrameMs;
    }

    public void recordFrame(double frameMs) {
        if (!Double.isFinite(frameMs) || frameMs < 0.0) {
            return;
        }

        if (frameMs > targetFrameMs * 1.15) {
            slowFrames++;
            fastFrames = 0;
            if (slowFrames >= SLOW_FRAMES_TO_DOWNGRADE) {
                quality = quality.lower();
                slowFrames = 0;
            }
            return;
        }

        if (frameMs < targetFrameMs * 0.70) {
            fastFrames++;
            slowFrames = 0;
            if (fastFrames >= FAST_FRAMES_TO_UPGRADE) {
                quality = quality.higher();
                fastFrames = 0;
            }
            return;
        }

        slowFrames = Math.max(0, slowFrames - 2);
        fastFrames = Math.max(0, fastFrames - 2);
    }

    public OceanQuality quality() {
        return quality;
    }

    public void forceQuality(OceanQuality quality) {
        if (quality == null) {
            throw new IllegalArgumentException("quality cannot be null");
        }
        this.quality = quality;
        this.slowFrames = 0;
        this.fastFrames = 0;
    }
}
