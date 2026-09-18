# Real-Water Ocean Renderer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the duplicate ocean overlay with a deterministic 3D real-water renderer and synchronized low-cost vessel physics while preserving Vanilla/Fabric, Sodium, Iris, and DEPTHS compatibility.

**Architecture:** Minecraft/Sodium keeps ownership of water geometry. Newest Ocean injects shared Gerstner displacement into the real water vertex path, supplies normals and foam data to water shading, uses a cached 15-block shore field on the client and vessel-local shore queries on the server, and evaluates only the dominant synchronized waves for authoritative vessel physics.

**Tech Stack:** Java 21, Fabric 1.21.1, Yarn 1.21.1+build.3, Fabric API, Mixin, Sodium 0.8.12, Iris 1.8.14-beta.1, GLSL, JUnit 5, Cloth Config, Mod Menu.

**Spec:** `docs/superpowers/specs/2026-09-18-real-water-ocean-renderer-design.md`

## Global Constraints

- Work only on `feature/ocean-core`.
- Never modify, merge, rebase, retarget, or force-update `main`.
- Keep PR #1 draft until the rewrite and graphical QA are complete.
- Use TDD for each behavior change: add the smallest failing test, verify RED, implement the minimum production change, verify GREEN, then run the relevant regression suite.
- Never restore the old `AFTER_TRANSLUCENT` duplicate ocean surface as a fallback.
- Unsupported renderer/shader injection must degrade to ordinary undisplaced Minecraft water with diagnostics.
- Client quality/configuration may change visible detail only. It must not change the authoritative dominant physical wave subset.
- Preserve synchronized ocean seed/time/weather networking and existing vessel adapter ownership.
- Keep Iris and Sodium optional. Newest Ocean must still launch when either is absent.
- Keep renderer-specific code out of shared `ocean` and `physics` packages.
- Use Physics Mod public documentation only as behavioral/integration reference. Do not copy proprietary implementation code.
- Do not delete old overlay classes until the replacement paths are verified and no production references remain.

---

## Task 1: Pin Renderer Compatibility Targets and Add Conditional Mixin Loading

**Purpose:** Establish exact optional compile targets and prevent optional Iris/Sodium mixins from loading when those mods are absent.

**Files:**
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Modify: `src/main/resources/newestocean.mixins.json`
- Create: `src/main/java/com/redslovesgames/newestocean/mixin/NewestOceanMixinPlugin.java`
- Create: `src/test/java/com/redslovesgames/newestocean/mixin/NewestOceanMixinPluginTest.java`
- Create or modify: compatibility contract tests under `src/test/java/com/redslovesgames/newestocean/client/`

- [ ] **1.1 Characterize the exact runtime targets before editing production code.**
  - Pin Sodium to the user-tested `0.8.12+mc1.21.1` source/tag corresponding to commit `53306ac4db8f9fae1655c81539ffcd79e4afc4fb`.
  - Pin Iris to the user-tested `1.8.14-beta.1+mc1.21.1` source/version.
  - Identify the exact Sodium shader-source creation/program classes that can be patched without drawing a second surface.
  - Identify the exact Iris `TransformPatcher` and program/uniform binding points used by that version.
  - Record the selected class/method descriptors in comments beside the compatibility mixins when they are later added.

- [ ] **1.2 Add a RED test for conditional mixin decisions.**
  - Extract a pure decision helper in the test target, for example `shouldApplyMixin(String mixinClassName, Set<String> loadedMods)`.
  - Assert `.compat.iris.` applies only when `iris` is loaded.
  - Assert `.compat.sodium.` applies only when `sodium` is loaded.
  - Assert ordinary Newest Ocean mixins always apply.
  - Run the focused test and verify it fails because the plugin/helper does not exist.

- [ ] **1.3 Implement `NewestOceanMixinPlugin`.**
  - Implement `IMixinConfigPlugin`.
  - Use Fabric Loader only to detect optional mods.
  - Keep decision logic deterministic and side-effect free.
  - Do not initialize Iris or Sodium classes from the plugin.

- [ ] **1.4 Register the plugin in `newestocean.mixins.json`.**
  - Preserve `required: true`, Java 21 compatibility, and existing `BoatEntityMixin`.
  - Do not add renderer mixins yet.

- [ ] **1.5 Add optional compile dependencies only after coordinates resolve successfully.**
  - Prefer compile-only/non-transitive dependencies.
  - If Modrinth Maven is used, add `https://api.modrinth.com/maven` only after exact coordinates are verified.
  - Do not turn Iris or Sodium into Fabric runtime dependencies in `fabric.mod.json`.

- [ ] **1.6 Verify GREEN.**
  - Run focused mixin plugin tests.
  - Run `gradle build --stacktrace`.
  - Launch/build must remain valid with optional renderer mods absent from the runtime dependency graph.

- [ ] **1.7 Commit.**
  - Suggested commit: `Pin optional renderer compatibility targets`

---

## Task 2: Expand the Shared Spectrum and Implement True 3D Gerstner Evaluation

**Purpose:** Replace the fixed six-wave default with a deterministic ordered spectrum whose dominant subset is shared by client and server.

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/ocean/OceanSpectrum.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/ocean/ProceduralOcean.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/ocean/OceanSurface.java`
- Modify if needed: `src/main/java/com/redslovesgames/newestocean/ocean/WaveComponent.java`
- Create: `src/test/java/com/redslovesgames/newestocean/ocean/OceanSpectrumTest.java`
- Modify: existing `ProceduralOcean`/surface tests

- [ ] **2.1 Add RED deterministic-spectrum tests.**
  - Same seed produces byte-for-byte equivalent component parameters.
  - Different seeds change at least direction/phase values.
  - Spectrum component count is `OceanSpectrum.MAX_COMPONENTS`, initially `24`.
  - Components are ordered strongest/longest first by a documented deterministic ordering rule.
  - `OceanSpectrum.PHYSICS_COMPONENTS` is initially `6` and is independent of client quality.

- [ ] **2.2 Add RED 3D Gerstner sample tests.**
  - For at least one known wave, assert non-zero X, Y, and Z displacement over a phase cycle.
  - Assert analytical surface velocity matches finite-difference position change within tolerance.
  - Assert normal is unit length and points generally upward.
  - Assert sampling only the first `N` components is stable and deterministic.

- [ ] **2.3 Evolve `OceanSurface.SurfaceSample`.**
  - Replace `horizontalDisplacement` as the canonical field with full `Vec3 displacement`.
  - If required during migration, keep a clearly named helper accessor that returns only X/Z components, then remove it after all callers migrate.
  - Preserve height, normal, and surface velocity.

- [ ] **2.4 Implement the deterministic spectrum pool.**
  - Generate approximately 24 directional components from the synchronized seed.
  - Preserve dominant swell direction for long components.
  - Increase directional spread and reduce amplitude toward shorter components.
  - Keep all randomness derived from deterministic salts/constants and the synchronized seed.

- [ ] **2.5 Implement analytical Gerstner position, tangent, normal, and velocity.**
  - For each wave use phase:

    `theta = k * (Dx*x + Dz*z) - omega*t + phase`

    where `theta` is wave phase, `k = 2π / wavelength` is wave number, `Dx` and `Dz` are normalized horizontal direction components, `x` and `z` are world coordinates, `omega` is angular frequency, `t` is time in seconds, and `phase` is the deterministic phase offset.

  - Position contribution uses amplitude `A` and steepness `Q`:

    `Px = x + Q*A*Dx*cos(theta)`

    `Py = baseline + A*sin(theta)`

    `Pz = z + Q*A*Dz*cos(theta)`

    where `Px`, `Py`, and `Pz` are the displaced surface coordinates.

  - Use analytical tangents and cross-product normal, not slope-only approximation, so horizontal displacement and normal remain consistent.
  - Preserve deterministic current and tide semantics from `OceanConditions`.

- [ ] **2.6 Keep old public behavior working where possible.**
  - Existing code calling the four-argument `sample(...)` must continue to receive the full physical subset or full canonical surface as explicitly documented.
  - Add a named method/view for dominant physical sampling instead of overloading client quality into server behavior.

- [ ] **2.7 Verify GREEN and regressions.**
  - Run ocean tests.
  - Run vessel physics tests because they depend on `SurfaceSample`.
  - Run full build.

- [ ] **2.8 Commit.**
  - Suggested commit: `Expand deterministic 3D ocean spectrum`

---

## Task 3: Add Shared 15-Block Shore Attenuation and Pure Distance Grid Logic

**Purpose:** Make the shoreline rule a shared deterministic mathematical contract before adding client GPU or server cache implementations.

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/ocean/ShoreAttenuation.java`
- Create: `src/main/java/com/redslovesgames/newestocean/ocean/ShoreDistanceProvider.java`
- Create: `src/main/java/com/redslovesgames/newestocean/ocean/ShoreDistanceGrid.java`
- Create: `src/main/java/com/redslovesgames/newestocean/ocean/ShoreAttenuatedOceanSurface.java`
- Create: `src/test/java/com/redslovesgames/newestocean/ocean/ShoreAttenuationTest.java`
- Create: `src/test/java/com/redslovesgames/newestocean/ocean/ShoreDistanceGridTest.java`

- [ ] **3.1 Add RED attenuation tests.**
  - Distance `0` gives factor `0`.
  - Distance `15` gives factor `1`.
  - Distance greater than `15` remains `1`.
  - Intermediate values are monotonic and smooth.
  - The exact function is shared by all callers.

- [ ] **3.2 Implement the shared smoothstep rule.**
  - Compute `t = clamp(distance / 15.0, 0, 1)`.
  - Compute `S = t*t*(3 - 2*t)`.
  - `distance` is horizontal distance in blocks to nearest land, `t` is normalized shore distance, and `S` is the final attenuation factor in `[0,1]`.

- [ ] **3.3 Add RED attenuated-surface tests.**
  - X/Y/Z wave displacement is multiplied by `S`.
  - Wave-relative orbital velocity is multiplied by `S`.
  - Base current remains intact rather than being erased near shore.
  - Normal blends smoothly toward `Vec3.UP` as `S` approaches zero.
  - Tide/base water level remains coherent.

- [ ] **3.4 Implement `ShoreAttenuatedOceanSurface`.**
  - Wrap an underlying canonical surface plus a `ShoreDistanceProvider`.
  - Keep it renderer-independent so the server and client can share the exact attenuation behavior.

- [ ] **3.5 Implement pure `ShoreDistanceGrid` logic.**
  - Input is a bounded water/land occupancy grid.
  - Output stores nearest-land horizontal distance clamped to 15.
  - Use a multi-source breadth-first or equivalent bounded distance transform, not per-cell radial scanning.
  - Keep this class free of Minecraft client classes so it is unit-testable.

- [ ] **3.6 Verify GREEN.**
  - Run new shore tests.
  - Run ocean and vessel suites.
  - Run full build.

- [ ] **3.7 Commit.**
  - Suggested commit: `Add shared shoreline attenuation`

---

## Task 4: Add Vessel-Local Server Shore Cache and Dominant Physical Surface

**Purpose:** Apply the same shore attenuation to authoritative vessel physics without building a world-scale server coastline texture.

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/minecraft/VesselShoreDistanceCache.java`
- Create if useful: `src/main/java/com/redslovesgames/newestocean/ocean/LimitedOceanSurface.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/minecraft/BoatPhysicsSupport.java`
- Create: `src/test/java/com/redslovesgames/newestocean/minecraft/VesselShoreDistanceCacheTest.java`
- Modify: vessel integration tests

- [ ] **4.1 Add RED cache tests.**
  - Repeated nearby queries reuse cached cells.
  - Moving beyond the cache window invalidates/rebuilds correctly.
  - Distances clamp at 15.
  - A narrow water body never reaches full-wave factor.
  - A point in a wide body more than 15 blocks from land reaches full-wave factor.

- [ ] **4.2 Implement vessel-local shore lookup.**
  - Query only around active vessel sample positions.
  - Use loaded world/chunk water/land state.
  - Bound the search to the 15-block rule plus a small cache margin.
  - Never force-load chunks solely for ocean physics.

- [ ] **4.3 Define the physical surface explicitly.**
  - Use only the first `OceanSpectrum.PHYSICS_COMPONENTS` dominant waves.
  - Wrap that surface with `ShoreAttenuatedOceanSurface` backed by the vessel cache.
  - Do not read client quality settings.

- [ ] **4.4 Wire `BoatPhysicsSupport` to the physical surface.**
  - All buoyancy, water velocity, surfing, planing, and launch checks use the same attenuated dominant surface.
  - Preserve existing seed/time/weather synchronization.

- [ ] **4.5 Verify GREEN.**
  - Run Minecraft-side cache tests.
  - Run all vessel physics tests.
  - Run full build.

- [ ] **4.6 Commit.**
  - Suggested commit: `Apply shoreline damping to vessel physics`

---

## Task 5: Rework Surfing, Planing, and Gliding as Full 3D Surface Interaction

**Purpose:** Deliver the user-approved middle-ground feel: strong gliding and launches with meaningful rough-sea drag and lateral wave effects.

**Files:**
- Modify: `src/main/java/com/redslovesgames/newestocean/physics/VesselWaveRidingDynamics.java`
- Modify if needed: `src/main/java/com/redslovesgames/newestocean/physics/VesselMotionController.java`
- Modify if needed: `src/main/java/com/redslovesgames/newestocean/physics/VesselReentryDynamics.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/minecraft/BoatPhysicsSupport.java`
- Modify/Create tests under: `src/test/java/com/redslovesgames/newestocean/physics/`

- [ ] **5.1 Add RED planing-normal tests.**
  - On a tilted water normal, planing support follows the local normal rather than world Y.
  - On flat water, behavior remains approximately upward.

- [ ] **5.2 Add RED lateral-wave tests.**
  - A wave with strong positive X orbital velocity can impart X force even if vessel forward is Z.
  - Reverse orbital direction reverses lateral contribution.
  - Force is capped and does not create unbounded acceleration.

- [ ] **5.3 Add RED gliding/rough-sea tests.**
  - Increasing speed and reducing wetted contact increases a bounded drag-relief fraction.
  - Cross-wave misalignment reduces planing/gliding efficiency.
  - Rough wave incidence can reduce forward momentum rather than becoming free acceleration.

- [ ] **5.4 Add RED crest-launch/no-snap tests.**
  - A fast vessel with decreasing support can transition to `LAUNCHING`/`AIRBORNE`.
  - While `AIRBORNE`, water forces do not snap the vessel to the mathematical surface.
  - Recontact still transitions through progressive wetting.

- [ ] **5.5 Refactor `VesselWaveRidingDynamics.Result`.**
  - Prefer explicit fields such as:
    - total wave-riding force
    - surfing flag
    - planing flag
    - planing support
    - surf acceleration
    - lateral acceleration
    - drag-relief fraction
    - forward speed
  - Keep units documented.

- [ ] **5.6 Implement the minimum 3D dynamics to satisfy tests.**
  - Use full water velocity, not a horizontally flattened copy for all effects.
  - Project surfing force from the moving local wave face.
  - Add bounded lateral orbital coupling.
  - Add local-normal planing support.
  - Reduce effective drag progressively with planing, never below a safe floor.
  - Keep rough-sea/cross-angle penalty explicit and bounded.

- [ ] **5.7 Integrate with `BoatPhysicsSupport`.**
  - Preserve vanilla/Small Ships/Shippy adapter ownership.
  - Do not duplicate forces in both adapter and common solver.

- [ ] **5.8 Verify GREEN.**
  - Run all physics tests.
  - Run full build.

- [ ] **5.9 Commit.**
  - Suggested commit: `Make vessel wave riding fully three dimensional`

---

## Task 6: Introduce Client Real-Water Runtime State and 24-Wave GPU Payload

**Purpose:** Create a renderer-neutral client data contract before any Vanilla/Sodium/Iris injection code.

**Files:**
- Create package: `src/main/java/com/redslovesgames/newestocean/client/water/`
- Create: `OceanWaterWaveData.java`
- Create: `OceanWaterFrameState.java`
- Create: `OceanWaterRuntime.java`
- Create: `OceanWaterInjectionState.java`
- Create: tests under `src/test/java/com/redslovesgames/newestocean/client/water/`

- [ ] **6.1 Add RED wave-packing tests.**
  - Can pack up to 24 deterministic components.
  - Preserves component order.
  - Quality-selected visible count never exceeds available components or configured cap.
  - Physical wave count remains six regardless of visual selection.

- [ ] **6.2 Define compact wave uniforms.**
  - Use two `vec4`-equivalent records per wave, for example:
    - A: direction X, direction Z, amplitude, wave number
    - B: angular frequency, phase, steepness, reserved/auxiliary value
  - Keep GLSL and Java layouts documented together.

- [ ] **6.3 Implement `OceanWaterFrameState`.**
  - Include frame/time, wave data, visual count, conditions, sea level, and active renderer path.
  - Do not include mutable Minecraft objects in the record.

- [ ] **6.4 Implement `OceanWaterInjectionState`.**
  - Track separately:
    - source patch success
    - uniform/sampler binding success
    - shadow patch success
    - active renderer path
    - fallback reason
  - Make state observable by diagnostics without hard dependency on Iris/Sodium.

- [ ] **6.5 Implement `OceanWaterRuntime`.**
  - Build current frame state from synchronized seed/time/weather and client config.
  - No rendering side effects yet.

- [ ] **6.6 Verify GREEN and full build.**

- [ ] **6.7 Commit.**
  - Suggested commit: `Add real-water client runtime state`

---

## Task 7: Build a Pure Shader Library and Idempotent Source Patcher

**Purpose:** Prove the shader math and patching contract in isolation before hooking Minecraft, Sodium, or Iris internals.

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/water/OceanShaderLibrary.java`
- Create: `src/main/java/com/redslovesgames/newestocean/client/water/OceanShaderSourcePatcher.java`
- Create: `src/main/resources/assets/newestocean/shaders/include/newestocean_water.glsl`
- Create: tests under `src/test/java/com/redslovesgames/newestocean/client/water/`

- [ ] **7.1 Add RED patcher tests with representative shader snippets.**
  - Vanilla-style water vertex source is patched once.
  - Sodium-style chunk vertex source is patched once.
  - Iris `gbuffers_water` source is patched once.
  - Shadow vertex source is patched once.
  - Running the patcher twice does not duplicate code.
  - Non-target shader source remains byte-for-byte unchanged.

- [ ] **7.2 Define an idempotence marker.**
  - Use a unique comment such as `// NEWESTOCEAN_WATER_V1`.
  - Every path must check for it before patching.

- [ ] **7.3 Implement shared GLSL Gerstner evaluation.**
  - Support up to 24 components with a runtime count.
  - Compute X/Y/Z displacement and analytical normal.
  - Apply shore factor to displacement.
  - Produce foam potential separately from base material color.
  - Avoid loops whose maximum bound is unknown to old drivers, use a fixed maximum loop with early/conditional exit if needed.

- [ ] **7.4 Add water-only classification.**
  - Do not displace arbitrary translucent terrain.
  - Pass still/flowing water atlas UV bounds or another renderer-proven water classifier through uniforms.
  - Patchers must guard displacement behind this classifier.
  - Add tests showing a non-water translucent sample is left undisplaced.

- [ ] **7.5 Define uniform names centrally.**
  - `newestocean_waveCount`
  - `newestocean_time`
  - `newestocean_waveScale`
  - `newestocean_seaLevel`
  - `newestocean_waveA0..23`
  - `newestocean_waveB0..23`
  - shore texture/origin/scale uniforms
  - water sprite bounds uniforms
  - foam/quality controls

- [ ] **7.6 Verify GREEN.**
  - Run shader patcher tests.
  - Full build must not require a live GL context.

- [ ] **7.7 Commit.**
  - Suggested commit: `Add reusable real-water shader patcher`

---

## Task 8: Build the Client Shore-Distance Field and GPU Upload Path

**Purpose:** Supply the 15-block smooth coast factor to the water shader without terrain searches per vertex/per frame.

**Files:**
- Create package: `src/main/java/com/redslovesgames/newestocean/client/shore/`
- Create: `ShoreDistanceFieldCache.java`
- Create: `ShoreDistanceTexture.java`
- Create: `WaterSpriteBounds.java`
- Create: tests under `src/test/java/com/redslovesgames/newestocean/client/shore/`

- [ ] **8.1 Add RED pure cache tests.**
  - Camera movement inside the current cache window does not rebuild everything.
  - Crossing a tile boundary refreshes only required regions where feasible.
  - Chunk invalidation marks affected cells dirty.
  - Distances are clamped to 15.

- [ ] **8.2 Implement a bounded camera-centered occupancy capture.**
  - Read only loaded chunk/world state.
  - Capture surface water vs land around the visible water region.
  - Feed the pure `ShoreDistanceGrid` from Task 3.
  - Never force chunk generation/loading.

- [ ] **8.3 Implement compact GPU representation.**
  - Prefer one byte per texel (`R8` or the closest supported equivalent) because only 0-15 distance is required.
  - Store world-space origin and texel scale.
  - Reuse texture allocation when dimensions do not change.

- [ ] **8.4 Implement `WaterSpriteBounds`.**
  - Resolve still and flowing water atlas sprite UV ranges after resource/atlas load.
  - Refresh on resource reload.
  - Keep water classification data available to all renderer paths.

- [ ] **8.5 Verify GREEN.**
  - Unit-test cache and conversion logic.
  - Full build.

- [ ] **8.6 Commit.**
  - Suggested commit: `Add cached shoreline field for water shaders`

---

## Task 9: Implement Vanilla/Fabric Real-Water Shader Injection

**Purpose:** Get one correct real-water path working without Sodium/Iris before adding compatibility layers.

**Files:**
- Create client mixin(s) under `src/main/java/com/redslovesgames/newestocean/mixin/client/`
- Modify: `src/main/resources/newestocean.mixins.json`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/NewestOceanClient.java`
- Modify: `client/water` runtime/binding classes
- Add/modify contract tests

- [ ] **9.1 Identify the exact mapped Minecraft 1.21.1 shader load and water/translucent program binding methods.**
  - Use Yarn/compiled classpath, not guessed descriptors.
  - Document the selected target descriptors in the mixin source.

- [ ] **9.2 Add a RED source/registration contract test.**
  - Vanilla injector is registered as a client mixin.
  - It calls the shared `OceanShaderSourcePatcher`, not old `OceanGpuShader` surface drawing.
  - It binds current `OceanWaterRuntime` uniforms and shore texture.

- [ ] **9.3 Implement Vanilla shader-source injection.**
  - Patch only the water-capable shader path.
  - Use water sprite classification before displacement.
  - Preserve Minecraft material/color/fog semantics.

- [ ] **9.4 Implement uniform and shore sampler binding.**
  - Bind current wave payload every relevant program use/frame.
  - Bind shore texture/origin/scale.
  - Bind water sprite bounds.
  - Update `OceanWaterInjectionState` on success/failure.

- [ ] **9.5 Keep the old overlay temporarily available only behind an explicit development switch if needed for comparison.**
  - It must not be selected by compatibility fallback.
  - Production default remains old path until Task 14 cutover, but the new Vanilla path must be independently testable.

- [ ] **9.6 Manual smoke gate.**
  - Fabric without Sodium/Iris.
  - Confirm actual water geometry moves in X/Y/Z.
  - Confirm non-water translucent blocks do not deform.
  - Confirm shore damping has no hard 15-block seam.

- [ ] **9.7 Run CI/full build and commit.**
  - Suggested commit: `Inject waves into vanilla water rendering`

---

## Task 10: Implement Sodium 0.8.12 Real-Water Injection

**Purpose:** Preserve animated real-water geometry when Sodium owns chunk shader generation/rendering.

**Files:**
- Create package/mixins under: `src/main/java/com/redslovesgames/newestocean/mixin/compat/sodium/`
- Create bridge under: `src/main/java/com/redslovesgames/newestocean/client/compat/sodium/`
- Modify: `src/main/resources/newestocean.mixins.json`
- Add Sodium compatibility tests/contracts

- [ ] **10.1 Use the exact 0.8.12 source characterization from Task 1.**
  - Target the pinned class/method that produces or owns the chunk water/translucent shader source/program.
  - Do not target class names from newer Sodium releases unless verified identical.

- [ ] **10.2 Add RED conditional/patch contract tests.**
  - Sodium mixins are skipped when Sodium is absent.
  - Sodium shader source reaches `OceanShaderSourcePatcher` exactly once.
  - Uniform binding reaches the same shared runtime contract as Vanilla.

- [ ] **10.3 Implement Sodium source injection and binding.**
  - Keep compatibility code thin.
  - Do not fork the wave math into Sodium-specific GLSL.
  - Preserve translucent non-water material behavior through water classification.

- [ ] **10.4 Update diagnostics state.**
  - Active path reports `SODIUM`.
  - Failure reason is explicit.
  - Failure falls back to ordinary Sodium water, not overlay water.

- [ ] **10.5 Manual smoke gate.**
  - Sodium 0.8.12, no Iris.
  - Verify water motion, coast damping, non-water transparency, chunk boundaries, camera movement, and resource reload.

- [ ] **10.6 Full build and commit.**
  - Suggested commit: `Add Sodium real-water compatibility`

---

## Task 11: Implement Iris Water and Shadow Shader Injection

**Purpose:** Keep X/Y/Z moving water under arbitrary Iris shaderpacks and use the same displacement for water shadow geometry.

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/compat/iris/IrisWaterShaderBridge.java`
- Create mixins under: `src/main/java/com/redslovesgames/newestocean/mixin/compat/iris/`
- Modify: `src/main/resources/newestocean.mixins.json`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/IrisCompatibilityBridge.java` only for metadata/state if needed
- Add Iris compatibility tests/contracts

- [ ] **11.1 Add RED source-patch tests for Iris stages.**
  - `gbuffers_water` vertex path receives Newest Ocean displacement.
  - Relevant water shadow vertex path receives the same large-scale displacement.
  - Fragment path can consume corrected normal/foam metadata without replacing pack lighting/color logic.
  - Non-water shader programs remain untouched.

- [ ] **11.2 Hook the pinned Iris transformation path.**
  - Prefer a narrow mixin around the exact `TransformPatcher.patchVanilla(...)` / `patchSodium(...)` result or equivalent verified location.
  - Only mutate source when the program/stage is a targeted water or matching shadow path.
  - Use the shared idempotence marker.

- [ ] **11.3 Bind Newest Ocean uniforms/sampler into Iris-created programs.**
  - Hook the verified `ProgramBuilder` or equivalent binding path from the pinned Iris version.
  - Bind wave vectors, time, shore texture, sprite bounds, and quality controls.
  - Keep every Iris-internal reference isolated in the Iris bridge package.

- [ ] **11.4 Remove old shadow-skip semantics from the new path.**
  - Shadow pass must use displaced geometry.
  - `IrisCompatibilityBridge` may still report current shadow pass for diagnostics, but must not globally suppress the new ocean geometry.

- [ ] **11.5 Implement version/capability failure behavior.**
  - If source injection or binding cannot resolve safely, set a diagnostic failure state and leave normal shaderpack water undisplaced.
  - Never reactivate `OceanWorldRenderer` overlay.

- [ ] **11.6 Manual smoke gate.**
  - Iris active, no shaderpack.
  - Iris with at least one generic shaderpack.
  - Verify water geometry, shadows, transparency, resource reload, pack switch/reload.

- [ ] **11.7 Full build and commit.**
  - Suggested commit: `Add Iris water and shadow injection`

---

## Task 12: Migrate Quality Presets, Config, and DEPTHS Tuning

**Purpose:** Make runtime quality controls describe the new real-water renderer instead of the finite overlay mesh.

**Files:**
- Modify: `src/main/java/com/redslovesgames/newestocean/client/OceanQuality.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/config/OceanClientConfig.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/config/OceanClientConfigCodec.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/config/NewestOceanConfigScreen.java`
- Modify: DEPTHS compatibility/config tests

- [ ] **12.1 Add RED config migration tests.**
  - Existing config JSON still loads.
  - Old visual-wave values `0..6` preserve meaning and do not corrupt newer ranges.
  - New visual wave cap supports up to 24.
  - DEPTHS cap supports the new range.
  - Server-authoritative physical count is not configurable from client JSON.

- [ ] **12.2 Redefine quality presets for real-water cost.**
  - Initial visible counts:
    - POTATO: 4-6
    - LOW: 6-8
    - MEDIUM: about 10
    - HIGH: about 14
    - ULTRA: 18-24
  - Keep LOW as the GTX 1050 Ti / GTX 1650 reference.
  - Quality should alter shader math/detail, not authoritative wave physics.

- [ ] **12.3 Migrate or retire overlay-only controls.**
  - `renderDistanceScale` and overlay opacity must not pretend to control a finite custom mesh after cutover.
  - Preserve config compatibility during migration, but remove/deprecate UI entries that no longer have meaningful behavior.

- [ ] **12.4 Keep DEPTHS geometry enabled.**
  - DEPTHS_LOW/MEDIUM/HIGH/ULTRA may cap visible waves, foam detail, or material modifiers.
  - None may disable X/Y/Z displacement merely because a shaderpack is active.

- [ ] **12.5 Verify GREEN and full build.**

- [ ] **12.6 Commit.**
  - Suggested commit: `Migrate quality settings to real-water renderer`

---

## Task 13: Replace Compatibility Diagnostics with Real Injection State

**Purpose:** Make failures actionable and remove old GPU-vs-CPU-overlay assumptions.

**Files:**
- Modify: `src/main/java/com/redslovesgames/newestocean/client/ShaderCompatibility.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/OceanDiagnosticsHud.java`
- Modify: diagnostics/config tests

- [ ] **13.1 Add RED diagnostics contract tests.**
  - HUD can report active renderer path: Vanilla, Sodium, Iris.
  - Reports source patch success/failure.
  - Reports uniform/sampler binding success/failure.
  - Reports shadow injection success/failure.
  - Reports visible and physical wave counts separately.
  - Reports shore-field status.
  - Reports fallback reason.

- [ ] **13.2 Redesign `ShaderCompatibility`.**
  - Remove the assumption that active Iris means CPU overlay fallback.
  - Remove new-path `skipWorldRender` shadow behavior.
  - Keep pack metadata and DEPTHS detection separate from geometry enablement.

- [ ] **13.3 Update HUD output.**
  - Keep it concise enough for real gameplay.
  - Include compatibility failure details only when diagnostics are enabled.

- [ ] **13.4 Verify GREEN and full build.**

- [ ] **13.5 Commit.**
  - Suggested commit: `Report real-water compatibility diagnostics`

---

## Task 14: Cut Over the Primary Renderer and Retire the Duplicate Ocean Surface

**Purpose:** Make real-water deformation the only primary ocean renderer and remove the architecture that caused the severe screenshots.

**Files:**
- Modify: `src/main/java/com/redslovesgames/newestocean/client/NewestOceanClient.java`
- Modify/Delete as references permit:
  - `OceanWorldRenderer.java`
  - `OceanGpuShader.java`
  - `OceanRenderState.java`
  - overlay-only LOD mesh/topology/coverage classes
  - overlay surface shader resources
  - obsolete shoreline overlay code
- Modify: wake/whitecap rendering as required
- Replace: `src/test/java/com/redslovesgames/newestocean/client/OceanVisualRegressionContractTest.java`

- [ ] **14.1 Add RED cutover contract tests before deleting anything.**
  - `NewestOceanClient` no longer registers `OceanWorldRenderer` as the primary ocean surface.
  - No production fallback calls old surface draw code.
  - Active real-water runtime is initialized instead.
  - Unsupported shader injection leaves ordinary water visible.

- [ ] **14.2 Move whitecaps into the real water material path.**
  - Use crest/steepness/curvature/weather data from shared shader evaluation.
  - Preserve user whitecap enable/intensity controls.
  - Do not draw a second full ocean sheet for foam.

- [ ] **14.3 Remove/migrate shoreline overlay visuals.**
  - Primary shore transition is now geometric attenuation from the shore field.
  - Any retained shoreline foam must be material-integrated or narrowly scoped, never another ocean plane.

- [ ] **14.4 Handle wakes safely.**
  - Existing wake simulation data may remain.
  - A temporary wake-only translucent effect is acceptable only if it does not recreate the primary ocean surface or hide geometry failures.
  - Prefer feeding wake perturbation/foam into the real water material when practical in this task.

- [ ] **14.5 Remove dead old surface code after references are zero.**
  - Search the repository for every old class/resource before deletion.
  - Do not delete shared math/data accidentally reused by the new renderer.

- [ ] **14.6 Replace old visual regression contracts.**
  - Assert no `AFTER_TRANSLUCENT` primary ocean registration.
  - Assert no duplicate-surface automatic fallback.
  - Assert real-water injector registrations exist.
  - Assert Iris shadow path is not globally skipped.

- [ ] **14.7 Full build and commit.**
  - Suggested commit: `Cut over to real-water ocean rendering`

---

## Task 15: Full Integration Matrix, Performance Acceptance, and Release Artifact

**Purpose:** Prove the rewrite in the actual renderer combinations and hardware budget that motivated it.

**Files:**
- Create: `docs/REAL_WATER_QA.md`
- Update other docs only if behavior materially changed
- No unrelated refactors

- [ ] **15.1 Run complete automated verification.**
  - `gradle clean build --stacktrace`
  - Confirm all JUnit tests pass.
  - Confirm optional compatibility code compiles against the pinned targets.
  - Confirm artifact upload workflow succeeds on the exact final head.

- [ ] **15.2 Manual renderer matrix.**
  - Vanilla/Fabric, no Sodium/Iris.
  - Sodium 0.8.12, no Iris.
  - Iris active with no shaderpack.
  - Iris with a generic shaderpack.
  - DEPTHS_LOW.
  - DEPTHS_MEDIUM.
  - DEPTHS_HIGH.
  - DEPTHS_ULTRA.

  For every case record:
  - actual X/Y/Z geometry motion
  - no duplicate cyan/black water sheet
  - no severe streaking/intersection artifacts
  - no hard 15-block shoreline seam
  - no deformation of non-water translucent materials
  - resource/shader reload stability
  - correct water shadow displacement where applicable
  - active diagnostics path and wave counts

- [ ] **15.3 Manual vessel matrix.**
  - Vanilla boat.
  - Small Ships supported vessel.
  - Shippy Ships supported vessel if its bridge is available.

  Test scenarios:
  - calm open water
  - diagonal swell
  - cross sea
  - high-speed crest sequence
  - surfing down a wave face
  - planing/gliding over several crests
  - launch and airborne travel
  - re-entry
  - near-shore damping transition
  - rain/thunder

- [ ] **15.4 Performance acceptance on LOW.**
  - Use GTX 1050 Ti / GTX 1650-class hardware or the closest available equivalent.
  - Record baseline FPS/frame time with Newest Ocean rendering disabled.
  - Record LOW FPS/frame time with real-water animation enabled.
  - Record render distance, shaderpack status, and active wave count.
  - If LOW is not acceptably usable, profile before reducing visual count blindly. Identify whether cost is wave ALU, shore texture work, shader injection/material cost, or unrelated pack overhead.

- [ ] **15.5 Write `docs/REAL_WATER_QA.md`.**
  - Include tested mod versions.
  - Include known compatibility failures/fallback behavior.
  - Include performance measurements.
  - Include screenshots or reproduction notes where useful, without claiming untested combinations work.

- [ ] **15.6 Verify final branch discipline.**
  - Final head is still `feature/ocean-core`.
  - `main` remains untouched.
  - PR #1 remains draft unless the user explicitly asks otherwise.

- [ ] **15.7 Produce and inspect the exact-head JAR.**
  - Download CI artifact from the successful exact-head workflow.
  - Inspect JAR contents for the new mixins, shader include, and absence of old primary overlay registration.
  - Provide the compiled non-sources JAR to the user for final in-pack QA.

- [ ] **15.8 Commit QA documentation.**
  - Suggested commit: `Document real-water integration QA`

---

## Definition of Done

This plan is complete only when all of these are true:

- The visible ocean is Minecraft/Sodium water geometry displaced in X, Y, and Z, not a second primary ocean mesh.
- Full wave strength occurs only at water points at least 15 blocks from land, with a smooth shore transition inside that band.
- Vanilla/Fabric, Sodium 0.8.12, Iris 1.8.14-beta.1, generic shaderpacks, and DEPTHS retain animated geometry in tested configurations.
- Iris water shadow geometry uses matching displacement where the pack renders water shadows.
- Unsupported injection fails to flat ordinary water with a diagnostic reason.
- The server evaluates only the dominant synchronized wave subset and does not depend on client graphics quality.
- Vessels respond in X/Y/Z, pitch and roll from multi-point contact, surf moving faces, plane/glide with bounded drag relief, launch from crests, remain ballistic while airborne, and re-enter progressively.
- Rough seas can still slow, shove, and destabilize vessels.
- LOW is profiled against the GTX 1050 Ti / GTX 1650-class target.
- The old duplicate `AFTER_TRANSLUCENT` ocean surface is no longer registered or reachable as an automatic fallback.
- Full automated build/tests pass on the exact delivered head.
