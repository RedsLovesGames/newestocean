package com.redslovesgames.newestocean.client.shore;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoreDistanceFieldCacheTest {
    @Test
    void cameraMovementInsideCurrentTileWindowReusesSnapshotWithoutReprobing() {
        AtomicInteger calls = new AtomicInteger();
        ShoreDistanceFieldCache.CellProbe probe = (x, z) -> {
            calls.incrementAndGet();
            return x == -20
                ? ShoreDistanceFieldCache.CellState.LAND
                : ShoreDistanceFieldCache.CellState.WATER;
        };
        ShoreDistanceFieldCache cache = new ShoreDistanceFieldCache(8, 16);

        ShoreDistanceFieldCache.Snapshot first = cache.update(0, 0, probe);
        int afterFirst = calls.get();
        ShoreDistanceFieldCache.Snapshot second = cache.update(1, 1, probe);

        assertSame(first, second);
        assertTrue(afterFirst > 0);
        assertTrue(calls.get() == afterFirst);
    }

    @Test
    void crossingTileBoundaryLoadsOnlyNewTilesInsteadOfReprobingWholeField() {
        AtomicInteger calls = new AtomicInteger();
        ShoreDistanceFieldCache.CellProbe probe = (x, z) -> {
            calls.incrementAndGet();
            return ShoreDistanceFieldCache.CellState.WATER;
        };
        ShoreDistanceFieldCache cache = new ShoreDistanceFieldCache(8, 16);

        ShoreDistanceFieldCache.Snapshot first = cache.update(0, 0, probe);
        int firstCalls = calls.get();
        ShoreDistanceFieldCache.Snapshot second = cache.update(16, 0, probe);
        int secondCalls = calls.get() - firstCalls;

        assertNotSame(first, second);
        assertTrue(secondCalls > 0);
        assertTrue(secondCalls < firstCalls);
    }

    @Test
    void chunkInvalidationRefreshesOverlappingCachedCellsOnNextUpdate() {
        AtomicInteger calls = new AtomicInteger();
        ShoreDistanceFieldCache.CellProbe probe = (x, z) -> {
            calls.incrementAndGet();
            return ShoreDistanceFieldCache.CellState.WATER;
        };
        ShoreDistanceFieldCache cache = new ShoreDistanceFieldCache(8, 16);

        ShoreDistanceFieldCache.Snapshot first = cache.update(0, 0, probe);
        int beforeInvalidation = calls.get();
        cache.invalidateChunk(0, 0);
        ShoreDistanceFieldCache.Snapshot refreshed = cache.update(0, 0, probe);

        assertNotSame(first, refreshed);
        assertTrue(calls.get() > beforeInvalidation);
        assertTrue(refreshed.generation() > first.generation());
    }

    @Test
    void snapshotDistancesAreClampedToFifteenBlocks() {
        ShoreDistanceFieldCache cache = new ShoreDistanceFieldCache(24, 16);
        ShoreDistanceFieldCache.CellProbe probe = (x, z) -> x == 0 && z == 0
            ? ShoreDistanceFieldCache.CellState.LAND
            : ShoreDistanceFieldCache.CellState.WATER;

        ShoreDistanceFieldCache.Snapshot snapshot = cache.update(20, 0, probe);

        assertTrue(snapshot.grid().distanceToLand(0, 0) == 0.0);
        assertTrue(snapshot.grid().distanceToLand(10, 0) <= 10.000001);
        assertTrue(snapshot.grid().distanceToLand(20, 0) == 15.0);
    }
}
