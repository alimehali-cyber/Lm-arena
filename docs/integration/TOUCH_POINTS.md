# Touch Points — Integration Mapping

Date: 2026-09-14
M-1

This file maps roadmap's abstract architecture (§4.1) to real repository's actual files, per §4.1 requirement to record mapping before creating anything.

## Roadmap Abstract vs Real Repo

| Roadmap Abstract Path | Real Repo Path | Mapping Decision |
|---|---|---|
| `<root>/settings.gradle.kts` | `/home/user/Lm-arena/settings.gradle.kts` | Same file, will add includes for new modules |
| `<root>/build.gradle.kts` | `/home/user/Lm-arena/build.gradle.kts` | Same, no edit needed for new modules (plugins apply false already) |
| `<root>/gradle/libs.versions.toml` | `/home/user/Lm-arena/gradle/libs.versions.toml` | Single source of versions, will add new libs (filament) here only if needed for new modules, but keep existing versions unchanged |
| `app/` (existing) | `/home/user/Lm-arena/app/` | Existing :app module, namespace com.alijafari.red.astronomy |
| `core/engine/` | New: `core/engine/` -> `:core:engine` | New module, directory `core/engine/`, package `com.zig.museum.core.engine` |
| `core/data/` | New: `core/data/` -> `:core:data` | New module, package `com.zig.museum.core.data` |
| `core/model/` | New: `core/model/` -> `:core:model` | New module, package `com.zig.museum.core.model` |
| `core/designsystem/` | Existing: `app/src/main/java/com/alijafari/red/astronomy/ui/theme/` | Real repo's design system is inside :app, not separate module. Reuse existing RED design system, do not create new designsystem module. Museum will use existing RedTheme etc. |
| `core/credits/` | New: `core/credits/` -> `:core:credits` | New module, package `com.zig.museum.core.credits` |
| `feature/museum/` | New: `feature/museum/` -> `:feature:museum` | New module, package `com.zig.museum.feature.museum` |
| `feature/viewer/` | New: `feature/viewer/` -> `:feature:viewer` | New module, package `com.zig.museum.feature.viewer` |
| `feature/settings/` | Not needed separately — settings (quality, exposure) will be part of viewer bottom sheet and/or existing SettingsDialog, not separate module | Omit as separate module, include controls in viewer |
| `tools/assetkit/` | New: `tools/assetkit/` -> `:tools:assetkit` | JVM-only, package `com.zig.museum.tools.assetkit` |
| `tools/blackhole-lut/` | New: `tools/blackhole-lut/` -> `:tools:blackhole-lut` | JVM-only, package `com.zig.museum.tools.blackhole` |
| `tools/ci/` | New: `tools/ci/` -> `:tools:ci` | JVM-only Python scripts, but Gradle module for CI tasks |
| `assets-src/` | New: `assets-src/` (git-ignored) | Downloaded source data, git-ignored |
| `assets-built/` | New: `assets-built/` (git-ignored) | Built packs .zigpack, git-ignored, checksummed |
| `manifests/` | New: `manifests/` (versioned) | Provenance manifests, versioned in git |
| `docs/decisions/DECISIONS.md` | New: `docs/decisions/DECISIONS.md` | Created in M-1 |
| `docs/pipeline.md` | To be created in M2 | How to rebuild every pack |
| `tests/golden/` | To be created in M4 | Reference images + tolerance config |
| `.github/workflows/` | Existing: `.github/workflows/build.yml` | Existing CI, will add new workflow files for gates, not modify existing |

## Integration Points (Only Existing Files That May Be Edited)

Per §26.3 and CHANGED_FILES.md:

1. `settings.gradle.kts` — add includes for new modules (necessary for Gradle discovery)
2. `app/build.gradle.kts` — add implementation(project(":...")) dependencies (necessary for :app to launch museum)
3. `app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt` — add SPACE_MUSEUM enum entry and branch (necessary entry point per §26.5, Appendix I)

Potential conditional touches (only if proven necessary):

- `app/src/main/AndroidManifest.xml` — only if new modules need FileProvider or similar, must not add INTERNET
- `app/src/main/java/com/alijafari/red/astronomy/MainActivity.kt` — only if FloatingBottomBar hiding needs extension beyond reuse of ImmersiveScreenState
- `app/src/main/res/values/strings.xml` / `values-fa/strings.xml` — ideally no touch, new strings live in new modules' own res

All other files must stay untouched per §26.2 test.

## Lab Screen Insertion Details (Real File)

File: `app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt`
- Enum: `LabFeatureType` lines 19-70
- Composable: `LabScreen` lines 85-199 with `selectedFeature` state
- Card: `LabFeatureCard` lines 199-291
- Insertion: Add new enum entry SPACE_MUSEUM after GRAVITY_SANDBOX, before ORBITAL_RESONANCE, isAvailable=true, icon distinct, testTag `lab_feature_card_space_museum`
- Branch: After existing `if (selectedFeature == TIME_DILATION)` and `else if (GRAVITY_SANDBOX)`, add `else if (SPACE_MUSEUM) { SpaceMuseumRoot(...) }`

This matches gravity sandbox integration pattern exactly.

## Navigation

- Real repo uses tab index int + selectedFeature state, not NavController
- Museum grid/viewer navigation is internal state inside SpaceMuseumRoot, not separate NavHost
- System back: Viewer -> Grid -> Lab via onBack callbacks

## Design System Reuse

- Real repo's design system: `app/src/main/java/com/alijafari/red/astronomy/ui/theme/` — RedPrimitives, RedTokens, Theme, Type, LiquidGlass, etc.
- Museum must reuse these, not create new theme, per §12
- Documented in COMPONENT_REUSE.md

## Verification

- All paths in this file were read from repository (checked via bash ls/find)
- No invented paths
- Mapping recorded before any production code (M-1 requirement)
