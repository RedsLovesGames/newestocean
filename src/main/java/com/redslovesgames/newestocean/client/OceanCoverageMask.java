package com.redslovesgames.newestocean.client;

/**
 * Cached water/land coverage for one snapped ocean mesh plan.
 *
 * <p>The renderer can rebuild this only when the mesh origin or quality changes, avoiding world
 * block/fluid lookups for every cell on every frame.</p>
 */
public final class OceanCoverageMask {
    @FunctionalInterface
    public interface ColumnProbe {
        boolean isWater(double x, double z);
    }

    private final int cellsPerSide;
    private final boolean[] water;

    private OceanCoverageMask(int cellsPerSide, boolean[] water) {
        this.cellsPerSide = cellsPerSide;
        this.water = water;
    }

    public static OceanCoverageMask build(OceanMeshPlanner.Plan plan, ColumnProbe probe) {
        if (plan == null || probe == null) {
            throw new IllegalArgumentException("plan and probe are required");
        }

        int cellsPerSide = plan.verticesPerSide() - 1;
        boolean[] water = new boolean[cellsPerSide * cellsPerSide];
        double startX = plan.originX() - plan.radiusBlocks();
        double startZ = plan.originZ() - plan.radiusBlocks();
        double step = plan.gridStepBlocks();

        int index = 0;
        for (int z = 0; z < cellsPerSide; z++) {
            double centerZ = startZ + (z + 0.5) * step;
            for (int x = 0; x < cellsPerSide; x++) {
                double centerX = startX + (x + 0.5) * step;
                water[index++] = probe.isWater(centerX, centerZ);
            }
        }

        return new OceanCoverageMask(cellsPerSide, water);
    }

    public boolean isWater(int x, int z) {
        if (x < 0 || z < 0 || x >= cellsPerSide || z >= cellsPerSide) {
            return false;
        }
        return water[z * cellsPerSide + x];
    }

    public int cellCount() {
        return water.length;
    }

    public int cellsPerSide() {
        return cellsPerSide;
    }
}
