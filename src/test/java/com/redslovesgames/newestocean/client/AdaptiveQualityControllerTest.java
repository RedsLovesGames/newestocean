package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdaptiveQualityControllerTest {
    @Test
    void sustainedSlowFramesReduceQualityWithoutTouchingPhysics() {
        AdaptiveQualityController controller = new AdaptiveQualityController(OceanQuality.HIGH, 16.7);

        for (int i = 0; i < 180; i++) {
            controller.recordFrame(28.0);
        }

        assertEquals(OceanQuality.MEDIUM, controller.quality());
    }

    @Test
    void sustainedHeadroomCanRaiseQualityOneStep() {
        AdaptiveQualityController controller = new AdaptiveQualityController(OceanQuality.LOW, 16.7);

        for (int i = 0; i < 360; i++) {
            controller.recordFrame(8.0);
        }

        assertEquals(OceanQuality.MEDIUM, controller.quality());
    }
}
