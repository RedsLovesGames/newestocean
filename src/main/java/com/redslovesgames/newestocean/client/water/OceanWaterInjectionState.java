package com.redslovesgames.newestocean.client.water;

/** Immutable diagnostics for real-water source injection and binding. */
public record OceanWaterInjectionState(
    boolean sourcePatchSuccess,
    boolean uniformBindingSuccess,
    boolean shadowPatchSuccess,
    RendererPath activeRendererPath,
    String fallbackReason
) {
    public OceanWaterInjectionState {
        if (activeRendererPath == null) {
            throw new IllegalArgumentException("active renderer path is required");
        }
        fallbackReason = fallbackReason == null ? "" : fallbackReason;
    }

    public boolean fullyInjected() {
        return sourcePatchSuccess && uniformBindingSuccess && shadowPatchSuccess;
    }

    public boolean hasFallback() {
        return !fallbackReason.isBlank();
    }

    public static OceanWaterInjectionState unsupported(RendererPath path, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("unsupported renderer state requires a fallback reason");
        }
        return new OceanWaterInjectionState(false, false, false, path, reason);
    }

    public enum RendererPath {
        VANILLA,
        SODIUM,
        IRIS,
        UNSUPPORTED
    }
}
