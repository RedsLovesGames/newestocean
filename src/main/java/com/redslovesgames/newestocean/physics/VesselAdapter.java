package com.redslovesgames.newestocean.physics;

import java.util.List;

/**
 * Boundary between the ocean solver and a concrete vessel implementation.
 *
 * <p>The core deliberately uses {@link Object} here so optional ship mods do not become hard runtime
 * dependencies. Compatibility modules can use direct types, Mixins, interfaces, or reflection behind
 * this boundary.</p>
 */
public interface VesselAdapter {
    String id();

    boolean supports(Object vessel);

    VesselPhysics.State capture(Object vessel);

    List<VesselPhysics.BuoyancyPoint> buoyancyPoints(Object vessel);

    VesselPhysics.Parameters parameters(Object vessel);

    void apply(Object vessel, VesselPhysics.Result result, double deltaSeconds);
}
