package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselPhysicsLodTest {
    @Test
    void displacementUpdateRateFallsWithDistance() {
        assertEquals(1, VesselPhysicsLod.updateIntervalTicks(0.0, false, VesselMotionController.Mode.DISPLACEMENT));
        assertEquals(1, VesselPhysicsLod.updateIntervalTicks(48.0, false, VesselMotionController.Mode.DISPLACEMENT));
        assertEquals(2, VesselPhysicsLod.updateIntervalTicks(48.01, false, VesselMotionController.Mode.DISPLACEMENT));
        assertEquals(2, VesselPhysicsLod.updateIntervalTicks(96.0, false, VesselMotionController.Mode.DISPLACEMENT));
        assertEquals(4, VesselPhysicsLod.updateIntervalTicks(96.01, false, VesselMotionController.Mode.DISPLACEMENT));
        assertEquals(4, VesselPhysicsLod.updateIntervalTicks(192.0, false, VesselMotionController.Mode.DISPLACEMENT));
        assertEquals(10, VesselPhysicsLod.updateIntervalTicks(192.01, false, VesselMotionController.Mode.DISPLACEMENT));
        assertEquals(10, VesselPhysicsLod.updateIntervalTicks(Double.POSITIVE_INFINITY, false, VesselMotionController.Mode.DISPLACEMENT));
    }

    @Test
    void playerControlledVesselsAlwaysStayAtTwentyHertz() {
        assertEquals(1, VesselPhysicsLod.updateIntervalTicks(5000.0, true, VesselMotionController.Mode.DISPLACEMENT));
    }

    @Test
    void launchAirborneAndRecontactAlwaysStayAtTwentyHertz() {
        assertEquals(1, VesselPhysicsLod.updateIntervalTicks(5000.0, false, VesselMotionController.Mode.LAUNCHING));
        assertEquals(1, VesselPhysicsLod.updateIntervalTicks(5000.0, false, VesselMotionController.Mode.AIRBORNE));
        assertEquals(1, VesselPhysicsLod.updateIntervalTicks(5000.0, false, VesselMotionController.Mode.RECONTACT));
    }

    @Test
    void solveTicksAreStaggeredByEntityId() {
        int interval = 10;
        assertTrue(VesselPhysicsLod.isSolveTick(100L, 0, interval));
        assertFalse(VesselPhysicsLod.isSolveTick(100L, 1, interval));
        assertTrue(VesselPhysicsLod.isSolveTick(109L, 1, interval));
        assertTrue(VesselPhysicsLod.isSolveTick(103L, 7, interval));
    }

    @Test
    void forceInterpolationTransitionsSmoothlyBetweenSparseSolves() {
        VesselPhysicsLod.ForceInterpolator interpolator = new VesselPhysicsLod.ForceInterpolator();
        interpolator.snap(new Vec3(0.0, 0.0, 0.0));
        interpolator.retarget(new Vec3(10.0, 20.0, -10.0), 4);

        assertEquals(new Vec3(2.5, 5.0, -2.5), interpolator.next());
        assertEquals(new Vec3(5.0, 10.0, -5.0), interpolator.next());
        assertEquals(new Vec3(7.5, 15.0, -7.5), interpolator.next());
        assertEquals(new Vec3(10.0, 20.0, -10.0), interpolator.next());
        assertEquals(new Vec3(10.0, 20.0, -10.0), interpolator.next());
    }

    @Test
    void fullRateRetargetAppliesImmediately() {
        VesselPhysicsLod.ForceInterpolator interpolator = new VesselPhysicsLod.ForceInterpolator();
        interpolator.snap(new Vec3(2.0, 3.0, 4.0));
        interpolator.retarget(new Vec3(-1.0, 8.0, 2.0), 1);

        assertEquals(new Vec3(-1.0, 8.0, 2.0), interpolator.next());
    }
}
