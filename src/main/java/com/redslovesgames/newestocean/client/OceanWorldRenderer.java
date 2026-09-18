package com.redslovesgames.newestocean.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.math.Vec3;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Visible ocean renderer. GPU displacement is preferred; the Phase 8 CPU path remains a safe fallback. */
public final class OceanWorldRenderer {
    private static final OceanLodTopology.Cache TOPOLOGY_CACHE = new OceanLodTopology.Cache();
    private static final int COVERAGE_REFRESH_TICKS = 100;
    private static final double TARGET_FRAME_MS = 1000.0 / 60.0;
    private static final AdaptiveQualityFrameSampler ADAPTIVE_QUALITY =
        new AdaptiveQualityFrameSampler(OceanQuality.MEDIUM, TARGET_FRAME_MS);

    private static volatile OceanQuality quality = OceanQuality.MEDIUM;
    private static volatile boolean adaptiveQualityEnabled = true;
    private static OceanLodCoverageMask coverage;
    private static OceanQuality coverageQuality;
    private static double coverageOriginX = Double.NaN;
    private static double coverageOriginZ = Double.NaN;
    private static String coverageDimension = "";
    private static long coverageBuiltAtTick = Long.MIN_VALUE;

    private OceanWorldRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(OceanWorldRenderer::render);
    }

    public static OceanQuality quality() {
        return quality;
    }

    public static boolean isAdaptiveQualityEnabled() {
        return adaptiveQualityEnabled;
    }

    public static void setAdaptiveQualityEnabled(boolean enabled) {
        adaptiveQualityEnabled = enabled;
        ADAPTIVE_QUALITY.forceQuality(quality);
    }

    public static void setQuality(OceanQuality newQuality) {
        if (newQuality == null) {
            throw new IllegalArgumentException("quality is required");
        }
        ADAPTIVE_QUALITY.forceQuality(newQuality);
        applyQuality(newQuality);
    }

    public static void reset() {
        resetCoverage();
        ADAPTIVE_QUALITY.reset();
    }

    private static void resetCoverage() {
        coverage = null;
        coverageQuality = null;
        coverageOriginX = Double.NaN;
        coverageOriginZ = Double.NaN;
        coverageDimension = "";
        coverageBuiltAtTick = Long.MIN_VALUE;
    }

    private static void applyQuality(OceanQuality newQuality) {
        if (quality != newQuality) {
            quality = newQuality;
            resetCoverage();
        }
    }

    private static void sampleAdaptiveQuality() {
        if (!adaptiveQualityEnabled) {
            return;
        }
        ADAPTIVE_QUALITY.recordTimestampNanos(System.nanoTime())
            .ifPresent(OceanWorldRenderer::applyQuality);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!NewestOceanClient.isOceanSynchronized() || client.world == null || context.matrixStack() == null) {
            ADAPTIVE_QUALITY.reset();
            return;
        }

        sampleAdaptiveQuality();

        Vec3d camera = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(true);
        float rainGradient = client.world.getRainGradient(tickDelta);
        float thunderGradient = client.world.getThunderGradient(tickDelta);
        OceanRenderFrame.Frame frame = OceanRenderFrame.prepare(
            true,
            quality,
            camera.x,
            camera.z,
            client.world.getTime(),
            tickDelta,
            client.world.getSeaLevel(),
            rainGradient,
            thunderGradient,
            NewestOcean.clientOceanSeed()
        ).orElseThrow();

        OceanLodTopology topology = TOPOLOGY_CACHE.get(frame.plan());
        OceanLodCoverageMask currentCoverage = coverageFor(client, frame.plan(), topology);
        int[] waterIndices = currentCoverage.waterIndices(topology);
        if (waterIndices.length == 0) {
            return;
        }

        if (OceanGpuShader.available()) {
            drawGpu(camera, frame, topology, waterIndices, rainGradient, thunderGradient);
        } else {
            OceanLodMeshGenerator.Mesh mesh = OceanLodMeshGenerator.generate(
                NewestOcean.clientOcean(),
                frame.plan(),
                topology,
                currentCoverage,
                frame.timeSeconds(),
                frame.conditions()
            );
            drawCpu(context, camera, mesh, frame, rainGradient, thunderGradient);
        }

        VesselWakeRenderer.render(context, camera, frame, quality);
    }

    private static OceanLodCoverageMask coverageFor(
        MinecraftClient client,
        OceanLodPlanner.Plan plan,
        OceanLodTopology topology
    ) {
        long worldTick = client.world.getTime();
        String dimension = client.world.getRegistryKey().getValue().toString();
        boolean moved = coverage == null
            || coverageQuality != plan.quality()
            || Double.compare(coverageOriginX, plan.originX()) != 0
            || Double.compare(coverageOriginZ, plan.originZ()) != 0
            || !coverageDimension.equals(dimension);
        boolean stale = coverage != null && Math.abs(worldTick - coverageBuiltAtTick) >= COVERAGE_REFRESH_TICKS;

        if (moved || stale) {
            int waterY = client.world.getSeaLevel() - 1;
            BlockPos.Mutable probePos = new BlockPos.Mutable();
            coverage = OceanLodCoverageMask.build(plan, topology, (x, z) -> {
                probePos.set(MathHelper.floor(x), waterY, MathHelper.floor(z));
                return client.world.getFluidState(probePos).isIn(FluidTags.WATER);
            });
            coverageQuality = plan.quality();
            coverageOriginX = plan.originX();
            coverageOriginZ = plan.originZ();
            coverageDimension = dimension;
            coverageBuiltAtTick = worldTick;
        }
        return coverage;
    }

    private static void drawGpu(
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        OceanLodTopology topology,
        int[] indices,
        float rainGradient,
        float thunderGradient
    ) {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        OceanLodTopology.LocalVertex[] localVertices = topology.vertices();
        OceanLodPlanner.Plan plan = frame.plan();

        for (int index = 0; index + 5 < indices.length; index += 6) {
            emitStatic(builder, localVertices[indices[index]], plan, camera);
            emitStatic(builder, localVertices[indices[index + 1]], plan, camera);
            emitStatic(builder, localVertices[indices[index + 5]], plan, camera);
            emitStatic(builder, localVertices[indices[index + 2]], plan, camera);
        }

        OceanGpuShader.apply(
            NewestOcean.clientOcean(),
            plan.visualWaveComponents(),
            frame.timeSeconds(),
            frame.conditions(),
            plan.quality(),
            rainGradient,
            thunderGradient,
            camera.x,
            camera.y,
            camera.z
        );
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

    private static void emitStatic(
        VertexConsumer consumer,
        OceanLodTopology.LocalVertex vertex,
        OceanLodPlanner.Plan plan,
        Vec3d camera
    ) {
        float x = (float) (plan.originX() + vertex.x() - camera.x);
        float z = (float) (plan.originZ() + vertex.z() - camera.z);
        consumer.vertex(x, 0.0F, z);
    }

    private static void drawCpu(
        WorldRenderContext context,
        Vec3d camera,
        OceanLodMeshGenerator.Mesh mesh,
        OceanRenderFrame.Frame frame,
        float rainGradient,
        float thunderGradient
    ) {
        MatrixStack matrices = context.matrixStack();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        OceanLodMeshGenerator.Vertex[] meshVertices = mesh.vertices();
        int[] indices = mesh.indices();
        for (int index = 0; index + 5 < indices.length; index += 6) {
            OceanLodMeshGenerator.Vertex topLeft = meshVertices[indices[index]];
            OceanLodMeshGenerator.Vertex bottomLeft = meshVertices[indices[index + 1]];
            OceanLodMeshGenerator.Vertex topRight = meshVertices[indices[index + 2]];
            OceanLodMeshGenerator.Vertex bottomRight = meshVertices[indices[index + 5]];

            Vec3 normal = topLeft.normal()
                .add(bottomLeft.normal())
                .add(topRight.normal())
                .add(bottomRight.normal())
                .multiply(0.25)
                .normalize();
            float light = (float) MathHelper.clamp(0.72 + normal.y() * 0.20, 0.68, 0.94);

            double slopeMagnitude = Math.hypot(normal.x(), normal.z()) / Math.max(0.05, normal.y());
            double averageHeight = (
                topLeft.y() + bottomLeft.y() + topRight.y() + bottomRight.y()
            ) * 0.25;
            double crestHeight = Math.max(0.0, averageHeight - frame.conditions().tideOffset());
            double crestCurvature = crestHeight * (0.12 + 0.38 * Math.min(1.5, slopeMagnitude));
            float foam = (float) OceanWhitecapModel.intensity(
                slopeMagnitude,
                crestCurvature,
                rainGradient,
                thunderGradient,
                frame.plan().quality(),
                1.0
            );

            float baseRed = 0.055F * light;
            float baseGreen = 0.34F * light;
            float baseBlue = 0.58F * light;
            float red = baseRed + (0.93F - baseRed) * foam;
            float green = baseGreen + (0.97F - baseGreen) * foam;
            float blue = baseBlue + (1.00F - baseBlue) * foam;
            float alpha = 0.72F + 0.16F * foam;

            emitCpu(builder, matrix, camera, topLeft, red, green, blue, alpha);
            emitCpu(builder, matrix, camera, bottomLeft, red, green, blue, alpha);
            emitCpu(builder, matrix, camera, bottomRight, red, green, blue, alpha);
            emitCpu(builder, matrix, camera, topRight, red, green, blue, alpha);
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

    private static void emitCpu(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vec3d camera,
        OceanLodMeshGenerator.Vertex vertex,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        OceanRenderCoordinates.Relative relative = OceanRenderCoordinates.relative(
            vertex.x(), vertex.y(), vertex.z(),
            camera.x, camera.y, camera.z
        );
        consumer.vertex(matrix, (float) relative.x(), (float) relative.y(), (float) relative.z())
            .color(red, green, blue, alpha);
    }
}
