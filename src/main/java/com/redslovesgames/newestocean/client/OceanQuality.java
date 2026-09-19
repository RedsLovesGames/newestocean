package com.redslovesgames.newestocean.client;

/**
 * Visual-only quality tiers. Physics never reads these values, so lowering graphics quality cannot
 * change server-authoritative vessel motion.
 */
public enum OceanQuality {
    POTATO(2, 28, 4),
    LOW(3, 40, 3),
    MEDIUM(4, 56, 2),
    HIGH(6, 72, 1),
    ULTRA(6, 96, 1);

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
