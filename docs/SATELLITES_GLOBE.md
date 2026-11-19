# Satellites · The Globe Is the App

The tab is an edge-to-edge Earth3D viewport with an inset-aware HUD, not a feed. `SatellitesStage` owns the clock, observer and repository integration; the non-scrolling `SatelliteHud` consumes the shared `SatellitesUiState`. `SatPanel` is a single destination. Time and Layers never install a full-screen scrim. Sheets have their own scroll areas. Location, camera orientation, scope and simulation offset are not reset on Detail Back.

The globe projection reserves a top utility band and a lower focus/navigation band so primary controls and the Focus Card do not cover the Earth. Satellite search is always visible in Fleet; it matches localized names, NORAD numbers, designations, operators and category names, normalizing Persian/Arabic digits and letter variants. Category scope and the Fleet's above-horizon / naked-eye-capable filters persist across visits. Catalog categories distinguish ISS, Tiangong, Hubble, Starlink and Earth-observation missions. Satellite-specific marker silhouettes are used on the globe, Fleet, passes and Detail. Detail heroes bundle attributed source photography/renders; see [`THIRD_PARTY_LICENSES`](../THIRD_PARTY_LICENSES/README.md), including the Envisat image's ESA non-commercial informational-use restriction.

## The whole constellation, not just the sky above you

The observer's horizon only decides what can be *seen* from a place; it does not decide what exists in orbit. The globe therefore paints the complete Starlink shell: `StarlinkTrainManager.computeConstellationPoints` sweeps every cached TLE once per three-second bucket on `Dispatchers.Default`, reduces each TEME state to a WGS-84 sub-satellite point plus an Earth-shadow flag, and hands the fixes to `SatelliteEarth3D`. The renderer caches their model-space vectors and draws them as two batched point clouds (sunlit / in shadow) beneath the graticule and the featured markers, so the ~8,000-dot layer stays a couple of draw calls instead of a per-marker pass at 60 FPS. Starlinks above the horizon, active trains and the curated fleet keep their own brighter markers and remain the only tappable targets, so the dense layer never steals a tap from a real marker. The layer follows the category scope — it is painted for All and Starlink and absent for ISS, Tiangong, Hubble, Earth-observation, and the naked-eye-only scope, which stays an observation shortlist.

## Filters: choose what the globe paints

The `Tune` button on the header's observer line opens `SatPanel.Filters`, a popover with one switch per object family the map can paint: the Starlink constellation, the Starlinks above the observer, Starlink trains, ISS, Tiangong, Hubble and Earth observation. `SatGlobeFilters` stores only the *hidden* families, so the default is the whole sky, and the set is persisted as `sat_globe_hidden_layers` beside the scope and lighting preferences (the Bundle saver keeps it too, so rotation never resurrects a hidden layer). Filters prune the globe and its tap targets only: the Focus card, Fleet, search and the pass agenda keep the full scope, an unknown layer name from an older build is ignored rather than fatal, and the focused satellite keeps its marker and orbit track so the card never points at nothing. Hiding the constellation also skips the bulk SGP4 sweep, which is the cheapest way to quiet a slow device; `Show everything` restores the default set.

## Preview matrix

Open [`SatellitePreviews.kt`](../app/src/debug/java/com/alijafari/red/astronomy/ui/satellites/SatellitePreviews.kt) with the **debug** build variant in Android Studio and choose **Split** or **Design**. It provides a 72-preview matrix (3 skins × 3 palettes × 2 languages × 4 states), plus two narrow Persian 130% cases. The Default entry points are below; Spatial and Aether use the same names prefixed by their skin:

| Palette | Globe at rest | Time tray | Passes (half) | Detail |
|---|---|---|---|---|
| Dark | `DarkGlobe` | `DarkTime` | `DarkPasses` | `DarkDetail` |
| Light | `LightGlobe` | `LightTime` | `LightPasses` | `LightDetail` |
| Wine | `WineGlobe` | `WineTime` | `WinePasses` | `WineDetail` |

Each reference preview has variants for English LTR and Persian RTL at 390 × 844 dp. The previews use the production HUD, sheet, pass-card, Detail and nav composables with a fixed clock and pass fixture. Layoutlib cannot host the native EGL viewport, so only the Earth image is substituted with debug-only stills baked from the production textures. Neither the stills nor the fixtures are included in the release APK.

## Checks

- `SatellitesRedesignTest`: state exclusivity, spotlight ordering, observer sunset/sunrise (including DST and polar day), time magnets, geographic bearings, contrast ratios, and free-area globe framing.
- `SatSignedTextTest`: the real Android bidi formatter, Persian numerals and the Unicode minus.
- `SatelliteHudRenderTest`: production HUD layout, floating-nav clearance, panel switching, and an RTL scrubber at 130% font size. Use `./gradlew testDebugUnitTest --tests '*SatelliteHudRenderTest*' -Proborazzi.test.record=true -Pgravity.ci.tests=false` to save review frames to `app/build/outputs/satellite-previews/`.
- `SatReminderPersistenceTest`: one manual lead replaces rolling coverage, and turning monitoring off preserves manual alarms.
- `Earth3DTest` and `StarlinkPipelineTest`: projection and occlusion, and preservation of the existing Earth and orbit data paths.
- `SatGlobeFiltersTest`: filter semantics and toggling, device and Bundle persistence, unknown-layer tolerance, the globe/show mapping per family, and the button, popover and bilingual strings being wired.
- `StarlinkConstellationGlobeTest`: the bulk shell sweep returns every satellite regardless of the observer, memoizes per bucket, drops decayed entries, keeps the allocation-free projection equal to `projectLatLon`, and guards the stage/globe wiring.
- On-device review: verify pinch and double-tap outside the HUD, tap overlapping markers twice, cycle three satellites with Focus Card swipes, scrub while watching the Earth and dismiss without returning to Live, and verify a scheduled reminder after leaving and re-entering the tab. Repeat on a narrow Persian screen at 130% font scale and with device animations disabled.

The tests use a still globe, not the native touch/render surface. For short landscape/large windows, the resting globe fits the height available between the header and Focus Card instead of cropping a width-sized sphere. Pinch remains available to zoom in.

```sh
./gradlew testDebugUnitTest -Pgravity.ci.tests=false
./gradlew assembleRelease
```

GitHub Actions signs and publishes the installable release in its `ZIG-real-app-apk` artifact. The standard local release uses debug signing when the project's signing configuration is not supplied.
