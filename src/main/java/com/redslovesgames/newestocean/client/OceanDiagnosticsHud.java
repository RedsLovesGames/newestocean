package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.client.config.OceanClientConfig;
import com.redslovesgames.newestocean.client.config.OceanConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Locale;

/** Lightweight client-only Phase 17 runtime diagnostics overlay. */
public final class OceanDiagnosticsHud {
    private OceanDiagnosticsHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> render(drawContext));
    }

    private static void render(DrawContext drawContext) {
        OceanClientConfig config = OceanConfigManager.current();
        if (!config.diagnosticsOverlay()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        OceanQuality quality = OceanWorldRenderer.quality();
        ShaderCompatibility.Snapshot compatibility = ShaderCompatibility.current(config);
        int requestedWaves = config.effectiveVisualWaveComponents(quality.visualWaveComponents());
        int effectiveWaves = compatibility.visualWaveComponents(requestedWaves);
        int fps = client.getCurrentFps();

        String line = String.format(
            Locale.ROOT,
            "Newest Ocean | FPS %d/%d | Quality %s | Shader %s | Waves %d | Render x%.2f",
            fps,
            config.targetFps(),
            quality.name(),
            compatibility.mode().name(),
            effectiveWaves,
            config.renderDistanceScale()
        );
        drawContext.drawTextWithShadow(client.textRenderer, Text.literal(line), 6, 6, 0xFFFFFFFF);
    }
}
