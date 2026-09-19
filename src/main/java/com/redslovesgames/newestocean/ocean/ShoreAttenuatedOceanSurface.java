package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;

/** Applies the shoreline transition to an existing ocean surface without changing its wave model. */
public final class ShoreAttenuatedOceanSurface implements OceanSurface {
    private final OceanSurface delegate;
    private final ShoreDistanceProvider shoreDistance;

    public ShoreAttenuatedOceanSurface(OceanSurface delegate, ShoreDistanceProvider shoreDistance) {
        if (delegate == null || shoreDistance == null) {
            throw new IllegalArgumentException("delegate and shore distance provider are required");
        }
        this.delegate = delegate;
        this.shoreDistance = shoreDistance;
    }

    @Override
    public SurfaceSample sample(double x, double z, double timeSeconds, OceanConditions conditions) {
        SurfaceSample base = delegate.sample(x, z, timeSeconds, conditions);
        double factor = ShoreAttenuation.factor(shoreDistance.distanceToLand(x, z));
        if (factor >= 1.0) {
            return base;
        }

        Vec3 displacement = base.displacement().multiply(factor);
        double waveHeight = base.height() - conditions.tideOffset();
        double height = conditions.tideOffset() + waveHeight * factor;

        Vec3 orbitalVelocity = base.surfaceVelocity().subtract(conditions.current());
        Vec3 velocity = conditions.current().add(orbitalVelocity.multiply(factor));

        Vec3 blendedNormal = Vec3.UP.multiply(1.0 - factor).add(base.normal().multiply(factor)).normalize();
        if (blendedNormal.lengthSquared() < 1.0e-18) {
            blendedNormal = Vec3.UP;
        } else if (blendedNormal.y() < 0.0) {
            blendedNormal = blendedNormal.multiply(-1.0);
        }

        return new SurfaceSample(height, blendedNormal, velocity, displacement);
    }
}
