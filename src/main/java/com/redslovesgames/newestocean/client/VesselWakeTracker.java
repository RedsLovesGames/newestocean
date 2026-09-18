package com.redslovesgames.newestocean.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client-only, event-driven wake history tracker for boat-like entities. */
public final class VesselWakeTracker {
    private static final Set<BoatEntity> LOADED_BOATS =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<BoatEntity, VesselWakeHistory> HISTORIES = new IdentityHashMap<>();
    private static boolean registered;

    private VesselWakeTracker() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof BoatEntity boat) {
                LOADED_BOATS.add(boat);
            }
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof BoatEntity boat) {
                LOADED_BOATS.remove(boat);
                HISTORIES.remove(boat);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(VesselWakeTracker::tick);
    }

    public static void reset() {
        LOADED_BOATS.clear();
        HISTORIES.clear();
    }

    public static List<Trail> snapshot(Vec3d cameraPosition, OceanQuality quality, double nowSeconds) {
        if (cameraPosition == null || quality == null || !Double.isFinite(nowSeconds)) {
            throw new IllegalArgumentException("wake snapshot requires camera, quality, and finite time");
        }

        VesselWakeQuality.Budget budget = VesselWakeQuality.budget(quality);
        List<Candidate> candidates = candidatesForLoadedBoats();
        List<Candidate> selected = selectNearest(
            candidates,
            cameraPosition.x,
            cameraPosition.z,
            budget
        );
        Set<Integer> selectedIds = new HashSet<>();
        for (Candidate candidate : selected) {
            selectedIds.add(candidate.id());
        }

        List<Trail> trails = new ArrayList<>();
        HISTORIES.entrySet().removeIf(entry -> !entry.getKey().isAlive());
        for (Map.Entry<BoatEntity, VesselWakeHistory> entry : HISTORIES.entrySet()) {
            BoatEntity boat = entry.getKey();
            if (!selectedIds.contains(boat.getId())) {
                continue;
            }
            List<VesselWakeDescriptor> samples = entry.getValue().snapshot(nowSeconds);
            if (samples.size() >= 2) {
                trails.add(new Trail(boat.getId(), samples));
            }
        }
        trails.sort(Comparator.comparingInt(Trail::entityId));
        return List.copyOf(trails);
    }

    static List<Candidate> selectNearest(
        List<Candidate> candidates,
        double cameraX,
        double cameraZ,
        VesselWakeQuality.Budget budget
    ) {
        if (candidates == null || budget == null || !Double.isFinite(cameraX) || !Double.isFinite(cameraZ)) {
            throw new IllegalArgumentException("wake selection inputs are invalid");
        }
        double radiusSquared = budget.trackingRadiusBlocks() * budget.trackingRadiusBlocks();
        return candidates.stream()
            .filter(candidate -> candidate.distanceSquared(cameraX, cameraZ) <= radiusSquared)
            .sorted(
                Comparator.comparingDouble((Candidate candidate) -> candidate.distanceSquared(cameraX, cameraZ))
                    .thenComparingInt(Candidate::id)
            )
            .limit(budget.maxVessels())
            .toList();
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null || !NewestOceanClient.isOceanSynchronized()) {
            return;
        }

        LOADED_BOATS.removeIf(boat -> !boat.isAlive() || boat.getWorld() != client.world);
        HISTORIES.entrySet().removeIf(entry -> !entry.getKey().isAlive() || entry.getKey().getWorld() != client.world);

        VesselWakeQuality.Budget budget = VesselWakeQuality.budget(OceanWorldRenderer.quality());
        List<Candidate> selected = selectNearest(
            candidatesForLoadedBoats(),
            client.player.getX(),
            client.player.getZ(),
            budget
        );
        Set<Integer> selectedIds = new HashSet<>();
        for (Candidate candidate : selected) {
            selectedIds.add(candidate.id());
        }

        double nowSeconds = client.world.getTime() / 20.0;
        for (BoatEntity boat : LOADED_BOATS) {
            if (!selectedIds.contains(boat.getId())) {
                continue;
            }

            Vec3d velocity = boat.getVelocity();
            double speed = Math.hypot(velocity.x, velocity.z);
            if (speed < VesselWakeHistory.MIN_SPEED) {
                continue;
            }

            double directionX = velocity.x / speed;
            double directionZ = velocity.z / speed;
            double lengthX = boat.getBoundingBox().getLengthX();
            double lengthZ = boat.getBoundingBox().getLengthZ();
            double beam = Math.max(1.0, Math.max(lengthX, lengthZ));

            HISTORIES.computeIfAbsent(boat, ignored -> new VesselWakeHistory()).record(
                boat.getX(),
                boat.getZ(),
                directionX,
                directionZ,
                speed,
                beam,
                nowSeconds
            );
        }

        HISTORIES.entrySet().removeIf(entry -> entry.getValue().snapshot(nowSeconds).isEmpty());
    }

    private static List<Candidate> candidatesForLoadedBoats() {
        List<Candidate> candidates = new ArrayList<>(LOADED_BOATS.size());
        for (BoatEntity boat : LOADED_BOATS) {
            if (boat.isAlive()) {
                candidates.add(new Candidate(boat.getId(), boat.getX(), boat.getZ()));
            }
        }
        return candidates;
    }

    static record Candidate(int id, double x, double z) {
        Candidate {
            if (!Double.isFinite(x) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("wake candidate coordinates must be finite");
            }
        }

        double distanceSquared(double originX, double originZ) {
            double dx = x - originX;
            double dz = z - originZ;
            return dx * dx + dz * dz;
        }
    }

    public record Trail(int entityId, List<VesselWakeDescriptor> samples) {
        public Trail {
            if (samples == null) {
                throw new IllegalArgumentException("wake trail samples are required");
            }
            samples = List.copyOf(samples);
        }
    }
}
