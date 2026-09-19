package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselWakeGeometryTest {
    @Test
    void geometryIsFiniteSymmetricAndWidensBehindVessel() {
        VesselWakeDescriptor newer = new VesselWakeDescriptor(
            0.0, 0.0, 1.0, 0.0, 0.40, 2.0, 0.0, 1.0
        );
        VesselWakeDescriptor older = new VesselWakeDescriptor(
            -1.0, 0.0, 1.0, 0.0, 0.40, 2.0, 1.0, 0.75
        );

        VesselWakeGeometry.Segment segment = VesselWakeGeometry.segment(newer, older);

        assertTrue(segment.widthAtOlder() > segment.widthAtNewer());
        assertEquals(
            Math.abs(segment.leftArm().olderOuter().z()),
            Math.abs(segment.rightArm().olderOuter().z()),
            1.0e-9
        );
        assertEquals(
            0.0,
            (segment.center().newerInner().z() + segment.center().newerOuter().z()) * 0.5,
            1.0e-9
        );
        assertEquals(
            0.0,
            (segment.center().olderInner().z() + segment.center().olderOuter().z()) * 0.5,
            1.0e-9
        );

        assertFinite(segment.leftArm());
        assertFinite(segment.rightArm());
        assertFinite(segment.center());
    }

    @Test
    void identicalDescriptorsProduceDeterministicGeometry() {
        VesselWakeDescriptor newer = new VesselWakeDescriptor(
            5.0, -3.0, 0.6, 0.8, 0.50, 3.0, 0.2, 0.9
        );
        VesselWakeDescriptor older = new VesselWakeDescriptor(
            4.4, -3.8, 0.6, 0.8, 0.50, 3.0, 1.2, 0.6
        );

        assertEquals(
            VesselWakeGeometry.segment(newer, older),
            VesselWakeGeometry.segment(newer, older)
        );
    }

    private static void assertFinite(VesselWakeGeometry.Strip strip) {
        assertPointFinite(strip.newerInner());
        assertPointFinite(strip.newerOuter());
        assertPointFinite(strip.olderOuter());
        assertPointFinite(strip.olderInner());
    }

    private static void assertPointFinite(VesselWakeGeometry.Point point) {
        assertTrue(Double.isFinite(point.x()));
        assertTrue(Double.isFinite(point.z()));
    }
}
