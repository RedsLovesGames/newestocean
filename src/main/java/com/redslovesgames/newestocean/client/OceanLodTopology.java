package com.redslovesgames.newestocean.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Camera-independent local topology for concentric ocean LOD rings. */
public final class OceanLodTopology {
    private final LocalVertex[] vertices;
    private final int[] indices;
    private final double[] cellCenterX;
    private final double[] cellCenterZ;

    private OceanLodTopology(
        LocalVertex[] vertices,
        int[] indices,
        double[] cellCenterX,
        double[] cellCenterZ
    ) {
        this.vertices = vertices;
        this.indices = indices;
        this.cellCenterX = cellCenterX;
        this.cellCenterZ = cellCenterZ;
    }

    public static OceanLodTopology build(OceanLodPlanner.Plan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan is required");
        }

        List<LocalVertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Double> centerX = new ArrayList<>();
        List<Double> centerZ = new ArrayList<>();
        Map<Long, Integer> vertexLookup = new HashMap<>();

        for (OceanLodPlanner.Ring ring : plan.rings()) {
            int inner = ring.innerRadiusBlocks();
            int outer = ring.outerRadiusBlocks();
            int step = ring.gridStepBlocks();

            if (inner == 0) {
                tileRectangle(-outer, outer, -outer, outer, step, vertices, indices, centerX, centerZ, vertexLookup);
                continue;
            }

            tileRectangle(-outer, outer, inner, outer, step, vertices, indices, centerX, centerZ, vertexLookup);
            tileRectangle(-outer, outer, -outer, -inner, step, vertices, indices, centerX, centerZ, vertexLookup);
            tileRectangle(-outer, -inner, -inner, inner, step, vertices, indices, centerX, centerZ, vertexLookup);
            tileRectangle(inner, outer, -inner, inner, step, vertices, indices, centerX, centerZ, vertexLookup);
        }

        LocalVertex[] vertexArray = vertices.toArray(LocalVertex[]::new);
        int[] indexArray = indices.stream().mapToInt(Integer::intValue).toArray();
        double[] xArray = centerX.stream().mapToDouble(Double::doubleValue).toArray();
        double[] zArray = centerZ.stream().mapToDouble(Double::doubleValue).toArray();
        return new OceanLodTopology(vertexArray, indexArray, xArray, zArray);
    }

    private static void tileRectangle(
        int minX,
        int maxX,
        int minZ,
        int maxZ,
        int step,
        List<LocalVertex> vertices,
        List<Integer> indices,
        List<Double> centerX,
        List<Double> centerZ,
        Map<Long, Integer> vertexLookup
    ) {
        if (minX >= maxX || minZ >= maxZ) {
            return;
        }

        for (int z = minZ; z < maxZ; z += step) {
            int z2 = Math.min(z + step, maxZ);
            for (int x = minX; x < maxX; x += step) {
                int x2 = Math.min(x + step, maxX);
                int topLeft = vertex(vertices, vertexLookup, x, z);
                int topRight = vertex(vertices, vertexLookup, x2, z);
                int bottomLeft = vertex(vertices, vertexLookup, x, z2);
                int bottomRight = vertex(vertices, vertexLookup, x2, z2);

                indices.add(topLeft);
                indices.add(bottomLeft);
                indices.add(topRight);
                indices.add(topRight);
                indices.add(bottomLeft);
                indices.add(bottomRight);
                centerX.add((x + x2) * 0.5);
                centerZ.add((z + z2) * 0.5);
            }
        }
    }

    private static int vertex(List<LocalVertex> vertices, Map<Long, Integer> lookup, int x, int z) {
        long key = (((long) x) << 32) ^ (z & 0xffffffffL);
        Integer existing = lookup.get(key);
        if (existing != null) {
            return existing;
        }
        int index = vertices.size();
        vertices.add(new LocalVertex(x, z));
        lookup.put(key, index);
        return index;
    }

    public int vertexCount() {
        return vertices.length;
    }

    public int triangleCount() {
        return indices.length / 3;
    }

    public int cellCount() {
        return cellCenterX.length;
    }

    public LocalVertex[] vertices() {
        return vertices.clone();
    }

    public int[] indices() {
        return indices.clone();
    }

    double cellCenterX(int cell) {
        return cellCenterX[cell];
    }

    double cellCenterZ(int cell) {
        return cellCenterZ[cell];
    }

    int indexAt(int offset) {
        return indices[offset];
    }

    public record LocalVertex(int x, int z) {
    }

    private record TopologyKey(OceanQuality quality, List<OceanLodPlanner.Ring> rings) {
        private TopologyKey(OceanLodPlanner.Plan plan) {
            this(plan.quality(), plan.rings());
        }
    }

    public static final class Cache {
        private final Map<TopologyKey, OceanLodTopology> byPlanShape = new HashMap<>();

        public OceanLodTopology get(OceanLodPlanner.Plan plan) {
            if (plan == null) {
                throw new IllegalArgumentException("plan is required");
            }
            TopologyKey key = new TopologyKey(plan);
            return byPlanShape.computeIfAbsent(key, ignored -> OceanLodTopology.build(plan));
        }

        public void clear() {
            byPlanShape.clear();
        }
    }
}
