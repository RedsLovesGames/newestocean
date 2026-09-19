package com.redslovesgames.newestocean.client.compat.iris;

import com.redslovesgames.newestocean.client.water.OceanShaderLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IrisWaterShaderBridgeTest {
    private static final String IRIS_SODIUM_TRANSFORMED_VERTEX = """
        #version 330 core
        in uvec2 a_Position;
        in uvec2 a_TexCoord;
        in vec4 a_Color;
        in uvec4 a_LightAndData;
        vec3 _vert_position;
        vec2 _vert_tex_diffuse_coord;
        vec2 _vert_tex_diffuse_coord_bias;
        vec2 _vert_tex_light_coord;
        vec4 _vert_color;
        uint _draw_id;
        uniform vec2 u_TexCoordShrink;
        uniform vec3 u_RegionOffset;
        vec3 _get_draw_translation(uint pos) { return vec3(0.0); }
        void _vert_init() {
            _vert_position = vec3(0.0);
            _vert_tex_diffuse_coord = vec2(0.25);
            _vert_tex_diffuse_coord_bias = vec2(0.0);
            _draw_id = 0u;
        }
        vec4 getVertexPosition() {
            return vec4(_vert_position + u_RegionOffset + _get_draw_translation(_draw_id), 1.0);
        }
        void main() {
            _vert_init();
            gl_Position = vec4(getVertexPosition().xyz, 1.0);
        }
        """;

    private static final String SIMPLE_FRAGMENT = """
        #version 330 core
        out vec4 fragColor;
        void main() { fragColor = vec4(1.0); }
        """;

    @Test
    void gbuffersWaterVertexGetsRealWaterDisplacementExactlyOnce() {
        String patched = IrisWaterShaderBridge.patchVertexSource("gbuffers_water", IRIS_SODIUM_TRANSFORMED_VERTEX);

        assertNotEquals(IRIS_SODIUM_TRANSFORMED_VERTEX, patched);
        assertTrue(patched.contains(OceanShaderLibrary.MARKER));
        assertTrue(patched.contains("newestocean_isWater(newestocean_atlasUv)"));
        assertTrue(patched.contains("_vert_position += newestocean_sample.displacement;"));
        assertTrue(patched.contains("newestocean_surfaceNormal = newestocean_sample.normal;"));
        assertTrue(patched.contains("newestocean_foam = newestocean_sample.foam;"));
        assertEquals(patched, IrisWaterShaderBridge.patchVertexSource("gbuffers_water", patched));
    }

    @Test
    void shadowWaterGetsTheSameLargeScaleDisplacement() {
        String patched = IrisWaterShaderBridge.patchVertexSource("shadow_water", IRIS_SODIUM_TRANSFORMED_VERTEX);

        assertNotEquals(IRIS_SODIUM_TRANSFORMED_VERTEX, patched);
        assertTrue(patched.contains("_vert_position += newestocean_sample.displacement;"));
        assertEquals(patched, IrisWaterShaderBridge.patchVertexSource("shadow_water", patched));
    }

    @Test
    void genericShadowFallbackCanBePatchedButStillClassifiesWater() {
        String patched = IrisWaterShaderBridge.patchVertexSource("shadow", IRIS_SODIUM_TRANSFORMED_VERTEX);

        assertTrue(patched.contains("newestocean_isWater(newestocean_atlasUv)"));
        assertTrue(patched.contains("_vert_position += newestocean_sample.displacement;"));
    }

    @Test
    void nonWaterProgramsRemainByteForByteUntouched() {
        assertEquals(
            IRIS_SODIUM_TRANSFORMED_VERTEX,
            IrisWaterShaderBridge.patchVertexSource("gbuffers_terrain", IRIS_SODIUM_TRANSFORMED_VERTEX)
        );
        assertEquals(
            SIMPLE_FRAGMENT,
            IrisWaterShaderBridge.patchFragmentSource("gbuffers_terrain", SIMPLE_FRAGMENT)
        );
    }

    @Test
    void waterFragmentReceivesNormalAndFoamMetadataWithoutReplacingPackLogic() {
        String patched = IrisWaterShaderBridge.patchFragmentSource("gbuffers_water", SIMPLE_FRAGMENT);

        assertNotEquals(SIMPLE_FRAGMENT, patched);
        assertTrue(patched.contains("in vec3 newestocean_surfaceNormal;"));
        assertTrue(patched.contains("in float newestocean_foam;"));
        assertTrue(patched.contains("fragColor = vec4(1.0);"));
        assertEquals(patched, IrisWaterShaderBridge.patchFragmentSource("gbuffers_water", patched));
    }

    @Test
    void exactPinnedIrisTargetsAreDocumented() {
        assertEquals(
            "net.irisshaders.iris.pipeline.transform.TransformPatcher",
            IrisWaterShaderBridge.TRANSFORM_PATCHER_CLASS
        );
        assertEquals("patchSodium", IrisWaterShaderBridge.PATCH_SODIUM_METHOD);
        assertEquals(
            "net.irisshaders.iris.pipeline.programs.SodiumShader",
            IrisWaterShaderBridge.SODIUM_SHADER_CLASS
        );
        assertEquals("setupState", IrisWaterShaderBridge.SETUP_STATE_METHOD);
    }
}
