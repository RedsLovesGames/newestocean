package com.redslovesgames.newestocean.client.water;

import com.redslovesgames.newestocean.client.OceanQuality;
import com.redslovesgames.newestocean.client.config.OceanClientConfig;
import com.redslovesgames.newestocean.client.shore.ShoreDistanceTexture;
import com.redslovesgames.newestocean.client.shore.WaterSpriteBounds;
import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class OceanWaterUniformBinderTest {
    @Test
    void bindsFrameWavesShoreTextureCameraAndWaterBoundsThroughSharedSink() {
        OceanWaterRuntime runtime = new OceanWaterRuntime();
        OceanClientConfig config = OceanClientConfig.defaults();
        OceanWaterFrameState frame = runtime.buildFrame(
            12L,
            55L,
            ProceduralOcean.createDefault(55L),
            25.0,
            63.0,
            0.4,
            0.2,
            OceanQuality.MEDIUM,
            config,
            24,
            OceanWaterInjectionState.RendererPath.VANILLA
        );
        ShoreDistanceTexture.Payload shore = new ShoreDistanceTexture.Payload(
            -32, -48, 64, 80, new byte[64 * 80]
        );
        Object textureHandle = new Object();
        WaterSpriteBounds bounds = new WaterSpriteBounds(
            new WaterSpriteBounds.Bounds(0.1F, 0.2F, 0.3F, 0.4F),
            new WaterSpriteBounds.Bounds(0.5F, 0.6F, 0.7F, 0.8F)
        );
        RecordingSink sink = new RecordingSink();

        OceanWaterUniformBinder.bind(
            frame,
            shore,
            textureHandle,
            bounds,
            new Vec3(100.5, 70.0, -220.25),
            0.75,
            sink
        );

        assertEquals(frame.visualWaveCount(), sink.ints.get(OceanShaderLibrary.WAVE_COUNT));
        assertEquals(25.0F, sink.floats.get(OceanShaderLibrary.TIME));
        assertEquals((float) frame.conditions().waveScale(), sink.floats.get(OceanShaderLibrary.WAVE_SCALE));
        assertEquals(63.0F, sink.floats.get(OceanShaderLibrary.SEA_LEVEL));
        assertEquals(0.75F, sink.floats.get(OceanShaderLibrary.FOAM_STRENGTH));
        assertArrayEquals(new float[] {100.5F, 70.0F, -220.25F}, sink.vec3.get(OceanShaderLibrary.CAMERA_ORIGIN));
        assertArrayEquals(new float[] {-32.0F, -48.0F}, sink.vec2.get(OceanShaderLibrary.SHORE_ORIGIN));
        assertArrayEquals(new float[] {1.0F / 64.0F, 1.0F / 80.0F}, sink.vec2.get(OceanShaderLibrary.SHORE_SCALE));
        assertArrayEquals(bounds.stillUniform(), sink.vec4.get(OceanShaderLibrary.STILL_WATER_BOUNDS));
        assertArrayEquals(bounds.flowingUniform(), sink.vec4.get(OceanShaderLibrary.FLOWING_WATER_BOUNDS));
        assertSame(textureHandle, sink.samplers.get(OceanShaderLibrary.SHORE_TEXTURE));

        OceanWaterWaveData.PackedWave first = frame.waveData().wave(0);
        assertArrayEquals(
            new float[] {
                first.a().directionX(),
                first.a().directionZ(),
                first.a().amplitude(),
                first.a().waveNumber()
            },
            sink.vec4.get(OceanShaderLibrary.waveAUniform(0))
        );
        assertArrayEquals(
            new float[] {
                first.b().angularFrequency(),
                first.b().phase(),
                first.b().steepness(),
                first.b().auxiliary()
            },
            sink.vec4.get(OceanShaderLibrary.waveBUniform(0))
        );
    }

    private static final class RecordingSink implements OceanWaterUniformBinder.Sink {
        private final Map<String, Integer> ints = new HashMap<>();
        private final Map<String, Float> floats = new HashMap<>();
        private final Map<String, float[]> vec2 = new HashMap<>();
        private final Map<String, float[]> vec3 = new HashMap<>();
        private final Map<String, float[]> vec4 = new HashMap<>();
        private final Map<String, Object> samplers = new HashMap<>();

        @Override
        public void setInt(String name, int value) {
            ints.put(name, value);
        }

        @Override
        public void setFloat(String name, float value) {
            floats.put(name, value);
        }

        @Override
        public void setVec2(String name, float x, float y) {
            vec2.put(name, new float[] {x, y});
        }

        @Override
        public void setVec3(String name, float x, float y, float z) {
            vec3.put(name, new float[] {x, y, z});
        }

        @Override
        public void setVec4(String name, float x, float y, float z, float w) {
            vec4.put(name, new float[] {x, y, z, w});
        }

        @Override
        public void bindSampler(String name, Object handle) {
            samplers.put(name, handle);
        }
    }
}
