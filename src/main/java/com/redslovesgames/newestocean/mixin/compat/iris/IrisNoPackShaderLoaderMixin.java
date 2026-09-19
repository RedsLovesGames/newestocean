package com.redslovesgames.newestocean.mixin.compat.iris;

import com.redslovesgames.newestocean.client.compat.iris.IrisWaterRuntimeBridge;
import com.redslovesgames.newestocean.client.compat.sodium.SodiumWaterShaderBridge;
import com.redslovesgames.newestocean.client.water.OceanShaderLibrary;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reuses the pinned Sodium 0.8.12 water source patch only when Iris is installed but no shaderpack
 * is active. Iris shaderpack rendering is owned by IrisTransformPatcherMixin instead.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class IrisNoPackShaderLoaderMixin {
    private static final String TARGET_NAMESPACE = "sodium";
    private static final String TARGET_PATH = "blocks/block_layer_opaque.vsh";

    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, remap = false)
    private static void newestocean$patchNoPackWaterSource(
        Identifier name,
        CallbackInfoReturnable<String> cir
    ) {
        if (!TARGET_NAMESPACE.equals(name.getNamespace()) || !TARGET_PATH.equals(name.getPath())) {
            return;
        }
        if (!IrisWaterRuntimeBridge.shouldUseNoPackFallback(true, IrisWaterRuntimeBridge.shaderPackInUse())) {
            return;
        }

        String patched = SodiumWaterShaderBridge.patchShaderSource(cir.getReturnValue());
        boolean success = patched.contains(OceanShaderLibrary.MARKER);
        IrisWaterRuntimeBridge.noteNoPackSourcePatch(
            success,
            success ? "" : "Iris no-pack Sodium 0.8.12 water source did not contain the expected patch anchor"
        );
        if (success) {
            cir.setReturnValue(patched);
        }
    }
}
