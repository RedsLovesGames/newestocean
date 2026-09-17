package com.redslovesgames.newestocean.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.util.Optional;

/** Minimal Phase 8 translucent position/color layer. Phase 9 replaces this with the ocean shader. */
public final class OceanRenderLayer extends RenderLayer {
    public static final OceanRenderLayer INSTANCE = new OceanRenderLayer();

    private OceanRenderLayer() {
        super(
            "newestocean_surface",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            262_144,
            false,
            true,
            () -> {
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
            },
            () -> {
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
            }
        );
    }

    @Override
    public Optional<RenderLayer> getAffectedOutline() {
        return Optional.empty();
    }

    @Override
    public boolean isOutline() {
        return false;
    }
}
