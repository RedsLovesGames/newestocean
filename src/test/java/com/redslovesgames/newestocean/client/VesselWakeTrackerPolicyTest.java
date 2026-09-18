package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VesselWakeTrackerPolicyTest {
    @Test
    void selectionIsRadiusAndCountBoundedAndNearestFirst() {
        VesselWakeQuality.Budget budget = new VesselWakeQuality.Budget(2, 50.0);
        List<VesselWakeTracker.Candidate> selected = VesselWakeTracker.selectNearest(
            List.of(
                new VesselWakeTracker.Candidate(1, 10.0, 0.0),
                new VesselWakeTracker.Candidate(2, 20.0, 0.0),
                new VesselWakeTracker.Candidate(3, 30.0, 0.0),
                new VesselWakeTracker.Candidate(4, 80.0, 0.0)
            ),
            0.0,
            0.0,
            budget
        );

        assertEquals(List.of(1, 2), selected.stream().map(VesselWakeTracker.Candidate::id).toList());
    }

    @Test
    void equalDistanceSelectionUsesEntityIdForDeterminism() {
        VesselWakeQuality.Budget budget = new VesselWakeQuality.Budget(2, 50.0);
        List<VesselWakeTracker.Candidate> selected = VesselWakeTracker.selectNearest(
            List.of(
                new VesselWakeTracker.Candidate(9, 10.0, 0.0),
                new VesselWakeTracker.Candidate(3, -10.0, 0.0),
                new VesselWakeTracker.Candidate(5, 0.0, 10.0)
            ),
            0.0,
            0.0,
            budget
        );

        assertEquals(List.of(3, 5), selected.stream().map(VesselWakeTracker.Candidate::id).toList());
    }
}
