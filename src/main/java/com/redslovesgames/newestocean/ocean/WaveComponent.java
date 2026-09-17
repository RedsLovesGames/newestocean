package com.redslovesgames.newestocean.ocean;

public record WaveComponent(
    double amplitude,
    double wavelength,
    double directionX,
    double directionZ,
    double phase,
    double angularFrequency,
    double steepness
) {
    public WaveComponent {
        if (!Double.isFinite(amplitude) || amplitude < 0.0) {
            throw new IllegalArgumentException("amplitude must be finite and non-negative");
        }
        if (!Double.isFinite(wavelength) || wavelength <= 0.0) {
            throw new IllegalArgumentException("wavelength must be finite and positive");
        }
        if (!Double.isFinite(directionX) || !Double.isFinite(directionZ)) {
            throw new IllegalArgumentException("direction must be finite");
        }
        double directionLength = Math.hypot(directionX, directionZ);
        if (directionLength < 1.0e-9) {
            throw new IllegalArgumentException("direction cannot be zero");
        }
        directionX /= directionLength;
        directionZ /= directionLength;

        if (!Double.isFinite(phase)) {
            throw new IllegalArgumentException("phase must be finite");
        }
        if (!Double.isFinite(angularFrequency) || angularFrequency < 0.0) {
            throw new IllegalArgumentException("angularFrequency must be finite and non-negative");
        }
        if (!Double.isFinite(steepness) || steepness < 0.0 || steepness > 1.0) {
            throw new IllegalArgumentException("steepness must be between 0 and 1");
        }
    }

    public double waveNumber() {
        return Math.PI * 2.0 / wavelength;
    }

    public static WaveComponent deepWater(
        double amplitude,
        double wavelength,
        double directionX,
        double directionZ,
        double phase,
        double steepness,
        double speedScale
    ) {
        double waveNumber = Math.PI * 2.0 / wavelength;
        double angularFrequency = Math.sqrt(9.81 * waveNumber) * speedScale;
        return new WaveComponent(
            amplitude,
            wavelength,
            directionX,
            directionZ,
            phase,
            angularFrequency,
            steepness
        );
    }
}
