# Phase 11: Vessel Physics LOD

Phase 11 reduces the cost of authoritative boat and Small Ships ocean physics without changing the physical ocean or networking model.

## Update bands

Stable displacement vessels select an expensive ocean-solve interval from the nearest non-spectator player:

| Nearest player distance | Ocean solve rate | Interval |
| --- | ---: | ---: |
| 0-48 blocks | 20 Hz | every tick |
| >48-96 blocks | 10 Hz | every 2 ticks |
| >96-192 blocks | 5 Hz | every 4 ticks |
| >192 blocks / no nearby player | 2 Hz | every 10 ticks |

The cheap vessel bookkeeping pass still runs every server tick. Minecraft's normal entity movement also continues every tick.

## Full-rate safety overrides

The distance schedule is bypassed and the vessel stays at 20 Hz when:

- any player is riding the vessel;
- the motion state is `LAUNCHING`;
- the motion state is `AIRBORNE`;
- the motion state is `RECONTACT`.

This keeps player controls, crest launches, airborne transitions, and hard landings on the full authoritative physics rate.

## Interpolated correction

Reduced-rate vessels do not receive one large impulse every solve tick. The latest water-force target is cached and linearly blended from the currently applied correction to the new target over the selected solve interval. That correction is applied every server tick while the expensive ocean sampling is skipped between solve ticks.

At 20 Hz the interpolation interval is one tick, so the result applies immediately and preserves the pre-LOD behavior.

## Load staggering

Sparse solve ticks are phase-offset by entity ID. Distant vessels therefore do not all perform their 2 Hz or 5 Hz ocean solves on the same server tick, reducing periodic server-time spikes when many ships are loaded.

## Shared integration

`BoatPhysicsSupport` owns the schedule, so both vanilla boats and optional Small Ships use the same LOD rules. The optimization does not require a Small Ships compile dependency.

## Invariants

Phase 11 does not alter:

- the deterministic ocean seed;
- physical wave count or equations;
- tides or currents;
- vessel mass, hull sampling, buoyancy, launch, re-entry, surfing, or planing equations;
- client visual quality;
- multiplayer wave networking.

Only the frequency of expensive stable-displacement ocean solves changes with distance.

## Verification

Unit tests cover:

- all four distance bands;
- player-controlled full-rate override;
- launch, airborne, and re-contact full-rate overrides;
- deterministic entity-ID solve staggering;
- smooth force interpolation;
- immediate full-rate force application.
