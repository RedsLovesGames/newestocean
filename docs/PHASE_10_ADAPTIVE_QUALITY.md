# Phase 10: Adaptive Quality

Phase 10 turns the visual quality tiers into a live frame-time controller while keeping authoritative ocean physics unchanged.

## Runtime behavior

- The renderer targets approximately 60 FPS (`16.67 ms` per frame).
- Frame timing is measured from monotonic render timestamps rather than frame counts, so adaptation behaves consistently at different frame rates.
- Sustained slow rendering must exceed 115% of the target frame time for about 3 seconds before the ocean drops one visual tier.
- Upgrades are deliberately slower: frame time must remain below 70% of the target for about 10 seconds before the ocean rises one tier.
- Neutral frame times drain both pending hysteresis windows so short spikes do not accumulate forever.
- Individual samples above 250 ms are ignored so loading screens, debugger pauses, alt-tab stalls, and other pathological gaps do not force the ocean toward Potato.
- A completed hysteresis window changes only one tier and resets before another transition can occur.

## Quality transitions

The visual ladder remains:

`POTATO -> LOW -> MEDIUM -> HIGH -> ULTRA`

Changing tier updates visual wave count, render radius, and LOD grid density through the existing `OceanQuality` and Phase 7 topology systems. Cached topology remains reusable by tier, while water coverage is invalidated when the active tier changes.

## Manual control

`OceanWorldRenderer.setQuality(...)` still selects an explicit starting/current tier and resets pending adaptive timing. Adaptive mode can be enabled or disabled through `setAdaptiveQualityEnabled(...)` without touching any server or vessel state.

## Physics isolation

Adaptive quality is client-render-only. It does not modify:

- the synchronized ocean seed,
- server `ProceduralOcean`,
- physical wave components,
- tides or currents,
- buoyancy,
- launch/re-entry state,
- surfing/planing physics,
- vessel motion or networking.

A Potato client and an Ultra client therefore continue to interact with the same authoritative physical ocean.

## Tests

Phase 10 tests cover:

- 3-second elapsed-time downgrade hysteresis,
- 10-second upgrade hysteresis,
- neutral-window decay and oscillation resistance,
- one-tier-per-window transitions,
- invalid/huge frame sample rejection,
- manual-quality reset behavior,
- first-frame sampler priming,
- long-pause rejection,
- renderer sampler reset behavior.
