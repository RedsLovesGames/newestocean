package com.redslovesgames.newestocean.client.water;

/** Pure, renderer-neutral shader source patcher used by renderer-specific compatibility bridges. */
public final class OceanShaderSourcePatcher {
    private OceanShaderSourcePatcher() {
    }

    public static String patchVertex(String source, PatchSite site) {
        if (source == null || site == null) {
            throw new IllegalArgumentException("shader source and patch site are required");
        }
        if (source.contains(OceanShaderLibrary.MARKER)) {
            return source;
        }
        if (!source.contains(site.anchor())) {
            return source;
        }

        String withLibrary = injectLibrary(source);
        int anchorIndex = withLibrary.indexOf(site.anchor());
        if (anchorIndex < 0) {
            return source;
        }

        int insertionIndex = anchorIndex + site.anchor().length();
        String body = "\n"
            + "    if (newestocean_isWater(" + site.atlasUvExpression() + ")) {\n"
            + "        NewestOceanWaterSample newestocean_sample = newestocean_evaluateWater("
            + site.worldPositionExpression() + ");\n"
            + "        " + site.positionLValue() + " += newestocean_sample.displacement;\n"
            + "    }";

        return withLibrary.substring(0, insertionIndex)
            + body
            + withLibrary.substring(insertionIndex);
    }

    private static String injectLibrary(String source) {
        String library = OceanShaderLibrary.source();
        int versionStart = source.indexOf("#version");
        if (versionStart < 0) {
            return library + "\n" + source;
        }

        int lineEnd = source.indexOf('\n', versionStart);
        if (lineEnd < 0) {
            return source + "\n" + library + "\n";
        }

        int insertion = lineEnd + 1;
        return source.substring(0, insertion)
            + "\n"
            + library
            + "\n"
            + source.substring(insertion);
    }

    public record PatchSite(
        String anchor,
        String positionLValue,
        String worldPositionExpression,
        String atlasUvExpression
    ) {
        public PatchSite {
            requireExpression(anchor, "anchor");
            requireExpression(positionLValue, "positionLValue");
            requireExpression(worldPositionExpression, "worldPositionExpression");
            requireExpression(atlasUvExpression, "atlasUvExpression");
        }

        private static void requireExpression(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " cannot be blank");
            }
        }
    }
}
