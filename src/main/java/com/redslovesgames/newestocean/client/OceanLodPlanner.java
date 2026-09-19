package com.redslovesgames.newestocean.client;

import java.util.List;

/** Plans camera-centered square LOD rings without coupling rendering quality to physics. */
public final class OceanLodPlanner {
    private OceanLodPlanner() {
    }

    public static Plan plan(OceanQuality quality, double cameraX, double cameraZ) {
        return plan(quality, cameraX, cameraZ, 1.0);
    }

    public static Plan plan(OceanQuality quality, double cameraX, double cameraZ, double renderDistanceScale) {
        if (quality == null || !Double.isFinite(cameraX) || !Double.isFinite(cameraZ)
            || !Double.isFinite(renderDistanceScale) || renderDistanceScale <= 0.0) {
            throw new IllegalArgumentException("quality, finite camera coordinates, and a positive render scale are required");
        }

        int fineStep = quality.gridStepBlocks();
        int radius = Math.max(fineStep * 3 + 2, (int) Math.round(quality.renderRadiusBlocks() * renderDistanceScale));
        int middleStep = fineStep * 2;
        int farStep = fineStep * 4;

        int nearRadius = Math.max(fineStep * 2, snapDown(radius / 3, fineStep));
        int middleRadius = Math.max(nearRadius + middleStep, snapDown((radius * 2) / 3, middleStep));
        middleRadius = Math.min(middleRadius, radius - 1);

        List<Ring> rings = List.of(
            new Ring(0, nearRadius, fineStep),
            new Ring(nearRadius, middleRadius, middleStep),
            new Ring(middleRadius, radius, farStep)
        );

        double originX = Math.floor(cameraX / farStep) * farStep;
        double originZ = Math.floor(cameraZ / farStep) * farStep;
        return new Plan(quality, originX, originZ, rings, quality.visualWaveComponents());
    }

    private static int snapDown(int value, int step) {
        return Math.max(step, (value / step) * step);
    }

    public record Ring(int innerRadiusBlocks, int outerRadiusBlocks, int gridStepBlocks) {
        public Ring {
            if (innerRadiusBlocks < 0 || outerRadiusBlocks <= innerRadiusBlocks || gridStepBlocks <= 0) {
                throw new IllegalArgumentException("invalid LOD ring");
            }
        }
    }

    public record Plan(
        OceanQuality quality,
        double originX,
        double originZ,
        List<Ring> rings,
        int visualWaveComponents
    ) {
        public Plan {
            if (quality == null || rings == null || rings.isEmpty()) {
                throw new IllegalArgumentException("quality and rings are required");
            }
            rings = List.copyOf(rings);
        }
    }
}