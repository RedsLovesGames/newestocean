package com.redslovesgames.newestocean.client.config;

import com.redslovesgames.newestocean.client.OceanQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanClientConfigTest {
    @Test
    void defaultsPreserveCurrentRenderingBehavior() {
        OceanClientConfig config = OceanClientConfig.defaults();
        assertTrue(config.oceanRenderingEnabled());
        assertEquals(OceanQuality.MEDIUM, config.quality());
        assertTrue(config.adaptiveQualityEnabled());
        assertEquals(60, config.targetFps());
        assertEquals(OceanQuality.POTATO, config.adaptiveMinQuality());
        assertEquals(OceanQuality.ULTRA, config.adaptiveMaxQuality());
        assertEquals(0, config.visualWaveOverride());
        assertEquals(1.0, config.renderDistanceScale(), 1.0e-9);
        assertEquals(1.0, config.oceanOpacity(), 1.0e-9);
        assertTrue(config.whitecapsEnabled());
        assertTrue(config.wakesEnabled());
        assertTrue(config.shorelineEnabled());
        assertTrue(config.customShadersEnabled());
        assertEquals(18, config.depthsVisualWaveCap());
        assertEquals(0.58, config.depthsOceanAlpha(), 1.0e-9);
        assertFalse(config.diagnosticsOverlay());
    }

    @Test
    void sanitizeClampsUnsafeValuesAndOrdersAdaptiveBounds() {
        OceanClientConfig config = OceanClientConfig.defaults();
        config.setTargetFps(999);
        config.setAdaptiveMinQuality(OceanQuality.ULTRA);
        config.setAdaptiveMaxQuality(OceanQuality.LOW);
        config.setVisualWaveOverride(99);
        config.setRenderDistanceScale(9.0);
        config.setOceanOpacity(-4.0);
        config.setWhitecapIntensity(8.0);
        config.setWakeIntensity(-1.0);
        config.setShorelineIntensity(9.0);
        config.setDepthsVisualWaveCap(0);
        config.setDepthsOceanAlpha(5.0);
        config.setDepthsWhitecapMultiplier(-3.0);
        config.sanitize();

        assertEquals(240, config.targetFps());
        assertEquals(OceanQuality.LOW, config.adaptiveMinQuality());
        assertEquals(OceanQuality.ULTRA, config.adaptiveMaxQuality());
        assertEquals(24, config.visualWaveOverride());
        assertEquals(2.0, config.renderDistanceScale(), 1.0e-9);
        assertEquals(0.25, config.oceanOpacity(), 1.0e-9);
        assertEquals(2.0, config.whitecapIntensity(), 1.0e-9);
        assertEquals(0.0, config.wakeIntensity(), 1.0e-9);
        assertEquals(2.0, config.shorelineIntensity(), 1.0e-9);
        assertEquals(1, config.depthsVisualWaveCap());
        assertEquals(1.0, config.depthsOceanAlpha(), 1.0e-9);
        assertEquals(0.0, config.depthsWhitecapMultiplier(), 1.0e-9);
    }

    @Test
    void visualWaveOverrideUsesAutoOrExplicitCountAcrossNewRange() {
        OceanClientConfig config = OceanClientConfig.defaults();
        assertEquals(10, config.effectiveVisualWaveComponents(10));
        config.setVisualWaveOverride(24);
        config.sanitize();
        assertEquals(24, config.effectiveVisualWaveComponents(10));
    }

    @Test
    void realWaterQualityPresetsScaleVisualShaderCostOnly() {
        assertEquals(4, OceanQuality.POTATO.visualWaveComponents());
        assertEquals(6, OceanQuality.LOW.visualWaveComponents());
        assertEquals(10, OceanQuality.MEDIUM.visualWaveComponents());
        assertEquals(14, OceanQuality.HIGH.visualWaveComponents());
        assertEquals(24, OceanQuality.ULTRA.visualWaveComponents());
    }

    @Test
    void legacyJsonPreservesOldVisualWaveValues() {
        String legacyJson = """
            {
              "quality": "HIGH",
              "visualWaveOverride": 6,
              "depthsVisualWaveCap": 4,
              "renderDistanceScale": 1.5,
              "oceanOpacity": 0.7
            }
            """;

        OceanClientConfig restored = OceanClientConfigCodec.decode(legacyJson);

        assertEquals(OceanQuality.HIGH, restored.quality());
        assertEquals(6, restored.visualWaveOverride());
        assertEquals(4, restored.depthsVisualWaveCap());
        assertEquals(1.5, restored.renderDistanceScale(), 1.0e-9);
        assertEquals(0.7, restored.oceanOpacity(), 1.0e-9);
    }

    @Test
    void clientJsonNeverDefinesServerAuthoritativePhysicalWaveCount() {
        String json = OceanClientConfigCodec.encode(OceanClientConfig.defaults());
        assertFalse(json.contains("physicalWave"));
        assertFalse(json.contains("serverWave"));
    }

    @Test
    void targetFrameTimeComesFromConfiguredFps() {
        OceanClientConfig config = OceanClientConfig.defaults();
        config.setTargetFps(120);
        assertEquals(1000.0 / 120.0, config.targetFrameMs(), 1.0e-9);
    }

    @Test
    void jsonRoundTripPreservesSettings() {
        OceanClientConfig config = OceanClientConfig.defaults();
        config.setQuality(OceanQuality.HIGH);
        config.setTargetFps(144);
        config.setVisualWaveOverride(18);
        config.setWakesEnabled(false);
        config.setDepthsWakeMultiplier(1.25);
        config.setDiagnosticsOverlay(true);

        String json = OceanClientConfigCodec.encode(config);
        OceanClientConfig restored = OceanClientConfigCodec.decode(json);

        assertEquals(OceanQuality.HIGH, restored.quality());
        assertEquals(144, restored.targetFps());
        assertEquals(18, restored.visualWaveOverride());
        assertFalse(restored.wakesEnabled());
        assertEquals(1.25, restored.depthsWakeMultiplier(), 1.0e-9);
        assertTrue(restored.diagnosticsOverlay());
    }
}
