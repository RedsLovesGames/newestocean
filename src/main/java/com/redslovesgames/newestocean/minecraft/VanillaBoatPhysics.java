package com.redslovesgames.newestocean.minecraft;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.physics.VesselPhysics;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;

/** Adds wave motion as a correction on top of vanilla boat buoyancy. */
public final class VanillaBoatPhysics {
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
        if (boat.getWorld().isClient() || !boat.isTouchingWater() || !isVanillaBoat(boat)) {
            return;
        }

        BoatPhysicsSupport.Correction correction = BoatPhysicsSupport.solveCorrection(
            boat,
            VANILLA_POINTS,
            PARAMETERS
        );
        BoatPhysicsSupport.applyForceCorrection(boat, correction.force(), PARAMETERS);
    }

    private static boolean isVanillaBoat(BoatEntity boat) {
        Identifier id = Registries.ENTITY_TYPE.getId(boat.getType());
        return id != null && "minecraft".equals(id.getNamespace());
    }
}
