package com.redslovesgames.newestocean.client;

/**
 * Produces a camera-centered, grid-snapped ocean mesh budget for the renderer.
 *
 * <p>The planner is intentionally pure and allocation-light so it can be tested without Minecraft.
 * Physics never depends on this class. Lower visual quality therefore reduces only render work.</p>
 */
public final class OceanMeshPlanner {
    private OceanMeshPlanner() {
    }

    public static Plan plan(OceanQuality quality, double cameraX, double cameraZ) {
        int radius = quality.renderRadiusBlocks();
        int step = quality.gridStepBlocks();

        // Snap to the render grid so sub-cell camera movement can reuse the same mesh topology.
        double originX = Math.floor(cameraX / step) * step;
        double originZ = Math.floor(cameraZ / step) * step;

        int verticesPerSide = (radius * 2) / step + 1;
        int vertexCount = verticesPerSide * verticesPerSide;
        int triangleCount = (verticesPerSide - 1) * (verticesPerSide - 1) * 2;

        return new Plan(
                originX,
                originZ,
                radius,
                step,
                verticesPerSide,
                vertexCount,
                triangleCount,
                quality.visualWaveComponents()
        );
    }

    public record Plan(
            double originX,
            double originZ,
            int radiusBlocks,
            int gridStepBlocks,
            int verticesPerSide,
            int vertexCount,
            int triangleCount,
            int visualWaveComponents
    ) {
    }
}
