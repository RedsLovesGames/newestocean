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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/** Draws bounded client-side wake trails after the ocean surface. */
public final class VesselWakeRenderer {
    private static final double SURFACE_LIFT = 0.03;

    private VesselWakeRenderer() {
    }

    public static void render(
        WorldRenderContext context,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        OceanQuality quality,
        ShaderCompatibility.Snapshot compatibility
    ) {
        if (context == null || camera == null || frame == null || quality == null || compatibility == null
            || context.matrixStack() == null) {
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

        if (compatibility.allowCustomShaders() && VesselWakeShader.available()) {
            drawGpu(camera, frame, trails);
        } else {
            drawCpu(context, camera, frame, trails, compatibility);
        }
    }

    private static void drawGpu(
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        List<VesselWakeTracker.Trail> trails
    ) {
        BufferBuilder builder = Tessellator.getInstance().begin(
            VertexFormat.DrawMode.QUADS,
            VertexFormats.POSITION_COLOR
        );
        boolean emitted = emitSegments(trails, (newer, older, segment) -> {
            emitGpuStrip(builder, camera, segment.leftArm(), newer.strength(), older.strength(), false);
            emitGpuStrip(builder, camera, segment.rightArm(), newer.strength(), older.strength(), false);
            emitGpuStrip(builder, camera, segment.center(), newer.strength(), older.strength(), true);
        });
        if (!emitted) {
            return;
        }

        VesselWakeShader.apply(
            NewestOcean.clientOcean(),
            frame.plan().visualWaveComponents(),
            frame.timeSeconds(),
            frame.conditions(),
            camera.x,
            camera.y,
            camera.z
        );
        drawPrepared(builder);
    }

    private static void drawCpu(
        WorldRenderContext context,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        List<VesselWakeTracker.Trail> trails,
        ShaderCompatibility.Snapshot compatibility
    ) {
        MatrixStack matrices = context.matrixStack();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(
            VertexFormat.DrawMode.QUADS,
            VertexFormats.POSITION_COLOR
        );
        int visualWaveComponents = compatibility.visualWaveComponents(frame.plan().visualWaveComponents());
        double strengthMultiplier = compatibility.wakeMultiplier();
        boolean emitted = emitSegments(trails, (newer, older, segment) -> {
            emitCpuStrip(builder, matrix, camera, frame, segment.leftArm(), newer.strength(), older.strength(), false,
                visualWaveComponents, strengthMultiplier);
            emitCpuStrip(builder, matrix, camera, frame, segment.rightArm(), newer.strength(), older.strength(), false,
                visualWaveComponents, strengthMultiplier);
            emitCpuStrip(builder, matrix, camera, frame, segment.center(), newer.strength(), older.strength(), true,
                visualWaveComponents, strengthMultiplier);
        });
        if (!emitted) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        drawPrepared(builder);
    }

    private static void drawPrepared(BufferBuilder builder) {
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

    private static boolean emitSegments(List<VesselWakeTracker.Trail> trails, SegmentConsumer consumer) {
        boolean emitted = false;
        for (VesselWakeTracker.Trail trail : trails) {
            List<VesselWakeDescriptor> samples = trail.samples();
            for (int i = 1; i < samples.size(); i++) {
                VesselWakeDescriptor older = samples.get(i - 1);
                VesselWakeDescriptor newer = samples.get(i);
                if (newer.strength() <= 0.0 && older.strength() <= 0.0) {
                    continue;
                }
                consumer.accept(newer, older, VesselWakeGeometry.segment(newer, older));
                emitted = true;
            }
        }
        return emitted;
    }

    private static void emitGpuStrip(
        VertexConsumer consumer,
        Vec3d camera,
        VesselWakeGeometry.Strip strip,
        double newerStrength,
        double olderStrength,
        boolean center
    ) {
        float centerFlag = center ? 1.0F : 0.0F;
        float innerEdge = center ? 0.88F : 1.0F;
        float outerEdge = center ? 0.88F : 0.05F;

        emitGpu(consumer, camera, strip.newerInner(), newerStrength, innerEdge, centerFlag);
        emitGpu(consumer, camera, strip.newerOuter(), newerStrength, outerEdge, centerFlag);
        emitGpu(consumer, camera, strip.olderOuter(), olderStrength, outerEdge, centerFlag);
        emitGpu(consumer, camera, strip.olderInner(), olderStrength, innerEdge, centerFlag);
    }

    private static void emitGpu(
        VertexConsumer consumer,
        Vec3d camera,
        VesselWakeGeometry.Point point,
        double strength,
        float edge,
        float center
    ) {
        OceanRenderCoordinates.Relative relative = OceanRenderCoordinates.relative(
            point.x(),
            camera.y + SURFACE_LIFT,
            point.z(),
            camera.x,
            camera.y,
            camera.z
        );
        consumer.vertex((float) relative.x(), (float) relative.y(), (float) relative.z())
            .color((float) clamp01(strength), edge, center, 1.0F);
    }

    private static void emitCpuStrip(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        VesselWakeGeometry.Strip strip,
        double newerStrength,
        double olderStrength,
        boolean center,
        int visualWaveComponents,
        double strengthMultiplier
    ) {
        float innerAlpha = center ? 0.78F : 0.88F;
        float outerAlpha = center ? 0.78F : 0.08F;

        emitCpu(consumer, matrix, camera, frame, strip.newerInner(), newerStrength, innerAlpha,
            visualWaveComponents, strengthMultiplier);
        emitCpu(consumer, matrix, camera, frame, strip.newerOuter(), newerStrength, outerAlpha,
            visualWaveComponents, strengthMultiplier);
        emitCpu(consumer, matrix, camera, frame, strip.olderOuter(), olderStrength, outerAlpha,
            visualWaveComponents, strengthMultiplier);
        emitCpu(consumer, matrix, camera, frame, strip.olderInner(), olderStrength, innerAlpha,
            visualWaveComponents, strengthMultiplier);
    }

    private static void emitCpu(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        VesselWakeGeometry.Point point,
        double strength,
        float edgeAlpha,
        int visualWaveComponents,
        double strengthMultiplier
    ) {
        OceanSurface.SurfaceSample sample = NewestOcean.clientOcean().sample(
            point.x(),
            point.z(),
            frame.timeSeconds(),
            frame.conditions(),
            visualWaveComponents
        );
        double worldX = point.x() + sample.horizontalDisplacement().x();
        double worldY = sample.height() + SURFACE_LIFT;
        double worldZ = point.z() + sample.horizontalDisplacement().z();
        OceanRenderCoordinates.Relative relative = OceanRenderCoordinates.relative(
            worldX,
            worldY,
            worldZ,
            camera.x,
            camera.y,
            camera.z
        );
        float alpha = (float) (clamp01(strength * strengthMultiplier) * edgeAlpha);
        consumer.vertex(matrix, (float) relative.x(), (float) relative.y(), (float) relative.z())
            .color(0.93F, 0.98F, 1.0F, alpha);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    @FunctionalInterface
    private interface SegmentConsumer {
        void accept(VesselWakeDescriptor newer, VesselWakeDescriptor older, VesselWakeGeometry.Segment segment);
    }
}
