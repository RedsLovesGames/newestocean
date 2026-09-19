package com.redslovesgames.newestocean.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps renderer-specific mixins optional. This class must not load Iris or Sodium classes.
 */
public final class NewestOceanMixinPlugin implements IMixinConfigPlugin {
    private static final String IRIS_COMPAT_PACKAGE = ".compat.iris.";
    private static final String SODIUM_COMPAT_PACKAGE = ".compat.sodium.";

    static boolean shouldApplyMixin(String mixinClassName, Set<String> loadedMods) {
        if (mixinClassName.contains(IRIS_COMPAT_PACKAGE)) {
            return loadedMods.contains("iris");
        }
        if (mixinClassName.contains(SODIUM_COMPAT_PACKAGE)) {
            return loadedMods.contains("sodium") && !loadedMods.contains("iris");
        }
        return true;
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
        if (loader.isModLoaded("iris")) {
            loadedRendererMods.add("iris");
        }
        if (loader.isModLoaded("sodium")) {
            loadedRendererMods.add("sodium");
        }
        return shouldApplyMixin(mixinClassName, loadedRendererMods);
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
