# GPT-6 Astra Handoff: Finish Newest Ocean

This file is the authoritative continuation brief. Do not start by rereading the older phase plans. Use them only if a referenced implementation detail is unclear.

## Goal

Finish the Fabric 1.21.1 Newest Ocean rewrite so Minecraft/Sodium/Iris water itself is displaced in X/Y/Z by the deterministic ocean. The finished mod must not render a second full ocean surface. Preserve efficient server-authoritative vessel physics, 15-block shore damping, shaderpack compatibility, quality scaling, Small Ships support, and add Shippy Ships support if its 1.21.1 API/runtime can be safely integrated.

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
- old config fields retained only for migration while the old overlay still exists

## Non-negotiable semantics

Preserve these exactly unless a compile/runtime bug forces a minimal correction:

1. `OceanSpectrum.MAX_COMPONENTS = 24`.
2. `OceanSpectrum.PHYSICS_COMPONENTS = 6` and client settings never change authoritative physics.
3. Full wave strength is reached at 15 blocks from land using smooth attenuation inside that band.
4. Do not force-load chunks for shore calculations.
5. Water displacement is X/Y/Z, not height-only.
6. Unsupported shader injection must leave ordinary Minecraft/Sodium/Iris water visible. Never fall back to the old duplicate ocean plane.
7. Iris/Sodium remain optional and version-gated. Current pinned targets are Sodium 0.8.12 and Iris 1.8.14-beta.1 for Minecraft 1.21.1.
8. Preserve synchronized seed/time/weather behavior.
9. Preserve existing Vanilla boat and Small Ships physics ownership. Do not double-apply forces.
10. LOW remains the low-end reference preset. Do not reduce physical fidelity to improve client FPS.

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
- visible wave count
- physical wave count = 6
- shore-field state
- explicit fallback reason

Do not globally suppress the new real-water path during Iris shadow rendering.

### C. Whitecaps, shoreline visuals and wakes

The final primary surface must remain real Minecraft/Sodium/Iris water.

- whitecaps/foam should use data from the real-water shader/material path
- shoreline geometry attenuation already comes from the shore field
- any retained shoreline foam must be narrow/material-integrated, never another full ocean plane
- wake simulation may remain; a narrow wake-only effect is acceptable, but it must not recreate the old ocean surface

### D. Shippy Ships is not currently implemented

The current tree has Vanilla and Small Ships integration but no Shippy Ships bridge.

Current verified distribution target as of 2026-09-20: Modrinth project `oxBNHOmi`, Fabric 1.21.1 release `1.0.18`, Modrinth version ID `CLqqk2TN`. Recent 1.21.1 releases use Ingenium API, so inspect the actual resolved JAR/API before choosing a hook.

Add a safe optional Shippy Ships integration for Fabric 1.21.1 if its classes/API can be resolved locally. Prefer reflection or isolated optional integration so Newest Ocean still launches without Shippy Ships. Reuse the common vessel physics rather than duplicating ocean forces.

Also verify the current Small Ships assumption before changing it: `SmallShipsIntegration` only tracks entities that are both `BoatEntity` instances and have the `smallships` namespace. Keep it if that matches the installed 1.21.1 implementation; otherwise adapt it without creating duplicate force application.

If the exact Shippy Ships hook cannot be established from available local dependencies/source, do not invent class names. Finish all other work, record the exact missing integration information, and include it in the human handoff.

## Files to inspect first

Read these before broad repository exploration:

- `src/main/java/com/redslovesgames/newestocean/client/NewestOceanClient.java`
- `src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java`
- `src/main/java/com/redslovesgames/newestocean/client/ShaderCompatibility.java`
- `src/main/java/com/redslovesgames/newestocean/client/OceanDiagnosticsHud.java`
- `src/main/java/com/redslovesgames/newestocean/client/water/`
- `src/main/java/com/redslovesgames/newestocean/client/compat/sodium/`
- `src/main/java/com/redslovesgames/newestocean/client/compat/iris/`
- `src/main/java/com/redslovesgames/newestocean/minecraft/BoatPhysicsSupport.java`
- `src/main/java/com/redslovesgames/newestocean/minecraft/SmallShipsIntegration.java`
- `src/main/resources/newestocean.mixins.json`
- `src/main/resources/assets/newestocean/shaders/include/newestocean_water.glsl`

## Execution order

1. Keep the corrected quality-preset unit expectations green.
2. Replace legacy compatibility/diagnostic assumptions with real injection state.
3. Cut over `NewestOceanClient` so `OceanWorldRenderer` is no longer the primary surface.
4. Integrate/retain whitecaps, shoreline visuals and wakes without a second full ocean surface.
5. Remove dead overlay-only classes/resources after reference search.
6. Verify Small Ships integration against the installed 1.21.1 implementation and add optional Shippy Ships integration if the actual hook is resolvable.
7. Build the normal remapped mod JAR.
8. STOP and request human visual/gameplay testing.

## Testing policy for Astra

Do not spend time building a large test matrix, writing QA documents, benchmarking, taking screenshots, or running repeated full suites.

Use only fast checks while editing:

- compile after structural changes
- run the smallest existing unit/contract test directly related to changed code
- add a tiny test only when it protects a non-obvious invariant
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
- 24 visual / 6 physical wave separation is intact
- 15-block shore damping is intact
- Vanilla and Small Ships integrations remain intact
- Shippy Ships integration is implemented, or a precise blocker is documented without guessing

At that point provide the JAR path and ask the human to test only these scenarios first:

1. Fabric only: ocean moves in X/Y/Z, no duplicate surface.
2. Sodium 0.8.12: same.
3. Iris 1.8.14-beta.1 with a generic shaderpack: water and water shadows move, non-water transparency is normal.
4. DEPTHS: no duplicate/cyan sheet and geometry still moves.
5. One Vanilla boat, one Small Ships vessel, and one Shippy Ships vessel if supported: buoyancy/wave riding/launch/re-entry feel plausible.

Do not continue tuning based on guessed visuals. Wait for human observations/screenshots after this gate.

## What not to do

- Do not restart the ocean math rewrite.
- Do not replace the 24-wave spectrum with a new system.
- Do not make client quality affect server physics.
- Do not copy Physics Mod Pro implementation code.
- Do not invent Shippy Ships APIs.
- Do not restore the old overlay as a fallback.
- Do not spend tokens rewriting historical documentation.
- Do not stop for routine confirmations before the human-testing gate.
