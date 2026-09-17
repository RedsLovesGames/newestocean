package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;

/**
 * Distance-based scheduling for expensive vessel/ocean solves.
 * Stable displacement vessels may solve less often, while player-controlled and unstable motion
 * states always remain at the full 20 Hz server rate.
 */
public final class VesselPhysicsLod {
    public static final double FULL_RATE_RADIUS = 48.0;
    public static final double TEN_HZ_RADIUS = 96.0;
    public static final double FIVE_HZ_RADIUS = 192.0;

    private VesselPhysicsLod() {
    }

    public static int updateIntervalTicks(
        double nearestPlayerDistance,
        boolean playerControlled,
        VesselMotionController.Mode mode
    ) {
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
        if (Double.isNaN(nearestPlayerDistance) || nearestPlayerDistance < 0.0) {
            throw new IllegalArgumentException("nearestPlayerDistance must be non-negative");
        }

        if (playerControlled || mode != VesselMotionController.Mode.DISPLACEMENT) {
            return 1;
        }
        if (nearestPlayerDistance <= FULL_RATE_RADIUS) {
            return 1;
        }
        if (nearestPlayerDistance <= TEN_HZ_RADIUS) {
            return 2;
        }
        if (nearestPlayerDistance <= FIVE_HZ_RADIUS) {
            return 4;
        }
        return 10;
    }

    public static boolean isSolveTick(long worldTick, int entityId, int intervalTicks) {
        if (intervalTicks <= 0) {
            throw new IllegalArgumentException("intervalTicks must be positive");
        }
        if (intervalTicks == 1) {
            return true;
        }
        return Math.floorMod(worldTick + entityId, (long) intervalTicks) == 0L;
    }

    /**
     * Smooths the cached water-force correction between sparse ocean solves.
     * Full-rate retargets apply immediately; lower-rate targets are reached over their solve interval.
     */
    public static final class ForceInterpolator {
        private Vec3 current = Vec3.ZERO;
        private Vec3 start = Vec3.ZERO;
        private Vec3 target = Vec3.ZERO;
        private int durationTicks = 1;
        private int elapsedTicks = 1;

        public void snap(Vec3 force) {
            requireFinite(force);
            current = force;
            start = force;
            target = force;
            durationTicks = 1;
            elapsedTicks = 1;
        }

        public void retarget(Vec3 force, int intervalTicks) {
            requireFinite(force);
            if (intervalTicks <= 0) {
                throw new IllegalArgumentException("intervalTicks must be positive");
            }
            start = current;
            target = force;
            durationTicks = intervalTicks;
            elapsedTicks = 0;
        }

        public Vec3 next() {
            if (elapsedTicks < durationTicks) {
                elapsedTicks++;
            }
            double alpha = durationTicks <= 1 ? 1.0 : elapsedTicks / (double) durationTicks;
            current = lerp(start, target, alpha);
            return current;
        }

        public Vec3 current() {
            return current;
        }

        private static Vec3 lerp(Vec3 from, Vec3 to, double alpha) {
            return from.add(to.subtract(from).multiply(alpha));
        }

        private static void requireFinite(Vec3 value) {
            if (value == null
                || !Double.isFinite(value.x())
                || !Double.isFinite(value.y())
                || !Double.isFinite(value.z())) {
                throw new IllegalArgumentException("force must be finite");
            }
        }
    }
}
