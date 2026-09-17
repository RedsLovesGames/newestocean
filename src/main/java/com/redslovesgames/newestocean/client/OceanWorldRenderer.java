package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.math.Vec3;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** First visible CPU ocean renderer. Phase 9 moves displacement and normals to the GPU. */
public final class OceanWorldRenderer {
    private static final OceanLodTopology.Cache TOPOLOGY_CACHE = new OceanLodTopology.Cache();
    private static final int COVERAGE_REFRESH_TICKS = 100;

    private static volatile OceanQuality quality = OceanQuality.MEDIUM;
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

    public static void setQuality(OceanQuality newQuality) {
        if (newQuality == null) {
            throw new IllegalArgumentException("quality is required");
        }
        quality = newQuality;
    }

    public static void reset() {
        coverage = null;
        coverageQuality = null;
        coverageOriginX = Double.NaN;
        coverageOriginZ = Double.NaN;
        coverageDimension = "";
        coverageBuiltAtTick = Long.MIN_VALUE;
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!NewestOceanClient.isOceanSynchronized() || client.world == null || context.matrixStack() == null) {
            return;
        }

        Vec3d camera = context.camera().getPos();
        float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
        OceanRenderFrame.Frame frame = OceanRenderFrame.prepare(
            true,
            quality,
            camera.x,
            camera.z,
            client.world.getTime(),
            tickDelta,
            client.world.getSeaLevel(),
            client.world.getRainGradient(tickDelta),
            client.world.getThunderGradient(tickDelta),
            NewestOcean.clientOceanSeed()
        ).orElseThrow();

        OceanLodTopology topology = TOPOLOGY_CACHE.get(frame.plan());
        OceanLodCoverageMask currentCoverage = coverageFor(client, frame.plan(), topology);
        OceanLodMeshGenerator.Mesh mesh = OceanLodMeshGenerator.generate(
            NewestOcean.clientOcean(),
            frame.plan(),
            topology,
            currentCoverage,
            frame.timeSeconds(),
            frame.conditions()
        );
        if (mesh.indices().length == 0) {
            return;
        }

        draw(context, camera, mesh);
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

    private static void draw(WorldRenderContext context, Vec3d camera, OceanLodMeshGenerator.Mesh mesh) {
        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = context.consumers().getBuffer(RenderLayer.getDebugQuads());

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
                .multiply(0.25);
            float light = (float) MathHelper.clamp(0.72 + normal.y() * 0.20, 0.68, 0.94);
            float red = 0.055F * light;
            float green = 0.34F * light;
            float blue = 0.58F * light;
            float alpha = 0.72F;

            emit(vertices, matrix, topLeft, red, green, blue, alpha);
            emit(vertices, matrix, bottomLeft, red, green, blue, alpha);
            emit(vertices, matrix, bottomRight, red, green, blue, alpha);
            emit(vertices, matrix, topRight, red, green, blue, alpha);
        }

        matrices.pop();
    }

    private static void emit(
        VertexConsumer consumer,
        Matrix4f matrix,
        OceanLodMeshGenerator.Vertex vertex,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        consumer.vertex(matrix, (float) vertex.x(), (float) vertex.y(), (float) vertex.z())
            .color(red, green, blue, alpha);
    }
}
