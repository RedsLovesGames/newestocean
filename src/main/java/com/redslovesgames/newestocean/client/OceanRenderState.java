package com.redslovesgames.newestocean.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;

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
}
