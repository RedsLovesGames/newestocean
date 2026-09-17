package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.network.OceanSeedPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-only entrypoint for deterministic ocean synchronization and rendering. */
public final class NewestOceanClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OceanSeedPayload.ID, (payload, context) ->
                context.client().execute(() -> NewestOcean.setOceanSeed(payload.seed()))
        );
    }
}
