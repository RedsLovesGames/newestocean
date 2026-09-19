package com.redslovesgames.newestocean.client.water;

import com.mojang.blaze3d.systems.RenderSystem;
import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.client.NewestOceanClient;
import com.redslovesgames.newestocean.client.OceanQuality;
import com.redslovesgames.newestocean.client.OceanWorldRenderer;
import com.redslovesgames.newestocean.client.config.OceanClientConfig;
import com.redslovesgames.newestocean.client.config.OceanConfigManager;
import com.redslovesgames.newestocean.client.shore.ShoreDistanceFieldCache;
import com.redslovesgames.newestocean.client.shore.ShoreDistanceTexture;
import com.redslovesgames.newestocean.client.shore.WaterSpriteBounds;
import com.redslovesgames.newestocean.math.Vec3;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Owns the Vanilla/Fabric real-water runtime state. This bridge is deliberately inactive when
 * Sodium or Iris owns the renderer, those paths receive their own thin adapters in later tasks.
 */
public final class VanillaWaterRuntimeBridge {
    private static final String TARGET_PROGRAM = "rendertype_translucent";
    private static final int SHORE_TILE_SIZE = 16;
    private static final int RENDERER_WAVE_CAP = OceanWaterWaveData.MAX_COMPONENTS;

    private static final OceanWaterRuntime WATER_RUNTIME = new OceanWaterRuntime();
    private static ShoreDistanceFieldCache shoreCache;
    private static ShoreDistanceTexture shoreTexture;
    private static WaterSpriteBounds waterBounds;
    private static int shoreRadius = -1;
    private static long uploadedShoreGeneration = Long.MIN_VALUE;
    private static long frameId;
    private static volatile boolean sourcePatchSuccess;
    private static volatile OceanWaterInjectionState state = OceanWaterInjectionState.unsupported("Vanilla water shader has not been patched yet");

    private VanillaWaterRuntimeBridge() {
    }

    public static void noteSourcePatch(boolean success, String failureReason) {
        sourcePatchSuccess = success;
        if (!success) {
            state = new OceanWaterInjectionState(
                false,
                false,
                false,
                OceanWaterInjectionState.RendererPath.VANILLA,
                failureReason == null || failureReason.isBlank() ? "Vanilla water shader patch failed" : failureReason
            );
        }
    }

    public static void bind(ShaderProgram program) {
        if (program == null || !TARGET_PROGRAM.equals(program.getName()) || !sourcePatchSuccess) {
            return;
        }
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("sodium") || loader.isModLoaded("iris")) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        OceanClientConfig config = OceanConfigManager.current().copy().sanitize();
        if (!config.oceanRenderingEnabled() || !config.customShadersEnabled()
            || client.world == null || !NewestOceanClient.isOceanSynchronized()) {
            return;
        }

        try {
            RenderSystem.assertOnRenderThread();
            Vec3d camera = client.gameRenderer.getCamera().getPos();
            float tickDelta = client.getRenderTickCounter().getTickDelta(true);
            double timeSeconds = (client.world.getTime() + tickDelta) / 20.0;
            OceanQuality quality = OceanWorldRenderer.quality();

            OceanWaterFrameState frame = WATER_RUNTIME.buildFrame(
                Math.max(0L, frameId++),
                NewestOcean.clientOceanSeed(),
                NewestOcean.clientOcean(),
                timeSeconds,
                client.world.getSeaLevel(),
                client.world.getRainGradient(tickDelta),
                client.world.getThunderGradient(tickDelta),
                quality,
                config,
                RENDERER_WAVE_CAP,
                OceanWaterInjectionState.RendererPath.VANILLA
            );

            ShoreDistanceTexture.Payload shore = updateShoreField(client, camera, quality, config);
            WaterSpriteBounds bounds = resolveWaterBounds(client);
            VanillaWaterProgramSink sink = new VanillaWaterProgramSink(program);
            OceanWaterUniformBinder.bind(
                frame,
                shore,
                shoreTexture.handle(),
                bounds,
                new Vec3(camera.x, camera.y, camera.z),
                config.whitecapsEnabled() ? config.whitecapIntensity() : 0.0,
                sink
            );

            boolean bound = sink.complete();
            state = new OceanWaterInjectionState(
                true,
                bound,
                false,
                OceanWaterInjectionState.RendererPath.VANILLA,
                bound ? "" : "One or more injected Vanilla water uniforms were not linked"
            );
        } catch (RuntimeException error) {
            state = new OceanWaterInjectionState(
                true,
                false,
                false,
                OceanWaterInjectionState.RendererPath.VANILLA,
                error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage())
            );
            NewestOcean.LOGGER.warn("Newest Ocean Vanilla real-water binding failed; leaving ordinary Vanilla water active.", error);
        }
    }

    public static void invalidateChunk(int chunkX, int chunkZ) {
        ShoreDistanceFieldCache cache = shoreCache;
        if (cache != null) {
            cache.invalidateChunk(chunkX, chunkZ);
        }
    }

    public static OceanWaterInjectionState state() {
        return state;
    }

    public static void reset() {
        WATER_RUNTIME.reset();
        frameId = 0L;
        uploadedShoreGeneration = Long.MIN_VALUE;
        waterBounds = null;
        if (shoreCache != null) {
            shoreCache.clear();
            shoreCache = null;
        }
        shoreRadius = -1;

        ShoreDistanceTexture texture = shoreTexture;
        shoreTexture = null;
        if (texture != null) {
            if (RenderSystem.isOnRenderThread()) {
                texture.close();
            } else {
                RenderSystem.recordRenderCall(texture::close);
            }
        }
        state = sourcePatchSuccess
            ? new OceanWaterInjectionState(true, false, false, OceanWaterInjectionState.RendererPath.VANILLA, "Awaiting water program bind")
            : OceanWaterInjectionState.unsupported("Vanilla water shader has not been patched yet");
    }

    private static ShoreDistanceTexture.Payload updateShoreField(
        MinecraftClient client,
        Vec3d camera,
        OceanQuality quality,
        OceanClientConfig config
    ) {
        int visibleRadius = Math.max(
            1,
            (int) Math.ceil(quality.renderRadiusBlocks() * config.renderDistanceScale())
        );
        if (shoreCache == null || shoreRadius != visibleRadius) {
            shoreCache = new ShoreDistanceFieldCache(visibleRadius, SHORE_TILE_SIZE);
            shoreRadius = visibleRadius;
            uploadedShoreGeneration = Long.MIN_VALUE;
        }
        if (shoreTexture == null) {
            shoreTexture = new ShoreDistanceTexture(new VanillaShoreTextureBackend());
        }

        int waterY = client.world.getSeaLevel() - 1;
        BlockPos.Mutable probePos = new BlockPos.Mutable();
        ShoreDistanceFieldCache.Snapshot snapshot = shoreCache.update(
            (int) Math.floor(camera.x),
            (int) Math.floor(camera.z),
            (x, z) -> {
                int chunkX = Math.floorDiv(x, 16);
                int chunkZ = Math.floorDiv(z, 16);
                if (!client.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                    return ShoreDistanceFieldCache.CellState.UNKNOWN;
                }
                probePos.set(x, waterY, z);
                return client.world.getFluidState(probePos).isIn(FluidTags.WATER)
                    ? ShoreDistanceFieldCache.CellState.WATER
                    : ShoreDistanceFieldCache.CellState.LAND;
            }
        );

        if (shoreTexture.payload() == null || snapshot.generation() != uploadedShoreGeneration) {
            shoreTexture.upload(ShoreDistanceTexture.encode(snapshot.grid()));
            uploadedShoreGeneration = snapshot.generation();
        }
        return shoreTexture.payload();
    }

    @SuppressWarnings("deprecation")
    private static WaterSpriteBounds resolveWaterBounds(MinecraftClient client) {
        if (waterBounds != null) {
            return waterBounds;
        }
        SpriteAtlasTexture atlas = client.getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        Sprite still = atlas.getSprite(Identifier.ofVanilla("block/water_still"));
        Sprite flowing = atlas.getSprite(Identifier.ofVanilla("block/water_flow"));
        waterBounds = new WaterSpriteBounds(bounds(still), bounds(flowing));
        return waterBounds;
    }

    private static WaterSpriteBounds.Bounds bounds(Sprite sprite) {
        return new WaterSpriteBounds.Bounds(
            sprite.getMinU(),
            sprite.getMinV(),
            sprite.getMaxU(),
            sprite.getMaxV()
        );
    }
}
