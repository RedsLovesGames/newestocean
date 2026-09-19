package com.redslovesgames.newestocean.ocean;

/** Smoothly damps ocean wave motion from the shoreline to full strength 15 blocks offshore. */
public final class ShoreAttenuation {
    public static final double FULL_STRENGTH_DISTANCE = 15.0;

    private ShoreAttenuation() {
    }

    public static double factor(double distanceBlocks) {
        if (!Double.isFinite(distanceBlocks)) {
            throw new IllegalArgumentException("shore distance must be finite");
        }
        double t = Math.max(0.0, Math.min(1.0, distanceBlocks / FULL_STRENGTH_DISTANCE));
        return t * t * (3.0 - 2.0 * t);
    }
}
