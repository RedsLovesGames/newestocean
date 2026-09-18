package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanEnvironment;

import java.util.Optional;

/** Pure preparation step for one client ocean render frame. */
public final class OceanRenderFrame {
    private OceanRenderFrame() {
    }

    public static Optional<Frame> prepare(
        boolean synchronizedOcean,
        OceanQuality quality,
        double cameraX,
        double cameraZ,
        long worldTimeTicks,
        double tickDelta,
        double baseWaterHeight,
        double rainGradient,
        double thunderGradient,
        long oceanSeed
    ) {
        return prepare(
            synchronizedOcean, quality, cameraX, cameraZ, worldTimeTicks, tickDelta,
            baseWaterHeight, rainGradient, thunderGradient, oceanSeed, 1.0
        );
    }

    public static Optional<Frame> prepare(
        boolean synchronizedOcean,
        OceanQuality quality,
        double cameraX,
        double cameraZ,
        long worldTimeTicks,
        double tickDelta,
        double baseWaterHeight,
        double rainGradient,
        double thunderGradient,
        long oceanSeed,
        double renderDistanceScale
    ) {
        if (!synchronizedOcean) {
            return Optional.empty();
        }
        if (quality == null
            || !Double.isFinite(cameraX)
            || !Double.isFinite(cameraZ)
            || !Double.isFinite(tickDelta)
            || !Double.isFinite(baseWaterHeight)
            || !Double.isFinite(rainGradient)
            || !Double.isFinite(thunderGradient)
            || !Double.isFinite(renderDistanceScale)
            || renderDistanceScale <= 0.0) {
            throw new IllegalArgumentException("render-frame inputs must be finite, render scale positive, and quality required");
        }

        double timeSeconds = (worldTimeTicks + tickDelta) / 20.0;
        OceanLodPlanner.Plan plan = OceanLodPlanner.plan(quality, cameraX, cameraZ, renderDistanceScale);
        OceanConditions conditions = OceanEnvironment.conditions(
            oceanSeed,
            baseWaterHeight,
            timeSeconds,
            rainGradient,
            thunderGradient
        );
        return Optional.of(new Frame(plan, timeSeconds, conditions));
    }

    public record Frame(
        OceanLodPlanner.Plan plan,
        double timeSeconds,
        OceanConditions conditions
    ) {
        public Frame {
            if (plan == null || conditions == null || !Double.isFinite(timeSeconds)) {
                throw new IllegalArgumentException("frame requires a plan, finite time, and conditions");
            }
        }
    }
}