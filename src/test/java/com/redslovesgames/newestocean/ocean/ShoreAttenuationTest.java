package com.redslovesgames.newestocean.ocean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoreAttenuationTest {
    @Test
    void factorIsZeroAtShoreAndOneAtFifteenBlocks() {
        assertEquals(0.0, ShoreAttenuation.factor(0.0), 0.0);
        assertEquals(1.0, ShoreAttenuation.factor(15.0), 0.0);
        assertEquals(1.0, ShoreAttenuation.factor(25.0), 0.0);
    }

    @Test
    void factorUsesSmoothstepAcrossTransition() {
        assertEquals(0.5, ShoreAttenuation.factor(7.5), 1.0e-12);

        double previous = 0.0;
        for (int i = 1; i <= 150; i++) {
            double factor = ShoreAttenuation.factor(i / 10.0);
            assertTrue(factor >= previous, "attenuation must be monotonic");
            previous = factor;
        }
    }

    @Test
    void negativeDistancesClampToShore() {
        assertEquals(0.0, ShoreAttenuation.factor(-3.0), 0.0);
    }
}
