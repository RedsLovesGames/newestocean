package com.redslovesgames.newestocean.client.compat.sodium;

import com.redslovesgames.newestocean.client.water.OceanShaderLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SodiumWaterShaderBridgeTest {
    private static final String SODIUM_0812_CHUNK_VERTEX = """
        #version 330 core

        #import <sodium:include/fog.glsl>
        #import <sodium:include/chunk_vertex.glsl>
        #import <sodium:include/chunk_matrices.glsl>

        out vec4 v_Color;
        out vec2 v_TexCoord;
        flat out uint v_Material;

        uniform int u_FogShape;
        uniform vec3 u_RegionOffset;
        uniform vec2 u_TexCoordShrink;
        uniform sampler2D u_LightTex;

        uvec3 _get_relative_chunk_coord(uint pos) {
            return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
        }

        vec3 _get_draw_translation(uint pos) {
            return _get_relative_chunk_coord(pos) * vec3(16.0);
        }

        void main() {
            _vert_init();
            vec3 translation = u_RegionOffset + _get_draw_translation(_draw_id);
            vec3 position = _vert_position + translation;
            gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);
            v_Color = _vert_color * texture(u_LightTex, _vert_tex_light_coord);
            v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;
            v_Material = _material_params;
        }
        """;

    @Test
    void exactSodium0812ChunkVertexSourceIsPatchedOnce() {
        String patched = SodiumWaterShaderBridge.patchShaderSource(SODIUM_0812_CHUNK_VERTEX);

        assertNotEquals(SODIUM_0812_CHUNK_VERTEX, patched);
        assertTrue(patched.contains(OceanShaderLibrary.MARKER));
        assertTrue(patched.contains("newestocean_isWater(newestocean_atlasUv)"));
        assertTrue(patched.contains("position + newestocean_cameraOrigin"));
        assertTrue(patched.contains("position += newestocean_sample.displacement;"));
        assertEquals(patched, SodiumWaterShaderBridge.patchShaderSource(patched));
    }

    @Test
    void unrelatedSodiumShaderSourceRemainsUntouched() {
        String source = "#version 330 core\nvoid main() { gl_Position = vec4(0.0); }\n";
        assertEquals(source, SodiumWaterShaderBridge.patchShaderSource(source));
    }

    @Test
    void exactPinnedSodiumTargetsAreDocumented() {
        assertEquals(
            "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader",
            SodiumWaterShaderBridge.SHADER_LOADER_CLASS
        );
        assertEquals("getShaderSource", SodiumWaterShaderBridge.SHADER_SOURCE_METHOD);
        assertEquals(
            "net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer",
            SodiumWaterShaderBridge.CHUNK_RENDERER_CLASS
        );
        assertEquals("begin", SodiumWaterShaderBridge.BEGIN_METHOD);
    }

    @Test
    void runtimeVersionGateAcceptsOnlyPinnedSodium0812Family() {
        assertTrue(SodiumWaterRuntimeBridge.supportsVersion("0.8.12"));
        assertTrue(SodiumWaterRuntimeBridge.supportsVersion("0.8.12+mc1.21.1"));
        assertTrue(SodiumWaterRuntimeBridge.supportsVersion("mc1.21.1-0.8.12-fabric"));
        assertFalse(SodiumWaterRuntimeBridge.supportsVersion("0.8.11+mc1.21.1"));
        assertFalse(SodiumWaterRuntimeBridge.supportsVersion("0.8.13+mc1.21.1"));
        assertFalse(SodiumWaterRuntimeBridge.supportsVersion(null));
    }
}
