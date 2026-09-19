# Phase 12: Foam and Whitecaps

Phase 12 adds lightweight visual foam and whitecaps without introducing fluid particles or changing authoritative ocean physics.

## Formation model

Whitecap intensity is driven by visual wave shape rather than a texture painted across the whole ocean:

- local surface slope identifies sufficiently steep water;
- positive Gerstner crest curvature emphasizes sharp wave tops instead of troughs;
- rain and thunder increase the amount of visible foam;
- visual quality scales foam coverage and detail;
- intensity is clamped to a stable 0-1 range.

The GPU vertex shader accumulates crest curvature from the same active Gerstner components that produce displacement and normals. The fragment shader then blends the ocean surface toward a pale foam color where the whitecap signal is strong.

## Procedural breakup

The preferred GPU path adds a small world-space animated breakup term so whitecaps do not appear as perfectly continuous stripes along every crest. This is analytic shader math only: there are no foam textures, simulation grids, or particle fields.

Higher visual quality allows more of this breakup/detail to appear. Potato quality keeps the effect weaker and simpler.

## Weather response

Storm strength is computed from Minecraft rain and thunder gradients using the same relative weighting that already drives the ocean's weather wave scale. Calm water therefore needs a genuinely steep/sharp crest to whiten, while storms allow more frequent whitecaps on the same geometry.

## CPU fallback

If the custom ocean shader is unavailable, the Phase 8 CPU-displaced mesh still receives approximate whitecaps. The fallback derives slope from the averaged surface normal and estimates crest sharpness from positive height above the local tide-adjusted baseline, then feeds those values through the same pure Java `OceanWhitecapModel`.

The fallback deliberately stays approximate and cheap; the GPU path remains the preferred visual implementation.

## Quality scaling

Whitecap visual strength scales independently from physics:

| Quality | Foam scale |
| --- | ---: |
| Potato | 0.35 |
| Low | 0.55 |
| Medium | 0.75 |
| High | 0.90 |
| Ultra | 1.00 |

Changing this setting does not alter wave height, buoyancy, launch behavior, re-entry, surfing, planing, or server simulation.

## Performance model

Phase 12 intentionally avoids expensive fluid effects:

- no full fluid simulation;
- no per-cell foam state;
- no server networking;
- no foam textures;
- no ocean-wide particle field;
- only a few extra arithmetic operations in the existing ocean shader.

Real spray particles remain reserved for Phase 15 near vessels, impacts, and shorelines.

## Verification

TDD coverage includes:

- calm flat water producing zero foam;
- steep sharp crests producing visible whitecaps;
- storm amplification;
- intensity clamping;
- quality-dependent foam strength;
- shader-resource contract checks for whitecap uniforms, curvature accumulation, varyings, and fragment blending.

The shader assets are packaged and their contract is tested in CI. A real OpenGL/Minecraft smoke test is still required later because standard Gradle CI does not create a live game rendering context to compile and display GLSL.
