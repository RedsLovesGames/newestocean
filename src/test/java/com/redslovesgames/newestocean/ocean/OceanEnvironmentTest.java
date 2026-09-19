package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanEnvironmentTest {
    @Test
    void seedDerivationIsStableAndSeparatedFromWorldSeed() {
        long derived = OceanSeed.derive(123456789L);
        assertEquals(derived, OceanSeed.derive(123456789L));
        assertNotEquals(123456789L, derived);
        assertNotEquals(derived, OceanSeed.derive(123456790L));
    }

    @Test
    void tidesAndCurrentsAreDeterministic() {
        long seed = OceanSeed.derive(42L);
        double time = 123.5;

        assertEquals(OceanEnvironment.tideOffset(seed, time), OceanEnvironment.tideOffset(seed, time));
        assertEquals(OceanEnvironment.current(seed, time), OceanEnvironment.current(seed, time));
    }

    @Test
    void tideStaysInsideConfiguredAmplitude() {
        long seed = OceanSeed.derive(99L);
        for (int i = 0; i < 500; i++) {
            double tide = OceanEnvironment.tideOffset(seed, i * 13.0);
            assertTrue(Math.abs(tide) <= 0.1800000001);
        }
    }

    @Test
    void baseCurrentRemainsHorizontalAndBounded() {
        long seed = OceanSeed.derive(-7L);
        for (int i = 0; i < 500; i++) {
            Vec3 current = OceanEnvironment.current(seed, i * 11.0);
            assertEquals(0.0, current.y());
            assertEquals(0.16, current.length(), 1.0e-9);
        }
    }

    @Test
    void conditionsAddTideToBaseWaterHeightAndWeatherBoostsCurrent() {
        long seed = OceanSeed.derive(123L);
        double baseHeight = 63.875;
        double time = 250.0;

        OceanConditions calm = OceanEnvironment.conditions(seed, baseHeight, time, 0.0, 0.0);
        OceanConditions storm = OceanEnvironment.conditions(seed, baseHeight, time, 1.0, 1.0);

        assertEquals(baseHeight + OceanEnvironment.tideOffset(seed, time), calm.tideOffset(), 1.0e-12);
        assertTrue(storm.waveScale() > calm.waveScale());
        assertTrue(storm.current().length() > calm.current().length());
    }
}
