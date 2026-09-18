package com.redslovesgames.newestocean.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/** Shared render-state boundaries for Newest Ocean translucent passes. */
final class OceanRenderState {
    private OceanRenderState() {
    }

    static void drawSurface(BufferBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("builder is required");
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        try {
            BufferRenderer.drawWithGlobalProgram(builder.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    static void drawSurface(BufferBuilder builder, Matrix4f cameraMatrix) {
        withCameraModelView(cameraMatrix, () -> drawSurface(builder));
    }

    static void drawTwoSided(BufferBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("builder is required");
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        try {
            BufferRenderer.drawWithGlobalProgram(builder.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    static void drawTwoSided(BufferBuilder builder, Matrix4f cameraMatrix) {
        withCameraModelView(cameraMatrix, () -> drawTwoSided(builder));
    }

    private static void withCameraModelView(Matrix4f cameraMatrix, Runnable draw) {
        if (cameraMatrix == null || draw == null) {
            throw new IllegalArgumentException("camera matrix and draw action are required");
        }

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(cameraMatrix);
        RenderSystem.applyModelViewMatrix();
        try {
            draw.run();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }
}
