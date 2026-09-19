package com.redslovesgames.newestocean.mixin.compat.sodium;

import com.redslovesgames.newestocean.client.compat.sodium.SodiumWaterRuntimeBridge;
import com.redslovesgames.newestocean.client.compat.sodium.SodiumWaterShaderBridge;
import com.redslovesgames.newestocean.client.water.OceanShaderLibrary;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Patches Sodium 0.8.12's exact chunk vertex shader source before preprocessing/compilation. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class SodiumShaderLoaderMixin {
    private static final String TARGET_NAMESPACE = "sodium";
    private static final String TARGET_PATH = "blocks/block_layer_opaque.vsh";

    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, remap = false)
    private static void newestocean$patchChunkVertex(
        Identifier name,
        CallbackInfoReturnable<String> cir
    ) {
        if (!TARGET_NAMESPACE.equals(name.getNamespace()) || !TARGET_PATH.equals(name.getPath())) {
            return;
        }

        String patched = SodiumWaterShaderBridge.patchShaderSource(cir.getReturnValue());
        boolean success = patched.contains(OceanShaderLibrary.MARKER);
        SodiumWaterRuntimeBridge.noteSourcePatch(
            success,
            success ? "" : "Sodium 0.8.12 block_layer_opaque.vsh did not contain the expected patch anchor"
        );
        if (success) {
            cir.setReturnValue(patched);
        }
    }
}
