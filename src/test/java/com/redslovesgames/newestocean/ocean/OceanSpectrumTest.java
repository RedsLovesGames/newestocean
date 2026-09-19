package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanSpectrumTest {
    @Test
    void spectrumIsDeterministicAndKeepsPhysicalBudgetFixed() {
        OceanSpectrum first = OceanSpectrum.generate(0x1234ABCDL);
        OceanSpectrum second = OceanSpectrum.generate(0x1234ABCDL);
        OceanSpectrum different = OceanSpectrum.generate(0x1234ABCEL);

        assertEquals(24, OceanSpectrum.MAX_COMPONENTS);
        assertEquals(6, OceanSpectrum.PHYSICS_COMPONENTS);
        assertEquals(OceanSpectrum.MAX_COMPONENTS, first.components().size());
        assertEquals(first.components(), second.components());
        assertNotEquals(first.components(), different.components());

        boolean directionOrPhaseChanged = false;
        for (int i = 0; i < first.components().size(); i++) {
            WaveComponent a = first.components().get(i);
            WaveComponent b = different.components().get(i);
            if (Double.compare(a.directionX(), b.directionX()) != 0
                || Double.compare(a.directionZ(), b.directionZ()) != 0
                || Double.compare(a.phase(), b.phase()) != 0) {
                directionOrPhaseChanged = true;
                break;
            }
        }
        assertTrue(directionOrPhaseChanged);
    }

    @Test
    void spectrumIsOrderedLongestFirstForStableLodPrefixes() {
        List<WaveComponent> components = OceanSpectrum.generate(77L).components();

        for (int i = 1; i < components.size(); i++) {
            assertTrue(
                components.get(i - 1).wavelength() >= components.get(i).wavelength(),
                "component prefixes must keep the dominant long waves first"
            );
        }
    }

    @Test
    void gerstnerSampleMovesInAllThreeAxesAndVelocityMatchesFiniteDifference() {
        WaveComponent wave = new WaveComponent(
            1.0,
            20.0,
            0.6,
            0.8,
            Math.PI / 4.0,
            1.15,
            0.55
        );
        ProceduralOcean ocean = new ProceduralOcean(5L, List.of(wave));
        OceanConditions conditions = new OceanConditions(1.0, 63.0, Vec3.ZERO);

        double x = 2.25;
        double z = -1.75;
        double time = 0.37;
        OceanSurface.SurfaceSample sample = ocean.sample(x, z, time, conditions, 1);

        assertTrue(Math.abs(sample.displacement().x()) > 1.0e-6);
        assertTrue(Math.abs(sample.displacement().y()) > 1.0e-6);
        assertTrue(Math.abs(sample.displacement().z()) > 1.0e-6);
        assertEquals(conditions.tideOffset() + sample.displacement().y(), sample.height(), 1.0e-12);
        assertEquals(1.0, sample.normal().length(), 1.0e-9);
        assertTrue(sample.normal().y() > 0.0);

        double dt = 1.0e-5;
        OceanSurface.SurfaceSample before = ocean.sample(x, z, time - dt, conditions, 1);
        OceanSurface.SurfaceSample after = ocean.sample(x, z, time + dt, conditions, 1);
        Vec3 finiteDifferenceVelocity = new Vec3(
            (after.displacement().x() - before.displacement().x()) / (2.0 * dt),
            (after.height() - before.height()) / (2.0 * dt),
            (after.displacement().z() - before.displacement().z()) / (2.0 * dt)
        );

        assertEquals(finiteDifferenceVelocity.x(), sample.surfaceVelocity().x(), 1.0e-6);
        assertEquals(finiteDifferenceVelocity.y(), sample.surfaceVelocity().y(), 1.0e-6);
        assertEquals(finiteDifferenceVelocity.z(), sample.surfaceVelocity().z(), 1.0e-6);
    }

    @Test
    void defaultPhysicalSampleUsesOnlyTheStableDominantSubset() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(90210L);
        OceanConditions conditions = OceanConditions.weather(0.4, 0.2, 63.0, Vec3.ZERO);

        assertEquals(OceanSpectrum.MAX_COMPONENTS, ocean.componentCount());
        assertEquals(
            ocean.sample(12.5, -34.25, 18.75, conditions, OceanSpectrum.PHYSICS_COMPONENTS),
            ocean.sample(12.5, -34.25, 18.75, conditions)
        );
        assertEquals(
            ocean.sample(12.5, -34.25, 18.75, conditions, 4),
            ocean.sample(12.5, -34.25, 18.75, conditions, 4)
        );
    }
}
