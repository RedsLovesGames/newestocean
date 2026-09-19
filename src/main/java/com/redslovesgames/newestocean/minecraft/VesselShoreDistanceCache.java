package com.redslovesgames.newestocean.minecraft;

import com.redslovesgames.newestocean.ocean.ShoreAttenuation;
import com.redslovesgames.newestocean.ocean.ShoreDistanceGrid;

/**
 * Small server-side shoreline cache centered around active vessel samples.
 * The cache never owns world state and never requests chunk loads. UNKNOWN cells simply do not
 * contribute land evidence, so missing chunks cannot create false shore walls.
 */
public final class VesselShoreDistanceCache {
    public static final int REBUILD_MARGIN_BLOCKS = 4;
    private static final int TRANSITION_RADIUS_BLOCKS = (int) Math.ceil(ShoreAttenuation.FULL_STRENGTH_DISTANCE);
    private static final int WINDOW_RADIUS_BLOCKS = TRANSITION_RADIUS_BLOCKS + REBUILD_MARGIN_BLOCKS;
    private static final int WINDOW_SIZE = WINDOW_RADIUS_BLOCKS * 2 + 1;

    private final CellProbe probe;
    private ShoreDistanceGrid grid;
    private int centerX;
    private int centerZ;
    private int waterY;

    public VesselShoreDistanceCache(CellProbe probe) {
        if (probe == null) {
            throw new IllegalArgumentException("cell probe is required");
        }
        this.probe = probe;
    }

    public double distanceToLand(double x, double z, int waterY) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("sample coordinates must be finite");
        }
        int cellX = (int) Math.floor(x);
        int cellZ = (int) Math.floor(z);
        if (needsRebuild(cellX, cellZ, waterY)) {
            rebuild(cellX, cellZ, waterY);
        }
        return grid.distanceToLand(x, z);
    }

    private boolean needsRebuild(int x, int z, int waterY) {
        return grid == null
            || this.waterY != waterY
            || Math.abs(x - centerX) > REBUILD_MARGIN_BLOCKS
            || Math.abs(z - centerZ) > REBUILD_MARGIN_BLOCKS;
    }

    private void rebuild(int centerX, int centerZ, int waterY) {
        int originX = centerX - WINDOW_RADIUS_BLOCKS;
        int originZ = centerZ - WINDOW_RADIUS_BLOCKS;
        boolean[][] land = new boolean[WINDOW_SIZE][WINDOW_SIZE];

        for (int localZ = 0; localZ < WINDOW_SIZE; localZ++) {
            int worldZ = originZ + localZ;
            for (int localX = 0; localX < WINDOW_SIZE; localX++) {
                int worldX = originX + localX;
                land[localZ][localX] = probe.sample(worldX, waterY, worldZ) == CellState.LAND;
            }
        }

        this.grid = ShoreDistanceGrid.fromLandMask(originX, originZ, land);
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.waterY = waterY;
    }

    @FunctionalInterface
    public interface CellProbe {
        CellState sample(int x, int y, int z);
    }

    public enum CellState {
        WATER,
        LAND,
        UNKNOWN
    }
}
