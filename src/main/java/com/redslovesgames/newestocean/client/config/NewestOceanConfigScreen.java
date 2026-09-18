package com.redslovesgames.newestocean.client.config;

import com.redslovesgames.newestocean.client.OceanQuality;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Cloth Config screen for every client-side Newest Ocean tuning setting. */
public final class NewestOceanConfigScreen {
    private NewestOceanConfigScreen() {
    }

    public static Screen create(Screen parent) {
        OceanClientConfig working = OceanConfigManager.current().copy();
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("Newest Ocean Settings"));
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        general.addEntry(entries.startBooleanToggle(Text.literal("Ocean Rendering"), working.oceanRenderingEnabled())
            .setDefaultValue(true)
            .setSaveConsumer(working::setOceanRenderingEnabled)
            .build());
        general.addEntry(entries.startEnumSelector(Text.literal("Quality Preset"), OceanQuality.class, working.quality())
            .setDefaultValue(OceanQuality.MEDIUM)
            .setSaveConsumer(working::setQuality)
            .build());
        general.addEntry(entries.startBooleanToggle(Text.literal("Adaptive Quality"), working.adaptiveQualityEnabled())
            .setDefaultValue(true)
            .setSaveConsumer(working::setAdaptiveQualityEnabled)
            .build());
        general.addEntry(entries.startIntSlider(Text.literal("Target FPS"), working.targetFps(), 30, 240)
            .setDefaultValue(60)
            .setSaveConsumer(working::setTargetFps)
            .build());
        general.addEntry(entries.startEnumSelector(Text.literal("Adaptive Minimum Quality"), OceanQuality.class, working.adaptiveMinQuality())
            .setDefaultValue(OceanQuality.POTATO)
            .setSaveConsumer(working::setAdaptiveMinQuality)
            .build());
        general.addEntry(entries.startEnumSelector(Text.literal("Adaptive Maximum Quality"), OceanQuality.class, working.adaptiveMaxQuality())
            .setDefaultValue(OceanQuality.ULTRA)
            .setSaveConsumer(working::setAdaptiveMaxQuality)
            .build());

        ConfigCategory ocean = builder.getOrCreateCategory(Text.literal("Ocean"));
        ocean.addEntry(entries.startIntSlider(Text.literal("Visual Wave Count (0 = Auto)"), working.visualWaveOverride(), 0, 6)
            .setDefaultValue(OceanClientConfig.AUTO_WAVES)
            .setSaveConsumer(working::setVisualWaveOverride)
            .build());
        ocean.addEntry(entries.startDoubleField(Text.literal("Render Distance Scale"), working.renderDistanceScale())
            .setMin(0.50)
            .setMax(2.00)
            .setDefaultValue(1.0)
            .setSaveConsumer(working::setRenderDistanceScale)
            .build());
        ocean.addEntry(entries.startDoubleField(Text.literal("Ocean Opacity"), working.oceanOpacity())
            .setMin(0.25)
            .setMax(1.00)
            .setDefaultValue(1.0)
            .setSaveConsumer(working::setOceanOpacity)
            .build());

        ConfigCategory whitecaps = builder.getOrCreateCategory(Text.literal("Whitecaps"));
        whitecaps.addEntry(entries.startBooleanToggle(Text.literal("Whitecaps Enabled"), working.whitecapsEnabled())
            .setDefaultValue(true)
            .setSaveConsumer(working::setWhitecapsEnabled)
            .build());
        whitecaps.addEntry(entries.startDoubleField(Text.literal("Whitecap Intensity"), working.whitecapIntensity())
            .setMin(0.0)
            .setMax(2.0)
            .setDefaultValue(1.0)
            .setSaveConsumer(working::setWhitecapIntensity)
            .build());

        ConfigCategory wakes = builder.getOrCreateCategory(Text.literal("Vessel Wakes"));
        wakes.addEntry(entries.startBooleanToggle(Text.literal("Vessel Wakes Enabled"), working.wakesEnabled())
            .setDefaultValue(true)
            .setSaveConsumer(working::setWakesEnabled)
            .build());
        wakes.addEntry(entries.startDoubleField(Text.literal("Wake Intensity"), working.wakeIntensity())
            .setMin(0.0)
            .setMax(2.0)
            .setDefaultValue(1.0)
            .setSaveConsumer(working::setWakeIntensity)
            .build());

        ConfigCategory shoreline = builder.getOrCreateCategory(Text.literal("Shoreline"));
        shoreline.addEntry(entries.startBooleanToggle(Text.literal("Shoreline Effects Enabled"), working.shorelineEnabled())
            .setDefaultValue(true)
            .setSaveConsumer(working::setShorelineEnabled)
            .build());
        shoreline.addEntry(entries.startDoubleField(Text.literal("Shoreline Intensity"), working.shorelineIntensity())
            .setMin(0.0)
            .setMax(2.0)
            .setDefaultValue(1.0)
            .setSaveConsumer(working::setShorelineIntensity)
            .build());

        ConfigCategory shaders = builder.getOrCreateCategory(Text.literal("Shader Compatibility"));
        shaders.addEntry(entries.startBooleanToggle(Text.literal("Newest Ocean Custom Shaders"), working.customShadersEnabled())
            .setDefaultValue(true)
            .setSaveConsumer(working::setCustomShadersEnabled)
            .build());
        shaders.addEntry(entries.startIntSlider(Text.literal("DEPTHS ULTRA Visual Wave Cap"), working.depthsVisualWaveCap(), 1, 6)
            .setDefaultValue(4)
            .setSaveConsumer(working::setDepthsVisualWaveCap)
            .build());
        shaders.addEntry(entries.startDoubleField(Text.literal("DEPTHS ULTRA Ocean Alpha"), working.depthsOceanAlpha())
            .setMin(0.25)
            .setMax(1.0)
            .setDefaultValue(0.58)
            .setSaveConsumer(working::setDepthsOceanAlpha)
            .build());
        shaders.addEntry(entries.startDoubleField(Text.literal("DEPTHS ULTRA Whitecap Multiplier"), working.depthsWhitecapMultiplier())
            .setMin(0.0)
            .setMax(2.0)
            .setDefaultValue(0.65)
            .setSaveConsumer(working::setDepthsWhitecapMultiplier)
            .build());
        shaders.addEntry(entries.startDoubleField(Text.literal("DEPTHS ULTRA Wake Multiplier"), working.depthsWakeMultiplier())
            .setMin(0.0)
            .setMax(2.0)
            .setDefaultValue(0.90)
            .setSaveConsumer(working::setDepthsWakeMultiplier)
            .build());
        shaders.addEntry(entries.startDoubleField(Text.literal("DEPTHS ULTRA Shoreline Multiplier"), working.depthsShorelineMultiplier())
            .setMin(0.0)
            .setMax(2.0)
            .setDefaultValue(0.90)
            .setSaveConsumer(working::setDepthsShorelineMultiplier)
            .build());

        ConfigCategory diagnostics = builder.getOrCreateCategory(Text.literal("Diagnostics"));
        diagnostics.addEntry(entries.startBooleanToggle(Text.literal("Diagnostics Overlay"), working.diagnosticsOverlay())
            .setDefaultValue(false)
            .setSaveConsumer(working::setDiagnosticsOverlay)
            .build());

        builder.setSavingRunnable(() -> OceanConfigManager.saveAndApply(working));
        return builder.build();
    }
}
