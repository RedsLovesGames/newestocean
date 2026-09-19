package com.redslovesgames.newestocean.mixin;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewestOceanMixinPluginTest {
    @Test
    void irisCompatibilityMixinRequiresIris() {
        String mixin = "com.redslovesgames.newestocean.mixin.compat.iris.IrisWaterShaderMixin";

        assertFalse(NewestOceanMixinPlugin.shouldApplyMixin(mixin, Set.of()));
        assertTrue(NewestOceanMixinPlugin.shouldApplyMixin(mixin, Set.of("iris")));
    }

    @Test
    void sodiumCompatibilityMixinRequiresPinnedSodiumWithoutIris() {
        String mixin = "com.redslovesgames.newestocean.mixin.compat.sodium.SodiumWaterShaderMixin";

        assertFalse(NewestOceanMixinPlugin.shouldApplyMixin(mixin, Set.of(), Map.of()));
        assertTrue(NewestOceanMixinPlugin.shouldApplyMixin(
            mixin,
            Set.of("sodium"),
            Map.of("sodium", "0.8.12+mc1.21.1")
        ));
        assertFalse(NewestOceanMixinPlugin.shouldApplyMixin(
            mixin,
            Set.of("sodium"),
            Map.of("sodium", "0.8.13+mc1.21.1")
        ));
        assertFalse(NewestOceanMixinPlugin.shouldApplyMixin(
            mixin,
            Set.of("sodium", "iris"),
            Map.of("sodium", "0.8.12+mc1.21.1", "iris", "1.8.14-beta.1+mc1.21.1")
        ));
    }

    @Test
    void ordinaryMixinsAlwaysApply() {
        String mixin = "com.redslovesgames.newestocean.mixin.BoatEntityMixin";

        assertTrue(NewestOceanMixinPlugin.shouldApplyMixin(mixin, Set.of()));
        assertTrue(NewestOceanMixinPlugin.shouldApplyMixin(mixin, Set.of("iris")));
        assertTrue(NewestOceanMixinPlugin.shouldApplyMixin(mixin, Set.of("sodium")));
        assertTrue(NewestOceanMixinPlugin.shouldApplyMixin(mixin, Set.of("iris", "sodium")));
    }

    @Test
    void unrelatedModNamesDoNotEnableCompatibilityMixins() {
        assertFalse(NewestOceanMixinPlugin.shouldApplyMixin(
                "com.redslovesgames.newestocean.mixin.compat.iris.IrisWaterShaderMixin",
                Set.of("sodium", "fabric-api")));
        assertFalse(NewestOceanMixinPlugin.shouldApplyMixin(
                "com.redslovesgames.newestocean.mixin.compat.sodium.SodiumWaterShaderMixin",
                Set.of("iris", "fabric-api")));
    }
}
