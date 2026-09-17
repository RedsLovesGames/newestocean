package com.redslovesgames.newestocean.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselMotionControllerTest {
    @Test
    void strongUpwardPartialContactStartsLaunch() {
        VesselMotionController.State state = VesselMotionController.advance(
            VesselMotionController.State.initial(),
            new VesselMotionController.Input(0.35, 1.4, 0.20, 2.2)
        );

        assertEquals(VesselMotionController.Mode.LAUNCHING, state.mode());
        assertTrue(state.allowWaterForces());
        assertFalse(state.allowDownwardWaterForce());
    }

    @Test
    void launchBecomesAirborneWhenHullLosesWater() {
        VesselMotionController.State launching = new VesselMotionController.State(
            VesselMotionController.Mode.LAUNCHING,
            2
        );

        VesselMotionController.State state = VesselMotionController.advance(
            launching,
            new VesselMotionController.Input(0.02, 1.1, 0.15, 2.5)
        );

        assertEquals(VesselMotionController.Mode.AIRBORNE, state.mode());
        assertFalse(state.allowWaterForces());
        assertFalse(state.allowDownwardWaterForce());
    }

    @Test
    void airborneStatePersistsWithoutContactAndDoesNotApplyWaterForces() {
        VesselMotionController.State airborne = new VesselMotionController.State(
            VesselMotionController.Mode.AIRBORNE,
            8
        );

        VesselMotionController.State state = VesselMotionController.advance(
            airborne,
            new VesselMotionController.Input(0.0, -0.4, 0.0, 2.0)
        );

        assertEquals(VesselMotionController.Mode.AIRBORNE, state.mode());
        assertFalse(state.allowWaterForces());
    }

    @Test
    void descendingAirborneHullTransitionsToRecontact() {
        VesselMotionController.State airborne = new VesselMotionController.State(
            VesselMotionController.Mode.AIRBORNE,
            7
        );

        VesselMotionController.State state = VesselMotionController.advance(
            airborne,
            new VesselMotionController.Input(0.22, -1.0, 0.10, 1.8)
        );

        assertEquals(VesselMotionController.Mode.RECONTACT, state.mode());
        assertTrue(state.allowWaterForces());
    }

    @Test
    void recontactReturnsToDisplacementAfterHullIsSupportedAgain() {
        VesselMotionController.State recontact = new VesselMotionController.State(
            VesselMotionController.Mode.RECONTACT,
            3
        );

        VesselMotionController.State state = VesselMotionController.advance(
            recontact,
            new VesselMotionController.Input(0.72, -0.2, 0.0, 0.8)
        );

        assertEquals(VesselMotionController.Mode.DISPLACEMENT, state.mode());
        assertTrue(state.allowDownwardWaterForce());
    }

    @Test
    void lowSpeedSmallRipplesDoNotLaunchTheVessel() {
        VesselMotionController.State state = VesselMotionController.advance(
            VesselMotionController.State.initial(),
            new VesselMotionController.Input(0.40, 0.35, 0.10, 0.2)
        );

        assertEquals(VesselMotionController.Mode.DISPLACEMENT, state.mode());
    }
}
