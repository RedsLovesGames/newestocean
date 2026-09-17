package com.redslovesgames.newestocean.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Registers the tiny, join-time synchronization needed by the deterministic ocean. */
public final class OceanNetworking {
    private OceanNetworking() {
    }

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(OceanSeedPayload.ID, OceanSeedPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerPlayNetworking.send(
                        handler.player,
                        new OceanSeedPayload(handler.player.getServerWorld().getSeed())
                )
        );
    }
}
