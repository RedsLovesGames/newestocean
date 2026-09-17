package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.ProceduralOcean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanLodMeshGeneratorTest {
    @Test
    void generatedLodMeshUsesCachedTopologyAndWaterIndices() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(42L);
        OceanLodPlanner.Plan plan = OceanLodPlanner.plan(OceanQuality.MEDIUM, 12.0, -8.0);
        OceanLodTopology topology = OceanLodTopology.build(plan);
        OceanLodCoverageMask mask = OceanLodCoverageMask.build(plan, topology, (x, z) -> x >= plan.originX());

        OceanLodMeshGenerator.Mesh mesh = OceanLodMeshGenerator.generate(
            ocean,
            plan,
            topology,
            mask,
            8.0,
            OceanConditions.CALM
        );

        assertEquals(topology.vertexCount(), mesh.vertices().length);
        assertTrue(mesh.indices().length > 0);
        assertTrue(mesh.indices().length < topology.indices().length);
        assertEquals(0, mesh.indices().length % 3);
    }

    @Test
    void sameInputsGenerateDeterministicDisplacedVertices() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(99L);
        OceanLodPlanner.Plan plan = OceanLodPlanner.plan(OceanQuality.LOW, 0.0, 0.0);
        OceanLodTopology topology = OceanLodTopology.build(plan);
        OceanLodCoverageMask mask = OceanLodCoverageMask.build(plan, topology, (x, z) -> true);

        OceanLodMeshGenerator.Mesh first = OceanLodMeshGenerator.generate(
            ocean, plan, topology, mask, 14.25, OceanConditions.CALM
        );
        OceanLodMeshGenerator.Mesh second = OceanLodMeshGenerator.generate(
            ocean, plan, topology, mask, 14.25, OceanConditions.CALM
        );

        assertArrayEquals(first.vertices(), second.vertices());
        assertArrayEquals(first.indices(), second.indices());
        for (OceanLodMeshGenerator.Vertex vertex : first.vertices()) {
            assertEquals(1.0, vertex.normal().length(), 1.0e-9);
        }
    }

    @Test
    void zeroWaveScaleKeepsLodMeshExactlyFlat() {
        ProceduralOcean ocean = ProceduralOcean.createDefault(7L);
        OceanLodPlanner.Plan plan = OceanLodPlanner.plan(OceanQuality.POTATO, 0.0, 0.0);
        OceanLodTopology topology = OceanLodTopology.build(plan);
        OceanLodCoverageMask mask = OceanLodCoverageMask.build(plan, topology, (x, z) -> true);
        OceanConditions flat = new OceanConditions(0.0, 63.25, Vec3.ZERO);

        OceanLodMeshGenerator.Mesh mesh = OceanLodMeshGenerator.generate(ocean, plan, topology, mask, 3.0, flat);

        for (OceanLodMeshGenerator.Vertex vertex : mesh.vertices()) {
            assertEquals(63.25, vertex.y());
            assertEquals(Vec3.UP, vertex.normal());
        }
    }
}
