# B3 — Star-tracker flag map: every gate from camera to OrientationProvider

Item R3-B3, 2026-09-04. All statements below are verified against current source (file:line given);
this is a map of what EXISTS in code, not what the phase docs promised.

## 0. The one switch

```
StarTrackerConfig.ENABLED   const val = false    fusion/StarTrackerConfig.kt:14
```

There is exactly ONE switch in the entire pipeline: a compile-time `const val`, default `false`,
with the documented safety contract "flag false must result in ZERO behavioral difference
anywhere in app" (StarTrackerConfig.kt:8-9). It is read at exactly TWO production sites
(grep `StarTrackerConfig.ENABLED` in `app/src/main`):

| # | Site | What it gates |
|---|------|---------------|
| G1 | `ui/screens/CompassARScreen.kt:859` | whether the `ImageAnalysis` use case is bound to the camera at all |
| G2 | `startracker/fusion/AttitudeBlender.kt:45` | whether `blend()` computes or passes through |

Because both sites read the SAME `const`, the two gates are NOT independent — they flip
together, by design (the CompassARScreen comment at :852-858 explicitly records the decision
to reuse the master flag rather than add a sibling switch). With the default `false`, the
Kotlin compiler dead-code-eliminates everything behind both gates
(AttitudeBlender.kt:39-44 documents this).

## 1. Stage-by-stage map (camera → OrientationProvider)

For each stage: file, default state, and what happens when the flag is OFF.

| Stage | File(s) | Flag OFF (default) | Flag ON today |
|---|---|---|---|
| Camera binding (G1) | `ui/screens/CompassARScreen.kt:860-868` | `bindToLifecycle(owner, selector, preview)` — byte-for-byte the pre-project call; no analysis use case | binds `cameraFrameObserver.getUseCase()` additionally |
| Frame observer | `ui/screens/CameraFrameObserver.kt` | object still constructed (`remember{}` at CompassARScreen.kt:228) and `onSensorTimestamp` fed (:240) — but the analyzer never receives a frame because the use case is never bound; its executor thread never spawns (thread starts lazily on first analyzer task); `shutdown()` runs in `DisposableEffect`. **Inert.** | analyzer runs; does NOT touch pixels: stores `latestImageTimestampNanos`, debug-log ~1/s (debug builds only). **No Y-plane → GrayscaleImage adapter exists** (tracked as missing in TrackingLoop.kt:143-162) |
| Detection | `startracker/detection/*` (StarDetectionPipeline, Centroider, StarBlobDetector, …) | no production caller at all (tests only) | unchanged — nothing feeds it from the frame observer |
| Solver | `startracker/solver/*` (LostInSpaceSolver, QuadCandidateBuilder, RansacOutlierRejector, …) | no production caller (tests only) | unchanged |
| Tracking loop | `startracker/tracking/TrackingLoop.kt`; `LiveSensorAdapter` at :164-180 is the documented wiring shape, NOT wired | no production caller (tests only) | unchanged — `LiveSensorAdapter.onGyroData`/`onNewFrame` have no callers in `app/src/main` |
| Confidence ladder | `startracker/diagnostics/ConfidenceLadderCoordinator.kt` ("final arbiter before AttitudeBlender") | no production caller (tests only) | unchanged |
| Blend (G2) | `startracker/fusion/AttitudeBlender.kt:45-48` | `blend()` returns `BlendResult(existingFusedQuaternion, currentMagnetometerWeight)` — exact passthrough; active path compile-time eliminated. **And the method has no production caller**, so even this passthrough never executes in the app | `blendActive()` math (SLERP by confidence × staleness decay, mag-weight recommendation) — but still no production caller, so nothing calls it |
| OrientationProvider | `astro_engine/OrientationProvider.kt` | **knows nothing about the star tracker**: zero `startracker` imports; emits `SkyOrientation` from rotation-vector/accel-mag + SLERP + declination + AR calibration exactly as pre-project | unchanged — the blend→provider feedback wire does not exist anywhere |

## 2. What "OFF" costs (inertness audit)

With `ENABLED=false` the only star-tracker-related objects that exist at runtime are:
- the `CameraFrameObserver` instance (holds two `volatile Long`s and an unstarted executor;
  receives `onSensorTimestamp` writes ~1/s — a long field write, no allocation, no logging);
- `CameraFrameObserver.shutdown()` at compose disposal (no-op on an unstarted executor).

Everything else — detection, solver, tracking, blending, catalog (ingest only at build time
per CATALOG_SOURCING) — is not instantiated in `app/src/main` at all. Verified by grep:
`AttitudeBlender(`, `TrackingLoop(`, `LiveSensorAdapter(`, `ConfidenceLadderCoordinator(`
have zero non-test, non-self instantiations.

## 3. What flipping the ONE switch to `true` actually does today

1. G1 turns on: the camera binds the extra `ImageAnalysis` use case (real, measurable
   behavioral difference: extra active stream; debug logs at ≤1/s).
2. G2 turns on: `blend()` would compute — but no production code calls it.
3. **Nothing else changes**: frames are logged, not detected; no frame→GrayscaleImage
   adapter; no gyro feed into TrackingLoop; no blend result fed back into
   OrientationProvider. Flag ON ≠ a working star tracker — the live wiring gap is
   exactly the PHASE6/PHASE7 integration that remains unapplied pending a real Android
   build (standing pass-2 decision).

"Flip order" is therefore trivial today: there is a single flag, and flipping it alone
cannot produce a half-blended attitude because the consumer side (blend→provider) has no
wire at all. If/when PHASE6/7 live wiring lands, the interesting order question becomes
real (e.g. enabling the frame feed before the blend consumer would be safe; the reverse
would blend against a TrackingLoop that never receives observations and thus stays
NO_LOCK → passthrough, also safe). The current single-const design makes an unsafe
intermediate state unreachable.

## 4. Two-switch independence — verdict

Not two switches. One `const val`, two read sites, flipping atomically together.
Independence is intentionally absent (CompassARScreen.kt:852-858 documents why reusing
the master flag was chosen over a sibling flag). No second flag exists anywhere in
`app/src/main` (grep for `useTracking|useBlend|enableTracking|enableBlend|useSolver|useGyro|starTrackerEnabled|isEnabled`
inside startracker/orientation-related sources returns nothing beyond `StarTrackerConfig`).
