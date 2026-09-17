# Newest Ocean

Performance-first ocean physics for Fabric 1.21.1.

Newest Ocean is a clean-room implementation of an efficient dynamic ocean for Minecraft. The goal is convincing moving seas and physically responsive vessels without simulating every water block.

## Design goals

- Deterministic procedural waves shared by client rendering and server physics.
- Cheap sampled buoyancy instead of full fluid simulation.
- Server-authoritative vessel movement with minimal network traffic.
- Independent visual and physics quality scaling for low-end hardware.
- Vanilla boats and Small Ships as the active vessel targets.
- Optional compatibility so Small Ships is not a hard dependency.
- Ships may launch, glide, surf, plane, and re-enter instead of being glued to the wave surface.

## Phase 1: deterministic ocean core

Phase 1 provides the common ocean state used by every later physics and rendering system:

- Six-band deterministic Gerstner-style wave field.
- Surface height, normal, velocity, and horizontal displacement queries.
- World-derived ocean seed using a stable salted mixing function.
- Join-time server-to-client ocean seed synchronization.
- Server initialization of the same authoritative ocean field used for vessel physics.
- Weather-driven wave scaling.
- Deterministic slow tides.
- Deterministic horizontal currents with weather amplification.
- Shared Minecraft world time as the animation/physics clock.
- Unit tests covering deterministic sampling, tide bounds, current bounds, and seed stability.

The client reconstructs the ocean locally from the synchronized seed and normal Minecraft time/weather state. Water vertices are not networked.

## Phase 2: vessel physics foundation

Phase 2 provides the common vessel model used by vanilla boats and Small Ships:

- Adaptive hull profiles scale buoyancy sample count, mass, draft, drag, damping, and acceleration limits with vessel size.
- Vanilla boats and Small Ships use the same profile-driven correction solver.
- Multi-point buoyancy produces force and pitch/roll torque from the shared ocean surface.
- Differential wave correction preserves Minecraft or ship-mod flat-water buoyancy instead of stacking a second complete buoyancy model on top.
- Contact state reports wet-point fraction, weighted submersion, local support center, bow/stern loading, and port/starboard loading.
- Wave-aligned pitch and roll targets are generated from the same ocean field for later renderer and motion-state integration.
- Asymmetric water-contact tests verify side loading and roll torque.

## Phase 3: launch, airborne glide, and re-contact

Phase 3 prevents vessels from being glued to the wave surface after a crest:

- A pure motion-state controller tracks displacement, launching, airborne, and re-contact modes.
- Launch requires real partial hull contact, forward speed, and upward vessel motion relative to the moving water surface.
- A fully dry vessel cannot accidentally enter wave-launch mode.
- Launching vessels may still receive upward and horizontal wave correction, but Newest Ocean cannot pull them downward toward the water.
- Once airborne, Newest Ocean applies zero water force, allowing existing momentum and normal Minecraft gravity to control the glide.
- Motion state continues updating even after the entity stops touching water.
- Descending vessels transition to re-contact only after the hull actually regains water support.
- Vanilla boats and Small Ships share the same motion-state runtime.

## Phase 4: progressive re-entry and anti-flip stability

Phase 4 makes water re-entry heavy and stable without hard-locking vessel orientation:

- Re-contact uses a smooth wetting curve, so first hull contact receives only a fraction of the full water correction instead of an instant force spike.
- Hard vertical slam correction is capped by vessel mass to prevent single-tick bounce launches.
- Impact severity is measured from vessel velocity relative to the moving water surface for later splash, sound, and damage effects.
- Raw wave torque is blended with restoring pitch and roll torque rather than replacing physical wave response.
- Angular damping suppresses runaway rotational energy while still allowing real capsizing forces to win.
- Rotational inertia scales from hull mass, beam, and length, so large ships rotate much more slowly than small boats.
- A hull-scaled torque ceiling blocks pathological one-tick cartwheel impulses without imposing a fixed maximum roll angle.
- The common solver exposes stabilized torque and angular acceleration for vessel adapters and render integration that support true pitch and roll.
- The shared vanilla/Small Ships runtime applies progressive re-entry force shaping immediately.
- Unit tests cover first-contact wetting, hard-impact force caps, restoring torque direction, hull-size angular inertia, torque spike limits, and normal displacement behavior.

Newest Ocean intentionally does not use a rule such as `roll > 30 degrees -> force roll back to 30 degrees`. Stability comes from forces, inertia, damping, and capped impulses.

## Current implementation

The `feature/ocean-core` branch currently contains:

- Phase 1 deterministic ocean core.
- Phase 2 shared vessel physics foundation.
- Phase 3 launch/airborne/re-contact state system.
- Phase 4 progressive re-entry and passive angular stability system.
- Vanilla boat wave-force correction.
- Optional Small Ships tracking and size-scaled physics integration.
- Vessel pose sampling from the same ocean surface.
- Visual quality tiers and adaptive-quality controller foundations.
- Camera-centered ocean mesh planning.
- CPU-side displaced ocean mesh generation and normals.
- Cached water-coverage masks for future shoreline-safe rendering.
- Java 21 / Fabric 1.21.1 CI.

## Planned architecture

```text
Deterministic ocean state
  -> waves
  -> weather
  -> tides
  -> currents
      -> server vessel physics
      -> client ocean renderer

Vessel physics
  -> shared hull profiles
  -> vanilla boats
  -> Small Ships
  -> detailed water contact
  -> launch / airborne glide / re-contact
  -> progressive re-entry / angular stability
  -> surfing / planing

Renderer
  -> camera-centered LOD mesh
  -> GPU displacement
  -> foam / wakes / shoreline effects
```

Physics and graphics remain separate. Reducing ocean graphics quality must never change authoritative vessel motion.
