package com.redslovesgames.newestocean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NewestOceanStateTest {
    @Test
    void clientSeedChangesDoNotMutateAuthoritativeServerOcean() {
        NewestOcean.setServerOceanSeed(111L);
        long serverOceanInternalSeed = NewestOcean.serverOcean().seed();

        NewestOcean.setClientOceanSeed(222L);

        assertEquals(111L, NewestOcean.serverOceanSeed());
        assertEquals(serverOceanInternalSeed, NewestOcean.serverOcean().seed());
        assertEquals(222L, NewestOcean.clientOceanSeed());
        assertNotEquals(NewestOcean.serverOcean().seed(), NewestOcean.clientOcean().seed());
    }

    @Test
    void serverSeedChangesDoNotMutateClientReconstruction() {
        NewestOcean.setClientOceanSeed(333L);
        long clientOceanInternalSeed = NewestOcean.clientOcean().seed();

        NewestOcean.setServerOceanSeed(444L);

        assertEquals(333L, NewestOcean.clientOceanSeed());
        assertEquals(clientOceanInternalSeed, NewestOcean.clientOcean().seed());
        assertEquals(444L, NewestOcean.serverOceanSeed());
        assertNotEquals(NewestOcean.serverOcean().seed(), NewestOcean.clientOcean().seed());
    }
}
