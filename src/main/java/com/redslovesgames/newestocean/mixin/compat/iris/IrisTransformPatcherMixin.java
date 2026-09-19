package com.redslovesgames.newestocean.mixin.compat.iris;

import com.redslovesgames.newestocean.client.compat.iris.IrisWaterRuntimeBridge;
import com.redslovesgames.newestocean.client.compat.iris.IrisWaterShaderBridge;
import com.redslovesgames.newestocean.client.water.OceanShaderLibrary;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.EnumMap;
import java.util.Map;

/**
 * Patches Iris 1.8.14-beta.1's post-TransformPatcher Sodium source immediately before GlShader
 * compilation. This exact seam is SodiumPrograms.<init> -> createGlShaders(passName, transformed).
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.programs.SodiumPrograms", remap = false)
public abstract class IrisTransformPatcherMixin {
    private static final String CREATE_GL_SHADERS =
        "Lnet/irisshaders/iris/pipeline/programs/SodiumPrograms;createGlShaders(Ljava/lang/String;Ljava/util/Map;)Ljava/util/Map;";

    @ModifyArgs(
        method = "<init>",
        at = @At(value = "INVOKE", target = CREATE_GL_SHADERS),
        remap = false
    )
    private void newestocean$patchPostTransformWaterSources(Args args) {
        String passName = args.get(0);
        @SuppressWarnings("unchecked")
        Map<PatchShaderType, String> original = args.get(1);
        String logicalProgram = IrisWaterShaderBridge.logicalProgramForPass(passName);
        if (logicalProgram == null || original == null) {
            return;
        }

        Map<PatchShaderType, String> patched = new EnumMap<>(PatchShaderType.class);
        patched.putAll(original);

        String vertex = original.get(PatchShaderType.VERTEX);
        String patchedVertex = vertex == null
            ? null
            : IrisWaterShaderBridge.patchVertexSource(logicalProgram, vertex);
        if (patchedVertex != null) {
            patched.put(PatchShaderType.VERTEX, patchedVertex);
        }

        if (IrisWaterShaderBridge.WATER_PROGRAM.equals(logicalProgram)) {
            String fragment = original.get(PatchShaderType.FRAGMENT);
            if (fragment != null) {
                patched.put(
                    PatchShaderType.FRAGMENT,
                    IrisWaterShaderBridge.patchFragmentSource(logicalProgram, fragment)
                );
            }
        }

        boolean success = patchedVertex != null && patchedVertex.contains(OceanShaderLibrary.MARKER);
        IrisWaterRuntimeBridge.ProgramKind kind = "shadow_trans".equals(passName)
            ? IrisWaterRuntimeBridge.ProgramKind.SHADOW_WATER
            : IrisWaterRuntimeBridge.ProgramKind.WATER;
        IrisWaterRuntimeBridge.noteSourcePatch(
            kind,
            success,
            success ? "" : "Pinned Iris transformed " + passName + " vertex source did not match the expected Sodium seam"
        );
        if (success) {
            args.set(1, patched);
        }
    }
}
