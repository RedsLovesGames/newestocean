package com.redslovesgames.newestocean.client.config;

import com.redslovesgames.newestocean.NewestOcean;
import com.redslovesgames.newestocean.client.OceanWorldRenderer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Owns persistence and live application of client-only Newest Ocean settings. */
public final class OceanConfigManager {
    private static final String FILE_NAME = "newestocean-client.json";
    private static volatile OceanClientConfig current = OceanClientConfig.defaults();

    private OceanConfigManager() {
    }

    public static synchronized void load() {
        Path path = configPath();
        OceanClientConfig loaded = OceanClientConfig.defaults();
        if (Files.isRegularFile(path)) {
            try {
                loaded = OceanClientConfigCodec.decode(Files.readString(path, StandardCharsets.UTF_8));
            } catch (IOException error) {
                NewestOcean.LOGGER.warn("Could not read {}; using default client settings.", path, error);
            }
        }
        current = loaded.sanitize();
        OceanWorldRenderer.applyConfig(current);
        if (!Files.exists(path)) {
            write(path, current);
        }
    }

    public static OceanClientConfig current() {
        return current;
    }

    public static synchronized void saveAndApply(OceanClientConfig updated) {
        if (updated == null) {
            throw new IllegalArgumentException("updated config is required");
        }
        OceanClientConfig sanitized = updated.copy().sanitize();
        current = sanitized;
        write(configPath(), sanitized);
        OceanWorldRenderer.applyConfig(sanitized);
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static void write(Path path, OceanClientConfig config) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, OceanClientConfigCodec.encode(config), StandardCharsets.UTF_8);
        } catch (IOException error) {
            NewestOcean.LOGGER.warn("Could not save Newest Ocean client settings to {}.", path, error);
        }
    }
}