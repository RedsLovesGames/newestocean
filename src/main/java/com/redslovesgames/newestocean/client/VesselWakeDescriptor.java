package com.redslovesgames.newestocean.client;

/** Immutable visual-only snapshot of one point in a vessel wake trail. */
public record VesselWakeDescriptor(
    double x,
    double z,
    double directionX,
    double directionZ,
    double speedBlocksPerTick,
    double beamBlocks,
    double ageSeconds,
    double strength
) {
    public VesselWakeDescriptor {
        if (!Double.isFinite(x)
            || !Double.isFinite(z)
            || !Double.isFinite(directionX)
            || !Double.isFinite(directionZ)
            || !Double.isFinite(speedBlocksPerTick)
            || !Double.isFinite(beamBlocks)
            || !Double.isFinite(ageSeconds)
            || !Double.isFinite(strength)) {
            throw new IllegalArgumentException("wake descriptor values must be finite");
        }
        double directionLength = Math.hypot(directionX, directionZ);
        if (Math.abs(directionLength - 1.0) > 1.0e-6) {
            throw new IllegalArgumentException("wake direction must be normalized");
        }
        if (speedBlocksPerTick < 0.0 || beamBlocks <= 0.0 || ageSeconds < 0.0 || strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("wake descriptor values are out of range");
        }
    }
}
