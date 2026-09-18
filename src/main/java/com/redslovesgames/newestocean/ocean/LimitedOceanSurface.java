package com.redslovesgames.newestocean.ocean;

/** OceanSurface view that samples only the first configured components of a ProceduralOcean. */
public final class LimitedOceanSurface implements OceanSurface {
    private final ProceduralOcean ocean;
    private final int componentLimit;

    public LimitedOceanSurface(ProceduralOcean ocean, int componentLimit) {
        if (ocean == null) {
            throw new IllegalArgumentException("ocean is required");
        }
        if (componentLimit < 1 || componentLimit > ocean.componentCount()) {
            throw new IllegalArgumentException("componentLimit must be between 1 and " + ocean.componentCount());
        }
        this.ocean = ocean;
        this.componentLimit = componentLimit;
    }

    @Override
    public SurfaceSample sample(double x, double z, double timeSeconds, OceanConditions conditions) {
        return ocean.sample(x, z, timeSeconds, conditions, componentLimit);
    }

    public int componentLimit() {
        return componentLimit;
    }
}
