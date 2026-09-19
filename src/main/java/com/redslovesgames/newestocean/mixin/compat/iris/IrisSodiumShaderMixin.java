package com.redslovesgames.newestocean.mixin.compat.iris;

import com.google.common.collect.ImmutableSet;
import com.redslovesgames.newestocean.client.compat.iris.IrisWaterRuntimeBridge;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.SodiumPrograms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

/** Binds Newest Ocean data to Iris's water and shadow-water SodiumShader programs. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.programs.SodiumShader", remap = false)
public abstract class IrisSodiumShaderMixin {
    @Unique
    private IrisWaterRuntimeBridge.ProgramKind newestocean$programKind;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void newestocean$capturePass(
        IrisRenderingPipeline pipeline,
        SodiumPrograms.Pass pass,
        ShaderBindingContext context,
        int handle,
        BlendModeOverride blendModeOverride,
        List<BufferBlendOverride> bufferBlendOverrides,
        CustomUniforms customUniforms,
        Supplier<ImmutableSet<Integer>> flipState,
        float alphaTest,
        boolean containsTessellation,
        CallbackInfo ci
    ) {
        if (pass == SodiumPrograms.Pass.TRANSLUCENT) {
            newestocean$programKind = IrisWaterRuntimeBridge.ProgramKind.WATER;
        } else if (pass == SodiumPrograms.Pass.SHADOW_TRANS) {
            newestocean$programKind = IrisWaterRuntimeBridge.ProgramKind.SHADOW_WATER;
        }
    }

    @Inject(method = "setupState", at = @At("RETURN"), remap = false)
    private void newestocean$bindRealWaterUniforms(CallbackInfo ci) {
        if (newestocean$programKind != null) {
            IrisWaterRuntimeBridge.bindShaderPackProgram(newestocean$programKind);
        }
    }
}
