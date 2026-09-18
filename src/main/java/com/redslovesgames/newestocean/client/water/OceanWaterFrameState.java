package com.redslovesgames.newestocean.client.water;

import com.redslovesgames.newestocean.ocean.OceanConditions;

/** Immutable input snapshot consumed by renderer-specific water integrations. */
public record OceanWaterFrameState(
    long frameId,
    double timeSeconds,
    OceanWaterWaveData waveData,
    int visualWaveCount,
    OceanConditions conditions,
    double seaLevel,
    OceanWaterInjectionState.RendererPath rendererPath
) {
    public OceanWaterFrameState {
        if (frameId < 0L) {
            throw new IllegalArgumentException("frameId cannot be negative");
        }
        if (!Double.isFinite(timeSeconds) || !Double.isFinite(seaLevel)) {
            throw new IllegalArgumentException("timeSeconds and seaLevel must be finite");
        }
        if (waveData == null || conditions == null || rendererPath == null) {
            throw new IllegalArgumentException("wave data, conditions, and renderer path are required");
        }
        if (visualWaveCount < 0 || visualWaveCount > waveData.availableComponentCount()) {
            throw new IllegalArgumentException("visualWaveCount must fit the packed wave payload");
        }
    }
}
