# Phase 7 Integration Patch — Self-Calibration Camera Intrinsics & Distortion Refinement (Documented, Ready-to-Apply, Environment Blocked)

**Status:** ENVIRONMENT STILL BLOCKED, LIVE WIRING WAS NOT PERFORMED, DOCUMENTED PATCH PROVIDED INSTEAD

This document contains the exact proposed diff for ARProjectionEngine.kt and StarTrackerConfig / new SelfCalibrationConfig showing precisely where and how self-calibration would be wired, with before/after excerpts, list of pre-existing tests that MUST pass before and after, and instructions for human engineer.

## Hard Gate Decision (Task 0 / Task 5)

Per Task 0 lightweight env check:
- `./gradlew --version` → `curl: (35) OpenSSL SSL_connect: SSL_ERROR_SYSCALL in connection to services.gradle.org:443` (same as Phases 1-6)
- `java -version` → not found, no JDK, .gradle-installs empty, wrapper 9.3.1 missing, apt-get permission denied
- `python3 3.11.2 + numpy 2.4.6` available — substitute verification used
- Pre-existing test suite: Cannot run due to blocked Gradle/Java — cannot confirm baseline passes
- Decision: **STOP at isolated implementation (Tasks 1-4), do NOT touch ARProjectionEngine.kt live**. Produce documented patch instead. This is correct disciplined outcome per hard gate matching Phase 6.

Per Task 5 gating rule: "only if pre-existing + Phases1-6 tests all pass else produce PHASE7_INTEGRATION_PATCH.md" — condition NOT met (cannot run tests), so we MUST produce this doc, not live edit.

## Isolated Implementation Completed (Tasks 1-4)

### Task 1: DistortionModel
- File: `app/src/main/java/com/alijafari/red/astronomy/startracker/calibration/DistortionModel.kt`
- Model: Brown-Conrady radial `1+k1*r2+k2*r4` + tangential `2*p1*x*y+p2*(r2+2*x^2)` / `p1*(r2+2*y^2)+2*p2*x*y`
- Undistort: iterative fixed-point 20 iterations, epsilon 1e-8, same as OpenCV iterative undistort
- Pixel variants: distortPixel/undistortPixel using fx,fy,cx,cy,skew
- Round-trip verified via python_crosscheck_phase7.py: error <1e-6 for mild/moderate/strong distortion across center/edge/corner points
- Python cross-check 3 points:
  - Ideal (0.1,0.2) -> Distorted (0.100495,0.201540) -> Undistorted (0.1,0.2) err 7.56e-11
  - Ideal (0.3,-0.2) -> Distorted (0.302471,-0.201258) err 2.83e-12
  - Ideal (0.0,0.0) -> Distorted (0,0) err 0

### Task 2: IntrinsicsRefiner
- File: `IntrinsicsRefiner.kt`
- ObservationPair: predictedIdealPixel, observedPixel, idealNormalized (x,y)
- Separate linear LS: u = fx*x + skew*y + cx (3 params), v = fy*y + cy (2 params) via normal equations AtA/Atb + Gaussian elimination partial pivot threshold 1e-12
- Iterative 10 max convergence 1e-6 RMS
- minObs 6, distribution check span <10% width → decline
- Sweep results from python_crosscheck_phase7.py (headline table):
```
Noise\Obs | 4 | 10 | 30 | 100
0.0px | FAIL (expected minObs gate) | rms=0.00 fxErr=0.0 | rms=0.00 fxErr=0.0 | rms=0.00 fxErr=0.0 |
0.2px | FAIL | rms=0.11 fxErr=0.1 | rms=0.12 fxErr=0.1 | rms=0.11 fxErr=0.0 |
0.5px | FAIL | rms=0.28 fxErr=0.7 | rms=0.26 fxErr=0.1 | rms=0.28 fxErr=0.0 |
1.0px | FAIL | rms=0.35 fxErr=0.3 | rms=0.53 fxErr=0.1 | rms=0.57 fxErr=0.1 |
```
- Degenerate/insufficient must refuse not garbage: tested clustered near same location → decline, 3 obs <6 → decline

### Task 3: DistortionRefiner
- File: `DistortionRefiner.kt`
- Linear LS for k1,k2,p1,p2 given fixed intrinsics: x_d - x = x*r2*k1 + x*r4*k2 + 2*x*y*p1 + (r2+2x^2)*p2, etc.
- minObs 10, minSpan 0.5, maxR2 <0.1 → decline (clustered near center unobservable)
- Overfitting guard: |k1|,|k2|>1.0 or |p1|,|p2|>0.1 → decline
- Sweep from python_crosscheck_phase7.py:
```
Noise\Obs | 10 | 30 | 100
0.0 | k1Err=0.0000 k2Err=0.0000 rms=0.00000 | k1Err=0.0000 k2Err=0.0000 rms=0.00000 | k1Err=0.0000 k2Err=0.0000 rms=0.00000 |
0.001 | k1Err=0.0003 k2Err=0.0003 rms=0.00049 | k1Err=0.0001 k2Err=0.0000 rms=0.00057 | k1Err=0.0000 k2Err=0.0001 rms=0.00057 |
0.005 | k1Err=0.0019 k2Err=0.0119 rms=0.00251 | k1Err=0.0114 k2Err=0.0144 rms=0.00288 | k1Err=0.0023 k2Err=0.0019 rms=0.00293 |
```
- Clustered near center: correctly declines "clustered spanX=0.18 spanY=0.18 maxR2=0.02"

### Task 4: CameraProfileCache
- File: `CameraProfileCache.kt`
- Interface get/put/merge weighted by sampleCount
- InMemory ref impl: weighted running average (100*existing + 20*new)/120
- Merge convergence test: good fx=1000 (200 samples) + bad fx=1500 (10 samples) → merged 1023.81, close to good, down-weights early bad batch
- DESIGN ONLY SharedPreferences stub doc keyed by CameraCharacteristics lens facing+focal+sensor hash: example `FACING_BACK_F_4.2_S_5.6x4.2`

## Proposed Diff for ARProjectionEngine.kt

### Current tiers (before)

```kotlin
enum class IntrinsicsSource {
    CALIBRATED_HARDWARE,        // LENS_INTRINSIC_CALIBRATION
    ESTIMATED_PHYSICAL_SENSOR,  // focal length + sensor size
    FALLBACK_DEFAULT            // 63.5° FOV fallback
}
fun getCameraIntrinsics(context): CameraIntrinsics {
    // 1. hardware calibration
    if (intrinsicCal != null) return CALIBRATED_HARDWARE
    // 2. physical sensor size
    if (focalLengths != null && sensorSize != null) return ESTIMATED_PHYSICAL_SENSOR
    // 3. fallback
    return FALLBACK_DEFAULT
}
```

### Proposed after (gated self-calibration)

```kotlin
enum class IntrinsicsSource {
    CALIBRATED_HARDWARE,
    ESTIMATED_PHYSICAL_SENSOR,
    SELF_CALIBRATED_CACHED,  // NEW: refined via star tracker self-calibration, cached
    FALLBACK_DEFAULT
}

// New config (matching StarTrackerConfig pattern)
object SelfCalibrationConfig {
    const val ENABLED: Boolean = false // default DISABLED, zero behavioral difference when off
    const val MIN_SAMPLES_FOR_INTRINSICS: Int = 20
    const val MIN_SAMPLES_FOR_DISTORTION: Int = 50
}

fun getCameraIntrinsics(context: Context?, calibrationCache: CameraProfileCache? = null): CameraIntrinsics {
    // 1. hardware
    // 2. physical sensor
    // ... existing ...

    // NEW TIER: self-calibrated cached, gated, before fallback
    if (SelfCalibrationConfig.ENABLED && calibrationCache != null && context != null) {
        try {
            val deviceLensKey = computeDeviceLensKey(context) // facing + focal + sensor hash
            val cachedProfile = calibrationCache.get(deviceLensKey)
            if (cachedProfile != null && cachedProfile.sampleCount >= SelfCalibrationConfig.MIN_SAMPLES_FOR_INTRINSICS) {
                // Use cached refined profile
                val result = CameraIntrinsics(
                    fx = cachedProfile.fx,
                    fy = cachedProfile.fy,
                    cx = cachedProfile.cx,
                    cy = cachedProfile.cy,
                    skew = cachedProfile.skew,
                    activeArrayWidth = ..., // from cached or current
                    activeArrayHeight = ...,
                    sensorOrientation = ...,
                    isLensFacingBack = true,
                    source = IntrinsicsSource.SELF_CALIBRATED_CACHED
                )
                logIntrinsicsTier(result.source)
                return result
            }
        } catch (e: Exception) {
            // fall through to fallback
        }
    }

    // 3. fallback (existing)
    return FALLBACK_DEFAULT
}

// Helper: keyed by CameraCharacteristics
fun computeDeviceLensKey(context: Context): String {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = ... // rear camera
    val chars = cameraManager.getCameraCharacteristics(cameraId)
    val facing = chars.get(CameraCharacteristics.LENS_FACING)
    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
    return "FACING_${facing}_F_${focalLengths?.firstOrNull()}_S_${sensorSize?.width}x${sensorSize?.height}"
}
```

**Placement justification:** SELF_CALIBRATED_CACHED is after hardware tiers (hardware still preferred when available) but before FALLBACK_DEFAULT (refined estimate better than generic 63.5° fallback). This matches task requirement.

### Undistort-centroids / forward-distort-overlay split

This is critical for angular accuracy:

**Current flow (simplified, before):**
```kotlin
// Detection pipeline gives observed pixel centroids (distorted by lens)
// Solver directly uses observed pixels as ideal (ignores distortion) → error
val unitVectors = centroids.map { pixelToUnitVector(it, intrinsics) } // assumes no distortion
```

**Proposed after (gated):**
```kotlin
// 1. Detection: centroids are distorted (real lens)
val distortedCentroids = detectionPipeline.detect(image) // observed distorted pixels

// 2. Undistort centroids for solver (solver expects ideal pinhole)
val distortionModel = if (SelfCalibrationConfig.ENABLED) cachedProfile.toDistortionModel() else DistortionModel.noDistortion()
val idealCentroids = distortedCentroids.map { (uDist, vDist) ->
    distortionModel.undistortPixel(uDist, vDist, intrinsics.fx, intrinsics.fy, intrinsics.cx, intrinsics.cy)
}
val unitVectors = idealCentroids.map { pixelToUnitVector(it, intrinsics) }
val attitude = attitudeSolver.solve(unitVectors, catalogVectors)

// 3. Forward-distort overlay: when projecting catalog stars to screen, apply distortion forward
//    so overlay matches distorted camera image
fun projectAltAzWithDistortion(..., distortionModel: DistortionModel): Offset? {
    val idealPixel = projectAltAzNoDistortion(...) // pinhole ideal
    val distortedPixel = distortionModel.distortPixel(idealPixel.x, idealPixel.y, fx, fy, cx, cy)
    // then apply sensorToViewMatrix etc.
    return distortedPixel
}
```

This split ensures:
- Solver gets undistorted ideal pixels → attitude error reduced
- Overlay projection applies forward distortion → star labels align with distorted camera image

### CameraProfile conversion helpers (proposed)

```kotlin
fun CameraProfile.toDistortionModel() = DistortionModel(k1, k2, p1, p2)
fun CameraProfile.toCameraIntrinsics(...): CameraIntrinsics = ...
```

## Pre-Existing Tests That MUST Pass Before and After

Per Phase 0-6 audit, these must pass both before and after applying patch (with flag OFF, behavior identical):

- RefractionTest (Phase 1, 4 tests)
- GrayscaleImageTest, SyntheticStarFieldGeneratorTest, BackgroundEstimatorTest, StarBlobDetectorTest, CentroiderTest, StarDetectionPipelineTest (Phase 2, 6 files)
- CatalogIngestorTest, AngularSeparationIndexTest, QuadPatternIndexTest, CatalogSerializerTest (Phase 3, 4 files)
- SyntheticSkyObserverTest, AttitudeSolverTest (Phase 4, 2 files)
- ConfidenceStateMachineTest, QuaternionIntegratorTest, RelockPolicyTest (Phase 5, 3 files)
- AttitudeBlenderTest (Phase 6, 1 file) — safety no-lock passthrough
- DistortionModelTest, IntrinsicsRefinerTest, DistortionRefinerTest, CameraProfileCacheTest (Phase 7, 4 new files — must pass as isolated before live wiring)
- SkyOrientationProjectionTest, HeroSkyProjectionTest, FrameTransformationEngineTest, ARCalibrationPromptTest, SatelliteARConsistencyTest, AstroTimeTest, SGP4PropagatorTest (if exist, from Phase 0 baseline)

**Total: ~20+ test files, each named individually, must pass with flag OFF.**

**With flag ON and synthetic calibration data:** IntrinsicsRefinerTest should show recovery within 0.1px RMS for 100 obs 0 noise, DistortionRefinerTest within 1e-4 for k1, and CameraProfileCacheTest merge down-weights bad early batch.

## Instructions for Human Engineer

1. Fix environment: ensure JDK 17+ and Gradle 9.3.1 can be downloaded (fix TLS/network issue that blocked Phases 1-7). Verify `./gradlew --version` works, `java -version` shows JDK.
2. Run baseline: `./gradlew :app:testDebugUnitTest` and confirm ALL pre-existing tests PASS. List each by name. If any fail, STOP and fix baseline before proceeding.
3. Apply patch: edit ARProjectionEngine.kt to add SELF_CALIBRATED_CACHED tier, gated by SelfCalibrationConfig.ENABLED (default false). Keep change minimal, isolated commit. Add SelfCalibrationConfig.kt with ENABLED=false matching StarTrackerConfig pattern. Add computeDeviceLensKey helper.
4. Verify flag OFF: re-run full test suite with flag OFF (default), confirm zero change — all tests still PASS, identical to baseline. This proves safety contract.
5. Verify flag ON: create synthetic calibration data (known true profile, generate observations via python_crosscheck_phase7.py logic), accumulate via SelfCalibrationEngine, refine, cache, then call getCameraIntrinsics with cache and flag ON, confirm tier SELF_CALIBRATED_CACHED selected and refined intrinsics within expected error. Test undistort-centroids / forward-distort-overlay split with synthetic image: distorted centroids → undistorted → solver attitude error < fallback.
6. If all passes, commit live wiring as separate commit clearly labeled "Live wiring — Phase 7 self-calibration, flag OFF by default, zero behavioral difference when disabled".
7. Do NOT merge to main until real device testing confirms self-calibration improves angular accuracy vs fallback and does not regress.

## Cumulative Environment Status (Phases 1-7)

| Phase | Automated Execution Achieved? | Substitute Verification Used | Risk |
|-------|-------------------------------|------------------------------|------|
| 1 | No — Gradle TLS failure, no Java | Static analysis, manual calc for refraction | Medium |
| 2 | No — same block | Python weighted centroid error 0.0756 px PASS, sigma-clipped median robust | High — 1149 lines new |
| 3 | No — same block, python+numpy now available | Python haversine vs dot 1e-9 PASS, quad descriptor square 0.707 ratios, k-vector O(1) | High — 6 new files |
| 4 | No — same block | Python Davenport q-method: zero noise 0 arcsec, 0.01° noise 27 arcsec, TRIAD 0 arcsec, Jacobi 1,3,3,4 PASS | Very High — nontrivial linear algebra |
| 5 | No — same block | Synthetic event sequences, analytic gyro 5°/s 10s → 50° yaw, trigger boundaries | Very High — state machine + quaternion |
| 6 | No — same block, hard gate stops live wiring | Isolated AttitudeBlender no-lock passthrough 1e-9 + documented patch | Critical — first touching live code but blocked |
| 7 | No — same block, hard gate stops live wiring | Python cross-check Phase7: distortion round-trip <1e-6, intrinsics sweep table, distortion sweep table, clustered decline, cache merge convergence | Critical — 7 phases without execution, ~4000+ lines pure Kotlin never run via JUnit |

**Plain-language escalation:** This is now SEVEN phases in a row where new code has shipped without ever being executed by automated test runner. Phases 2-7 total ~4000+ lines of new pure-Kotlin code for detection, catalog, solver, tracking, fusion, calibration — all reasoned about, manually cross-checked via Python where possible, but never run as Kotlin via JUnit. The project's own Phase 6 and Phase 7 hard gates correctly block live wiring into OrientationProvider and ARProjectionEngine until baseline tests pass before and after — these gates are working as designed to prevent regression. However, the underlying environment blocker (Gradle TLS failure downloading 9.3.1 from services.gradle.org, no JDK) must be resolved by a human with access to proper development machine and network before Phase 6/7 live wiring and before Phase 8+ can proceed. Do not just note and forget — escalate to human with real device.

## If Environment Not Fixed Before Phase 7, Phase 7 Will Be BLOCKED

Per Phase 7 Task 5: gated live wiring only if pre-existing + Phases1-6 tests all pass else produce PHASE7_INTEGRATION_PATCH.md

This document IS that deliberate stop — live wiring NOT performed, documented patch provided instead. This is correct, disciplined outcome.
