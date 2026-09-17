package com.redslovesgames.newestocean.network;

/**
 * Small logical-connection state holder used to reject stale client ocean state between sessions.
 * It deliberately stores only the deterministic seed and whether that seed belongs to the current
 * play connection.
 */
public final class OceanSyncState {
    private long seed;
    private long generation;
    private boolean synchronizedState;

    public void accept(long seed) {
        this.seed = seed;
        this.synchronizedState = true;
        this.generation++;
    }

    public void reset() {
        this.seed = 0L;
        this.synchronizedState = false;
        this.generation++;
    }

    public long seed() {
        return seed;
    }

    public long generation() {
        return generation;
    }

    public boolean isSynchronized() {
        return synchronizedState;
    }
}
