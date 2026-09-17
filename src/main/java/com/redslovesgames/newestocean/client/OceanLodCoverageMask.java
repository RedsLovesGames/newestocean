package com.redslovesgames.newestocean.client;

/** Cached water/land classification for the cells in one snapped LOD plan. */
public final class OceanLodCoverageMask {
    @FunctionalInterface
    public interface ColumnProbe {
        boolean isWater(double x, double z);
    }

    private final boolean[] waterCells;

    private OceanLodCoverageMask(boolean[] waterCells) {
        this.waterCells = waterCells;
    }

    public static OceanLodCoverageMask build(
        OceanLodPlanner.Plan plan,
        OceanLodTopology topology,
        ColumnProbe probe
    ) {
        if (plan == null || topology == null || probe == null) {
            throw new IllegalArgumentException("plan, topology, and probe are required");
        }

        boolean[] water = new boolean[topology.cellCount()];
        for (int cell = 0; cell < water.length; cell++) {
            double worldX = plan.originX() + topology.cellCenterX(cell);
            double worldZ = plan.originZ() + topology.cellCenterZ(cell);
            water[cell] = probe.isWater(worldX, worldZ);
        }
        return new OceanLodCoverageMask(water);
    }

    public int[] waterIndices(OceanLodTopology topology) {
        if (topology == null || topology.cellCount() != waterCells.length) {
            throw new IllegalArgumentException("mask does not match topology");
        }

        int wetCells = 0;
        for (boolean water : waterCells) {
            if (water) {
                wetCells++;
            }
        }

        int[] filtered = new int[wetCells * 6];
        int output = 0;
        for (int cell = 0; cell < waterCells.length; cell++) {
            if (!waterCells[cell]) {
                continue;
            }
            int source = cell * 6;
            for (int i = 0; i < 6; i++) {
                filtered[output++] = topology.indexAt(source + i);
            }
        }
        return filtered;
    }

    public int cellCount() {
        return waterCells.length;
    }
}
