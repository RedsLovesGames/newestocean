package com.redslovesgames.newestocean.client;

/** Builds a bounded, topology-aligned shoreline field from client water probes. */
public final class ShorelineAnalyzer {
    public static final int MAX_DEPTH_BLOCKS = 12;

    @FunctionalInterface
    public interface Probe {
        boolean isWater(int x, int y, int z);
    }

    private ShorelineAnalyzer() {
    }

    public static ShorelineField build(
        OceanLodPlanner.Plan plan,
        OceanLodTopology topology,
        OceanLodCoverageMask coverage,
        int seaLevel,
        OceanQuality quality,
        Probe probe
    ) {
        if (plan == null || topology == null || coverage == null || quality == null || probe == null) {
            throw new IllegalArgumentException("shoreline analysis inputs are required");
        }
        if (topology.cellCount() != coverage.cellCount()) {
            throw new IllegalArgumentException("shoreline coverage must match topology");
        }

        ShorelineSample[] samples = new ShorelineSample[topology.cellCount()];
        int radius = ShorelineBreakModel.searchRadius(quality);
        int radiusSquared = radius * radius;
        int waterY = seaLevel - 1;

        for (int cell = 0; cell < samples.length; cell++) {
            if (!coverage.isWaterCell(cell)) {
                samples[cell] = ShorelineSample.NONE;
                continue;
            }

            int waterX = (int) Math.floor(plan.originX() + topology.cellCenterX(cell));
            int waterZ = (int) Math.floor(plan.originZ() + topology.cellCenterZ(cell));
            int depth = sampleDepth(probe, waterX, waterY, waterZ);

            // Deep cells cannot produce visual shoreline influence even at zero shore distance.
            // Skip the much more expensive horizontal search entirely for those cells.
            if (ShorelineBreakModel.shoreInfluence(depth, 0.0, radius) <= 0.0) {
                samples[cell] = ShorelineSample.NONE;
                continue;
            }

            int bestDx = 0;
            int bestDz = 0;
            int bestDistanceSquared = Integer.MAX_VALUE;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    int distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared > radiusSquared || distanceSquared > bestDistanceSquared) {
                        continue;
                    }
                    if (probe.isWater(waterX + dx, waterY, waterZ + dz)) {
                        continue;
                    }
                    if (distanceSquared < bestDistanceSquared
                        || (distanceSquared == bestDistanceSquared
                            && (dx < bestDx || (dx == bestDx && dz < bestDz)))) {
                        bestDistanceSquared = distanceSquared;
                        bestDx = dx;
                        bestDz = dz;
                    }
                }
            }

            if (bestDistanceSquared == Integer.MAX_VALUE) {
                samples[cell] = ShorelineSample.NONE;
                continue;
            }

            double distance = Math.sqrt(bestDistanceSquared);
            double influence = ShorelineBreakModel.shoreInfluence(depth, distance, radius);
            if (influence <= 0.0) {
                samples[cell] = new ShorelineSample(depth, distance, bestDx, bestDz, 0.0);
                continue;
            }
            samples[cell] = new ShorelineSample(depth, distance, bestDx, bestDz, influence);
        }

        return new ShorelineField(samples);
    }

    private static int sampleDepth(Probe probe, int x, int waterY, int z) {
        int depth = 0;
        for (int offset = 0; offset < MAX_DEPTH_BLOCKS; offset++) {
            if (!probe.isWater(x, waterY - offset, z)) {
                break;
            }
            depth++;
        }
        return depth;
    }
}
