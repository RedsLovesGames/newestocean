# Phase 16 Shader Compatibility and DEPTHS_ULTRA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add automatic Iris shaderpack compatibility with a DEPTHS_ULTRA-specific low-overhead fallback profile while preserving the existing fast custom-shader path for Sodium-only and no-shader configurations.

**Architecture:** A centralized `ShaderCompatibility` snapshot decides whether Newest Ocean custom shaders are safe for the current frame. A reflective `IrisCompatibilityBridge` uses stable Iris public API methods for active/shadow state, optional best-effort pack-name reflection for DEPTHS detection, and fails safe to CPU rendering when Iris is present but API access fails. Ocean, shoreline, and wake renderers consume the same snapshot so toggling shaderpacks changes behavior immediately without restart.

**Tech Stack:** Java 21, Fabric 1.21.1, Yarn 1.21.1+build.3, Fabric Loader 0.16.14, Fabric API 0.116.13+1.21.1, Minecraft core shaders, JUnit 5. No hard Iris or Sodium dependency.

**Spec:** `docs/superpowers/specs/2026-09-17-shader-compatibility-depths-ultra-design.md`

## Global Constraints

- Never modify `main`; work only on `feature/ocean-core`.
- Iris and Sodium remain optional and must not be added to `depends` or Gradle dependencies.
- No Iris class may appear in public signatures, field types, or eager static initialization.
- `isShaderPackInUse()` and `isRenderingShadowPass()` are the authoritative compatibility signals when Iris API reflection succeeds.
- DEPTHS pack-name recognition is optional optimization only; failure falls back to `IRIS_GENERIC`.
- Iris present plus active-state bridge failure must disable Newest Ocean custom shaders for safety.
- DEPTHS compatibility caps visual CPU wave evaluation at 4, but physical/server ocean remains all 6 components.
- DEPTHS tuning constants: ocean base alpha 0.58, whitecap multiplier 0.65, wake multiplier 0.90, shoreline multiplier 0.90.
- Preserve current LOD topology, render radius, shoreline cache lifecycle, networking, and vessel physics.
- Skip Newest Ocean world rendering during an Iris shadow pass.
- Existing custom shader paths remain unchanged and available when compatibility mode is `NORMAL_GPU`.

---

### Task 1: Central compatibility model and reflective Iris bridge

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/ShaderCompatibility.java`
- Create: `src/main/java/com/redslovesgames/newestocean/client/IrisCompatibilityBridge.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/ShaderCompatibilityTest.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/IrisCompatibilityBridgeContractTest.java`

**Interfaces:**
- `ShaderCompatibility.Mode { NORMAL_GPU, IRIS_GENERIC, IRIS_DEPTHS_ULTRA }`
- `ShaderCompatibility.IrisState(boolean irisPresent, boolean bridgeResolved, boolean shaderPackInUse, boolean shadowPass, String shaderPackName)`
- `ShaderCompatibility.Snapshot snapshot(IrisState state)`
- `ShaderCompatibility.Snapshot current()` delegates to `IrisCompatibilityBridge.currentState()`.
- Snapshot methods: `mode()`, `allowCustomShaders()`, `skipWorldRender()`, `visualWaveComponents(int requested)`, `oceanBaseAlpha()`, `whitecapMultiplier()`, `wakeMultiplier()`, `shorelineMultiplier()`.
- `IrisCompatibilityBridge.currentState()` returns only Newest Ocean-owned value types.

- [ ] **Step 1: Write failing pure compatibility tests**

Create tests for exact mode decisions and constants:

```java
@Test
void noIrisUsesNormalGpu() {
    var snapshot = ShaderCompatibility.snapshot(
        new ShaderCompatibility.IrisState(false, true, false, false, null)
    );
    assertEquals(ShaderCompatibility.Mode.NORMAL_GPU, snapshot.mode());
    assertTrue(snapshot.allowCustomShaders());
}

@Test
void activeGenericIrisPackForcesCpu() {
    var snapshot = ShaderCompatibility.snapshot(
        new ShaderCompatibility.IrisState(true, true, true, false, "ComplementaryReimagined_r5.2.zip")
    );
    assertEquals(ShaderCompatibility.Mode.IRIS_GENERIC, snapshot.mode());
    assertFalse(snapshot.allowCustomShaders());
}

@Test
void depthsNamesNormalizeToDepthsProfile() {
    for (String name : List.of("DEPTHS_ULTRA.zip", "DEPTHS ULTRA", "depths-ultra", "depths_ultra")) {
        var snapshot = ShaderCompatibility.snapshot(
            new ShaderCompatibility.IrisState(true, true, true, false, name)
        );
        assertEquals(ShaderCompatibility.Mode.IRIS_DEPTHS_ULTRA, snapshot.mode());
        assertEquals(4, snapshot.visualWaveComponents(6));
        assertEquals(0.58, snapshot.oceanBaseAlpha(), 1.0e-9);
        assertEquals(0.65, snapshot.whitecapMultiplier(), 1.0e-9);
        assertEquals(0.90, snapshot.wakeMultiplier(), 1.0e-9);
        assertEquals(0.90, snapshot.shorelineMultiplier(), 1.0e-9);
    }
}

@Test
void bridgeFailureFailsSafeToCpu() {
    var snapshot = ShaderCompatibility.snapshot(
        new ShaderCompatibility.IrisState(true, false, false, false, null)
    );
    assertEquals(ShaderCompatibility.Mode.IRIS_GENERIC, snapshot.mode());
    assertFalse(snapshot.allowCustomShaders());
}

@Test
void activeShadowPassSkipsWorldRender() {
    var snapshot = ShaderCompatibility.snapshot(
        new ShaderCompatibility.IrisState(true, true, true, true, "DEPTHS_ULTRA.zip")
    );
    assertTrue(snapshot.skipWorldRender());
}
```

Also test shaders-disabled Iris returns `NORMAL_GPU`, normal mode preserves requested wave counts, and DEPTHS never raises a requested count below 4.

- [ ] **Step 2: Run the focused test and confirm RED**

Run: `gradle test --tests com.redslovesgames.newestocean.client.ShaderCompatibilityTest`

Expected: compile failure because `ShaderCompatibility` does not exist.

- [ ] **Step 3: Implement `ShaderCompatibility`**

Use normalization that lowercases with `Locale.ROOT`, strips a trailing `.zip`, and removes spaces, `_`, and `-`; recognize exactly normalized `depthsultra` or strings containing `depthsultra`.

Mode rules:

```java
if (!state.irisPresent()) NORMAL_GPU;
else if (!state.bridgeResolved()) IRIS_GENERIC;
else if (!state.shaderPackInUse()) NORMAL_GPU;
else if (isDepthsUltra(state.shaderPackName())) IRIS_DEPTHS_ULTRA;
else IRIS_GENERIC;
```

`skipWorldRender()` is true only when Iris is present, bridge resolved, shaderpack in use, and shadow pass true.

- [ ] **Step 4: Implement the reflection bridge**

Use `FabricLoader.getInstance().isModLoaded("iris")` first. Lazily resolve and cache these reflective handles once:

```text
net.irisshaders.iris.api.v0.IrisApi#getInstance
IrisApi#isShaderPackInUse
IrisApi#isRenderingShadowPass
```

For pack name, try in order:

```text
IrisApi#getConfig + getShaderPackName if method exists
net.irisshaders.iris.Iris#getCurrentPackName if method exists
```

Accept pack-name return values as `String`, `Optional<?>`, or null. Any pack-name failure leaves the name null without marking active-state bridge failure. Any failure resolving/invoking `getInstance`, `isShaderPackInUse`, or `isRenderingShadowPass` returns `irisPresent=true, bridgeResolved=false` and logs once with `NewestOcean.LOGGER.warn(...)`.

- [ ] **Step 5: Add reflection contract tests**

`IrisCompatibilityBridgeContractTest` reads the source and asserts:

```java
assertTrue(source.contains("isModLoaded(\"iris\")"));
assertTrue(source.contains("net.irisshaders.iris.api.v0.IrisApi"));
assertTrue(source.contains("isShaderPackInUse"));
assertTrue(source.contains("isRenderingShadowPass"));
assertTrue(source.contains("getCurrentPackName"));
assertFalse(source.contains("import net.irisshaders.iris"));
```

- [ ] **Step 6: Run focused tests and full test suite**

Run: `gradle test --tests com.redslovesgames.newestocean.client.ShaderCompatibilityTest --tests com.redslovesgames.newestocean.client.IrisCompatibilityBridgeContractTest`

Expected: PASS.

- [ ] **Step 7: Commit**

Commit message: `Add centralized Iris shader compatibility`

---

### Task 2: DEPTHS CPU wave budget and ocean renderer integration

**Files:**
- Modify: `src/main/java/com/redslovesgames/newestocean/client/OceanLodMeshGenerator.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/OceanLodMeshGeneratorTest.java`
- Create: `src/test/java/com/redslovesgames/newestocean/client/ShaderCompatibilityRendererContractTest.java`

**Interfaces:**
- Add overload `OceanLodMeshGenerator.generate(..., OceanConditions conditions, int visualWaveComponents)`.
- Existing six-argument `generate(...)` delegates using `plan.visualWaveComponents()` so prior callers/tests retain behavior.
- `OceanWorldRenderer` takes one `ShaderCompatibility.Snapshot` at the beginning of each world render and passes the same snapshot to later passes.

- [ ] **Step 1: Write failing mesh wave-limit test**

Compare a HIGH plan generated with explicit limit 4 against direct `ocean.sample(..., 4)` at a known topology vertex and prove it differs from six-component output at at least one tested vertex.

- [ ] **Step 2: Write failing renderer contract test**

Read `OceanWorldRenderer.java` and require:

```java
assertTrue(source.contains("ShaderCompatibility.current()"));
assertTrue(source.contains("compatibility.skipWorldRender()"));
assertTrue(source.contains("compatibility.allowCustomShaders()"));
assertTrue(source.contains("compatibility.visualWaveComponents"));
assertTrue(source.contains("compatibility.oceanBaseAlpha()"));
assertTrue(source.contains("compatibility.whitecapMultiplier()"));
```

- [ ] **Step 3: Run tests and confirm RED**

Expected: new overload/compatibility calls absent.

- [ ] **Step 4: Implement explicit mesh component limit**

Validate `visualWaveComponents >= 0`, clamp via the ocean sampler naturally to available components, and use the passed value instead of `plan.visualWaveComponents()` in the new overload.

- [ ] **Step 5: Integrate compatibility into `OceanWorldRenderer`**

At render entry after synchronization/world/null checks:

```java
ShaderCompatibility.Snapshot compatibility = ShaderCompatibility.current();
if (compatibility.skipWorldRender()) {
    return;
}
```

GPU route requires both `compatibility.allowCustomShaders()` and `OceanGpuShader.available()`.

CPU route uses:

```java
int visualWaves = compatibility.visualWaveComponents(frame.plan().visualWaveComponents());
OceanLodMeshGenerator.generate(..., frame.conditions(), visualWaves);
```

CPU whitecap result becomes:

```java
float foam = (float) (OceanWhitecapModel.intensity(...) * compatibility.whitecapMultiplier());
```

CPU alpha becomes:

```java
float baseAlpha = (float) compatibility.oceanBaseAlpha();
float alpha = baseAlpha + 0.16F * foam;
```

Pass the same snapshot to shoreline and wake renderers.

- [ ] **Step 6: Run focused and full tests**

Expected: PASS.

- [ ] **Step 7: Commit**

Commit message: `Route ocean rendering through shader compatibility`

---

### Task 3: Wake and shoreline compatibility fallbacks

**Files:**
- Modify: `src/main/java/com/redslovesgames/newestocean/client/VesselWakeRenderer.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/ShorelineRenderer.java`
- Modify tests: `VesselWakeRendererContractTest.java`, `ShorelineRendererContractTest.java`
- Create: `src/test/java/com/redslovesgames/newestocean/client/ShaderCompatibilityOverlayContractTest.java`

**Interfaces:**
- `VesselWakeRenderer.render(..., OceanQuality quality, ShaderCompatibility.Snapshot compatibility)`.
- `ShorelineRenderer.render(..., float rainGradient, float thunderGradient, ShaderCompatibility.Snapshot compatibility)`.
- CPU sampling in both uses `compatibility.visualWaveComponents(frame.plan().visualWaveComponents())`.

- [ ] **Step 1: Add failing source contract tests**

Require wake GPU choice to include `compatibility.allowCustomShaders() && VesselWakeShader.available()` and shoreline GPU choice to include `compatibility.allowCustomShaders() && ShorelineGpuShader.available()`.

Require wake CPU alpha to reference `compatibility.wakeMultiplier()` and shoreline CPU intensity/alpha to reference `compatibility.shorelineMultiplier()`.

- [ ] **Step 2: Run contract tests and confirm RED**

Expected: FAIL because existing renderers choose only shader availability.

- [ ] **Step 3: Implement wake fallback routing**

Use the compatibility visual wave count for CPU ocean samples and multiply effective sample strength by `compatibility.wakeMultiplier()` before final alpha calculation. Normal/generic mode multiplier remains 1.0.

- [ ] **Step 4: Implement shoreline fallback routing**

Use compatibility visual wave count in both `breakInputs(...)` and CPU ocean samples. Multiply breaker strength by `compatibility.shorelineMultiplier()` before threshold/alpha calculation.

Do not change GPU shader uniforms or resources.

- [ ] **Step 5: Run overlay contracts and full suite**

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `Add Iris-safe wake and shoreline fallbacks`

---

### Task 4: Render-state hardening and isolation checks

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/OceanRenderState.java`
- Modify: `OceanWorldRenderer.java`
- Modify: `VesselWakeRenderer.java`
- Modify: `ShorelineRenderer.java`
- Create: `src/test/java/com/redslovesgames/newestocean/client/OceanRenderStateContractTest.java`
- Create: `src/test/java/com/redslovesgames/newestocean/client/ShaderCompatibilityIsolationTest.java`

**Interfaces:**
- `OceanRenderState.drawSurface(BufferBuilder builder)` handles blend/depth-write state for one-sided ocean surface.
- `OceanRenderState.drawTwoSided(BufferBuilder builder)` additionally disables/enables culling for wake/shore overlays.

- [ ] **Step 1: Write failing render-state contract test**

Require helper source to include a `try/finally` with `depthMask(false)` followed by restoration to true, blend disable in finally, and cull restoration in the two-sided path.

- [ ] **Step 2: Write physics-isolation test**

Search all Java files under `src/main/java/com/redslovesgames/newestocean` outside `/client/` and assert none contain `ShaderCompatibility` or `IrisCompatibilityBridge`.

- [ ] **Step 3: Run tests and confirm RED for state helper**

- [ ] **Step 4: Move duplicated draw-state code into helper**

Keep `RenderSystem.setShader(...)` at the caller immediately before CPU draw; the helper owns only the state it explicitly mutates. Do not attempt unsupported GL state introspection.

- [ ] **Step 5: Run full suite**

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `Harden ocean render state restoration`

---

### Task 5: Phase 16 documentation and exact-head verification

**Files:**
- Create: `docs/PHASE_16_SHADER_COMPATIBILITY.md`
- Modify: `README.md`

- [ ] **Step 1: Document runtime modes**

Document `NORMAL_GPU`, `IRIS_GENERIC`, and `IRIS_DEPTHS_ULTRA`, including the exact 4-wave cap and 0.58/0.65/0.90/0.90 DEPTHS constants, shadow-pass skip, dynamic switching, no hard Iris dependency, and no physics/networking changes.

- [ ] **Step 2: Document DEPTHS_ULTRA rationale**

Record that the supplied pack already owns water reflections, caustics, foam, vertex waving, Distant Horizons water, colored lighting, and shadow-water behavior, so compatibility mode intentionally avoids running Newest Ocean custom shaders simultaneously.

- [ ] **Step 3: Update README**

Add Phase 16 to Current implementation and update the architecture section to show centralized shader compatibility routing. Mark Phase 15 spray/impacts optional rather than next.

- [ ] **Step 4: Run full verification**

Run: `gradle build --stacktrace`

Expected: all tests pass and mod/remapped JARs build.

- [ ] **Step 5: Verify GitHub Actions on exact final branch head**

Require the run whose `head_sha` exactly equals the current `feature/ocean-core` SHA to complete `success`, with Build and test + Upload mod jar both successful and `newestocean-dev` artifact present.

- [ ] **Step 6: Commit docs if needed and re-run exact-head verification**

Final commit message: `Document Phase 16 shader compatibility`
