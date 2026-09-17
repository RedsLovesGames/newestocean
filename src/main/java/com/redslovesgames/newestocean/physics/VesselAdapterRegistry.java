package com.redslovesgames.newestocean.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VesselAdapterRegistry {
    private final List<VesselAdapter> adapters = new ArrayList<>();

    public synchronized void register(VesselAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("adapter cannot be null");
        }
        if (adapters.stream().anyMatch(existing -> existing.id().equals(adapter.id()))) {
            throw new IllegalArgumentException("adapter id already registered: " + adapter.id());
        }
        adapters.add(adapter);
    }

    public synchronized Optional<VesselAdapter> find(Object vessel) {
        if (vessel == null) {
            return Optional.empty();
        }
        return adapters.stream().filter(adapter -> adapter.supports(vessel)).findFirst();
    }

    public synchronized List<String> adapterIds() {
        return adapters.stream().map(VesselAdapter::id).toList();
    }
}
