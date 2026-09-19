package com.redslovesgames.newestocean.client;

/** Smoothly fades the custom ocean overlay before the finite square LOD boundary. */
public final class OceanSurfaceEdgeFade {
    private static final double FADE_START_FRACTION = 0.72;

    private OceanSurfaceEdgeFade() {
    }

    public static double factor(double localX, double localZ, double outerRadiusBlocks) {
        if (!Double.isFinite(localX) || !Double.isFinite(localZ)
            || !Double.isFinite(outerRadiusBlocks) || outerRadiusBlocks <= 0.0) {
            throw new IllegalArgumentException("finite local coordinates and positive radius are required");
        }

        double distance = Math.max(Math.abs(localX), Math.abs(localZ));
        double fadeStart = outerRadiusBlocks * FADE_START_FRACTION;
        if (distance <= fadeStart) return 1.0;
        if (distance >= outerRadiusBlocks) return 0.0;

        double t = (distance - fadeStart) / (outerRadiusBlocks - fadeStart);
        double smooth = t * t * (3.0 - 2.0 * t);
        return 1.0 - smooth;
    }

    public static int outerRadius(OceanLodPlanner.Plan plan) {
        if (plan == null || plan.rings().isEmpty()) {
            throw new IllegalArgumentException("plan with rings is required");
        }
        return plan.rings().get(plan.rings().size() - 1).outerRadiusBlocks();
    }
}
