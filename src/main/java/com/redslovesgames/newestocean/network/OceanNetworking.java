package com.redslovesgames.newestocean.network;

import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.ocean.OceanSeed;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Registers the tiny, join-time synchronization needed by the deterministic ocean. */
public final class OceanNetworking {
    private OceanNetworking() {
    }

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(OceanSeedPayload.ID, OceanSeedPayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
            NewestOcean.setOceanSeed(OceanSeed.derive(server.getOverworld().getSeed()))
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            long oceanSeed = OceanSeed.derive(handler.player.getServerWorld().getSeed());
            NewestOcean.setOceanSeed(oceanSeed);
            ServerPlayNetworking.send(handler.player, new OceanSeedPayload(oceanSeed));
        });
    }
}
