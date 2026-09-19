package com.redslovesgames.newestocean.client.compat.sodium;

import com.redslovesgames.newestocean.client.water.OceanShaderLibrary;
import com.redslovesgames.newestocean.client.water.OceanShaderSourcePatcher;

/** Pure source-patching contract for the pinned Sodium 0.8.12 chunk shader. */
public final class SodiumWaterShaderBridge {
    public static final String SHADER_LOADER_CLASS =
        "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader";
    public static final String SHADER_SOURCE_METHOD = "getShaderSource";
    public static final String CHUNK_RENDERER_CLASS =
        "net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer";
    public static final String BEGIN_METHOD = "begin";

    private static final String SODIUM_VERTEX_SIGNATURE = "#import <sodium:include/chunk_vertex.glsl>";
    private static final String POSITION_ANCHOR = "vec3 position = _vert_position + translation;";
    private static final String ATLAS_UV_DECLARATION =
        "vec2 newestocean_atlasUv = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;";
    private static final String PATCH_ANCHOR = POSITION_ANCHOR + "\n    " + ATLAS_UV_DECLARATION;

    private static final OceanShaderSourcePatcher.PatchSite PATCH_SITE =
        new OceanShaderSourcePatcher.PatchSite(
            PATCH_ANCHOR,
            "position",
            "position + newestocean_cameraOrigin",
            "newestocean_atlasUv"
        );

    private SodiumWaterShaderBridge() {
    }

    /**
     * Patches only the exact structural seam used by Sodium 0.8.12's
     * {@code sodium:blocks/block_layer_opaque.vsh}. Unknown sources fail closed.
     */
    public static String patchShaderSource(String source) {
        if (source == null) {
            throw new IllegalArgumentException("shader source is required");
        }
        if (source.contains(OceanShaderLibrary.MARKER)) {
            return source;
        }
        if (!source.contains(SODIUM_VERTEX_SIGNATURE) || !source.contains(POSITION_ANCHOR)) {
            return source;
        }

        String prepared = source.replace(POSITION_ANCHOR, PATCH_ANCHOR);
        return OceanShaderSourcePatcher.patchVertex(prepared, PATCH_SITE);
    }
}
