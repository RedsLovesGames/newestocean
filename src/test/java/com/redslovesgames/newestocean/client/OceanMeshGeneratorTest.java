package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanMeshGeneratorTest {
    @Test
    void generatedMeshMatchesPlannerBudget() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(42L);
        OceanMeshPlanner.Plan plan = OceanMeshPlanner.plan(OceanQuality.POTATO, 8.0, -4.0);

        OceanMeshGenerator.Mesh mesh = OceanMeshGenerator.generate(
                ocean,
                plan,
                12.5,
                OceanConditions.calm()
        );

        assertEquals(plan.vertexCount(), mesh.vertices().length);
        assertEquals(plan.triangleCount() * 3, mesh.indices().length);
    }

    @Test
    void generatedVerticesUseVisualWaveBudgetAndNormalizedNormals() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(99L);
        OceanMeshPlanner.Plan plan = OceanMeshPlanner.plan(OceanQuality.LOW, 0.0, 0.0);

        OceanMeshGenerator.Mesh mesh = OceanMeshGenerator.generate(
                ocean,
                plan,
                4.0,
                OceanConditions.calm()
        );

        boolean sawDisplacement = false;
        for (OceanMeshGenerator.Vertex vertex : mesh.vertices()) {
            assertTrue(Double.isFinite(vertex.x()));
            assertTrue(Double.isFinite(vertex.y()));
            assertTrue(Double.isFinite(vertex.z()));
            assertEquals(1.0, vertex.normal().length(), 1.0e-9);
            sawDisplacement |= Math.abs(vertex.y()) > 1.0e-6;
        }
        assertTrue(sawDisplacement);
    }

    @Test
    void flatConditionsProduceExactFlatMesh() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(7L);
        OceanMeshPlanner.Plan plan = OceanMeshPlanner.plan(OceanQuality.POTATO, 0.0, 0.0);
        OceanConditions flat = new OceanConditions(0.0, 0.0, Vec3.ZERO);

        OceanMeshGenerator.Mesh mesh = OceanMeshGenerator.generate(ocean, plan, 9.0, flat);

        for (OceanMeshGenerator.Vertex vertex : mesh.vertices()) {
            assertEquals(0.0, vertex.y());
            assertEquals(Vec3.UP, vertex.normal());
        }
    }
}
