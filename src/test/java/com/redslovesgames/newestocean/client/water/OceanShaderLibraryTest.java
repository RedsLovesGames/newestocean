package com.redslovesgames.newestocean.client.water;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanShaderLibraryTest {
    @Test
    void sharedLibraryDeclaresTheCentralWaterUniformContract() {
        String glsl = OceanShaderLibrary.source();

        assertTrue(glsl.contains(OceanShaderLibrary.MARKER));
        assertTrue(glsl.contains("uniform int " + OceanShaderLibrary.WAVE_COUNT));
        assertTrue(glsl.contains("uniform float " + OceanShaderLibrary.TIME));
        assertTrue(glsl.contains("uniform float " + OceanShaderLibrary.WAVE_SCALE));
        assertTrue(glsl.contains("uniform float " + OceanShaderLibrary.SEA_LEVEL));
        assertTrue(glsl.contains("uniform sampler2D " + OceanShaderLibrary.SHORE_TEXTURE));
        assertTrue(glsl.contains("uniform vec2 " + OceanShaderLibrary.SHORE_ORIGIN));
        assertTrue(glsl.contains("uniform vec2 " + OceanShaderLibrary.SHORE_SCALE));
        assertTrue(glsl.contains("uniform vec4 " + OceanShaderLibrary.STILL_WATER_BOUNDS));
        assertTrue(glsl.contains("uniform vec4 " + OceanShaderLibrary.FLOWING_WATER_BOUNDS));
        assertTrue(glsl.contains("uniform float " + OceanShaderLibrary.FOAM_STRENGTH));
    }

    @Test
    void sharedLibraryHasAllTwentyFourPackedWaveUniformPairs() {
        String glsl = OceanShaderLibrary.source();

        for (int i = 0; i < OceanWaterWaveData.MAX_COMPONENTS; i++) {
            assertTrue(glsl.contains("uniform vec4 " + OceanShaderLibrary.waveAUniform(i)));
            assertTrue(glsl.contains("uniform vec4 " + OceanShaderLibrary.waveBUniform(i)));
        }

        assertEquals("newestocean_waveA0", OceanShaderLibrary.waveAUniform(0));
        assertEquals("newestocean_waveB23", OceanShaderLibrary.waveBUniform(23));
        assertThrows(IllegalArgumentException.class, () -> OceanShaderLibrary.waveAUniform(-1));
        assertThrows(IllegalArgumentException.class, () -> OceanShaderLibrary.waveBUniform(24));
    }

    @Test
    void shaderUsesFixedTwentyFourWaveLoopAndBuildsFullThreeDimensionalSurfaceData() {
        String glsl = OceanShaderLibrary.source();

        assertTrue(glsl.contains("for (int i = 0; i < 24; i++)"));
        assertTrue(glsl.contains("sample.displacement.x"));
        assertTrue(glsl.contains("sample.displacement.y"));
        assertTrue(glsl.contains("sample.displacement.z"));
        assertTrue(glsl.contains("sample.normal"));
        assertTrue(glsl.contains("sample.foam"));
        assertTrue(glsl.contains("newestocean_shoreFactor"));
        assertTrue(glsl.contains("newestocean_isWater"));
    }

    @Test
    void waterClassifierChecksBothStillAndFlowingSpriteBounds() {
        String glsl = OceanShaderLibrary.source();

        assertTrue(glsl.contains("newestocean_uvInside(atlasUv, " + OceanShaderLibrary.STILL_WATER_BOUNDS + ")"));
        assertTrue(glsl.contains("newestocean_uvInside(atlasUv, " + OceanShaderLibrary.FLOWING_WATER_BOUNDS + ")"));
    }
}
