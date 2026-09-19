package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;

@FunctionalInterface
public interface OceanSurface {
    SurfaceSample sample(double x, double z, double timeSeconds, OceanConditions conditions);

    record SurfaceSample(
        double height,
        Vec3 normal,
        Vec3 surfaceVelocity,
        Vec3 displacement
    ) {
        public SurfaceSample {
            if (!Double.isFinite(height)) {
                throw new IllegalArgumentException("height must be finite");
            }
            if (normal == null || surfaceVelocity == null || displacement == null) {
                throw new IllegalArgumentException("sample vectors cannot be null");
            }
        }

        /**
         * Migration helper for older callers that only consume horizontal orbital displacement.
         */
        public Vec3 horizontalDisplacement() {
            return new Vec3(displacement.x(), 0.0, displacement.z());
        }
    }
}
