package com.redslovesgames.newestocean.client;

/** Immutable visual-only shoreline metrics for one cached ocean cell. */
public record ShorelineSample(
    double depthBlocks,
    double distanceToShoreBlocks,
    double shoreDirectionX,
    double shoreDirectionZ,
    double shoreInfluence
) {
    public static final ShorelineSample NONE = new ShorelineSample(12.0, Double.MAX_VALUE, 0.0, 0.0, 0.0);

    public ShorelineSample {
        if (!Double.isFinite(depthBlocks)
            || !Double.isFinite(distanceToShoreBlocks)
            || !Double.isFinite(shoreDirectionX)
            || !Double.isFinite(shoreDirectionZ)
            || !Double.isFinite(shoreInfluence)) {
            throw new IllegalArgumentException("shoreline sample values must be finite");
        }
        if (depthBlocks < 0.0 || distanceToShoreBlocks < 0.0) {
            throw new IllegalArgumentException("shoreline depth and distance cannot be negative");
        }
        shoreInfluence = Math.max(0.0, Math.min(1.0, shoreInfluence));

        double directionLength = Math.hypot(shoreDirectionX, shoreDirectionZ);
        if (directionLength < 1.0e-9) {
            if (shoreInfluence > 0.0) {
                throw new IllegalArgumentException("influenced shoreline samples require a direction");
            }
            shoreDirectionX = 0.0;
            shoreDirectionZ = 0.0;
        } else {
            shoreDirectionX /= directionLength;
            shoreDirectionZ /= directionLength;
        }
    }
}
