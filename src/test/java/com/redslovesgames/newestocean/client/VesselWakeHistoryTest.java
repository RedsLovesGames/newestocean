package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselWakeHistoryTest {
    @Test
    void stationaryVesselDoesNotCreateWake() {
        VesselWakeHistory history = new VesselWakeHistory();
        assertFalse(history.record(0.0, 0.0, 1.0, 0.0, 0.10, 1.4, 0.0));
        assertTrue(history.snapshot(0.0).isEmpty());
    }

    @Test
    void samplesRequireMovementAndExpire() {
        VesselWakeHistory history = new VesselWakeHistory();
        assertTrue(history.record(0.0, 0.0, 1.0, 0.0, 0.30, 1.4, 0.0));
        assertFalse(history.record(0.4, 0.0, 1.0, 0.0, 0.30, 1.4, 0.1));
        assertTrue(history.record(0.8, 0.0, 1.0, 0.0, 0.30, 1.4, 0.2));
        assertEquals(2, history.snapshot(0.2).size());
        assertTrue(history.snapshot(4.21).isEmpty());
    }

    @Test
    void historyNeverExceedsTwelveSamples() {
        VesselWakeHistory history = new VesselWakeHistory();
        for (int i = 0; i < 20; i++) {
            history.record(i, 0.0, 1.0, 0.0, 0.35, 1.4, i * 0.1);
        }
        assertEquals(12, history.snapshot(2.0).size());
    }

    @Test
    void strengthIncreasesWithSpeedAndBeamAndFadesWithAge() {
        VesselWakeHistory slow = new VesselWakeHistory();
        VesselWakeHistory fast = new VesselWakeHistory();
        VesselWakeHistory wide = new VesselWakeHistory();

        slow.record(0.0, 0.0, 1.0, 0.0, 0.20, 1.0, 0.0);
        fast.record(0.0, 0.0, 1.0, 0.0, 0.55, 1.0, 0.0);
        wide.record(0.0, 0.0, 1.0, 0.0, 0.55, 5.0, 0.0);

        double slowStrength = slow.snapshot(0.0).get(0).strength();
        double fastStrength = fast.snapshot(0.0).get(0).strength();
        double wideStrength = wide.snapshot(0.0).get(0).strength();
        double fadedStrength = wide.snapshot(3.0).get(0).strength();

        assertTrue(fastStrength > slowStrength);
        assertTrue(wideStrength > fastStrength);
        assertTrue(fadedStrength < wideStrength);
    }
}
