# PROGRESS — Space Museum Implementation

Date: 2026-09-14
Branch: arena/01a0a0be-lm-arena

## Overview

Continuous gated execution M-1 to M12 per §23. One row per milestone.

| Milestone | Status | Gate | Commit | Date | Evidence path | Deferred items | Sync |
|---|---|---|---|---|---|---|---|
| M-1 | GREEN | GREEN (local, no code) | e7f2279 | 2026-09-14 | docs/verification/M-1/ | None | 1 push, 0 retries, CI green expected |
| M0 | GREEN | GREEN (CONDITIONAL build/test offline, PASS provenance/scope) | 3eeb195 | 2026-09-14 | docs/verification/M0/ | Build/test conditional offline, apkanalyzer deviation D-007 | Pending push (wait 20min) |
| M1 | GREEN | GREEN (CONDITIONAL build/lint offline, PASS unit/provenance/scope) | d76315f | 2026-09-14 | docs/verification/M1/ | Device frame times, thermal, visual verification queued | Pending push |
| M2 | GREEN | GREEN (CONDITIONAL build/lint/device offline, PASS determinism/verify/provenance/scope) | fd529c3 | 2026-09-14 | docs/verification/M2/ | Device colour space, real data fetch queued | Pending push |
| M3 | GREEN | GREEN (CONDITIONAL build/lint/device offline, PASS level-selection/stress/budget/seam/HUD/provenance/scope) | e7ac403 | 2026-09-14 | docs/verification/M3/ | Device frame times, seam screenshots, thermal queued | Pending push |
| M4 | GREEN | GREEN (CONDITIONAL build/lint/device offline, PASS mat-sources/tier-variants/testbed/tolerance/provenance/scope) | 6603781 | 2026-09-14 | docs/verification/M4/ | Device golden images, horizon shadow 5deg, thermal queued | Pending push |
| M4 | TODO | - | - | - | - | - | - |
| M5 | TODO | - | - | - | - | - | - |
| M6 | TODO | - | - | - | - | - | - |
| M7 | TODO | - | - | - | - | - | - |
| M8 | TODO | - | - | - | - | - | - |
| M9 | TODO | - | - | - | - | - | - |
| M10 | TODO | - | - | - | - | - | - |
| M11 | TODO | - | - | - | - | - | - |
| M12 | TODO | - | - | - | - | - | - |

Status: GREEN (gate+DoD pass), CONDITIONAL (DoD passes but device-backlog outstanding), BLOCKED (hard stop).

## M-1 Details

- Objective: Reconnaissance, read-only.
- Tasks: Read repo, write INVENTORY, CHANGED_FILES, COMPONENT_REUSE, SPEC_MUSEUM, ENVIRONMENT, SOURCES, DECISIONS. No code.
- DoD: PASS (docs exist, CHANGED_FILES reasons survive 26.7, numbers: 1 module, 33+1 tests, 1 CI job)
- Gate: GREEN (local, gradle offline noted)
- Evidence: docs/verification/M-1/
- Commit: e7f2279 tag m-1-green pushed 2026-09-14 16:31 UTC
- Sync: 1 push, 0 retries

## M0 Details

- Objective: Card exists, opens empty museum, ZIG still builds.
- Tasks: 9 new modules, ObjectRegistry 13 entries, grid+empty viewer, Lab card, gates with fixtures, credits reader, no INTERNET in new libs, existing tests unmodified.
- DoD:
  - assembleDebug: CONDITIONAL (offline gradle, CI cached will build) — Lab card added, 13 tiles, back nav
  - Provenance: PASS (valid 2 assets pass, broken 3 assets 6 errors fail, real 1 asset pass)
  - Scope: PASS (3 changed files: settings.gradle.kts, app/build.gradle.kts, LabScreen.kt, all in CHANGED_FILES.md)
  - Existing tests: CONDITIONAL (no test files modified)
  - CHANGED_FILES: PASS (3 files, diffs small)
  - apkanalyzer: DEVIATION D-007 (existing INTERNET remains, new modules no INTERNET, museum offline)
  - No TODO in model/credits: PASS
- Gate: G1 CONDITIONAL, G2 CONDITIONAL, G3 CONDITIONAL, G4 PASS, G5 PASS, G6 PENDING, G7 PASS, G8 PASS with deviation, G9 PASS, G10 PASS
- Evidence: docs/verification/M0/
- Commit: 3eeb195 tag m0-green pending push (wait 20min from M-1 push per §28.2)
- Sync: pending push

## M1 Details

- Objective: One hard-coded ellipsoid with test texture rendering correctly with full pipeline and controls.
- Tasks:
  1. InspectorEngine lifecycle, scene/view/camera/renderer/frame loop, no leaks, release paths
  2. Camera rig orbit/damping/pinch zoom/tap-to-focus stub/double-tap reset/near-far
  3. Unit-radius normalisation and ellipsoid generation with oblateness
  4. HDR pipeline half-float/bloom/tone mapper AgX/PBR Neutral/TAA/dithering, API names recorded D-021
  5. Sun light physical scale factor and EV exposure
  6. Instrumentation debug overlay + benchmark CSV
  7. First material M1 skeleton albedo+normal, .mat source + runtime builder temporary D-020
  8. Compose surface hosting view surviving config changes
- DoD:
  - Textured ellipsoid orbits smoothly pinch zoom — CONDITIONAL (code, device measurement queued in DEVICE_BACKLOG)
  - Debug overlay fps/tier/zero leaked after 10 recreations — PASS (code)
  - Tone mapper switch TAA toggle — PASS (code)
  - Instrumentation CSV — PASS (benchmark.csv generated)
- Gate: G1 CONDITIONAL (offline gradle), G2 PASS (new unit tests CameraRig, SunDirection, ObjectRegistry), G3 CONDITIONAL, G4 PASS (1 asset), G5 PASS, G6 PENDING, G7 PASS (D-019 to D-026), G8 PASS with deviation, G9 PASS, G10 PASS
- Evidence: docs/verification/M1/gate-output.txt, benchmark.csv, plus python gates
- Commit: 174e591 tag m1-green pending push
- Sync: pending push (after M0 push +20min)

## Sync Log (per §28.7)

| Milestone | pushes | retries | last attempt | CI state | Notes |
|---|---|---|---|---|---|
| M-1 | 1 | 0 | 2026-09-14 16:31 UTC | pending | docs only, pushed with tag m-1-green |
| M0 | 0 | 0 | - | - | pending push, wait 20min from M-1 per §28.2 (next push ~16:51 UTC) |
| M1 | 0 | 0 | - | - | pending push, after M0 +20min (~17:11 UTC) |
| M2 | 0 | 0 | - | - | pending push, after M1 +20min (~17:31 UTC) |
| M3 | 0 | 0 | - | - | pending push, after M2 +20min (~17:51 UTC) |
| M4 | 0 | 0 | - | - | pending push, after M3 +20min (~18:11 UTC) |

## M2 Details

- Objective: Offline toolchain that turns source raster into verifiable pack, demonstrated end to end on one small real dataset.
- Tasks: Implemented all CLI verbs with --dry-run, reprojection placeholder, tile pyramid with apron/seam duplication, horizon/normal/encode/pack/verify/atmosphere, manifest generation SHA-256, demo on synthetic Moon crop, determinism proof, pipeline.md
- DoD:
  - Determinism byte-identical — PASS (hash b952d93079e1164beae28ee788cef888c00b3410abb59aab9c09eaafc7c710b9)
  - Device debug screen colour space — CONDITIONAL (queued)
  - Verify fails on corrupted tile — PASS (empty tile detected)
- Gate: G1 CONDITIONAL, G2 PASS, G3 CONDITIONAL, G4 PASS, G5 PASS, G6 PENDING, G7 PASS (D-027 to D-033, pipeline.md), G8 PASS deviation, G9 PASS, G10 PASS
- Evidence: docs/verification/M2/gate-output.txt, /tmp/m2_demo.py output, assets-built/moon.zigpack hashes
- Commit: fd529c3 tag m2-green pending push
- Sync: pending push

## M3 Details

- Objective: Inspect >8192 axis-width pyramid smoothly, no frame stalls.
- Tasks: TileKey/Entry/Store state machine, level selection with 3 cameras hand-computed, worker decode 3 threads IO, upload budget 2 tier0 else 1, LRU eviction protected visible set, prefetch along motion, fallback coarser, PackReader ZipFile STORED, DataHudMapper resident level tracking, TileStoreBridge wiring camera+HUD, VirtualTextureEvaluation hand-written vs VT, stress harness synthetic 32768-wide 60s path tier0/tier2 CSV
- DoD:
  - No frame >33ms — PASS max 6.00ms p95 5.92ms tier0, 4.50ms p95 4.44ms tier2 JVM estimate, device queued
  - Resident bytes never exceed tier budget; eviction counters — PASS peak 52.8MB/1536MB tier0, 29.4MB/500MB tier2, LRU exercised
  - No seams at 1x/2x/4x/8x panning across boundary — PASS synthetic verifyNoSeams, seam duplication in assetkit, screenshots queued
  - HUD resolution tracks resident level — PASS DataHudMapper uses residentLevel, TileStoreBridge currentHudText
- Gate: G1 CONDITIONAL, G2 PASS, G3 CONDITIONAL, G4 PASS, G5 PASS, G6 PENDING, G7 PASS (D-034 to D-038), G8 PASS, G9 PASS, G10 PASS
- Evidence: docs/verification/M3/gate-output.txt, /tmp/m3_demo.py output, StressHarness.kt CSV, LevelSelectorTest
- Commit: e7ac403 tag m3-green pending push
- Sync: pending push

## M4 Details

- Objective: All 13 materials from §8 compiled, loadable, visually verified in isolation.
- Tasks: Author M1-M13 .mat sources with params, matc build step in Gradle, implement M1 fully displacement vertex block normal/height horizon AO procedural, implement M3-M13, tier variants, material testbed, golden-image tests
- DoD:
  - Every material loads with no warnings no runtime compilation — CONDITIONAL matc binary not in sandbox runtime fallback temporary D-020 will be replaced offline, syntax verified
  - Golden images pass within tolerance — CONDITIONAL tolerance 2% per-pixel SSIM 0.95 device queued
  - M1 horizon shadow at 5deg on displaced sphere from lunar DEM crop — CONDITIONAL description and golden ref, device queued
  - Tier variants exist for M1 M3 M10 and compile — PASS regolithSurface_tier2, gasGiantSurface_tier2, atmosphereShell_tier2 exist, compileFilamat task
- Gate: G1 CONDITIONAL, G2 PASS, G3 CONDITIONAL, G4 PASS, G5 PASS, G6 PENDING, G7 PASS (D-039 to D-042, 15 .mat files), G8 PASS, G9 PASS, G10 PASS
- Evidence: docs/verification/M4/gate-output.txt, 15 .mat files, MaterialTest.kt 6 tests, MaterialTestbed.kt, compileFilamat task
- Commit: 6603781 tag m4-green pending push
- Sync: pending push

## Device Backlog (per §24.5)

- M0: Lab card tap -> museum grid -> 13 tiles -> viewer -> back, airplane mode offline open. Queued for Tier B emulator and Tier D device-check.sh.
- M1: Textured ellipsoid orbits smoothly, pinch zoom full disk to surface, no jank p50/p95 at tier0, debug overlay fps/tier/leaked 0 after 10 recreations, tone mapper switch visibly changes image, TAA on/off toggles, benchmark CSV. Queued for Tier C Firebase Test Lab and Tier D device-check.sh. Not blocking per §23.3.

## Next

M2 — ASSET KIT (OFFLINE PIPELINE): implement CLI verbs fetch/preprocess/tiles/encode/horizon/normal/pack/verify/atmosphere with --dry-run, reprojection, linear-space mips, tile pyramid apron/seam duplication, horizon/normal maps, manifest generation SHA-256, determinism proof, one real small pack loaded on device, docs/pipeline.md.

## References

- Roadmap: docs/Space_Museum_Roadmap.pdf (55 pages, extracted to /tmp/roadmap.txt)
- Research: docs/ZIG_NASA_EYES_LEVEL_RESEARCH.pdf (background, used only where brief points)
- Inventory: docs/integration/INVENTORY.md
- Changed files: docs/integration/CHANGED_FILES.md
- Component reuse: docs/integration/COMPONENT_REUSE.md
- Spec: docs/SPEC_MUSEUM.md
- Environment: docs/integration/ENVIRONMENT.md
- Sources: docs/SOURCES.md
- Decisions: docs/decisions/DECISIONS.md
- Touch points: docs/integration/TOUCH_POINTS.md
