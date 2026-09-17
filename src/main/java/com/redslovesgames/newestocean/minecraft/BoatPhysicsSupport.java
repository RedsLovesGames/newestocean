package com.redslovesgames.newestocean.minecraft;

import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanEnvironment;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import com.redslovesgames.newestocean.physics.AdaptiveHullProfile;
import com.redslovesgames.newestocean.physics.OceanVesselPose;
import com.redslovesgames.newestocean.physics.VesselMotionController;
import com.redslovesgames.newestocean.physics.VesselPhysics;
import com.redslovesgames.newestocean.physics.VesselReentryDynamics;
import com.redslovesgames.newestocean.physics.VesselWaveRidingDynamics;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

final class BoatPhysicsSupport {
    private static final double TICK_SECONDS = 1.0 / 20.0;
    private static final double BLOCKS_PER_SECOND_TO_BLOCKS_PER_TICK = 1.0 / 20.0;
    private static final Map<BoatEntity, VesselMotionController.State> MOTION_STATES = new WeakHashMap<>();

    private BoatPhysicsSupport() {
    }

    static void tick(BoatEntity boat, AdaptiveHullProfile.Profile profile) {
        if (boat.getWorld().isClient() || boat.isRemoved()) {
            return;
        }

        VesselMotionController.State previous = MOTION_STATES.getOrDefault(
            boat,
            VesselMotionController.State.initial()
        );

        if (!boat.isTouchingWater()) {
            VesselMotionController.State next = VesselMotionController.advance(
                previous,
                new VesselMotionController.Input(0.0, verticalVelocityPerSecond(boat), 0.0, horizontalSpeedPerSecond(boat))
            );
            MOTION_STATES.put(boat, next);
            return;
        }

        Correction correction = solveCorrection(boat, profile);
        double relativeVerticalVelocity = verticalVelocityPerSecond(boat) - correction.waveVelocity().y();
        VesselMotionController.State next = VesselMotionController.advance(
            previous,
            new VesselMotionController.Input(
                correction.contact().wetFraction(),
                verticalVelocityPerSecond(boat),
                correction.waveVelocity().y(),
                horizontalSpeedPerSecond(boat)
            )
        );
        MOTION_STATES.put(boat, next);

        if (!next.allowWaterForces()) {
            return;
        }

        double pitchError = Math.toRadians(boat.getPitch()) - correction.poseTarget().pitchRadians();
        double rollError = -correction.poseTarget().rollRadians();
        VesselReentryDynamics.Result reentry = VesselReentryDynamics.resolve(
            new VesselReentryDynamics.Input(
                next.mode(),
                correction.contact().wetFraction(),
                relativeVerticalVelocity,
                correction.force(),
                correction.torque(),
                pitchError,
                rollError,
                Vec3.ZERO,
                profile.beam(),
                profile.length(),
                profile.parameters().mass()
            )
        );

        Vec3 force = reentry.force();
        if (next.mode() == VesselMotionController.Mode.DISPLACEMENT) {
            VesselWaveRidingDynamics.Result riding = VesselWaveRidingDynamics.resolve(
                new VesselWaveRidingDynamics.Input(
                    correction.contact().wetFraction(),
                    velocityPerSecond(boat),
                    correction.waveVelocity(),
                    forwardDirection(boat),
                    profile.beam(),
                    profile.length(),
                    profile.parameters().mass(),
                    profile.planingFactor()
                )
            );
            force = force.add(riding.force());
        }

        if (!next.allowDownwardWaterForce() && force.y() < 0.0) {
            force = new Vec3(force.x(), 0.0, force.z());
        }
        applyForceCorrection(boat, force, profile.parameters());
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
        OceanSurface.SurfaceSample centerWater = ocean.sample(
            boat.getX(),
            boat.getZ(),
            timeSeconds,
            dynamicConditions
        );
        Vec3 waveVelocity = centerWater.surfaceVelocity().subtract(dynamicConditions.current());

        return new Correction(
            dynamic.force().subtract(flat.force()),
            dynamic.torque().subtract(flat.torque()),
            dynamic.contact(),
            poseTarget,
            waveVelocity
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

    private static Vec3 velocityPerSecond(BoatEntity boat) {
        Vec3d velocity = boat.getVelocity();
        return new Vec3(velocity.x * 20.0, velocity.y * 20.0, velocity.z * 20.0);
    }

    private static double verticalVelocityPerSecond(BoatEntity boat) {
        return boat.getVelocity().y * 20.0;
    }

    private static double horizontalSpeedPerSecond(BoatEntity boat) {
        Vec3d velocity = boat.getVelocity();
        return Math.hypot(velocity.x, velocity.z) * 20.0;
    }

    private static Vec3 forwardDirection(BoatEntity boat) {
        double yawRadians = Math.toRadians(-boat.getYaw());
        return new Vec3(Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
    }

    private static VesselPhysics.State capture(BoatEntity boat) {
        double yawRadians = Math.toRadians(-boat.getYaw());
        return new VesselPhysics.State(
            VesselPhysics.Pose.uprightYaw(
                new Vec3(boat.getX(), boat.getY(), boat.getZ()),
                yawRadians
            ),
            velocityPerSecond(boat),
            Vec3.ZERO
        );
    }

    record Correction(
        Vec3 force,
        Vec3 torque,
        VesselPhysics.ContactState contact,
        OceanVesselPose.Angles poseTarget,
        Vec3 waveVelocity
    ) {
        static final Correction ZERO = new Correction(
            Vec3.ZERO,
            Vec3.ZERO,
            new VesselPhysics.ContactState(0.0, 0.0, Vec3.ZERO, 0.0, 0.0, 0.0, 0.0),
            new OceanVesselPose.Angles(0.0, 0.0),
            Vec3.ZERO
        );
    }
}
