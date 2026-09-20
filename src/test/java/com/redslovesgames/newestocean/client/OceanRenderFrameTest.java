package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanRenderFrameTest {
    @Test
    void unsynchronizedClientProducesNoRenderFrame() {
        assertFalse(OceanRenderFrame.prepare(
            false,
            OceanQuality.MEDIUM,
            12.0,
            -8.0,
            400L,
            0.5,
            63.0,
            0.0,
            0.0,
            123L
        ).isPresent());
    }

    @Test
    void synchronizedFrameUsesInterpolatedMinecraftTimeAndLodPlan() {
        OceanRenderFrame.Frame frame = OceanRenderFrame.prepare(
            true,
            OceanQuality.HIGH,
            12.25,
            -8.75,
            400L,
            0.5,
            63.0,
            0.0,
            0.0,
            123L
        ).orElseThrow();

        assertEquals(20.025, frame.timeSeconds(), 1.0e-9);
        assertEquals(OceanQuality.HIGH, frame.plan().quality());
        assertEquals(14, frame.plan().visualWaveComponents());
    }

    @Test
    void weatherFeedsTheSameOceanEnvironmentUsedByPhysics() {
        OceanRenderFrame.Frame calm = OceanRenderFrame.prepare(
            true, OceanQuality.MEDIUM, 0.0, 0.0, 100L, 0.0, 63.0, 0.0, 0.0, 77L
        ).orElseThrow();
        OceanRenderFrame.Frame storm = OceanRenderFrame.prepare(
            true, OceanQuality.MEDIUM, 0.0, 0.0, 100L, 0.0, 63.0, 1.0, 1.0, 77L
        ).orElseThrow();

        assertTrue(storm.conditions().waveScale() > calm.conditions().waveScale());
        assertTrue(storm.conditions().current().length() > calm.conditions().current().length());
    }
}
