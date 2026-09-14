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
