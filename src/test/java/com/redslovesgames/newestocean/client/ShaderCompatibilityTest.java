package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.client.config.OceanClientConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderCompatibilityTest {
    @Test
    void noIrisUsesNormalGpu() {
        ShaderCompatibility.Snapshot snapshot = ShaderCompatibility.snapshot(
            new ShaderCompatibility.IrisState(false, true, false, false, null)
        );
        assertEquals(ShaderCompatibility.Mode.NORMAL_GPU, snapshot.mode());
        assertTrue(snapshot.allowCustomShaders());
        assertFalse(snapshot.skipWorldRender());
        assertEquals(24, snapshot.visualWaveComponents(24));
    }

    @Test
    void irisWithShadersDisabledKeepsNormalGpu() {
        ShaderCompatibility.Snapshot snapshot = ShaderCompatibility.snapshot(
            new ShaderCompatibility.IrisState(true, true, false, false, "DEPTHS_ULTRA.zip")
        );
        assertEquals(ShaderCompatibility.Mode.NORMAL_GPU, snapshot.mode());
        assertTrue(snapshot.allowCustomShaders());
    }

    @Test
    void activeGenericIrisPackForcesCpu() {
        ShaderCompatibility.Snapshot snapshot = ShaderCompatibility.snapshot(
            new ShaderCompatibility.IrisState(true, true, true, false, "ComplementaryReimagined_r5.2.zip")
        );
        assertEquals(ShaderCompatibility.Mode.IRIS_GENERIC, snapshot.mode());
        assertFalse(snapshot.allowCustomShaders());
        assertEquals(24, snapshot.visualWaveComponents(24));
        assertEquals(0.72, snapshot.oceanBaseAlpha(), 1.0e-9);
        assertEquals(1.0, snapshot.whitecapMultiplier(), 1.0e-9);
    }

    @Test
    void depthsNamesNormalizeToDepthsProfile() {
        for (String name : List.of("DEPTHS_ULTRA.zip", "DEPTHS ULTRA", "depths-ultra", "depths_ultra")) {
            ShaderCompatibility.Snapshot snapshot = ShaderCompatibility.snapshot(
                new ShaderCompatibility.IrisState(true, true, true, false, name)
            );
            assertEquals(ShaderCompatibility.Mode.IRIS_DEPTHS_ULTRA, snapshot.mode());
            assertFalse(snapshot.allowCustomShaders());
            assertEquals(18, snapshot.visualWaveComponents(24));
            assertEquals(6, snapshot.visualWaveComponents(6));
            assertEquals(3, snapshot.visualWaveComponents(3));
            assertEquals(0.58, snapshot.oceanBaseAlpha(), 1.0e-9);
            assertEquals(0.65, snapshot.whitecapMultiplier(), 1.0e-9);
            assertEquals(0.90, snapshot.wakeMultiplier(), 1.0e-9);
            assertEquals(0.90, snapshot.shorelineMultiplier(), 1.0e-9);
        }
    }

    @Test
    void bridgeFailureFailsSafeToCpu() {
        ShaderCompatibility.Snapshot snapshot = ShaderCompatibility.snapshot(
            new ShaderCompatibility.IrisState(true, false, false, false, null)
        );
        assertEquals(ShaderCompatibility.Mode.IRIS_GENERIC, snapshot.mode());
        assertFalse(snapshot.allowCustomShaders());
    }

    @Test
    void activeShadowPassSkipsWorldRender() {
        ShaderCompatibility.Snapshot snapshot = ShaderCompatibility.snapshot(
            new ShaderCompatibility.IrisState(true, true, true, true, "DEPTHS_ULTRA.zip")
        );
        assertTrue(snapshot.skipWorldRender());
    }

    @Test
    void shadowFlagWithoutActivePackDoesNotSkipNormalRendering() {
        ShaderCompatibility.Snapshot snapshot = ShaderCompatibility.snapshot(
            new ShaderCompatibility.IrisState(true, true, false, true, null)
        );
        assertFalse(snapshot.skipWorldRender());
    }

    @Test
    void userCanDisableCustomShadersWithoutDisablingOceanRendering() {
        OceanClientConfig config = OceanClientConfig.defaults();
        config.setCustomShadersEnabled(false);
        ShaderCompatibility.Snapshot snapshot = ShaderCompatibility.snapshot(
            new ShaderCompatibility.IrisState(false, true, false, false, null),
            config
        );
        assertEquals(ShaderCompatibility.Mode.NORMAL_GPU, snapshot.mode());
        assertFalse(snapshot.allowCustomShaders());
        assertFalse(snapshot.skipWorldRender());
    }

    @Test
    void depthsRuntimeTuningSupportsFullRealWaterWaveRange() {
        OceanClientConfig config = OceanClientConfig.defaults();
        config.setDepthsVisualWaveCap(24);
        config.setDepthsOceanAlpha(0.44);
        config.setDepthsWhitecapMultiplier(0.30);
        config.setDepthsWakeMultiplier(1.25);
        config.setDepthsShorelineMultiplier(1.40);
        ShaderCompatibility.Snapshot snapshot = ShaderCompatibility.snapshot(
            new ShaderCompatibility.IrisState(true, true, true, false, "DEPTHS_ULTRA.zip"),
            config
        );
        assertEquals(24, snapshot.visualWaveComponents(24));
        assertEquals(0.44, snapshot.oceanBaseAlpha(), 1.0e-9);
        assertEquals(0.30, snapshot.whitecapMultiplier(), 1.0e-9);
        assertEquals(1.25, snapshot.wakeMultiplier(), 1.0e-9);
        assertEquals(1.40, snapshot.shorelineMultiplier(), 1.0e-9);
    }
}
