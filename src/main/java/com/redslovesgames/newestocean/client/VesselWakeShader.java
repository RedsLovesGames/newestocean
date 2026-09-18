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

/** GPU wake shader. The CPU submits sparse wake strips while the GPU conforms them to the ocean. */
public final class VesselWakeShader {
    private static ShaderProgram program;

    private VesselWakeShader() {
    }

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(
            Identifier.of(NewestOcean.MOD_ID, "vessel_wake"),
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
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (program == null) {
            throw new IllegalStateException("vessel wake shader has not loaded");
        }

        OceanGpuWaveData data = OceanGpuWaveData.from(ocean, visualWaveComponents);
        RenderSystem.setShader(() -> program);
        set1("WakeTime", (float) timeSeconds);
        set1("WakeWaveScale", (float) conditions.waveScale());
        set1("WakeWaterHeight", (float) (conditions.tideOffset() - cameraY));
        set2("WakeCameraXZ", (float) cameraX, (float) cameraZ);
        set1("WakeWaveCount", data.activeWaveCount());

        for (int i = 0; i < OceanGpuWaveData.MAX_WAVES; i++) {
            OceanGpuWaveData.PackedWave wave = data.wave(i);
            set4("WakeWaveA" + i, wave.directionX(), wave.directionZ(), wave.amplitude(), wave.waveNumber());
            set4("WakeWaveB" + i, wave.phase(), wave.angularFrequency(), wave.steepness(), 0.0F);
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
