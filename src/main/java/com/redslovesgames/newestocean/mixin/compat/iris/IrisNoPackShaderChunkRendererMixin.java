package com.redslovesgames.newestocean.mixin.compat.iris;

import com.redslovesgames.newestocean.client.compat.iris.IrisWaterRuntimeBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Binds Newest Ocean uniforms to Sodium's ordinary chunk program when Iris has no active pack. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer", remap = false)
public abstract class IrisNoPackShaderChunkRendererMixin {
    @Inject(method = "begin", at = @At("RETURN"), remap = false)
    private void newestocean$bindNoPackWaterUniforms(CallbackInfo ci) {
        IrisWaterRuntimeBridge.bindNoPackSodiumProgram();
    }
}
