# Phase 17: Runtime Tuning and Mod Menu Configuration

Phase 17 turns Newest Ocean's client rendering controls into a persistent, live-applied configuration system while preserving the authoritative six-wave physical ocean.

## Configuration stack

- Minecraft: 1.21.1
- Fabric
- Cloth Config Fabric: 15.0.140
- Mod Menu API: 11.0.3
- Config file: `config/newestocean-client.json`

Cloth Config is the runtime configuration UI dependency. Mod Menu is optional. If Mod Menu is installed, its configuration button opens `NewestOceanConfigScreen` through `NewestOceanModMenu`.

Settings are loaded during client initialization. Pressing the Cloth Config save button sanitizes the edited values, writes the JSON file, and applies the new rendering configuration without restarting Minecraft.

## Settings

### General

| Setting | Default | Range / values | Effect |
| --- | --- | --- | --- |
| Ocean Rendering | On | On / Off | Enables or disables Newest Ocean client rendering. It does not disable server physics. |
| Quality Preset | Medium | Potato / Low / Medium / High / Ultra | Sets the starting visual quality tier. |
| Adaptive Quality | On | On / Off | Allows visual quality to move between the configured minimum and maximum tiers. |
| Target FPS | 60 | 30 to 240 | Sets the adaptive-quality frame-time target. |
| Adaptive Minimum Quality | Potato | Potato to Ultra | Lowest tier adaptive quality may select. |
| Adaptive Maximum Quality | Ultra | Potato to Ultra | Highest tier adaptive quality may select. |

If the minimum quality is configured above the maximum quality, sanitization swaps the two values so the resulting interval is valid.

### Ocean

| Setting | Default | Range / values | Effect |
| --- | --- | --- | --- |
| Visual Wave Count | Auto | Auto or 1 to 6 | Overrides the quality preset's visual wave count. Auto uses the active quality tier. |
| Render Distance Scale | 1.00 | 0.50 to 2.00 | Scales the visual ocean LOD radius. |
| Ocean Opacity | 1.00 | 0.25 to 1.00 | Multiplies ocean surface alpha in both GPU and CPU paths. |

Visual wave count is rendering-only. The physical ocean continues to evaluate all six deterministic wave components.

### Whitecaps

| Setting | Default | Range / values | Effect |
| --- | --- | --- | --- |
| Whitecaps Enabled | On | On / Off | Enables crest foam / whitecap contribution. |
| Whitecap Intensity | 1.00 | 0.00 to 2.00 | Multiplies visible whitecap intensity. |

### Vessel wakes

| Setting | Default | Range / values | Effect |
| --- | --- | --- | --- |
| Vessel Wakes Enabled | On | On / Off | Enables client-only vessel wake rendering. |
| Wake Intensity | 1.00 | 0.00 to 2.00 | Multiplies visible wake strength. |

Wake settings do not alter vessel velocity, buoyancy, launch behavior, or server state.

### Shoreline

| Setting | Default | Range / values | Effect |
| --- | --- | --- | --- |
| Shoreline Effects Enabled | On | On / Off | Enables shoreline breaker, shoaling, and wash rendering. |
| Shoreline Intensity | 1.00 | 0.00 to 2.00 | Multiplies visible shoreline effect strength. |

### Shader compatibility

| Setting | Default | Range / values | Effect |
| --- | --- | --- | --- |
| Newest Ocean Custom Shaders | On | On / Off | Allows Newest Ocean's custom GPU shaders when the compatibility mode says they are safe. |
| DEPTHS_ULTRA Visual Wave Cap | 4 | 1 to 6 | Limits visual CPU wave evaluation while the dedicated DEPTHS_ULTRA compatibility profile is active. |
| DEPTHS_ULTRA Ocean Alpha | 0.58 | 0.25 to 1.00 | Sets DEPTHS_ULTRA CPU-fallback ocean base alpha. |
| DEPTHS_ULTRA Whitecap Multiplier | 0.65 | 0.00 to 2.00 | Scales CPU whitecaps in DEPTHS_ULTRA mode. |
| DEPTHS_ULTRA Wake Multiplier | 0.90 | 0.00 to 2.00 | Scales CPU wakes in DEPTHS_ULTRA mode. |
| DEPTHS_ULTRA Shoreline Multiplier | 0.90 | 0.00 to 2.00 | Scales CPU shoreline effects in DEPTHS_ULTRA mode. |

Phase 16 compatibility routing remains authoritative. Disabling custom Newest Ocean shaders cannot force them on under Iris. Iris shadow-pass skipping is unchanged. Unknown active Iris packs continue to use the generic Iris-safe CPU fallback. The values above only tune the selected compatibility path.

### Diagnostics

| Setting | Default | Effect |
| --- | --- | --- |
| Diagnostics Overlay | Off | Shows current FPS and target FPS, effective quality, shader compatibility mode, effective visual wave count, and render-distance scale. |

The overlay is client-only and reads existing rendering state. It does not sample or mutate server physics.

## Live application

`OceanConfigManager` owns the current sanitized client settings. On load or save it calls `OceanWorldRenderer.applyConfig(...)`.

Applying configuration:

- selects the configured starting quality tier;
- rebuilds the adaptive-quality sampler using target FPS and minimum/maximum quality bounds;
- resets visual coverage and shoreline caches so render-distance or quality changes cannot retain stale geometry;
- causes later frames to read the new visual-wave, opacity, whitecap, wake, shoreline, and shader-compatibility values;
- does not restart the logical server or replace the synchronized ocean seed.

The config screen edits a copy of the active configuration. Closing without saving does not mutate the live settings. Saving sanitizes, persists, and applies the copy.

## Adaptive-quality behavior

The Phase 10 hysteresis rules remain intact. Phase 17 adds configurable target FPS and hard visual-quality bounds.

- sustained slow frame time may lower one tier at a time;
- sustained headroom may raise one tier at a time;
- quality never moves below `adaptiveMinQuality`;
- quality never moves above `adaptiveMaxQuality`;
- disabling adaptive quality leaves the selected visual quality fixed;
- changing graphics quality never changes the physical wave field.

## Shader-mode behavior

The central `ShaderCompatibility` snapshot remains the single decision point for renderer routing.

### NORMAL_GPU

When no active Iris shaderpack owns the water pipeline and custom Newest Ocean shaders are enabled:

- the ocean uses GPU Gerstner displacement;
- shoreline effects may use the shoreline shader;
- vessel wakes may use the wake shader;
- Phase 17 opacity and effect intensity values are uploaded/applied to the relevant visual path.

If the user disables Newest Ocean custom shaders, the mode can remain logically `NORMAL_GPU`, but `allowCustomShaders()` becomes false and the renderer takes the existing CPU fallback paths.

### IRIS_GENERIC

An active non-DEPTHS Iris shaderpack keeps the Phase 16 Iris-safe fallback behavior. Phase 17 effect toggles and intensities apply on top of that fallback.

### IRIS_DEPTHS_ULTRA

The dedicated DEPTHS_ULTRA fallback keeps the Phase 16 defaults of:

- four visual CPU waves;
- 0.58 ocean base alpha;
- 0.65 whitecap multiplier;
- 0.90 wake multiplier;
- 0.90 shoreline multiplier.

Phase 17 makes those visual constants user-configurable while preserving the same defaults and safe rendering route.

## Physics isolation

No Phase 17 client setting is read from the authoritative server-side ocean or vessel-physics packages.

The following remain outside the client configuration system:

- six-component physical ocean evaluation;
- authoritative ocean seed;
- world-time and weather synchronization;
- buoyancy force;
- pitch/roll torque;
- launch, airborne, and re-contact state;
- surfing and planing forces;
- collision;
- networking;
- server physics LOD authority.

This means two clients may choose different ocean graphics settings while the server still computes the same vessel motion for everyone.

## Automated verification

Phase 17 extends the test suite with contracts for:

- config defaults and sanitization;
- malformed JSON fallback;
- JSON round-trip behavior;
- adaptive minimum/maximum quality bounds;
- config persistence and live application;
- scaled LOD radius;
- renderer setting propagation;
- configurable shader-compatibility values;
- Cloth Config coverage of every user-facing setting;
- Mod Menu entrypoint wiring;
- diagnostics HUD registration and runtime-state reporting;
- client-config isolation from non-client/server physics packages.

The GitHub Actions release gate remains `gradle build --stacktrace`, followed by upload of the `newestocean-dev` artifact only after the build and tests pass.

## Runtime validation still useful in a real client

Automated CI verifies compilation, unit behavior, source contracts, and artifact packaging. Final visual tuning is still best inspected in a real Minecraft client for:

- above-water and underwater views;
- day, night, rain, and thunderstorms;
- shoreline-heavy scenes;
- multiple vanilla and Small Ships wakes;
- changing settings from Mod Menu while in-game;
- enabling and disabling Iris shaderpacks without restarting;
- DEPTHS_ULTRA shadow passes;
- Potato through Ultra performance scaling;
- final visual preference for opacity, whitecaps, wake strength, and shoreline strength.

Those checks are visual/runtime tuning rather than missing Phase 17 architecture. Phase 15 spray and impact particles remain optional and deferred.
