# Phase 17 Runtime Tuning and Mod Menu Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persistent client tuning, a Cloth Config screen exposed through Mod Menu, diagnostics, and live renderer application while preserving the authoritative six-wave physical ocean.

**Architecture:** `OceanClientConfig` is a pure sanitized settings model, `OceanClientConfigCodec` handles JSON, and `OceanConfigManager` owns Fabric config-path persistence plus live application. Existing render and compatibility classes consume the current config. Mod Menu provides the entrypoint, Cloth Config renders the settings UI, and a lightweight HUD diagnostics class exposes effective runtime state.

**Tech Stack:** Java 21, Fabric 1.21.1, Cloth Config Fabric 15.0.140, Mod Menu 11.0.3, Gson, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-18-phase-17-mod-menu-tuning-design.md`

## Global Constraints

- Work only on `feature/ocean-core`; never modify `main`.
- Keep the physical/server ocean at all six components.
- Keep server seed, networking, buoyancy, collision, and vessel force outside client config.
- Mod Menu is optional; Cloth Config is the runtime UI dependency.
- Settings save to `config/newestocean-client.json` and apply without restart.
- Iris shadow-pass skipping remains mandatory.
- Preserve the Phase 16 safe CPU fallback when custom shaders are disabled or Iris owns the water pipeline.

---

### Task 1: Pure persistent config model

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/config/OceanClientConfig.java`
- Create: `src/main/java/com/redslovesgames/newestocean/client/config/OceanClientConfigCodec.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/config/OceanClientConfigTest.java`

**Interfaces:**
- `OceanClientConfig defaults()`
- `OceanClientConfig copy()`
- `OceanClientConfig sanitize()`
- `int effectiveVisualWaveComponents(int qualityDefault)`
- `double targetFrameMs()`
- `OceanClientConfigCodec.encode/decode`

- [ ] Write failing tests for defaults, bounds, min/max quality ordering, visual-wave override, target FPS, and JSON round-trip.
- [ ] Run focused tests and confirm RED because config classes do not exist.
- [ ] Implement the minimal model and codec.
- [ ] Run focused tests and full suite.
- [ ] Commit.

### Task 2: Runtime manager and adaptive-quality bounds

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/config/OceanConfigManager.java`
- Modify: `AdaptiveQualityController.java`
- Modify: `AdaptiveQualityFrameSampler.java`
- Modify: `NewestOceanClient.java`
- Test: `AdaptiveQualityControllerTest.java`
- Create: `OceanConfigManagerContractTest.java`

**Interfaces:**
- `OceanConfigManager.load()`
- `OceanConfigManager.current()`
- `OceanConfigManager.saveAndApply(OceanClientConfig)`
- adaptive controller overload with min/max quality bounds

- [ ] Add failing tests for adaptive quality bounds and manager source contracts.
- [ ] Run focused tests and confirm RED.
- [ ] Add bounded adaptive controller behavior and config manager persistence.
- [ ] Load/apply config during client initialization.
- [ ] Run focused and full tests.
- [ ] Commit.

### Task 3: Live renderer tuning

**Files:**
- Modify: `OceanLodPlanner.java`
- Modify: `OceanRenderFrame.java`
- Modify: `OceanWorldRenderer.java`
- Modify: `OceanGpuShader.java`
- Modify: `VesselWakeRenderer.java`
- Modify: `ShorelineRenderer.java`
- Modify shader resources for ocean opacity/whitecap intensity
- Test: planner/config renderer contract tests

**Interfaces:**
- `OceanLodPlanner.plan(..., double renderDistanceScale)`
- runtime effect toggles and intensity multipliers from `OceanClientConfig`

- [ ] Write failing tests for scaled LOD radius and renderer config propagation.
- [ ] Run tests and confirm RED.
- [ ] Implement render radius scaling, visual wave override, effect toggles/intensities, ocean opacity, and GPU uniforms.
- [ ] Ensure coverage state resets when applying config.
- [ ] Run focused and full tests.
- [ ] Commit.

### Task 4: Config-aware shader compatibility

**Files:**
- Modify: `ShaderCompatibility.java`
- Test: `ShaderCompatibilityTest.java`

**Interfaces:**
- `snapshot(IrisState, OceanClientConfig)`
- one-argument `snapshot(IrisState)` retains Phase 16 defaults for compatibility tests

- [ ] Add failing tests for custom-shader disable and configurable DEPTHS cap/multipliers.
- [ ] Run tests and confirm RED.
- [ ] Implement config-aware snapshot values.
- [ ] Run focused and full tests.
- [ ] Commit.

### Task 5: Cloth Config + Mod Menu screen

**Files:**
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Modify: `fabric.mod.json`
- Create: `NewestOceanConfigScreen.java`
- Create: `NewestOceanModMenu.java`
- Create: source contract test

**Interfaces:**
- `NewestOceanConfigScreen.create(Screen parent)`
- `NewestOceanModMenu#getModConfigScreenFactory()`

- [ ] Add failing source contract test requiring all user-facing settings and `modmenu` entrypoint.
- [ ] Add exact compatible dependency repositories/versions.
- [ ] Implement categories and save consumers for every setting.
- [ ] Save through `OceanConfigManager.saveAndApply`.
- [ ] Run full suite/build and fix API mismatches only from compiler evidence.
- [ ] Commit.

### Task 6: Phase 17 diagnostics HUD

**Files:**
- Create: `OceanDiagnosticsHud.java`
- Modify: `NewestOceanClient.java`
- Add diagnostics contract test

**Interfaces:**
- client HUD registration
- reads current config, effective quality, compatibility mode, and frame rate without touching physics

- [ ] Add failing diagnostics source contract.
- [ ] Implement toggleable one-line diagnostics overlay.
- [ ] Run full tests/build.
- [ ] Commit.

### Task 7: Documentation and exact-head verification

**Files:**
- Create: `docs/PHASE_17_RUNTIME_TUNING.md`
- Modify: `README.md`

- [ ] Document every setting, defaults, ranges, live-apply behavior, and physics isolation.
- [ ] Run full `gradle build --stacktrace` through GitHub Actions.
- [ ] Require successful exact-head `Build and test`, successful `Upload mod jar`, and `newestocean-dev` artifact.
- [ ] Report final SHA and any live-game validation still requiring a real Minecraft/Iris context.
