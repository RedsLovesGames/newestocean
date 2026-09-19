# Phase 16: Shader Compatibility and DEPTHS_ULTRA Optimization

## Status

Approved architectural direction: automatic compatibility mode when Iris is actively rendering a shaderpack, with a DEPTHS_ULTRA-specific tuning profile. Sodium without an active Iris shaderpack remains on Newest Ocean's normal GPU path.

## Problem

Newest Ocean currently owns three custom core-shader paths:

- `newestocean:ocean_surface`
- `newestocean:vessel_wake`
- `newestocean:shoreline_break`

Iris documents that custom shaders added by mods are not generally compatible with an active Iris shaderpack and recommends a non-custom-shader fallback path. Newest Ocean already has CPU/vanilla-program fallback implementations for the ocean surface, vessel wakes, and shoreline breakers; Phase 16 centralizes when those fallbacks must be used and hardens their render-state behavior.

The supplied `DEPTHS_ULTRA.zip` is a Complementary-derived shaderpack with its own heavyweight water pipeline. Inspection of the supplied pack shows:

- dedicated `gbuffers_water` and Distant Horizons `dh_water` programs,
- `WAVING_WATER_VERTEX` enabled,
- Ultra profile `shadowDistance=256.0`,
- Ultra profile `WATER_REFLECT_QUALITY=2`,
- Ultra profile `COLORED_LIGHTING=512`,
- Ultra profile `WORLD_SPACE_REFLECTIONS=1`,
- water foam enabled (`WATER_FOAM_I=100`),
- water caustics/reflections and underwater surface-depth fading,
- custom shadow-water handling.

Newest Ocean must not stack its own custom shaders on top of that pipeline when DEPTHS_ULTRA is active.

## Compatibility modes

Introduce one centralized `ShaderCompatibility` decision with three effective modes.

### 1. NORMAL_GPU

Used when:

- Iris is absent, or
- Iris is installed but no shaderpack is currently in use.

Behavior:

- use `OceanGpuShader` when available,
- use `VesselWakeShader` when available,
- use `ShorelineGpuShader` when available,
- preserve all current quality-tier wave counts and visual parameters.

Sodium alone therefore keeps the fast GPU path.

### 2. IRIS_GENERIC

Used when Iris reports that a shaderpack is actively rendering and the active pack is not recognized as DEPTHS_ULTRA.

Behavior:

- never call Newest Ocean's custom shader programs,
- force ocean CPU mesh generation,
- force wake CPU wave-conformity fallback,
- force shoreline CPU fallback,
- skip Newest Ocean world rendering during the Iris shadow pass,
- preserve the selected Newest Ocean visual quality and normal CPU fallback appearance,
- restore render state after every pass.

This mode prioritizes correctness over speed because an unknown shaderpack may otherwise ignore or conflict with Newest Ocean's custom programs.

### 3. IRIS_DEPTHS_ULTRA

Used when Iris reports an active shaderpack and the configured shaderpack name normalizes to a DEPTHS_ULTRA identifier, including forms such as `DEPTHS_ULTRA.zip`, `DEPTHS ULTRA`, or `depths-ultra`.

Behavior includes every `IRIS_GENERIC` rule plus DEPTHS-specific performance and blending tuning:

- preserve the current LOD topology and render radius,
- cap Newest Ocean CPU ocean evaluation to `min(selected visual wave count, 4)`,
- keep authoritative physics at all six physical wave components,
- use CPU fallback ocean base alpha `0.58` instead of the normal `0.72`,
- multiply CPU whitecap strength by `0.65`,
- multiply CPU wake alpha/strength by `0.90`,
- multiply CPU shoreline foam alpha/strength by `0.90`,
- do not render Newest Ocean in the Iris shadow pass,
- do not attempt to reproduce DEPTHS reflections, caustics, colored-lighting, or shadow logic inside Newest Ocean.

The four-component cap removes the two smallest visual wave bands from the CPU compatibility surface while keeping the long and medium waves that define the ocean silhouette. DEPTHS keeps responsibility for its own fine water shading, fog, reflection, and post-processing character.

Graphics compatibility may therefore differ slightly from the six-wave GPU surface while vessel physics remains unchanged and server-authoritative.

## Iris access without a hard dependency

Newest Ocean must not require Iris to load.

Use Fabric Loader to check whether mod id `iris` is present. When present, resolve the Iris API reflectively and cache the reflective handles once.

Required active-state calls, when available:

- `IrisApi.getInstance()`
- `isShaderPackInUse()`
- `isRenderingShadowPass()`

Pack-name detection is best-effort and version-tolerant:

1. first try `IrisApi.getInstance().getConfig()` and a reflective `getShaderPackName()` if the installed API exposes it,
2. otherwise try the internal static `net.irisshaders.iris.Iris.getCurrentPackName()` reflectively if present,
3. otherwise leave the pack name unknown and use `IRIS_GENERIC`.

The internal name lookup is optional optimization only. Failure to resolve it must never prevent generic Iris compatibility.

No Iris class may appear in a Newest Ocean method signature, field type, or static initializer that would load when Iris is absent.

If Iris is loaded but reflective active-state API resolution fails, compatibility must fail safe to the CPU path instead of attempting Newest Ocean custom shaders. Log the bridge failure only once.

If shaderpack-name lookup fails but `isShaderPackInUse()` succeeds, use `IRIS_GENERIC`.

## Dynamic switching

Compatibility state is queried from the Iris bridge at render time, not frozen at game startup.

This allows:

- shaderpack enabled -> automatic CPU compatibility path,
- shaderpack disabled -> automatic return to Newest Ocean GPU shaders,
- shaderpack changed -> automatic generic/DEPTHS profile change,
- no Minecraft restart required.

The reflection objects themselves are cached; only inexpensive boolean/name state is queried per render frame.

## Shadow-pass behavior

When Iris reports `isRenderingShadowPass() == true`, Newest Ocean skips its world render callback entirely.

Rationale:

- DEPTHS_ULTRA already owns water shadow behavior,
- Newest Ocean translucent geometry is sorted for the player camera, not the shadow camera,
- rendering the custom ocean again during the shadow pass would duplicate expensive CPU wave work and can create bad translucent shadow ordering.

Server physics, wake histories, and synchronization continue normally.

## Renderer integration

### OceanWorldRenderer

At the beginning of the render callback:

1. get the current `ShaderCompatibility.Snapshot`,
2. return early for an Iris shadow pass,
3. choose GPU or CPU ocean rendering from the snapshot rather than only `OceanGpuShader.available()`,
4. choose the CPU visual wave count through the compatibility profile,
5. pass compatibility visual tuning into CPU color/alpha/whitecap calculations,
6. pass the same snapshot to shoreline and wake rendering.

### VesselWakeRenderer

When custom shaders are disallowed by the snapshot:

- bypass `VesselWakeShader`,
- use the existing synchronized procedural-ocean CPU conformity path,
- apply the DEPTHS `0.90` strength multiplier only in `IRIS_DEPTHS_ULTRA`.

### ShorelineRenderer

When custom shaders are disallowed by the snapshot:

- bypass `ShorelineGpuShader`,
- use the existing CPU synchronized-ocean fallback,
- apply the DEPTHS `0.90` breaker/foam multiplier only in `IRIS_DEPTHS_ULTRA`.

## Render-state hardening

Phase 16 adds a small shared render-state helper or equivalent `try/finally` discipline so every Newest Ocean pass explicitly restores the state it mutates.

At minimum the implementation must protect:

- blend enable/disable,
- depth-mask state,
- culling state for two-sided wake/shore overlays,
- selected shader/program where the existing API permits safe restoration.

The goal is to avoid leaking Newest Ocean state into Iris composite/final stages or later Fabric render callbacks.

## Performance goals

With DEPTHS_ULTRA active, Newest Ocean must avoid:

- custom core-shader invocation,
- a second shadow rendering pass,
- six-band CPU ocean evaluation at High/Ultra,
- extra terrain/shore analysis beyond the existing cached Phase 14 lifecycle,
- new network traffic,
- shaderpack file parsing on every frame.

The DEPTHS-specific CPU wave cap is visual only. Physics remains six-band and server-authoritative.

## Scope exclusions

Phase 16 will not:

- modify or redistribute the supplied DEPTHS_ULTRA shaderpack,
- copy DEPTHS/Complementary shader code into Newest Ocean,
- add Iris as a required dependency,
- add Sodium as a required dependency,
- change vessel physics,
- change ocean synchronization,
- change shoreline cache authority,
- attempt general shaderpack source rewriting.

A separately patched DEPTHS shaderpack can be considered later if direct `gbuffers_water` integration is desired, but it is not required for Phase 16 compatibility.

## Tests

Add deterministic unit/contract coverage for:

- Iris absent -> `NORMAL_GPU`,
- Iris present with shaders disabled -> `NORMAL_GPU`,
- generic shaderpack active -> `IRIS_GENERIC`,
- DEPTHS_ULTRA pack-name normalization -> `IRIS_DEPTHS_ULTRA`,
- active shaderpack + shadow pass -> rendering skipped,
- reflective bridge failure -> fail-safe CPU compatibility mode,
- shaderpack toggle changes mode without restart,
- public-config pack-name lookup unavailable -> internal-name fallback attempted,
- all pack-name lookup unavailable -> `IRIS_GENERIC`,
- DEPTHS CPU visual wave count capped at four while normal mode preserves the plan count,
- DEPTHS visual tuning constants,
- `OceanWorldRenderer` consults centralized compatibility state,
- wake and shoreline renderers bypass custom shaders when compatibility mode disallows them,
- physics code does not read compatibility mode.

Existing ocean/wake/shore shader contract tests remain unchanged because those GPU paths are still used when Iris shaderpacks are not active.

## Completion gate

Phase 16 is complete only when:

- all new tests pass,
- the full Gradle build passes,
- the mod JAR is produced and uploaded by CI,
- README and a dedicated Phase 16 runtime document are updated,
- the exact final `feature/ocean-core` head has a green CI run.

A live Minecraft test with Iris + the supplied DEPTHS_ULTRA pack is still required in Phase 17 because CI does not create an actual Iris/OpenGL render context.