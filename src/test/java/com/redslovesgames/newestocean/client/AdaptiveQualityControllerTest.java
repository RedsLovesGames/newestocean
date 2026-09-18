package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdaptiveQualityControllerTest {
    @Test
    void sustainedSlowTimeReducesQualityAfterThreeSeconds() {
        AdaptiveQualityController controller = new AdaptiveQualityController(OceanQuality.HIGH, 16.67);
        for (int i = 0; i < 100; i++) controller.recordFrame(30.0);
        assertEquals(OceanQuality.HIGH, controller.quality());
        controller.recordFrame(30.0);
        assertEquals(OceanQuality.MEDIUM, controller.quality());
    }

    @Test
    void sustainedHeadroomRaisesQualityOnlyAfterTenSeconds() {
        AdaptiveQualityController controller = new AdaptiveQualityController(OceanQuality.LOW, 16.67);
        for (int i = 0; i < 1249; i++) controller.recordFrame(8.0);
        assertEquals(OceanQuality.LOW, controller.quality());
        controller.recordFrame(8.0);
        assertEquals(OceanQuality.MEDIUM, controller.quality());
    }

    @Test
    void neutralFramesDrainBothHysteresisWindowsInsteadOfOscillating() {
        AdaptiveQualityController controller = new AdaptiveQualityController(OceanQuality.HIGH, 16.67);
        for (int i = 0; i < 80; i++) controller.recordFrame(30.0);
        for (int i = 0; i < 200; i++) controller.recordFrame(16.0);
        for (int i = 0; i < 80; i++) controller.recordFrame(30.0);
        assertEquals(OceanQuality.HIGH, controller.quality());
    }

    @Test
    void oneHysteresisWindowChangesOnlyOneTier() {
        AdaptiveQualityController controller = new AdaptiveQualityController(OceanQuality.ULTRA, 16.67);
        for (int i = 0; i < 101; i++) controller.recordFrame(30.0);
        assertEquals(OceanQuality.HIGH, controller.quality());
    }

    @Test
    void invalidAndHugeFramesDoNotDestabilizeQuality() {
        AdaptiveQualityController controller = new AdaptiveQualityController(OceanQuality.MEDIUM, 16.67);
        controller.recordFrame(Double.NaN);
        controller.recordFrame(-1.0);
        controller.recordFrame(10_000.0);
        assertEquals(OceanQuality.MEDIUM, controller.quality());
    }

    @Test
    void forceQualityResetsPendingAdaptation() {
        AdaptiveQualityController controller = new AdaptiveQualityController(OceanQuality.HIGH, 16.67);
        for (int i = 0; i < 90; i++) controller.recordFrame(30.0);
        controller.forceQuality(OceanQuality.ULTRA);
        for (int i = 0; i < 20; i++) controller.recordFrame(30.0);
        assertEquals(OceanQuality.ULTRA, controller.quality());
    }

    @Test
    void configuredMinimumPreventsFurtherDowngrade() {
        AdaptiveQualityController controller = new AdaptiveQualityController(
            OceanQuality.LOW, 16.67, OceanQuality.LOW, OceanQuality.HIGH
        );
        for (int i = 0; i < 220; i++) controller.recordFrame(30.0);
        assertEquals(OceanQuality.LOW, controller.quality());
    }

    @Test
    void configuredMaximumPreventsFurtherUpgrade() {
        AdaptiveQualityController controller = new AdaptiveQualityController(
            OceanQuality.HIGH, 16.67, OceanQuality.LOW, OceanQuality.HIGH
        );
        for (int i = 0; i < 2600; i++) controller.recordFrame(8.0);
        assertEquals(OceanQuality.HIGH, controller.quality());
    }

    @Test
    void initialAndForcedQualityAreClampedToConfiguredBounds() {
        AdaptiveQualityController controller = new AdaptiveQualityController(
            OceanQuality.POTATO, 16.67, OceanQuality.MEDIUM, OceanQuality.HIGH
        );
        assertEquals(OceanQuality.MEDIUM, controller.quality());
        controller.forceQuality(OceanQuality.ULTRA);
        assertEquals(OceanQuality.HIGH, controller.quality());
    }
}