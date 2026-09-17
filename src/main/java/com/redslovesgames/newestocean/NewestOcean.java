package com.redslovesgames.newestocean;

import com.redslovesgames.newestocean.minecraft.SmallShipsIntegration;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NewestOcean implements ModInitializer {
    public static final String MOD_ID = "newestocean";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile ProceduralOcean ocean = ProceduralOcean.createDefault(0L);

    @Override
    public void onInitialize() {
        SmallShipsIntegration.register();
        LOGGER.info("Newest Ocean initialized with {} deterministic physical wave components.", ocean.componentCount());
    }

    public static ProceduralOcean ocean() {
        return ocean;
    }

    public static void setOceanSeed(long seed) {
        ocean = ProceduralOcean.createDefault(seed);
    }

    private NewestOcean() {
    }
}
