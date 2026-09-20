# GPT-6 Astra Handoff: Finish Newest Ocean

This file is the authoritative continuation brief. Do not start by rereading the older phase plans. Use them only if a referenced implementation detail is unclear.

## Goal

Finish the Fabric 1.21.1 Newest Ocean rewrite so Minecraft/Sodium/Iris water itself is displaced in X/Y/Z by the deterministic ocean, with convincing material detail and efficient vessel interaction. The finished mod must not render a second full ocean surface. Preserve efficient server-authoritative physics, 15-block shore damping, shaderpack compatibility, quality scaling, Vanilla boats, and Small Ships. Shippy Ships is explicitly out of scope and must not be added.

## Current state

The repository already contains the hard parts:

- deterministic 24-component `OceanSpectrum`
- 6 dominant authoritative physical components
- 3D Gerstner displacement, analytical normals and surface velocity
- shared 15-block shore attenuation
- vessel-local server shore cache
- buoyancy, pitch/roll, wave riding, planing/gliding, launch/re-entry foundations
- 24-wave renderer-neutral client payload/runtime
- cached client shore-distance texture
- shared GLSL water evaluation
- Vanilla real-water shader injection
- Sodium 0.8.12 real-water injection
- Iris 1.8.14-beta.1 water and shadow injection
- Iris installed/no-pack Sodium fallback
- required-vs-optional uniform liveness policy
- quality presets: POTATO 4, LOW 6, MEDIUM 10, HIGH 14, ULTRA 24
- client visual override 0=Auto, otherwise 1..24
- DEPTHS visual cap in the real Iris path, default 18
- existing Vanilla boat and Small Ships integration
- old config fields retained only for migration while the old overlay still exists

## Non-negotiable semantics

Preserve these exactly unless a compile/runtime bug forces a minimal correction:

1. `OceanSpectrum.MAX_COMPONENTS = 24`.
2. `OceanSpectrum.PHYSICS_COMPONENTS = 6`; client settings never change authoritative physics.
3. Full wave strength is reached at 15 blocks from land using smooth attenuation inside that band.
4. Do not force-load chunks for shore calculations.
5. Large-wave geometry displacement is X/Y/Z, not height-only.
6. Unsupported shader injection must leave ordinary Minecraft/Sodium/Iris water visible. Never fall back to the old duplicate ocean plane.
7. Iris/Sodium remain optional and version-gated. Current pinned targets are Sodium 0.8.12 and Iris 1.8.14-beta.1 for Minecraft 1.21.1.
8. Preserve synchronized seed/time/weather behavior.
9. Preserve Vanilla boat and Small Ships physics ownership. Do not double-apply forces.
10. LOW remains the low-end reference preset. Do not reduce physical/server fidelity to improve client FPS.
11. Do not add Shippy Ships support, dependencies, adapters, reflection hooks, tests, or documentation.

## Clean-room optimization direction

A local Physics Mod Pro JAR was inspected only to understand architectural ideas. Do not copy, translate, or reproduce its proprietary source. Reimplement only general techniques independently.

### 1. Split geometry waves from material-detail waves

Do not require all 24 visual components to perform full geometric work on every water vertex.

Use the same deterministic 24-wave spectrum, but split its rendering responsibility:

- the 6 authoritative physical waves remain unchanged for server physics
- a quality-dependent dominant subset drives real X/Y/Z water geometry
- remaining/smaller components contribute to material normals, highlights, foam, and optional UV/normal micro-detail rather than unnecessary geometric displacement

The visual result should still represent the 24-wave spectrum on ULTRA. This is a workload split, not a replacement spectrum.

Suggested starting geometry budgets, subject to profiling and visual sanity:

- POTATO: 4 geometry waves
- LOW: 6 geometry waves
- MEDIUM: 8 geometry waves
- HIGH: 10 geometry waves
- ULTRA: 12 geometry waves

Remaining active visual components should feed normal/material detail. Keep the existing user-facing visual budgets 4/6/10/14/24 unless there is a compelling compatibility reason to change them.

### 2. Actually use calculated normals and foam in the final water material

The shared GLSL already calculates `NewestOceanWaterSample.normal` and `.foam`; the current generic source patch primarily consumes displacement.

Finish the renderer so, where the renderer/shaderpack permits it:

- large waves move geometry
- smaller waves influence the effective water normal/material response
- crest/steepness data drives whitecaps/foam
- Vanilla water keeps its recognizable texture rather than being replaced wholesale
- Iris shaderpacks retain their own water color/reflection/refraction logic wherever possible

Do not blindly overwrite shaderpack water materials. Inject the minimum data necessary to make their existing water respond to our displacement/normal information.

### 3. Replace the shore grid hot path with a linear-time distance transform

`ShoreDistanceGrid` currently propagates a precomputed radius of offsets from each land source. Replace that client-side field construction with a standard clean-room O(width*height) Euclidean distance-transform implementation, clamped to the existing 15-block requirement.

Preserve:

- exact or sufficiently precise Euclidean distance within the 15-block transition band
- full strength at 15 blocks
- UNKNOWN/unloaded terrain treated conservatively
- no chunk force-loading

If practical, snapshot the required WATER/LAND occupancy on the Minecraft thread and perform only the pure distance-transform computation off-thread. Never access mutable Minecraft world/chunk state from the worker.

### 4. Prefer a low-resolution disturbance texture for wakes/ripples

Do not keep expensive wake geometry merely because it already exists.

Where compatible with the real-water material path, prefer a camera-centered low-resolution disturbance field/texture for:

- vessel wakes
- small ripple rings
- other sub-block disturbances

Use neighboring texel differences to perturb the effective water normal and optionally foam. Keep any wake geometry only if it is materially cheaper or required for a visible feature the texture approach cannot represent.

Do not recreate a second ocean surface.

### 5. Add vessel solver-complexity LOD

Keep full server-authoritative vessel physics near relevant players, but reduce solve complexity for distant/unoccupied vessels.

Use the existing update-rate LOD as the base, then introduce a cheaper sampling mode such as center/front/back/left/right for sufficiently distant vessels. Farther vessels may use an even simpler center-based approximation if needed.

Requirements:

- occupied/player-controlled vessels get full fidelity
- near vessels preserve full hull behavior
- distant simplification must not change the shared ocean definition
- transitions should not create visible velocity/pose jumps
- Small Ships must use the same common ocean physics path rather than a second force model

## Known unfinished architecture

### A. Remove the duplicate primary ocean renderer

`NewestOceanClient` still registers `OceanWorldRenderer.register()`. `OceanWorldRenderer` still owns the old `AFTER_TRANSLUCENT` custom ocean surface and references `OceanGpuShader`, old LOD mesh/topology/coverage, old shoreline overlay behavior, and old opacity/render-distance concepts.

Cut over so the real-water injection paths are the only primary ocean surface. There must be no automatic path that draws a second full ocean sheet.

Delete old surface-only code/resources only after confirming there are no remaining production references. Keep shared math or wake data if the new path still uses them.

### B. Replace legacy compatibility diagnostics

`ShaderCompatibility` still models active Iris packs as old overlay/CPU fallback modes and contains `skipWorldRender` shadow behavior. That model is obsolete for real-water injection.

Make diagnostics describe actual injection state:

- active renderer path: VANILLA / SODIUM / IRIS / unsupported
- source patch success
- uniform/sampler binding success
- shadow injection success when relevant
- active visual wave budget
- active geometry-wave budget if split from visual count
- physical wave count = 6
- shore-field state
- explicit fallback reason

Do not globally suppress the new real-water path during Iris shadow rendering.

### C. Whitecaps, shoreline visuals and wakes

The final primary surface must remain real Minecraft/Sodium/Iris water.

- whitecaps/foam should use data from the real-water shader/material path
- calculated wave normals should actually affect material response where compatible
- shoreline geometry attenuation already comes from the shore field
- any retained shoreline foam must be narrow/material-integrated, never another full ocean plane
- prefer disturbance-texture wakes over broad geometry where practical

### D. Small Ships only

Small Ships is the only external vessel mod that needs explicit support.

Verify the current integration assumption: `SmallShipsIntegration` tracks entities that are both `BoatEntity` instances and use the `smallships` namespace. Keep that path if it matches the actual installed 1.21.1 Small Ships implementation. If it does not, adapt only the Small Ships bridge while preserving common `BoatPhysicsSupport` ownership and preventing duplicate force application.

Do not add support for Shippy Ships or any other ship mod in this phase.

## Files to inspect first

Read these before broad repository exploration:

- `src/main/java/com/redslovesgames/newestocean/client/NewestOceanClient.java`
- `src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java`
- `src/main/java/com/redslovesgames/newestocean/client/ShaderCompatibility.java`
- `src/main/java/com/redslovesgames/newestocean/client/OceanDiagnosticsHud.java`
- `src/main/java/com/redslovesgames/newestocean/client/water/`
- `src/main/java/com/redslovesgames/newestocean/client/compat/sodium/`
- `src/main/java/com/redslovesgames/newestocean/client/compat/iris/`
- `src/main/java/com/redslovesgames/newestocean/client/shore/ShoreDistanceFieldCache.java`
- `src/main/java/com/redslovesgames/newestocean/ocean/ShoreDistanceGrid.java`
- `src/main/java/com/redslovesgames/newestocean/minecraft/BoatPhysicsSupport.java`
- `src/main/java/com/redslovesgames/newestocean/minecraft/SmallShipsIntegration.java`
- `src/main/resources/newestocean.mixins.json`
- `src/main/resources/assets/newestocean/shaders/include/newestocean_water.glsl`

## Execution order

1. Keep the corrected quality-preset expectations green.
2. Replace legacy compatibility/diagnostic assumptions with real injection state.
3. Cut over `NewestOceanClient` so `OceanWorldRenderer` is no longer the primary ocean surface.
4. Split visual work between dominant geometry waves and material-detail waves without changing the 24/6 model.
5. Wire calculated wave normals and foam into the real water material paths where renderer-compatible.
6. Replace the client shore-distance hot path with a bounded linear-time distance transform; off-thread only the pure computation if safe.
7. Prefer a low-resolution disturbance texture for wakes/ripples where it is cheaper than geometry.
8. Add vessel solver-complexity LOD while keeping full fidelity near/for players.
9. Verify and preserve Small Ships integration only.
10. Remove dead overlay-only classes/resources after reference search.
11. Build the normal remapped mod JAR.
12. STOP and request human visual/gameplay testing.

## Testing policy for Astra

Do not spend time building a large test matrix, writing QA documents, benchmarking, taking screenshots, or repeatedly running the entire suite.

Use only fast checks while editing:

- compile after structural changes
- run the smallest existing unit/contract test directly related to changed code
- add a tiny test only when it protects a non-obvious invariant
- use simple targeted performance counters/profiling only when choosing between two hot-path implementations
- one final compile/build sufficient to produce the JAR before the human-testing stop

Do not attempt manual Minecraft visual validation yourself unless an interactive game environment is already available and costs essentially nothing.

## Human testing stop gate

Stop coding and ask the human to test when all of the following are true:

- project compiles and a remapped JAR is produced
- `OceanWorldRenderer.register()` is no longer the primary ocean path
- no automatic duplicate full-ocean fallback remains
- Vanilla/Sodium/Iris injection paths remain registered and version-gated
- Iris shadow path is not globally skipped by legacy overlay logic
- diagnostics report real injection state
- 24 visual / 6 physical wave architecture is intact
- dominant geometry-wave/material-detail split is active
- calculated water normals and foam are actually consumed where compatible
- 15-block shore damping is intact
- Vanilla and Small Ships integrations remain intact
- no Shippy Ships dependency/integration has been added

At that point provide the JAR path and ask the human to test only these scenarios first:

1. Fabric only: ocean moves in X/Y/Z, looks smoother/more detailed than raw block-vertex displacement, and has no duplicate surface.
2. Sodium 0.8.12: same.
3. Iris 1.8.14-beta.1 with a generic shaderpack: displaced water/shadows where applicable, normal material behavior, and no non-water transparency corruption.
4. DEPTHS: no duplicate/cyan sheet; geometry still moves; shaderpack water appearance remains intact.
5. Vanilla boat and Small Ships vessel: buoyancy, pitch/roll, wave riding, crest launch, airborne behavior, re-entry, and shoreline damping feel plausible.
6. Check LOW for obvious frame-time regressions before any further visual tuning.

Do not continue tuning based on guessed visuals. Wait for human observations/screenshots after this gate.

## What not to do

- Do not restart the ocean math rewrite.
- Do not replace the deterministic 24-wave spectrum with Physics Mod Pro's wave function.
- Do not make client quality affect server physics.
- Do not copy, translate, or reproduce Physics Mod Pro source code.
- Do not add Shippy Ships support.
- Do not restore the old overlay as a fallback.
- Do not spend tokens rewriting historical documentation.
- Do not stop for routine confirmations before the human-testing gate.
