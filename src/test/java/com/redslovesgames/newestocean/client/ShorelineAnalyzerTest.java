package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShorelineAnalyzerTest {
    private static final int SEA_LEVEL = 64;

    @Test
    void analyzerFindsNormalizedDirectionTowardNearestLand() {
        Fixture fixture = fixture((x, y, z) -> x < 2 && y >= 61 && y <= 63, (x, z) -> x < 2);
        int cell = findCell(fixture.topology(), 1.5, 0.5);

        ShorelineSample sample = fixture.field().sample(cell);

        assertTrue(sample.shoreInfluence() > 0.0);
        assertEquals(3.0, sample.depthBlocks(), 1.0e-9);
        assertEquals(1.0, Math.hypot(sample.shoreDirectionX(), sample.shoreDirectionZ()), 1.0e-9);
        assertTrue(sample.shoreDirectionX() > 0.99);
    }

    @Test
    void noLandInsideRadiusProducesNoInfluence() {
        Fixture fixture = fixture((x, y, z) -> y >= 61 && y <= 63, (x, z) -> true);
        int cell = findCell(fixture.topology(), 0.5, 0.5);
        assertEquals(0.0, fixture.field().sample(cell).shoreInfluence(), 1.0e-9);
    }

    @Test
    void dryCoverageCellsNeverReceiveInfluence() {
        Fixture fixture = fixture((x, y, z) -> x < 2 && y >= 61 && y <= 63, (x, z) -> x < 0);
        int dryCell = findCell(fixture.topology(), 1.5, 0.5);
        assertSame(ShorelineSample.NONE, fixture.field().sample(dryCell));
    }

    @Test
    void depthProbeNeverExceedsTwelveBlocks() {
        Fixture fixture = fixture((x, y, z) -> {
            if (y < SEA_LEVEL - 12) {
                throw new AssertionError("depth probe exceeded 12 blocks");
            }
            return x < 2;
        }, (x, z) -> x < 2);
        int cell = findCell(fixture.topology(), 1.5, 0.5);
        assertEquals(12.0, fixture.field().sample(cell).depthBlocks(), 1.0e-9);
    }

    @Test
    void deepWaterSkipsHorizontalShoreSearch() {
        OceanLodPlanner.Plan plan = new OceanLodPlanner.Plan(
            OceanQuality.MEDIUM,
            0.0,
            0.0,
            List.of(new OceanLodPlanner.Ring(0, 4, 1)),
            4
        );
        OceanLodTopology topology = OceanLodTopology.build(plan);
        OceanLodCoverageMask coverage = OceanLodCoverageMask.build(
            plan,
            topology,
            (x, z) -> x >= 0.0 && x < 1.0 && z >= 0.0 && z < 1.0
        );
        AtomicInteger offColumnSurfaceProbes = new AtomicInteger();
        ShorelineField field = ShorelineAnalyzer.build(
            plan,
            topology,
            coverage,
            SEA_LEVEL,
            OceanQuality.MEDIUM,
            (x, y, z) -> {
                if (y == SEA_LEVEL - 1 && (x != 0 || z != 0)) {
                    offColumnSurfaceProbes.incrementAndGet();
                }
                return true;
            }
        );

        int cell = findCell(topology, 0.5, 0.5);
        assertSame(ShorelineSample.NONE, field.sample(cell));
        assertEquals(0, offColumnSurfaceProbes.get());
    }

    @Test
    void identicalProbeDataProducesIdenticalSamples() {
        Fixture first = fixture((x, y, z) -> x < 2 && y >= 61 && y <= 63, (x, z) -> x < 2);
        Fixture second = fixture((x, y, z) -> x < 2 && y >= 61 && y <= 63, (x, z) -> x < 2);
        int cell = findCell(first.topology(), 1.5, 0.5);
        assertEquals(first.field().sample(cell), second.field().sample(cell));
    }

    private static Fixture fixture(ShorelineAnalyzer.Probe probe, OceanLodCoverageMask.ColumnProbe coverageProbe) {
        OceanLodPlanner.Plan plan = new OceanLodPlanner.Plan(
            OceanQuality.MEDIUM,
            0.0,
            0.0,
            List.of(new OceanLodPlanner.Ring(0, 4, 1)),
            4
        );
        OceanLodTopology topology = OceanLodTopology.build(plan);
        OceanLodCoverageMask coverage = OceanLodCoverageMask.build(plan, topology, coverageProbe);
        ShorelineField field = ShorelineAnalyzer.build(plan, topology, coverage, SEA_LEVEL, OceanQuality.MEDIUM, probe);
        return new Fixture(topology, field);
    }

    private static int findCell(OceanLodTopology topology, double centerX, double centerZ) {
        for (int cell = 0; cell < topology.cellCount(); cell++) {
            if (Double.compare(topology.cellCenterX(cell), centerX) == 0
                && Double.compare(topology.cellCenterZ(cell), centerZ) == 0) {
                return cell;
            }
        }
        throw new AssertionError("expected topology cell not found");
    }

    private record Fixture(OceanLodTopology topology, ShorelineField field) {
    }
}
