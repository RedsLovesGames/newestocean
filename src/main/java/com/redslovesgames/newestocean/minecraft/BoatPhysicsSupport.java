package com.redslovesgames.newestocean.minecraft;

import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanEnvironment;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import com.redslovesgames.newestocean.physics.AdaptiveHullProfile;
import com.redslovesgames.newestocean.physics.OceanVesselPose;
import com.redslovesgames.newestocean.physics.VesselPhysics;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

final class BoatPhysicsSupport {
    private static final double TICK_SECONDS = 1.0 / 20.0;
    private static final double BLOCKS_PER_SECOND_TO_BLOCKS_PER_TICK = 1.0 / 20.0;

    private BoatPhysicsSupport() {
    }

    static Correction solveCorrection(BoatEntity boat, AdaptiveHullProfile.Profile profile) {
        double baseWaterHeight = boat.getWaterHeightBelow();
        if (!Double.isFinite(baseWaterHeight)) {
            return Correction.ZERO;
        }

        World world = boat.getWorld();
        double timeSeconds = world.getTime() * TICK_SECONDS;
        OceanConditions dynamicConditions = OceanEnvironment.conditions(
            NewestOcean.oceanSeed(),
            baseWaterHeight,
            timeSeconds,
            world.getRainGradient(1.0F),
            world.getThunderGradient(1.0F)
        );
        OceanConditions flatConditions = new OceanConditions(0.0, baseWaterHeight, Vec3.ZERO);
        VesselPhysics.State state = capture(boat);
        OceanSurface ocean = NewestOcean.ocean();
        VesselPhysics.Parameters parameters = profile.parameters();

        VesselPhysics.Result dynamic = VesselPhysics.solve(
            ocean,
            dynamicConditions,
            timeSeconds,
            state,
            profile.points(),
            parameters
        );
        VesselPhysics.Result flat = VesselPhysics.solve(
            ocean,
            flatConditions,
            timeSeconds,
            state,
            profile.points(),
            parameters
        );

        double yawRadians = Math.toRadians(-boat.getYaw());
        OceanVesselPose.Angles poseTarget = OceanVesselPose.sample(
            ocean,
            dynamicConditions,
            timeSeconds,
            new Vec3(boat.getX(), baseWaterHeight, boat.getZ()),
            yawRadians,
            profile.beam(),
            profile.length()
        );

        return new Correction(
            dynamic.force().subtract(flat.force()),
            dynamic.torque().subtract(flat.torque()),
            dynamic.contact(),
            poseTarget
        );
    }

    static void applyForceCorrection(BoatEntity boat, Vec3 force, VesselPhysics.Parameters parameters) {
        Vec3 acceleration = force.multiply(1.0 / parameters.mass());
        Vec3 velocityDeltaPerSecond = acceleration.multiply(TICK_SECONDS);
        Vec3 velocityDeltaPerTick = velocityDeltaPerSecond.multiply(BLOCKS_PER_SECOND_TO_BLOCKS_PER_TICK);

        Vec3d current = boat.getVelocity();
        boat.setVelocity(
            current.x + velocityDeltaPerTick.x(),
            current.y + velocityDeltaPerTick.y(),
            current.z + velocityDeltaPerTick.z()
        );
    }

    private static VesselPhysics.State capture(BoatEntity boat) {
        Vec3d velocity = boat.getVelocity();
        Vec3 velocityPerSecond = new Vec3(
            velocity.x * 20.0,
            velocity.y * 20.0,
            velocity.z * 20.0
        );

        double yawRadians = Math.toRadians(-boat.getYaw());
        return new VesselPhysics.State(
            VesselPhysics.Pose.uprightYaw(
                new Vec3(boat.getX(), boat.getY(), boat.getZ()),
                yawRadians
            ),
            velocityPerSecond,
            Vec3.ZERO
        );
    }

    record Correction(
        Vec3 force,
        Vec3 torque,
        VesselPhysics.ContactState contact,
        OceanVesselPose.Angles poseTarget
    ) {
        static final Correction ZERO = new Correction(
            Vec3.ZERO,
            Vec3.ZERO,
            new VesselPhysics.ContactState(0.0, 0.0, Vec3.ZERO, 0.0, 0.0, 0.0, 0.0),
            new OceanVesselPose.Angles(0.0, 0.0)
        );
    }
}
