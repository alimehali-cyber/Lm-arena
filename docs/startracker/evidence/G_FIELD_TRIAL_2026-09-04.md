# G-pass — Field-Trial Guide, 2026-09-04 (in progress)

Goal: replace the retired debug overlay with a guided, level-based field trial a
non-developer can execute looking at the sky. Scope exemption: debug-only Compose
UI (BuildConfig.DEBUG / debug source set). Nothing in release builds (CI dex
inspection — to be extended to the fieldtrial package with the UI commit).
Commit prefix G-. "Loosened: none."

## P0 — what this build can actually do (answered before design)

**(a) Was the pipeline wired before this pass? NO.** Evidence (pre-G-P0 tree = d67f50f):

- `docs/startracker/evidence/W2_ANDROID_ADAPTERS_2026-09-04.patch:2` — "NOT applied
  to the source tree"; the adapters existed only as this unapplied diff.
- `app/src/main/java/com/alijafari/red/astronomy/ui/screens/CameraFrameObserver.kt`
  (pre-pass :72–107): the analyzer read metadata only ("NO pixel processing beyond
  metadata in this phase") and always closed the frame; no pipeline symbol existed.
- `app/src/main/java/com/alijafari/red/astronomy/astro_engine/OrientationProvider.kt`:
  no star-result hook of any kind (grep `onStarTrackerResult` = 0 hits pre-pass).
- `app/src/main/java/com/alijafari/red/astronomy/astro_engine/ARProjectionEngine.kt`
  (pre-pass :73–81): IntrinsicsSource had exactly 3 tiers — no SELF_CALIBRATED_CACHED.
- `CompassARScreen.kt:891` (pre-pass): the ImageAnalysis use case was bound only when
  the compile-time const `StarTrackerConfig.ENABLED` (false) — and even bound, the
  observer did nothing with pixels.
So the chain CameraFrameObserver → StarTrackerPipeline → OrientationProvider did not
exist in any built APK before commit `9eb9376`.

**(b) Runtime flags and what each ACTUALLY switches (after G-P0 `9eb9376`):**

- `ENABLED` — now REAL: gates the ImageAnalysis binding (`CompassARScreen.kt:905`,
  via `StarTrackerDebugFlags.runtime().enabled`) and (ANDed with the next flag) the
  per-frame pipeline feed (`CameraFrameObserver.kt:126`).
- `PIPELINE_CAMERA_FEED` (analysis gate) — REAL: with ENABLED, Y-plane frames are
  converted and processed (`:126–156`), off-main-thread on the analyzer executor,
  drop-not-queue (`pipelineBusy` latch `:81`, `:129`, `:139`).
- `TRACKER_TO_ORIENTATION_PHASE6` — HOOK ONLY: `OrientationProvider.onStarTrackerResult`
  (`:124`) records the tracker attitude/lock/timestamp (always, when attached) for the
  OD1 acquisition-discrepancy measurement; the emission/blend is deliberately NOT
  implemented (camera→device rotation is the documented device-trial integration
  step). Toggling this flag changes nothing observable yet — stated here so it is
  never reported as live.
- `PROJECTION_SELF_CALIBRATED_PHASE7` — HOOK ONLY: tier `SELF_CALIBRATED_CACHED`
  (`ARProjectionEngine.kt:81`) + `publishSelfCalibratedIntrinsics` (`:122`) +
  runtime gate (`:148`); nothing publishes a profile yet, so the tier is unreachable
  until the calibration flow exists.
Release builds: `StarTrackerDebugFlags.runtime()` resolves the consts (all false) —
`installOverridesProvider` (StarTrackerDebugFlags.kt:61) is called only from debug
code; the feed/tier branches are dead code in release (bit-identical flags, D1 tests).

**Part B verdict: L8/L10/L11 are LIVE in this build** (the pipeline really runs on
frames when the tester taps Turn on — StarTrackerRuntime builds the capped index from
the debug-only asset `app/src/debug/assets/startracker/hyg_v36_vle6.5_j2000.csv` and
attaches the sinks). L9 uses the tracker attitude directly through the same pure
projection (green ring) vs the compass path (blue ring). PHASE6 blend and PHASE7 tier
remain recorded-but-inert, per (b).

**CI proof (green run 33915569313, head fab2f1f):** build ✓ (both variants + dex
assert), unit-tests ✓ — **71 files / 478 tests / 0 failures**, including the new
fieldtrial engine tests: EnginePreludeTest 2/2, InverseProjectionTest 4/4,
ProjectionLockstepTest 2/2 (replica pinned to the real ARProjectionEngine < 1e-3 px),
TapMeasurementTest 4/4. Harness (offline): 189/0/0.

## Progress ledger

| Item | State |
|---|---|
| G-P0 adapters + runtime switchgear | DONE `9eb9376` (+fix `fab2f1f`), CI green above |
| G-2.1 inverse projection (+lockstep mutation proof) | DONE `500bdaf` |
| G-2.2/2.3 TapMeasurement + SunDiagnosis | DONE `e7df396`+`a430eae` |
| G-2.4 target picker + SunEvents | DONE `4a9ced4` |
| G-2.5 probes | runtime collector + FailureWording + GrayPng in G-P0; L9 dual-ring + summary integration pending |
| G-1.3 state machine + JSON store + summary.md | NEXT |
| G-1/G-3 UI (FAB, guide card, levels, night mode, tap confirm, share zip) | pending |
| G-4.2 UI tests, G-4.4 rehearsal zip, G-4.5 screenshots, G-4.6 protocol | pending |
| Old overlay retirement (replace with guide) | with the UI commit |

## Findings (this pass, none fixed — engine untouched per constraints)

1. **Horizon refraction branch discontinuity (pre-existing):**
   `CoordinateEngine.equatorialToHorizontal` steps APPARENT altitude by ~0.71° just
   below the horizon (measured 2026-09-23 Frankfurt sunrise: −1.5006° → −0.7904° in
   <1 s of simulated time; RA/Dec/LAST smooth, so the step is inside the refraction
   branch). Sunrise finding therefore asserts a BRACKET (TargetPickerTest), not point
   equality. Left untouched; flagged for an owner decision.
2. `PipelineResult` has no wall-clock solve time — CFO now records
   `latestPipelineProcessMs` (main-source, release-inert) rather than the engine
   being modified.
3. Repeated sandbox history resets (7×) are absorbed by the documented recovery
   recipe; one near-miss (a stale-base patch would have reverted pushed work) was
   caught by diffing against FETCH_HEAD before committing.

## Honest limits

- Everything Android-runtime (camera feed, pipeline on real frames, probe UI) is
  UNEXECUTED on a device until the trial; compile + unit tests are CI-green.
- The rehearsal (4.4) and screenshots (4.5) are still pending — they will be
  produced before the pass is declared done.
