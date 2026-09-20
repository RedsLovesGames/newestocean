package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.client.config.OceanClientConfig;
import com.redslovesgames.newestocean.client.config.OceanConfigManager;

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

    public record Snapshot(
        Mode mode,
        boolean skipWorldRender,
        boolean customShadersEnabled,
        int depthsVisualWaveCap,
        double depthsOceanAlpha,
        double depthsWhitecapMultiplier,
        double depthsWakeMultiplier,
        double depthsShorelineMultiplier
    ) {
        public Snapshot {
            if (mode == null) throw new IllegalArgumentException("compatibility mode is required");
            if (depthsVisualWaveCap < 1 || depthsVisualWaveCap > OceanClientConfig.MAX_VISUAL_WAVES) {
                throw new IllegalArgumentException("DEPTHS visual wave cap must be 1-" + OceanClientConfig.MAX_VISUAL_WAVES);
            }
        }

        public boolean allowCustomShaders() {
            return customShadersEnabled && mode == Mode.NORMAL_GPU;
        }

        public int visualWaveComponents(int requested) {
            if (requested < 0) {
                throw new IllegalArgumentException("requested visual wave count cannot be negative");
            }
            return mode == Mode.IRIS_DEPTHS_ULTRA ? Math.min(requested, depthsVisualWaveCap) : requested;
        }

        public double oceanBaseAlpha() {
            return mode == Mode.IRIS_DEPTHS_ULTRA ? depthsOceanAlpha : 0.72;
        }

        public double whitecapMultiplier() {
            return mode == Mode.IRIS_DEPTHS_ULTRA ? depthsWhitecapMultiplier : 1.0;
        }

        public double wakeMultiplier() {
            return mode == Mode.IRIS_DEPTHS_ULTRA ? depthsWakeMultiplier : 1.0;
        }

        public double shorelineMultiplier() {
            return mode == Mode.IRIS_DEPTHS_ULTRA ? depthsShorelineMultiplier : 1.0;
        }
    }

    private ShaderCompatibility() {
    }

    public static Snapshot current() {
        return current(OceanConfigManager.current());
    }

    public static Snapshot current(OceanClientConfig config) {
        return snapshot(IrisCompatibilityBridge.currentState(), config);
    }

    static Snapshot snapshot(IrisState state) {
        return snapshot(state, OceanClientConfig.defaults());
    }

    static Snapshot snapshot(IrisState state, OceanClientConfig config) {
        if (state == null || config == null) {
            throw new IllegalArgumentException("Iris compatibility state and config are required");
        }
        OceanClientConfig safe = config.copy().sanitize();

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
        return new Snapshot(
            mode,
            skip,
            safe.customShadersEnabled(),
            safe.depthsVisualWaveCap(),
            safe.depthsOceanAlpha(),
            safe.depthsWhitecapMultiplier(),
            safe.depthsWakeMultiplier(),
            safe.depthsShorelineMultiplier()
        );
    }

    static boolean isDepthsUltra(String shaderPackName) {
        if (shaderPackName == null || shaderPackName.isBlank()) return false;
        String normalized = shaderPackName.toLowerCase(Locale.ROOT).trim();
        if (normalized.endsWith(".zip")) normalized = normalized.substring(0, normalized.length() - 4);
        normalized = normalized.replace(" ", "").replace("_", "").replace("-", "");
        return normalized.contains("depthsultra");
    }
}
