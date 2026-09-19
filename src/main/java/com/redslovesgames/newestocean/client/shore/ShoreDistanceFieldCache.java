package com.redslovesgames.newestocean.client.shore;

import com.redslovesgames.newestocean.ocean.ShoreAttenuation;
import com.redslovesgames.newestocean.ocean.ShoreDistanceGrid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Camera-centered, tile-reusing shoreline occupancy cache.
 *
 * <p>The cache itself is Minecraft-independent. A renderer adapter supplies a {@link CellProbe}
 * that reads only already-loaded world data. UNKNOWN cells are treated conservatively as land so
 * unavailable terrain cannot be mistaken for open ocean.</p>
 */
public final class ShoreDistanceFieldCache {
    private final int visibleRadiusBlocks;
    private final int tileSize;
    private final int sampleRadiusBlocks;
    private final Map<TileKey, Tile> tiles = new HashMap<>();
    private final Set<TileKey> dirtyTiles = new HashSet<>();

    private Window activeWindow;
    private Snapshot snapshot;
    private long generation;

    public ShoreDistanceFieldCache(int visibleRadiusBlocks, int tileSize) {
        if (visibleRadiusBlocks < 0) {
            throw new IllegalArgumentException("visibleRadiusBlocks cannot be negative");
        }
        if (tileSize <= 0) {
            throw new IllegalArgumentException("tileSize must be positive");
        }
        this.visibleRadiusBlocks = visibleRadiusBlocks;
        this.tileSize = tileSize;
        this.sampleRadiusBlocks = visibleRadiusBlocks
            + (int) Math.ceil(ShoreAttenuation.FULL_STRENGTH_DISTANCE);
    }

    /**
     * Updates the field for the tile containing the camera. Movement inside that tile reuses the
     * exact same immutable snapshot unless a cached tile was explicitly invalidated.
     */
    public Snapshot update(int cameraX, int cameraZ, CellProbe probe) {
        if (probe == null) {
            throw new IllegalArgumentException("probe cannot be null");
        }

        Window required = windowFor(cameraX, cameraZ);
        boolean windowChanged = !required.equals(activeWindow);
        boolean changed = windowChanged;

        pruneOutside(required);

        for (int tileZ = required.minTileZ(); tileZ <= required.maxTileZ(); tileZ++) {
            for (int tileX = required.minTileX(); tileX <= required.maxTileX(); tileX++) {
                TileKey key = new TileKey(tileX, tileZ);
                Tile tile = tiles.get(key);
                if (tile == null || dirtyTiles.remove(key)) {
                    tiles.put(key, captureTile(key, probe));
                    changed = true;
                }
            }
        }

        if (!changed && snapshot != null) {
            return snapshot;
        }

        ShoreDistanceGrid grid = assembleGrid(required);
        activeWindow = required;
        generation++;
        snapshot = new Snapshot(grid, generation);
        return snapshot;
    }

    /** Marks cached tiles overlapping a Minecraft-style 16x16 chunk as dirty, without probing. */
    public void invalidateChunk(int chunkX, int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        int minTileX = Math.floorDiv(minX, tileSize);
        int maxTileX = Math.floorDiv(maxX, tileSize);
        int minTileZ = Math.floorDiv(minZ, tileSize);
        int maxTileZ = Math.floorDiv(maxZ, tileSize);
        for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
            for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                TileKey key = new TileKey(tileX, tileZ);
                if (tiles.containsKey(key)) {
                    dirtyTiles.add(key);
                }
            }
        }
    }

    public void clear() {
        tiles.clear();
        dirtyTiles.clear();
        activeWindow = null;
        snapshot = null;
    }

    public int visibleRadiusBlocks() {
        return visibleRadiusBlocks;
    }

    public int tileSize() {
        return tileSize;
    }

    private Window windowFor(int cameraX, int cameraZ) {
        int cameraTileX = Math.floorDiv(cameraX, tileSize);
        int cameraTileZ = Math.floorDiv(cameraZ, tileSize);
        int centerMinX = cameraTileX * tileSize;
        int centerMinZ = cameraTileZ * tileSize;
        int centerMaxX = centerMinX + tileSize - 1;
        int centerMaxZ = centerMinZ + tileSize - 1;

        return new Window(
            Math.floorDiv(centerMinX - sampleRadiusBlocks, tileSize),
            Math.floorDiv(centerMaxX + sampleRadiusBlocks, tileSize),
            Math.floorDiv(centerMinZ - sampleRadiusBlocks, tileSize),
            Math.floorDiv(centerMaxZ + sampleRadiusBlocks, tileSize)
        );
    }

    private Tile captureTile(TileKey key, CellProbe probe) {
        boolean[] land = new boolean[tileSize * tileSize];
        int originX = key.x() * tileSize;
        int originZ = key.z() * tileSize;
        for (int localZ = 0; localZ < tileSize; localZ++) {
            for (int localX = 0; localX < tileSize; localX++) {
                CellState state = probe.probe(originX + localX, originZ + localZ);
                land[localZ * tileSize + localX] = state != CellState.WATER;
            }
        }
        return new Tile(land);
    }

    private void pruneOutside(Window required) {
        Iterator<Map.Entry<TileKey, Tile>> iterator = tiles.entrySet().iterator();
        while (iterator.hasNext()) {
            TileKey key = iterator.next().getKey();
            if (!required.contains(key)) {
                iterator.remove();
                dirtyTiles.remove(key);
            }
        }
    }

    private ShoreDistanceGrid assembleGrid(Window window) {
        int tileWidth = window.maxTileX() - window.minTileX() + 1;
        int tileHeight = window.maxTileZ() - window.minTileZ() + 1;
        int width = tileWidth * tileSize;
        int height = tileHeight * tileSize;
        boolean[][] landMask = new boolean[height][width];

        for (int tileZ = window.minTileZ(); tileZ <= window.maxTileZ(); tileZ++) {
            for (int tileX = window.minTileX(); tileX <= window.maxTileX(); tileX++) {
                Tile tile = tiles.get(new TileKey(tileX, tileZ));
                if (tile == null) {
                    throw new IllegalStateException("shore tile window was assembled before capture completed");
                }
                int offsetX = (tileX - window.minTileX()) * tileSize;
                int offsetZ = (tileZ - window.minTileZ()) * tileSize;
                for (int localZ = 0; localZ < tileSize; localZ++) {
                    for (int localX = 0; localX < tileSize; localX++) {
                        landMask[offsetZ + localZ][offsetX + localX]
                            = tile.land()[localZ * tileSize + localX];
                    }
                }
            }
        }

        return ShoreDistanceGrid.fromLandMask(
            window.minTileX() * tileSize,
            window.minTileZ() * tileSize,
            landMask
        );
    }

    public enum CellState {
        WATER,
        LAND,
        UNKNOWN
    }

    @FunctionalInterface
    public interface CellProbe {
        CellState probe(int worldX, int worldZ);
    }

    public record Snapshot(ShoreDistanceGrid grid, long generation) {
        public Snapshot {
            if (grid == null) {
                throw new IllegalArgumentException("grid cannot be null");
            }
            if (generation < 1) {
                throw new IllegalArgumentException("generation must be positive");
            }
        }
    }

    private record TileKey(int x, int z) {
    }

    private record Tile(boolean[] land) {
    }

    private record Window(int minTileX, int maxTileX, int minTileZ, int maxTileZ) {
        private boolean contains(TileKey key) {
            return key.x() >= minTileX && key.x() <= maxTileX
                && key.z() >= minTileZ && key.z() <= maxTileZ;
        }
    }
}
