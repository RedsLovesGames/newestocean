package com.redslovesgames.newestocean.client.water;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanWaterStateTest {
    @Test
    void frameStateCarriesOnlyImmutableRenderingInputs() {
        OceanWaterWaveData waves = OceanWaterWaveData.from(ProceduralOcean.createDefault(7L));
        OceanConditions conditions = new OceanConditions(1.25, 63.1, new Vec3(0.08, 0.0, -0.03));
        OceanWaterFrameState state = new OceanWaterFrameState(
            91L,
            12.75,
            waves,
            10,
            conditions,
            63.0,
            OceanWaterInjectionState.RendererPath.SODIUM
        );

        assertEquals(91L, state.frameId());
        assertEquals(12.75, state.timeSeconds(), 0.0);
        assertEquals(10, state.visualWaveCount());
        assertEquals(conditions, state.conditions());
        assertEquals(63.0, state.seaLevel(), 0.0);
        assertEquals(OceanWaterInjectionState.RendererPath.SODIUM, state.rendererPath());
    }

    @Test
    void frameStateRejectsVisualCountsBeyondPackedWaves() {
        OceanWaterWaveData waves = OceanWaterWaveData.from(ProceduralOcean.createDefault(8L));
        OceanConditions conditions = new OceanConditions(1.0, 63.0, Vec3.ZERO);

        assertThrows(IllegalArgumentException.class, () -> new OceanWaterFrameState(
            1L,
            0.0,
            waves,
            waves.availableComponentCount() + 1,
            conditions,
            63.0,
            OceanWaterInjectionState.RendererPath.VANILLA
        ));
    }

    @Test
    void injectionStateTracksPatchBindingShadowAndFallbackSeparately() {
        OceanWaterInjectionState ready = new OceanWaterInjectionState(
            true,
            true,
            true,
            OceanWaterInjectionState.RendererPath.IRIS,
            ""
        );
        assertTrue(ready.sourcePatchSuccess());
        assertTrue(ready.uniformBindingSuccess());
        assertTrue(ready.shadowPatchSuccess());
        assertTrue(ready.fullyInjected());
        assertFalse(ready.hasFallback());

        OceanWaterInjectionState fallback = new OceanWaterInjectionState(
            false,
            false,
            false,
            OceanWaterInjectionState.RendererPath.IRIS,
            "unsupported Iris transform"
        );
        assertFalse(fallback.fullyInjected());
        assertTrue(fallback.hasFallback());
        assertEquals("unsupported Iris transform", fallback.fallbackReason());
    }
}
