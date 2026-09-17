package com.redslovesgames.newestocean.ocean;

/** Stable derivation of the public ocean seed from the Minecraft world seed. */
public final class OceanSeed {
    private static final long OCEAN_SALT = 0x4E45574553544F43L;

    private OceanSeed() {
    }

    public static long derive(long worldSeed) {
        long value = worldSeed ^ OCEAN_SALT;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }
}
