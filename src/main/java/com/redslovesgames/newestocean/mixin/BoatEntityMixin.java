package com.redslovesgames.newestocean.mixin;

import com.redslovesgames.newestocean.minecraft.VanillaBoatPhysics;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoatEntity.class)
abstract class BoatEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void newestocean$applyOceanPhysics(CallbackInfo ci) {
        VanillaBoatPhysics.tick((BoatEntity) (Object) this);
    }
}
