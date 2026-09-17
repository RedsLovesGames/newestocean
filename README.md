# Newest Ocean

Performance-first ocean physics for Fabric 1.21.1.

Newest Ocean is being built as a clean-room implementation of an efficient dynamic ocean for Minecraft. The goal is to deliver convincing moving seas and real vessel response without simulating every water block.

## Design goals

- Deterministic procedural waves shared by client rendering and server physics.
- Cheap sampled buoyancy instead of full fluid simulation.
- Server-authoritative vessel movement with minimal network traffic.
- Independent visual and physics quality scaling for low-end hardware.
- Vanilla boat support first, followed by Small Ships and Shippy Ships compatibility.
- Optional compatibility adapters so ship mods are not hard dependencies.

## Current implementation

The `feature/ocean-core` branch currently contains:

- A six-band deterministic Gerstner-style wave field.
- Surface queries for wave height, normal, velocity, and horizontal displacement.
- Weather, tide, and current inputs.
- Multi-point vessel buoyancy with pitch/roll torque, damping, current drag, and force limits.
- A generic vessel-adapter registry for vanilla and modded ships.
- Unit tests for deterministic sampling and core buoyancy behavior.
- Java 21 / Fabric 1.21.1 CI.

The renderer and Minecraft entity hooks are intentionally not part of the first layer. The physics math is being kept testable and independent so visual quality can be reduced on weak computers without changing gameplay behavior.

## Planned architecture

```text
Ocean state
  -> deterministic wave field
      -> server vessel physics
      -> client ocean renderer

Vessel adapter registry
  -> vanilla boat adapter
  -> Small Ships adapter
  -> Shippy Ships adapter
  -> future ship adapters
```

A vessel only samples a small set of points under its hull. Distant water does not run vessel physics at all. Future rendering work will use a camera-centered LOD mesh so ocean geometry becomes cheaper with distance.
