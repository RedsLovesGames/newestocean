package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17ConfigIntegrationContractTest {
    @Test
    void configManagerPersistsAndAppliesClientSettings() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/config/OceanConfigManager.java"
        ));
        assertTrue(source.contains("getConfigDir()"));
        assertTrue(source.contains("newestocean-client.json"));
        assertTrue(source.contains("saveAndApply"));
        assertTrue(source.contains("OceanWorldRenderer.applyConfig"));
    }

    @Test
    void clothConfigScreenExposesActiveRealWaterSettings() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/config/NewestOceanConfigScreen.java"
        ));
        assertTrue(source.contains("ConfigBuilder.create()"));
        for (String setter : new String[] {
            "setOceanRenderingEnabled", "setQuality", "setAdaptiveQualityEnabled", "setTargetFps",
            "setAdaptiveMinQuality", "setAdaptiveMaxQuality", "setVisualWaveOverride",
            "setWhitecapsEnabled", "setWhitecapIntensity", "setWakesEnabled", "setWakeIntensity",
            "setShorelineEnabled", "setShorelineIntensity", "setCustomShadersEnabled",
            "setDepthsVisualWaveCap", "setDepthsWhitecapMultiplier", "setDepthsWakeMultiplier",
            "setDepthsShorelineMultiplier", "setDiagnosticsOverlay"
        }) {
            assertTrue(source.contains(setter), setter + " must be exposed in the config screen");
        }
        assertTrue(source.contains("working.visualWaveOverride(), 0, OceanClientConfig.MAX_VISUAL_WAVES"));
        assertTrue(source.contains("working.depthsVisualWaveCap(), 1, OceanClientConfig.MAX_VISUAL_WAVES"));
        assertFalse(source.contains("Text.literal(\"Render Distance Scale\")"));
        assertFalse(source.contains("Text.literal(\"Ocean Opacity\")"));
        assertFalse(source.contains("Text.literal(\"DEPTHS ULTRA Ocean Alpha\")"));
        assertTrue(source.contains("OceanConfigManager.saveAndApply"));
    }

    @Test
    void modMenuEntrypointUsesClothConfigFactory() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/config/NewestOceanModMenu.java"
        ));
        assertTrue(source.contains("implements ModMenuApi"));
        assertTrue(source.contains("getModConfigScreenFactory"));
        assertTrue(source.contains("NewestOceanConfigScreen::create"));

        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        assertTrue(metadata.contains("\"modmenu\""));
        assertTrue(metadata.contains("\"cloth-config\""));
    }

    @Test
    void clientConfigDoesNotLeakIntoServerPhysicsPackages() throws IOException {
        Path root = Path.of("src/main/java/com/redslovesgames/newestocean");
        try (var files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.toString().contains("/client/") && !path.toString().contains("\\client\\"))
                .forEach(path -> {
                    try {
                        String source = Files.readString(path);
                        assertFalse(source.contains("OceanClientConfig"), path + " must not depend on client config");
                        assertFalse(source.contains("OceanConfigManager"), path + " must not depend on client config manager");
                    } catch (IOException error) {
                        throw new RuntimeException(error);
                    }
                });
        }
    }
}
