package com.redslovesgames.newestocean.client.water;

/**
 * Pure Vanilla 1.21.1 shader-source bridge. Minecraft-specific mixins only feed source through
 * this class, keeping renderer target selection and source mutation independently testable.
 */
public final class VanillaWaterShaderBridge {
    public static final String LOAD_SHADER_SELECTOR =
        "Lnet/minecraft/client/gl/ShaderProgram;loadShader(Lnet/minecraft/resource/ResourceFactory;Lnet/minecraft/client/gl/ShaderStage$Type;Ljava/lang/String;)Lnet/minecraft/client/gl/ShaderStage;";
    public static final String BIND_SELECTOR =
        "Lnet/minecraft/client/gl/ShaderProgram;bind()V";

    private static final String TARGET_SHADER = "rendertype_translucent";
    private static final String POSITION_ANCHOR = "vec3 pos = Position + ChunkOffset;";

    private VanillaWaterShaderBridge() {
    }

    public static String patchShaderSource(Stage stage, String shaderName, String source) {
        if (stage == null || shaderName == null || source == null) {
            throw new IllegalArgumentException("stage, shaderName, and source are required");
        }
        if (stage != Stage.VERTEX || !TARGET_SHADER.equals(shaderName)) {
            return source;
        }

        return OceanShaderSourcePatcher.patchVertex(
            source,
            new OceanShaderSourcePatcher.PatchSite(
                POSITION_ANCHOR,
                "pos",
                "pos + " + OceanShaderLibrary.CAMERA_ORIGIN,
                "UV0"
            )
        );
    }

    public static boolean targets(Stage stage, String shaderName) {
        return stage == Stage.VERTEX && TARGET_SHADER.equals(shaderName);
    }

    public enum Stage {
        VERTEX,
        FRAGMENT
    }
}
