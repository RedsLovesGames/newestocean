package com.redslovesgames.newestocean.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Small rolling visual history for one vessel's wake. */
public final class VesselWakeHistory {
    static final int MAX_SAMPLES = 12;
    static final double MIN_SAMPLE_DISTANCE = 0.75;
    static final double MIN_SPEED = 0.12;
    static final double LIFETIME_SECONDS = 4.0;

    private final Deque<Sample> samples = new ArrayDeque<>();

    public boolean record(
        double x,
        double z,
        double directionX,
        double directionZ,
        double speedBlocksPerTick,
        double beamBlocks,
        double nowSeconds
    ) {
        validateFinite(x, z, directionX, directionZ, speedBlocksPerTick, beamBlocks, nowSeconds);
        if (speedBlocksPerTick < MIN_SPEED || beamBlocks <= 0.0) {
            return false;
        }

        double directionLength = Math.hypot(directionX, directionZ);
        if (directionLength < 1.0e-9) {
            return false;
        }
        directionX /= directionLength;
        directionZ /= directionLength;

        Sample last = samples.peekLast();
        if (last != null && Math.hypot(x - last.x, z - last.z) < MIN_SAMPLE_DISTANCE) {
            return false;
        }

        expire(nowSeconds);
        double speedResponse = clamp01((speedBlocksPerTick - MIN_SPEED) / 0.55);
        double beamResponse = 0.75 + 0.25 * clamp01((beamBlocks - 1.0) / 5.0);
        double initialStrength = clamp01(speedResponse * beamResponse);
        if (initialStrength <= 0.0) {
            return false;
        }

        samples.addLast(new Sample(
            x,
            z,
            directionX,
            directionZ,
            speedBlocksPerTick,
            beamBlocks,
            nowSeconds,
            initialStrength
        ));
        while (samples.size() > MAX_SAMPLES) {
            samples.removeFirst();
        }
        return true;
    }

    public List<VesselWakeDescriptor> snapshot(double nowSeconds) {
        if (!Double.isFinite(nowSeconds)) {
            throw new IllegalArgumentException("wake time must be finite");
        }
        expire(nowSeconds);
        List<VesselWakeDescriptor> result = new ArrayList<>(samples.size());
        for (Sample sample : samples) {
            double age = Math.max(0.0, nowSeconds - sample.createdAtSeconds);
            double ageFade = clamp01(1.0 - age / LIFETIME_SECONDS);
            result.add(new VesselWakeDescriptor(
                sample.x,
                sample.z,
                sample.directionX,
                sample.directionZ,
                sample.speedBlocksPerTick,
                sample.beamBlocks,
                age,
                clamp01(sample.initialStrength * ageFade)
            ));
        }
        return List.copyOf(result);
    }

    public void clear() {
        samples.clear();
    }

    public boolean isEmpty() {
        return samples.isEmpty();
    }

    private void expire(double nowSeconds) {
        while (!samples.isEmpty() && nowSeconds - samples.peekFirst().createdAtSeconds >= LIFETIME_SECONDS) {
            samples.removeFirst();
        }
    }

    private static void validateFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("wake history values must be finite");
            }
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record Sample(
        double x,
        double z,
        double directionX,
        double directionZ,
        double speedBlocksPerTick,
        double beamBlocks,
        double createdAtSeconds,
        double initialStrength
    ) {
    }
}
