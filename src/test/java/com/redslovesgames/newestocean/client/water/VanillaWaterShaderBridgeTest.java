package com.redslovesgames.newestocean.client.water;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaWaterShaderBridgeTest {
    private static final String VANILLA_1211_TRANSLUCENT_VERTEX = """
        #version 150

        #moj_import <light.glsl>
        #moj_import <fog.glsl>

        in vec3 Position;
        in vec4 Color;
        in vec2 UV0;
        in ivec2 UV2;
        in vec3 Normal;

        uniform sampler2D Sampler2;
        uniform mat4 ModelViewMat;
        uniform mat4 ProjMat;
        uniform vec3 ChunkOffset;
        uniform int FogShape;

        void main() {
            vec3 pos = Position + ChunkOffset;
            gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
        }
        """;

    @Test
    void exactVanilla1211TranslucentVertexSourceIsPatchedAtPositionAnchor() {
        String patched = VanillaWaterShaderBridge.patchShaderSource(
            VanillaWaterShaderBridge.Stage.VERTEX,
            "rendertype_translucent",
            VANILLA_1211_TRANSLUCENT_VERTEX
        );

        assertNotEquals(VANILLA_1211_TRANSLUCENT_VERTEX, patched);
        assertTrue(patched.contains(OceanShaderLibrary.MARKER));
        assertTrue(patched.contains("if (newestocean_isWater(UV0))"));
        assertTrue(patched.contains("newestocean_cameraOrigin"));
        assertTrue(patched.contains("pos += newestocean_sample.displacement;"));
    }

    @Test
    void fragmentAndNonTranslucentShadersRemainUntouched() {
        assertEquals(
            VANILLA_1211_TRANSLUCENT_VERTEX,
            VanillaWaterShaderBridge.patchShaderSource(
                VanillaWaterShaderBridge.Stage.FRAGMENT,
                "rendertype_translucent",
                VANILLA_1211_TRANSLUCENT_VERTEX
            )
        );
        assertEquals(
            VANILLA_1211_TRANSLUCENT_VERTEX,
            VanillaWaterShaderBridge.patchShaderSource(
                VanillaWaterShaderBridge.Stage.VERTEX,
                "rendertype_solid",
                VANILLA_1211_TRANSLUCENT_VERTEX
            )
        );
    }

    @Test
    void sourcePatchIsIdempotent() {
        String once = VanillaWaterShaderBridge.patchShaderSource(
            VanillaWaterShaderBridge.Stage.VERTEX,
            "rendertype_translucent",
            VANILLA_1211_TRANSLUCENT_VERTEX
        );
        String twice = VanillaWaterShaderBridge.patchShaderSource(
            VanillaWaterShaderBridge.Stage.VERTEX,
            "rendertype_translucent",
            once
        );

        assertEquals(once, twice);
    }

    @Test
    void exactYarn1211ShaderTargetsAreDocumentedAsConstants() {
        assertEquals(
            "Lnet/minecraft/client/gl/ShaderProgram;loadShader(Lnet/minecraft/resource/ResourceFactory;Lnet/minecraft/client/gl/ShaderStage$Type;Ljava/lang/String;)Lnet/minecraft/client/gl/ShaderStage;",
            VanillaWaterShaderBridge.LOAD_SHADER_SELECTOR
        );
        assertEquals(
            "Lnet/minecraft/client/gl/ShaderProgram;bind()V",
            VanillaWaterShaderBridge.BIND_SELECTOR
        );
    }
}
