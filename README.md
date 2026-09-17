# Newest Ocean

Performance-first ocean physics for Fabric 1.21.1.

Goals:

- Deterministic procedural waves shared by client rendering and server physics.
- Cheap sampled buoyancy instead of full fluid simulation.
- Vanilla boat support first, then Small Ships and Shippy Ships compatibility adapters.
- Independent visual and physics quality scaling for low-end hardware.
- Server-authoritative multiplayer behavior.

Development will proceed in small testable layers: core wave math, buoyancy sampling, vessel abstraction, Minecraft integration, compatibility adapters, then rendering and adaptive quality.
