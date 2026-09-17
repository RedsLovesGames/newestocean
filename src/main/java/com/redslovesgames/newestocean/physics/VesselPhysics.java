package com.redslovesgames.newestocean.physics;

import com.redslovesgames.newestocean.math.Vec3;
import com.redslovesgames.newestocean.ocean.OceanConditions;
import com.redslovesgames.newestocean.ocean.OceanSurface;

import java.util.List;

public final class VesselPhysics {
    private VesselPhysics() {
    }

    public static Result solve(
        OceanSurface surface,
        OceanConditions conditions,
        double timeSeconds,
        State state,
        List<BuoyancyPoint> points,
        Parameters parameters
    ) {
        if (surface == null || conditions == null || state == null || parameters == null) {
            throw new IllegalArgumentException("solver inputs cannot be null");
        }
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("at least one buoyancy point is required");
        }

        double totalWeight = points.stream().mapToDouble(BuoyancyPoint::weight).sum();
        if (totalWeight <= 0.0) {
            throw new IllegalArgumentException("buoyancy point weights must sum to a positive value");
        }

        Vec3 totalForce = Vec3.ZERO;
        Vec3 totalTorque = Vec3.ZERO;
        double submersionSum = 0.0;
        int wetPoints = 0;

        double weightedSubmersion = 0.0;
        Vec3 weightedContactCenter = Vec3.ZERO;
        double bowLoad = 0.0;
        double sternLoad = 0.0;
        double portLoad = 0.0;
        double starboardLoad = 0.0;

        for (BuoyancyPoint point : points) {
            double normalizedWeight = point.weight() / totalWeight;
            Vec3 leverArm = state.pose().transformDirection(point.localPosition());
            Vec3 worldPoint = state.pose().position().add(leverArm);
            OceanSurface.SurfaceSample water = surface.sample(worldPoint.x(), worldPoint.z(), timeSeconds, conditions);

            double depth = water.height() - worldPoint.y();
            if (depth <= 0.0) {
                continue;
            }

            double submersion = clamp01(depth / parameters.maxSubmersionDepth());
            double contactLoad = normalizedWeight * submersion;
            wetPoints++;
            submersionSum += submersion;
            weightedSubmersion += contactLoad;
            weightedContactCenter = weightedContactCenter.add(point.localPosition().multiply(contactLoad));

            if (point.localPosition().z() >= 0.0) {
                bowLoad += contactLoad;
            } else {
                sternLoad += contactLoad;
            }
            if (point.localPosition().x() < 0.0) {
                portLoad += contactLoad;
            } else {
                starboardLoad += contactLoad;
            }

            Vec3 pointVelocity = state.linearVelocity().add(state.angularVelocity().cross(leverArm));
            Vec3 relativeVelocity = pointVelocity.subtract(water.surfaceVelocity());

            double buoyancy = parameters.mass()
                * parameters.buoyancyAcceleration()
                * normalizedWeight
                * submersion;
            double damping = -relativeVelocity.y()
                * parameters.mass()
                * parameters.verticalDamping()
                * normalizedWeight
                * submersion;

            Vec3 forceDirection = Vec3.UP.multiply(1.0 - parameters.normalInfluence())
                .add(water.normal().multiply(parameters.normalInfluence()))
                .normalize();

            double verticalMagnitude = Math.max(0.0, buoyancy + damping);
            Vec3 force = forceDirection.multiply(verticalMagnitude);

            Vec3 horizontalDrag = new Vec3(relativeVelocity.x(), 0.0, relativeVelocity.z())
                .multiply(-parameters.mass() * parameters.horizontalDrag() * normalizedWeight * submersion);
            force = force.add(horizontalDrag);

            double pointForceLimit = parameters.mass()
                * parameters.maxAcceleration()
                * normalizedWeight;
            force = force.clampLength(pointForceLimit);

            totalForce = totalForce.add(force);
            totalTorque = totalTorque.add(leverArm.cross(force));
        }

        double averageSubmersion = wetPoints == 0 ? 0.0 : submersionSum / wetPoints;
        Vec3 localCenter = weightedSubmersion <= 1.0e-12
            ? Vec3.ZERO
            : weightedContactCenter.multiply(1.0 / weightedSubmersion);
        ContactState contact = new ContactState(
            (double) wetPoints / points.size(),
            weightedSubmersion,
            localCenter,
            bowLoad,
            sternLoad,
            portLoad,
            starboardLoad
        );
        return new Result(totalForce, totalTorque, wetPoints, averageSubmersion, contact);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public record BuoyancyPoint(Vec3 localPosition, double weight) {
        public BuoyancyPoint {
            if (localPosition == null) {
                throw new IllegalArgumentException("localPosition cannot be null");
            }
            if (!Double.isFinite(weight) || weight <= 0.0) {
                throw new IllegalArgumentException("weight must be finite and positive");
            }
        }
    }

    public record Pose(Vec3 position, Vec3 right, Vec3 up, Vec3 forward) {
        public Pose {
            if (position == null || right == null || up == null || forward == null) {
                throw new IllegalArgumentException("pose vectors cannot be null");
            }
            right = right.normalize();
            up = up.normalize();
            forward = forward.normalize();
            if (right.lengthSquared() < 0.99 || up.lengthSquared() < 0.99 || forward.lengthSquared() < 0.99) {
                throw new IllegalArgumentException("pose basis vectors cannot be zero");
            }
        }

        public static Pose uprightYaw(Vec3 position, double yawRadians) {
            double sin = Math.sin(yawRadians);
            double cos = Math.cos(yawRadians);
            Vec3 right = new Vec3(cos, 0.0, -sin);
            Vec3 forward = new Vec3(sin, 0.0, cos);
            return new Pose(position, right, Vec3.UP, forward);
        }

        public Vec3 transformDirection(Vec3 local) {
            return right.multiply(local.x())
                .add(up.multiply(local.y()))
                .add(forward.multiply(local.z()));
        }
    }

    public record State(Pose pose, Vec3 linearVelocity, Vec3 angularVelocity) {
        public State {
            if (pose == null || linearVelocity == null || angularVelocity == null) {
                throw new IllegalArgumentException("state values cannot be null");
            }
        }
    }

    public record Parameters(
        double mass,
        double buoyancyAcceleration,
        double verticalDamping,
        double horizontalDrag,
        double maxSubmersionDepth,
        double normalInfluence,
        double maxAcceleration
    ) {
        public Parameters {
            requirePositiveFinite(mass, "mass");
            requirePositiveFinite(buoyancyAcceleration, "buoyancyAcceleration");
            requireNonNegativeFinite(verticalDamping, "verticalDamping");
            requireNonNegativeFinite(horizontalDrag, "horizontalDrag");
            requirePositiveFinite(maxSubmersionDepth, "maxSubmersionDepth");
            requirePositiveFinite(maxAcceleration, "maxAcceleration");
            if (!Double.isFinite(normalInfluence) || normalInfluence < 0.0 || normalInfluence > 1.0) {
                throw new IllegalArgumentException("normalInfluence must be between 0 and 1");
            }
        }

        public static Parameters smallBoat() {
            return new Parameters(1.0, 12.0, 3.0, 1.2, 0.65, 0.18, 24.0);
        }

        private static void requirePositiveFinite(double value, String name) {
            if (!Double.isFinite(value) || value <= 0.0) {
                throw new IllegalArgumentException(name + " must be finite and positive");
            }
        }

        private static void requireNonNegativeFinite(double value, String name) {
            if (!Double.isFinite(value) || value < 0.0) {
                throw new IllegalArgumentException(name + " must be finite and non-negative");
            }
        }
    }

    public record ContactState(
        double wetFraction,
        double weightedSubmersion,
        Vec3 localCenter,
        double bowLoad,
        double sternLoad,
        double portLoad,
        double starboardLoad
    ) {
        public ContactState {
            if (!Double.isFinite(wetFraction) || wetFraction < 0.0 || wetFraction > 1.0) {
                throw new IllegalArgumentException("wetFraction must be between 0 and 1");
            }
            if (!Double.isFinite(weightedSubmersion) || weightedSubmersion < 0.0 || weightedSubmersion > 1.0) {
                throw new IllegalArgumentException("weightedSubmersion must be between 0 and 1");
            }
            if (localCenter == null) {
                throw new IllegalArgumentException("localCenter cannot be null");
            }
            requireUnitLoad(bowLoad, "bowLoad");
            requireUnitLoad(sternLoad, "sternLoad");
            requireUnitLoad(portLoad, "portLoad");
            requireUnitLoad(starboardLoad, "starboardLoad");
        }

        private static void requireUnitLoad(double value, String name) {
            if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException(name + " must be between 0 and 1");
            }
        }
    }

    public record Result(
        Vec3 force,
        Vec3 torque,
        int wetPoints,
        double averageSubmersion,
        ContactState contact
    ) {
        public Result {
            if (force == null || torque == null || contact == null) {
                throw new IllegalArgumentException("result values cannot be null");
            }
            if (wetPoints < 0) {
                throw new IllegalArgumentException("wetPoints cannot be negative");
            }
            if (!Double.isFinite(averageSubmersion) || averageSubmersion < 0.0 || averageSubmersion > 1.0) {
                throw new IllegalArgumentException("averageSubmersion must be between 0 and 1");
            }
        }
    }
}
