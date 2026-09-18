package com.redslovesgames.newestocean.client.water;

import com.redslovesgames.newestocean.client.OceanQuality;
import com.redslovesgames.newestocean.client.config.OceanClientConfig;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanEnvironment;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;

/**
 * Builds immutable per-frame real-water state without performing rendering operations.
 * Packed deterministic wave parameters are reused until the synchronized ocean changes.
 */
public final class OceanWaterRuntime {
    private long cachedSeed = Long.MIN_VALUE;
    private ProceduralOcean cachedOcean;
    private OceanWaterWaveData cachedWaveData;

    public OceanWaterFrameState buildFrame(
        long frameId,
        long oceanSeed,
        ProceduralOcean ocean,
        double timeSeconds,
        double seaLevel,
        double rainGradient,
        double thunderGradient,
        OceanQuality quality,
        OceanClientConfig config,
        int rendererWaveCap,
        OceanWaterInjectionState.RendererPath rendererPath
    ) {
        if (ocean == null || quality == null || config == null || rendererPath == null) {
            throw new IllegalArgumentException("ocean, quality, config, and renderer path are required");
        }
        if (rendererWaveCap < 1) {
            throw new IllegalArgumentException("rendererWaveCap must be positive");
        }
        if (!Double.isFinite(timeSeconds) || !Double.isFinite(seaLevel)) {
            throw new IllegalArgumentException("timeSeconds and seaLevel must be finite");
        }

        OceanWaterWaveData waveData = waveData(oceanSeed, ocean);
        OceanClientConfig safeConfig = config.copy().sanitize();
        int requestedVisualWaves = safeConfig.effectiveVisualWaveComponents(
            quality.visualWaveComponents()
        );
        int visibleWaveCount = waveData.visibleComponentCount(
            requestedVisualWaves,
            rendererWaveCap
        );
        OceanConditions conditions = OceanEnvironment.conditions(
            oceanSeed,
            seaLevel,
            timeSeconds,
            rainGradient,
            thunderGradient
        );

        return new OceanWaterFrameState(
            frameId,
            timeSeconds,
            waveData,
            visibleWaveCount,
            conditions,
            seaLevel,
            rendererPath
        );
    }

    private OceanWaterWaveData waveData(long oceanSeed, ProceduralOcean ocean) {
        if (cachedWaveData == null || cachedSeed != oceanSeed || cachedOcean != ocean) {
            cachedSeed = oceanSeed;
            cachedOcean = ocean;
            cachedWaveData = OceanWaterWaveData.from(ocean);
        }
        return cachedWaveData;
    }

    public void reset() {
        cachedSeed = Long.MIN_VALUE;
        cachedOcean = null;
        cachedWaveData = null;
    }
}
