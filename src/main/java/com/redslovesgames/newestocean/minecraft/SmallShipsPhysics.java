package com.redslovesgames.newestocean.minecraft;

import com.redslovesgames.newestocean.physics.AdaptiveHullProfile;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;

final class SmallShipsPhysics {
    private SmallShipsPhysics() {
    }

    static void tick(BoatEntity boat) {
        if (boat.getWorld().isClient() || !boat.isTouchingWater() || boat.isRemoved()) {
            return;
        }

        AdaptiveHullProfile.Profile profile = profileFor(boat);
        BoatPhysicsSupport.Correction correction = BoatPhysicsSupport.solveCorrection(boat, profile);
        BoatPhysicsSupport.applyForceCorrection(boat, correction.force(), profile.parameters());
    }

    private static AdaptiveHullProfile.Profile profileFor(BoatEntity boat) {
        Box box = boat.getBoundingBox();
        double spanX = Math.max(1.0, box.getLengthX());
        double spanZ = Math.max(1.0, box.getLengthZ());
        double beam = Math.min(spanX, spanZ);
        double length = Math.max(spanX, spanZ);
        return AdaptiveHullProfile.fromDimensions(beam, length);
    }
}
