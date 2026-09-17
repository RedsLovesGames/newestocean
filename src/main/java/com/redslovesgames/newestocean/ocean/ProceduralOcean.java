package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public final class ProceduralOcean implements OceanSurface {
    private static final double TWO_PI = Math.PI * 2.0;

    private final long seed;
    private final List<WaveComponent> components;

    public ProceduralOcean(long seed, List<WaveComponent> components) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("at least one wave component is required");
        }
        this.seed = seed;
        this.components = List.copyOf(components);
    }

    public static ProceduralOcean createDefault(long seed) {
        SplittableRandom random = new SplittableRandom(seed ^ 0x4F4345414E4C4F4EL);
        double dominantDirection = random.nextDouble(0.0, TWO_PI);

        List<WaveComponent> waves = new ArrayList<>(6);
        waves.add(createBand(random, dominantDirection, Math.toRadians(10.0), 0.34, 0.52, 52.0, 84.0, 0.34, 0.46));
        waves.add(createBand(random, dominantDirection, Math.toRadians(18.0), 0.22, 0.36, 30.0, 50.0, 0.38, 0.52));
        waves.add(createBand(random, dominantDirection, Math.toRadians(28.0), 0.14, 0.24, 17.0, 30.0, 0.42, 0.58));
        waves.add(createBand(random, dominantDirection, Math.toRadians(42.0), 0.08, 0.15, 9.0, 17.0, 0.44, 0.62));
        waves.add(createBand(random, dominantDirection, Math.toRadians(65.0), 0.04, 0.09, 4.5, 9.0, 0.40, 0.58));
        waves.add(createBand(random, dominantDirection, Math.toRadians(90.0), 0.02, 0.05, 2.5, 5.0, 0.32, 0.50));

        return new ProceduralOcean(seed, waves);
    }

    private static WaveComponent createBand(
        SplittableRandom random,
        double dominantDirection,
        double spread,
        double minAmplitude,
        double maxAmplitude,
        double minWavelength,
        double maxWavelength,
        double minSteepness,
        double maxSteepness
    ) {
        double direction = dominantDirection + random.nextDouble(-spread, spread);
        double amplitude = random.nextDouble(minAmplitude, maxAmplitude);
        double wavelength = random.nextDouble(minWavelength, maxWavelength);
        double phase = random.nextDouble(0.0, TWO_PI);
        double steepness = random.nextDouble(minSteepness, maxSteepness);
        double speedScale = random.nextDouble(0.48, 0.62);

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

    @Override
    public SurfaceSample sample(double x, double z, double timeSeconds, OceanConditions conditions) {
        return sample(x, z, timeSeconds, conditions, components.size());
    }

    /**
     * Samples only the largest {@code componentLimit} waves. Physical simulation should use the
     * four-argument overload, while rendering may lower this value on weaker hardware.
     */
    public SurfaceSample sample(
        double x,
        double z,
        double timeSeconds,
        OceanConditions conditions,
        int componentLimit
    ) {
        if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(timeSeconds)) {
            throw new IllegalArgumentException("sample coordinates and time must be finite");
        }
        if (conditions == null) {
            throw new IllegalArgumentException("conditions cannot be null");
        }
        if (componentLimit < 1 || componentLimit > components.size()) {
            throw new IllegalArgumentException("componentLimit must be between 1 and " + components.size());
        }

        double height = conditions.tideOffset();
        double slopeX = 0.0;
        double slopeZ = 0.0;
        double displacementX = 0.0;
        double displacementZ = 0.0;
        double velocityX = conditions.current().x();
        double velocityY = conditions.current().y();
        double velocityZ = conditions.current().z();

        for (int i = 0; i < componentLimit; i++) {
            WaveComponent wave = components.get(i);
            double amplitude = wave.amplitude() * conditions.waveScale();
            double k = wave.waveNumber();
            double theta = k * (wave.directionX() * x + wave.directionZ() * z)
                - wave.angularFrequency() * timeSeconds
                + wave.phase();
            double sin = Math.sin(theta);
            double cos = Math.cos(theta);

            height += amplitude * sin;
            slopeX += amplitude * k * wave.directionX() * cos;
            slopeZ += amplitude * k * wave.directionZ() * cos;

            double horizontalAmount = wave.steepness() * amplitude * cos;
            displacementX += horizontalAmount * wave.directionX();
            displacementZ += horizontalAmount * wave.directionZ();

            double horizontalVelocity = wave.steepness() * amplitude * wave.angularFrequency() * sin;
            velocityX += horizontalVelocity * wave.directionX();
            velocityY -= amplitude * wave.angularFrequency() * cos;
            velocityZ += horizontalVelocity * wave.directionZ();
        }

        Vec3 normal = new Vec3(-slopeX, 1.0, -slopeZ).normalize();
        Vec3 velocity = new Vec3(velocityX, velocityY, velocityZ);
        Vec3 displacement = new Vec3(displacementX, 0.0, displacementZ);
        return new SurfaceSample(height, normal, velocity, displacement);
    }

    public long seed() {
        return seed;
    }

    public int componentCount() {
        return components.size();
    }

    public List<WaveComponent> components() {
        return components;
    }
}
