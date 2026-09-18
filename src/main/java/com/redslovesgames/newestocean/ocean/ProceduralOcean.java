package com.redslovesgames.newestocean.ocean;

import com.redslovesgames.newestocean.math.Vec3;

import java.util.List;

public final class ProceduralOcean implements OceanSurface {
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
        return new ProceduralOcean(seed, OceanSpectrum.generate(seed).components());
    }

    @Override
    public SurfaceSample sample(double x, double z, double timeSeconds, OceanConditions conditions) {
        return samplePhysical(x, z, timeSeconds, conditions);
    }

    /**
     * Samples the fixed dominant physical subset used by authoritative simulation.
     * Custom one-off oceans with fewer components use all components they contain.
     */
    public SurfaceSample samplePhysical(double x, double z, double timeSeconds, OceanConditions conditions) {
        return sample(
            x,
            z,
            timeSeconds,
            conditions,
            Math.min(OceanSpectrum.PHYSICS_COMPONENTS, components.size())
        );
    }

    /**
     * Samples only the longest {@code componentLimit} waves. Rendering may increase or lower this
     * visible budget, while authoritative physics should use {@link #samplePhysical(double, double, double, OceanConditions)}.
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
        if (conditions.waveScale() == 0.0) {
            return new SurfaceSample(
                conditions.tideOffset(),
                Vec3.UP,
                conditions.current(),
                Vec3.ZERO
            );
        }

        double displacementX = 0.0;
        double displacementY = 0.0;
        double displacementZ = 0.0;

        double tangentXx = 1.0;
        double tangentXy = 0.0;
        double tangentXz = 0.0;
        double tangentZx = 0.0;
        double tangentZy = 0.0;
        double tangentZz = 1.0;

        double velocityX = conditions.current().x();
        double velocityY = conditions.current().y();
        double velocityZ = conditions.current().z();

        for (int i = 0; i < componentLimit; i++) {
            WaveComponent wave = components.get(i);
            double amplitude = wave.amplitude() * conditions.waveScale();
            double waveNumber = wave.waveNumber();
            double directionX = wave.directionX();
            double directionZ = wave.directionZ();
            double theta = waveNumber * (directionX * x + directionZ * z)
                - wave.angularFrequency() * timeSeconds
                + wave.phase();
            double sin = Math.sin(theta);
            double cos = Math.cos(theta);
            double steepnessAmplitude = wave.steepness() * amplitude;

            displacementX += steepnessAmplitude * directionX * cos;
            displacementY += amplitude * sin;
            displacementZ += steepnessAmplitude * directionZ * cos;

            double horizontalDerivative = steepnessAmplitude * waveNumber * sin;
            tangentXx -= horizontalDerivative * directionX * directionX;
            tangentXy += amplitude * waveNumber * directionX * cos;
            tangentXz -= horizontalDerivative * directionX * directionZ;
            tangentZx -= horizontalDerivative * directionX * directionZ;
            tangentZy += amplitude * waveNumber * directionZ * cos;
            tangentZz -= horizontalDerivative * directionZ * directionZ;

            double horizontalVelocity = steepnessAmplitude * wave.angularFrequency() * sin;
            velocityX += horizontalVelocity * directionX;
            velocityY -= amplitude * wave.angularFrequency() * cos;
            velocityZ += horizontalVelocity * directionZ;
        }

        Vec3 tangentX = new Vec3(tangentXx, tangentXy, tangentXz);
        Vec3 tangentZ = new Vec3(tangentZx, tangentZy, tangentZz);
        Vec3 normal = tangentZ.cross(tangentX).normalize();
        if (normal.lengthSquared() < 1.0e-18) {
            normal = Vec3.UP;
        } else if (normal.y() < 0.0) {
            normal = normal.multiply(-1.0);
        }

        Vec3 displacement = new Vec3(displacementX, displacementY, displacementZ);
        Vec3 velocity = new Vec3(velocityX, velocityY, velocityZ);
        double height = conditions.tideOffset() + displacementY;
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
