package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselWakeRendererContractTest {
    @Test
    void wakeRendererConsumesTrackerGeometryAndOceanHeight() throws IOException {
        String renderer = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/VesselWakeRenderer.java"
        ));
        String oceanRenderer = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java"
        ));

        assertTrue(renderer.contains("VesselWakeTracker.snapshot"));
        assertTrue(renderer.contains("VesselWakeGeometry.segment"));
        assertTrue(renderer.contains("NewestOcean.clientOcean().sample"));
        assertTrue(renderer.contains("OceanRenderState.drawTwoSided"));
        assertTrue(renderer.contains("OceanRenderCoordinates.relative"));
        assertTrue(oceanRenderer.contains("VesselWakeRenderer.render"));
    }
}
