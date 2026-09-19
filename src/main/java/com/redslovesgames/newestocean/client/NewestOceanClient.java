package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.client.config.OceanConfigManager;
import com.redslovesgames.newestocean.client.water.VanillaWaterRuntimeBridge;
import com.redslovesgames.newestocean.network.OceanSeedPayload;
import com.redslovesgames.newestocean.network.OceanSyncState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-only entrypoint for deterministic ocean synchronization, rendering, and tuning. */
public final class NewestOceanClient implements ClientModInitializer {
    private static final OceanSyncState OCEAN_SYNC = new OceanSyncState();

    @Override
    public void onInitializeClient() {
        OceanConfigManager.load();

        ClientPlayConnectionEvents.INIT.register((handler, client) -> resetOceanSync());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetOceanSync());
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) ->
            VanillaWaterRuntimeBridge.invalidateChunk(chunk.getPos().x, chunk.getPos().z)
        );
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
            VanillaWaterRuntimeBridge.invalidateChunk(chunk.getPos().x, chunk.getPos().z)
        );

        ClientPlayNetworking.registerGlobalReceiver(OceanSeedPayload.ID, (payload, context) ->
            context.client().execute(() -> {
                OCEAN_SYNC.accept(payload.seed());
                NewestOcean.setClientOceanSeed(payload.seed());
                OceanWorldRenderer.reset();
                VanillaWaterRuntimeBridge.reset();
                VesselWakeTracker.reset();
            })
        );

        OceanGpuShader.register();
        ShorelineGpuShader.register();
        VesselWakeShader.register();
        VesselWakeTracker.register();
        OceanWorldRenderer.register();
        OceanDiagnosticsHud.register();
    }

    public static boolean isOceanSynchronized() {
        return OCEAN_SYNC.isSynchronized();
    }

    public static long oceanSyncGeneration() {
        return OCEAN_SYNC.generation();
    }

    private static void resetOceanSync() {
        OCEAN_SYNC.reset();
        NewestOcean.setClientOceanSeed(0L);
        OceanWorldRenderer.reset();
        VanillaWaterRuntimeBridge.reset();
        VesselWakeTracker.reset();
    }
}
