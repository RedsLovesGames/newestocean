package com.redslovesgames.newestocean.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/** GPU shoreline overlay. Terrain metrics stay cached on CPU while Gerstner animation runs here. */
public final class ShorelineGpuShader {
    private static ShaderProgram program;

    private ShorelineGpuShader() {
    }

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(
            Identifier.of(NewestOcean.MOD_ID, "shoreline_break"),
            VertexFormats.POSITION_COLOR,
            loaded -> program = loaded
        ));
    }

    public static boolean available() {
        return program != null;
    }

    public static void apply(
        ProceduralOcean ocean,
        int visualWaveComponents,
        double timeSeconds,
        OceanConditions conditions,
        OceanQuality quality,
        double rainGradient,
        double thunderGradient,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (program == null) {
            throw new IllegalStateException("shoreline shader has not loaded");
        }
        if (quality == null) {
            throw new IllegalArgumentException("quality is required");
        }

        OceanGpuWaveData data = OceanGpuWaveData.from(ocean, visualWaveComponents);
        RenderSystem.setShader(() -> program);
        set1("ShorelineTime", (float) timeSeconds);
        set1("ShorelineWaveScale", (float) conditions.waveScale());
        set1("ShorelineWaterHeight", (float) (conditions.tideOffset() - cameraY));
        set2("ShorelineCameraXZ", (float) cameraX, (float) cameraZ);
        set1("ShorelineWaveCount", data.activeWaveCount());
        set1("ShorelineStormStrength", (float) OceanWhitecapModel.stormStrength(rainGradient, thunderGradient));
        set1("ShorelineQuality", (float) ShorelineBreakModel.qualityScale(quality));

        for (int i = 0; i < OceanGpuWaveData.MAX_WAVES; i++) {
            OceanGpuWaveData.PackedWave wave = data.wave(i);
            set4("ShorelineWaveA" + i, wave.directionX(), wave.directionZ(), wave.amplitude(), wave.waveNumber());
            set4("ShorelineWaveB" + i, wave.phase(), wave.angularFrequency(), wave.steepness(), 0.0F);
        }
    }

    private static void set1(String name, float value) {
        GlUniform uniform = program.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void set2(String name, float x, float y) {
        GlUniform uniform = program.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private static void set4(String name, float x, float y, float z, float w) {
        GlUniform uniform = program.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z, w);
        }
    }
}
