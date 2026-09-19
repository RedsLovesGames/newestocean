package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VesselWakeQualityTest {
    @Test
    void qualityBudgetsMatchPhaseThirteenSpec() {
        assertEquals(new VesselWakeQuality.Budget(4, 48.0), VesselWakeQuality.budget(OceanQuality.POTATO));
        assertEquals(new VesselWakeQuality.Budget(8, 64.0), VesselWakeQuality.budget(OceanQuality.LOW));
        assertEquals(new VesselWakeQuality.Budget(16, 96.0), VesselWakeQuality.budget(OceanQuality.MEDIUM));
        assertEquals(new VesselWakeQuality.Budget(24, 128.0), VesselWakeQuality.budget(OceanQuality.HIGH));
        assertEquals(new VesselWakeQuality.Budget(32, 160.0), VesselWakeQuality.budget(OceanQuality.ULTRA));
    }
}
