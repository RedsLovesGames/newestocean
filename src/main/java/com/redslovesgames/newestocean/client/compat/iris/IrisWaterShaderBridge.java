package com.redslovesgames.newestocean.client.compat.iris;

import com.redslovesgames.newestocean.client.water.OceanShaderLibrary;
import com.redslovesgames.newestocean.client.water.OceanShaderSourcePatcher;

/**
 * Pure source patching for the pinned Iris 1.8.14-beta.1 + Minecraft 1.21.1 pipeline.
 *
 * <p>Iris transforms shaderpack water programs into Sodium-compatible GLSL before compiling
 * them. We patch that transformed source, not the original pack source, so shaderpacks retain
 * ownership of material, lighting, fog, reflections, refraction, and final fragment color.</p>
 */
public final class IrisWaterShaderBridge {
    public static final String TRANSFORM_PATCHER_CLASS =
        "net.irisshaders.iris.pipeline.transform.TransformPatcher";
    public static final String PATCH_SODIUM_METHOD = "patchSodium";
    public static final String SODIUM_SHADER_CLASS =
        "net.irisshaders.iris.pipeline.programs.SodiumShader";
    public static final String SETUP_STATE_METHOD = "setupState";

    public static final String WATER_PROGRAM = "gbuffers_water";
    public static final String SHADOW_WATER_PROGRAM = "shadow_water";
    public static final String SHADOW_FALLBACK_PROGRAM = "shadow";
    public static final String FRAGMENT_MARKER = "// NEWESTOCEAN_IRIS_WATER_FRAGMENT_V1";

    private static final String VERT_INIT = "_vert_init();";
    private static final String POSITION_SIGNATURE = "vec3 _vert_position;";
    private static final String REGION_SIGNATURE = "uniform vec3 u_RegionOffset;";
    private static final String TEX_SHRINK_SIGNATURE = "uniform vec2 u_TexCoordShrink;";
    private static final String DRAW_TRANSLATION_SIGNATURE = "_get_draw_translation";
    private static final String ATLAS_UV_DECLARATION =
        "vec2 newestocean_atlasUv = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;";
    private static final String OUTPUT_DEFAULTS =
        "newestocean_surfaceNormal = vec3(0.0, 1.0, 0.0);\n"
            + "    newestocean_foam = 0.0;";
    private static final String PATCH_ANCHOR = VERT_INIT + "\n    " + ATLAS_UV_DECLARATION + "\n    " + OUTPUT_DEFAULTS;
    private static final String VERTEX_VARYINGS =
        "out vec3 newestocean_surfaceNormal;\nout float newestocean_foam;\n";
    private static final String FRAGMENT_VARYINGS =
        FRAGMENT_MARKER + "\nin vec3 newestocean_surfaceNormal;\nin float newestocean_foam;\n";

    private static final OceanShaderSourcePatcher.PatchSite PATCH_SITE =
        new OceanShaderSourcePatcher.PatchSite(
            PATCH_ANCHOR,
            "_vert_position",
            "_vert_position + u_RegionOffset + _get_draw_translation(_draw_id) + newestocean_cameraOrigin",
            "newestocean_atlasUv"
        );

    private IrisWaterShaderBridge() {
    }

    public static boolean targetsWaterVertex(String programName) {
        return WATER_PROGRAM.equals(programName)
            || SHADOW_WATER_PROGRAM.equals(programName)
            || SHADOW_FALLBACK_PROGRAM.equals(programName);
    }

    public static boolean targetsWaterFragment(String programName) {
        return WATER_PROGRAM.equals(programName);
    }

    public static String patchVertexSource(String programName, String source) {
        if (programName == null || source == null) {
            throw new IllegalArgumentException("program name and shader source are required");
        }
        if (!targetsWaterVertex(programName) || source.contains(OceanShaderLibrary.MARKER)) {
            return source;
        }
        if (!hasPinnedTransformedVertexShape(source)) {
            return source;
        }

        String prepared = insertAfterVersion(source, VERTEX_VARYINGS);
        prepared = prepared.replace(VERT_INIT, PATCH_ANCHOR);
        String patched = OceanShaderSourcePatcher.patchVertex(prepared, PATCH_SITE);
        if (!patched.contains(OceanShaderLibrary.MARKER)) {
            return source;
        }

        return patched.replace(
            "_vert_position += newestocean_sample.displacement;",
            "_vert_position += newestocean_sample.displacement;\n"
                + "        newestocean_surfaceNormal = newestocean_sample.normal;\n"
                + "        newestocean_foam = newestocean_sample.foam;"
        );
    }

    public static String patchFragmentSource(String programName, String source) {
        if (programName == null || source == null) {
            throw new IllegalArgumentException("program name and shader source are required");
        }
        if (!targetsWaterFragment(programName) || source.contains(FRAGMENT_MARKER)) {
            return source;
        }
        return insertAfterVersion(source, FRAGMENT_VARYINGS);
    }

    /** Maps Iris's concrete SodiumPrograms pass name to the logical shaderpack program we patch. */
    public static String logicalProgramForPass(String passName) {
        if (passName == null) {
            return null;
        }
        return switch (passName) {
            case "translucent" -> WATER_PROGRAM;
            case "shadow_trans" -> SHADOW_WATER_PROGRAM;
            default -> null;
        };
    }

    private static boolean hasPinnedTransformedVertexShape(String source) {
        return source.contains(VERT_INIT)
            && source.contains(POSITION_SIGNATURE)
            && source.contains(REGION_SIGNATURE)
            && source.contains(TEX_SHRINK_SIGNATURE)
            && source.contains(DRAW_TRANSLATION_SIGNATURE)
            && source.contains("_vert_tex_diffuse_coord")
            && source.contains("_draw_id");
    }

    private static String insertAfterVersion(String source, String injected) {
        int version = source.indexOf("#version");
        if (version < 0) {
            return injected + source;
        }
        int lineEnd = source.indexOf('\n', version);
        if (lineEnd < 0) {
            return source + "\n" + injected;
        }
        int insertion = lineEnd + 1;
        return source.substring(0, insertion) + injected + source.substring(insertion);
    }
}
