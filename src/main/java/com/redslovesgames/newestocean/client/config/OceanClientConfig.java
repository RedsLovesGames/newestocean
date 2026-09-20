package com.redslovesgames.newestocean.client.config;

import com.redslovesgames.newestocean.client.OceanQuality;

/** Client-only rendering and performance settings. Physical ocean behavior never reads this class. */
public final class OceanClientConfig {
    public static final int AUTO_WAVES = 0;
    public static final int MAX_VISUAL_WAVES = 24;

    private boolean oceanRenderingEnabled = true;
    private OceanQuality quality = OceanQuality.MEDIUM;
    private boolean adaptiveQualityEnabled = true;
    private int targetFps = 60;
    private OceanQuality adaptiveMinQuality = OceanQuality.POTATO;
    private OceanQuality adaptiveMaxQuality = OceanQuality.ULTRA;
    private int visualWaveOverride = AUTO_WAVES;
    private double renderDistanceScale = 1.0;
    private double oceanOpacity = 1.0;
    private boolean whitecapsEnabled = true;
    private double whitecapIntensity = 1.0;
    private boolean wakesEnabled = true;
    private double wakeIntensity = 1.0;
    private boolean shorelineEnabled = true;
    private double shorelineIntensity = 1.0;
    private boolean customShadersEnabled = true;
    private int depthsVisualWaveCap = 18;
    private double depthsOceanAlpha = 0.58;
    private double depthsWhitecapMultiplier = 0.65;
    private double depthsWakeMultiplier = 0.90;
    private double depthsShorelineMultiplier = 0.90;
    private boolean diagnosticsOverlay;

    public static OceanClientConfig defaults() {
        return new OceanClientConfig();
    }

    public OceanClientConfig copy() {
        OceanClientConfig copy = new OceanClientConfig();
        copy.oceanRenderingEnabled = oceanRenderingEnabled;
        copy.quality = quality;
        copy.adaptiveQualityEnabled = adaptiveQualityEnabled;
        copy.targetFps = targetFps;
        copy.adaptiveMinQuality = adaptiveMinQuality;
        copy.adaptiveMaxQuality = adaptiveMaxQuality;
        copy.visualWaveOverride = visualWaveOverride;
        copy.renderDistanceScale = renderDistanceScale;
        copy.oceanOpacity = oceanOpacity;
        copy.whitecapsEnabled = whitecapsEnabled;
        copy.whitecapIntensity = whitecapIntensity;
        copy.wakesEnabled = wakesEnabled;
        copy.wakeIntensity = wakeIntensity;
        copy.shorelineEnabled = shorelineEnabled;
        copy.shorelineIntensity = shorelineIntensity;
        copy.customShadersEnabled = customShadersEnabled;
        copy.depthsVisualWaveCap = depthsVisualWaveCap;
        copy.depthsOceanAlpha = depthsOceanAlpha;
        copy.depthsWhitecapMultiplier = depthsWhitecapMultiplier;
        copy.depthsWakeMultiplier = depthsWakeMultiplier;
        copy.depthsShorelineMultiplier = depthsShorelineMultiplier;
        copy.diagnosticsOverlay = diagnosticsOverlay;
        return copy;
    }

    public OceanClientConfig sanitize() {
        if (quality == null) quality = OceanQuality.MEDIUM;
        if (adaptiveMinQuality == null) adaptiveMinQuality = OceanQuality.POTATO;
        if (adaptiveMaxQuality == null) adaptiveMaxQuality = OceanQuality.ULTRA;
        if (adaptiveMinQuality.ordinal() > adaptiveMaxQuality.ordinal()) {
            OceanQuality swap = adaptiveMinQuality;
            adaptiveMinQuality = adaptiveMaxQuality;
            adaptiveMaxQuality = swap;
        }
        targetFps = clamp(targetFps, 30, 240);
        visualWaveOverride = clamp(visualWaveOverride, AUTO_WAVES, MAX_VISUAL_WAVES);
        renderDistanceScale = clampFinite(renderDistanceScale, 0.50, 2.00, 1.0);
        oceanOpacity = clampFinite(oceanOpacity, 0.25, 1.00, 1.0);
        whitecapIntensity = clampFinite(whitecapIntensity, 0.0, 2.0, 1.0);
        wakeIntensity = clampFinite(wakeIntensity, 0.0, 2.0, 1.0);
        shorelineIntensity = clampFinite(shorelineIntensity, 0.0, 2.0, 1.0);
        depthsVisualWaveCap = clamp(depthsVisualWaveCap, 1, MAX_VISUAL_WAVES);
        depthsOceanAlpha = clampFinite(depthsOceanAlpha, 0.25, 1.0, 0.58);
        depthsWhitecapMultiplier = clampFinite(depthsWhitecapMultiplier, 0.0, 2.0, 0.65);
        depthsWakeMultiplier = clampFinite(depthsWakeMultiplier, 0.0, 2.0, 0.90);
        depthsShorelineMultiplier = clampFinite(depthsShorelineMultiplier, 0.0, 2.0, 0.90);
        return this;
    }

    public int effectiveVisualWaveComponents(int qualityDefault) {
        if (qualityDefault < 0) {
            throw new IllegalArgumentException("quality visual wave count cannot be negative");
        }
        return visualWaveOverride == AUTO_WAVES ? qualityDefault : visualWaveOverride;
    }

    public double targetFrameMs() {
        return 1000.0 / targetFps;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampFinite(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    public boolean oceanRenderingEnabled() { return oceanRenderingEnabled; }
    public void setOceanRenderingEnabled(boolean value) { oceanRenderingEnabled = value; }
    public OceanQuality quality() { return quality; }
    public void setQuality(OceanQuality value) { quality = value; }
    public boolean adaptiveQualityEnabled() { return adaptiveQualityEnabled; }
    public void setAdaptiveQualityEnabled(boolean value) { adaptiveQualityEnabled = value; }
    public int targetFps() { return targetFps; }
    public void setTargetFps(int value) { targetFps = value; }
    public OceanQuality adaptiveMinQuality() { return adaptiveMinQuality; }
    public void setAdaptiveMinQuality(OceanQuality value) { adaptiveMinQuality = value; }
    public OceanQuality adaptiveMaxQuality() { return adaptiveMaxQuality; }
    public void setAdaptiveMaxQuality(OceanQuality value) { adaptiveMaxQuality = value; }
    public int visualWaveOverride() { return visualWaveOverride; }
    public void setVisualWaveOverride(int value) { visualWaveOverride = value; }
    public double renderDistanceScale() { return renderDistanceScale; }
    public void setRenderDistanceScale(double value) { renderDistanceScale = value; }
    public double oceanOpacity() { return oceanOpacity; }
    public void setOceanOpacity(double value) { oceanOpacity = value; }
    public boolean whitecapsEnabled() { return whitecapsEnabled; }
    public void setWhitecapsEnabled(boolean value) { whitecapsEnabled = value; }
    public double whitecapIntensity() { return whitecapIntensity; }
    public void setWhitecapIntensity(double value) { whitecapIntensity = value; }
    public boolean wakesEnabled() { return wakesEnabled; }
    public void setWakesEnabled(boolean value) { wakesEnabled = value; }
    public double wakeIntensity() { return wakeIntensity; }
    public void setWakeIntensity(double value) { wakeIntensity = value; }
    public boolean shorelineEnabled() { return shorelineEnabled; }
    public void setShorelineEnabled(boolean value) { shorelineEnabled = value; }
    public double shorelineIntensity() { return shorelineIntensity; }
    public void setShorelineIntensity(double value) { shorelineIntensity = value; }
    public boolean customShadersEnabled() { return customShadersEnabled; }
    public void setCustomShadersEnabled(boolean value) { customShadersEnabled = value; }
    public int depthsVisualWaveCap() { return depthsVisualWaveCap; }
    public void setDepthsVisualWaveCap(int value) { depthsVisualWaveCap = value; }
    public double depthsOceanAlpha() { return depthsOceanAlpha; }
    public void setDepthsOceanAlpha(double value) { depthsOceanAlpha = value; }
    public double depthsWhitecapMultiplier() { return depthsWhitecapMultiplier; }
    public void setDepthsWhitecapMultiplier(double value) { depthsWhitecapMultiplier = value; }
    public double depthsWakeMultiplier() { return depthsWakeMultiplier; }
    public void setDepthsWakeMultiplier(double value) { depthsWakeMultiplier = value; }
    public double depthsShorelineMultiplier() { return depthsShorelineMultiplier; }
    public void setDepthsShorelineMultiplier(double value) { depthsShorelineMultiplier = value; }
    public boolean diagnosticsOverlay() { return diagnosticsOverlay; }
    public void setDiagnosticsOverlay(boolean value) { diagnosticsOverlay = value; }
}