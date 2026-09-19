package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OceanRenderCoordinatesTest {
    @Test
    void worldCoordinatesBecomeCameraRelativeBeforeSubmission() {
        OceanRenderCoordinates.Relative relative = OceanRenderCoordinates.relative(
            120.5, 64.25, -32.0,
            100.0, 60.0, -40.0
        );

        assertEquals(20.5, relative.x(), 1.0e-9);
        assertEquals(4.25, relative.y(), 1.0e-9);
        assertEquals(8.0, relative.z(), 1.0e-9);
    }

    @Test
    void cameraPositionMapsToOrigin() {
        OceanRenderCoordinates.Relative relative = OceanRenderCoordinates.relative(
            8.0, 70.0, 9.0,
            8.0, 70.0, 9.0
        );

        assertEquals(0.0, relative.x(), 1.0e-9);
        assertEquals(0.0, relative.y(), 1.0e-9);
        assertEquals(0.0, relative.z(), 1.0e-9);
    }
}
