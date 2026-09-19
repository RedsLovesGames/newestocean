package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;

/** Deterministic large-scale ocean conditions shared by physics and rendering. */
public final class OceanEnvironment {
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double TIDE_PERIOD_SECONDS = 3.0 * 20.0 * 60.0;
    private static final double TIDE_AMPLITUDE_BLOCKS = 0.18;
    private static final double CURRENT_PERIOD_SECONDS = 7.0 * 20.0 * 60.0;
    private static final double CURRENT_SPEED_BLOCKS_PER_SECOND = 0.16;

    private OceanEnvironment() {
    }

    public static OceanConditions conditions(
        long oceanSeed,
        double baseWaterHeight,
        double timeSeconds,
        double rainGradient,
        double thunderGradient
    ) {
        if (!Double.isFinite(baseWaterHeight) || !Double.isFinite(timeSeconds)) {
            throw new IllegalArgumentException("baseWaterHeight and timeSeconds must be finite");
        }

        double tidePhase = phaseFromSeed(oceanSeed, 0x54494445L);
        double tide = Math.sin(TWO_PI * timeSeconds / TIDE_PERIOD_SECONDS + tidePhase)
            * TIDE_AMPLITUDE_BLOCKS;

        double currentPhase = phaseFromSeed(oceanSeed, 0x43555252L);
        double currentAngle = TWO_PI * timeSeconds / CURRENT_PERIOD_SECONDS + currentPhase;
        double weatherBoost = 1.0 + clamp01(rainGradient) * 0.12 + clamp01(thunderGradient) * 0.28;
        Vec3 current = new Vec3(
            Math.cos(currentAngle) * CURRENT_SPEED_BLOCKS_PER_SECOND * weatherBoost,
            0.0,
            Math.sin(currentAngle) * CURRENT_SPEED_BLOCKS_PER_SECOND * weatherBoost
        );

        return OceanConditions.weather(
            rainGradient,
            thunderGradient,
            baseWaterHeight + tide,
            current
        );
    }

    static double tideOffset(long oceanSeed, double timeSeconds) {
        double tidePhase = phaseFromSeed(oceanSeed, 0x54494445L);
        return Math.sin(TWO_PI * timeSeconds / TIDE_PERIOD_SECONDS + tidePhase)
            * TIDE_AMPLITUDE_BLOCKS;
    }

    static Vec3 current(long oceanSeed, double timeSeconds) {
        double currentPhase = phaseFromSeed(oceanSeed, 0x43555252L);
        double currentAngle = TWO_PI * timeSeconds / CURRENT_PERIOD_SECONDS + currentPhase;
        return new Vec3(
            Math.cos(currentAngle) * CURRENT_SPEED_BLOCKS_PER_SECOND,
            0.0,
            Math.sin(currentAngle) * CURRENT_SPEED_BLOCKS_PER_SECOND
        );
    }

    private static double phaseFromSeed(long seed, long salt) {
        long mixed = OceanSeed.derive(seed ^ salt);
        long mantissa = mixed >>> 11;
        double unit = mantissa * 0x1.0p-53;
        return unit * TWO_PI;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
