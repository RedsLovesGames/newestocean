package com.redslovesgames.newestocean.minecraft;

import com.redslovesgames.newestocean.ocean.ShoreAttenuation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselShoreDistanceCacheTest {
    @Test
    void repeatedNearbyQueriesReuseTheSameCachedWindow() {
        AtomicInteger probes = new AtomicInteger();
        VesselShoreDistanceCache cache = new VesselShoreDistanceCache((x, y, z) -> {
            probes.incrementAndGet();
            return VesselShoreDistanceCache.CellState.WATER;
        });

        assertEquals(ShoreAttenuation.FULL_STRENGTH_DISTANCE, cache.distanceToLand(0.5, 0.5, 62), 0.0);
        int afterFirst = probes.get();
        assertEquals(ShoreAttenuation.FULL_STRENGTH_DISTANCE, cache.distanceToLand(2.5, -1.5, 62), 0.0);

        assertEquals(afterFirst, probes.get(), "nearby samples should reuse the cached occupancy window");
    }

    @Test
    void movingBeyondSafeInteriorRebuildsTheWindow() {
        AtomicInteger probes = new AtomicInteger();
        VesselShoreDistanceCache cache = new VesselShoreDistanceCache((x, y, z) -> {
            probes.incrementAndGet();
            return VesselShoreDistanceCache.CellState.WATER;
        });

        cache.distanceToLand(0.5, 0.5, 62);
        int afterFirst = probes.get();
        cache.distanceToLand(VesselShoreDistanceCache.REBUILD_MARGIN_BLOCKS + 2.5, 0.5, 62);

        assertTrue(probes.get() > afterFirst);
    }

    @Test
    void changingWaterLayerRebuildsTheWindow() {
        AtomicInteger probes = new AtomicInteger();
        VesselShoreDistanceCache cache = new VesselShoreDistanceCache((x, y, z) -> {
            probes.incrementAndGet();
            return VesselShoreDistanceCache.CellState.WATER;
        });

        cache.distanceToLand(0.5, 0.5, 62);
        int afterFirst = probes.get();
        cache.distanceToLand(0.5, 0.5, 63);

        assertTrue(probes.get() > afterFirst);
    }

    @Test
    void narrowWaterBodyNeverReachesFullWaveStrength() {
        VesselShoreDistanceCache cache = new VesselShoreDistanceCache((x, y, z) ->
            Math.abs(x) >= 4
                ? VesselShoreDistanceCache.CellState.LAND
                : VesselShoreDistanceCache.CellState.WATER
        );

        double distance = cache.distanceToLand(0.5, 0.5, 62);

        assertEquals(4.0, distance, 1.0e-12);
        assertTrue(distance < ShoreAttenuation.FULL_STRENGTH_DISTANCE);
        assertTrue(ShoreAttenuation.factor(distance) < 1.0);
    }

    @Test
    void wideWaterBodyClampsAtFullStrengthDistance() {
        VesselShoreDistanceCache cache = new VesselShoreDistanceCache((x, y, z) ->
            Math.abs(x) >= 30 || Math.abs(z) >= 30
                ? VesselShoreDistanceCache.CellState.LAND
                : VesselShoreDistanceCache.CellState.WATER
        );

        assertEquals(
            ShoreAttenuation.FULL_STRENGTH_DISTANCE,
            cache.distanceToLand(0.5, 0.5, 62),
            0.0
        );
    }

    @Test
    void unknownCellsDoNotPretendToBeLand() {
        VesselShoreDistanceCache cache = new VesselShoreDistanceCache((x, y, z) ->
            VesselShoreDistanceCache.CellState.UNKNOWN
        );

        assertEquals(
            ShoreAttenuation.FULL_STRENGTH_DISTANCE,
            cache.distanceToLand(0.5, 0.5, 62),
            0.0
        );
    }
}
