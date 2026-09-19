# Phase 13 Vessel Wakes Design

## Goal

Add efficient visual vessel wakes for vanilla boats and Small Ships without adding wake networking, particle-heavy simulation, or any influence on authoritative vessel physics.

## Chosen architecture

Phase 13 is client-only and transform-driven. Minecraft already synchronizes vessel positions and velocities, so the client derives visual wake descriptors from nearby `BoatEntity` instances instead of receiving new server packets.

Small Ships compatibility remains optional and dependency-free. Small Ships vessels inherit Minecraft boat behavior, so the wake tracker can treat compatible `BoatEntity` instances uniformly and may use registry namespace only when a visual size/profile hint is needed.

## Components

### `VesselWakeDescriptor`

A pure data record for one sampled wake point:

- world X/Z position
- normalized horizontal travel direction
- vessel speed
- effective beam/width
- sample age in seconds
- normalized strength

Descriptors are visual data only.

### `VesselWakeHistory`

Maintains a small rolling history for one vessel.

Rules:

- maximum 12 samples per vessel
- add a sample only after the stern has moved at least 0.75 blocks from the previous accepted sample
- do not add a wake sample below 0.12 blocks/tick horizontal speed
- expire samples after 4.0 seconds
- newest samples are strongest and older samples fade smoothly to zero
- speed and beam determine wake width and initial foam strength
- history is cleared when the vessel disappears or becomes invalid

The history stores only enough information to reconstruct a wake trail. It does not store particles or ocean vertices.

### `VesselWakeTracker`

Runs on the client and discovers nearby boat-like entities already present in the client world.

Rules:

- track only vessels inside the current wake tracking radius
- cap tracked vessels by quality tier
- prefer nearer vessels when the cap is exceeded
- ignore stationary or nearly stationary vessels
- update histories from current synchronized entity transforms
- prune histories for entities no longer present

Quality caps:

- POTATO: 4 vessels, 48-block tracking radius
- LOW: 8 vessels, 64-block tracking radius
- MEDIUM: 16 vessels, 96-block tracking radius
- HIGH: 24 vessels, 128-block tracking radius
- ULTRA: 32 vessels, 160-block tracking radius

These limits are visual-only and may change later through tuning without changing physics.

### Wake geometry

Each history segment produces a cheap V-shaped stern wake plus a narrow turbulent center trail.

For a descriptor with travel direction `d`, compute a perpendicular horizontal vector `p`.

The wake uses:

- a left arm offset along `-p`
- a right arm offset along `+p`
- a center turbulence strip behind the stern
- width increasing gradually with sample age/distance behind the vessel
- alpha and foam strength decreasing with sample age

Wake geometry is camera-relative at submission time.

No tessellated fluid simulation is used. The geometry is a small number of quads per history segment.

## Strength model

Wake strength is bounded to `0..1` and depends on horizontal vessel speed and effective beam.

Conceptually:

`strength = speedResponse * beamResponse * ageFade`

Where:

- `speedResponse` is zero below the wake threshold and rises smoothly with speed
- `beamResponse` gives larger hulls a wider/stronger wake without allowing huge ships to dominate the screen
- `ageFade` decreases smoothly from 1 at creation to 0 at 4 seconds

The exact implementation will use clamped smooth interpolation and will be covered by pure Java tests.

## Effective hull width

Vanilla boats use their client bounding-box width with a conservative minimum.

Small Ships and other larger `BoatEntity` implementations use their actual client bounding-box width, so wake scale follows real vessel dimensions without direct compile-time integration.

No server-side hull profile data is required for Phase 13.

## Rendering

Phase 13 adds a separate wake render pass after the ocean surface.

The wake pass:

- uses cached client wake histories
- emits only nearby wake quads
- depth-tests against terrain and vessels
- uses blending with depth writes disabled
- samples the same synchronized ocean surface height at descriptor positions so wake foam follows waves instead of remaining at a fixed Y level
- uses procedural color/alpha only, with no texture requirement

The first Phase 13 renderer will be CPU-generated geometry because descriptor counts are tightly capped and each vessel contributes only a few quads. This avoids expanding the ocean shader interface with large per-vessel uniform arrays. A later renderer optimization may batch or move wake shaping to the GPU if profiling shows value.

## Data flow

```text
Minecraft synchronized vessel transforms
        ↓
client VesselWakeTracker
        ↓
small rolling history per nearby vessel
        ↓
VesselWakeDescriptor snapshots
        ↓
wave-height query at wake positions
        ↓
camera-relative wake quads
        ↓
translucent wake render pass
```

There are no new packets in this flow.

## Interaction with adaptive quality

Changing visual quality immediately changes:

- tracking radius
- maximum tracked vessel count

Existing histories may remain until normal expiry, but only the nearest descriptors within the current quality budget are rendered.

Adaptive quality never changes vessel motion, ocean physics, wake-producing speed thresholds, or server behavior.

## Lifecycle and failure behavior

- Disconnect/reset clears all wake histories.
- World/dimension changes clear stale histories through client reset or entity pruning.
- Missing/invalid direction data produces no new sample rather than invalid geometry.
- Non-finite inputs are rejected by pure wake-model classes.
- If ocean rendering is unavailable, wake rendering is skipped rather than rendered at an incorrect height.

## Testing

Pure Java tests will cover:

- no wake below movement threshold
- wake strength increases with speed
- larger beam produces wider wake
- accepted samples require minimum movement distance
- histories never exceed 12 samples
- samples expire after 4 seconds
- age fade reaches zero at expiry
- geometry remains finite and symmetric around travel direction
- quality vessel caps and tracking radii
- deterministic geometry for identical descriptors

Renderer/resource integration tests will verify that Phase 13 is registered and reset with the client renderer lifecycle.

## Non-goals

Phase 13 does not add:

- wake networking
- gameplay drag or propulsion changes
- collision effects
- damage
- spray particles
- shoreline breaking
- persistent world foam simulation
- full fluid dynamics

Spray and large impact particles remain Phase 15. Shoreline behavior remains Phase 14.

## Acceptance criteria

Phase 13 is complete when:

1. Nearby moving vanilla boats and Small Ships produce capped, fading wake histories client-side.
2. Stationary vessels do not produce visible wakes.
3. Wake size responds to vessel speed and hull width.
4. Wake trails follow synchronized ocean height and fade after four seconds.
5. Tracking/render cost is bounded by quality-tier radius, vessel count, and history length.
6. No wake packets or server-side wake simulation are introduced.
7. Full test suite, JAR build, and artifact upload pass on the exact current branch head.
