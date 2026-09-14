# Changed Files — Space Museum Integration

This file lists every pre-existing file touched by Space Museum, one line each, with reason and necessity test.

Format: `path | reason | necessity`

## M-1 (reconnaissance, read-only)
No pre-existing files touched in M-1. This file itself is new.

## Planned touches (M-0 onwards)

- `settings.gradle.kts` | Add include() for new modules :core:model, :core:engine, :core:data, :core:credits, :feature:museum, :feature:viewer, :tools:assetkit, :tools:ci | DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS? Yes — Gradle cannot discover new modules without include, so feature code would not compile or ship.

- `app/build.gradle.kts` | Add implementation(project(":core:model")), implementation(project(":core:credits")), implementation(project(":feature:museum")), implementation(project(":feature:viewer")) and implementation(project(":core:engine")) when engine exists | DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS? Yes — :app (LabScreen, MainActivity) must depend on new modules to launch museum screens; otherwise museum code is unreachable.

- `app/src/main/java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt` | Add SPACE_MUSEUM entry to LabFeatureType enum and add branch in LabScreen composable to show Space Museum when selectedFeature == SPACE_MUSEUM | DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS? Yes — this is the single entry point per spec §26.5 and Appendix I. Without this card, user cannot reach Space Museum.

Potential future touches (only if proven necessary, must be added here before edit):

- `app/src/main/AndroidManifest.xml` | Only if new modules require no INTERNET permission check or need to declare a FileProvider for packs (unlikely). Any edit must keep INTERNET permission status for museum modules absent and not add new permissions unless proven necessary for offline pack reading. | Necessity to be evaluated per milestone; default is no touch.

- `app/src/main/res/values/strings.xml` and `values-fa/strings.xml` | Ideally no touch — new strings live in new modules' own res. Only if existing string resources must be referenced for testTag or accessibility and cannot be duplicated. | Touch only if museum fails without it.

- `app/src/main/java/com/alijafari/red/astronomy/MainActivity.kt` | Only if FloatingBottomBar hiding needs extension beyond ImmersiveScreenState reuse. Current plan reuses `com.zig.gravity.ui.ImmersiveScreenState.active` from museum viewer, so no edit needed. If new immersive state is created, MainActivity would need to check it — then this file would be touched with reason "hide floating nav bar during immersive museum viewer, same pattern as gravity sandbox". | Necessity: does museum fail to hide nav bar without it? If reuse works, no touch.

## Rule enforcement
- Every edit must pass test: DOES SPACE MUSEUM FAIL TO WORK WITHOUT THIS CHANGE? If not unqualified yes, do not make change.
- No refactoring, renaming, reformatting, dependency bumps, version alignment, "while I was here" fixes.
- Existing calculations (ISS etc.), screens, models, strings, assets, tests, build config stay exactly as they are unless listed above with necessity.
- This file is reviewed at every milestone green gate (G10 scope gate).
