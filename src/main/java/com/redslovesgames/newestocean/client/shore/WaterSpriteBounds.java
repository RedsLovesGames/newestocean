package com.redslovesgames.newestocean.client.shore;

/** Immutable still/flowing water atlas UV bounds shared by every renderer adapter. */
public final class WaterSpriteBounds {
    private final Bounds still;
    private final Bounds flowing;

    public WaterSpriteBounds(Bounds still, Bounds flowing) {
        if (still == null || flowing == null) {
            throw new IllegalArgumentException("still and flowing bounds are required");
        }
        this.still = still;
        this.flowing = flowing;
    }

    public boolean isWater(float u, float v) {
        return still.contains(u, v) || flowing.contains(u, v);
    }

    public Bounds still() {
        return still;
    }

    public Bounds flowing() {
        return flowing;
    }

    /** GLSL vec4 order: minU, minV, maxU, maxV. */
    public float[] stillUniform() {
        return still.uniform();
    }

    /** GLSL vec4 order: minU, minV, maxU, maxV. */
    public float[] flowingUniform() {
        return flowing.uniform();
    }

    public record Bounds(float minU, float minV, float maxU, float maxV) {
        public Bounds {
            requireFinite(minU, "minU");
            requireFinite(minV, "minV");
            requireFinite(maxU, "maxU");
            requireFinite(maxV, "maxV");

            float normalizedMinU = Math.min(minU, maxU);
            float normalizedMaxU = Math.max(minU, maxU);
            float normalizedMinV = Math.min(minV, maxV);
            float normalizedMaxV = Math.max(minV, maxV);
            minU = normalizedMinU;
            minV = normalizedMinV;
            maxU = normalizedMaxU;
            maxV = normalizedMaxV;
        }

        public boolean contains(float u, float v) {
            if (!Float.isFinite(u) || !Float.isFinite(v)) {
                return false;
            }
            return u >= minU && u <= maxU && v >= minV && v <= maxV;
        }

        public float[] uniform() {
            return new float[] {minU, minV, maxU, maxV};
        }

        private static void requireFinite(float value, String name) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(name + " must be finite");
            }
        }
    }
}
