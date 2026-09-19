package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17DiagnosticsContractTest {
    @Test
    void clientLoadsConfigAndRegistersDiagnostics() throws IOException {
        String client = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/NewestOceanClient.java"
        ));
        assertTrue(client.contains("OceanConfigManager.load()"));
        assertTrue(client.contains("OceanDiagnosticsHud.register()"));
    }

    @Test
    void diagnosticsHudReportsEffectiveRuntimeStateOnlyWhenEnabled() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/redslovesgames/newestocean/client/OceanDiagnosticsHud.java"
        ));
        assertTrue(source.contains("HudRenderCallback.EVENT.register"));
        assertTrue(source.contains("diagnosticsOverlay()"));
        assertTrue(source.contains("OceanWorldRenderer.quality()"));
        assertTrue(source.contains("ShaderCompatibility.current"));
        assertTrue(source.contains("effectiveVisualWaveComponents"));
        assertTrue(source.contains("drawTextWithShadow"));
    }
}
