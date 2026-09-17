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

/** Owns the Phase 9 core shader and uploads the compact deterministic wave uniform payload. */
public final class OceanGpuShader {
    private static ShaderProgram program;

    private OceanGpuShader() {
    }

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(
            Identifier.of(NewestOcean.MOD_ID, "ocean_surface"),
            VertexFormats.POSITION,
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
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (program == null) {
            throw new IllegalStateException("ocean shader has not loaded");
        }

        OceanGpuWaveData data = OceanGpuWaveData.from(ocean, visualWaveComponents);
        RenderSystem.setShader(() -> program);

        set1("OceanTime", (float) timeSeconds);
        set1("OceanWaveScale", (float) conditions.waveScale());
        set1("OceanWaterHeight", (float) (conditions.tideOffset() - cameraY));
        set2("OceanCameraXZ", (float) cameraX, (float) cameraZ);
        set1("OceanWaveCount", data.activeWaveCount());

        for (int i = 0; i < OceanGpuWaveData.MAX_WAVES; i++) {
            OceanGpuWaveData.PackedWave wave = data.wave(i);
            set4("OceanWaveA" + i, wave.directionX(), wave.directionZ(), wave.amplitude(), wave.waveNumber());
            set4("OceanWaveB" + i, wave.phase(), wave.angularFrequency(), wave.steepness(), 0.0F);
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
