package com.redslovesgames.newestocean.mixin.compat.sodium;

import com.redslovesgames.newestocean.client.compat.sodium.SodiumWaterRuntimeBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Binds Newest Ocean uniforms after Sodium 0.8.12 binds each chunk shader program. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer", remap = false)
public abstract class SodiumShaderChunkRendererMixin {
    @Inject(method = "begin", at = @At("RETURN"), remap = false)
    private void newestocean$bindRealWaterUniforms(CallbackInfo ci) {
        SodiumWaterRuntimeBridge.bindCurrentProgram();
    }
}
