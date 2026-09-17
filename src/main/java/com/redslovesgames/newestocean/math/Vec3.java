package com.redslovesgames.newestocean.math;

public record Vec3(double x, double y, double z) {
    public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
    public static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 multiply(double scalar) {
        return new Vec3(x * scalar, y * scalar, z * scalar);
    }

    public double dot(Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vec3 cross(Vec3 other) {
        return new Vec3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        );
    }

    public double lengthSquared() {
        return dot(this);
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public Vec3 normalize() {
        double length = length();
        if (length < 1.0e-9) {
            return ZERO;
        }
        return multiply(1.0 / length);
    }

    public Vec3 clampLength(double maxLength) {
        if (maxLength < 0.0) {
            throw new IllegalArgumentException("maxLength must be non-negative");
        }
        double lengthSquared = lengthSquared();
        double maxSquared = maxLength * maxLength;
        if (lengthSquared <= maxSquared || lengthSquared < 1.0e-18) {
            return this;
        }
        return multiply(maxLength / Math.sqrt(lengthSquared));
    }
}
