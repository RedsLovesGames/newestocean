package com.redslovesgames.newestocean.client.water;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanShaderRequiredUniformTest {
    @Test
    void displacementInputsAreRequiredButCurrentlyMaterialOnlyInputsAreOptional() {
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.WAVE_COUNT));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.TIME));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.WAVE_SCALE));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.CAMERA_ORIGIN));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.SHORE_TEXTURE));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.SHORE_ORIGIN));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.SHORE_SCALE));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.STILL_WATER_BOUNDS));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.FLOWING_WATER_BOUNDS));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.waveAUniform(0)));
        assertTrue(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.waveBUniform(23)));

        assertFalse(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.SEA_LEVEL));
        assertFalse(OceanShaderLibrary.requiredForDisplacement(OceanShaderLibrary.FOAM_STRENGTH));
    }
}
