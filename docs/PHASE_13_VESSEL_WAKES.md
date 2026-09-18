# Phase 13: Vessel Wakes

Phase 13 adds bounded, client-only visual wakes for vanilla boats and Small Ships.

## Runtime model

Minecraft already synchronizes vessel transforms to clients. Newest Ocean therefore derives wakes locally rather than adding wake packets or server-side wake simulation.

`VesselWakeTracker` listens to Fabric client entity load/unload events for `BoatEntity` instances and updates wake histories once per client tick. Small Ships vessels share this path without a compile-time Small Ships dependency.

## Wake history limits

Each vessel history is intentionally small:

- maximum 12 accepted samples
- minimum accepted sample spacing: 0.75 blocks
- minimum horizontal speed: 0.12 blocks/tick
- sample lifetime: 4.0 seconds
- strength fades smoothly to zero with age
- speed and effective hull beam scale initial wake strength

Stationary and nearly stationary vessels do not create new wake samples.

## Quality budgets

Wake tracking is visual-only and follows the active ocean quality tier:

| Quality | Max vessels | Tracking radius |
| --- | ---: | ---: |
| Potato | 4 | 48 blocks |
| Low | 8 | 64 blocks |
| Medium | 16 | 96 blocks |
| High | 24 | 128 blocks |
| Ultra | 32 | 160 blocks |

Nearest vessels are selected first. Equal-distance selection is deterministic by entity ID.

## Geometry

Each pair of accepted history points creates three inexpensive strips:

- left V-shaped stern arm
- right V-shaped stern arm
- centered turbulence strip

Wake width scales from vessel beam and expands gradually behind the vessel. Geometry is generated as pure world-space X/Z data and converted to camera-relative coordinates only during drawing.

## Following the waves

Wake foam does not stay at a fixed sea-level Y coordinate.

The preferred path uses the dedicated `newestocean:vessel_wake` core shader. The CPU submits sparse camera-relative wake strips plus strength/edge data, and the wake vertex shader applies the same packed Gerstner wave components used by the ocean renderer. This moves wake conformance to the GPU and avoids repeated CPU trigonometric sampling on the normal path.

If the wake shader is unavailable, a CPU fallback samples the synchronized procedural ocean and applies height plus horizontal Gerstner displacement before drawing. The fallback preserves visible wakes instead of making them disappear when the custom shader cannot load.

## Rendering

Wake rendering runs immediately after the ocean surface in the existing `AFTER_TRANSLUCENT` world-render path.

The pass uses:

- sparse `POSITION_COLOR` wake quads
- a dedicated GPU wake shader when available
- CPU procedural-ocean fallback
- pale foam color with edge fading
- alpha driven by wake strength and age
- depth testing enabled
- culling disabled while drawing two-sided wake strips
- blending enabled
- depth writes disabled during the wake pass
- render state restored afterward

No wake textures or particles are required.

## Networking and physics

Phase 13 adds:

- no network packets
- no server wake state
- no propulsion changes
- no drag changes
- no collision changes
- no vessel-force changes
- no persistent world foam simulation

Wake quality can change independently on every client without changing physical vessel behavior.

## Tests

Phase 13 tests cover:

- stationary wake suppression
- minimum sample spacing
- 12-sample history cap
- four-second expiry
- speed and beam strength response
- age fading
- exact quality budgets
- finite and symmetric V-wake geometry
- deterministic geometry
- radius/count-bounded nearest-vessel selection
- deterministic selection ties
- renderer use of tracker snapshots, wake geometry, procedural-ocean fallback, camera-relative coordinates, and safe depth writes

## Remaining validation

CI verifies Java compilation, unit/contract tests, shader resources in the JAR, JAR generation, and artifact upload. CI does not create a real Minecraft OpenGL context, so a later in-game graphics/stress pass should visually tune foam width, opacity, Small Ships hull scaling, GPU shader appearance, and many-vessel scenes. Spray particles remain Phase 15 and shoreline breaking remains Phase 14.
