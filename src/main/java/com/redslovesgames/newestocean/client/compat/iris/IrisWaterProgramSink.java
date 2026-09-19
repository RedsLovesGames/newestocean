package com.redslovesgames.newestocean.client.compat.iris;

import com.mojang.blaze3d.systems.RenderSystem;
import com.redslovesgames.newestocean.client.water.OceanShaderLibrary;
import com.redslovesgames.newestocean.client.water.OceanWaterUniformBinder;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/** Direct uniform/sampler sink for Iris's already-bound Sodium-compatible water programs. */
final class IrisWaterProgramSink implements OceanWaterUniformBinder.Sink {
    private static final int SHORE_TEXTURE_UNIT = 11;

    private final int programId;
    private boolean missingUniform;

    IrisWaterProgramSink(int programId) {
        if (programId <= 0) {
            throw new IllegalArgumentException("Iris shader program id must be positive");
        }
        this.programId = programId;
    }

    @Override
    public void setInt(String name, int value) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    @Override
    public void setFloat(String name, float value) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    @Override
    public void setVec2(String name, float x, float y) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform2f(location, x, y);
        }
    }

    @Override
    public void setVec3(String name, float x, float y, float z) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform3f(location, x, y, z);
        }
    }

    @Override
    public void setVec4(String name, float x, float y, float z, float w) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform4f(location, x, y, z, w);
        }
    }

    @Override
    public void bindSampler(String name, Object handle) {
        int location = location(name);
        if (location < 0) {
            return;
        }
        if (!(handle instanceof Integer textureId) || textureId <= 0) {
            throw new IllegalArgumentException("shore sampler handle must be a positive OpenGL texture id");
        }

        int previousTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + SHORE_TEXTURE_UNIT);
        RenderSystem.bindTexture(textureId);
        GL20.glUniform1i(location, SHORE_TEXTURE_UNIT);
        RenderSystem.activeTexture(previousTexture);
    }

    boolean complete() {
        return !missingUniform;
    }

    private int location(String name) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (OceanShaderLibrary.missingUniformBreaksDisplacement(name, location)) {
            missingUniform = true;
        }
        return location;
    }
}
