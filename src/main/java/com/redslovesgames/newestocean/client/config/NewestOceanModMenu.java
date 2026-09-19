package com.redslovesgames.newestocean.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional Mod Menu entrypoint that exposes the Cloth Config screen. */
public final class NewestOceanModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return NewestOceanConfigScreen::create;
    }
}
