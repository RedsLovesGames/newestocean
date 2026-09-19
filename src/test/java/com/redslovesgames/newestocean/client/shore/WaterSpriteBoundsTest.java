package com.redslovesgames.newestocean.client.shore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterSpriteBoundsTest {
    @Test
    void classifierAcceptsStillAndFlowingWaterUvRangesOnly() {
        WaterSpriteBounds bounds = new WaterSpriteBounds(
            new WaterSpriteBounds.Bounds(0.10F, 0.20F, 0.20F, 0.30F),
            new WaterSpriteBounds.Bounds(0.40F, 0.50F, 0.55F, 0.70F)
        );

        assertTrue(bounds.isWater(0.15F, 0.25F));
        assertTrue(bounds.isWater(0.50F, 0.60F));
        assertFalse(bounds.isWater(0.30F, 0.40F));
    }

    @Test
    void boundsExposeExactVec4UniformOrder() {
        WaterSpriteBounds bounds = new WaterSpriteBounds(
            new WaterSpriteBounds.Bounds(0.10F, 0.20F, 0.30F, 0.40F),
            new WaterSpriteBounds.Bounds(0.50F, 0.60F, 0.70F, 0.80F)
        );

        assertArrayEquals(new float[] {0.10F, 0.20F, 0.30F, 0.40F}, bounds.stillUniform());
        assertArrayEquals(new float[] {0.50F, 0.60F, 0.70F, 0.80F}, bounds.flowingUniform());
    }

    @Test
    void reversedInputCoordinatesAreNormalizedForSafeAtlasMatching() {
        WaterSpriteBounds.Bounds bounds = new WaterSpriteBounds.Bounds(0.4F, 0.7F, 0.2F, 0.5F);

        assertTrue(bounds.contains(0.3F, 0.6F));
        assertFalse(bounds.contains(0.1F, 0.6F));
    }
}
