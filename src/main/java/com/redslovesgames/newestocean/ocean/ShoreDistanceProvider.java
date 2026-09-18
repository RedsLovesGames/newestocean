package com.redslovesgames.newestocean.ocean;

@FunctionalInterface
public interface ShoreDistanceProvider {
    double distanceToLand(double x, double z);
}
