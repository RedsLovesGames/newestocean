# Phase 16: Shader Compatibility and DEPTHS_ULTRA Optimization

Phase 16 makes Newest Ocean coexist safely with Iris shaderpacks without adding a hard Iris or Sodium dependency. The normal fast custom-shader path remains active when no Iris shaderpack is rendering, while active shaderpacks automatically switch Newest Ocean to its CPU/vanilla-program fallbacks.

## Compatibility modes

### NORMAL_GPU

Used when Iris is absent or Iris is installed with shaders disabled.

- `newestocean:ocean_surface` remains available.
- `newestocean:shoreline_break` remains available.
- `newestocean:vessel_wake` remains available.
- The selected visual quality keeps its normal visual wave count.
- Sodium by itself therefore stays on Newest Ocean's fast GPU path.

### IRIS_GENERIC

Used when an Iris shaderpack is active but is not recognized as DEPTHS_ULTRA, or when Iris is installed but the required active-state API bridge cannot be resolved safely.

- Newest Ocean custom shader programs are not invoked.
- The ocean uses the existing CPU-displaced LOD mesh and Minecraft position/color program.
- Vessel wakes use CPU procedural-ocean conformity.
- Shoreline breakers use the CPU synchronized-ocean fallback.
- Newest Ocean skips its world render callback during an Iris shadow pass.
- Physics, synchronization, LOD radius, and shoreline cache authority are unchanged.

This mode deliberately favors compatibility over visual-GPU throughput for unknown shaderpacks.

### IRIS_DEPTHS_ULTRA

Used when an active Iris shaderpack name normalizes to DEPTHS_ULTRA.

It includes every `IRIS_GENERIC` rule plus a profile tuned for the supplied `DEPTHS_ULTRA.zip`:

| Setting | DEPTHS_ULTRA compatibility value |
| --- | ---: |
| Maximum Newest Ocean CPU visual wave components | 4 |
| CPU ocean base alpha | 0.58 |
| CPU whitecap multiplier | 0.65 |
| CPU wake multiplier | 0.90 |
| CPU shoreline multiplier | 0.90 |

The CPU wave cap is visual only. The server-authoritative physical ocean remains all six physical wave components.

## Why DEPTHS_ULTRA gets a dedicated profile

The supplied DEPTHS_ULTRA shaderpack was inspected while designing this phase. It is Complementary-derived and already owns a substantial water pipeline, including:

- dedicated `gbuffers_water` programs,
- dedicated Distant Horizons `dh_water` programs,
- water vertex waving through `WAVING_WATER_VERTEX`,
- water foam,
- reflections and world-space reflection handling,
- water caustics and underwater surface-depth behavior,
- colored-lighting integration,
- custom water/shadow handling,
- an Ultra profile with 256-block shadow distance, water reflection quality 2, colored lighting 512, and world-space reflections enabled.

Running Newest Ocean's custom water, wake, and shoreline core shaders at the same time would create two competing shader pipelines and duplicate expensive work. DEPTHS mode therefore keeps Newest Ocean responsible for deterministic geometry and motion while leaving DEPTHS responsible for its own reflection, fog, caustic, lighting, and post-processing character.

The reduced 0.58 base alpha also makes the CPU fallback less visually dominant over DEPTHS's water treatment.

## Optional Iris bridge

`IrisCompatibilityBridge` contains no hard Iris imports and starts by checking Fabric Loader for mod id `iris`.

When Iris is present it reflectively resolves:

- `IrisApi.getInstance()`
- `IrisApi.isShaderPackInUse()`
- `IrisApi.isRenderingShadowPass()`

Shaderpack-name discovery is optional and best-effort. It tries a config name accessor if the installed API exposes one and otherwise tries Iris's internal `getCurrentPackName()` reflectively. If name discovery fails, Newest Ocean simply uses `IRIS_GENERIC`.

If the required active-state API bridge itself fails, Newest Ocean fails safe to CPU compatibility mode and logs the bridge problem only once.

No Iris or Sodium dependency is added to `fabric.mod.json` or Gradle.

## Dynamic switching

The compatibility snapshot is evaluated at render time rather than frozen at startup. This allows:

- enabling a shaderpack to switch to compatibility rendering,
- disabling a shaderpack to return to Newest Ocean custom shaders,
- changing shaderpacks to switch between generic and DEPTHS profiles,
- doing all of the above without restarting Minecraft.

Reflective method handles are resolved once; only lightweight state queries occur per rendered frame.

## Shadow pass

When Iris reports an active shaderpack shadow pass, Newest Ocean skips its world render callback for that pass.

This prevents:

- duplicate CPU ocean evaluation for the shadow camera,
- translucent geometry being submitted using player-camera assumptions during the shadow pass,
- interference with DEPTHS's own water-shadow logic.

Server physics, networking, ocean synchronization, and wake-history bookkeeping continue independently.

## Shared render-state boundary

Phase 16 centralizes transient render-state mutations in `OceanRenderState`.

- Main ocean draws use `drawSurface(...)`.
- Shoreline and wake overlays use `drawTwoSided(...)`.
- Depth writes are disabled only around the draw and restored afterward.
- Blending is disabled again after each pass.
- Two-sided passes restore culling afterward.
- CPU callers still explicitly select Minecraft's position/color program before entering the shared state boundary.

This reduces the chance that Newest Ocean leaves render state behind for Iris composite/final stages or later Fabric render callbacks.

## Physics and networking isolation

Phase 16 changes only client rendering policy.

It adds no:

- server shader state,
- ocean packets,
- vessel-force changes,
- buoyancy changes,
- collision changes,
- physical-wave quality reduction,
- shoreline networking,
- DEPTHS shaderpack redistribution or source copying.

Graphics quality and shader compatibility remain isolated from the authoritative six-component physical ocean.

## Tests

Automated coverage includes:

- Iris absent -> normal GPU mode,
- Iris installed with shaders disabled -> normal GPU mode,
- active generic Iris shaderpack -> CPU compatibility mode,
- DEPTHS_ULTRA filename/name normalization,
- DEPTHS four-component visual cap and exact tuning constants,
- fail-safe behavior when the Iris bridge cannot resolve,
- shadow-pass rendering suppression,
- reflection-only Iris bridge contract with no hard Iris imports,
- explicit CPU mesh wave-component limits,
- centralized compatibility routing in the ocean renderer,
- custom-shader bypass for wakes and shoreline effects,
- compatibility wave/tuning propagation through CPU fallbacks,
- shared render-state restoration,
- absence of shader-compatibility references from non-client physics/server code.

## Remaining runtime validation

CI verifies Java compilation, unit/contract tests, resources, remapped JAR generation, and artifact upload. It does not create a real Iris/OpenGL gameplay context.

Phase 17 should therefore perform live testing with Iris + the supplied DEPTHS_ULTRA pack, including:

- above-water and underwater views,
- day/night and weather,
- shoreline-heavy scenes,
- multiple Small Ships wakes,
- shaderpack enable/disable without restart,
- DEPTHS shadow passes,
- performance at each Newest Ocean quality tier,
- final alpha, whitecap, wake, and shoreline visual tuning.

Phase 15 spray/impact particles are optional polish and are currently deferred rather than required for the core ocean system.
