# Changed Files — Space Museum Integration

This file lists every pre-existing file touched by Space Museum, one line each, with reason and necessity test.

Format: `path | reason | necessity`

## M-1 (reconnaissance, read-only)
No pre-existing files touched in M-1. This file itself is new.

## Planned touches (M-0 onwards)

- `settings.gradle.kts` | Add include() for new modules :core:model, :core:engine, :core:data, :core:credits, :feature:museum, :feature:viewer, :tools:assetkit, :tools:ci | DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS? Yes — Gradle cannot discover new modules without include, so feature code would not compile or ship.

- `app/build.gradle.kts` | Add implementation(project(":core:model")), implementation(project(":core:credits")), implementation(project(":feature:museum")), implementation(project(":feature:viewer")) and implementation(project(":core:engine")) when engine exists | DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS? Yes — :app (LabScreen, MainActivity) must depend on new modules to launch museum screens; otherwise museum code is unreachable.

- `app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt` | Add SPACE_MUSEUM entry to LabFeatureType enum and add branch in LabScreen composable to show Space Museum when selectedFeature == SPACE_MUSEUM | DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS? Yes — this is the single entry point per spec §26.5 and Appendix I. Without this card, user cannot reach Space Museum.

- `.github/workflows/build.yml` | Add cache-disabled: true to Setup Gradle step to fix Restore Gradle distribution 9.3.1 failed Cache service responded with 400 Failed to restore gradle-home-v1 Error 400 Full unit test suite tests=0 files=0 gradle_status=1 AR classes tests=0 FAIL infra not code, and add upload of unit test log with --stacktrace for debugging Build APK failure org.gradle.api.GradleException Error resolving plugin id com.android.library version 9.1.1 InvalidPluginRequestException plugin already on classpath | DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS? Yes — Build APK cannot pass with cache 400 failure and plugin resolution error, museum fails CI G6 without this fix per §26.2.

- `core/data/build.gradle.kts` `core/credits/build.gradle.kts` `core/engine/build.gradle.kts` `feature/museum/build.gradle.kts` `feature/viewer/build.gradle.kts` `core/model/build.gradle.kts` `tools/assetkit/build.gradle.kts` `tools/blackhole-lut/build.gradle.kts` `tools/ci/build.gradle.kts` | Change id(\"com.android.library\") version \"9.1.1\" to id(\"com.android.library\") without version and id(\"org.jetbrains.kotlin.plugin.compose\") version \"2.2.10\" to without version and id(\"org.jetbrains.kotlin.jvm\") version \"2.2.10\" to without version to fix InvalidPluginRequestException The request for this plugin could not be satisfied because the plugin is already on the classpath with an unknown version | DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS? Yes — Build APK fails with plugin resolution error, museum fails CI without this fix per §26.2.

Potential future touches (only if proven necessary, must be added here before edit):

- `app/src/main/AndroidManifest.xml` | Only if new modules require no INTERNET permission check or need to declare a FileProvider for packs (unlikely). Any edit must keep INTERNET permission status for museum modules absent and not add new permissions unless proven necessary for offline pack reading. | Necessity to be evaluated per milestone; default is no touch.

- `app/src/main/res/values/strings.xml` and `values-fa/strings.xml` | Ideally no touch — new strings live in new modules' own res. Only if existing string resources must be referenced for testTag or accessibility and cannot be duplicated. | Touch only if museum fails without it.

- `app/src/main/java/com/alijafari/red/astronomy/MainActivity.kt` | Only if FloatingBottomBar hiding needs extension beyond ImmersiveScreenState reuse. Current plan reuses `com.zig.gravity.ui.ImmersiveScreenState.active` from museum viewer, so no edit needed. If new immersive state is created, MainActivity would need to check it — then this file would be touched with reason "hide floating nav bar during immersive museum viewer, same pattern as gravity sandbox". | Necessity: does museum fail to hide nav bar without it? If reuse works, no touch.

## Rule enforcement
- Every edit must pass test: DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS CHANGE? If not unqualified yes, do not make change.
- No refactoring, renaming, reformatting, dependency bumps, version alignment, "while I was here" fixes.
- Existing calculations (ISS etc.), screens, models, strings, assets, tests, build config stay exactly as they are unless listed above with necessity.
- This file is reviewed at every milestone green gate (G10 scope gate).
