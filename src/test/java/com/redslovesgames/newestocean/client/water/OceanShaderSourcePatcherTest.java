package com.redslovesgames.newestocean.client.water;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanShaderSourcePatcherTest {
    @Test
    void patchesRepresentativeVanillaSodiumIrisAndShadowSourcesOnce() {
        assertPatched(
            "#version 150\nvoid main() {\n    vec3 vanillaPos = vec3(1.0);\n    vec2 vanillaUv = vec2(0.5);\n    // VANILLA_WATER_VERTEX\n    gl_Position = vec4(vanillaPos, 1.0);\n}\n",
            new OceanShaderSourcePatcher.PatchSite(
                "// VANILLA_WATER_VERTEX", "vanillaPos", "vanillaPos", "vanillaUv"
            )
        );
        assertPatched(
            "#version 330 core\nvoid main() {\n    vec3 sodiumPos = vec3(2.0);\n    vec2 sodiumUv = vec2(0.25);\n    // SODIUM_WATER_VERTEX\n    gl_Position = vec4(sodiumPos, 1.0);\n}\n",
            new OceanShaderSourcePatcher.PatchSite(
                "// SODIUM_WATER_VERTEX", "sodiumPos", "sodiumPos", "sodiumUv"
            )
        );
        assertPatched(
            "#version 330 compatibility\nvoid main() {\n    vec3 irisPos = vec3(3.0);\n    vec2 irisUv = vec2(0.75);\n    // IRIS_GBUFFERS_WATER_VERTEX\n    gl_Position = vec4(irisPos, 1.0);\n}\n",
            new OceanShaderSourcePatcher.PatchSite(
                "// IRIS_GBUFFERS_WATER_VERTEX", "irisPos", "irisPos", "irisUv"
            )
        );
        assertPatched(
            "#version 330 compatibility\nvoid main() {\n    vec3 shadowPos = vec3(4.0);\n    vec2 shadowUv = vec2(0.9);\n    // IRIS_SHADOW_WATER_VERTEX\n    gl_Position = vec4(shadowPos, 1.0);\n}\n",
            new OceanShaderSourcePatcher.PatchSite(
                "// IRIS_SHADOW_WATER_VERTEX", "shadowPos", "shadowPos", "shadowUv"
            )
        );
    }

    @Test
    void applyingPatchTwiceIsIdempotent() {
        String source = "#version 150\nvoid main() {\n    vec3 position = vec3(1.0);\n    vec2 uv = vec2(0.5);\n    // WATER_ANCHOR\n    gl_Position = vec4(position, 1.0);\n}\n";
        OceanShaderSourcePatcher.PatchSite site = new OceanShaderSourcePatcher.PatchSite(
            "// WATER_ANCHOR", "position", "position", "uv"
        );

        String once = OceanShaderSourcePatcher.patchVertex(source, site);
        String twice = OceanShaderSourcePatcher.patchVertex(once, site);

        assertEquals(once, twice);
        assertEquals(1, occurrences(once, OceanShaderLibrary.MARKER));
    }

    @Test
    void sourceWithoutRequiredAnchorIsLeftByteForByteUnchanged() {
        String source = "#version 150\nvoid main() { gl_Position = vec4(0.0); }\n";
        OceanShaderSourcePatcher.PatchSite site = new OceanShaderSourcePatcher.PatchSite(
            "// MISSING_WATER_ANCHOR", "position", "position", "uv"
        );

        assertEquals(source, OceanShaderSourcePatcher.patchVertex(source, site));
    }

    @Test
    void injectedDisplacementIsGuardedByWaterSpriteClassification() {
        String source = "#version 150\nvoid main() {\n    vec3 position = vec3(1.0);\n    vec2 atlasUv = vec2(0.5);\n    // WATER_ANCHOR\n    gl_Position = vec4(position, 1.0);\n}\n";
        OceanShaderSourcePatcher.PatchSite site = new OceanShaderSourcePatcher.PatchSite(
            "// WATER_ANCHOR", "position", "position", "atlasUv"
        );

        String patched = OceanShaderSourcePatcher.patchVertex(source, site);
        int guard = patched.indexOf("if (newestocean_isWater(atlasUv))");
        int displacement = patched.indexOf("position += newestocean_sample.displacement;");

        assertTrue(guard >= 0);
        assertTrue(displacement > guard);
        assertFalse(patched.contains("position += newestocean_sample.displacement;\n    gl_Position")
            && guard > displacement);
    }

    private static void assertPatched(String source, OceanShaderSourcePatcher.PatchSite site) {
        String patched = OceanShaderSourcePatcher.patchVertex(source, site);

        assertNotEquals(source, patched);
        assertTrue(patched.contains(OceanShaderLibrary.MARKER));
        assertTrue(patched.contains("if (newestocean_isWater(" + site.atlasUvExpression() + "))"));
        assertTrue(patched.contains(site.positionLValue() + " += newestocean_sample.displacement;"));
        assertEquals(1, occurrences(patched, OceanShaderLibrary.MARKER));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
