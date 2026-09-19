package com.redslovesgames.newestocean.client.water;

import com.redslovesgames.newestocean.ocean.OceanSpectrum;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import com.redslovesgames.newestocean.ocean.WaveComponent;

/**
 * Immutable renderer-neutral GPU payload for the real-water path.
 *
 * GLSL layout per wave:
 * A = (directionX, directionZ, amplitude, waveNumber)
 * B = (angularFrequency, phase, steepness, auxiliary)
 */
public final class OceanWaterWaveData {
    public static final int MAX_COMPONENTS = OceanSpectrum.MAX_COMPONENTS;

    private final PackedWave[] waves;
    private final int availableComponentCount;
    private final int physicalComponentCount;

    private OceanWaterWaveData(
        PackedWave[] waves,
        int availableComponentCount,
        int physicalComponentCount
    ) {
        this.waves = waves;
        this.availableComponentCount = availableComponentCount;
        this.physicalComponentCount = physicalComponentCount;
    }

    public static OceanWaterWaveData from(ProceduralOcean ocean) {
        if (ocean == null) {
            throw new IllegalArgumentException("ocean is required");
        }

        int available = Math.min(MAX_COMPONENTS, ocean.componentCount());
        PackedWave[] packed = new PackedWave[MAX_COMPONENTS];
        for (int i = 0; i < MAX_COMPONENTS; i++) {
            if (i < available) {
                WaveComponent wave = ocean.components().get(i);
                packed[i] = new PackedWave(
                    new WaveA(
                        (float) wave.directionX(),
                        (float) wave.directionZ(),
                        (float) wave.amplitude(),
                        (float) wave.waveNumber()
                    ),
                    new WaveB(
                        (float) wave.angularFrequency(),
                        (float) wave.phase(),
                        (float) wave.steepness(),
                        0.0F
                    )
                );
            } else {
                packed[i] = PackedWave.ZERO;
            }
        }

        return new OceanWaterWaveData(
            packed,
            available,
            Math.min(OceanSpectrum.PHYSICS_COMPONENTS, available)
        );
    }

    public int availableComponentCount() {
        return availableComponentCount;
    }

    public int physicalComponentCount() {
        return physicalComponentCount;
    }

    public PackedWave wave(int index) {
        if (index < 0 || index >= MAX_COMPONENTS) {
            throw new IndexOutOfBoundsException("wave index must be between 0 and " + (MAX_COMPONENTS - 1));
        }
        return waves[index];
    }

    public int visibleComponentCount(int requested, int configuredCap) {
        if (requested < 0) {
            throw new IllegalArgumentException("requested visible component count cannot be negative");
        }
        if (configuredCap < 1) {
            throw new IllegalArgumentException("configured visible component cap must be positive");
        }
        return Math.min(availableComponentCount, Math.min(requested, configuredCap));
    }

    public record WaveA(
        float directionX,
        float directionZ,
        float amplitude,
        float waveNumber
    ) {
        private static final WaveA ZERO = new WaveA(0.0F, 0.0F, 0.0F, 0.0F);
    }

    public record WaveB(
        float angularFrequency,
        float phase,
        float steepness,
        float auxiliary
    ) {
        private static final WaveB ZERO = new WaveB(0.0F, 0.0F, 0.0F, 0.0F);
    }

    public record PackedWave(WaveA a, WaveB b) {
        private static final PackedWave ZERO = new PackedWave(WaveA.ZERO, WaveB.ZERO);

        public PackedWave {
            if (a == null || b == null) {
                throw new IllegalArgumentException("packed wave vectors are required");
            }
        }
    }
}
