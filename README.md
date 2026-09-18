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

## Phase 5: surfing and planing

Phase 5 lets supported vessels use wave energy instead of only reacting vertically to the water surface:

- Surfing uses the already-sampled local water normal to identify the downhill face of a wave.
- Forward surfing force is added only when the vessel is moving along a sufficiently steep downhill face and its heading is aligned with that face.
- Horizontal Gerstner wave velocity contributes a small additional push, while deterministic background current is excluded so steady current cannot masquerade as surfing.
- Surfing uses the real wave slope scale produced by the six-band ocean rather than requiring unrealistic water speeds.
- Planing lift rises smoothly with forward speed and hull support instead of switching on abruptly.
- Adaptive hull profiles include a planing factor: small/light hulls plane readily, while large displacement hulls receive much less dynamic lift.
- Planing strength is squared by the hull factor so heavy Cogs and Caravel-sized hulls remain primarily displacement vessels.
- Surfing and planing forces are only applied during normal displacement contact. Launching, airborne, and re-contact modes retain the protections from Phases 3 and 4.
- Unit tests cover downhill surfing, opposite-face rejection, low-speed non-planing, high-speed planing, hull-specific planing strength, and nearly dry hulls.

## Phase 6: multiplayer synchronization hardening

Phase 6 makes the deterministic ocean safe across dedicated servers, singleplayer/LAN, reconnects, respawns, and dimension changes without adding continuous wave networking:

- The logical server owns one authoritative ocean seed for the entire server session, derived once from the overworld seed at server startup.
- Joining players receive that already-authoritative seed instead of causing the global ocean to be recomputed from their current dimension.
- Logical server and logical client ocean state are stored separately, which is required because singleplayer/LAN can run both sides inside the same JVM.
- Client play-session state is explicitly invalidated when a new play connection initializes and again on disconnect, preventing a previous server's ocean from being reused while reconnecting.
- Receipt of the seed marks the current client play session synchronized and rebuilds only the client-side deterministic ocean.
- Respawns and dimension changes keep the synchronized seed because they remain within the same play connection.
- Minecraft's existing synchronized world time and weather state remain the shared clock/environment source, so no per-tick wave packets are needed.
- The synchronization state tracks generations so a fresh session remains distinguishable even when two sessions happen to use the same seed.
- Unit tests cover unsynchronized startup, seed acceptance, stale-state reset, reconnects, and same-seed fresh-session synchronization.

The networking cost remains one tiny seed payload per play connection. Mesh vertices, wave samples, vessel forces, and ongoing ocean animation are never networked.

## Phase 7: ocean LOD mesh architecture

Phase 7 provides the reusable visual topology used by the renderer:

- Three camera-centered square LOD regions use dense near cells and progressively coarser middle/far cells.
- The rings are true annular strips rather than overlapping complete grids, avoiding hidden duplicate geometry.
- The camera origin snaps to the far-grid spacing so small camera movement does not rebuild topology.
- Topology is local/camera-independent and cached by visual quality tier.
- Shared coordinates are deduplicated across LOD regions.
- Water coverage is classified separately from topology, allowing shoreline index filtering without rebuilding connectivity.
- CPU mesh generation applies the same synchronized deterministic wave height, horizontal displacement, and normals used by physics.
- Quality-tier vertex budgets are substantially lower than equivalent full-resolution uniform grids.

## Phase 8: first visible synchronized ocean

Phase 8 connects the deterministic LOD ocean to Minecraft's world renderer:

- The renderer registers through Fabric `WorldRenderEvents.AFTER_TRANSLUCENT`.
- Rendering is disabled until the current client play connection has received its synchronized ocean seed.
- Render-frame preparation uses synchronized world time with tick interpolation, Minecraft rain/thunder state, tide/current conditions, and the current visual wave budget.
- LOD topology is reused from Phase 7 and translated relative to the active camera.
- Sea-level water cells are discovered from the actual client world fluid state and cached by snapped mesh origin, quality tier, and dimension.
- Coverage refreshes periodically so nearby water edits can eventually update without per-frame block/fluid probing.
- Only water cells emit visible geometry; land cells do not receive ocean quads.
- The Phase 8 proof path uses immediate position/color submission after translucent terrain with blending and depth testing.
- Surface color is lightly modulated from deterministic wave normals so the proof mesh has readable moving shape.
- Unit tests cover synchronization gating, interpolated render time, LOD-frame preparation, camera-relative coordinates, and shared weather/environment inputs.

## Phase 9: GPU displacement

Phase 9 removes per-visible-vertex wave evaluation from the preferred CPU render path:

- A Fabric core shader is registered from the `newestocean:ocean_surface` shader resources.
- The CPU packs the deterministic physical wave components into a compact six-slot uniform payload.
- Quality tiers still choose how many of those physical components are active visually, while server physics always keeps the full physical wave field.
- The vertex shader evaluates the same Gerstner phase, vertical height, horizontal displacement, slope, and normal equations used by `ProceduralOcean`.
- CPU rendering submits undisplaced camera-relative base geometry from the cached LOD topology instead of recomputing animated positions and normals every frame.
- World-space phase sampling is reconstructed from the camera position so distant coordinates remain stable while submitted geometry remains camera-relative.
- Time, wave scale, tide-adjusted water height, camera coordinates, and packed wave parameters are the only dynamic shader inputs required for wave animation.
- Water/land coverage and LOD topology remain CPU responsibilities because they change much less often than wave animation.
- A CPU reference implementation of the packed GPU wave payload is regression-tested against the authoritative `ProceduralOcean` equations at realistic GPU float precision.
- If the custom shader has not loaded, the Phase 8 CPU-displaced renderer remains available as a safe fallback rather than making the ocean disappear.

Phase 9 moves the expensive trigonometric wave animation and normal generation to the GPU without changing authoritative vessel physics or adding network traffic.

## Phase 10: adaptive quality

Phase 10 turns visual quality into a real time-based performance controller. Sustained frame misses reduce one visual tier after roughly three seconds, while upgrades require roughly ten seconds of stable headroom. Loading stalls are ignored and physics never changes with graphics quality.

## Phase 11: physics LOD

Phase 11 reduces expensive stable-vessel ocean solves by distance while retaining cheap per-tick bookkeeping and smooth cached-force interpolation. Player-controlled, launching, airborne, and re-contact vessels stay at full 20 Hz physics.

## Phase 12: foam and whitecaps

Phase 12 adds shader/math-driven crest foam from wave slope, positive crest curvature, storm strength, and visual quality. The effect requires no foam simulation grid, network traffic, or ocean-wide particle system, and the CPU fallback has an approximate matching path.

## Phase 13: vessel wakes

Phase 13 adds bounded client-only wakes for vanilla boats and Small Ships:

- Client entity events track loaded `BoatEntity` instances without a world-wide scan.
- Each vessel keeps at most 12 accepted wake samples for four seconds.
- Stationary vessels below 0.12 blocks/tick do not generate new samples.
- New points require at least 0.75 blocks of movement.
- Quality tiers cap both tracked vessel count and tracking radius.
- Each segment generates two V-shaped stern arms plus a centered turbulence strip.
- Wake height follows the synchronized procedural ocean.
- Wave height is sampled once per history point and reused across that point's wake vertices to keep CPU cost bounded.
- The wake pass is blended, depth-tested, camera-relative, and visual-only.
- No wake packets, server wake state, particles, or vessel-force changes are introduced.

See `docs/PHASE_13_VESSEL_WAKES.md` for the detailed runtime limits.

## Current implementation

The `feature/ocean-core` branch currently contains:

- Phase 1 deterministic ocean core.
- Phase 2 shared vessel physics foundation.
- Phase 3 launch/airborne/re-contact state system.
- Phase 4 progressive re-entry and passive angular stability system.
- Phase 5 surfing and hull-specific planing dynamics.
- Phase 6 hardened logical server/client ocean synchronization.
- Phase 7 cached concentric LOD ocean topology and water masking.
- Phase 8 first visible synchronized ocean renderer and CPU fallback.
- Phase 9 dedicated GPU Gerstner displacement and normal generation.
- Phase 10 time-based adaptive visual quality.
- Phase 11 distance-based vessel physics LOD with full-rate safety exceptions.
- Phase 12 procedural foam and whitecaps.
- Phase 13 bounded client-only, wave-following vessel wakes.
- Vanilla boat wave-force correction.
- Optional Small Ships tracking and size-scaled physics integration.
- Vessel pose sampling from the same ocean surface.
- Java 21 / Fabric 1.21.1 CI.

## Planned architecture

```text
Deterministic ocean state
  -> waves
  -> weather
  -> tides
  -> currents
      -> authoritative server ocean
      -> synchronized client reconstruction
          -> cached concentric LOD topology
          -> cached water coverage
          -> GPU wave-uniform payload
          -> GPU-displaced visible ocean
          -> procedural whitecaps
          -> bounded vessel wake histories
          -> wave-following wake quads
          -> CPU fallback

Vessel physics
  -> shared hull profiles
  -> vanilla boats
  -> Small Ships
  -> detailed water contact
  -> launch / airborne glide / re-contact
  -> progressive re-entry / angular stability
  -> surfing / hull-specific planing
  -> distance-based physics LOD

Renderer
  -> camera-centered LOD mesh
  -> water-only coverage indices
  -> GPU Gerstner displacement and normals
  -> adaptive quality
  -> foam / whitecaps
  -> vessel wakes
  -> shoreline effects
```

Physics and graphics remain separate. Reducing ocean graphics quality must never change authoritative vessel motion.
