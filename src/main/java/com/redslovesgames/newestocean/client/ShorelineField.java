package com.redslovesgames.newestocean.client;

/** Immutable shoreline metrics aligned one-for-one with ocean LOD cells. */
public final class ShorelineField {
    private final ShorelineSample[] samples;
    private final int influencedCellCount;

    ShorelineField(ShorelineSample[] samples) {
        if (samples == null) {
            throw new IllegalArgumentException("shoreline samples are required");
        }
        this.samples = samples.clone();
        int influenced = 0;
        for (ShorelineSample sample : this.samples) {
            if (sample == null) {
                throw new IllegalArgumentException("shoreline samples cannot contain null");
            }
            if (sample.shoreInfluence() > 0.0) {
                influenced++;
            }
        }
        this.influencedCellCount = influenced;
    }

    public ShorelineSample sample(int cell) {
        if (cell < 0 || cell >= samples.length) {
            throw new IndexOutOfBoundsException("shoreline cell out of range");
        }
        return samples[cell];
    }

    public int cellCount() {
        return samples.length;
    }

    public boolean hasInfluence(int cell) {
        return sample(cell).shoreInfluence() > 0.0;
    }

    public int influencedCellCount() {
        return influencedCellCount;
    }
}
