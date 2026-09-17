package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;

/** Builds CPU-side displaced ocean geometry from the same deterministic surface used by physics. */
public final class OceanMeshGenerator {
    private OceanMeshGenerator() {
    }

    public static Mesh generate(
            ProceduralOcean ocean,
            OceanMeshPlanner.Plan plan,
            double timeSeconds,
            OceanConditions conditions
    ) {
        if (ocean == null || plan == null || conditions == null) {
            throw new IllegalArgumentException("ocean, plan, and conditions are required");
        }

        int side = plan.verticesPerSide();
        Vertex[] vertices = new Vertex[plan.vertexCount()];
        int[] indices = new int[plan.triangleCount() * 3];

        double startX = plan.originX() - plan.radiusBlocks();
        double startZ = plan.originZ() - plan.radiusBlocks();
        int step = plan.gridStepBlocks();

        int vertexIndex = 0;
        for (int zIndex = 0; zIndex < side; zIndex++) {
            double baseZ = startZ + (double) zIndex * step;
            for (int xIndex = 0; xIndex < side; xIndex++) {
                double baseX = startX + (double) xIndex * step;
                OceanSurface.SurfaceSample sample = ocean.sample(
                        baseX,
                        baseZ,
                        timeSeconds,
                        conditions,
                        plan.visualWaveComponents()
                );

                Vec3 displacement = sample.horizontalDisplacement();
                vertices[vertexIndex++] = new Vertex(
                        baseX + displacement.x(),
                        sample.height(),
                        baseZ + displacement.z(),
                        sample.normal()
                );
            }
        }

        int index = 0;
        for (int z = 0; z < side - 1; z++) {
            for (int x = 0; x < side - 1; x++) {
                int topLeft = z * side + x;
                int topRight = topLeft + 1;
                int bottomLeft = topLeft + side;
                int bottomRight = bottomLeft + 1;

                indices[index++] = topLeft;
                indices[index++] = bottomLeft;
                indices[index++] = topRight;

                indices[index++] = topRight;
                indices[index++] = bottomLeft;
                indices[index++] = bottomRight;
            }
        }

        return new Mesh(vertices, indices);
    }

    public record Vertex(double x, double y, double z, Vec3 normal) {
    }

    public record Mesh(Vertex[] vertices, int[] indices) {
    }
}
