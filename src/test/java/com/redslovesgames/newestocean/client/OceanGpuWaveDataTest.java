package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OceanGpuWaveDataTest {
    @Test
    void packedGpuWaveMathMatchesCpuOceanForVisualComponents() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(987654321L);
        OceanGpuWaveData data = OceanGpuWaveData.from(ocean, 4);
        OceanConditions conditions = new OceanConditions(1.35, 0.18, new Vec3(0.02, 0.0, -0.01));

        double x = 137.25;
        double z = -91.5;
        double time = 23.75;
        OceanSurface.SurfaceSample cpu = ocean.sample(x, z, time, conditions, 4);
        OceanGpuWaveData.Sample gpuReference = data.sampleReference(x, z, time, conditions);

        assertEquals(cpu.height(), gpuReference.height(), 1.0e-9);
        assertEquals(cpu.horizontalDisplacement().x(), gpuReference.displacementX(), 1.0e-9);
        assertEquals(cpu.horizontalDisplacement().z(), gpuReference.displacementZ(), 1.0e-9);
        assertEquals(cpu.normal().x(), gpuReference.normal().x(), 1.0e-9);
        assertEquals(cpu.normal().y(), gpuReference.normal().y(), 1.0e-9);
        assertEquals(cpu.normal().z(), gpuReference.normal().z(), 1.0e-9);
    }

    @Test
    void unusedWaveSlotsAreZeroFilled() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(42L);
        OceanGpuWaveData data = OceanGpuWaveData.from(ocean, 2);

        assertEquals(2, data.activeWaveCount());
        for (int i = 2; i < OceanGpuWaveData.MAX_WAVES; i++) {
            OceanGpuWaveData.PackedWave wave = data.wave(i);
            assertEquals(0.0F, wave.amplitude());
            assertEquals(0.0F, wave.waveNumber());
            assertEquals(0.0F, wave.angularFrequency());
            assertEquals(0.0F, wave.steepness());
        }
    }
}
