package com.redslovesgames.newestocean.client.water;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Shared GLSL source and uniform naming contract for all real-water renderer paths. */
public final class OceanShaderLibrary {
    public static final String MARKER = "// NEWESTOCEAN_WATER_V1";
    public static final String WAVE_COUNT = "newestocean_waveCount";
    public static final String TIME = "newestocean_time";
    public static final String WAVE_SCALE = "newestocean_waveScale";
    public static final String SEA_LEVEL = "newestocean_seaLevel";
    public static final String CAMERA_ORIGIN = "newestocean_cameraOrigin";
    public static final String SHORE_TEXTURE = "newestocean_shoreTexture";
    public static final String SHORE_ORIGIN = "newestocean_shoreOrigin";
    public static final String SHORE_SCALE = "newestocean_shoreScale";
    public static final String STILL_WATER_BOUNDS = "newestocean_waterStillBounds";
    public static final String FLOWING_WATER_BOUNDS = "newestocean_waterFlowingBounds";
    public static final String FOAM_STRENGTH = "newestocean_foamStrength";

    private static final String RESOURCE = "/assets/newestocean/shaders/include/newestocean_water.glsl";
    private static final String WAVE_A_PREFIX = "newestocean_waveA";
    private static final String WAVE_B_PREFIX = "newestocean_waveB";

    private OceanShaderLibrary() {
    }

    public static String source() {
        return SourceHolder.SOURCE;
    }

    public static String waveAUniform(int index) {
        validateWaveIndex(index);
        return WAVE_A_PREFIX + index;
    }

    public static String waveBUniform(int index) {
        validateWaveIndex(index);
        return WAVE_B_PREFIX + index;
    }

    /**
     * Returns whether a missing uniform makes geometric water displacement unsafe.
     * Material-only inputs may be optimized away by shaderpacks without disabling
     * otherwise valid displaced water.
     */
    public static boolean requiredForDisplacement(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.startsWith(WAVE_A_PREFIX) || name.startsWith(WAVE_B_PREFIX)) {
            return true;
        }
        return name.equals(WAVE_COUNT)
            || name.equals(TIME)
            || name.equals(WAVE_SCALE)
            || name.equals(CAMERA_ORIGIN)
            || name.equals(SHORE_TEXTURE)
            || name.equals(SHORE_ORIGIN)
            || name.equals(SHORE_SCALE)
            || name.equals(STILL_WATER_BOUNDS)
            || name.equals(FLOWING_WATER_BOUNDS);
    }

    public static boolean missingUniformBreaksDisplacement(String name, int location) {
        return location < 0 && requiredForDisplacement(name);
    }

    private static void validateWaveIndex(int index) {
        if (index < 0 || index >= OceanWaterWaveData.MAX_COMPONENTS) {
            throw new IllegalArgumentException(
                "wave index must be between 0 and " + (OceanWaterWaveData.MAX_COMPONENTS - 1)
            );
        }
    }

    private static String loadSource() {
        try (InputStream stream = OceanShaderLibrary.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("missing shader library resource: " + RESOURCE);
            }
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (!source.contains(MARKER)) {
                throw new IllegalStateException("shader library is missing idempotence marker " + MARKER);
            }
            return source;
        } catch (IOException exception) {
            throw new IllegalStateException("failed to load shader library resource: " + RESOURCE, exception);
        }
    }

    private static final class SourceHolder {
        private static final String SOURCE = loadSource();
    }
}
