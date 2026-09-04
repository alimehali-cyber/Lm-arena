# Z-W2 — Android adapters (UNEXECUTED diffs), 2026-09-04

`W2_ANDROID_ADAPTERS_2026-09-04.patch` — unified diffs, **NOT applied**, Android-only
code that cannot compile in the offline harness (androidx.camera / ImageProxy /
Context). Label: UNEXECUTED. All hunks are default-OFF flag-gated.

## Flag map — ordered switch list (bring-up order)

| # | Flag (StarTrackerConfig) | Default | Gates |
|---|---|---|---|
| 0 | `ENABLED` | false | master switch (pre-existing; stays false) |
| 1 | `PIPELINE_CAMERA_FEED` | false | CameraFrameObserver → StarTrackerPipeline (PHASE5) |
| 2 | `TRACKER_TO_ORIENTATION_PHASE6` | false | OrientationProvider result consumption (PHASE6) |
| 3 | `PROJECTION_SELF_CALIBRATED_PHASE7` | false | ARProjectionEngine SELF_CALIBRATED_CACHED tier (PHASE7) |

Order rationale: frames must flow (1) before the orientation layer consumes results
(2), and the overlay must not change scale (3) until the pipeline+orientation path is
observed healthy; every step is independently revertible to prior behavior by its own
flag.

## Adapter 1 — CameraFrameObserver → pipeline (PHASE5)

Off-main-thread by construction (the analyzer already runs on the single-thread
`backgroundExecutor`); DROP-NOT-QUEUE enforced twice: CameraX
`STRATEGY_KEEP_ONLY_LATEST` (existing) + an `AtomicBoolean` busy latch that drops any
frame arriving while `pipeline.process` is still running (a stale sky frame is
worthless). Y-plane → `GrayscaleImage` (row-stride aware, luminance only; the
pipeline's BackgroundEstimator handles illumination). Result sink callback keeps the
observer decoupled from the provider.

## Adapter 2 — pipeline → OrientationProvider (PHASE6)

`OrientationProvider.onStarTrackerResult(result)`: records the latest tracker attitude,
lock confidence and timestamp (thread-safe @Volatile) — always, so OD1
acquisition-discrepancy logging works even while the emission path is off. The
EMISSION path (blending tracker attitude into the emitted orientation via
AttitudeBlender under TRACKER_TO_ORIENTATION_PHASE6) is deliberately stubbed with a
pointer to the device-trial step: the tracker attitude is catalog→CAMERA and needs the
live sensorOrientation/displayRotation frame rotation, which exists only on device.

## Adapter 3 — ARProjectionEngine SELF_CALIBRATED_CACHED tier (PHASE7 + gap fix)

New `IntrinsicsSource.SELF_CALIBRATED_CACHED` between CALIBRATED_HARDWARE and
ESTIMATED_PHYSICAL_SENSOR. `publishSelfCalibratedIntrinsics(...)` accepts a
NORMALIZED profile (fx as fraction of array width, principal point in [0,1]) and
de-normalizes against the live active array — the gap fix: a cache expressed in pixels
would silently mis-scale the overlay after any resolution change; normalized units +
de-normalization at read time keep the tier correct across array sizes.

## Validation status

UNEXECUTED. The pure-Kotlin counterparts these adapters call (StarTrackerPipeline,
FullFieldVerifier, LostInSpaceSolver, ConfidenceLadderCoordinator, AttitudeBlender) are
harness-validated (161/0/0; W1 evidence). The Android halves compile-check only on a
device build. Loosened: none; no threshold changed anywhere in this item.
