package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanSurface;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;

/** Applies the deterministic ocean surface to reusable camera-independent LOD topology. */
public final class OceanLodMeshGenerator {
    private OceanLodMeshGenerator() {
    }

    public static Mesh generate(
        ProceduralOcean ocean,
        OceanLodPlanner.Plan plan,
        OceanLodTopology topology,
        OceanLodCoverageMask coverage,
        double timeSeconds,
        OceanConditions conditions
    ) {
        return generate(
            ocean,
            plan,
            topology,
            coverage,
            timeSeconds,
            conditions,
            plan == null ? 0 : plan.visualWaveComponents()
        );
    }

    public static Mesh generate(
        ProceduralOcean ocean,
        OceanLodPlanner.Plan plan,
        OceanLodTopology topology,
        OceanLodCoverageMask coverage,
        double timeSeconds,
        OceanConditions conditions,
        int visualWaveComponents
    ) {
        if (ocean == null || plan == null || topology == null || coverage == null || conditions == null) {
            throw new IllegalArgumentException("ocean, plan, topology, coverage, and conditions are required");
        }
        if (!Double.isFinite(timeSeconds)) {
            throw new IllegalArgumentException("timeSeconds must be finite");
        }
        if (visualWaveComponents < 0) {
            throw new IllegalArgumentException("visualWaveComponents cannot be negative");
        }

        OceanLodTopology.LocalVertex[] localVertices = topology.vertices();
        Vertex[] vertices = new Vertex[localVertices.length];
        for (int i = 0; i < localVertices.length; i++) {
            OceanLodTopology.LocalVertex local = localVertices[i];
            double baseX = plan.originX() + local.x();
            double baseZ = plan.originZ() + local.z();
            OceanSurface.SurfaceSample sample = ocean.sample(
                baseX,
                baseZ,
                timeSeconds,
                conditions,
                visualWaveComponents
            );
            Vec3 displacement = sample.horizontalDisplacement();
            vertices[i] = new Vertex(
                baseX + displacement.x(),
                sample.height(),
                baseZ + displacement.z(),
                sample.normal()
            );
        }

        return new Mesh(vertices, coverage.waterIndices(topology));
    }

    public record Vertex(double x, double y, double z, Vec3 normal) {
        public Vertex {
            if (normal == null) {
                throw new IllegalArgumentException("normal is required");
            }
        }
    }

    public record Mesh(Vertex[] vertices, int[] indices) {
        public Mesh {
            if (vertices == null || indices == null) {
                throw new IllegalArgumentException("vertices and indices are required");
            }
        }
    }
}
