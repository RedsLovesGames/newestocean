# Phase 17 Runtime Tuning and Mod Menu Configuration Design

## Goal

Add persistent client-side tuning controls, a Cloth Config screen exposed through Mod Menu, diagnostics, and runtime validation hooks without changing Newest Ocean's server-authoritative six-component physical ocean.

## Scope

Phase 17 owns client rendering and performance settings only. It must not make ocean seed, physical wave components, buoyancy, collision, vessel force, networking, or server synchronization client-configurable.

## Dependencies

- Minecraft 1.21.1 / Fabric
- Cloth Config Fabric 15.0.140, required for the configuration screen
- Mod Menu 11.0.3, optional integration entrypoint
- Gson for JSON persistence

## Persistent client settings

The client config is stored in `config/newestocean-client.json` and contains:

- ocean rendering enabled
- quality preset: POTATO / LOW / MEDIUM / HIGH / ULTRA
- adaptive quality enabled
- target FPS
- adaptive minimum quality
- adaptive maximum quality
- visual wave override: AUTO or 1-6
- render distance scale
- ocean opacity
- whitecaps enabled and intensity
- wakes enabled and intensity
- shoreline enabled and intensity
- custom Newest Ocean shaders enabled when compatibility permits them
- DEPTHS_ULTRA visual wave cap
- DEPTHS ocean alpha
- DEPTHS whitecap multiplier
- DEPTHS wake multiplier
- DEPTHS shoreline multiplier
- diagnostics overlay enabled

Invalid or stale JSON values are sanitized to safe bounds. Unknown/missing fields use defaults.

## Runtime behavior

`OceanConfigManager` loads the config during client initialization and exposes the current sanitized snapshot. Saving from the config screen writes JSON and applies the new settings immediately.

The renderer uses the config to:

- skip ocean rendering entirely when disabled
- rebuild coverage/topology state after quality or render-distance changes
- apply target FPS and adaptive quality bounds
- apply visual-wave override without changing physical waves
- scale render radius
- enable/disable whitecaps, wakes, and shoreline effects independently
- scale CPU/GPU ocean opacity and whitecap strength
- scale wake and shoreline intensity
- disable Newest Ocean custom shaders while retaining CPU fallback
- override the Phase 16 DEPTHS tuning constants and visual cap

## Shader compatibility

`ShaderCompatibility` remains the single per-frame compatibility authority. Its existing one-argument test API keeps Phase 16 defaults, while a config-aware overload powers runtime settings. Iris shadow-pass skipping remains mandatory and cannot be disabled from the UI.

## Mod Menu and Cloth Config

A `ModMenuApi` entrypoint returns a Cloth Config screen factory. Mod Menu remains optional. Cloth Config is a runtime dependency because the screen implementation directly uses its API.

The screen groups settings into General, Ocean, Whitecaps, Wakes, Shoreline, Shader Compatibility, and Diagnostics. Save applies changes live. Defaults are visible through Cloth Config's reset controls.

## Diagnostics

When enabled, a lightweight HUD line reports effective quality, compatibility mode, visual wave count, configured target FPS, and approximate observed FPS. Diagnostics are client-only.

## Testing

Automated tests cover:

- config defaults and sanitization
- JSON round trip
- visual-wave override
- render-distance scaling
- adaptive min/max bounds
- config-aware shader compatibility constants and custom-shader disable
- source contracts for Mod Menu/Cloth Config wiring
- renderer propagation for effect toggles/intensities
- physics/server isolation from client config classes

Final verification requires a successful GitHub Actions build on the exact final `feature/ocean-core` SHA and a `newestocean-dev` artifact for that SHA.
