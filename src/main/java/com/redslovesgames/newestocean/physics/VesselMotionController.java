package com.redslovesgames.newestocean.physics;

/**
 * Pure vessel motion state machine. It decides when Newest Ocean water forces may act,
 * independently from Minecraft entity plumbing.
 */
public final class VesselMotionController {
    private static final double LAUNCH_CONTACT_MAX = 0.45;
    private static final double LAUNCH_RELATIVE_UP_SPEED = 0.90;
    private static final double LAUNCH_FORWARD_SPEED = 0.60;
    private static final double AIRBORNE_CONTACT_MAX = 0.08;
    private static final double RECONTACT_CONTACT_MIN = 0.12;
    private static final double FULL_SUPPORT_CONTACT = 0.55;

    private VesselMotionController() {
    }

    public static State advance(State previous, Input input) {
        if (previous == null || input == null) {
            throw new IllegalArgumentException("state and input cannot be null");
        }

        Mode next = previous.mode();
        double relativeVertical = input.vesselVerticalVelocity() - input.waterVerticalVelocity();

        switch (previous.mode()) {
            case DISPLACEMENT -> {
                if (input.contactFraction() <= LAUNCH_CONTACT_MAX
                    && relativeVertical >= LAUNCH_RELATIVE_UP_SPEED
                    && input.forwardSpeed() >= LAUNCH_FORWARD_SPEED) {
                    next = Mode.LAUNCHING;
                }
            }
            case LAUNCHING -> {
                if (input.contactFraction() <= AIRBORNE_CONTACT_MAX) {
                    next = Mode.AIRBORNE;
                } else if (input.contactFraction() >= FULL_SUPPORT_CONTACT && relativeVertical <= 0.0) {
                    next = Mode.DISPLACEMENT;
                }
            }
            case AIRBORNE -> {
                if (input.contactFraction() >= RECONTACT_CONTACT_MIN && relativeVertical <= 0.35) {
                    next = Mode.RECONTACT;
                }
            }
            case RECONTACT -> {
                if (input.contactFraction() <= 0.03 && relativeVertical > 0.0) {
                    next = Mode.AIRBORNE;
                } else if (input.contactFraction() >= FULL_SUPPORT_CONTACT) {
                    next = Mode.DISPLACEMENT;
                }
            }
        }

        int ticks = next == previous.mode() ? previous.ticksInMode() + 1 : 0;
        return new State(next, ticks);
    }

    public enum Mode {
        DISPLACEMENT,
        LAUNCHING,
        AIRBORNE,
        RECONTACT
    }

    public record State(Mode mode, int ticksInMode) {
        public State {
            if (mode == null || ticksInMode < 0) {
                throw new IllegalArgumentException("invalid vessel motion state");
            }
        }

        public static State initial() {
            return new State(Mode.DISPLACEMENT, 0);
        }

        public boolean allowWaterForces() {
            return mode != Mode.AIRBORNE;
        }

        public boolean allowDownwardWaterForce() {
            return mode == Mode.DISPLACEMENT || mode == Mode.RECONTACT;
        }
    }

    public record Input(
        double contactFraction,
        double vesselVerticalVelocity,
        double waterVerticalVelocity,
        double forwardSpeed
    ) {
        public Input {
            if (!Double.isFinite(contactFraction) || contactFraction < 0.0 || contactFraction > 1.0) {
                throw new IllegalArgumentException("contactFraction must be between 0 and 1");
            }
            if (!Double.isFinite(vesselVerticalVelocity)
                || !Double.isFinite(waterVerticalVelocity)
                || !Double.isFinite(forwardSpeed)
                || forwardSpeed < 0.0) {
                throw new IllegalArgumentException("motion inputs must be finite");
            }
        }
    }
}
