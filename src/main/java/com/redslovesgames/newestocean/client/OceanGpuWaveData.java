package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import com.redslovesgames.newestocean.ocean.WaveComponent;

/** Immutable uniform payload shared by the GPU shader and its CPU parity tests. */
public final class OceanGpuWaveData {
    public static final int MAX_WAVES = 6;

    private final PackedWave[] waves;
    private final int activeWaveCount;

    private OceanGpuWaveData(PackedWave[] waves, int activeWaveCount) {
        this.waves = waves;
        this.activeWaveCount = activeWaveCount;
    }

    public static OceanGpuWaveData from(ProceduralOcean ocean, int componentLimit) {
        if (ocean == null) {
            throw new IllegalArgumentException("ocean is required");
        }
        if (componentLimit < 1 || componentLimit > Math.min(MAX_WAVES, ocean.componentCount())) {
            throw new IllegalArgumentException("componentLimit must be between 1 and available GPU waves");
        }

        PackedWave[] packed = new PackedWave[MAX_WAVES];
        for (int i = 0; i < MAX_WAVES; i++) {
            if (i < componentLimit) {
                WaveComponent wave = ocean.components().get(i);
                packed[i] = new PackedWave(
                    (float) wave.directionX(),
                    (float) wave.directionZ(),
                    (float) wave.amplitude(),
                    (float) wave.waveNumber(),
                    (float) wave.phase(),
                    (float) wave.angularFrequency(),
                    (float) wave.steepness()
                );
            } else {
                packed[i] = PackedWave.ZERO;
            }
        }
        return new OceanGpuWaveData(packed, componentLimit);
    }

    public int activeWaveCount() {
        return activeWaveCount;
    }

    public PackedWave wave(int index) {
        return waves[index];
    }

    Sample sampleReference(double x, double z, double timeSeconds, OceanConditions conditions) {
        double height = conditions.tideOffset();
        double slopeX = 0.0;
        double slopeZ = 0.0;
        double displacementX = 0.0;
        double displacementZ = 0.0;

        if (conditions.waveScale() != 0.0) {
            for (int i = 0; i < activeWaveCount; i++) {
                PackedWave wave = waves[i];
                double amplitude = wave.amplitude() * conditions.waveScale();
                double theta = wave.waveNumber() * (wave.directionX() * x + wave.directionZ() * z)
                    - wave.angularFrequency() * timeSeconds
                    + wave.phase();
                double sin = Math.sin(theta);
                double cos = Math.cos(theta);

                height += amplitude * sin;
                slopeX += amplitude * wave.waveNumber() * wave.directionX() * cos;
                slopeZ += amplitude * wave.waveNumber() * wave.directionZ() * cos;
                double horizontalAmount = wave.steepness() * amplitude * cos;
                displacementX += horizontalAmount * wave.directionX();
                displacementZ += horizontalAmount * wave.directionZ();
            }
        }

        Vec3 normal = new Vec3(-slopeX, 1.0, -slopeZ).normalize();
        return new Sample(height, displacementX, displacementZ, normal);
    }

    public record PackedWave(
        float directionX,
        float directionZ,
        float amplitude,
        float waveNumber,
        float phase,
        float angularFrequency,
        float steepness
    ) {
        private static final PackedWave ZERO = new PackedWave(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    record Sample(double height, double displacementX, double displacementZ, Vec3 normal) {
    }
}
