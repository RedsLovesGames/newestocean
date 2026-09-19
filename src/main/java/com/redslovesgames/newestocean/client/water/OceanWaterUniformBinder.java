package com.redslovesgames.newestocean.client.water;

import com.redslovesgames.newestocean.client.shore.ShoreDistanceTexture;
import com.redslovesgames.newestocean.client.shore.WaterSpriteBounds;
import com.redslovesgames.newestocean.math.Vec3;

/**
 * Renderer-neutral binding contract for the real-water shader uniforms.
 * Renderer adapters provide a {@link Sink} backed by their own program/uniform API.
 */
public final class OceanWaterUniformBinder {
    private OceanWaterUniformBinder() {
    }

    public static void bind(
        OceanWaterFrameState frame,
        ShoreDistanceTexture.Payload shore,
        Object shoreTextureHandle,
        WaterSpriteBounds waterBounds,
        Vec3 cameraOrigin,
        double foamStrength,
        Sink sink
    ) {
        if (frame == null || shore == null || shoreTextureHandle == null || waterBounds == null
            || cameraOrigin == null || sink == null) {
            throw new IllegalArgumentException("frame, shore texture, water bounds, camera origin, and sink are required");
        }
        if (!Double.isFinite(foamStrength) || foamStrength < 0.0) {
            throw new IllegalArgumentException("foamStrength must be finite and non-negative");
        }

        sink.setInt(OceanShaderLibrary.WAVE_COUNT, frame.visualWaveCount());
        sink.setFloat(OceanShaderLibrary.TIME, (float) frame.timeSeconds());
        sink.setFloat(OceanShaderLibrary.WAVE_SCALE, (float) frame.conditions().waveScale());
        sink.setFloat(OceanShaderLibrary.SEA_LEVEL, (float) frame.seaLevel());
        sink.setFloat(OceanShaderLibrary.FOAM_STRENGTH, (float) foamStrength);
        sink.setVec3(
            OceanShaderLibrary.CAMERA_ORIGIN,
            (float) cameraOrigin.x(),
            (float) cameraOrigin.y(),
            (float) cameraOrigin.z()
        );
        sink.setVec2(OceanShaderLibrary.SHORE_ORIGIN, shore.originX(), shore.originZ());
        sink.setVec2(
            OceanShaderLibrary.SHORE_SCALE,
            (float) shore.texelScaleX(),
            (float) shore.texelScaleZ()
        );

        float[] still = waterBounds.stillUniform();
        float[] flowing = waterBounds.flowingUniform();
        sink.setVec4(OceanShaderLibrary.STILL_WATER_BOUNDS, still[0], still[1], still[2], still[3]);
        sink.setVec4(OceanShaderLibrary.FLOWING_WATER_BOUNDS, flowing[0], flowing[1], flowing[2], flowing[3]);
        sink.bindSampler(OceanShaderLibrary.SHORE_TEXTURE, shoreTextureHandle);

        for (int i = 0; i < OceanWaterWaveData.MAX_COMPONENTS; i++) {
            OceanWaterWaveData.PackedWave packed = frame.waveData().wave(i);
            OceanWaterWaveData.WaveA a = packed.a();
            OceanWaterWaveData.WaveB b = packed.b();
            sink.setVec4(
                OceanShaderLibrary.waveAUniform(i),
                a.directionX(),
                a.directionZ(),
                a.amplitude(),
                a.waveNumber()
            );
            sink.setVec4(
                OceanShaderLibrary.waveBUniform(i),
                b.angularFrequency(),
                b.phase(),
                b.steepness(),
                b.auxiliary()
            );
        }
    }

    public interface Sink {
        void setInt(String name, int value);

        void setFloat(String name, float value);

        void setVec2(String name, float x, float y);

        void setVec3(String name, float x, float y, float z);

        void setVec4(String name, float x, float y, float z, float w);

        void bindSampler(String name, Object handle);
    }
}
