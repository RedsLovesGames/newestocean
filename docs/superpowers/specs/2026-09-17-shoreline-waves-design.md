# Phase 14 Shoreline Waves and Breaking Design

**Project:** Newest Ocean

**Branch:** `feature/ocean-core`

**Status:** Approved design, ready for implementation planning after spec review

## Goal

Add convincing shoreline wave behavior without full fluid simulation. Waves should visibly shoal, steepen, break, and generate shore foam as they approach land while preserving the existing deterministic ocean, server-authoritative vessel physics, graphics/physics separation, and low-end-PC performance target.

## Non-goals

Phase 14 does not add:

- CFD or block-by-block fluid dynamics
- server-side shoreline wave state
- new shoreline networking
- gameplay drag or vessel-force changes
- collision changes
- water block displacement
- persistent foam simulation
- spray particles or splash emitters, which remain Phase 15
- Shippy Ships support

## Architectural choice

Use a **cached CPU shoreline field plus GPU breaking-wave rendering**.

The CPU is responsible for terrain-aware quantities that the shader cannot cheaply derive from Minecraft block data:

- approximate water depth
- distance to nearby non-water/shoreline
- approximate shore-facing direction
- bounded near-shore classification

The GPU is responsible for continuously animated effects:

- shoaling visual scale
- crest steepening/compression
- directional breaker intensity
- procedural breaking foam
- shore wash/fade pattern

This avoids both expensive per-frame terrain scans and CPU-generated animated breaker geometry.

## Shore field lifecycle

A new shoreline cache is built alongside the existing snapped LOD/water-coverage lifecycle.

The cache key must include at least:

- dimension
- active visual quality
- snapped LOD origin X/Z
- topology/coverage identity or equivalent grid dimensions

The cache refresh cadence should follow the existing water-coverage refresh model, currently 100 world ticks, so nearby block/water edits can eventually update without probing terrain every render frame.

Changing visual quality or resetting the renderer invalidates the shoreline cache.

## Bounded probing

Only water cells already admitted by the ocean coverage mask are candidates for shore analysis.

For each candidate, Phase 14 uses bounded local probes rather than arbitrary chunk/world scans.

### Distance-to-land

Search a small horizontal neighborhood around the cell for the nearest non-water/shore boundary. The maximum search radius is quality-scaled and hard-capped.

Recommended visual search radii:

| Quality | Maximum shore search radius |
| --- | ---: |
| Potato | 4 blocks |
| Low | 6 blocks |
| Medium | 8 blocks |
| High | 10 blocks |
| Ultra | 12 blocks |

Cells with no shoreline inside the search radius receive zero shoreline influence.

### Approximate water depth

Depth is estimated from the sea-level surface downward using a bounded vertical probe until a non-water/supporting block is found.

Recommended maximum depth probe: 12 blocks.

Depth does not need to reproduce a full bathymetric solver. It only needs a stable shallow/deep classification for visual shoaling.

### Shore direction

Use the nearest-land vector or a small local distance-gradient approximation to produce a normalized direction from water toward shore.

If a valid direction cannot be established, the cell remains water but receives zero directional breaking contribution.

## Pure shoreline sample model

Introduce a pure Java representation for one analyzed water location. It should expose finite, normalized/bounded values suitable for unit testing and shader upload.

Suggested conceptual fields:

- `depthBlocks`
- `distanceToShoreBlocks`
- `shoreDirectionX`
- `shoreDirectionZ`
- `shoreInfluence` in `0..1`

The exact storage layout may be optimized during implementation, but public behavior must remain testable independent of Minecraft rendering APIs.

## Breaker model

Breaker strength is a visual-only bounded function of:

1. **Shallow-water factor**
   - approaches 0 in deep water
   - rises as depth becomes shallow

2. **Shore-distance factor**
   - 0 outside the bounded shore band
   - increases approaching the shore

3. **Incoming-wave alignment**
   - derived from the active Gerstner wave directions
   - waves traveling toward shore break more strongly
   - waves traveling parallel to shore contribute less
   - waves traveling away from shore contribute little or none

4. **Wave steepness / amplitude**
   - stronger physical wave components produce stronger visual breaking

5. **Weather strength**
   - rain/thunder can amplify visible foam/break intensity

6. **Visual quality**
   - controls visual detail and shore sampling radius only
   - must never alter authoritative physical waves or vessel behavior

The final breaker value must clamp to `0..1`.

## Incoming-wave direction

The shader already receives the packed Gerstner components used by the ocean renderer. Phase 14 should reuse those components rather than introduce a second independent wave model.

For directional breaking, each active visual wave can contribute using the dot product between its horizontal travel direction and the direction toward shore.

Conceptually:

`incoming = max(0, dot(waveDirection, shoreDirection))`

where:

- `waveDirection` is the normalized horizontal direction of one Gerstner component
- `shoreDirection` is the normalized horizontal direction from water toward land
- `incoming` is a unitless value from 0 to 1 indicating how directly that wave moves toward shore

Implementation may aggregate across active visual waves using amplitude/steepness weighting.

## GPU representation

The preferred path should keep shoreline terrain analysis on the CPU but move animated breaking appearance to the GPU.

Two implementation forms are acceptable, in order of preference:

1. Extend the existing ocean surface vertex/color data path with a compact shoreline attribute/payload for each submitted base vertex or cell.
2. If Minecraft 1.21.1 shader/vertex-format constraints make that fragile, use a separate sparse shoreline overlay pass with its own shader and bounded geometry.

The selected implementation must preserve:

- camera-relative coordinates
- depth testing
- no depth writes for translucent foam overlays
- no per-frame terrain scans
- no new network traffic

## Visual behavior

### Shoaling

Near shore, wave appearance should become visually steeper and slightly compressed rather than simply translating the entire physical surface upward.

This effect is visual-only. It must not change `ProceduralOcean` or any physics sample.

### Breaking

Breaker foam appears when shallow water, shore proximity, incoming direction, and wave steepness combine above a threshold.

The foam should be procedural, using the Phase 12 style of mathematical breakup instead of a persistent foam texture/simulation grid.

### Shore wash

A low-cost animated shore-wash term may move/fade foam toward land after a breaker, but it must remain a visual shader effect. No block water animation or fluid state changes are allowed.

## Quality scaling

Visual quality changes the amount of shoreline work and detail, not physics.

Recommended behavior:

- **Potato:** shortest shore search, broad/simple breaker foam, no secondary wash detail
- **Low:** short shore band, simple directional breaking
- **Medium:** full baseline directional breaker and procedural breakup
- **High:** wider shore band and finer breakup
- **Ultra:** widest bounded shore band and highest visual breakup detail

Adaptive quality changes should naturally rebuild/invalidate the shoreline cache through the existing visual quality lifecycle.

## CPU fallback

If the custom shoreline/ocean shader path is unavailable, Phase 14 must still render an approximate shoreline effect.

The fallback may use cached shore influence and CPU ocean mesh normals/heights to tint/blend near-shore quads toward foam. It does not need to reproduce every GPU breakup detail, but shoreline effects must not disappear completely solely because the custom shader failed to load.

## Performance constraints

Phase 14 must satisfy all of the following:

- no full-world shoreline scans
- no per-frame block/fluid probing
- no unbounded radius searches
- no per-cell particle emitters
- no new per-tick server work
- no new packets
- no modification to vessel physics update rates
- shore cache rebuild only on snapped-grid/quality/dimension changes or periodic stale refresh

## Proposed implementation units

Likely files/classes:

- `ShorelineSample` or equivalent pure value record
- `ShorelineField` for cached topology-aligned shore metrics
- `ShorelineAnalyzer` for bounded Minecraft world probes
- `ShorelineBreakModel` for pure `0..1` breaker math
- shader changes in `ocean_surface.vsh/.fsh/.json` or a dedicated shoreline shader if vertex-format constraints justify it
- `OceanWorldRenderer` integration and cache invalidation
- `docs/PHASE_14_SHORELINE_WAVES.md`

Names may change if the implementation reveals a cleaner split, but responsibilities must remain separated: analysis/cache, pure breaker math, rendering.

## Testing requirements

### Pure model tests

Must cover:

- deep water produces negligible/no shore influence
- shallow water increases influence
- smaller shore distance increases influence
- incoming wave direction produces stronger breaking than parallel direction
- outgoing direction produces negligible directional breaking
- breaker intensity clamps to `0..1`
- quality scaling is monotonic where applicable
- invalid/non-finite inputs are rejected

### Cache/analyzer tests

Must cover bounded behavior where practical:

- maximum horizontal search radius obeys quality budget
- depth probing is hard-capped
- no-shore-within-radius returns zero influence
- deterministic result for identical probe data
- shore direction remains normalized/finite

Minecraft-world-specific probing may use a pure probe interface so the logic can be tested without starting a client.

### Shader/resource contract tests

Must assert that the chosen shader path includes the shoreline inputs and actually uses them in breaker/foam calculations.

If the ocean shader is extended, tests should verify the shoreline attribute/uniform/varying contract. If a separate shader is used, tests should verify its registration and resource files.

### Renderer contract tests

Must verify:

- shoreline cache is used by the renderer
- shoreline state is invalidated/reset with renderer reset/quality changes
- fallback path remains available
- depth writes are disabled during translucent shoreline foam drawing where applicable

## Acceptance criteria

Phase 14 is complete only when:

1. near-shore water is identified through bounded cached analysis
2. shallow depth and shorter shore distance increase visual shore influence
3. incoming waves break more strongly than parallel/outgoing waves
4. preferred rendering uses GPU/procedural animation rather than CPU per-frame terrain work
5. CPU fallback retains approximate shore foam
6. adaptive visual quality affects only graphics detail/cost
7. server physics, vessel physics, synchronization, and networking remain unchanged
8. all existing and new tests pass
9. the mod JAR builds and uploads in GitHub Actions
10. the exact final `feature/ocean-core` head is CI-green

## Remaining manual validation

As with Phases 8, 9, 12, and 13, CI cannot create a real Minecraft OpenGL scene. Later Phase 17 testing should visually tune:

- breaker threshold
- foam width
- shallow-water steepening strength
- shore wash timing
- cliffs vs beaches
- irregular coastlines
- rivers/lakes near sea level
- Sodium/Iris compatibility
- many visible shoreline cells on weak hardware
