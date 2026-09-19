package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShorelineBreakModelTest {
    @Test
    void qualitySearchRadiiMatchSpec() {
        assertEquals(4, ShorelineBreakModel.searchRadius(OceanQuality.POTATO));
        assertEquals(6, ShorelineBreakModel.searchRadius(OceanQuality.LOW));
        assertEquals(8, ShorelineBreakModel.searchRadius(OceanQuality.MEDIUM));
        assertEquals(10, ShorelineBreakModel.searchRadius(OceanQuality.HIGH));
        assertEquals(12, ShorelineBreakModel.searchRadius(OceanQuality.ULTRA));
    }

    @Test
    void shallowCloseWaterHasMoreInfluenceThanDeepFarWater() {
        double shallow = ShorelineBreakModel.shoreInfluence(2.0, 1.0, 8.0);
        double deep = ShorelineBreakModel.shoreInfluence(10.0, 7.0, 8.0);
        assertTrue(shallow > deep);
        assertTrue(shallow > 0.5);
    }

    @Test
    void incomingWaveBreaksMoreThanParallelOrOutgoingWave() {
        ShorelineSample sample = new ShorelineSample(2.0, 1.0, 1.0, 0.0, 0.9);
        double incoming = ShorelineBreakModel.breakerIntensity(sample, 1.0, 0.8, 0.0, OceanQuality.HIGH, 1.0);
        double parallel = ShorelineBreakModel.breakerIntensity(sample, 0.0, 0.8, 0.0, OceanQuality.HIGH, 1.0);
        double outgoing = ShorelineBreakModel.breakerIntensity(sample, -1.0, 0.8, 0.0, OceanQuality.HIGH, 1.0);
        assertTrue(incoming > parallel);
        assertEquals(0.0, outgoing, 1.0e-9);
    }

    @Test
    void breakerIntensityClampsToUnitInterval() {
        ShorelineSample sample = new ShorelineSample(1.0, 0.25, 1.0, 0.0, 1.0);
        double value = ShorelineBreakModel.breakerIntensity(sample, 5.0, 5.0, 5.0, OceanQuality.ULTRA, 5.0);
        assertTrue(value >= 0.0 && value <= 1.0);
    }

    @Test
    void sampleNormalizesDirectionAndRejectsInvalidValues() {
        ShorelineSample sample = new ShorelineSample(3.0, 2.0, 3.0, 4.0, 0.5);
        assertEquals(1.0, Math.hypot(sample.shoreDirectionX(), sample.shoreDirectionZ()), 1.0e-9);
        assertThrows(IllegalArgumentException.class, () ->
            new ShorelineSample(Double.NaN, 1.0, 1.0, 0.0, 0.5)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ShorelineSample(2.0, 1.0, 0.0, 0.0, 0.5)
        );
    }
}
