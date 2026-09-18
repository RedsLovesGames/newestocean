package com.redslovesgames.newestocean.ocean;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable nearest-land distance field. Distances are exact Euclidean block distances within the
 * 15-block transition radius and are clamped to 15 beyond it.
 */
public final class ShoreDistanceGrid implements ShoreDistanceProvider {
    private static final double MAX_DISTANCE = ShoreAttenuation.FULL_STRENGTH_DISTANCE;
    private static final Offset[] SEARCH_OFFSETS = buildSearchOffsets();

    private final int originX;
    private final int originZ;
    private final int width;
    private final int height;
    private final double[] distances;

    private ShoreDistanceGrid(int originX, int originZ, int width, int height, double[] distances) {
        this.originX = originX;
        this.originZ = originZ;
        this.width = width;
        this.height = height;
        this.distances = distances;
    }

    /**
     * Builds a zero-origin grid. {@code landMask[z][x] == true} marks land.
     */
    public static ShoreDistanceGrid fromLandMask(boolean[][] landMask) {
        return fromLandMask(0, 0, landMask);
    }

    /**
     * Builds a world-aligned grid. {@code landMask[z][x] == true} marks land.
     */
    public static ShoreDistanceGrid fromLandMask(int originX, int originZ, boolean[][] landMask) {
        if (landMask == null || landMask.length == 0 || landMask[0] == null || landMask[0].length == 0) {
            throw new IllegalArgumentException("land mask must be non-empty");
        }
        int height = landMask.length;
        int width = landMask[0].length;
        for (boolean[] row : landMask) {
            if (row == null || row.length != width) {
                throw new IllegalArgumentException("land mask must be rectangular");
            }
        }

        double[] distances = new double[width * height];
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                distances[z * width + x] = nearestDistance(landMask, width, height, x, z);
            }
        }
        return new ShoreDistanceGrid(originX, originZ, width, height, distances);
    }

    private static double nearestDistance(boolean[][] landMask, int width, int height, int x, int z) {
        for (Offset offset : SEARCH_OFFSETS) {
            int landX = x + offset.dx();
            int landZ = z + offset.dz();
            if (landX < 0 || landX >= width || landZ < 0 || landZ >= height) {
                continue;
            }
            if (landMask[landZ][landX]) {
                return offset.distance();
            }
        }
        return MAX_DISTANCE;
    }

    private static Offset[] buildSearchOffsets() {
        int radius = (int) Math.ceil(MAX_DISTANCE);
        List<Offset> offsets = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                double distance = Math.hypot(dx, dz);
                if (distance <= MAX_DISTANCE) {
                    offsets.add(new Offset(dx, dz, distance));
                }
            }
        }
        offsets.sort(Comparator
            .comparingDouble(Offset::distance)
            .thenComparingInt(Offset::dz)
            .thenComparingInt(Offset::dx));
        return offsets.toArray(Offset[]::new);
    }

    @Override
    public double distanceToLand(double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("sample coordinates must be finite");
        }
        int cellX = (int) Math.floor(x) - originX;
        int cellZ = (int) Math.floor(z) - originZ;
        if (cellX < 0 || cellX >= width || cellZ < 0 || cellZ >= height) {
            return MAX_DISTANCE;
        }
        return distances[cellZ * width + cellX];
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return originZ;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    private record Offset(int dx, int dz, double distance) {
    }
}
