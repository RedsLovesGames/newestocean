package com.redslovesgames.newestocean.minecraft;

import com.redslovesgames.newestocean.NewestOcean;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Optional, dependency-free runtime integration for Small Ships. */
public final class SmallShipsIntegration {
    private static final String MOD_ID = "smallships";
    private static final Map<ServerWorld, Set<BoatEntity>> TRACKED = new WeakHashMap<>();

    private SmallShipsIntegration() {
    }

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }

        ServerEntityEvents.ENTITY_LOAD.register(SmallShipsIntegration::onEntityLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(SmallShipsIntegration::onEntityUnload);
        ServerTickEvents.END_WORLD_TICK.register(SmallShipsIntegration::onEndWorldTick);
        NewestOcean.LOGGER.info("Small Ships ocean physics integration enabled.");
    }

    private static void onEntityLoad(Entity entity, ServerWorld world) {
        if (entity instanceof BoatEntity boat && isSmallShip(boat)) {
            tracked(world).add(boat);
        }
    }

    private static void onEntityUnload(Entity entity, ServerWorld world) {
        if (!(entity instanceof BoatEntity boat)) {
            return;
        }

        Set<BoatEntity> boats = TRACKED.get(world);
        if (boats != null) {
            boats.remove(boat);
            if (boats.isEmpty()) {
                TRACKED.remove(world);
            }
        }
    }

    private static void onEndWorldTick(ServerWorld world) {
        Set<BoatEntity> boats = TRACKED.get(world);
        if (boats == null || boats.isEmpty()) {
            return;
        }

        boats.removeIf(BoatEntity::isRemoved);
        for (BoatEntity boat : boats) {
            SmallShipsPhysics.tick(boat);
        }

        if (boats.isEmpty()) {
            TRACKED.remove(world);
        }
    }

    private static Set<BoatEntity> tracked(ServerWorld world) {
        return TRACKED.computeIfAbsent(
            world,
            ignored -> Collections.newSetFromMap(new IdentityHashMap<>())
        );
    }

    private static boolean isSmallShip(BoatEntity boat) {
        Identifier id = Registries.ENTITY_TYPE.getId(boat.getType());
        return id != null && MOD_ID.equals(id.getNamespace());
    }
}
