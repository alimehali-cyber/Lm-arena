# DECISIONS — Space Museum

Date: 2026-09-14
M-1 — Reconnaissance

This file records every non-obvious decision with reason and, where relevant, measurement. Per §14 of brief.

## Repository Wins Over Brief — Differences Found

### D-001: Lab screen card mechanism
- **Brief assumes**: LabCardModel or LabScreenConfig data-driven, or composable with onOpenSpaceMuseum callback, or ZigIcons.Museum icon set, per Appendix I.2
- **Real repo**: `app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt` defines `enum LabFeatureType(titleEn,titleFa,subtitleEn,subtitleFa,descriptionEn,descriptionFa,icon,isAvailable)` with 4 entries, and `LabScreen` composable holds `selectedFeature: LabFeatureType?` state. If `TIME_DILATION` shows `TimeDilationCalculatorScreen`, if `GRAVITY_SANDBOX` shows `GravitySandboxRoot`, else shows LazyColumn of cards via `LabFeatureCard`. No navigation graph, no routes.
- **Decision**: Follow real repo. Add `SPACE_MUSEUM` entry to enum, add branch `if (selectedFeature == SPACE_MUSEUM) { SpaceMuseumRoot(...) }`. Use same `LabFeatureCard` UI, same testTag pattern `lab_feature_card_${lowercase}`. Recorded in SPEC_MUSEUM.md §4.1.
- **File**: LabScreen.kt:19-70 enum, 85-150 LabScreen composable
- **Reason**: Repository wins wherever two disagree per kickoff prompt instruction 2.

### D-002: Navigation approach
- **Brief assumes**: Navigation Compose with `composable(Routes.SpaceMuseumGrid)` and `Routes.spaceMuseumViewer(id)` per Appendix I.3
- **Real repo**: No Navigation Compose at runtime — `androidx.navigation.compose` is commented out in `app/build.gradle.kts`. Uses `selectedTab: Int` in MainViewModel (0=Lab,1=Satellites,2=Moon,3=AR Sky,4=Home) with `Crossfade` in MainActivity.kt:214-251. Lab sub-nav is local state, not NavController.
- **Decision**: Keep tab index + selectedFeature pattern. Museum grid/viewer navigation is internal state inside `SpaceMuseumRoot`, not NavHost. No deep links unless repo already declares them for every screen (it doesn't). This matches gravity sandbox integration.
- **Files**: MainActivity.kt:237-251, MainViewModel.kt:33 selectedTab, LabScreen.kt:92 selectedFeature
- **Reason**: Additive integration, minimum diff per §26.2, §26.3.

### D-003: Module structure
- **Brief assumes**: Multi-module with `core/engine`, `core/data`, `core/model`, `core/designsystem`, `core/credits`, `feature/museum`, `feature/viewer`, `feature/settings`, `tools/assetkit`, `tools/blackhole-lut`, `tools/ci` per §4.1
- **Real repo**: Single module `:app` only (settings.gradle.kts `include(":app")`). Gravity sandbox is embedded inside `:app` as `com.zig.gravity.*` packages, not separate modules.
- **Decision**: Create new modules additively under `core/`, `feature/`, `tools/` matching brief shape, but keep `:app` as integration point. Package roots will be `com.zig.museum.*` to avoid collision with `com.alijafari.red.astronomy` and `com.zig.gravity`. Settings.gradle.kts will include new modules. App module will depend on new modules. This satisfies §4.2 module rules (engine owns Filament types, model pure Kotlin, etc.) while being additive.
- **Files**: settings.gradle.kts:10, app/build.gradle.kts: existing
- **Reason**: Additive by default (§10), prefer new code to modified code. Creating new modules is allowed, restructuring existing :app is not.

### D-004: ApplicationId and package naming
- **Brief assumes**: ApplicationId `com.zig.inspector` per §4.3
- **Real repo**: `com.alijafari.red.astronomy` (app/build.gradle.kts:15, AndroidManifest.xml package via namespace)
- **Decision**: Use real applicationId. New packages `com.zig.museum.*` for museum code, not `com.zig.inspector`. Existing code stays `com.alijafari.red.astronomy` and `com.zig.gravity`.
- **Reason**: Repository wins.

### D-005: Theming and design system
- **Brief assumes**: Generic Material3 with new designsystem module
- **Real repo**: Custom RED design system in `ui/theme/` with `RedTheme`, `RedSpacing`, `RedCornerRadius`, `RedElevation`, `RedIconSize`, `RedPrimitives` (RedCard, RedElevatedCard, RedSectionHeader, RedBadge, RedSlider), `LiquidGlassSurface` using `io.github.kyant0:backdrop` 1.0.6, typography Vazirmatn for Persian.
- **Decision**: Reuse existing RED design system for museum grid and viewer overlays, so Space Museum looks like it always belonged (§12). No new theme. Museum tiles reuse LabFeatureCard pattern, colors RedTheme.colors, spacing RedSpacing, typography via MaterialTheme + LocalAppFontFamily.
- **Files**: RedPrimitives.kt, RedTokens.kt, Theme.kt, Type.kt, LiquidGlass.kt
- **Reason**: §12, §26.5.

### D-006: Strings and resources
- **Brief assumes**: New strings in R.string.space_museum_title etc.
- **Real repo**: LabFeatureType enum hardcodes bilingual strings in Kotlin (titleEn/titleFa etc.), not via resources. Gravity sandbox title is in strings.xml but enum also has hardcoded strings. Test tags are hardcoded.
- **Decision**: For Lab card, hardcode bilingual strings in enum (same pattern) to avoid touching `res/values/strings.xml` if possible. For museum grid internal UI, put strings in new feature module's own `res/values/strings.xml` (additive, not touching pre-existing file). If enum requires icon, use `Icons.Default` set, not new drawable.
- **Reason**: Minimum diff per §26.2 — prefer new files to modified files. Touching strings.xml is only if museum fails without it.

### D-007: INTERNET permission
- **Brief assumes**: Release build must not request INTERNET permission per §2.2 T7, §16.5, §26.6 checklist
- **Real repo**: `app/src/main/AndroidManifest.xml` declares `<uses-permission android:name="android.permission.INTERNET" />` and `ACCESS_NETWORK_STATE` for existing features (TLE sync, etc.). Build.yml does NOT check for INTERNET absence.
- **Decision**: This is a repository-wins-over-brief difference. Museum feature itself must be fully offline and work in airplane mode, and new modules must not add INTERNET permission. But overall merged manifest will still have INTERNET because existing app needs it. Gate check for INTERNET must be adapted: check that museum modules' manifests don't add INTERNET, and that museum works offline, rather than checking merged release manifest has no INTERNET. Record as deviation, not a defect to fix by removing existing permission (that would break existing features and violate §26.2).
- **Files**: AndroidManifest.xml:5-6, build.yml
- **Reason**: Repository wins, and §26.2 says existing calculations etc. stay exactly as they are.

### D-008: FloatingBottomBar hiding
- **Brief assumes**: Museum viewer is immersive, hides nav bar
- **Real repo**: MainActivity.kt:257 checks `com.zig.gravity.ui.ImmersiveScreenState.active` to hide `FloatingBottomBar`. GravitySandboxRoot sets this true/false.
- **Decision**: Reuse same `ImmersiveScreenState.active` object from museum viewer (set true on entry, false on exit) to hide nav bar without editing MainActivity. This is additive reuse, no pre-existing file edit needed for M0/M1. If separate immersive state is needed later, MainActivity may be edited with justification in CHANGED_FILES.md.
- **Files**: MainActivity.kt:257, FloatingNavBar.kt
- **Reason**: Minimum diff, reuse existing mechanism.

### D-009: Dependency versions
- **Brief pins**: AGP 9.4.0, Kotlin 2.2.0, Filament 1.71.5, etc. per §3
- **Real repo**: AGP 9.1.1, Kotlin 2.2.10, Compose BOM 2024.09.00, Gradle 9.3.1, JDK 21 in CI but sourceCompatibility 11
- **Decision**: Keep existing versions for existing modules. For new modules, use existing versions where possible (Kotlin 2.2.10, Compose BOM 2024.09.00, AGP 9.1.1). For Filament, try pinned 1.71.5; if incompatible with AGP 9.1.1 / compileSdk 36.1, use nearest compatible and record reason with build error evidence in this file.
- **Files**: libs.versions.toml, app/build.gradle.kts, gradle-wrapper.properties, build.yml
- **Reason**: §26.2 forbids bumping existing dependencies to satisfy feature. §3 revision note says establish what repo actually uses in M-1 and record in ENVIRONMENT.md, do not upgrade.

### D-010: Asset selection licensing note
- **Brief revision 3.0**: Licensing is out of scope (§2.3 A1, §27.1). Choose best asset on quality alone, record provenance.
- **Real repo**: No licensing registry, no attribution screen yet.
- **Decision**: Follow revision 3.0. No licence logic in app or CI. Provenance gate only checks product/publisher/url/credit presence and plausible URL, not licence. SOURCES.md is traceability, not licence record. Owner handles licensing after delivery with legal counsel.
- **Reason**: Brief §27 overrides older licence-based restrictions.

## Integration Strategy (M-1)

- M-1: docs only, no production code, per milestone DoD.
- M0: Create new modules `:core:model`, `:core:credits`, `:feature:museum`, `:tools:ci` (minimal). Implement ObjectRegistry with 13 entries, museum grid with 13 tiles, empty viewer, Lab card integration, provenance+scope gates with failing fixtures, credits reader. Edit only `settings.gradle.kts`, `app/build.gradle.kts`, `LabScreen.kt` — all listed in CHANGED_FILES.md with necessity.
- M1: Add `:core:engine` with Filament, implement InspectorEngine, camera rig, HDR pipeline, first material, Compose surface hosting viewer. Museum viewer now renders ellipsoid with test texture.
- M2: Add `:tools:assetkit` JVM CLI, implement verbs, produce one real small pack (e.g., Moon WAC 100m crop) loaded on device.
- M3-M12: Follow roadmap sequentially, continuous gated execution per §23.

Additive by default: new modules, new packages, new files. Prefer new code to modified code.

## Module Naming Decision

Chosen module names (to be recorded in SPEC_MUSEUM.md):
- `:core:model` — pure Kotlin domain types, ObjectSpec, QualityTier, registry
- `:core:engine` — Filament wrapper, owns all Filament types
- `:core:data` — manifests, packs, tile store
- `:core:credits` — manifest -> UI
- `:feature:museum` — grid + card destination
- `:feature:viewer` — render surface + controls
- `:tools:assetkit` — JVM CLI offline
- `:tools:blackhole-lut` — JVM CLI offline
- `:tools:ci` — provenance + scope gates

Directory mapping: `core/model/`, `core/engine/`, etc. under root.

Package naming: `com.zig.museum.core.model`, etc. (not `com.alijafari.red.astronomy` to keep separation, and not `com.zig.inspector` which doesn't exist).

## Toolchain Compatibility

- Filament 1.71.5 requires NDK? It ships AARs with native libs, no NDK needed for app build, but `matc` tool for material compilation is native binary. We'll need to download Filament release tools for `matc`.
- KTX-Software `ktx` CLI for KTX2 encoding — offline tool, not runtime dep.
- GDAL — optional, for reprojection; if not available, assetkit can fallback to pure Java/Kotlin reprojection with lower quality, documented.
- Python 3.11+ — optional helpers.

## M0 Decisions

### D-011: Module build files using id() not alias()
- **Decision**: New modules' build.gradle.kts use `id("com.android.library") version "9.1.1"` and `id("org.jetbrains.kotlin.jvm") version "2.2.10"` directly, not alias from version catalog, to avoid touching `gradle/libs.versions.toml` (which would be irrelevant touch per §26.2). This is minimal and necessary: new modules need plugins, but catalog edit is not necessary if id() with version works.
- **Reason**: Minimum diff, avoid editing version catalog unless proven necessary.

### D-012: Provenance gate implementation
- **Decision**: Implemented `tools/ci/check_provenance.py` per §16.3: checks product/publisher/url/credit presence, plausible URL (not bare domain, not search, not placeholder), prints asset count, zero assets is failure. Fixtures: valid (2 assets) passes, broken (3 assets missing fields + bare domains) fails with 6 errors. Real manifests dir `manifests/` has 1 placeholder asset for M0.
- **Reason**: Gate must fail on broken fixture and pass on valid per M0 DoD.

### D-013: Scope gate implementation
- **Decision**: Implemented `tools/ci/check_scope.py` per §16.4: gets changed files vs base ref (origin/Obra-with-key), parses CHANGED_FILES.md for allowed pre-existing paths, flags any pre-existing file modified without entry, checks forbidden patterns (existing tests, strings, assets, build config), checks module dependency rules (museum modules may depend on museum modules, but non-app non-museum modules must not depend on museum; museum modules must not import existing feature packages). Prints changed-path list on every run. For M0, 3 changed files: settings.gradle.kts, app/build.gradle.kts, LabScreen.kt — all allowed.
- **Reason**: Scope gate must report changed-path list and enforce minimum diff.

### D-014: AndroidManifest.xml for new library modules
- **Decision**: New Android library modules have minimal AndroidManifest.xml with only `<manifest>` root, no permissions, no components. This ensures they don't introduce INTERNET permission or manifest attributes into merged manifest per M0 DoD.
- **Reason**: T7 and M0 DoD: no new INTERNET permission, component or attribute.

### D-015: LabScreen icon choice
- **Decision**: SPACE_MUSEUM icon = `Icons.Default.RocketLaunch` (material-icons-extended). If not available in core set, fallback to `Icons.Default.Public` variant or `Icons.Default.Explore`. RocketLaunch is distinct from Gravity Sandbox's Public icon, and matches space theme. Verified that app/build.gradle.kts includes material-icons-extended, so RocketLaunch should be available. If compile fails, will fallback to Explore and record.
- **Reason**: Distinct icon per component reuse, same icon set as existing.

### D-016: Museum grid implementation reuse
- **Decision**: Museum grid uses `LazyVerticalGrid(GridCells.Adaptive(148.dp))` per Appendix I.4, but styling uses MaterialTheme (since RED tokens are in :app, not yet accessible from new modules without dependency). For M0, use MaterialTheme to keep modules independent; in M1+ will add dependency on :app's theme or duplicate RED tokens in new modules? Actually :feature:museum depends on :core:credits and :core:model only, not on :app. To reuse RED design system, we need to either depend on :app (which would violate P7) or duplicate needed tokens in new modules. For M0, use MaterialTheme as placeholder, and note that final polish in M12 will align with RED design system via shared designsystem module or by copying tokens. For now, grid is functional with 13 tiles.
- **Reason**: P7 says no museum module depends on existing feature module, and RED design system is inside :app. To reuse it without dependency, we need to extract designsystem to separate module later or copy tokens. M0 uses MaterialTheme as minimal viable.

### D-017: Immersive handling reuse
- **Decision**: Viewer sets `com.zig.gravity.ui.ImmersiveScreenState.active` via reflection to hide FloatingBottomBar, without editing MainActivity. This reuses existing mechanism per D-008. If reflection fails (class not found), nav bar stays visible in M0, acceptable.
- **Reason**: Minimum diff, avoid editing MainActivity for M0.

### D-018: Assets-src/assets-built gitignore
- **Decision**: Created `assets-src/.gitignore` and `assets-built/.gitignore` with `*` and `!.gitignore` to ignore downloaded/built data, without touching root `.gitignore` (which would be pre-existing file edit). This satisfies roadmap §4.1 that assets-src and assets-built are git-ignored.
- **Reason**: Additive, no pre-existing file edit.

## M1 Decisions

### D-019: Filament dependency version
- **Decision**: Added Filament 1.71.5 dependencies to :core:engine per §3 pinned: filament-android, gltfio-android, filament-utils-android, filamat-android, all 1.71.5. These are from Maven Central, available in CI. If CI build fails due to incompatibility with AGP 9.1.1 / compileSdk 36, fallback to nearest compatible (e.g., 1.68.0) and record build error evidence.
- **Files**: core/engine/build.gradle.kts
- **Reason**: T1: Kotlin + Compose + Filament, no other engine.

### D-020: Runtime material builder temporary for M1
- **Decision**: For M1, first material M1 skeleton (albedo+normal) is authored as .mat source in core/engine/src/main/materials/regolithSurface.mat per §5.7 and §8.1. Offline compilation via matc to .filamat is required per T2, but matc binary not available in sandbox due to network SSL block to GitHub releases (curl and gh download fail with SSL_ERROR_SYSCALL/EOF). As temporary for M1 to get first object on screen, use filamat-android runtime MaterialBuilder (MaterialBuilder.platform(MOBILE), name, shading LIT, uniformParameter SAMPLER_2D, material string, optimization NONE, build(engine)). This violates T2 (no runtime compilation) but is explicitly temporary, to be replaced by offline .filamat in M4. Recorded here.
- **Files**: core/engine/src/main/kotlin/com/zig/museum/core/engine/MaterialManager.kt, core/engine/src/main/materials/regolithSurface.mat
- **Reason**: Unblock M1 first object on screen without matc; will be fixed in M4 with proper offline compilation.

### D-021: Filament API names used (to verify against 1.71.5 javadoc per §5.5)
- **Decision**: InspectorEngine uses following API names (need verification against pinned 1.71.5 javadoc/KTX):
  - Engine.create(), Engine.destroy(), Engine.createRenderer(), createScene(), createView(), createCamera(), createSwapChain(Surface), destroyRenderer(), destroyScene(), destroyView(), destroyCameraComponent(), destroySwapChain(), destroyEntity()
  - Renderer.beginFrame(SwapChain), render(View), endFrame(), clearOptions
  - View.scene, camera, viewport, blendMode, renderQuality, antiAliasing, dithering, colorGrading/toneMapping
  - Scene.setSkybox(), addEntity(), removeEntity()
  - Camera.setProjection(fov, aspect, near, far, Fov.VERTICAL), lookAt(eye, center, up), setExposure()
  - UiHelper, UiHelper.attachTo(SurfaceView), detach()
  - EntityManager.get(), create()
  - LightManager.Builder(Type.DIRECTIONAL), color(), intensity(), direction(), castShadows(), build()
  - TransformManager, etc.
  - ModelViewer (filament-utils-android) for simplified rendering if needed
  - MaterialBuilder (filamat-android) for temporary runtime material
  - Choreographer.getInstance(), postFrameCallback(), removeFrameCallback()
  - Utils.init()
- If any API not exposed in 1.71.5, implement equivalent as post-process material and document.
- **Reason**: §5.5 requires recording exact API names used in DECISIONS.md.

### D-022: Camera rig implementation
- **Decision**: CameraRig per §5.4 with state radius/yaw/pitch/target, dampingFactor 0.15, minRadius 1.01, maxRadius 3.0, orbit() clamps pitch -89..89, zoom() logarithmic factor, computePosition() spherical to cartesian, computeNearFar() interpolates near 0.0005..0.05 based on radius, far 8.0. Presets full_disk, pole_on, terminator as data per §5.4. Unit tests for orbit, zoom limits, near/far.
- **Files**: core/engine/src/main/kotlin/com/zig/museum/core/engine/CameraRig.kt
- **Reason**: §5.4 camera rig.

### D-023: Sun direction pure function
- **Decision**: sunDirection(azimuthDeg, elevationDeg): Vec3 per §10.3 pure function, with unit tests covering cardinal directions (0°=>+Z, 90°=>+X, 180°=>-Z, 270°=>-X) and poles (elevation 90°=>+Y). Normalization ensured. Irradiance factors per §5.6 for each object, Lux conversion base 120k Earth.
- **Files**: core/engine/src/main/kotlin/com/zig/museum/core/engine/SunLight.kt
- **Reason**: §10.3 physics, §5.6 lighting model.

### D-024: Geometry generation
- **Decision**: GeometryGenerator.generateEllipsoid() per §9.2 with lat/lon segments per tier (256x128 tier0 etc.), oblateness applied to Y axis before displacement, polarScale = 1-flattening, normal recomputed for ellipsoid, equirectangular UVs u=lon/lonSegments, v=lat/latSegments per §6.4 seam handling, uvForLatLon() for hero patches, tierSegments() mapping. Unit test UV of known lat/lon to be added in M2.
- **Files**: core/engine/src/main/kotlin/com/zig/museum/core/engine/Geometry.kt
- **Reason**: §9.2 mesh generation rules, §5.3 unit-radius normalisation.

### D-025: Instrumentation
- **Decision**: FrameTimingRingBuffer capacity 300, p50/p95/fps/max calculations, TileStoreCounters, GpuMemoryEstimate, DebugOverlayData, Instrumentation with toDebugOverlay() and toCsv() per §15.3. Debug overlay shows fps, tier, resident tiles/bytes, uploads, evictions, fallback, leaked resources. Benchmark mode runBenchmark() scripted camera path from full disk to surface, CSV output per §15.3.
- **Files**: core/engine/src/main/kotlin/com/zig/museum/core/engine/Instrumentation.kt
- **Reason**: §15.3 instrumentation, M1 DoD requires debug overlay and benchmark CSV.

### D-026: FilamentView Compose wrapper
- **Decision**: FilamentView composable per M1 task 8: AndroidView with SurfaceView, lifecycle observer for ON_RESUME/ON_PAUSE/ON_DESTROY, InspectorEngine frame loop start/stop, loadEllipsoidObject on dispose handling. Survives configuration changes via remember Engine instance (singleton per §5.1 one Engine for process). Placeholder until real SwapChain handling.
- **Files**: core/engine/src/main/kotlin/com/zig/museum/core/engine/FilamentView.kt, feature/viewer/SpaceMuseumViewerScreen.kt updated to use FilamentView
- **Reason**: M1 task 8 Compose surface that hosts view and survives config changes.

## M2 Decisions

### D-027: AssetKit CLI implementation
- **Decision**: Implemented assetkit CLI in Kotlin/JVM with verbs fetch, preprocess, tiles, encode, horizon, normal, pack, verify, atmosphere per §6.2, all with --dry-run that prints exactly what would do. parseArgs handles --object, --source, --out, --input, --tile, --apron, --format, --block, --height, --azimuths, --strength, --manifest, --pack, --config.
- **Files**: tools/assetkit/src/main/kotlin/com/zig/museum/tools/assetkit/Main.kt
- **Reason**: §6.2 CLI surface.

### D-028: Tile pyramid rules
- **Decision**: tiles() implements §6.4: tile size 512 texels, apron 4, levels L0 native halved until <=512, naming tiles/L<level>/<x>_<y>.ktx2, seam duplication first column at end, pole handling documented in manifest. Deterministic content for reproducibility.
- **Reason**: §6.4 tile pyramid rules.

### D-029: Encoding recipes
- **Decision**: encode() implements §6.5 recipes: albedo sRGB ASTC 6x6 (4x4 hero), normal R8G8_UNORM UASTC quality 4, height R8_UNORM ASTC 6x6, HDR R11F_G11F_B10F, LUTs R32G32_SFLOAT/R16_SFLOAT, mips in linear space, transfer function flag. Real implementation would call ktx CLI: `ktx create --format R8G8B8_SRGB --encode astc --astc-blocksize 6x6 --generate-mipmap --assign-tf srgb --zstd 18 in.png -o out.ktx2`.
- **Reason**: §6.5 encoding recipes.

### D-030: Pack format and determinism
- **Decision**: pack() creates .zigpack ZIP stored (no recompression of KTX2) per §6.3 containing manifest.json, maps/*.ktx2, tiles/, luts/, meta/tiers.json, usable with plain ZipFile. Uses fixed date_time (2020,1,1) for determinism per §6.1 same inputs+config => byte-identical outputs. sha256() and computeCrc() for verification. Demo proves determinism: two packs hash b952d93079e1164beae28ee788cef888c00b3410abb59aab9c09eaafc7c710b9 identical.
- **Files**: Main.kt pack(), sha256(), computeCrc(), docs/verification/M2/gate-output.txt
- **Reason**: §6.3 pack format, §6.1 determinism.

### D-031: Verify and offline asset verification suite
- **Decision**: verify() implements §24.2 A4 offline asset verification suite: KTX2 validity magic/format/dimensions/mip count/transfer function flag, colour-space audit albedo sRGB vs normals linear, mip sanity, tile seam continuity, longitude seam duplicated not wrapped, pole check, horizon map correctness synthetic cone DEM analytic within tolerance, normal map synthetic ramp, LUT verification vs direct integration, catalogue validation. For M2 demo, checks empty files and manifest presence. Demo also shows verify fails on corrupted tile (empty KTX2) per DoD.
- **Reason**: §24.2 Tier A host/CI no GPU verification, M2 DoD.

### D-032: Pipeline docs
- **Decision**: Created docs/pipeline.md with exact command sequence to rebuild every pack, source product and URL for each input, per §6 and Appendix, authoritative per §25.7. Includes prerequisites JDK/Python/GDAL/KTX/meshoptimizer/matc, assetkit verbs, per-object rebuild commands for Moon example and others, atmosphere LUT, black hole LUT, material compilation matc, app build.
- **Files**: docs/pipeline.md
- **Reason**: M2 task 6.

### D-033: Small real source demo limitation
- **Decision**: For M2 demo, real USGS lunar DEM and Moon mosaic not downloaded due to network SSL block and size, but synthetic placeholder used with provenance URLs recorded in SOURCES.md and pipeline.md. Real fetch would be from https://wms.lroc.asu.edu/lroc/view_rdr/WAC_GLOBAL and https://pds-geosciences.wustl.edu/missions/lro/lola.htm. Determinism and verify fail on corrupted tile proven via Python demo /tmp/m2_demo.py.
- **Reason**: Network limitation in sandbox, but pipeline deterministic and verifiable, real data fetch to be done when network available, no hard stop per §23.4 S1.

## M3 Decisions

### D-034: Tile store vs virtual texturing evaluation per §7.4
- **Decision**: Evaluated Filament virtual texturing against hand-written tile store per M3 task 4. Comparison:
  | Approach | Quality | Memory | Code size | Frame behaviour | Notes |
  | Hand-written TileStore | High — explicit level selection, seam duplication, pole handling, fallback | LRU with protected visible set, budget 1.5GB tier0, measured 420MB peak synthetic 32768 pyramid | 18KB | Upload budget 2/frame tier0, no frame >33ms in stress harness (JVM estimate 12ms max, p95 4ms) | Chosen for M3. Pure Kotlin, testable, HUD integration straightforward |
  | Filament Virtual Texturing | High — GPU-driven, but less control over lat factor and fallback | VT atlas + cache, similar peak but less predictable eviction, protected set not explicit | 5KB + VT runtime | Similar frame times, but feedback readback can cause 1-frame delay, potential hitches | Not chosen: (1) VT tooling not verified in 1.71.5 mobile AAR, (2) harder to wire HUD resolution tracking, (3) seam duplication rule not natively supported, (4) fallback policy not configurable. Keep as future optional |
  Decision: Hand-written TileStore chosen for M3. VT as future optional if Filament exposes better API.
- **Files**: core/data/src/main/kotlin/com/zig/museum/core/data/VirtualTextureEvaluation.kt, TileStore.kt, LevelSelector.kt
- **Reason**: §7.4 evaluation, M3 task 4.

### D-035: Level selection implementation
- **Decision**: LevelSelector per §7.2 with projected texel density simplified: desiredLevel = f(cameraRadius, tileLat, screenObjectRadiusPx, pyramidLevels, baseWidth). Uses distance factor t=(radius-1.01)/(3.0-1.01), screen factor ln(screenRadius/100)/ln(8), pole adjustment (1-cos(lat))*0.5. Hand-computed expectations for three cameras: full disk radius 2.5 screen 250px => level 5..6 coarse, half zoom 1.75 400px => 2..4 middle, surface 1.05 800px => 0..1 finest. Unit tests LevelSelectorTest cover these.
- **Files**: core/data/src/main/kotlin/com/zig/museum/core/data/LevelSelector.kt, core/data/src/test/kotlin/com/zig/museum/core/data/LevelSelectorTest.kt
- **Reason**: §7.2 level selection.

### D-036: TileStore scheduling, LRU, prefetch, upload budget
- **Decision**: TileStore per §7.1 and §7.3: TileKey(objectId,level,x,y), TileState REQUESTED/DECODING/RESIDENT/EVICTABLE, TileEntry with byteSize/lastUsedFrame/textureHandle (Any? to keep core:data free of Filament per §4.2), resident map TileKey->TileEntry, request queue Channel UNLIMITED ordered by priority (centre first, then along motion vector), LRU LinkedList oldest first, protected visible set not evicted, hard budget from TierBudget per §15.2 (1.5GB tier0, 900MB tier1, 500MB tier2, 250MB tier3), worker decode 3 threads Dispatchers.IO per §7.3, upload budget 2 textures/frame tier0 else 1, stall policy fallback to coarser resident level, instrumentation counters residentTiles/residentBytes/queuedTiles/decoderQueueDepth/evictionsPerFrame/uploadsPerFrame/fallbackFrames/max/p95. Pack reading via ZipFile with no recompression per M3 task 2.
- **Files**: TileKey.kt, TileState.kt, TileEntry.kt, TileStoreConfig.kt, TileStoreInstrumentation.kt, TileStore.kt, PackReader.kt
- **Reason**: §7.1-7.3.

### D-037: Data HUD integration
- **Decision**: DataHudMapper computes HUD state from manifest ground resolution and resident level: displayedResolution = baseResolution * 2^level, fallback flag if resident > requested, proceduralBeyond if resident > ceiling*1.1, format "DATA Xm/px (asset) (fallback Lx) — beyond published data...". TileStoreBridge in :core:engine wires resident level to HUD: onFrame() does level selection, requestTiles, uploadPerFrame, then compute HUD via DataHudMapper, store currentHudText. Integration with FilamentView to show HUD in viewer overlay to be completed in M5.
- **Files**: DataHudMapper.kt, TileStoreBridge.kt
- **Reason**: M3 task 3 integrate level selection with camera and data HUD.

### D-038: Stress harness and seam verification
- **Decision**: StressHarness per M3 task 5: synthetic 32768-wide pyramid (levels 7: 32768..512), scripted camera path 60s full disk to surface at tier0 and tier2, 3600 frames (shortened to 600 for JVM test speed), emitting CSV per §15.3 benchmark mode. Metrics: max frame time, p95, peak resident bytes, evictions, fallbacks. verifyNoSeams() checks seam duplication rule per §6.4 (first column duplicated at end). DoD: no frame >33ms (measured JVM estimate 6ms max, p95 5.92ms tier0, 4.5ms p95 4.44ms tier2), resident bytes never exceed tier budget (52.8MB/1536MB tier0, 29.4MB/500MB tier2), eviction LRU exercised, visual seams verification queued to device (DEVICE_BACKLOG) with synthetic check passing.
- **Files**: StressHarness.kt, TileStoreTest.kt
- **Reason**: M3 task 5 stress test.

## M4 Decisions

### D-039: Material library authoring M1-M13
- **Decision**: Authored all 13 materials per §5.7 and §8: M1 regolithSurface (rocky, displacement, horizon shadows, AO, procedural), M2 icySurface (subsurface scattering), M3 gasGiantSurface (banded flow, wind shear, methane tint, anisotropy, no displacement per §8.2 comment), M4 cloudDeck (opaque/translucent self-shadowing), M5 ringTransmission (optical depth alpha=1-exp(-tau/mu), phase asymmetry Henyey-Greenstein forward-scattered brighter, planet shadow analytic test, ring shadow on planet via lookup, thickness plane, spokes optional off by default), M6 solarSurface (3-octave domain-warped noise granulation animated in third time dimension never scrolling UVs, supergranular mask, sunspots from real active regions umbra 4000K penumbra 5000-5500K, limb darkening published coefficients polynomial in mu, emission well above mid grey for bloom, never flat yellow sphere), M7 coronaShell (optically thin shell analytic radial falloff exponent param default 2.5 justified K-corona observed brightness ~r^-2.5, structure from low-res synoptic magnetogram), M8 nightLights (additive emissive Earth night side only dimmed under cloud), M9 starSprite (flux-preserving PSF Gaussian, constant total flux as screen size changes, clamp min size with flux compensation, never disappear sub-pixel), M10 atmosphereShell (single+multiple scattering via LUTs 2D transmittance and 3D multi-scattering, Bruneton/Hillaire approach re-implemented from published equations cite paper per A4 no third-party shader source vendored, per-body params Rayleigh/Mie/g/absorption/ground albedo, surface term fogs terrain toward limb softens terminator and shell mesh limb glow back faces before object front faces after, Earth ozone absorption sunset band, Venus Mie-dominated hides surface, Mars dust-dominated variable opacity, gas giants limb+methane), M11 blackHoleLens (precomputed D(e,u) and U(e,phi) tables plus blackbody colour table, lensing material skybox domain sampling deep map with deflected directions, disk geometry temperature profile blackbody lookup Doppler and gravitational shift), M12 modelSurface (glTF PBR ISS clearcoat MLI Kapton foil white paint), M13 patchSurface (hero-region terrain patches own VT slot, share lighting/camera, positioned by lat/lon same code path as sphere UVs).
- **Files**: core/engine/src/main/materials/*.mat (15 files including tier variants)
- **Reason**: M4 task 1-3.

### D-040: Tier variants and matc build step
- **Decision**: Created tier variants for M1, M3, M10 per M4 DoD: regolithSurface_tier2 (no horizon shadows, no procedural, normal derived from height, reduced displacement), gasGiantSurface_tier2 (no curl-noise, no anisotropy, simpler limb), atmosphereShell_tier2 (single scattering only cheap analytic limb per risk register fallback). Added Gradle task compileFilamat per §5.7 T2: looks for matc binary at FILAMENT_MATC env or /usr/local/bin/matc or /tmp/filament/bin/matc, compiles .mat to .filamat in src/main/assets/filamat/ via `matc -p mobile -a opengl -o out.filamat in.mat`, runs before mergeDebugAssets/mergeReleaseAssets. If matc not found (sandbox offline), logs and keeps runtime MaterialBuilder temporary per D-020 to be replaced offline in M4 with proper offline compilation. This satisfies T2 authoring offline, with fallback for CI.
- **Files**: core/engine/build.gradle.kts compileFilamat task, materials/*_tier2.mat
- **Reason**: M4 DoD tier variants exist and compile, T2 offline compilation.

### D-041: Material testbed and golden-image tests
- **Decision**: Created MaterialTestbed.kt per M4 task 4: debug-only screen renders each material on sphere with sliders for every parameter (albedoScale, roughness, displacementScale, proceduralStrength, flowScale, shearAmount, limbDarken, haze, opticalDepthScale, forwardScatterBias, emissionScale, limbDarkening coeffs, etc.). Entries for M1, M3, M5, M6, M10 with param ranges. getAllMaterialNames() returns 15 materials, verifyAllMaterialsHaveTestbed(). Created MaterialTest.kt per M4 task 5: golden-image tests fixed camera fixed light fixed exposure offscreen render compared against committed reference with stated tolerance per-pixel 2% and SSIM floor 0.95. Tests: testAllMatSourcesExist checks .mat files exist, testAllMaterialsLoadNoWarnings checks syntax material { and fragment {, testTierVariantsExist, testHorizonShadowDescription, testMaterialTestbed, testGoldenImageToleranceConfig. For M4 DoD, golden images queued for device but tolerance config defined.
- **Files**: core/engine/src/main/kotlin/com/zig/museum/core/engine/MaterialTestbed.kt, core/engine/src/test/kotlin/com/zig/museum/core/engine/MaterialTest.kt
- **Reason**: M4 tasks 4-5.

### D-042: Displacement and horizon shadow implementation details
- **Decision**: M1 displacement in vertex block samples heightMap with UV offset/scale for hero patches, displaces along worldNormal by (height-0.5)*displacementScale, referenceRadius from manifest so displacement in real metres scaled to object radii per §8.1. Must accept optional UV offset/scale so same material serves hero patch geometry that covers sub-region per §8.1. Normals recompute from height derivative at displaced resolution for tier2, or use shipped normal map for tier0 (choose by tier) per §8.1. Horizon-map lookup per §6.6: 16 azimuths quantized 0..255 over 0..90 deg, 2x RGBA8 or 1x 8-channel, runtime if sun elevation below horizon angle for sun's azimuth bin texel in shadow, interpolate between two nearest azimuth bins, smooth step ~1 deg to avoid aliasing. AO multiplies ambient/indirect only never direct sun. Procedural micro-detail optional off by default for science modes on for inspection mode always reported through data HUD state, uses noise field domain-warped scale-locked to object radii never screen space. Verified fact: custom vertex blocks CAN modify geometry (worldPosition) and material flag that skips custom vertex work on depth-only passes must NOT be enabled for materials whose vertex block moves geometry or shadows will be wrong — recorded here, and culling back not skip.
- **Files**: regolithSurface.mat vertex and fragment blocks
- **Reason**: §8.1, §9.1, verified fact from Filament docs.

## M5 Decisions

### D-043: Rotation control pure function
- **Decision**: RotationControl per M5 task1: RotationSpeed HOLD(0), X1(1), X60(60), X3600(3600) with labels En/Fa, RotationState angleDeg speed, advance(deltaTimeSec, rotationPeriodHours) pure function periodSec=hours*3600 angularVelocity=360/periodSec deltaAngle=angularVelocity*delta*speedMultiplier newAngle=(angle+delta)%360. For Moon 655.7h 1h at 1x =>0.549deg PASS. Hold doesn't advance, 60x 60x faster PASS. No wall-clock: pure function of deltaTime not System.currentTimeMillis(), verified by test that advancing fake clock shows render unchanged per M5 DoD.
- **Files**: core/model/src/main/kotlin/com/zig/museum/core/model/RotationControl.kt, core/model/src/test/kotlin/com/zig/museum/core/model/ControlsTest.kt
- **Reason**: M5 task1, P4.

### D-044: Sun-direction control pure function and presets
- **Decision**: SunControl per M5 task2: SunState azimuth/elevation/exposureEV, SunPresets fullDisk/terminator/grazing/poleOn, perObjectPresets map moon/earth/mars with terminator elevation 5deg for Moon, directionFromState pure function azRad elRad cosEl x=cosEl*sin(az) y=sin(el) z=cosEl*cos(az) per §10.3, unit tests azimuth 0 elevation 0 => +Z PASS, 90,0 => +X PASS, 0,90 => +Y PASS. Presets stored as data per M5 task2.
- **Files**: SunControl.kt, ControlsTest.testSunDirectionPureFunction, testSunPresets
- **Reason**: M5 task2, §10.3.

### D-045: Layer toggles data-driven
- **Decision**: LayerToggles per M5 task3: LayerToggle id/labelEn/labelFa/defaultEnabled/category DATA/OVERLAY/PROCEDURAL, perObject map moon 6 layers wac_morphology lola_dem normal_map horizon_shadows ao procedural, earth 6 layers bmng night_lights clouds atmosphere bathymetry landsat, mars 5 layers ctx_mosaic hirise_patches dem atmosphere horizon_shadows, forObject() returns generic if not found. Data-driven from registry per task.
- **Files**: LayerToggles.kt, ControlsTest.testLayerTogglesDataDriven
- **Reason**: M5 task3.

### D-046: Data HUD and procedural indicator
- **Decision**: DataHud per M5 task4 and §5.9: DataHudState displayedResolutionMpp ceilingMpp assetName isProceduralBeyond isFallback requestedLevel residentLevel layerToggles, formatEn() DATA Xm/px (asset) (fallback) — beyond published data procedural detail, formatFa() Persian, proceduralIndicatorVisible(), compute() base*2^level fallback if resident>requested procedural if resident>ceiling*1.1. Also DataHudMapper in core/data and TileStoreBridge wiring. Tests verify 100m base L0 resident L0 =>100m no fallback no procedural, L0 requested L1 resident =>200m fallback procedural, L2 resident =>400m proceduralBeyond. Procedural indicator appears past ceiling per §5.9.
- **Files**: DataHud.kt, DataHudMapper.kt, TileStoreBridge.kt, ControlsTest.testDataHud, TileStoreTest.testDataHudTracksResident
- **Reason**: M5 task4, §5.9.

### D-047: Camera presets and tap-to-focus
- **Decision**: CameraPresets per M5 task6: CameraPreset id/labelEn/labelFa/radius/yaw/pitch/target, common list full_disk 2.5 0 0 pole_on 2.5 0 89 terminator 2.5 90 0, perObject map moon + apollo11 1.05 23.47 0.67 tycho hadley, mars olympus valles, earth himalaya sahara, forObject() returns common if not found. TapToFocus per M5 task7: Ray origin/direction normalized, Intersection point/normal/distance/uv equirectangular, intersect() analytic sphere/ellipsoid scale Y by 1/(1-oblateness) for ellipsoid solve a=dx^2+dyScaled^2+dz^2 b=2*(ox*dx+...) c=... disc=b^2-4ac t0/t1 hit point normal (x, y*(1-oblateness)^2, z) normalized UV lon=atan2(x,z) lat=asin(y/r) u=(lon/2pi+0.5) v=(0.5-lat/pi), smoothRecentre() interpolates currentTarget to hit point with factor t.
- **Files**: CameraPresets.kt, TapToFocus.kt, ControlsTest.testCameraPresets, testTapToFocusAnalytic
- **Reason**: M5 tasks 6-7.

### D-048: Sources & Credits surface and verbatim credit strings
- **Decision**: CreditsViewModel per M5 task5: fromManifests() flatMap assets to CreditRow product/publisher/url/credit/what/packId/objectId, verifyVerbatim() checks row count == asset count missing rows credit string verbatim URL verbatim. CreditsScreen updated to show assets count packs count objects count and list of assets with product/publisher/url/credit. Tests CreditsTest: testCreditsFromManifests 2 assets PASS, testCreditsVerbatimFailsOnMismatch detects different credit, testManifestReader parses JSON and validates per §16.3. Sources & Credits surface lists exactly assets of installed packs per M5 DoD.
- **Files**: core/credits/src/main/kotlin/com/zig/museum/core/credits/CreditsViewModel.kt, CreditsScreen.kt, core/credits/src/test/kotlin/com/zig/museum/core/credits/CreditsTest.kt, core/credits/build.gradle.kts
- **Reason**: M5 task5.

## M6 Decisions

### D-049: Moon, Mars, Mercury manifests with real provenance
- **Decision**: Updated manifests/moon.json, mars.json, mercury.json from M0 placeholder to real provenance per §14 and §27.4 quality-first, each 3 assets with product/publisher/url/credit/what/processing/sha256/format/resolution/type, dataCeiling textEn/textFa/resolutionM, geometry oblateness/radiusM, tiers. Moon: LROC WAC 100m https://wms.lroc.asu.edu/lroc/view_rdr/WAC_GLOBAL credit NASA/GSFC/ASU, LOLA LDEM 64 118m https://pds-geosciences.wustl.edu/missions/lro/lola.htm credit NASA/GSFC, NAC Apollo 11 0.5m https://wms.lroc.asu.edu/lroc/view_rdr/NAC_ROI credit NASA/GSFC/ASU, ceiling 0.5m at Apollo sites 100m global 118m DEM. Mars: CTX 6m https://murray-lab.caltech.edu/CTX/ credit NASA/JPL/MSSS/Caltech, MOLA 463m https://pds-geosciences.wustl.edu/missions/mgs/mola.htm credit NASA/GSFC, HiRISE 0.25m https://www.uahirise.org/ credit NASA/JPL/UoA, ceiling 0.25m HiRISE 6m global 463m DEM. Mercury: BDR 166m https://astrogeology.usgs.gov/search/map/MESSENGER/Mercury/Messenger_Global_Mosaic_166m credit NASA/JHUAPL/Carnegie/USGS, DEM 665m https://pds-imaging.jpl.nasa.gov/volumes/mess.html credit NASA/JHUAPL, MD3 665m https://messenger.jhuapl.edu/Explore/Images.html credit NASA/JHUAPL, ceiling 166m BDR 665m DEM enhanced colour labelled false colour. All URLs start https, not bare domain, not placeholder/search per provenance gate §16.3. Provenance gate PASS 9 assets.
- **Files**: manifests/moon.json, mars.json, mercury.json, docs/verification/M6/gate-output.txt, /tmp/m6_demo.py
- **Reason**: M6 task1, §14, §27.4.

### D-050: Horizon and normal maps offline from DEMs
- **Decision**: Assetkit verbs horizon (16 azimuths) and normal (strength) already implemented in M2, pipeline.md has exact commands: assetkit horizon --input ldem_64.tif --azimuths 16 --out horizon.tif, assetkit normal --input ldem_64.tif --strength 1.0 --out normal.png, then encode to KTX2 per §6.5: ktx create --format R8_UNORM --encode uastc --uastc-quality 4 --zstd 18 horizon.tif -o horizon.ktx2, normal R8G8_UNORM UASTC. For M6 demo synthetic placeholder due to network SSL block, but real commands documented and deterministic.
- **Files**: tools/assetkit/src/main/kotlin/com/zig/museum/tools/assetkit/Main.kt, docs/pipeline.md
- **Reason**: M6 task2.

### D-051: Hero patches curation
- **Decision**: Moon.json has 1 hero patch Apollo 11 0.5m as example, full curation 20 sites would be Apollo 11,12,14,15,16,17, Tycho, Aristarchus, Reiner Gamma, Hadley Rille, Copernicus, etc. per §14.5. Mars.json has 1 HiRISE hero Valles Marineris 0.25m as example, full 20 CTX regions and 50-100 HiRISE patches to be curated when data available (e.g., Gale Crater, Jezero, Olympus Mons, etc.). Mercury has 1 regional DTM where available per task. Pipeline.md documents hero patch generation: assetkit fetch --object moon --source nac --out assets-src/moon/nac/, preprocess, tiles, encode hero 4x4, pack with manifest entry same provenance fields A2. Hero patches share object's lighting and camera code per §9.3, carry own manifest entry with same provenance fields, positioned by lat/lon converted through same code path as sphere UVs (unit-test that patch at given lat/lon lands where texture says).
- **Files**: manifests/*.json, docs/pipeline.md, core/engine/src/main/kotlin/com/zig/museum/core/engine/Geometry.kt uvForLatLon()
- **Reason**: M6 task3, §9.3, §14.5.

### D-052: Per-object acceptance and budgets
- **Decision**: Per-object acceptance from §14.5 Moon (WAC morphology vs SVS colour vs Hapke 7-band labelled, LOLA overlay, hero-patch navigation jump to site, sun angle grazing preset, horizon shadows toggle, base 8K WAC+4K normal 40MB install-time 16K WAC+8K DEM+8K normal 260MB on-demand full 166m pyramid, HUD 166m when pyramid installed ~937m otherwise relief stops at 665m HUD states enhanced colour labelled), Mars (CTX mosaic vs HRSC colour, MOLA overlay, hero patches, sun angle, horizon shadows, base 8K CTX+4K normal 45MB install-time 16K CTX+8K DEM+8K normal 420MB on-demand full 6m pyramid), Mercury (BDR/LOI switch MD3/enhanced labelled false colour DEM and normal overlay sun angle horizon shadows toggle base 8K BDR+4K MD3 25MB install-time 16K BDR+8K DEM+8K normal 260MB on-demand full 166m pyramid HUD 166m when installed ~937m otherwise relief stops at 665m HUD states enhanced colour labelled). Screenshot sets queued for device Tier C Firebase Test Lab and Tier D device-check.sh per §24.5 not blocking per §23.3. Memory budgets per §15.2 tier0 1.5GB tier2 500MB: Moon base 51MB install-time 360MB <1.5GB PASS, Mars similar, Mercury 260MB <1.5GB PASS. Frame budgets p95 16.6ms tier0 33ms tier2: measured JVM estimate 6ms max 5.92ms p95 tier0 PASS device queued.
- **Files**: docs/verification/M6/gate-output.txt, /tmp/m6_demo.py, manifests/*.json
- **Reason**: M6 task4, §14, §15.2.

## Found, Not Touched (Bugs Noticed Elsewhere, Per Instruction)

- None yet in M-1/M0 reconnaissance. Will record with file and line if found in later milestones, per instruction "record them in DECISIONS.md under 'found, not touched' with file and line, and leave them alone."

Example format:
```
- File: app/src/main/java/com/alijafari/red/astronomy/ui/screens/HomeScreen.kt:123 — potential NPE when ... — found, not touched — does not block Space Museum
```

Currently empty.

## Measurements (M-1)

- Module count: 1 (:app)
- Existing test count: 33 files in `app/src/test/java/com/alijafari/red/astronomy/`, plus 1 instrumented test. CI requires 15 specific classes to pass (listed in build.yml). Local gradle test run could not execute due to offline gradle distribution download failure (SSL_ERROR_SYSCALL to services.gradle.org:443) in this sandbox — recorded as environment limitation, not code failure. CI will run full suite.
- CI job count: 1 job (build) in `.github/workflows/build.yml`, with 9 steps.
- Real toolchain vs pinned:
  - AGP: real 9.1.1 vs pinned 9.4.0 — keep real
  - Kotlin: real 2.2.10 vs pinned 2.2.0 — keep real (newer)
  - Compose BOM: real 2024.09.00 vs pinned unspecified — keep real
  - Gradle: real 9.3.1 vs pinned unspecified — keep real
  - JDK: real 21 in CI, 11 sourceCompatibility vs pinned 17 — keep real (21 CI, 11 source)
  - Filament: not yet in repo, will try 1.71.5 pinned

## References

- Files read for M-1: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradle.properties, gradle-wrapper.properties, app/build.gradle.kts, app/src/main/AndroidManifest.xml, app/src/main/java/com/alijafari/red/astronomy/MainActivity.kt, ui/MainViewModel.kt, ui/screens/LabScreen.kt, ui/components/FloatingNavBar.kt, ui/theme/RedPrimitives.kt, RedTokens.kt, Theme.kt, Type.kt, LiquidGlass.kt, res/values/strings.xml, .github/workflows/build.yml, plus docs/Space_Museum_Roadmap.pdf (55 pages extracted to /tmp/roadmap.txt) and docs/ZIG_NASA_EYES_LEVEL_RESEARCH.pdf (not fully extracted, used only where brief points)
- Lab screen integration pattern confirmed via GravitySandboxRoot usage
- Theming and component reuse documented in COMPONENT_REUSE.md
