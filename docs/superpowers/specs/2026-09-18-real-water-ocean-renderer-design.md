# Newest Ocean Real-Water Renderer and 3D Vessel Physics Design

Date: 2026-09-18
Branch: `feature/ocean-core`
Status: Approved architecture, awaiting written-spec review before implementation planning

## 1. Goal

Replace Newest Ocean's current duplicate translucent ocean sheet with a Physics Mod Pro-like moving ocean that deforms Minecraft's actual water surface while remaining substantially lighter and keeping shader compatibility mandatory.

The target experience is:

- actual water geometry moves in X, Y, and Z
- the visible ocean and vessel physics share the same deterministic dominant wave field
- vessels pitch, roll, surf, plane, glide across multiple crests, launch, and re-enter
- rough seas still matter and can slow, shove, or destabilize a vessel
- full-strength ocean motion applies only to water farther than 15 blocks from shore
- the final 15 blocks toward shore use a smooth attenuation rather than a hard cutoff
- Vanilla/Fabric, Sodium, Iris shaderpacks, and DEPTHS must retain animated ocean geometry
- LOW is designed around GTX 1050 Ti / GTX 1650-class dedicated GPUs

This is a clean-room implementation. Physics Mod's public documentation may be used to understand integration behavior, but Newest Ocean will not copy proprietary ocean-engine implementation code.

## 2. Why the Current Renderer Must Be Replaced

The current renderer adds a second custom ocean surface during a post-world translucent render event after Minecraft has already rendered vanilla water. This creates two nearly coincident water surfaces with different displacement, depth, and shader behavior.

That architecture causes the observed failures:

- severe overlap and intersection artifacts
- z-fighting-like dark sheets and streaks
- visible finite-mesh boundaries
- shaderpack compositing mismatches
- water that looks like an overlay rather than a moving body of water

The old camera-centered ocean mesh must not remain as the normal renderer or as an automatic shader fallback.

## 3. Chosen Architecture

Use a hybrid deterministic spectral/Gerstner ocean injected into the real Minecraft water rendering path.

### 3.1 Shared deterministic spectrum

A synchronized ocean seed generates an ordered wave spectrum shared by client and server. Components are sorted strongest and longest first so physics can evaluate only the dominant subset while graphics evaluates more detail.

Conceptual flow:

```text
              Shared deterministic ocean spectrum
                            |
              +-------------+-------------+
              |                           |
          SERVER PHYSICS              CLIENT GPU
              |                           |
      dominant 4-6 waves           quality-selected waves
              |                           |
       X/Y/Z water state            X/Y/Z vertex motion
       normals + velocity            normals + foam data
              |                           |
        vessel buoyancy                real water
        surfing/gliding               rendering path
```

The large-scale waves therefore remain synchronized even when client quality changes.

### 3.2 Real water, not a second ocean sheet

Minecraft or Sodium continues to generate normal water geometry. Newest Ocean modifies the water vertex path itself.

```text
Minecraft/Sodium water geometry
            |
            v
Newest Ocean vertex displacement
            |
            v
Vanilla/Iris water shading
            |
            v
One visible water surface
```

No duplicate primary ocean surface is rendered on top.

## 4. Wave Model

### 4.1 Gerstner-style 3D displacement

Each wave contributes true horizontal and vertical displacement.

For each input water position `(x, y, z)`, the evaluated result includes:

- displaced X position
- displaced Y position
- displaced Z position
- displaced surface normal
- surface/orbital velocity in X, Y, and Z
- local slope
- crest/curvature information
- foam potential

The result must not be a vertical sine-only deformation. Horizontal displacement is required so crests visibly travel and sharpen.

### 4.2 Spectrum generation

Replace the fixed six-band default generator with a deterministic spectrum pool of approximately 18-24 ordered components.

The spectrum is derived only from synchronized state such as:

- ocean seed
- world time
- weather-derived conditions
- deterministic salts/constants

No random client-only wave parameters may affect the dominant physical surface.

The spectrum should preserve a dominant swell direction while introducing directional spread across shorter wavelengths to avoid obviously repetitive parallel ridges.

### 4.3 Quality levels

Initial target wave counts:

- POTATO: 4-6 visual components
- LOW: 6-8 visual components
- MEDIUM: approximately 10
- HIGH: approximately 14
- ULTRA: approximately 18-24

The exact counts may be tuned during profiling, but LOW must remain the performance reference for GTX 1050 Ti / GTX 1650-class GPUs.

Higher quality should primarily spend additional shader arithmetic and normal/foam detail. It must not dramatically increase water geometry count.

### 4.4 Physics subset

Authoritative vessel physics evaluates approximately the strongest 4-6 components regardless of client graphics quality.

This subset defines:

- large-scale surface height
- surface normal
- horizontal orbital motion
- vertical orbital motion
- wave-face movement used by surfing and gliding

Extra visual components add small-scale appearance but do not change authoritative vessel motion.

### 4.5 Weather behavior

Weather modifies the same deterministic spectrum rather than replacing it with a separate visual effect.

Rain and thunder may affect:

- total wave amplitude
- short-wave energy
- chop/steepness
- directional spread
- foam likelihood

Long swell should remain coherent enough that vessels can visibly ride, surf, and launch from wave trains.

## 5. Shoreline and Open-Ocean Rule

### 5.1 User rule

Full ocean displacement applies to water that is more than 15 blocks from shore/land.

The final 15 blocks toward land use a smooth transition.

### 5.2 Smooth attenuation

Define a shore factor `S` in the range 0-1 using a smoothstep-style function over shoreline distance:

- at or immediately beside shore: `S` approaches 0
- between shore and 15 blocks: `S` rises smoothly
- at 15 blocks or farther: `S = 1`

Apply `S` to all Gerstner displacement, including:

- X displacement
- Y displacement
- Z displacement
- wave orbital velocity used for vessel interaction
- wave-derived foam/breaking intensity where appropriate

This prevents a visible seam at the 15-block threshold.

### 5.3 Client shore-distance field

The client maintains a camera-centered cached shore-distance field for visible water.

Requirements:

- stores water-to-land distance clamped to 15 blocks
- updates incrementally or when camera/chunk state invalidates relevant regions
- is uploaded in a GPU-friendly representation such as a compact texture or equivalent buffer
- shader samples it in world X/Z space
- must not scan terrain per water vertex per frame

The field is a rendering acceleration structure, not authoritative world state.

### 5.4 Server vessel-local shoreline queries

The server does not build a world-scale coastline texture.

For each active physics vessel, shoreline attenuation is calculated/cached only around the vessel and required buoyancy/sample points.

The server and client must use the same 15-block attenuation function even though their data structures differ.

### 5.5 Rivers and lakes

The rule is geometric rather than biome-name based.

A sufficiently wide river or lake can receive full ocean motion at points farther than 15 blocks from land. Narrow water bodies naturally stay damped because no point reaches the full-distance threshold.

## 6. Water Rendering Integration

### 6.1 Vanilla/Fabric path

Newest Ocean injects the shared displacement function into the active water vertex rendering path.

The fragment side receives or reconstructs:

- corrected displaced normal
- foam factor
- optional wave metadata needed for Newest Ocean-specific effects

Minecraft's own water coloration, fog, lighting, and normal material behavior should remain intact unless explicitly replaced by a supported Newest Ocean effect.

### 6.2 Sodium path

Sodium compatibility is mandatory.

Newest Ocean must integrate with Sodium's water shader/chunk rendering path rather than drawing a separate surface after Sodium completes world rendering.

The integration layer must be isolated behind a Sodium compatibility module so version-specific hooks do not leak into the shared spectrum or physics packages.

### 6.3 Iris shaderpack path

Iris compatibility is mandatory.

Newest Ocean injects deformation into shaderpack water programs, conceptually matching the integration style publicly documented by Physics Mod:

- water vertex stage receives Newest Ocean displacement
- water fragment stage can receive displaced normal and foam information
- shadow water geometry receives matching displacement

Target shader stages include the active water program, commonly `gbuffers_water`, and corresponding shadow paths when the shaderpack renders water into shadows.

### 6.4 Iris internal API risk

Iris shader transformation internals are not treated as a stable public API.

Therefore:

- isolate all Iris-internal interaction in a small version-gated bridge
- detect supported Iris versions/capabilities explicitly
- keep shared rendering math independent of Iris classes
- provide diagnostics when injection cannot be resolved safely
- never silently switch back to the old duplicate ocean renderer

### 6.5 DEPTHS behavior

DEPTHS presets use the same displaced geometry as all other supported shaderpacks.

Pack-specific tuning may adjust:

- foam strength
- normal-detail contribution
- alpha/material parameters if needed
- performance caps on visual wave count

Pack-specific tuning must not disable the X/Y/Z geometry motion.

### 6.6 Shadow rendering

The previous rule that skipped Newest Ocean during Iris shadow passes is removed for the new primary renderer.

Displaced water must participate in shadow geometry using the same large-scale displacement as the visible pass so wave silhouettes and shadows remain spatially consistent.

### 6.7 Unsupported compatibility fallback

If a renderer or shaderpack version cannot be injected safely:

- log and expose the failure in diagnostics
- use ordinary undisplaced Minecraft water
- keep gameplay stable
- never enable the old duplicate ocean overlay automatically

## 7. Vessel Physics

### 7.1 Existing foundation to preserve

Retain and evolve the current multi-point vessel solver.

Existing useful behavior includes:

- multiple buoyancy points
- force application at hull points
- surface normal influence
- relative water velocity
- bow/stern load accounting
- port/starboard load accounting
- torque accumulation
- re-entry shaping
- force and torque caps

### 7.2 Simplified synchronized 3D water field

Vessel physics uses the dominant shared wave subset and samples:

- surface height
- surface normal
- X water velocity
- Y water velocity
- Z water velocity

This is intentionally simpler than the final visual ocean but phase-aligned with the dominant visible waves.

### 7.3 Pitch and roll

Pitch and roll should emerge from multi-point support rather than visual-only animation.

- bow vs stern water support drives pitch
- port vs starboard support drives roll
- local wave normals influence force direction
- larger vessels use profile-specific inertia and damping

### 7.4 Surfing

Surfing uses the actual moving local wave face.

A vessel can gain forward or diagonal momentum when descending a compatible moving wave face. The transferred energy must depend on:

- wave-face slope
- vessel heading
- vessel velocity
- local water orbital velocity
- contact fraction
- hull/profile parameters

Wave push is not restricted to the vessel's forward axis. Cross-wave orbital motion can shove the vessel laterally.

### 7.5 Planing and gliding

Target feel: between realistic and cinematic.

At sufficient speed:

- hydrodynamic support increases along the local water normal
- effective water drag reduces progressively as wetted contact falls
- momentum is preserved across multiple crests
- the vessel can skim rather than snap vertically to every sample
- rough cross-seas still impose drag and impact loads

Planing lift must not be implemented as unconditional world-Y lift.

### 7.6 Crest crossing and launch

When a fast vessel climbs a crest:

- climbing costs some forward momentum
- support can rapidly decrease after the crest
- if support falls below a defined threshold and upward/forward momentum is sufficient, the vessel becomes airborne

While airborne:

- water solver does not glue the vessel to the mathematical surface
- normal entity gravity and inertial motion continue
- ocean contact is tested for re-entry

### 7.7 Re-entry

Reuse and evolve the existing re-entry system.

Requirements:

- progressive wetting on first contact
- capped slam-force spikes
- impact severity from relative velocity
- torque damping
- hull-size-aware angular response
- no immediate surface snapping

### 7.8 Rough-sea penalty

Gliding must not become free acceleration.

Poor heading into steep or cross-moving waves can cause:

- forward speed loss
- lateral shove
- roll/pitch disturbance
- harder re-entry
- reduced planing efficiency

This preserves meaningful sea state while still allowing satisfying high-speed wave riding.

## 8. Whitecaps, Shore Breakers, and Wakes

### 8.1 Whitecaps

Whitecaps migrate into the real water shading path.

Foam is driven by deterministic wave metrics such as:

- crest height
- steepness
- local curvature
- weather strength
- shoreline attenuation/breaker context

No full-screen or duplicate translucent ocean sheet is used.

### 8.2 Shore breakers

The 15-block shore-distance field provides the basis for future breaker behavior.

Initial rewrite requirement is smooth geometric attenuation. Breaker curling/spray can be layered later, but any shoreline foam must remain integrated with the real water material.

### 8.3 Vessel wakes

Existing wake simulation data may be retained, but visual wakes should ultimately feed the water material/shader path rather than remain a permanently independent translucent sheet.

Wake migration can be staged after the primary surface replacement if needed, provided the old primary ocean overlay is removed first.

## 9. Performance Architecture

### 9.1 Hardware floor

LOW is targeted at GTX 1050 Ti / GTX 1650-class dedicated GPUs.

The phase is not considered complete based only on a high-end development GPU.

### 9.2 Performance principles

- reuse Minecraft/Sodium water geometry
- avoid a second ocean mesh
- avoid per-frame CPU generation of displaced visible vertices
- evaluate visual waves on GPU
- keep server physics to dominant components only
- cache shoreline distance
- update shoreline data incrementally
- keep compatibility bridges thin
- allocate no large per-frame temporary arrays in the hot path

### 9.3 Adaptive quality

Existing runtime quality configuration remains useful but changes what it controls.

Adaptive quality may lower:

- number of visual spectrum components
- normal-detail octave count
- foam complexity
- shoreline field resolution/update frequency if necessary

Adaptive quality must not change authoritative vessel physics.

## 10. Configuration

Retain Mod Menu + Cloth Config as the client configuration surface.

Existing visual options should be mapped onto the new architecture where meaningful.

Likely retained controls:

- ocean rendering enabled
- quality preset
- adaptive quality enabled
- target FPS
- adaptive min/max quality
- visual wave override/cap
- whitecaps enabled/intensity
- wakes enabled/intensity
- shoreline enabled/intensity
- shader compatibility diagnostics
- DEPTHS tuning

Options that referred specifically to the old finite overlay mesh may be removed, renamed, or redefined during implementation planning.

Server-authoritative physics remains non-client-configurable.

## 11. Package and Ownership Direction

Keep responsibilities separated.

Suggested ownership:

- `ocean`: deterministic spectrum, shared wave evaluation, conditions
- `physics`: buoyancy, vessel motion, surfing/planing/re-entry
- `client.water`: renderer-independent water displacement data and client runtime state
- `client.compat.sodium`: Sodium-specific hooks
- `client.compat.iris`: Iris shader transformation/injection bridge
- `client.shore`: shore-distance field generation/cache/upload
- `mixin`: only narrow Minecraft/Sodium/Iris hook entrypoints where required

Do not place core wave math inside compatibility classes.

## 12. Migration from the Current Renderer

Implementation should be staged so the broken renderer cannot remain accidentally active.

Recommended migration sequence:

1. Introduce the expanded deterministic spectrum while preserving existing tests.
2. Add shared shore attenuation and vessel-side use.
3. Add real-water vanilla rendering integration behind a development flag.
4. Add Sodium integration.
5. Add Iris water and shadow injection.
6. Validate DEPTHS.
7. Switch the primary renderer from `OceanWorldRenderer` overlay rendering to real-water deformation.
8. Disable/remove the duplicate surface path.
9. Migrate whitecaps and shoreline visuals into the water material path.
10. Migrate wakes where practical.
11. Remove dead overlay-only shader and LOD code after regression coverage proves it is no longer needed.

At no stage should a failed shader bridge automatically reactivate the old duplicate ocean surface.

## 13. Testing Strategy

### 13.1 Unit tests

Add deterministic tests for:

- spectrum generation from seed
- component ordering
- dominant subset stability
- X/Y/Z displacement
- water velocity in X/Y/Z
- normal calculation
- weather modulation
- shore smoothstep attenuation
- client/server attenuation equivalence
- vessel planing force direction
- lateral wave push
- crest launch conditions
- airborne no-snap behavior
- re-entry shaping

### 13.2 Contract tests

Add source/structure contracts ensuring:

- old `AFTER_TRANSLUCENT` surface is not the primary ocean renderer
- Iris shadow displacement is present
- unsupported Iris falls back to flat vanilla water
- no automatic duplicate-surface fallback exists
- compatibility code remains isolated from core ocean math

### 13.3 Integration matrix

Manual graphical QA must cover at minimum:

- Vanilla/Fabric, no Sodium/Iris
- Sodium, no Iris
- Iris with no shaderpack active
- Iris with a generic shaderpack
- DEPTHS_LOW
- DEPTHS_MEDIUM
- DEPTHS_HIGH
- DEPTHS_ULTRA

For each case verify:

- moving X/Y/Z geometry
- correct camera-relative rendering
- no duplicate water sheet
- no obvious shore seam
- correct shadow movement where supported
- no severe transparency/depth artifacts
- boats remain aligned with dominant visible waves

### 13.4 Vessel QA

Test at least:

- vanilla boat
- Small Ships supported vessel
- Shippy Ships supported vessel if bridge is available

Scenarios:

- calm open water
- diagonal swell
- cross sea
- high-speed crest sequence
- launch and re-entry
- near-shore damping transition
- rain/thunder

## 14. Performance Acceptance

Before completion, capture repeatable measurements for LOW on representative GTX 1050 Ti / GTX 1650-class hardware or the closest available equivalent.

Record at minimum:

- baseline FPS with Newest Ocean disabled
- LOW FPS with animated ocean
- frame-time impact
- visible water area/render distance
- shaderpack status
- number of active visual wave components

The exact acceptable percentage budget should be finalized during implementation planning after a first prototype profile, but LOW must remain usable on the target hardware and must be materially lighter than a full FFT approach.

## 15. Failure Handling and Diagnostics

Diagnostics should expose:

- active renderer path: Vanilla, Sodium, Iris
- active shaderpack name when available
- water injection success/failure
- shadow injection success/failure
- current visual wave count
- physical wave count
- shore-field status
- adaptive quality state
- fallback reason if water remains flat

A compatibility failure must degrade to ordinary Minecraft water, not graphical corruption.

## 16. Acceptance Criteria

The architecture is complete only when all of the following are true:

1. No duplicate cyan/black ocean sheet exists under any supported renderer.
2. Open-ocean water visibly moves in X, Y, and Z.
3. Water geometry remains animated under Vanilla/Fabric.
4. Water geometry remains animated under Sodium.
5. Water geometry remains animated under Iris shaderpacks.
6. DEPTHS presets retain animated geometry.
7. Displaced water participates correctly in Iris shadow rendering where the pack renders water shadows.
8. The 15-block shoreline transition is smooth with no hard boundary.
9. Narrow water bodies naturally remain damped unless a point is more than 15 blocks from land.
10. Vessel physics uses the same dominant deterministic wave field as graphics.
11. Boats can pitch, roll, surf, plane, glide across successive crests, launch, and re-enter.
12. Wave motion can influence vessels laterally as well as vertically and forward/backward.
13. Airborne vessels are not snapped back to the surface.
14. Rough seas still impose meaningful drag and instability.
15. Unsupported renderer/shader injection falls back to flat Minecraft water, never the old overlay renderer.
16. LOW is profiled against the GTX 1050 Ti / GTX 1650-class performance target.
17. Existing networking, synchronized ocean seed, vessel adapters, Mod Menu config, and server-authoritative gameplay remain intact unless explicitly changed by the implementation plan.

## 17. Out of Scope for This Rewrite

Unless required to complete the primary renderer, the following remain separate follow-up work:

- full FFT simulation
- cinematic spray/impact particle system
- volumetric breaking-wave simulation
- physically simulated fluid volume or voxel fluid redistribution
- changing Minecraft's actual block-level water state as waves pass
- copying Physics Mod Pro proprietary implementation code

The objective is a lightweight, deterministic, real-water 3D ocean that approaches the visible and gameplay feel of Physics Mod Pro while remaining maintainable and compatible with the target Fabric rendering stack.
