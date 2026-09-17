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

The detailed contact state is intentionally exposed before launch/glide logic. Phase 3 can decide whether a vessel is displacement-floating, launching, or airborne without changing the underlying buoyancy solver.

## Current implementation

The `feature/ocean-core` branch currently contains:

- Phase 1 deterministic ocean core.
- Phase 2 shared vessel physics foundation.
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
  -> launch / glide / re-entry
  -> surfing / planing

Renderer
  -> camera-centered LOD mesh
  -> GPU displacement
  -> foam / wakes / shoreline effects
```

Physics and graphics remain separate. Reducing ocean graphics quality must never change authoritative vessel motion.
