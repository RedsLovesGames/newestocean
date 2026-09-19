package com.redslovesgames.newestocean.client.compat.iris;

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
import com.redslovesgames.newestocean.client.water.OceanWaterFrameState;
import com.redslovesgames.newestocean.client.water.OceanWaterInjectionState;
import com.redslovesgames.newestocean.client.water.OceanWaterRuntime;
import com.redslovesgames.newestocean.client.water.OceanWaterUniformBinder;
import com.redslovesgames.newestocean.client.water.OceanWaterWaveData;
import com.redslovesgames.newestocean.client.water.VanillaShoreTextureBackend;
import com.redslovesgames.newestocean.math.Vec3;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.lang.reflect.Method;

/** Runtime owner for Iris 1.8.14-beta.1 real-water and shadow-water programs. */
public final class IrisWaterRuntimeBridge {
    private static final String SUPPORTED_IRIS = "1.8.14-beta.1";
    private static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";
    private static final int SHORE_TILE_SIZE = 16;
    private static final int RENDERER_WAVE_CAP = OceanWaterWaveData.MAX_COMPONENTS;

    private static final OceanWaterRuntime WATER_RUNTIME = new OceanWaterRuntime();
    private static ShoreDistanceFieldCache shoreCache;
    private static ShoreDistanceTexture shoreTexture;
    private static WaterSpriteBounds waterBounds;
    private static int shoreRadius = -1;
    private static long uploadedShoreGeneration = Long.MIN_VALUE;
    private static long frameId;

    private static volatile boolean waterSourcePatchSuccess;
    private static volatile boolean shadowSourcePatchSuccess;
    private static volatile boolean waterBindingSuccess;
    private static volatile boolean shadowBindingSuccess;
    private static volatile String lastFailure = "Iris water shader has not been patched yet";
    private static volatile OceanWaterInjectionState state = OceanWaterInjectionState.unsupported(
        OceanWaterInjectionState.RendererPath.IRIS,
        lastFailure
    );

    private static volatile boolean irisApiResolved;
    private static Method irisGetInstance;
    private static Method irisIsShaderPackInUse;

    private IrisWaterRuntimeBridge() {
    }

    public static boolean supportsVersion(String version) {
        if (version == null) {
            return false;
        }
        String normalized = version.trim();
        return normalized.equals(SUPPORTED_IRIS)
            || normalized.startsWith(SUPPORTED_IRIS + "+")
            || normalized.startsWith(SUPPORTED_IRIS + "-mc1.21.1")
            || normalized.startsWith(SUPPORTED_IRIS + "+mc1.21.1");
    }

    public static void noteSourcePatch(ProgramKind kind, boolean success, String failureReason) {
        if (kind == null) {
            throw new IllegalArgumentException("Iris program kind is required");
        }
        if (kind == ProgramKind.WATER) {
            waterSourcePatchSuccess = success;
        } else {
            shadowSourcePatchSuccess = success;
        }
        if (!success) {
            lastFailure = failureReason == null || failureReason.isBlank()
                ? "Iris " + kind.name().toLowerCase() + " shader patch failed"
                : failureReason;
        }
        refreshState();
    }

    /** Marks the ordinary Sodium source used while Iris is installed but no shaderpack is active. */
    public static void noteNoPackSourcePatch(boolean success, String failureReason) {
        waterSourcePatchSuccess = success;
        if (!success) {
            lastFailure = failureReason == null || failureReason.isBlank()
                ? "Iris no-pack Sodium water shader patch failed"
                : failureReason;
        }
        refreshState();
    }

    /** Called from Iris's transformed SodiumShader setupState for water/shadow-water passes. */
    public static void bindShaderPackProgram(ProgramKind kind) {
        if (!isSupportedInstalledIris() || !shaderPackInUse()) {
            return;
        }
        boolean sourceReady = kind == ProgramKind.WATER ? waterSourcePatchSuccess : shadowSourcePatchSuccess;
        if (!sourceReady) {
            return;
        }
        bindCurrentProgram(kind, true);
    }

    /** Called from Sodium's default chunk path when Iris is installed but no shaderpack is active. */
    public static void bindNoPackSodiumProgram() {
        if (!isSupportedInstalledIris() || shaderPackInUse() || !waterSourcePatchSuccess) {
            return;
        }
        bindCurrentProgram(ProgramKind.WATER, false);
    }

    public static boolean shaderPackInUse() {
        try {
            resolveIrisApi();
            if (irisGetInstance == null || irisIsShaderPackInUse == null) {
                return false;
            }
            Object api = irisGetInstance.invoke(null);
            return Boolean.TRUE.equals(irisIsShaderPackInUse.invoke(api));
        } catch (ReflectiveOperationException | RuntimeException error) {
            lastFailure = "Unable to query Iris shaderpack state: " + error.getClass().getSimpleName();
            refreshState();
            return false;
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
        waterBindingSuccess = false;
        shadowBindingSuccess = false;
        lastFailure = waterSourcePatchSuccess ? "Awaiting Iris water program bind" : "Iris water shader has not been patched yet";
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
        refreshState();
    }

    private static void bindCurrentProgram(ProgramKind kind, boolean shaderPack) {
        MinecraftClient client = MinecraftClient.getInstance();
        OceanClientConfig config = OceanConfigManager.current().copy().sanitize();
        if (!config.oceanRenderingEnabled() || !config.customShadersEnabled()
            || client.world == null || !NewestOceanClient.isOceanSynchronized()) {
            return;
        }

        try {
            RenderSystem.assertOnRenderThread();
            int programId = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            if (programId <= 0) {
                throw new IllegalStateException("Iris/Sodium did not leave the target water program bound");
            }

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
                OceanWaterInjectionState.RendererPath.IRIS
            );

            ShoreDistanceTexture.Payload shore = updateShoreField(client, camera, quality, config);
            WaterSpriteBounds bounds = resolveWaterBounds(client);
            IrisWaterProgramSink sink = new IrisWaterProgramSink(programId);
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
            if (kind == ProgramKind.WATER) {
                waterBindingSuccess = bound;
            } else {
                shadowBindingSuccess = bound;
            }
            if (!bound) {
                lastFailure = "One or more injected Iris " + kind.name().toLowerCase() + " uniforms were not linked";
            } else if (!shaderPack || (waterBindingSuccess && shadowBindingSuccess)) {
                lastFailure = "";
            }
            refreshState();
        } catch (RuntimeException error) {
            if (kind == ProgramKind.WATER) {
                waterBindingSuccess = false;
            } else {
                shadowBindingSuccess = false;
            }
            lastFailure = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
            refreshState();
            NewestOcean.LOGGER.warn(
                "Newest Ocean Iris real-water binding failed; leaving ordinary Iris water undisplaced.",
                error
            );
        }
    }

    private static boolean isSupportedInstalledIris() {
        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isModLoaded("iris")) {
            return false;
        }
        String version = loader.getModContainer("iris")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("");
        if (!supportsVersion(version)) {
            state = OceanWaterInjectionState.unsupported(
                OceanWaterInjectionState.RendererPath.IRIS,
                "Unsupported Iris version: " + (version.isBlank() ? "unknown" : version)
            );
            return false;
        }
        return true;
    }

    private static synchronized void resolveIrisApi() throws ReflectiveOperationException {
        if (irisApiResolved) {
            return;
        }
        irisApiResolved = true;
        Class<?> api = Class.forName(IRIS_API, false, IrisWaterRuntimeBridge.class.getClassLoader());
        irisGetInstance = api.getMethod("getInstance");
        irisIsShaderPackInUse = api.getMethod("isShaderPackInUse");
    }

    private static void refreshState() {
        boolean pack = shaderPackInUseUnchecked();
        boolean shadowRequired = pack;
        boolean source = waterSourcePatchSuccess;
        boolean uniforms = waterBindingSuccess && (!shadowRequired || shadowBindingSuccess);
        boolean shadow = !shadowRequired || shadowSourcePatchSuccess;
        String reason = (source && uniforms && shadow) ? "" : lastFailure;
        state = new OceanWaterInjectionState(
            source,
            uniforms,
            shadow,
            OceanWaterInjectionState.RendererPath.IRIS,
            reason == null ? "" : reason
        );
    }

    private static boolean shaderPackInUseUnchecked() {
        try {
            resolveIrisApi();
            if (irisGetInstance == null || irisIsShaderPackInUse == null) {
                return false;
            }
            Object api = irisGetInstance.invoke(null);
            return Boolean.TRUE.equals(irisIsShaderPackInUse.invoke(api));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
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

    public enum ProgramKind {
        WATER,
        SHADOW_WATER
    }
}
