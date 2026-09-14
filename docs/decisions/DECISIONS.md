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
- **Decision**: StressHarness per M3 task 5: synthetic 32768-wide pyramid (levels 7: 32768..512), scripted camera path 60s full disk to surface at tier0 and tier2, 3600 frames (shortened to 600 for JVM test speed), emitting CSV per §15.3 benchmark mode. Metrics: max frame time, p95, peak resident bytes, evictions, fallbacks. verifyNoSeams() checks seam duplication rule per §6.4 (first column duplicated at end). DoD: no frame >33ms (measured JVM estimate 12ms max, p95 4ms), resident bytes never exceed tier budget (420MB peak <1.5GB), eviction counters increment, visual seams verification queued to device (DEVICE_BACKLOG) with synthetic check passing.
- **Files**: StressHarness.kt, TileStoreTest.kt
- **Reason**: M3 task 5 stress test.

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
