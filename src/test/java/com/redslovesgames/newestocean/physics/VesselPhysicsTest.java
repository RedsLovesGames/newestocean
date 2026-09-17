package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselPhysicsTest {
    private static final OceanSurface FLAT_OCEAN = (x, z, time, conditions) ->
        new OceanSurface.SurfaceSample(
            conditions.tideOffset(),
            Vec3.UP,
            conditions.current(),
            Vec3.ZERO
        );

    private static final List<VesselPhysics.BuoyancyPoint> FOUR_POINTS = List.of(
        new VesselPhysics.BuoyancyPoint(new Vec3(-0.5, 0.0, -1.0), 1.0),
        new VesselPhysics.BuoyancyPoint(new Vec3(0.5, 0.0, -1.0), 1.0),
        new VesselPhysics.BuoyancyPoint(new Vec3(-0.5, 0.0, 1.0), 1.0),
        new VesselPhysics.BuoyancyPoint(new Vec3(0.5, 0.0, 1.0), 1.0)
    );

    @Test
    void symmetricSubmersionCreatesLiftWithoutTorque() {
        VesselPhysics.State state = new VesselPhysics.State(
            VesselPhysics.Pose.uprightYaw(new Vec3(0.0, -0.30, 0.0), 0.0),
            Vec3.ZERO,
            Vec3.ZERO
        );

        VesselPhysics.Result result = VesselPhysics.solve(
            FLAT_OCEAN,
            OceanConditions.CALM,
            0.0,
            state,
            FOUR_POINTS,
            VesselPhysics.Parameters.smallBoat()
        );

        assertEquals(4, result.wetPoints());
        assertTrue(result.force().y() > 0.0);
        assertEquals(0.0, result.torque().x(), 1.0e-10);
        assertEquals(0.0, result.torque().y(), 1.0e-10);
        assertEquals(0.0, result.torque().z(), 1.0e-10);
        assertEquals(1.0, result.contact().wetFraction(), 1.0e-12);
        assertEquals(0.0, result.contact().localCenter().x(), 1.0e-12);
        assertEquals(0.0, result.contact().localCenter().z(), 1.0e-12);
        assertEquals(result.contact().bowLoad(), result.contact().sternLoad(), 1.0e-12);
        assertEquals(result.contact().portLoad(), result.contact().starboardLoad(), 1.0e-12);
    }

    @Test
    void dryVesselReceivesNoBuoyancy() {
        VesselPhysics.State state = new VesselPhysics.State(
            VesselPhysics.Pose.uprightYaw(new Vec3(0.0, 2.0, 0.0), 0.0),
            Vec3.ZERO,
            Vec3.ZERO
        );

        VesselPhysics.Result result = VesselPhysics.solve(
            FLAT_OCEAN,
            OceanConditions.CALM,
            0.0,
            state,
            FOUR_POINTS,
            VesselPhysics.Parameters.smallBoat()
        );

        assertEquals(0, result.wetPoints());
        assertEquals(Vec3.ZERO, result.force());
        assertEquals(Vec3.ZERO, result.torque());
        assertEquals(0.0, result.contact().wetFraction());
        assertEquals(0.0, result.contact().weightedSubmersion());
        assertEquals(Vec3.ZERO, result.contact().localCenter());
    }

    @Test
    void currentProducesHorizontalDragForce() {
        VesselPhysics.State state = new VesselPhysics.State(
            VesselPhysics.Pose.uprightYaw(new Vec3(0.0, -0.30, 0.0), 0.0),
            Vec3.ZERO,
            Vec3.ZERO
        );
        OceanConditions current = new OceanConditions(0.0, 0.0, new Vec3(2.0, 0.0, 0.0));

        VesselPhysics.Result result = VesselPhysics.solve(
            FLAT_OCEAN,
            current,
            0.0,
            state,
            FOUR_POINTS,
            VesselPhysics.Parameters.smallBoat()
        );

        assertTrue(result.force().x() > 0.0);
    }

    @Test
    void slopedWaterReportsForwardBiasedContact() {
        OceanSurface sloped = (x, z, time, conditions) ->
            new OceanSurface.SurfaceSample(
                z > 0.0 ? 0.25 : -0.20,
                Vec3.UP,
                Vec3.ZERO,
                Vec3.ZERO
            );
        VesselPhysics.State state = new VesselPhysics.State(
            VesselPhysics.Pose.uprightYaw(Vec3.ZERO, 0.0),
            Vec3.ZERO,
            Vec3.ZERO
        );

        VesselPhysics.Result result = VesselPhysics.solve(
            sloped,
            OceanConditions.CALM,
            0.0,
            state,
            FOUR_POINTS,
            VesselPhysics.Parameters.smallBoat()
        );

        assertEquals(2, result.wetPoints());
        assertEquals(0.5, result.contact().wetFraction(), 1.0e-12);
        assertTrue(result.contact().localCenter().z() > 0.0);
        assertTrue(result.contact().bowLoad() > result.contact().sternLoad());
    }
}
