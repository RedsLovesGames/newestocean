package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveHullProfileTest {
    @Test
    void sampleCountScalesWithHullSize() {
        assertEquals(4, AdaptiveHullProfile.fromDimensions(1.4, 2.0).points().size());
        assertEquals(8, AdaptiveHullProfile.fromDimensions(2.6, 5.0).points().size());
        assertEquals(12, AdaptiveHullProfile.fromDimensions(3.8, 8.0).points().size());
        assertEquals(16, AdaptiveHullProfile.fromDimensions(5.5, 14.0).points().size());
    }

    @Test
    void generatedPointsStaySymmetricAroundHullCenter() {
        AdaptiveHullProfile.Profile profile = AdaptiveHullProfile.fromDimensions(4.0, 9.0);
        List<VesselPhysics.BuoyancyPoint> points = profile.points();

        double meanX = points.stream().map(VesselPhysics.BuoyancyPoint::localPosition).mapToDouble(Vec3::x).average().orElseThrow();
        double meanZ = points.stream().map(VesselPhysics.BuoyancyPoint::localPosition).mapToDouble(Vec3::z).average().orElseThrow();

        assertEquals(0.0, meanX, 1.0e-12);
        assertEquals(0.0, meanZ, 1.0e-12);
    }

    @Test
    void largerShipsGainMassAndSubmersionDepthWithoutExtremeAcceleration() {
        AdaptiveHullProfile.Profile small = AdaptiveHullProfile.fromDimensions(1.4, 2.0);
        AdaptiveHullProfile.Profile large = AdaptiveHullProfile.fromDimensions(5.0, 12.0);

        assertTrue(large.parameters().mass() > small.parameters().mass());
        assertTrue(large.parameters().maxSubmersionDepth() > small.parameters().maxSubmersionDepth());
        assertTrue(large.parameters().maxAcceleration() < small.parameters().maxAcceleration());
    }

    @Test
    void invalidOrExtremeDimensionsAreClampedToSafeFiniteProfiles() {
        AdaptiveHullProfile.Profile profile = AdaptiveHullProfile.fromDimensions(0.01, 500.0);

        assertEquals(16, profile.points().size());
        assertTrue(Double.isFinite(profile.parameters().mass()));
        assertTrue(profile.parameters().mass() > 0.0);
        for (VesselPhysics.BuoyancyPoint point : profile.points()) {
            assertTrue(Double.isFinite(point.localPosition().x()));
            assertTrue(Double.isFinite(point.localPosition().z()));
        }
    }
}
