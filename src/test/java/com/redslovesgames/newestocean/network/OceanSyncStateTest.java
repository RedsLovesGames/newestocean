package com.redslovesgames.newestocean.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanSyncStateTest {
    @Test
    void startsUnsynchronized() {
        OceanSyncState state = new OceanSyncState();

        assertFalse(state.isSynchronized());
        assertEquals(0L, state.seed());
        assertEquals(0L, state.generation());
    }

    @Test
    void acceptingSeedMarksStateSynchronizedAndAdvancesGeneration() {
        OceanSyncState state = new OceanSyncState();

        state.accept(1234L);

        assertTrue(state.isSynchronized());
        assertEquals(1234L, state.seed());
        assertEquals(1L, state.generation());
    }

    @Test
    void resetClearsStaleSeedAndAdvancesGeneration() {
        OceanSyncState state = new OceanSyncState();
        state.accept(1234L);

        state.reset();

        assertFalse(state.isSynchronized());
        assertEquals(0L, state.seed());
        assertEquals(2L, state.generation());
    }

    @Test
    void repeatedAcceptOfSameSeedStillRepresentsFreshSessionSync() {
        OceanSyncState state = new OceanSyncState();
        state.accept(55L);
        long firstGeneration = state.generation();

        state.reset();
        state.accept(55L);

        assertTrue(state.isSynchronized());
        assertEquals(55L, state.seed());
        assertTrue(state.generation() > firstGeneration);
    }
}
