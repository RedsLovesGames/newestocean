package com.redslovesgames.newestocean.client;

/**
 * Visual-only quality tiers. Physics never reads these values, so lowering graphics quality cannot
 * change server-authoritative vessel motion.
 */
public enum OceanQuality {
    POTATO(4, 28, 4),
    LOW(6, 40, 3),
    MEDIUM(10, 56, 2),
    HIGH(14, 72, 1),
    ULTRA(24, 96, 1);

    private final int visualWaveComponents;
    private final int renderRadiusBlocks;
    private final int gridStepBlocks;

    OceanQuality(int visualWaveComponents, int renderRadiusBlocks, int gridStepBlocks) {
        this.visualWaveComponents = visualWaveComponents;
        this.renderRadiusBlocks = renderRadiusBlocks;
        this.gridStepBlocks = gridStepBlocks;
    }

    public int visualWaveComponents() {
        return visualWaveComponents;
    }

    public int renderRadiusBlocks() {
        return renderRadiusBlocks;
    }

    public int gridStepBlocks() {
        return gridStepBlocks;
    }

    public OceanQuality lower() {
        int index = ordinal();
        return index == 0 ? this : values()[index - 1];
    }

    public OceanQuality higher() {
        int index = ordinal();
        return index == values().length - 1 ? this : values()[index + 1];
    }
}
