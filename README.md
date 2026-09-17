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

## Current implementation

The `feature/ocean-core` branch currently contains:

- The Phase 1 deterministic ocean core.
- Multi-point vessel buoyancy with pitch/roll torque, damping, current drag, and force limits.
- Adaptive hull sizing for larger vessels.
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
  -> vanilla boats
  -> Small Ships
  -> launch / glide / re-entry
  -> surfing / planing

Renderer
  -> camera-centered LOD mesh
  -> GPU displacement
  -> foam / wakes / shoreline effects
```

Physics and graphics remain separate. Reducing ocean graphics quality must never change authoritative vessel motion.
