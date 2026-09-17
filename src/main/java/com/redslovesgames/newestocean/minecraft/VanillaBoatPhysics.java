package com.redslovesgames.newestocean.minecraft;

import com.redslovesgames.newestocean.physics.AdaptiveHullProfile;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

/** Adds wave motion as a correction on top of vanilla boat buoyancy. */
public final class VanillaBoatPhysics {
    private VanillaBoatPhysics() {
    }

    public static void tick(BoatEntity boat) {
        if (boat.getWorld().isClient() || !isVanillaBoat(boat)) {
            return;
        }

        AdaptiveHullProfile.Profile profile = profileFor(boat);
        BoatPhysicsSupport.tick(boat, profile);
    }

    private static AdaptiveHullProfile.Profile profileFor(BoatEntity boat) {
        Box box = boat.getBoundingBox();
        double spanX = Math.max(1.0, box.getLengthX());
        double spanZ = Math.max(1.0, box.getLengthZ());
        return AdaptiveHullProfile.fromDimensions(
            Math.min(spanX, spanZ),
            Math.max(spanX, spanZ)
        );
    }

    private static boolean isVanillaBoat(BoatEntity boat) {
        Identifier id = Registries.ENTITY_TYPE.getId(boat.getType());
        return id != null && "minecraft".equals(id.getNamespace());
    }
}
