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
        if (!synchronizedOcean) {
            return Optional.empty();
        }
        if (quality == null
            || !Double.isFinite(cameraX)
            || !Double.isFinite(cameraZ)
            || !Double.isFinite(tickDelta)
            || !Double.isFinite(baseWaterHeight)
            || !Double.isFinite(rainGradient)
            || !Double.isFinite(thunderGradient)) {
            throw new IllegalArgumentException("render-frame inputs must be finite and quality is required");
        }

        double timeSeconds = (worldTimeTicks + tickDelta) / 20.0;
        OceanLodPlanner.Plan plan = OceanLodPlanner.plan(quality, cameraX, cameraZ);
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
