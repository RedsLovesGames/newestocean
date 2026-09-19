# Phase 14: Shoreline Waves

Phase 14 adds terrain-aware visual shoaling, directional breaking waves, and shoreline foam/wash without introducing ocean-wide fluid simulation or server-side shoreline state.

## Architecture

The shoreline system is intentionally hybrid:

1. CPU terrain probes build a small cached shoreline field aligned to the existing ocean LOD cells.
2. The field stores shallow-water depth, nearest-shore distance, normalized direction toward shore, and a bounded shoreline influence value.
3. The preferred GPU path combines those cached terrain metrics with the same packed deterministic Gerstner waves already used by the visible ocean.
4. A CPU fallback samples the synchronized procedural ocean and applies an approximate breaker effect if the dedicated shader is unavailable.

This keeps terrain discovery off the GPU while keeping animated wave deformation and procedural foam off the normal CPU frame path.

## Bounded terrain analysis

`ShorelineAnalyzer` only examines cells already classified as water by the existing `OceanLodCoverageMask`.

For each wet cell it:

- probes downward from sea level for at most 12 blocks,
- skips the horizontal shore search entirely when the sampled depth is too deep to produce any shoreline influence,
- otherwise searches for the nearest non-water column only inside the current quality-tier radius,
- uses deterministic tie-breaking for equal-distance shore candidates,
- stores no shoreline influence for dry cells or cells with no shore inside the bounded radius.

There is no chunk-wide scan, flood fill, path finding, or persistent world shoreline simulation.

## Quality budgets

Shore search radius and visual contribution scale with the client visual quality tier:

| Quality | Shore search radius | Visual quality scale |
| --- | ---: | ---: |
| Potato | 4 blocks | 0.45 |
| Low | 6 blocks | 0.60 |
| Medium | 8 blocks | 0.78 |
| High | 10 blocks | 0.90 |
| Ultra | 12 blocks | 1.00 |

Graphics quality changes only visual shoreline work. It does not alter authoritative vessel physics.

## Shoreline influence

The pure `ShorelineBreakModel` combines two bounded terms:

- shallowness, which fades out as water becomes deep,
- proximity, which fades out as the nearest shore approaches the quality-tier search radius.

The final shoreline influence is clamped to 0..1. Deep ocean and water with no nearby shore therefore contribute no shoreline breaker work.

## Directional breaking

The system stores a normalized direction from each water cell toward the nearest shore.

The dedicated `newestocean:shoreline_break` vertex shader compares that direction with the active Gerstner wave directions. Waves moving toward shore contribute to incoming-wave alignment, while waves traveling away from shore do not receive the same breaker emphasis.

The shader then combines:

- cached shallow-water influence,
- incoming-wave alignment,
- active deterministic wave energy,
- storm strength,
- visual quality scaling,
- procedural breakup.

This produces directional breakers instead of a static foam border around land.

## Visual shoaling

The GPU shoreline pass reuses the same six-slot packed Gerstner wave payload as the ocean renderer.

Near shallow shoreline cells it adds a deliberately small visual-only crest lift and breaker shaping. This approximates wave shoaling without modifying `ProceduralOcean`, server physics, vessel forces, or the synchronized physical water surface.

The effect is intentionally bounded so shoreline visuals cannot create a second incompatible physical ocean.

## Procedural foam and wash

The shoreline fragment shader generates breaker foam and wash procedurally from the shoreline metrics and animated wave state.

The pass uses sparse shoreline cell quads rather than an ocean-wide foam simulation grid. It requires no foam textures, no persistent particles, and no shoreline network packets.

Phase 15 remains responsible for localized spray/impact particles.

## Cache lifecycle

The shoreline field shares the same lifecycle as the existing water-coverage cache.

It rebuilds when the ocean coverage rebuilds because of:

- snapped LOD origin movement,
- visual quality change,
- dimension change,
- the existing periodic 100-tick coverage refresh.

`OceanWorldRenderer.resetCoverage()` clears both coverage and shoreline state together.

This means terrain probing does not happen every frame.

## Render order

The `AFTER_TRANSLUCENT` ocean render path is ordered as:

1. main ocean surface,
2. shoreline breakers and foam,
3. vessel wakes.

The shoreline pass uses blending and depth testing with depth writes disabled during the overlay, then restores render state afterward.

## GPU path and CPU fallback

Preferred path:

- `ShorelineShader`
- `newestocean:shoreline_break`
- sparse shoreline `POSITION_COLOR` quads
- packed Gerstner wave uniforms
- GPU wave conformance, shoaling, breaker strength, and procedural foam

Fallback path:

- `ShorelineRenderer` samples `NewestOcean.clientOcean()` on the CPU,
- uses the synchronized render time and ocean conditions,
- places the shoreline overlay on the moving ocean surface,
- applies approximate breaker intensity and foam color.

If the custom shoreline shader cannot load, shoreline visuals degrade rather than disappearing completely.

## Networking and physics isolation

Phase 14 adds:

- no network packets,
- no server shoreline cache,
- no server terrain scan,
- no vessel-force changes,
- no collision changes,
- no changes to the deterministic physical wave field,
- no change to Small Ships or vanilla boat authority.

The system is entirely client visual state derived from Minecraft terrain plus the already-synchronized deterministic ocean.

## Tests

Phase 14 tests cover:

- bounded shoreline influence math,
- quality search radii and visual scales,
- directional incoming-wave breaker behavior,
- normalized nearest-shore direction,
- no-shore behavior,
- dry-cell suppression,
- the hard 12-block depth-probe ceiling,
- deterministic shoreline samples,
- deep-water horizontal-search suppression,
- shoreline shader registration and required uniforms,
- reuse of packed Gerstner wave data,
- directional breaking and procedural foam shader contracts,
- renderer use of the cached shoreline field,
- GPU shoreline path and CPU procedural-ocean fallback,
- shared cache rebuild/reset lifecycle,
- shoreline render ordering before vessel wakes.

## Validation status

CI verifies Java compilation, all unit/contract tests, shader resources in the JAR, remapped JAR generation, and artifact upload.

CI does not provide a real Minecraft/OpenGL gameplay context. Phase 14 therefore still needs later in-game graphics/stress tuning for final breaker height, foam opacity, complex coast geometry, shader-pack interaction, and worst-case Ultra shoreline scenes. That runtime tuning belongs to the later shader-compatibility and stress-testing phases rather than changing the Phase 14 architecture.
