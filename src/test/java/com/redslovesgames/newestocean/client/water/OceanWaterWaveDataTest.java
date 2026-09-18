package com.redslovesgames.newestocean.client.water;

import com.redslovesgames.newestocean.ocean.OceanSpectrum;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import com.redslovesgames.newestocean.ocean.WaveComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OceanWaterWaveDataTest {
    private static final double FLOAT_TOLERANCE = 2.0e-6;

    @Test
    void packsAllTwentyFourComponentsInDeterministicOrder() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(0x5EEDBEEFL);
        OceanWaterWaveData data = OceanWaterWaveData.from(ocean);

        assertEquals(OceanSpectrum.MAX_COMPONENTS, data.availableComponentCount());
        assertEquals(OceanSpectrum.MAX_COMPONENTS, OceanWaterWaveData.MAX_COMPONENTS);
        assertEquals(OceanSpectrum.PHYSICS_COMPONENTS, data.physicalComponentCount());

        for (int i = 0; i < ocean.componentCount(); i++) {
            WaveComponent source = ocean.components().get(i);
            OceanWaterWaveData.PackedWave packed = data.wave(i);
            assertEquals(source.directionX(), packed.a().directionX(), FLOAT_TOLERANCE);
            assertEquals(source.directionZ(), packed.a().directionZ(), FLOAT_TOLERANCE);
            assertEquals(source.amplitude(), packed.a().amplitude(), FLOAT_TOLERANCE);
            assertEquals(source.waveNumber(), packed.a().waveNumber(), FLOAT_TOLERANCE);
            assertEquals(source.angularFrequency(), packed.b().angularFrequency(), FLOAT_TOLERANCE);
            assertEquals(source.phase(), packed.b().phase(), FLOAT_TOLERANCE);
            assertEquals(source.steepness(), packed.b().steepness(), FLOAT_TOLERANCE);
            assertEquals(0.0F, packed.b().auxiliary());
        }
    }

    @Test
    void visibleCountRespectsQualityRequestAvailableWavesAndRendererCap() {
        OceanWaterWaveData data = OceanWaterWaveData.from(ProceduralOcean.createDefault(42L));

        assertEquals(4, data.visibleComponentCount(4, 24));
        assertEquals(8, data.visibleComponentCount(14, 8));
        assertEquals(24, data.visibleComponentCount(99, 99));
        assertThrows(IllegalArgumentException.class, () -> data.visibleComponentCount(-1, 24));
        assertThrows(IllegalArgumentException.class, () -> data.visibleComponentCount(4, 0));
    }

    @Test
    void physicalCountDoesNotChangeWhenVisualSelectionChanges() {
        OceanWaterWaveData data = OceanWaterWaveData.from(ProceduralOcean.createDefault(99L));

        data.visibleComponentCount(2, 2);
        assertEquals(OceanSpectrum.PHYSICS_COMPONENTS, data.physicalComponentCount());
        data.visibleComponentCount(24, 24);
        assertEquals(OceanSpectrum.PHYSICS_COMPONENTS, data.physicalComponentCount());
    }
}
