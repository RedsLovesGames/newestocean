package com.redslovesgames.newestocean.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.client.config.OceanClientConfig;
import com.redslovesgames.newestocean.client.config.OceanConfigManager;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import com.redslovesgames.newestocean.ocean.WaveComponent;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Sparse visual shoreline overlay backed by cached terrain metrics. */
public final class ShorelineRenderer {
    private static final double SURFACE_LIFT = 0.035;
    private static final double MIN_CPU_ALPHA = 0.025;

    private ShorelineRenderer() {
    }

    public static void render(
        WorldRenderContext context,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        OceanLodTopology topology,
        ShorelineField shoreline,
        float rainGradient,
        float thunderGradient,
        ShaderCompatibility.Snapshot compatibility
    ) {
        render(context, camera, frame, topology, shoreline, rainGradient, thunderGradient,
            compatibility, OceanConfigManager.current());
    }

    public static void render(
        WorldRenderContext context,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        OceanLodTopology topology,
        ShorelineField shoreline,
        float rainGradient,
        float thunderGradient,
        ShaderCompatibility.Snapshot compatibility,
        OceanClientConfig config
    ) {
        if (context == null || camera == null || frame == null || topology == null || shoreline == null
            || compatibility == null || config == null || context.matrixStack() == null
            || shoreline.cellCount() != topology.cellCount() || shoreline.influencedCellCount() == 0
            || !config.shorelineEnabled()) {
            return;
        }

        if (compatibility.allowCustomShaders() && ShorelineGpuShader.available()) {
            drawGpu(context, camera, frame, topology, shoreline, rainGradient, thunderGradient, config);
        } else {
            drawCpu(context, camera, frame, topology, shoreline, rainGradient, thunderGradient, compatibility, config);
        }
    }

    private static void drawGpu(
        WorldRenderContext context,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        OceanLodTopology topology,
        ShorelineField shoreline,
        float rainGradient,
        float thunderGradient,
        OceanClientConfig config
    ) {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        OceanLodTopology.LocalVertex[] vertices = topology.vertices();
        boolean emitted = false;
        for (int cell = 0; cell < topology.cellCount(); cell++) {
            ShorelineSample sample = shoreline.sample(cell);
            if (sample.shoreInfluence() <= 0.0) continue;
            int source = cell * 6;
            emitGpu(builder, camera, frame.plan(), vertices[topology.indexAt(source)], sample, config.shorelineIntensity());
            emitGpu(builder, camera, frame.plan(), vertices[topology.indexAt(source + 1)], sample, config.shorelineIntensity());
            emitGpu(builder, camera, frame.plan(), vertices[topology.indexAt(source + 5)], sample, config.shorelineIntensity());
            emitGpu(builder, camera, frame.plan(), vertices[topology.indexAt(source + 2)], sample, config.shorelineIntensity());
            emitted = true;
        }
        if (!emitted) return;

        int visualWaves = config.effectiveVisualWaveComponents(frame.plan().visualWaveComponents());
        ShorelineGpuShader.apply(
            NewestOcean.clientOcean(), visualWaves, frame.timeSeconds(), frame.conditions(), frame.plan().quality(),
            rainGradient, thunderGradient, camera.x, camera.y, camera.z
        );
        Matrix4f cameraMatrix = context.matrixStack().peek().getPositionMatrix();
        OceanRenderState.drawTwoSided(builder, cameraMatrix);
    }

    private static void emitGpu(
        VertexConsumer consumer,
        Vec3d camera,
        OceanLodPlanner.Plan plan,
        OceanLodTopology.LocalVertex vertex,
        ShorelineSample sample,
        double shorelineIntensity
    ) {
        double worldX = plan.originX() + vertex.x();
        double worldZ = plan.originZ() + vertex.z();
        OceanRenderCoordinates.Relative relative = OceanRenderCoordinates.relative(
            worldX, camera.y + SURFACE_LIFT, worldZ, camera.x, camera.y, camera.z
        );
        float encodedDirectionX = (float) (sample.shoreDirectionX() * 0.5 + 0.5);
        float encodedDirectionZ = (float) (sample.shoreDirectionZ() * 0.5 + 0.5);
        float influence = (float) clamp01(sample.shoreInfluence() * shorelineIntensity);
        float shallow = (float) shallowFactor(sample.depthBlocks());
        consumer.vertex((float) relative.x(), (float) relative.y(), (float) relative.z())
            .color(encodedDirectionX, encodedDirectionZ, influence, shallow);
    }

    private static void drawCpu(
        WorldRenderContext context,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        OceanLodTopology topology,
        ShorelineField shoreline,
        float rainGradient,
        float thunderGradient,
        ShaderCompatibility.Snapshot compatibility,
        OceanClientConfig config
    ) {
        MatrixStack matrices = context.matrixStack();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        OceanLodTopology.LocalVertex[] vertices = topology.vertices();
        ProceduralOcean ocean = NewestOcean.clientOcean();
        double storm = OceanWhitecapModel.stormStrength(rainGradient, thunderGradient);
        int requestedWaves = config.effectiveVisualWaveComponents(frame.plan().visualWaveComponents());
        int visualWaveComponents = compatibility.visualWaveComponents(requestedWaves);
        double shorelineMultiplier = compatibility.shorelineMultiplier() * config.shorelineIntensity();
        boolean emitted = false;

        for (int cell = 0; cell < topology.cellCount(); cell++) {
            ShorelineSample shore = shoreline.sample(cell);
            if (shore.shoreInfluence() <= 0.0) continue;

            BreakInputs breakInputs = breakInputs(ocean, visualWaveComponents, shore);
            double strength = ShorelineBreakModel.breakerIntensity(
                shore, breakInputs.incomingAlignment(), breakInputs.waveEnergy(), storm, frame.plan().quality(), 1.0
            ) * shorelineMultiplier;
            if (strength < MIN_CPU_ALPHA) continue;

            float alpha = (float) Math.min(0.78, strength * (0.70 + 0.30 * shallowFactor(shore.depthBlocks())));
            int source = cell * 6;
            emitCpu(builder, matrix, camera, frame, vertices[topology.indexAt(source)], alpha, visualWaveComponents);
            emitCpu(builder, matrix, camera, frame, vertices[topology.indexAt(source + 1)], alpha, visualWaveComponents);
            emitCpu(builder, matrix, camera, frame, vertices[topology.indexAt(source + 5)], alpha, visualWaveComponents);
            emitCpu(builder, matrix, camera, frame, vertices[topology.indexAt(source + 2)], alpha, visualWaveComponents);
            emitted = true;
        }

        if (!emitted) return;
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        OceanRenderState.drawTwoSided(builder);
    }

    private static void emitCpu(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vec3d camera,
        OceanRenderFrame.Frame frame,
        OceanLodTopology.LocalVertex vertex,
        float alpha,
        int visualWaveComponents
    ) {
        double baseX = frame.plan().originX() + vertex.x();
        double baseZ = frame.plan().originZ() + vertex.z();
        OceanSurface.SurfaceSample sample = NewestOcean.clientOcean().sample(
            baseX, baseZ, frame.timeSeconds(), frame.conditions(), visualWaveComponents
        );
        double worldX = baseX + sample.horizontalDisplacement().x();
        double worldY = sample.height() + SURFACE_LIFT;
        double worldZ = baseZ + sample.horizontalDisplacement().z();
        OceanRenderCoordinates.Relative relative = OceanRenderCoordinates.relative(
            worldX, worldY, worldZ, camera.x, camera.y, camera.z
        );
        consumer.vertex(matrix, (float) relative.x(), (float) relative.y(), (float) relative.z())
            .color(0.93F, 0.98F, 1.0F, alpha);
    }

    private static BreakInputs breakInputs(ProceduralOcean ocean, int componentLimit, ShorelineSample shore) {
        double weightedIncoming = 0.0;
        double totalWeight = 0.0;
        double totalEnergy = 0.0;
        int limit = Math.min(componentLimit, ocean.components().size());
        for (int index = 0; index < limit; index++) {
            WaveComponent wave = ocean.components().get(index);
            double weight = wave.amplitude() * (0.35 + 0.65 * wave.steepness());
            double incoming = Math.max(0.0,
                wave.directionX() * shore.shoreDirectionX() + wave.directionZ() * shore.shoreDirectionZ());
            weightedIncoming += incoming * weight;
            totalWeight += weight;
            totalEnergy += wave.amplitude() * wave.steepness();
        }
        double alignment = totalWeight > 1.0e-9 ? weightedIncoming / totalWeight : 0.0;
        return new BreakInputs(clamp01(alignment), clamp01(totalEnergy));
    }

    private static double shallowFactor(double depthBlocks) {
        return clamp01(1.0 - (depthBlocks - 1.0) / 9.0);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record BreakInputs(double incomingAlignment, double waveEnergy) {
    }
}
