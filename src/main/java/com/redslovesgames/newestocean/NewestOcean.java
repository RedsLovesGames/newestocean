package com.redslovesgames.newestocean;

import com.redslovesgames.newestocean.minecraft.SmallShipsIntegration;
import com.redslovesgames.newestocean.network.OceanNetworking;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NewestOcean implements ModInitializer {
    public static final String MOD_ID = "newestocean";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /*
     * Logical server and logical client can live in the same JVM in singleplayer/LAN.
     * Keep their deterministic oceans separate so client connection resets can never mutate
     * authoritative server physics.
     */
    private static volatile long serverOceanSeed;
    private static volatile ProceduralOcean serverOcean = ProceduralOcean.createDefault(0L);
    private static volatile long clientOceanSeed;
    private static volatile ProceduralOcean clientOcean = ProceduralOcean.createDefault(0L);

    @Override
    public void onInitialize() {
        OceanNetworking.registerServer();
        SmallShipsIntegration.register();
        LOGGER.info("Newest Ocean initialized with {} deterministic physical wave components.", serverOcean.componentCount());
    }

    public static ProceduralOcean serverOcean() {
        return serverOcean;
    }

    public static long serverOceanSeed() {
        return serverOceanSeed;
    }

    public static void setServerOceanSeed(long seed) {
        serverOceanSeed = seed;
        serverOcean = ProceduralOcean.createDefault(seed);
    }

    public static ProceduralOcean clientOcean() {
        return clientOcean;
    }

    public static long clientOceanSeed() {
        return clientOceanSeed;
    }

    public static void setClientOceanSeed(long seed) {
        clientOceanSeed = seed;
        clientOcean = ProceduralOcean.createDefault(seed);
    }

    private NewestOcean() {
    }
}
