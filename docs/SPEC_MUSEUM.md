# SPEC_MUSEUM — Space Museum Feature as It Will Exist in This Repository

Date: 2026-09-14
M-1 — Read-only reconnaissance
Repository: ZIG, single module :app, package com.alijafari.red.astronomy, plus com.zig.gravity

## 1. Overview

Space Museum is a new Lab-screen destination inside existing ZIG app. User flow:
- Lab screen (tab 0) → one new card "Space Museum" (available, not Coming Soon)
- Tap card → Space Museum grid screen: 13 tiles, name + data ceiling subtitle, museum header
- Tap tile → Viewer screen: one high-fidelity object at a time, orbit/pinch/tap-to-focus/double-tap-reset, rotation control, sun-direction control, layer toggles, camera presets, data HUD, sources & credits
- System back: Viewer → Grid → Lab

Fully offline after install, no INTERNET permission in release for new modules, no network calls, no WebView.

## 2. Module Graph (Adapted to Real Repo)

Real repo has only `:app`. We create new modules additively, matching roadmap shape §4.1 but under real repo's conventions.

Root `settings.gradle.kts` will include:

```
include(":app")
include(":core:model")
include(":core:engine")
include(":core:data")
include(":core:credits")
include(":feature:museum")
include(":feature:viewer")
include(":tools:assetkit")
include(":tools:blackhole-lut")
include(":tools:ci")
```

Directory layout:

```
<root>/
  core/
    model/      -> :core:model (pure Kotlin, no Android)
    engine/     -> :core:engine (Filament wrapper, owns all com.google.android.filament.*)
    data/       -> :core:data (manifests, packs, tile store)
    credits/    -> :core:credits (manifest -> UI)
  feature/
    museum/     -> :feature:museum (grid + navigation)
    viewer/     -> :feature:viewer (render surface + controls)
  tools/
    assetkit/   -> :tools:assetkit (JVM CLI, offline, never ships)
    blackhole-lut/ -> :tools:blackhole-lut (JVM CLI, offline)
    ci/         -> :tools:ci (provenance + scope gates, JVM)
  manifests/    -> provenance manifests (versioned)
  assets-src/   -> downloaded source data (git-ignored)
  assets-built/ -> built packs .zigpack (git-ignored, checksummed)
  docs/
    SPEC_MUSEUM.md (this file)
    SOURCES.md
    decisions/DECISIONS.md
    integration/CHANGED_FILES.md, INVENTORY.md, etc.
    verification/M<N>/
```

### Module Rules (from §4.2, enforced)

- `:core:model` has no Android dependency. Pure Kotlin data classes only: ObjectSpec, QualityTier, etc.
- `:core:engine` owns every Filament type. Nothing outside this module imports `com.google.android.filament.*`. Interface `InspectorEngine`, `ObjectSource`, etc.
- `:core:data` owns manifests, pack discovery, tile store storage side.
- `:feature:*` contains UI and state; talks to `:core:engine` through interfaces defined in `:core:engine` (no direct engine calls from Composables except via wrapper).
- `:tools:*` are JVM-only, never shipped, runnable via `./gradlew :tools:assetkit:run --args=...`
- No existing module gains dependency on museum modules except `:app`, and no museum module depends on existing feature module (e.g., no dependency on `com.alijafari.red.astronomy.astro_engine` or `com.zig.gravity.*`).
- `:app` depends on `:core:model`, `:core:credits`, `:feature:museum`, `:feature:viewer`, `:core:engine`, `:core:data` (when needed).

Package naming:
- ApplicationId remains `com.alijafari.red.astronomy` (existing)
- New Kotlin packages: `com.zig.museum.core.model`, `com.zig.museum.core.engine`, `com.zig.museum.core.data`, `com.zig.museum.core.credits`, `com.zig.museum.feature.museum`, `com.zig.museum.feature.viewer`, `com.zig.museum.tools.assetkit`, etc. (mirror module paths, per §4.3 but adapted from `com.zig.inspector` to `com.zig.museum` to avoid collision with existing `com.alijafari.red.astronomy`).

## 3. Object Registry (Spine)

File: `core/model/src/main/kotlin/com/zig/museum/core/model/ObjectRegistry.kt`

```kotlin
data class ObjectSpec(
  val id: String, // e.g., "sun", "mercury", ... "black_hole"
  val displayNameEn: String,
  val displayNameFa: String,
  val sceneRadiusMetres: Double, // mean radius, IAU WGCCRE + NASA fact sheets
  val oblateness: Double, // (a-b)/a
  val axialTiltDeg: Double,
  val rotationPeriodHours: Double, // signed for retrograde (Venus, Uranus)
  val packId: String,
  val qualityTiers: List<QualityTier>,
  val dataCeilingTextEn: String, // e.g., "0.5 m/px at Apollo sites"
  val dataCeilingTextFa: String,
  val credits: List<String> // placeholder, real credits from manifest
)

enum class QualityTier { TIER_0, TIER_1, TIER_2, TIER_3 }

object ObjectRegistry {
  val all: List<ObjectSpec> = listOf(
    // 13 entries: Sun, Mercury, Venus, Earth, Moon, Mars, Jupiter, Saturn, Uranus, Neptune, Milky Way, ISS, Black Hole
  )
  fun byId(id: String): ObjectSpec? = all.find { it.id == id }
}
```

Values cited from IAU WGCCRE and NASA planetary fact sheets, with comment next to each numeric literal per §4.4.

This file is single source of truth for UI, loader, credits.

## 4. UI Structure

### 4.1 Lab Screen Card (Integration Point)

File: `app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt`

- Enum `LabFeatureType` gets new entry:

```kotlin
SPACE_MUSEUM(
    titleEn = "Space Museum",
    titleFa = "موزه فضا",
    subtitleEn = "Thirteen Worlds, One at a Time",
    subtitleFa = "سیزده جهان، یک به یک",
    descriptionEn = "Inspect high-fidelity, offline 3D models from the Sun to a black hole.",
    descriptionFa = "مدل‌های سه‌بعدی آفلاین و دقیق از خورشید تا سیاه‌چاله را کاوش کنید.",
    icon = Icons.Default.RocketLaunch, // or Museum, to be verified
    isAvailable = true
)
```

- In `LabScreen` composable, add:

```kotlin
if (selectedFeature == LabFeatureType.SPACE_MUSEUM) {
    com.zig.museum.feature.museum.SpaceMuseumRoot(
        onBack = { selectedFeature = null },
        isFa = isFa,
        isDark = uiState.themeMode != ThemeMode.LIGHT
    )
}
```

- Card appears alongside existing cards, same style, testTag `lab_feature_card_space_museum`. Available cards first, then Coming Soon.

### 4.2 Space Museum Grid Screen

Module: `:feature:museum`
File: `feature/museum/src/main/kotlin/com/zig/museum/feature/museum/SpaceMuseumGridScreen.kt`

- Composable `SpaceMuseumRoot` that hosts grid and viewer navigation internally (or via simple state). For M0, just grid + empty viewer.
- Grid: `LazyVerticalGrid(columns = GridCells.Adaptive(minSize=148.dp))`, contentPadding horizontal lg vertical sm, verticalArrangement spacedBy lg, same as LabScreen.
- Header: `RedElevatedCard` with museum icon, title "Space Museum" / "موزه فضا", subtitle "Offline, high-fidelity 3D — one object at a time"
- Tiles: `MuseumTile` composable — similar to LabFeatureCard but smaller, with name and ceiling subtitle. Test tags `space_museum_tile_${id}`. All 13 tiles visible with zero packs installed (no network).
- On tile click: `onObjectClick(spec.id)` navigates to viewer.
- Reuses RED design system: `RedTheme`, `RedSpacing`, `RedCornerRadius`, `RedTypographyTokens`.

### 4.3 Viewer Screen

Module: `:feature:viewer`
File: `feature/viewer/src/main/kotlin/com/zig/museum/feature/viewer/SpaceMuseumViewerScreen.kt`

- M0: empty viewer with top bar (object name, back, credits icon), bottom sheet placeholder, and a Box saying "Viewer coming in M1" — proves navigation works.
- M1+: hosts Filament surface via `InspectorEngine` from `:core:engine`, Compose `AndroidView` with SurfaceView, lifecycle handling.
- Controls per §5.9 and §10.3:
  - Top bar: object name, back, credits (i)
  - Bottom sheet or rail: rotation control (1x/60x/3600x/hold), sun-direction (azimuth, elevation, EV), layer toggles, camera presets
  - Data HUD: "DATA 166 m/px (MESSENGER MDIS BDR)" updating with zoom, "beyond published data - procedural detail" indicator past ceiling, toggle to switch procedural off
  - Debug overlay (debug builds only): fps, frame time p50/p95, resident tile count, VRAM estimate
- Camera: orbit camera per §5.4, tap-to-focus, double-tap reset, zoom limits min=(2*data resolution in radii) max=3.0 radii
- Lighting: one directional sun light scaled by real distance per §5.6, EV slider, fill light only where justified (earthshine for Moon)

### 4.4 Sources & Credits

Module: `:core:credits`
- Manifest reader that reads `manifests/*.json` or packs' `manifest.json`
- UI screen listing one row per shipped asset: object, product, publisher, source URL, credit string, what data means, processing applied. Generated from manifests, never hand-written.

## 5. Engine & Data

### Engine (:core:engine)
- `InspectorEngine` class: owns single Filament Engine, creates off main thread, scene/view/camera/renderer, Choreographer frame callback, lifecycle release paths in same commit.
- Unit-radius normalisation per §5.3: object mean radius maps to 1.0 scene unit, camera in radii, near 0.0005-0.05, far 8.0.
- HDR pipeline per §5.5: clear to deep space black (non-zero), skybox, opaque object, opaque overlays, translucent layers (cloud, atmosphere back, ring, atmosphere front), emissive additions, post bloom->tone mapping->color grading->TAA->dither->UI overlay. Tone mapper switch AgX / PBR Neutral.
- Materials per §5.7: M1 regolithSurface, M2 icySurface, M3 gasGiantSurface, M4 cloudDeck, M5 ringTransmission, M6 solarSurface, M7 coronaShell, M8 nightLights, M9 starSprite, M10 atmosphereShell, M11 blackHoleLens, M12 modelSurface, M13 patchSurface. Compiled offline with matc to .filamat, shipped precompiled.
- Object loader: `ObjectSource` interface with `load(spec, quality): ObjectHandles` and `release(handles)`, implementations SphereSource, RingedSource, ModelSource, SkySource, LensingSource. Cancelable via structured concurrency.

### Data (:core:data)
- Tile pyramid per §6.4: 512x512 texels, apron 4, L0 native resolution halved until <=512, naming tiles/L<level>/<x>_<y>.ktx2, seam duplication, pole handling documented in manifest.
- Encoding per §6.5: albedo sRGB ASTC 6x6 (4x4 hero), normal R8G8_UNORM UASTC quality 4, height R8_UNORM ASTC 6x6, HDR sky R11F_G11F_B10F, LUTs R32G32_SFLOAT / R16_SFLOAT. Mips in linear space.
- Pack format per §6.3: .zigpack ZIP (stored, no recompression) containing manifest.json, maps/*.ktx2, tiles/L<x>/<y>.ktx2, luts/*.ktx2, meta/tiers.json. Usable with plain ZipFile.
- Tile store per §7: TileKey, TileEntry, state machine REQUESTED/DECODING/RESIDENT/EVICTABLE, worker decode 3 threads IO dispatcher, upload budget 2 per frame tier0 else 1, LRU eviction protected visible set, prefetch along camera motion, fallback to coarser levels.

### Asset Kit (:tools:assetkit)
- CLI verbs per §6.2: fetch, preprocess, tiles, encode, horizon, normal, pack, verify, atmosphere, all with --dry-run
- Deterministic, reproducible: same inputs+config => byte-identical outputs, verified by hashing twice.
- Implements reprojection to equirectangular, linear-space mips, horizon maps (16 azimuths), normal maps.

### Black Hole LUT (:tools:blackhole-lut)
- Generates D(e,u) and U(e,phi) tables + blackbody colour table, --verify verb vs direct integration.

### CI Gates (:tools:ci)
- `check_provenance.py` per §16.3: fails if missing product/publisher/url/credit, url not plausible, SHA mismatch, asset referenced without manifest entry. Prints asset count, zero is failure.
- `check_scope.py` per §16.4: fails if pre-existing file modified without CHANGED_FILES.md entry, diff larger than reason justifies, module gains illegal dependency, existing test/string/asset/build config modified.

## 6. Per-Object Summary (Data Ceilings & Materials)

- Sun: SDO/AIA+HMI 4096, Carrington EUV SVS 30362, sphere 64x32 base, M6+M7, emissive, controls: channel selector (304,171,etc), EUV mode, corona toggle
- Mercury: MESSENGER MDIS BDR 166 m/px, ellipsoid, M1, horizon shadows
- Venus: Magellan FMAP 75m radar + Akatsuki UV, sphere, M1/M3 hybrid, modes true radar vs UV enhanced, labelled
- Earth: Landsat 15m albedo + Blue Marble + night lights + SRTM/ETOPO DEM, ellipsoid, M1+M4+M8+M10, layers: day, night, clouds, atmosphere, controls: ozone, dust opacity
- Moon: LRO WAC 100m + LOLA DEM + NAC 0.5m hero sites, sphere + patches, M1+M13, horizon maps
- Mars: CTX 6m mosaic + HRSC + HiRISE 0.25m hero, sphere + patches, M1
- Jupiter: OPAL 0.1deg/px + JunoCam, oblate ellipsoid, M3+M10, wind LUT shear
- Saturn: Cassini ISS maps + PDS tau profiles, oblate + rings, M3+M5, both shadow directions
- Uranus: OPAL + Voyager2, strongly oblate 97.77deg tilt, M3+M5, 13 narrow rings
- Neptune: OPAL + Voyager2 mosaics 1989 epoch, ellipsoid, M3+M5, Adams ring arcs
- Milky Way: SVS Deep Star Maps 2020 item 4851 (non-Gaia layers) + Gaia DR3 + DSS2 + gigapixel panorama + UCAC4/BSC5 + IAU constellations, inside-out sphere, unlit skybox + M9 starSprite + constellation lines, controls magnitude limit, deep map on/off, figures/boundaries/labels, star picking
- ISS: NASA VTAD ISS_stationary.glb 44.5MB 247k tris 28 PBR mats + NASA 3D Resources, glTF with 4 LODs offline meshoptimizer, M12 with clearcoat MLI, metallic Kapton foil anisotropic, roughened white truss, metre scale bar, module labels, lighting presets, NO orbit/position logic
- Black Hole: EHT M87* 42±3 µas and Sgr A* releases as appearance reference, Bruneton 2020 precomputed-deflection method, Schwarzschild b_c=3√3M shadow radius 2.598 r_s, D(e,u) 512x512 RG32F, U(e,phi) 64x32, blackbody table 256 RGBA16F, M11 skybox lensing + disk geometry, modes physically correct, cinematic (labelled artistic variant per James et al 2015 a/M=0.6 6500K), M87-like, Sgr A*-like, tiers, quality slider

## 7. Navigation & Integration (Real Repo Wins)

- Lab screen is source of truth: enum LabFeatureType, selectedFeature state, LabFeatureCard UI, testTag pattern.
- Museum card added as new enum entry SPACE_MUSEUM, isAvailable true, icon distinct.
- No Navigation Compose routes — museum navigation is internal state: Grid ↔ Viewer, back to Lab via onBack callback.
- FloatingBottomBar hiding: reuse `com.zig.gravity.ui.ImmersiveScreenState.active` — set true in viewer, false on exit, no edit to MainActivity needed for M0/M1. If separate state needed later, MainActivity may be edited with justification in CHANGED_FILES.md.
- No changes to existing tabs, strings, assets, tests, build config except settings.gradle.kts and app/build.gradle.kts dependencies and LabScreen.kt.

## 8. Offline & Performance

- Fully offline after install: no INTERNET permission in new modules' release manifests, no HTTP client, no WebView. Existing app manifest has INTERNET for other features, but new modules must not introduce new permission; merged manifest must still have INTERNET because existing app needs it, but gate checks that new modules don't add it? Actually spec says release build must not request INTERNET permission (§2.2 T7) — but existing app already requests INTERNET. Difference: roadmap §26.6 says apkanalyzer shows no INTERNET in release variant — but existing ZIG has INTERNET. This is a repository-wins-over-brief difference. Real repo has INTERNET for TLE sync, etc. Museum feature itself must not require INTERNET and must work in airplane mode. The gate check for INTERNET must be adapted: check that museum modules don't add INTERNET, and that museum works offline, but overall app may still have INTERNET because existing features need it. Record difference in DECISIONS.md.
- Performance budgets per §15.2: frame time p95 16.6ms tier0/1, 33ms tier2/3, resident GPU textures under 1.5GB/900MB/500MB/250MB, draw calls under 30/30/25/20, triangles up to 4M/1.5M/600k/300k, uploads 2/1/1/1, first pixel <400ms tier0/1 <600ms tier2 <800ms tier3, full detail <6s/<6s/<8s/<10s.
- Instrumentation per §15.3: frame timing ring buffer 300 frames, tile store counters, GPU mem estimate, debug overlay, headless benchmark mode `--benchmark <object> --tier <n> --seconds <n>` CSV.

## 9. Provenance & Scope

- Every asset that ships needs manifest entry: product name, publisher, credit string, URL. Manifest drives Sources & Credits UI.
- SOURCES.md lists all planned assets with product, publisher, URL (traceability, not licence record).
- Scope gate ensures no irrelevant touches.

## 10. Milestone Mapping to This Repo

- M-1: docs only, no code
- M0: scaffold modules, registry with 13 entries, museum grid + empty viewer, Lab card, provenance+scope gates with failing fixtures, credits reader, no INTERNET in new modules, existing tests green
- M1: engine core, HDR, camera, first object
- M2: assetkit CLI, one real small pack
- M3: tile store critical path
- M4: materials M1 fully + M3-M13
- M5: controls and HUD
- M6: rocky trio Moon/Mars/Mercury
- M7: Earth/Venus/Sun
- M8: giants and rings
- M9: Milky Way and ISS
- M10: black hole
- M11: performance, thermals, device matrix
- M12: polish, accessibility, release prep

## 11. Open Questions / Repository Wins Over Brief

- Brief says application id com.zig.inspector, real is com.alijafari.red.astronomy — use real.
- Brief says no INTERNET permission in release, real app has INTERNET — museum must work offline in airplane mode, but overall manifest will still have INTERNET for existing features. Difference recorded in DECISIONS.md.
- Brief shows multi-module core/designsystem etc., real repo single-module — we create new modules additively.
- Brief shows navigation routes composable(Routes.SpaceMuseumGrid) — real repo uses tab index + selectedFeature state — we follow real repo.
- Brief shows LabCardModel with titleRes etc. — real repo uses LabFeatureType enum with hardcoded bilingual strings — we follow real repo.

## 12. Deliverables for M-0

- New modules with build.gradle.kts
- ObjectRegistry with 13 specs, cited sources
- SpaceMuseumRoot composable with grid (13 tiles) + empty viewer
- LabScreen.kt edit: SPACE_MUSEUM enum + branch
- settings.gradle.kts and app/build.gradle.kts edits
- tools/ci gates with fixtures
- core/credits manifest reader + empty credits screen
- Docs updated, tests green
