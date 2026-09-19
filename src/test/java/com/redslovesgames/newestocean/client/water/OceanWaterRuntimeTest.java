package com.redslovesgames.newestocean.client.water;

import com.redslovesgames.newestocean.client.OceanQuality;
import com.redslovesgames.newestocean.client.config.OceanClientConfig;
import com.redslovesgames.newestocean.ocean.OceanEnvironment;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class OceanWaterRuntimeTest {
    @Test
    void buildsFrameFromSynchronizedOceanWeatherAndClientVisualSelection() {
        long seed = 123456789L;
        ProceduralOcean ocean = ProceduralOcean.createDefault(seed);
        OceanClientConfig config = OceanClientConfig.defaults();
        config.setVisualWaveOverride(OceanClientConfig.AUTO_WAVES);
        OceanWaterRuntime runtime = new OceanWaterRuntime();

        OceanWaterFrameState frame = runtime.buildFrame(
            17L,
            seed,
            ocean,
            82.5,
            63.0,
            0.45,
            0.20,
            OceanQuality.HIGH,
            config,
            12,
            OceanWaterInjectionState.RendererPath.SODIUM
        );

        assertEquals(17L, frame.frameId());
        assertEquals(82.5, frame.timeSeconds(), 0.0);
        assertEquals(
            Math.min(OceanQuality.HIGH.visualWaveComponents(), 12),
            frame.visualWaveCount()
        );
        assertEquals(
            OceanEnvironment.conditions(seed, 63.0, 82.5, 0.45, 0.20),
            frame.conditions()
        );
        assertEquals(63.0, frame.seaLevel(), 0.0);
        assertEquals(OceanWaterInjectionState.RendererPath.SODIUM, frame.rendererPath());
    }

    @Test
    void reusesPackedWavePayloadForSameSynchronizedOcean() {
        long seed = 77L;
        ProceduralOcean ocean = ProceduralOcean.createDefault(seed);
        OceanWaterRuntime runtime = new OceanWaterRuntime();
        OceanClientConfig config = OceanClientConfig.defaults();

        OceanWaterFrameState first = runtime.buildFrame(
            1L, seed, ocean, 1.0, 63.0, 0.0, 0.0,
            OceanQuality.MEDIUM, config, 24, OceanWaterInjectionState.RendererPath.VANILLA
        );
        OceanWaterFrameState second = runtime.buildFrame(
            2L, seed, ocean, 1.05, 63.0, 0.0, 0.0,
            OceanQuality.MEDIUM, config, 24, OceanWaterInjectionState.RendererPath.VANILLA
        );

        assertSame(first.waveData(), second.waveData());
    }

    @Test
    void explicitVisualOverrideStillCannotExceedRendererCap() {
        long seed = 88L;
        ProceduralOcean ocean = ProceduralOcean.createDefault(seed);
        OceanClientConfig config = OceanClientConfig.defaults();
        config.setVisualWaveOverride(6);
        OceanWaterRuntime runtime = new OceanWaterRuntime();

        OceanWaterFrameState frame = runtime.buildFrame(
            3L, seed, ocean, 4.0, 63.0, 0.0, 0.0,
            OceanQuality.ULTRA, config, 4, OceanWaterInjectionState.RendererPath.IRIS
        );

        assertEquals(4, frame.visualWaveCount());
        assertEquals(6, frame.waveData().physicalComponentCount());
    }
}
