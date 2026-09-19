package com.redslovesgames.newestocean.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps renderer-specific mixins optional. This class must not load Iris or Sodium classes.
 */
public final class NewestOceanMixinPlugin implements IMixinConfigPlugin {
    private static final String IRIS_COMPAT_PACKAGE = ".compat.iris.";
    private static final String SODIUM_COMPAT_PACKAGE = ".compat.sodium.";
    private static final String PINNED_SODIUM_VERSION = "0.8.12";
    private static final String PINNED_IRIS_VERSION = "1.8.14-beta.1";

    static boolean shouldApplyMixin(String mixinClassName, Set<String> loadedMods) {
        if (mixinClassName.contains(IRIS_COMPAT_PACKAGE)) {
            return loadedMods.contains("iris");
        }
        if (mixinClassName.contains(SODIUM_COMPAT_PACKAGE)) {
            return loadedMods.contains("sodium") && !loadedMods.contains("iris");
        }
        return true;
    }

    static boolean shouldApplyMixin(
        String mixinClassName,
        Set<String> loadedMods,
        Map<String, String> loadedVersions
    ) {
        if (!shouldApplyMixin(mixinClassName, loadedMods)) {
            return false;
        }
        if (mixinClassName.contains(IRIS_COMPAT_PACKAGE)) {
            return supportsPinnedIris(loadedVersions.get("iris"))
                && loadedMods.contains("sodium")
                && supportsPinnedSodium(loadedVersions.get("sodium"));
        }
        if (mixinClassName.contains(SODIUM_COMPAT_PACKAGE)) {
            return supportsPinnedSodium(loadedVersions.get("sodium"));
        }
        return true;
    }

    private static boolean supportsPinnedIris(String version) {
        if (version == null) {
            return false;
        }
        String normalized = version.trim();
        return normalized.equals(PINNED_IRIS_VERSION)
            || normalized.startsWith(PINNED_IRIS_VERSION + "+")
            || normalized.startsWith(PINNED_IRIS_VERSION + "-mc1.21.1");
    }

    private static boolean supportsPinnedSodium(String version) {
        if (version == null) {
            return false;
        }
        String normalized = version.trim();
        return normalized.equals(PINNED_SODIUM_VERSION)
            || normalized.startsWith(PINNED_SODIUM_VERSION + "+")
            || normalized.startsWith("mc1.21.1-" + PINNED_SODIUM_VERSION)
            || normalized.startsWith(PINNED_SODIUM_VERSION + "-mc1.21.1");
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        FabricLoader loader = FabricLoader.getInstance();
        Set<String> loadedRendererMods = new HashSet<>(2);
        Map<String, String> loadedVersions = new HashMap<>(2);
        if (loader.isModLoaded("iris")) {
            loadedRendererMods.add("iris");
            loader.getModContainer("iris").ifPresent(container ->
                loadedVersions.put("iris", container.getMetadata().getVersion().getFriendlyString())
            );
        }
        if (loader.isModLoaded("sodium")) {
            loadedRendererMods.add("sodium");
            loader.getModContainer("sodium").ifPresent(container ->
                loadedVersions.put("sodium", container.getMetadata().getVersion().getFriendlyString())
            );
        }
        return shouldApplyMixin(mixinClassName, loadedRendererMods, loadedVersions);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
