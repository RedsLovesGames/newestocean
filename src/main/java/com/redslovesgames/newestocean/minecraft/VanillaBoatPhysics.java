package com.redslovesgames.newestocean.minecraft;

import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import com.redslovesgames.newestocean.physics.VesselPhysics;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Adds wave motion as a correction on top of vanilla boat buoyancy.
 *
 * <p>Vanilla already keeps boats afloat. Applying a second complete buoyancy model would double-count
 * that force, so this integration solves the vessel twice: once against the dynamic ocean and once
 * against a flat copy of the vanilla water surface. Only the difference is applied to the boat.</p>
 */
public final class VanillaBoatPhysics {
    private static final double TICK_SECONDS = 1.0 / 20.0;
    private static final double BLOCKS_PER_SECOND_TO_BLOCKS_PER_TICK = 1.0 / 20.0;

    private static final List<VesselPhysics.BuoyancyPoint> VANILLA_POINTS = List.of(
        new VesselPhysics.BuoyancyPoint(new Vec3(-0.55, 0.0, -0.85), 1.0),
        new VesselPhysics.BuoyancyPoint(new Vec3(0.55, 0.0, -0.85), 1.0),
        new VesselPhysics.BuoyancyPoint(new Vec3(-0.55, 0.0, 0.85), 1.0),
        new VesselPhysics.BuoyancyPoint(new Vec3(0.55, 0.0, 0.85), 1.0)
    );

    private static final VesselPhysics.Parameters PARAMETERS = VesselPhysics.Parameters.smallBoat();

    private VanillaBoatPhysics() {
    }

    public static void tick(BoatEntity boat) {
        World world = boat.getWorld();
        if (world.isClient() || !boat.isTouchingWater() || !isVanillaBoat(boat)) {
            return;
        }

        double baseWaterHeight = boat.getWaterHeightBelow();
        if (!Double.isFinite(baseWaterHeight)) {
            return;
        }

        double timeSeconds = world.getTime() * TICK_SECONDS;
        OceanConditions dynamicConditions = OceanConditions.weather(
            world.getRainGradient(1.0F),
            world.getThunderGradient(1.0F),
            baseWaterHeight,
            Vec3.ZERO
        );
        OceanConditions flatConditions = new OceanConditions(0.0, baseWaterHeight, Vec3.ZERO);

        VesselPhysics.State state = capture(boat);
        OceanSurface ocean = NewestOcean.ocean();

        VesselPhysics.Result dynamic = VesselPhysics.solve(
            ocean,
            dynamicConditions,
            timeSeconds,
            state,
            VANILLA_POINTS,
            PARAMETERS
        );
        VesselPhysics.Result flat = VesselPhysics.solve(
            ocean,
            flatConditions,
            timeSeconds,
            state,
            VANILLA_POINTS,
            PARAMETERS
        );

        Vec3 correctionForce = dynamic.force().subtract(flat.force());
        applyForceCorrection(boat, correctionForce);
    }

    private static boolean isVanillaBoat(BoatEntity boat) {
        Identifier id = Registries.ENTITY_TYPE.getId(boat.getType());
        return id != null && "minecraft".equals(id.getNamespace());
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

    private static void applyForceCorrection(BoatEntity boat, Vec3 force) {
        Vec3 acceleration = force.multiply(1.0 / PARAMETERS.mass());
        Vec3 velocityDeltaPerSecond = acceleration.multiply(TICK_SECONDS);
        Vec3 velocityDeltaPerTick = velocityDeltaPerSecond.multiply(BLOCKS_PER_SECOND_TO_BLOCKS_PER_TICK);

        Vec3d current = boat.getVelocity();
        boat.setVelocity(
            current.x + velocityDeltaPerTick.x(),
            current.y + velocityDeltaPerTick.y(),
            current.z + velocityDeltaPerTick.z()
        );
    }
}
