package com.redslovesgames.newestocean.client;

/**
 * Hysteresis controller that protects frame rate by moving only the visual quality tier.
 * Thresholds accumulate real observed frame time so behavior is independent of current FPS.
 */
public final class AdaptiveQualityController {
    private static final double SLOW_TIME_TO_DOWNGRADE_MS = 3_000.0;
    private static final double FAST_TIME_TO_UPGRADE_MS = 10_000.0;
    private static final double MAX_FRAME_SAMPLE_MS = 250.0;
    private static final double NEUTRAL_DRAIN_RATE = 2.0;

    private final double targetFrameMs;
    private final OceanQuality minQuality;
    private final OceanQuality maxQuality;
    private OceanQuality quality;
    private double slowTimeMs;
    private double fastTimeMs;

    public AdaptiveQualityController(OceanQuality initialQuality, double targetFrameMs) {
        this(initialQuality, targetFrameMs, OceanQuality.POTATO, OceanQuality.ULTRA);
    }

    public AdaptiveQualityController(
        OceanQuality initialQuality,
        double targetFrameMs,
        OceanQuality minQuality,
        OceanQuality maxQuality
    ) {
        if (initialQuality == null || minQuality == null || maxQuality == null) {
            throw new IllegalArgumentException("quality values cannot be null");
        }
        if (!Double.isFinite(targetFrameMs) || targetFrameMs <= 0.0) {
            throw new IllegalArgumentException("targetFrameMs must be finite and positive");
        }
        if (minQuality.ordinal() > maxQuality.ordinal()) {
            throw new IllegalArgumentException("minimum quality cannot exceed maximum quality");
        }
        this.targetFrameMs = targetFrameMs;
        this.minQuality = minQuality;
        this.maxQuality = maxQuality;
        this.quality = clamp(initialQuality);
    }

    public void recordFrame(double frameMs) {
        if (!Double.isFinite(frameMs) || frameMs < 0.0 || frameMs > MAX_FRAME_SAMPLE_MS) {
            return;
        }

        if (frameMs > targetFrameMs * 1.15) {
            slowTimeMs += frameMs;
            fastTimeMs = 0.0;
            if (slowTimeMs > SLOW_TIME_TO_DOWNGRADE_MS) {
                quality = clamp(quality.lower());
                resetWindows();
            }
            return;
        }

        if (frameMs < targetFrameMs * 0.70) {
            fastTimeMs += frameMs;
            slowTimeMs = 0.0;
            if (fastTimeMs >= FAST_TIME_TO_UPGRADE_MS) {
                quality = clamp(quality.higher());
                resetWindows();
            }
            return;
        }

        double drain = frameMs * NEUTRAL_DRAIN_RATE;
        slowTimeMs = Math.max(0.0, slowTimeMs - drain);
        fastTimeMs = Math.max(0.0, fastTimeMs - drain);
    }

    public OceanQuality quality() {
        return quality;
    }

    public void forceQuality(OceanQuality quality) {
        if (quality == null) {
            throw new IllegalArgumentException("quality cannot be null");
        }
        this.quality = clamp(quality);
        resetWindows();
    }

    public void resetSampling() {
        resetWindows();
    }

    private OceanQuality clamp(OceanQuality candidate) {
        if (candidate.ordinal() < minQuality.ordinal()) return minQuality;
        if (candidate.ordinal() > maxQuality.ordinal()) return maxQuality;
        return candidate;
    }

    private void resetWindows() {
        slowTimeMs = 0.0;
        fastTimeMs = 0.0;
    }
}