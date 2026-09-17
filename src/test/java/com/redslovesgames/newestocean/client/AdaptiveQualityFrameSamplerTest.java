package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveQualityFrameSamplerTest {
    @Test
    void firstTimestampOnlyPrimesSampler() {
        AdaptiveQualityFrameSampler sampler = new AdaptiveQualityFrameSampler(OceanQuality.HIGH, 16.67);

        assertFalse(sampler.recordTimestampNanos(1_000_000_000L).isPresent());
        assertEquals(OceanQuality.HIGH, sampler.quality());
    }

    @Test
    void sustainedThirtyMillisecondFramesRequestOneDowngrade() {
        AdaptiveQualityFrameSampler sampler = new AdaptiveQualityFrameSampler(OceanQuality.HIGH, 16.67);
        long now = 1_000_000_000L;
        sampler.recordTimestampNanos(now);

        boolean changed = false;
        for (int i = 0; i < 101; i++) {
            now += 30_000_000L;
            changed |= sampler.recordTimestampNanos(now).isPresent();
        }

        assertTrue(changed);
        assertEquals(OceanQuality.MEDIUM, sampler.quality());
    }

    @Test
    void longPauseIsIgnoredInsteadOfForcingPotato() {
        AdaptiveQualityFrameSampler sampler = new AdaptiveQualityFrameSampler(OceanQuality.ULTRA, 16.67);
        sampler.recordTimestampNanos(1_000_000_000L);
        sampler.recordTimestampNanos(11_000_000_000L);

        assertEquals(OceanQuality.ULTRA, sampler.quality());
    }

    @Test
    void resetDropsOldTimestampAndPendingWindows() {
        AdaptiveQualityFrameSampler sampler = new AdaptiveQualityFrameSampler(OceanQuality.HIGH, 16.67);
        long now = 1_000_000_000L;
        sampler.recordTimestampNanos(now);
        for (int i = 0; i < 90; i++) {
            now += 30_000_000L;
            sampler.recordTimestampNanos(now);
        }

        sampler.reset();
        now += 10_000_000_000L;
        sampler.recordTimestampNanos(now);
        for (int i = 0; i < 20; i++) {
            now += 30_000_000L;
            sampler.recordTimestampNanos(now);
        }

        assertEquals(OceanQuality.HIGH, sampler.quality());
    }
}
