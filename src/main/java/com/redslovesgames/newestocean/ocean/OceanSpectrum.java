package com.redslovesgames.newestocean.ocean;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Deterministic ordered pool of ocean waves shared by rendering and authoritative physics.
 * The first {@link #PHYSICS_COMPONENTS} entries are the dominant synchronized physical subset.
 */
public final class OceanSpectrum {
    public static final int MAX_COMPONENTS = 24;
    public static final int PHYSICS_COMPONENTS = 6;

    private static final double TWO_PI = Math.PI * 2.0;
    private static final long SPECTRUM_SALT = 0x4F4345414E535043L;

    private final long seed;
    private final List<WaveComponent> components;

    private OceanSpectrum(long seed, List<WaveComponent> components) {
        if (components.size() != MAX_COMPONENTS) {
            throw new IllegalArgumentException("spectrum must contain exactly " + MAX_COMPONENTS + " components");
        }
        this.seed = seed;
        this.components = List.copyOf(components);
    }

    public static OceanSpectrum generate(long seed) {
        SplittableRandom random = new SplittableRandom(seed ^ SPECTRUM_SALT);
        double dominantDirection = random.nextDouble(0.0, TWO_PI);

        List<WaveComponent> waves = new ArrayList<>(MAX_COMPONENTS);

        // Dominant physical swell. The non-overlapping wavelength bands keep this prefix ordered
        // longest-first while preserving the original six-wave scale and feel.
        waves.add(createBand(random, dominantDirection, 10.0, 0.34, 0.52, 52.0, 84.0, 0.34, 0.46, 0.48, 0.62));
        waves.add(createBand(random, dominantDirection, 18.0, 0.22, 0.36, 30.0, 50.0, 0.38, 0.52, 0.48, 0.62));
        waves.add(createBand(random, dominantDirection, 28.0, 0.14, 0.24, 17.0, 30.0, 0.42, 0.58, 0.48, 0.62));
        waves.add(createBand(random, dominantDirection, 42.0, 0.08, 0.15, 9.0, 17.0, 0.44, 0.62, 0.48, 0.62));
        waves.add(createBand(random, dominantDirection, 65.0, 0.04, 0.09, 4.5, 9.0, 0.40, 0.58, 0.48, 0.62));
        waves.add(createBand(random, dominantDirection, 90.0, 0.02, 0.05, 2.5, 5.0, 0.32, 0.50, 0.48, 0.62));

        // Fine visual spectrum. Each wavelength occupies its own descending geometric band,
        // so any prefix is deterministic and remains longest/strongest first for LOD use.
        int detailCount = MAX_COMPONENTS - PHYSICS_COMPONENTS;
        double longestDetail = 2.4;
        double shortestDetail = 0.45;
        for (int i = 0; i < detailCount; i++) {
            double bandStart = geometric(longestDetail, shortestDetail, (double) i / detailCount);
            double bandEnd = geometric(longestDetail, shortestDetail, (double) (i + 1) / detailCount);
            double progress = detailCount == 1 ? 1.0 : (double) i / (detailCount - 1);

            double maxAmplitude = geometric(0.018, 0.0025, progress);
            double minAmplitude = maxAmplitude * 0.68;
            double spreadDegrees = 100.0 + 75.0 * progress;
            double minSteepness = 0.20 + 0.05 * progress;
            double maxSteepness = 0.40 + 0.05 * progress;

            waves.add(createBand(
                random,
                dominantDirection,
                spreadDegrees,
                minAmplitude,
                maxAmplitude,
                bandEnd,
                bandStart,
                minSteepness,
                maxSteepness,
                0.50,
                0.68
            ));
        }

        return new OceanSpectrum(seed, waves);
    }

    private static double geometric(double start, double end, double progress) {
        return start * Math.pow(end / start, progress);
    }

    private static WaveComponent createBand(
        SplittableRandom random,
        double dominantDirection,
        double spreadDegrees,
        double minAmplitude,
        double maxAmplitude,
        double minWavelength,
        double maxWavelength,
        double minSteepness,
        double maxSteepness,
        double minSpeedScale,
        double maxSpeedScale
    ) {
        double spread = Math.toRadians(spreadDegrees);
        double direction = dominantDirection + random.nextDouble(-spread, spread);
        double amplitude = random.nextDouble(minAmplitude, maxAmplitude);
        double wavelength = random.nextDouble(minWavelength, maxWavelength);
        double phase = random.nextDouble(0.0, TWO_PI);
        double steepness = random.nextDouble(minSteepness, maxSteepness);
        double speedScale = random.nextDouble(minSpeedScale, maxSpeedScale);

        return WaveComponent.deepWater(
            amplitude,
            wavelength,
            Math.cos(direction),
            Math.sin(direction),
            phase,
            steepness,
            speedScale
        );
    }

    public long seed() {
        return seed;
    }

    public List<WaveComponent> components() {
        return components;
    }

    public List<WaveComponent> physicalComponents() {
        return components.subList(0, PHYSICS_COMPONENTS);
    }
}
