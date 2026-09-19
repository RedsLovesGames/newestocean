package com.redslovesgames.newestocean.client;

/** Pure X/Z geometry for one visual wake segment. Y is sampled from the ocean at render time. */
public final class VesselWakeGeometry {
    private VesselWakeGeometry() {
    }

    public static Segment segment(VesselWakeDescriptor newer, VesselWakeDescriptor older) {
        if (newer == null || older == null) {
            throw new IllegalArgumentException("wake descriptors are required");
        }

        double directionX = newer.directionX();
        double directionZ = newer.directionZ();
        double perpendicularX = -directionZ;
        double perpendicularZ = directionX;

        double baseHalfWidth = Math.max(0.35, newer.beamBlocks() * 0.45);
        double newerExpansion = 1.0 + Math.min(newer.ageSeconds() / 4.0, 1.0) * 0.9;
        double olderExpansion = 1.0 + Math.min(older.ageSeconds() / 4.0, 1.0) * 0.9;
        double widthAtNewer = baseHalfWidth * newerExpansion;
        double widthAtOlder = Math.max(widthAtNewer, Math.max(0.35, older.beamBlocks() * 0.45) * olderExpansion);

        Strip leftArm = arm(newer, older, perpendicularX, perpendicularZ, widthAtNewer, widthAtOlder, 1.0);
        Strip rightArm = arm(newer, older, perpendicularX, perpendicularZ, widthAtNewer, widthAtOlder, -1.0);

        double centerNewer = Math.max(0.08, widthAtNewer * 0.14);
        double centerOlder = Math.max(0.10, widthAtOlder * 0.18);
        Strip center = new Strip(
            offset(newer.x(), newer.z(), perpendicularX, perpendicularZ, -centerNewer),
            offset(newer.x(), newer.z(), perpendicularX, perpendicularZ, centerNewer),
            offset(older.x(), older.z(), perpendicularX, perpendicularZ, centerOlder),
            offset(older.x(), older.z(), perpendicularX, perpendicularZ, -centerOlder)
        );

        return new Segment(leftArm, rightArm, center, widthAtNewer, widthAtOlder);
    }

    private static Strip arm(
        VesselWakeDescriptor newer,
        VesselWakeDescriptor older,
        double perpendicularX,
        double perpendicularZ,
        double widthAtNewer,
        double widthAtOlder,
        double side
    ) {
        double innerRatio = 0.58;
        return new Strip(
            offset(newer.x(), newer.z(), perpendicularX, perpendicularZ, side * widthAtNewer * innerRatio),
            offset(newer.x(), newer.z(), perpendicularX, perpendicularZ, side * widthAtNewer),
            offset(older.x(), older.z(), perpendicularX, perpendicularZ, side * widthAtOlder),
            offset(older.x(), older.z(), perpendicularX, perpendicularZ, side * widthAtOlder * innerRatio)
        );
    }

    private static Point offset(double x, double z, double perpendicularX, double perpendicularZ, double amount) {
        return new Point(x + perpendicularX * amount, z + perpendicularZ * amount);
    }

    public record Point(double x, double z) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("wake geometry points must be finite");
            }
        }
    }

    public record Strip(Point newerInner, Point newerOuter, Point olderOuter, Point olderInner) {
        public Strip {
            if (newerInner == null || newerOuter == null || olderOuter == null || olderInner == null) {
                throw new IllegalArgumentException("wake strip points are required");
            }
        }
    }

    public record Segment(
        Strip leftArm,
        Strip rightArm,
        Strip center,
        double widthAtNewer,
        double widthAtOlder
    ) {
        public Segment {
            if (leftArm == null || rightArm == null || center == null
                || !Double.isFinite(widthAtNewer) || !Double.isFinite(widthAtOlder)
                || widthAtNewer <= 0.0 || widthAtOlder <= 0.0) {
                throw new IllegalArgumentException("wake segment is invalid");
            }
        }
    }
}
