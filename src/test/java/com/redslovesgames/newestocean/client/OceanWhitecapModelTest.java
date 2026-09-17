package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanWhitecapModelTest {
    @Test
    void calmFlatWaterProducesNoFoam() {
        double foam = OceanWhitecapModel.intensity(
            0.02,
            0.01,
            0.0,
            0.0,
            OceanQuality.ULTRA,
            1.0
        );

        assertEquals(0.0, foam, 1.0e-9);
    }

    @Test
    void steepSharpCrestProducesWhitecap() {
        double foam = OceanWhitecapModel.intensity(
            0.75,
            1.10,
            0.0,
            0.0,
            OceanQuality.HIGH,
            1.0
        );

        assertTrue(foam > 0.25);
    }

    @Test
    void stormAmplifiesSameCrest() {
        double calm = OceanWhitecapModel.intensity(
            0.55,
            0.72,
            0.0,
            0.0,
            OceanQuality.HIGH,
            1.0
        );
        double storm = OceanWhitecapModel.intensity(
            0.55,
            0.72,
            1.0,
            1.0,
            OceanQuality.HIGH,
            1.0
        );

        assertTrue(storm > calm);
    }

    @Test
    void intensityAlwaysClampsToUnitRange() {
        assertEquals(0.0, OceanWhitecapModel.intensity(0.0, 0.0, 0.0, 0.0, OceanQuality.MEDIUM, -10.0));
        assertEquals(1.0, OceanWhitecapModel.intensity(10.0, 10.0, 1.0, 1.0, OceanQuality.ULTRA, 10.0));
    }

    @Test
    void higherQualityAllowsMoreFoamDetail() {
        double potato = OceanWhitecapModel.intensity(
            0.70,
            1.0,
            0.7,
            0.5,
            OceanQuality.POTATO,
            1.0
        );
        double ultra = OceanWhitecapModel.intensity(
            0.70,
            1.0,
            0.7,
            0.5,
            OceanQuality.ULTRA,
            1.0
        );

        assertTrue(ultra > potato);
    }
}
