# Vessel Wakes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bounded, client-only, wave-following vessel wakes for vanilla boats and Small Ships without adding wake networking or changing authoritative physics.

**Architecture:** Pure Java wake model classes own descriptor strength, history acceptance/expiry, quality budgets, and deterministic wake geometry. A thin client tracker observes synchronized `BoatEntity` transforms and produces descriptors. A separate wake renderer consumes those descriptors after the ocean pass, samples the synchronized ocean for Y placement, and emits a small number of translucent quads.

**Tech Stack:** Java 21, Fabric 1.21.1, Yarn 1.21.1+build.3, Fabric rendering/client lifecycle APIs, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-17-vessel-wakes-design.md`

## Global Constraints

- Client-only visual system. No new wake packets or server-side wake simulation.
- Vanilla boats and Small Ships must share the same path through `BoatEntity` behavior and client transforms.
- Maximum 12 accepted wake samples per vessel.
- Minimum sample spacing: 0.75 blocks.
- Minimum horizontal wake speed: 0.12 blocks/tick.
- Wake lifetime: 4.0 seconds.
- Quality budgets: POTATO 4 vessels / 48 blocks; LOW 8 / 64; MEDIUM 16 / 96; HIGH 24 / 128; ULTRA 32 / 160.
- Wake geometry must be finite, camera-relative at submission, depth-tested, blended, and visual-only.
- No particle spray, shoreline breaking, gameplay drag, propulsion changes, damage, or persistent foam simulation in Phase 13.

---

### Task 1: Pure wake descriptor and history model

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/VesselWakeDescriptor.java`
- Create: `src/main/java/com/redslovesgames/newestocean/client/VesselWakeHistory.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/VesselWakeHistoryTest.java`

**Interfaces:**
- Produces: `VesselWakeDescriptor` with `x`, `z`, normalized `directionX`, normalized `directionZ`, `speedBlocksPerTick`, `beamBlocks`, `ageSeconds`, `strength`.
- Produces: `VesselWakeHistory.record(...)`, `VesselWakeHistory.snapshot(double nowSeconds)`, `VesselWakeHistory.clear()`.

- [ ] **Step 1: Write failing history tests**

```java
@Test
void stationaryVesselDoesNotCreateWake() {
    VesselWakeHistory history = new VesselWakeHistory();
    assertFalse(history.record(0.0, 0.0, 1.0, 0.0, 0.10, 1.4, 0.0));
    assertTrue(history.snapshot(0.0).isEmpty());
}

@Test
void samplesRequireMovementAndExpire() {
    VesselWakeHistory history = new VesselWakeHistory();
    assertTrue(history.record(0.0, 0.0, 1.0, 0.0, 0.30, 1.4, 0.0));
    assertFalse(history.record(0.4, 0.0, 1.0, 0.0, 0.30, 1.4, 0.1));
    assertTrue(history.record(0.8, 0.0, 1.0, 0.0, 0.30, 1.4, 0.2));
    assertEquals(2, history.snapshot(0.2).size());
    assertTrue(history.snapshot(4.21).isEmpty());
}

@Test
void historyNeverExceedsTwelveSamples() {
    VesselWakeHistory history = new VesselWakeHistory();
    for (int i = 0; i < 20; i++) {
        history.record(i, 0.0, 1.0, 0.0, 0.35, 1.4, i * 0.1);
    }
    assertEquals(12, history.snapshot(2.0).size());
}
```

- [ ] **Step 2: Run tests and verify red state**

Run: `gradle test --tests com.redslovesgames.newestocean.client.VesselWakeHistoryTest`

Expected: compilation failure because the wake model classes do not exist.

- [ ] **Step 3: Implement descriptor validation and strength model**

Use constants in `VesselWakeHistory`:

```java
static final int MAX_SAMPLES = 12;
static final double MIN_SAMPLE_DISTANCE = 0.75;
static final double MIN_SPEED = 0.12;
static final double LIFETIME_SECONDS = 4.0;
```

Normalize horizontal direction on acceptance. Reject non-finite values and near-zero direction magnitude. Initial strength uses a clamped smooth response:

```java
double speedResponse = clamp01((speedBlocksPerTick - MIN_SPEED) / 0.55);
double beamResponse = 0.75 + 0.25 * clamp01((beamBlocks - 1.0) / 5.0);
double initialStrength = clamp01(speedResponse * beamResponse);
```

`snapshot(nowSeconds)` removes expired entries and returns new descriptors whose age is `nowSeconds - createdAtSeconds` and whose strength is `initialStrength * clamp01(1.0 - age / LIFETIME_SECONDS)`.

- [ ] **Step 4: Run the focused tests**

Run: `gradle test --tests com.redslovesgames.newestocean.client.VesselWakeHistoryTest`

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `Add bounded vessel wake history model`

---

### Task 2: Quality budgets and deterministic wake geometry

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/VesselWakeQuality.java`
- Create: `src/main/java/com/redslovesgames/newestocean/client/VesselWakeGeometry.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/VesselWakeGeometryTest.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/VesselWakeQualityTest.java`

**Interfaces:**
- Produces: `VesselWakeQuality.budget(OceanQuality)` returning `Budget(maxVessels, trackingRadiusBlocks)`.
- Produces: `VesselWakeGeometry.segment(VesselWakeDescriptor newer, VesselWakeDescriptor older)` returning finite left-arm, right-arm, and center-strip quad data.

- [ ] **Step 1: Write failing quality and geometry tests**

```java
@Test
void qualityBudgetsMatchSpec() {
    assertEquals(new VesselWakeQuality.Budget(4, 48.0), VesselWakeQuality.budget(OceanQuality.POTATO));
    assertEquals(new VesselWakeQuality.Budget(16, 96.0), VesselWakeQuality.budget(OceanQuality.MEDIUM));
    assertEquals(new VesselWakeQuality.Budget(32, 160.0), VesselWakeQuality.budget(OceanQuality.ULTRA));
}

@Test
void geometryIsSymmetricAroundTravelDirection() {
    VesselWakeDescriptor newer = new VesselWakeDescriptor(0.0, 0.0, 1.0, 0.0, 0.40, 2.0, 0.0, 1.0);
    VesselWakeDescriptor older = new VesselWakeDescriptor(-1.0, 0.0, 1.0, 0.0, 0.40, 2.0, 1.0, 0.75);
    VesselWakeGeometry.Segment segment = VesselWakeGeometry.segment(newer, older);
    assertEquals(Math.abs(segment.leftOuterZ()), Math.abs(segment.rightOuterZ()), 1.0e-9);
    assertTrue(segment.width() > 0.0);
}
```

- [ ] **Step 2: Run focused tests and verify red state**

Run: `gradle test --tests com.redslovesgames.newestocean.client.VesselWakeQualityTest --tests com.redslovesgames.newestocean.client.VesselWakeGeometryTest`

Expected: compilation failure because the classes do not exist.

- [ ] **Step 3: Implement quality budgets and three-strip wake geometry**

Quality mapping is exactly:

```java
POTATO -> new Budget(4, 48.0)
LOW -> new Budget(8, 64.0)
MEDIUM -> new Budget(16, 96.0)
HIGH -> new Budget(24, 128.0)
ULTRA -> new Budget(32, 160.0)
```

For geometry, compute the perpendicular vector `p = (-directionZ, directionX)`. Base half-width is `max(0.35, beamBlocks * 0.45)`. Older samples expand by `1.0 + min(ageSeconds / 4.0, 1.0) * 0.9`. Produce two narrow arm quads and one center strip between newer and older descriptors. Keep geometry pure world-X/Z data; Y is added by the renderer from ocean samples.

- [ ] **Step 4: Run focused tests**

Expected: PASS, including finite-value assertions for every generated coordinate.

- [ ] **Step 5: Commit**

Commit message: `Add wake quality budgets and geometry`

---

### Task 3: Client wake tracker and lifecycle

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/VesselWakeTracker.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/NewestOceanClient.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/VesselWakeTrackerPolicyTest.java`

**Interfaces:**
- Produces: `VesselWakeTracker.register()`, `VesselWakeTracker.reset()`, `VesselWakeTracker.snapshot(Vec3d cameraPos, OceanQuality quality, double nowSeconds)`.
- Consumes synchronized client `BoatEntity` transforms only.

- [ ] **Step 1: Write pure policy tests**

Extract a nested/package-visible pure helper `selectNearest(List<Candidate>, cameraX, cameraZ, Budget)` and test:

```java
@Test
void selectionIsRadiusAndCountBounded() {
    var budget = new VesselWakeQuality.Budget(2, 50.0);
    var selected = VesselWakeTracker.selectNearest(List.of(
        new Candidate(1, 10.0, 0.0),
        new Candidate(2, 20.0, 0.0),
        new Candidate(3, 30.0, 0.0),
        new Candidate(4, 80.0, 0.0)
    ), 0.0, 0.0, budget);
    assertEquals(List.of(1, 2), selected.stream().map(Candidate::id).toList());
}
```

- [ ] **Step 2: Verify the policy tests fail**

Expected: compilation failure before tracker creation.

- [ ] **Step 3: Implement runtime tracking**

Register client entity load/unload callbacks for `BoatEntity` where available in Fabric 1.21.1, keeping a weak/identity collection of loaded boats. Register an end-client-tick callback that records histories for valid nearby boats using horizontal velocity and bounding-box width. If the client entity event API differs in this Fabric version, use one bounded client-world entity query per tick restricted to the current quality radius; do not scan arbitrary chunks or the server world.

Sampling rules:

```java
speed = Math.hypot(boat.getVelocity().x, boat.getVelocity().z);
direction = normalized horizontal velocity;
beam = Math.max(1.0, boat.getBoundingBox().getLengthX());
```

Use `max(lengthX, lengthZ)` if available so rotated/broad hulls are not underestimated. Prune invalid/unloaded entities and histories. `reset()` clears both tracked entities and histories.

- [ ] **Step 4: Wire lifecycle reset**

In `NewestOceanClient.resetOceanSync()`, call `VesselWakeTracker.reset()`. In `onInitializeClient()`, call `VesselWakeTracker.register()` once.

- [ ] **Step 5: Run full test suite**

Run: `gradle test`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `Track client vessel wake histories`

---

### Task 4: Wake renderer integration

**Files:**
- Create: `src/main/java/com/redslovesgames/newestocean/client/VesselWakeRenderer.java`
- Modify: `src/main/java/com/redslovesgames/newestocean/client/OceanWorldRenderer.java`
- Test: `src/test/java/com/redslovesgames/newestocean/client/VesselWakeRendererContractTest.java`

**Interfaces:**
- Produces: `VesselWakeRenderer.render(WorldRenderContext context, OceanRenderFrame.Frame frame, OceanQuality quality)`.
- Consumes: `VesselWakeTracker.snapshot(...)`, `VesselWakeGeometry.segment(...)`, and `NewestOcean.clientOcean().sample(...)`.

- [ ] **Step 1: Write a renderer contract test**

The test reads source/resources and asserts the renderer:

```java
assertTrue(source.contains("VesselWakeTracker.snapshot"));
assertTrue(source.contains("VesselWakeGeometry.segment"));
assertTrue(source.contains("NewestOcean.clientOcean().sample"));
assertTrue(source.contains("RenderSystem.depthMask(false)"));
```

Also assert `OceanWorldRenderer` invokes `VesselWakeRenderer.render` only after synchronized ocean frame preparation.

- [ ] **Step 2: Verify red state**

Expected: contract test fails because the renderer/invocation is missing.

- [ ] **Step 3: Implement wake rendering**

For each consecutive descriptor pair, generate the three wake strips. For each quad corner, sample synchronized ocean height at its world X/Z using the current frame time and conditions, then offset Y by `0.025` blocks to reduce z-fighting. Convert to camera-relative coordinates with `OceanRenderCoordinates.relative(...)`.

Use `VertexFormats.POSITION_COLOR`, `GameRenderer::getPositionColorProgram`, blending enabled, depth test enabled, and depth writes disabled during wake draw. Base foam color is approximately `(0.88, 0.95, 1.0)`; alpha is `0.10 + 0.55 * strength`, clamped to `0..0.70`. Center turbulence uses 75% of arm alpha.

Do not create particles and do not alter the main ocean shader uniform interface.

- [ ] **Step 4: Run focused renderer contract and full tests**

Run: `gradle test --tests com.redslovesgames.newestocean.client.VesselWakeRendererContractTest`

Then: `gradle test`

Expected: PASS.

- [ ] **Step 5: Commit**

Commit message: `Render wave-following vessel wakes`

---

### Task 5: Documentation and exact-head verification

**Files:**
- Create: `docs/PHASE_13_VESSEL_WAKES.md`
- Modify: `README.md`

**Interfaces:**
- Documents the exact client-only architecture and quality budgets.

- [ ] **Step 1: Document implemented behavior**

Record the 12-sample cap, 0.75-block spacing, 0.12 blocks/tick threshold, 4-second lifetime, quality budgets, wave-height following, no-networking rule, and CPU wake-quad rendering approach.

- [ ] **Step 2: Update README current implementation and renderer flow**

Add Phase 13 to the completed implementation list and place vessel wakes after foam/whitecaps in the renderer architecture.

- [ ] **Step 3: Run exact-head CI**

Push the documentation head and verify GitHub Actions completes `Build and test` and `Upload mod jar` successfully on that exact commit.

- [ ] **Step 4: Acceptance check**

Confirm the branch head is the verified commit and report the roadmap with Phase 13 at 100% only after that run is green.
