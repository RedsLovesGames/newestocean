package com.redslovesgames.newestocean.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/** Draws the bounded client-only foam trails produced by {@link VesselWakeTracker}. */
public final class VesselWakeRenderer {
    private static final double SURFACE_OFFSET = 0.025;

    private VesselWakeRenderer() {
    }

    public static void render(
        WorldRenderContext context,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        OceanQuality quality
    ) {
        if (context == null || camera == null || frame == null || quality == null || context.matrixStack() == null) {
            return;
        }

        List<VesselWakeTracker.Trail> trails = VesselWakeTracker.snapshot(
            camera,
            quality,
            frame.timeSeconds()
        );
        if (trails.isEmpty()) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(
            VertexFormat.DrawMode.QUADS,
            VertexFormats.POSITION_COLOR
        );
        boolean emitted = false;

        for (VesselWakeTracker.Trail trail : trails) {
            List<VesselWakeDescriptor> samples = trail.samples();
            if (samples.size() < 2) {
                continue;
            }

            double[] heights = new double[samples.size()];
            for (int i = 0; i < samples.size(); i++) {
                VesselWakeDescriptor descriptor = samples.get(i);
                OceanSurface.SurfaceSample surface = NewestOcean.clientOcean().sample(
                    descriptor.x(),
                    descriptor.z(),
                    frame.timeSeconds(),
                    frame.conditions(),
                    frame.plan().visualWaveComponents()
                );
                heights[i] = surface.height() + SURFACE_OFFSET;
            }

            for (int i = 1; i < samples.size(); i++) {
                VesselWakeDescriptor older = samples.get(i - 1);
                VesselWakeDescriptor newer = samples.get(i);
                VesselWakeGeometry.Segment segment = VesselWakeGeometry.segment(newer, older);
                double newerY = heights[i];
                double olderY = heights[i - 1];
                float strength = (float) MathHelper.clamp(
                    (newer.strength() + older.strength()) * 0.5,
                    0.0,
                    1.0
                );
                float armAlpha = (float) MathHelper.clamp(0.10 + 0.55 * strength, 0.0, 0.70);
                float centerAlpha = armAlpha * 0.75F;

                emitStrip(builder, matrix, camera, segment.leftArm(), newerY, olderY, armAlpha);
                emitStrip(builder, matrix, camera, segment.rightArm(), newerY, olderY, armAlpha);
                emitStrip(builder, matrix, camera, segment.center(), newerY, olderY, centerAlpha);
                emitted = true;
            }
        }

        if (!emitted) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
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

    private static void emitStrip(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vec3d camera,
        VesselWakeGeometry.Strip strip,
        double newerY,
        double olderY,
        float alpha
    ) {
        emitPoint(consumer, matrix, camera, strip.newerInner(), newerY, alpha);
        emitPoint(consumer, matrix, camera, strip.newerOuter(), newerY, alpha);
        emitPoint(consumer, matrix, camera, strip.olderOuter(), olderY, alpha);
        emitPoint(consumer, matrix, camera, strip.olderInner(), olderY, alpha);
    }

    private static void emitPoint(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vec3d camera,
        VesselWakeGeometry.Point point,
        double y,
        float alpha
    ) {
        OceanRenderCoordinates.Relative relative = OceanRenderCoordinates.relative(
            point.x(),
            y,
            point.z(),
            camera.x,
            camera.y,
            camera.z
        );
        consumer.vertex(
            matrix,
            (float) relative.x(),
            (float) relative.y(),
            (float) relative.z()
        ).color(0.88F, 0.95F, 1.0F, alpha);
    }
}
