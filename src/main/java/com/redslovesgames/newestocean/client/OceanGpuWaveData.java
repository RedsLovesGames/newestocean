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
        double displacementX = 0.0;
        double displacementZ = 0.0;

        double tangentXx = 1.0;
        double tangentXy = 0.0;
        double tangentXz = 0.0;
        double tangentZx = 0.0;
        double tangentZy = 0.0;
        double tangentZz = 1.0;

        if (conditions.waveScale() != 0.0) {
            for (int i = 0; i < activeWaveCount; i++) {
                PackedWave wave = waves[i];
                double amplitude = wave.amplitude() * conditions.waveScale();
                double directionX = wave.directionX();
                double directionZ = wave.directionZ();
                double waveNumber = wave.waveNumber();
                double theta = waveNumber * (directionX * x + directionZ * z)
                    - wave.angularFrequency() * timeSeconds
                    + wave.phase();
                double sin = Math.sin(theta);
                double cos = Math.cos(theta);
                double steepnessAmplitude = wave.steepness() * amplitude;

                height += amplitude * sin;
                displacementX += steepnessAmplitude * cos * directionX;
                displacementZ += steepnessAmplitude * cos * directionZ;

                double horizontalDerivative = steepnessAmplitude * waveNumber * sin;
                tangentXx -= horizontalDerivative * directionX * directionX;
                tangentXy += amplitude * waveNumber * directionX * cos;
                tangentXz -= horizontalDerivative * directionX * directionZ;
                tangentZx -= horizontalDerivative * directionX * directionZ;
                tangentZy += amplitude * waveNumber * directionZ * cos;
                tangentZz -= horizontalDerivative * directionZ * directionZ;
            }
        }

        Vec3 tangentX = new Vec3(tangentXx, tangentXy, tangentXz);
        Vec3 tangentZ = new Vec3(tangentZx, tangentZy, tangentZz);
        Vec3 normal = tangentZ.cross(tangentX).normalize();
        if (normal.lengthSquared() < 1.0e-18) {
            normal = Vec3.UP;
        } else if (normal.y() < 0.0) {
            normal = normal.multiply(-1.0);
        }
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
