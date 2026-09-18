package com.redslovesgames.newestocean.ocean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoreDistanceGridTest {
    @Test
    void distanceTracksNearestLandAndClampsAtFifteenBlocks() {
        boolean[][] land = new boolean[1][21];
        land[0][0] = true;
        ShoreDistanceGrid grid = ShoreDistanceGrid.fromLandMask(land);

        assertEquals(0.0, grid.distanceToLand(0.0, 0.0), 0.0);
        assertEquals(5.0, grid.distanceToLand(5.0, 0.0), 0.0);
        assertEquals(15.0, grid.distanceToLand(15.0, 0.0), 0.0);
        assertEquals(15.0, grid.distanceToLand(20.0, 0.0), 0.0);
    }

    @Test
    void multipleSourcesUseNearestLand() {
        boolean[][] land = new boolean[1][11];
        land[0][0] = true;
        land[0][10] = true;
        ShoreDistanceGrid grid = ShoreDistanceGrid.fromLandMask(land);

        assertEquals(4.0, grid.distanceToLand(4.0, 0.0), 0.0);
        assertEquals(4.0, grid.distanceToLand(6.0, 0.0), 0.0);
    }

    @Test
    void noLandInGridMeansFullOffshoreDistance() {
        ShoreDistanceGrid grid = ShoreDistanceGrid.fromLandMask(new boolean[4][4]);

        assertEquals(15.0, grid.distanceToLand(1.0, 2.0), 0.0);
    }
}
