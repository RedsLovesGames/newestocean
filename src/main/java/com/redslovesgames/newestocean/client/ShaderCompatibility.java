package com.redslovesgames.newestocean.client;

import java.util.Locale;

/** Central per-frame decision for optional Iris shaderpack compatibility. */
public final class ShaderCompatibility {
    public enum Mode {
        NORMAL_GPU,
        IRIS_GENERIC,
        IRIS_DEPTHS_ULTRA
    }

    public record IrisState(
        boolean irisPresent,
        boolean bridgeResolved,
        boolean shaderPackInUse,
        boolean shadowPass,
        String shaderPackName
    ) {
    }

    public record Snapshot(Mode mode, boolean skipWorldRender) {
        public Snapshot {
            if (mode == null) {
                throw new IllegalArgumentException("compatibility mode is required");
            }
        }

        public boolean allowCustomShaders() {
            return mode == Mode.NORMAL_GPU;
        }

        public int visualWaveComponents(int requested) {
            if (requested < 0) {
                throw new IllegalArgumentException("requested visual wave count cannot be negative");
            }
            return mode == Mode.IRIS_DEPTHS_ULTRA ? Math.min(requested, 4) : requested;
        }

        public double oceanBaseAlpha() {
            return mode == Mode.IRIS_DEPTHS_ULTRA ? 0.58 : 0.72;
        }

        public double whitecapMultiplier() {
            return mode == Mode.IRIS_DEPTHS_ULTRA ? 0.65 : 1.0;
        }

        public double wakeMultiplier() {
            return mode == Mode.IRIS_DEPTHS_ULTRA ? 0.90 : 1.0;
        }

        public double shorelineMultiplier() {
            return mode == Mode.IRIS_DEPTHS_ULTRA ? 0.90 : 1.0;
        }
    }

    private ShaderCompatibility() {
    }

    public static Snapshot current() {
        return snapshot(IrisCompatibilityBridge.currentState());
    }

    static Snapshot snapshot(IrisState state) {
        if (state == null) {
            throw new IllegalArgumentException("Iris compatibility state is required");
        }

        Mode mode;
        if (!state.irisPresent()) {
            mode = Mode.NORMAL_GPU;
        } else if (!state.bridgeResolved()) {
            mode = Mode.IRIS_GENERIC;
        } else if (!state.shaderPackInUse()) {
            mode = Mode.NORMAL_GPU;
        } else if (isDepthsUltra(state.shaderPackName())) {
            mode = Mode.IRIS_DEPTHS_ULTRA;
        } else {
            mode = Mode.IRIS_GENERIC;
        }

        boolean skip = state.irisPresent()
            && state.bridgeResolved()
            && state.shaderPackInUse()
            && state.shadowPass();
        return new Snapshot(mode, skip);
    }

    static boolean isDepthsUltra(String shaderPackName) {
        if (shaderPackName == null || shaderPackName.isBlank()) {
            return false;
        }
        String normalized = shaderPackName.toLowerCase(Locale.ROOT).trim();
        if (normalized.endsWith(".zip")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        normalized = normalized
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "");
        return normalized.contains("depthsultra");
    }
}
