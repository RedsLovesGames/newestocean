package com.redslovesgames.newestocean.mixin.client;

import com.redslovesgames.newestocean.client.water.OceanShaderLibrary;
import com.redslovesgames.newestocean.client.water.VanillaWaterRuntimeBridge;
import com.redslovesgames.newestocean.client.water.VanillaWaterShaderBridge;
import net.minecraft.client.gl.GlImportProcessor;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Vanilla 1.21.1 real-water hooks.
 *
 * <p>Mapped targets verified against Yarn 1.21.1+build.3:</p>
 * <ul>
 *   <li>ShaderProgram.loadShader(ResourceFactory, ShaderStage.Type, String) -> ShaderStage</li>
 *   <li>ShaderStage.createFromResource(Type, String, InputStream, String, GlImportProcessor) -> ShaderStage</li>
 *   <li>ShaderProgram.bind() -> void</li>
 * </ul>
 */
@Mixin(ShaderProgram.class)
public abstract class ShaderProgramMixin {
    private static final String CREATE_STAGE_SELECTOR =
        "Lnet/minecraft/client/gl/ShaderStage;createFromResource(Lnet/minecraft/client/gl/ShaderStage$Type;Ljava/lang/String;Ljava/io/InputStream;Ljava/lang/String;Lnet/minecraft/client/gl/GlImportProcessor;)Lnet/minecraft/client/gl/ShaderStage;";

    @Redirect(
        method = "loadShader",
        at = @At(value = "INVOKE", target = CREATE_STAGE_SELECTOR)
    )
    private static ShaderStage newestocean$patchVanillaWaterSource(
        ShaderStage.Type type,
        String name,
        InputStream stream,
        String domain,
        GlImportProcessor loader
    ) throws IOException {
        String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        VanillaWaterShaderBridge.Stage stage = type == ShaderStage.Type.VERTEX
            ? VanillaWaterShaderBridge.Stage.VERTEX
            : VanillaWaterShaderBridge.Stage.FRAGMENT;
        String patched = VanillaWaterShaderBridge.patchShaderSource(stage, name, source);

        if (VanillaWaterShaderBridge.targets(stage, name)) {
            boolean success = patched.contains(OceanShaderLibrary.MARKER);
            VanillaWaterRuntimeBridge.noteSourcePatch(
                success,
                success ? "" : "Vanilla rendertype_translucent vertex shader did not contain the expected 1.21.1 patch anchor"
            );
        }

        return ShaderStage.createFromResource(
            type,
            name,
            new ByteArrayInputStream(patched.getBytes(StandardCharsets.UTF_8)),
            domain,
            loader
        );
    }

    @Inject(method = "bind", at = @At("RETURN"))
    private void newestocean$bindRealWaterUniforms(CallbackInfo ci) {
        VanillaWaterRuntimeBridge.bind((ShaderProgram) (Object) this);
    }
}
