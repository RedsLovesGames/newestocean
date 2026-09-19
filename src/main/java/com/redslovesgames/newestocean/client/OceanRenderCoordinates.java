package com.redslovesgames.newestocean.client;

/** Converts world-space ocean vertices into camera-relative coordinates for Fabric world rendering. */
public final class OceanRenderCoordinates {
    private OceanRenderCoordinates() {
    }

    public static Relative relative(
        double worldX,
        double worldY,
        double worldZ,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (!Double.isFinite(worldX) || !Double.isFinite(worldY) || !Double.isFinite(worldZ)
            || !Double.isFinite(cameraX) || !Double.isFinite(cameraY) || !Double.isFinite(cameraZ)) {
            throw new IllegalArgumentException("render coordinates must be finite");
        }
        return new Relative(worldX - cameraX, worldY - cameraY, worldZ - cameraZ);
    }

    public record Relative(double x, double y, double z) {
    }
}
