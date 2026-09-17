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
import com.redslovesgames.newestocean.physics.VesselPhysicsLod;
import com.redslovesgames.newestocean.physics.VesselReentryDynamics;
import com.redslovesgames.newestocean.physics.VesselWaveRidingDynamics;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

final class BoatPhysicsSupport {
    private static final double TICK_SECONDS = 1.0 / 20.0;
    private static final double BLOCKS_PER_SECOND_TO_BLOCKS_PER_TICK = 1.0 / 20.0;
    private static final Map<BoatEntity, VesselMotionController.State> MOTION_STATES = new WeakHashMap<>();
    private static final Map<BoatEntity, LodRuntime> LOD_STATES = new WeakHashMap<>();

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
        LodRuntime lod = LOD_STATES.computeIfAbsent(boat, ignored -> new LodRuntime());

        if (!boat.isTouchingWater()) {
            VesselMotionController.State next = VesselMotionController.advance(
                previous,
                new VesselMotionController.Input(0.0, verticalVelocityPerSecond(boat), 0.0, horizontalSpeedPerSecond(boat))
            );
            MOTION_STATES.put(boat, next);
            lod.force.snap(Vec3.ZERO);
            lod.hasSample = false;
            return;
        }

        int solveInterval = VesselPhysicsLod.updateIntervalTicks(
            nearestPlayerDistance(boat),
            hasPlayerPassenger(boat),
            previous.mode()
        );
        long worldTick = boat.getWorld().getTime();
        boolean solveNow = !lod.hasSample
            || VesselPhysicsLod.isSolveTick(worldTick, boat.getId(), solveInterval);

        if (!solveNow) {
            MOTION_STATES.put(
                boat,
                new VesselMotionController.State(previous.mode(), previous.ticksInMode() + 1)
            );
            if (previous.allowWaterForces()) {
                applyForceCorrection(boat, lod.force.next(), profile.parameters());
            }
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
        lod.hasSample = true;

        if (!next.allowWaterForces()) {
            lod.force.snap(Vec3.ZERO);
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
                    correction.surfaceNormal(),
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

        int interpolationTicks = next.mode() == VesselMotionController.Mode.DISPLACEMENT
            ? solveInterval
            : 1;
        lod.force.retarget(force, interpolationTicks);
        applyForceCorrection(boat, lod.force.next(), profile.parameters());
    }

    static Correction solveCorrection(BoatEntity boat, AdaptiveHullProfile.Profile profile) {
        double baseWaterHeight = boat.getWaterHeightBelow();
        if (!Double.isFinite(baseWaterHeight)) {
            return Correction.ZERO;
        }

        World world = boat.getWorld();
        double timeSeconds = world.getTime() * TICK_SECONDS;
        OceanConditions dynamicConditions = OceanEnvironment.conditions(
            NewestOcean.serverOceanSeed(),
            baseWaterHeight,
            timeSeconds,
            world.getRainGradient(1.0F),
            world.getThunderGradient(1.0F)
        );
        OceanConditions flatConditions = new OceanConditions(0.0, baseWaterHeight, Vec3.ZERO);
        VesselPhysics.State state = capture(boat);
        OceanSurface ocean = NewestOcean.serverOcean();
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
            waveVelocity,
            centerWater.normal()
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

    private static double nearestPlayerDistance(BoatEntity boat) {
        if (!(boat.getWorld() instanceof ServerWorld world)) {
            return Double.POSITIVE_INFINITY;
        }

        double nearestSquared = Double.POSITIVE_INFINITY;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }
            double dx = player.getX() - boat.getX();
            double dy = player.getY() - boat.getY();
            double dz = player.getZ() - boat.getZ();
            nearestSquared = Math.min(nearestSquared, dx * dx + dy * dy + dz * dz);
        }
        return Math.sqrt(nearestSquared);
    }

    private static boolean hasPlayerPassenger(BoatEntity boat) {
        for (var passenger : boat.getPassengerList()) {
            if (passenger instanceof PlayerEntity) {
                return true;
            }
        }
        return false;
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

    private static final class LodRuntime {
        private final VesselPhysicsLod.ForceInterpolator force = new VesselPhysicsLod.ForceInterpolator();
        private boolean hasSample;
    }

    record Correction(
        Vec3 force,
        Vec3 torque,
        VesselPhysics.ContactState contact,
        OceanVesselPose.Angles poseTarget,
        Vec3 waveVelocity,
        Vec3 surfaceNormal
    ) {
        static final Correction ZERO = new Correction(
            Vec3.ZERO,
            Vec3.ZERO,
            new VesselPhysics.ContactState(0.0, 0.0, Vec3.ZERO, 0.0, 0.0, 0.0, 0.0),
            new OceanVesselPose.Angles(0.0, 0.0),
            Vec3.ZERO,
            Vec3.UP
        );
    }
}
