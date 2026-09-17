package com.redslovesgames.newestocean.client;

import java.util.Optional;

/** Converts monotonic render timestamps into frame-time samples for the adaptive quality controller. */
public final class AdaptiveQualityFrameSampler {
    private final AdaptiveQualityController controller;
    private long lastTimestampNanos = Long.MIN_VALUE;

    public AdaptiveQualityFrameSampler(OceanQuality initialQuality, double targetFrameMs) {
        controller = new AdaptiveQualityController(initialQuality, targetFrameMs);
    }

    public Optional<OceanQuality> recordTimestampNanos(long timestampNanos) {
        if (lastTimestampNanos == Long.MIN_VALUE) {
            lastTimestampNanos = timestampNanos;
            return Optional.empty();
        }

        long elapsedNanos = timestampNanos - lastTimestampNanos;
        lastTimestampNanos = timestampNanos;
        if (elapsedNanos <= 0L) {
            return Optional.empty();
        }

        OceanQuality before = controller.quality();
        controller.recordFrame(elapsedNanos / 1_000_000.0);
        OceanQuality after = controller.quality();
        return before == after ? Optional.empty() : Optional.of(after);
    }

    public OceanQuality quality() {
        return controller.quality();
    }

    public void forceQuality(OceanQuality quality) {
        controller.forceQuality(quality);
        lastTimestampNanos = Long.MIN_VALUE;
    }

    public void reset() {
        controller.resetSampling();
        lastTimestampNanos = Long.MIN_VALUE;
    }
}
