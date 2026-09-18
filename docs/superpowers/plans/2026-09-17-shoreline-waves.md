# Phase 14 Shoreline Waves Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bounded, cached, visual-only shoreline shoaling, directional breaking, and procedural shore foam without changing authoritative ocean or vessel physics.

**Architecture:** Terrain-aware shore metrics are computed on the client only when the snapped LOD/coverage cache is rebuilt. A pure Java breaker model converts shallow depth, shore distance, incoming-wave alignment, weather, and quality into bounded visual intensity. A dedicated sparse shoreline overlay shader reuses the packed Gerstner waves to follow the rendered ocean and create animated breaker foam, while a CPU fallback uses the same cached shore field and synchronized ocean samples.

**Tech Stack:** Java 21, Fabric 1.21.1, Yarn 1.21.1+build.3, Fabric rendering API, Minecraft core shaders GLSL 150, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-17-shoreline-waves-design.md`

## Global Constraints

- No CFD, block-by-block fluid simulation, persistent foam grid, or water-block displacement.
- No server-side shoreline state, new packets, vessel-force changes, drag changes, collision changes, or physics-quality coupling.
- Shore analysis is client-only and cached with the snapped LOD/coverage lifecycle.
- Existing water coverage refresh cadence remains 100 world ticks.
- Maximum vertical depth probe is exactly 12 blocks.
- Maximum horizontal shore search radius is quality-scaled: POTATO 4, LOW 6, MEDIUM 8, HIGH 10, ULTRA 12 blocks.
- Shader work must be visual-only and reuse the existing deterministic Gerstner components.
- CPU fallback must retain approximate shore foam if the dedicated shader is unavailable.
- Spray particles remain Phase 15.
- Shippy Ships remains out of scope.

---

### Task 1: Pure shoreline sample and breaker model

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/ShorelineSample.java`
- Create: `src/main/java/com/redslovesgames/newestocean/client/ShorelineBreakModel.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/ShorelineBreakModelTest.java`

**Interfaces:**
- Produces: `ShorelineSample(depthBlocks, distanceToShoreBlocks, shoreDirectionX, shoreDirectionZ, shoreInfluence)`.
- Produces: `ShorelineBreakModel.searchRadius(OceanQuality)`.
- Produces: `ShorelineBreakModel.shoreInfluence(depthBlocks, distanceBlocks, searchRadiusBlocks)`.
- Produces: `ShorelineBreakModel.breakerIntensity(ShorelineSample, incomingAlignment, waveEnergy, stormStrength, OceanQuality, breakup)`.
- Produces: `ShorelineBreakModel.qualityScale(OceanQuality)`.

- [ ] **Step 1: Write failing model tests**

Create tests covering exact quality radii and pure behavior:

```java
@Test
void qualitySearchRadiiMatchSpec() {
    assertEquals(4, ShorelineBreakModel.searchRadius(OceanQuality.POTATO));
    assertEquals(8, ShorelineBreakModel.searchRadius(OceanQuality.MEDIUM));
    assertEquals(12, ShorelineBreakModel.searchRadius(OceanQuality.ULTRA));
}

@Test
void shallowCloseWaterHasMoreInfluenceThanDeepFarWater() {
    double shallow = ShorelineBreakModel.shoreInfluence(2.0, 1.0, 8.0);
    double deep = ShorelineBreakModel.shoreInfluence(10.0, 7.0, 8.0);
    assertTrue(shallow > deep);
    assertTrue(shallow > 0.5);
}

@Test
void incomingWaveBreaksMoreThanParallelOrOutgoingWave() {
    ShorelineSample sample = new ShorelineSample(2.0, 1.0, 1.0, 0.0, 0.9);
    double incoming = ShorelineBreakModel.breakerIntensity(sample, 1.0, 0.8, 0.0, OceanQuality.HIGH, 1.0);
    double parallel = ShorelineBreakModel.breakerIntensity(sample, 0.0, 0.8, 0.0, OceanQuality.HIGH, 1.0);
    double outgoing = ShorelineBreakModel.breakerIntensity(sample, -1.0, 0.8, 0.0, OceanQuality.HIGH, 1.0);
    assertTrue(incoming > parallel);
    assertEquals(0.0, outgoing, 1.0e-9);
}

@Test
void breakerIntensityAlwaysClampsToUnitInterval() {
    ShorelineSample sample = new ShorelineSample(1.0, 0.25, 1.0, 0.0, 1.0);
    double value = ShorelineBreakModel.breakerIntensity(sample, 5.0, 5.0, 5.0, OceanQuality.ULTRA, 5.0);
    assertTrue(value >= 0.0 && value <= 1.0);
}
```

Also test that non-finite values and non-normalizable nonzero shore direction inputs are rejected.

- [ ] **Step 2: Run focused tests and confirm red**

Run: `gradle test --tests com.redslovesgames.newestocean.client.ShorelineBreakModelTest`

Expected: compilation failure because `ShorelineSample` and `ShorelineBreakModel` do not exist.

- [ ] **Step 3: Implement the minimal pure model**

Use exact quality search radii:

```java
POTATO -> 4
LOW -> 6
MEDIUM -> 8
HIGH -> 10
ULTRA -> 12
```

Use a bounded influence model:

```java
double shallow = clamp01(1.0 - (depthBlocks - 1.0) / 9.0);
double proximity = clamp01(1.0 - distanceBlocks / searchRadiusBlocks);
return clamp01(shallow * proximity);
```

`ShorelineSample` normalizes a nonzero shore direction, permits `(0,0)` only when influence is zero, and clamps/rejects values so all exposed fields remain finite.

Use breaker intensity:

```java
double incoming = clamp01(incomingAlignment);
double energy = clamp01(waveEnergy);
double storm = clamp01(stormStrength);
double detail = qualityScale(quality);
double breakupScale = clamp01(breakup);
double formation = sample.shoreInfluence() * incoming * (0.35 + 0.65 * energy);
return clamp01(formation * (0.75 + 0.45 * storm) * detail * breakupScale);
```

Quality scales are POTATO 0.45, LOW 0.60, MEDIUM 0.78, HIGH 0.90, ULTRA 1.00.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `Add shoreline breaker model`

---

### Task 2: Bounded shoreline analyzer and topology-aligned field

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/ShorelineAnalyzer.java`
- Create: `src/main/java/com/redslovesgames/newestocean/client/ShorelineField.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/OceanLodCoverageMask.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/ShorelineAnalyzerTest.java`

**Interfaces:**
- Consumes: `OceanLodPlanner.Plan`, `OceanLodTopology`, `OceanLodCoverageMask`, `OceanQuality`.
- Produces: package-visible `OceanLodCoverageMask.isWaterCell(int cell)` for topology-aligned analysis.
- Produces: `ShorelineAnalyzer.Probe` with `boolean isWater(int x, int y, int z)`.
- Produces: `ShorelineAnalyzer.build(plan, topology, coverage, seaLevel, quality, probe)` returning `ShorelineField`.
- Produces: `ShorelineField.sample(int cell)`, `cellCount()`, `hasInfluence(int cell)`, and `influencedCellCount()`.

- [ ] **Step 1: Write failing analyzer tests with a synthetic probe**

Use an in-memory probe where water occupies `x < 4` at sea level and extends downward three blocks. Assert:

```java
@Test
void analyzerFindsNormalizedDirectionTowardNearestLand() {
    ShorelineField field = buildSyntheticField(OceanQuality.MEDIUM);
    ShorelineSample sample = nearestKnownWetCell(field);
    assertTrue(sample.shoreInfluence() > 0.0);
    assertEquals(1.0, Math.hypot(sample.shoreDirectionX(), sample.shoreDirectionZ()), 1.0e-9);
    assertTrue(sample.shoreDirectionX() > 0.0);
}
```

Add tests proving:
- no land within the quality radius gives zero influence;
- the analyzer never calls the horizontal probe beyond `searchRadius(quality)` from a tested cell;
- depth probing stops at 12 downward checks even if the synthetic column stays water forever;
- identical probe data yields identical `ShorelineSample` values;
- dry coverage cells always receive zero influence.

- [ ] **Step 2: Run focused tests and confirm red**

Run: `gradle test --tests com.redslovesgames.newestocean.client.ShorelineAnalyzerTest`

Expected: compilation failure because the analyzer/field classes do not exist and coverage has no cell accessor.

- [ ] **Step 3: Implement the field and bounded analyzer**

`ShorelineField` stores exactly one immutable `ShorelineSample` per topology cell. Use a shared zero sample for dry/deep/no-shore cells.

For each wet cell:
1. Floor the topology cell-center world X/Z.
2. Probe depth from `seaLevel - 1` downward, maximum 12 positions, stopping on the first non-water block.
3. Search integer offsets inside the quality radius. Reject offsets whose squared Euclidean distance exceeds `radius * radius`.
4. Among positions that are not water at `seaLevel - 1`, select the nearest; break equal-distance ties deterministically by smaller `dx`, then smaller `dz`.
5. Normalize `(landX - waterX, landZ - waterZ)` as the shore direction.
6. Compute `shoreInfluence` through `ShorelineBreakModel.shoreInfluence(...)`.

Do not scan chunks or query cells outside the fixed radius/depth caps.

- [ ] **Step 4: Run analyzer and coverage tests**

Run:
`gradle test --tests com.redslovesgames.newestocean.client.ShorelineAnalyzerTest --tests com.redslovesgames.newestocean.client.OceanLodArchitectureTest`

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `Add cached shoreline field analysis`

---

### Task 3: Shoreline overlay geometry and GPU shader contract

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/ShorelineGpuShader.java`
- Create: `src/main/java/com/redslovesgames/newestocean/client/ShorelineRenderer.java`
- Create: `src/main/resources/assets/newestocean/shaders/core/shoreline_break.json`
- Create: `src/main/resources/assets/newestocean/shaders/core/shoreline_break.vsh`
- Create: `src/main/resources/assets/newestocean/shaders/core/shoreline_break.fsh`
- Test: `src/test/java/com/redslovesgames/newestocean/client/ShorelineShaderContractTest.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/ShorelineRendererContractTest.java`

**Interfaces:**
- Produces: `ShorelineGpuShader.register()`, `available()`, and `apply(ProceduralOcean ocean, int visualWaveComponents, double timeSeconds, OceanConditions conditions, OceanQuality quality, double rain, double thunder, double cameraX, double cameraY, double cameraZ)`.
- Produces: `ShorelineRenderer.renderGpu(...)` and `ShorelineRenderer.renderCpu(...)` consuming a `ShorelineField`.
- Uses `VertexFormats.POSITION_COLOR` where vertex `Color` encodes `(shoreDirX mapped 0..1, shoreDirZ mapped 0..1, shoreInfluence, shallowFactor)`.

- [ ] **Step 1: Write failing shader/resource contract tests**

Assert all three `shoreline_break` resources exist and contain:

```java
assertTrue(vertex.contains("in vec4 Color"));
assertTrue(vertex.contains("ShorelineWaveA0"));
assertTrue(vertex.contains("shoreDirection"));
assertTrue(vertex.contains("incoming"));
assertTrue(vertex.contains("shoal"));
assertTrue(fragment.contains("shoreBreaker"));
assertTrue(fragment.contains("discard"));
assertTrue(json.contains("\"Color\""));
```

Renderer contract assertions must verify:

```java
assertTrue(renderer.contains("ShorelineField"));
assertTrue(renderer.contains("ShorelineGpuShader.available"));
assertTrue(renderer.contains("RenderSystem.depthMask(false)"));
assertTrue(renderer.contains("OceanRenderCoordinates.relative"));
assertTrue(renderer.contains("NewestOcean.clientOcean().sample"));
```

- [ ] **Step 2: Run tests and confirm red**

Run:
`gradle test --tests com.redslovesgames.newestocean.client.ShorelineShaderContractTest --tests com.redslovesgames.newestocean.client.ShorelineRendererContractTest`

Expected: failure because the renderer/shader resources do not exist.

- [ ] **Step 3: Implement the dedicated shoreline shader**

Register `newestocean:shoreline_break` with `VertexFormats.POSITION_COLOR`.

Upload the same six packed waves as `OceanGpuShader` using uniforms named `ShorelineWaveA0..5`, `ShorelineWaveB0..5`, plus:

```text
ShorelineTime
ShorelineWaveScale
ShorelineWaterHeight
ShorelineCameraXZ
ShorelineWaveCount
ShorelineStormStrength
ShorelineQuality
```

The vertex shader must:
- reconstruct world X/Z from camera-relative `Position`;
- decode normalized shore direction from `Color.rg`;
- use `Color.b` as shore influence and `Color.a` as shallow factor;
- evaluate the same Gerstner phase/displacement as the ocean shader;
- accumulate amplitude/steepness-weighted `max(0, dot(waveDirection, shoreDirection))` into an `incoming` term;
- add only a small visual crest lift, at most roughly `15% * shoreInfluence * shallowFactor` of positive crest amplitude;
- output `shoreBreaker` and world X/Z to the fragment shader.

The fragment shader must use procedural world-space breakup plus a shore-directed moving wash band. It should `discard` fragments below a very small breaker alpha threshold and output pale foam with alpha clamped below 0.80.

- [ ] **Step 4: Implement sparse shoreline geometry and CPU fallback**

For each influenced wet topology cell, submit one quad from its four topology vertices. GPU mode submits camera-relative base X/Z at Y=0 with encoded shoreline color data; the shader performs wave displacement.

CPU fallback samples `NewestOcean.clientOcean().sample(...)` at each corner using the frame time/conditions/visual wave budget. Use `ShorelineBreakModel.breakerIntensity(...)` with a CPU aggregate incoming alignment computed from active `ProceduralOcean.components()` weighted by `amplitude * steepness`. Draw only quads with nontrivial alpha using `POSITION_COLOR`, blending, depth test, and `depthMask(false)`.

- [ ] **Step 5: Run focused and full tests**

Run:
`gradle test --tests com.redslovesgames.newestocean.client.ShorelineShaderContractTest --tests com.redslovesgames.newestocean.client.ShorelineRendererContractTest`

Then: `gradle test`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `Render shader-backed shoreline breakers`

---

### Task 4: Renderer cache integration and lifecycle invalidation

**Files:**
- Modify: `src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/NewestOceanClient.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/ShorelineIntegrationContractTest.java`

**Interfaces:**
- Consumes: `ShorelineAnalyzer.build(...)`, `ShorelineField`, `ShorelineRenderer`.
- Produces: shoreline cache lifetime bound to the same dimension/quality/snapped-origin/stale-refresh lifecycle as water coverage.

- [ ] **Step 1: Write failing integration contract test**

Read `OceanWorldRenderer.java` and `NewestOceanClient.java` and assert:

```java
assertTrue(renderer.contains("ShorelineAnalyzer.build"));
assertTrue(renderer.contains("ShorelineRenderer.render"));
assertTrue(renderer.contains("shoreline = null"));
assertTrue(renderer.contains("coverageBuiltAtTick"));
assertTrue(client.contains("ShorelineGpuShader.register"));
```

Also assert shoreline rendering occurs after the main ocean draw and before/alongside vessel wake rendering, and renderer reset invalidates shoreline state.

- [ ] **Step 2: Confirm red**

Run: `gradle test --tests com.redslovesgames.newestocean.client.ShorelineIntegrationContractTest`

Expected: FAIL because the main renderer/client lifecycle does not yet reference the Phase 14 subsystem.

- [ ] **Step 3: Integrate cache construction**

Add cached fields to `OceanWorldRenderer`:

```java
private static ShorelineField shoreline;
```

Do not create a second independent refresh clock. When `coverageFor(...)` rebuilds coverage because origin/quality/dimension changed or the 100-tick cache became stale, build a new shoreline field from that exact coverage/topology/plan before returning. `resetCoverage()` also sets `shoreline = null`.

The Minecraft probe implementation uses `client.world.getFluidState(BlockPos)` and only evaluates positions requested by `ShorelineAnalyzer`.

- [ ] **Step 4: Integrate rendering and shader registration**

After the main ocean surface draw, invoke:

```java
ShorelineRenderer.render(context, camera, frame, topology, shoreline);
```

Then render vessel wakes. Register `ShorelineGpuShader.register()` in `NewestOceanClient.onInitializeClient()` beside the ocean and wake shader registrations.

If no influenced shoreline cells exist, the renderer returns immediately.

- [ ] **Step 5: Run integration and full tests**

Run:
`gradle test --tests com.redslovesgames.newestocean.client.ShorelineIntegrationContractTest`

Then: `gradle test`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `Integrate cached shoreline breaking`

---

### Task 5: Documentation, README, and exact-head CI

**Files:**
- Create: `docs/PHASE_14_SHORELINE_WAVES.md`
- Modify: `README.md`

**Interfaces:**
- Documents the implemented cache/search/depth limits, shader path, CPU fallback, quality scaling, and physics isolation.

- [ ] **Step 1: Document exact implemented behavior**

The Phase 14 document must explicitly list:
- search radii 4/6/8/10/12 blocks;
- 12-block depth cap;
- 100-tick stale refresh inherited from coverage;
- incoming-wave directional breaker model;
- dedicated GPU shoreline overlay and Gerstner reuse;
- CPU fallback;
- no new packets/server work/physics changes;
- manual OpenGL tuning still pending.

- [ ] **Step 2: Update README**

Add Phase 14 to completed phases and renderer flow after vessel wakes/whitecaps. State that shoreline breaking is visual-only and cache-driven.

- [ ] **Step 3: Run the full test suite on the documentation head**

Run: `gradle test`

Expected: PASS.

- [ ] **Step 4: Verify exact-head GitHub Actions**

Verify the workflow run whose `head_sha` equals the final `feature/ocean-core` branch SHA completes with:
- `Build and test` = success;
- `Upload mod jar` = success;
- overall workflow conclusion = success.

- [ ] **Step 5: Acceptance check**

Mark Phase 14 at 100% only after the current branch tip itself is green. Keep `main` unchanged and preserve the existing draft PR workflow.
