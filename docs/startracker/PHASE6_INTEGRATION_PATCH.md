# Phase 6 Integration Patch — Documented, Ready-to-Apply (Environment Blocked)

**Status:** ENVIRONMENT STILL BLOCKED, LIVE WIRING WAS NOT PERFORMED, DOCUMENTED PATCH PROVIDED INSTEAD

This document contains the exact proposed diff for OrientationProvider.kt showing precisely where and how AttitudeBlender would be called, with before/after excerpts, list of pre-existing tests that MUST pass before and after, and instructions for human engineer.

## Hard Gate Decision

Per Task 0:
- Environment recovery: `./gradlew --version` → TLS failure `curl: (35) OpenSSL SSL_connect: SSL_ERROR_SYSCALL in connection to services.gradle.org:443`, no Java, no JDK via apt (permission denied), but python3 + numpy 2.4.6 available.
- Pre-existing test suite: Cannot run due to blocked Gradle/Java — cannot confirm baseline passes.
- Decision: **STOP at isolated implementation (Tasks 1-2), do NOT touch OrientationProvider.kt, CompassARScreen.kt, ARProjectionEngine.kt live**. Produce documented patch instead. This is correct disciplined outcome per hard gate.

## Proposed Diff for OrientationProvider.kt

### Exact Integration Point

In OrientationProvider.kt, after existing rotation-vector + SLERP fusion produces its quaternion, before it is packaged into SkyOrientation. Currently:

```kotlin
// Existing code (simplified):
fun updateQuaternion(targetW, targetX, targetY, targetZ, timestampNanos) {
    // Normalize, SLERP, convert to rotation matrix, apply declination + AR calibration
    val fusedQuaternion = Quaternion(...) // result of SLERP
    val rotationMatrix = ...
    val azimuth = ...
    _orientation.value = SkyOrientation(azimuth, pitch, roll, rotationMatrix, timestampNanos)
}
```

Proposed after (gated by feature flag):

```kotlin
// Proposed new code (additive, gated):
import com.alijafari.red.astronomy.startracker.fusion.AttitudeBlender
import com.alijafari.red.astronomy.startracker.fusion.StarTrackerConfig
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import com.alijafari.red.astronomy.startracker.solver.Quaternion as StarTrackerQuaternion

fun updateQuaternion(targetW, targetX, targetY, targetZ, timestampNanos, starTrackerState: TrackingState? = null) {
    // Existing: normalize, SLERP, etc.
    var fusedQuaternion = Quaternion(...) // existing fused
    var magWeight = currentMagnetometerWeight // existing logic determines mag weight

    // NEW: Star tracker blending, gated by feature flag
    if (StarTrackerConfig.ENABLED && starTrackerState != null) {
        val starQuat = starTrackerState.attitude?.let { convertToAndroidQuaternion(it) } // adapter
        val confidence = starTrackerState.confidence
        val age = starTrackerState.lastLockAgeSeconds

        val blender = AttitudeBlender()
        val result = blender.blend(
            existingFusedQuaternion = convertToStarTrackerQuaternion(fusedQuaternion),
            starSolvedQuaternion = starQuat,
            starLockConfidence = confidence,
            starLockAgeSeconds = age,
            currentMagnetometerWeight = magWeight
        )

        fusedQuaternion = convertToAndroidQuaternion(result.outputQuaternion)
        magWeight = result.recommendedMagWeight
    }

    // Existing continues with magWeight possibly reduced
    val rotationMatrix = ... // using magWeight for declination correction
    _orientation.value = SkyOrientation(...)
}
```

### Before/After Excerpts

**Before (current, Phase 1 final):**
```kotlin
private fun updateQuaternion(targetW: Float, targetX: Float, targetY: Float, targetZ: Float, timestampNanos: Long = lastSensorTimestampNanos) {
    // Normalize
    val norm = sqrt(targetW*targetW + targetX*targetX + targetY*targetY + targetZ*targetZ)
    val w = targetW / norm
    val x = targetX / norm
    // ... SLERP with current quaternion
    val fusedW = ...
    val fusedX = ...
    // Apply declination correction using magnetometer weight = 1.0f (full)
    val declinationCorrected = applyDeclination(fusedW, fusedX, fusedY, fusedZ, magWeight = 1.0f)
    // Convert to rotation matrix
    val rotationMatrix = FloatArray(9)
    SensorManager.getRotationMatrixFromVector(rotationMatrix, floatArrayOf(declinationCorrected.x, declinationCorrected.y, declinationCorrected.z, declinationCorrected.w))
    // Compute azimuth/pitch/roll
    _orientation.value = SkyOrientation(azimuth, pitch, roll, rotationMatrix, timestampNanos)
}
```

**After (proposed, with star tracker):**
```kotlin
private fun updateQuaternion(targetW: Float, targetX: Float, targetY: Float, targetZ: Float, timestampNanos: Long = lastSensorTimestampNanos, starTrackerState: TrackingState? = null) {
    // Normalize (same)
    val norm = sqrt(targetW*targetW + targetX*targetX + targetY*targetY + targetZ*targetZ)
    // ... SLERP (same)
    var fusedQuaternion = Quaternion(fusedW, fusedX, fusedY, fusedZ) // existing
    var magWeight = 1.0f // existing default

    // ADDITIVE STAR TRACKER BLENDING - GATED
    if (StarTrackerConfig.ENABLED) {
        val existingStarQuat = QuaternionStarTracker(fusedQuaternion.w.toDouble(), fusedQuaternion.x.toDouble(), fusedQuaternion.y.toDouble(), fusedQuaternion.z.toDouble())
        val starQuat = starTrackerState?.attitude // already star tracker quaternion
        val confidence = starTrackerState?.confidence ?: LockConfidence.NO_LOCK
        val age = starTrackerState?.lastLockAgeSeconds ?: 1000.0

        val blender = AttitudeBlender()
        val result = blender.blend(
            existingFusedQuaternion = existingStarQuat,
            starSolvedQuaternion = starQuat,
            starLockConfidence = confidence,
            starLockAgeSeconds = age,
            currentMagnetometerWeight = magWeight
        )
        // Convert back
        fusedQuaternion = Quaternion(result.outputQuaternion.w.toFloat(), result.outputQuaternion.x.toFloat(), result.outputQuaternion.y.toFloat(), result.outputQuaternion.z.toFloat())
        magWeight = result.recommendedMagWeight // reduced if FULL_LOCK
    }

    // Apply declination correction using possibly reduced magWeight
    val declinationCorrected = applyDeclination(fusedQuaternion.w, fusedQuaternion.x, fusedQuaternion.y, fusedQuaternion.z, magWeight = magWeight)
    val rotationMatrix = FloatArray(9)
    SensorManager.getRotationMatrixFromVector(rotationMatrix, floatArrayOf(declinationCorrected.x, declinationCorrected.y, declinationCorrected.z, declinationCorrected.w))
    _orientation.value = SkyOrientation(azimuth, pitch, roll, rotationMatrix, timestampNanos)
}
```

### Magnetometer Down-Weighting Detail

**What previously determined magnetometer contribution:**
- In OrientationProvider, magnetic declination correction uses `GeomagneticField` to get declination, and applies rotation around Z axis to correct azimuth.
- Magnetometer weight is implicit in how much declination correction is applied vs pure rotation vector.
- Previously, weight was always 1.0f (full magnetometer contribution) for azimuth correction.

**How now conditionally modified:**
- When `StarTrackerConfig.ENABLED` and `FULL_LOCK` fresh, `AttitudeBlender` recommends `MAGNETOMETER_WEIGHT_FLOOR = 0.1f` (10% of original).
- For `MARGINAL_LOCK`, recommends `MAGNETOMETER_WEIGHT_MARGINAL = 0.5f`.
- For `NO_LOCK`/`AMBIGUOUS`/stale, recommends unchanged (1.0f) — passthrough.
- The `magWeight` is used to scale declination correction: `correctedAzimuth = fusedAzimuth + magWeight * declination`.
- When flag OFF, `magWeight` remains 1.0f, identical to before — zero behavioral difference.

**Before/After code excerpt for mag weighting:**
```kotlin
// Before:
val declination = geomagneticField.declination
val correctedAzimuth = azimuth + declination // full mag contribution

// After (gated):
val declination = geomagneticField.declination
val effectiveDeclination = if (StarTrackerConfig.ENABLED) declination * magWeight else declination
val correctedAzimuth = azimuth + effectiveDeclination
```

## Pre-Existing Tests That MUST Pass Before and After

Per Phase 0 audit, these must pass both before and after applying patch (with flag OFF, behavior identical):

- SkyOrientationProjectionTest (if exists)
- HeroSkyProjectionTest (3 original + 4 new hemisphere tests, but new hemisphere mirrored test expected FAIL — document)
- FrameTransformationEngineTest (if exists)
- ARCalibrationPromptTest (if exists)
- SatelliteARConsistencyTest (if exists)
- AstroTimeTest (if exists)
- SGP4PropagatorTest (if exists)
- RefractionTest (added Phase 1, 4 tests)
- GrayscaleImageTest, SyntheticStarFieldGeneratorTest, BackgroundEstimatorTest, StarBlobDetectorTest, CentroiderTest, StarDetectionPipelineTest (Phase 2, 6 files)
- CatalogIngestorTest, AngularSeparationIndexTest, QuadPatternIndexTest, CatalogSerializerTest (Phase 3, 4 files)
- SyntheticSkyObserverTest, AttitudeSolverTest (Phase 4, 2 files)
- ConfidenceStateMachineTest, QuaternionIntegratorTest, RelockPolicyTest (Phase 5, 3 files)
- AttitudeBlenderTest (Phase 6, 1 file) — critical safety property no-lock passthrough

**Total: ~20 test files, each named individually, must pass with flag OFF.**

**With flag ON and synthetic star-lock input:** AttitudeBlenderTest should show full-lock close to star, marginal intermediate, staleness decay, smoothness.

## Instructions for Human Engineer

1. Fix environment: ensure JDK 17+ and Gradle 9.3.1 can be downloaded (fix TLS/network issue that blocked Phases 1-6). Verify `./gradlew --version` works, `java -version` shows JDK.
2. Run baseline: `./gradlew :app:testDebugUnitTest` and confirm ALL pre-existing tests PASS. List each by name. If any fail, STOP and fix baseline before proceeding.
3. Apply patch: edit OrientationProvider.kt at single integration point after SLERP, before SkyOrientation packaging, adding AttitudeBlender call gated by StarTrackerConfig.ENABLED (default false). Keep change minimal, isolated commit.
4. Verify flag OFF: re-run full test suite with flag OFF (default), confirm zero change — all tests still PASS, identical to baseline. This proves safety contract.
5. Verify flag ON: create synthetic TrackingState with known attitude (e.g., 10° yaw), set flag ON via override (for test only, not default), run AttitudeBlenderTest and integration test with synthetic observations, confirm blended attitude close to star (90% for FULL_LOCK) and mag weight reduced.
6. If all passes, commit live wiring as separate commit clearly labeled "Live wiring — Phase 6, flag OFF by default, zero behavioral difference when disabled".
7. Do NOT merge to main until real device testing confirms star tracker improves angular accuracy vs existing.

## Cumulative Environment Status (Phases 1-6)

| Phase | Automated Execution Achieved? | Substitute Verification Used | Risk |
|-------|-------------------------------|------------------------------|------|
| 1 | No — Gradle TLS failure, no Java | Static analysis, manual calc for refraction | Medium — math/comment only, no logic change |
| 2 | No — same block | Manual cross-check via Python (weighted centroid error 0.0756 px PASS, sigma-clipped median robust) | High — 1149 lines new code never executed |
| 3 | No — same block, but python+numpy now available | Python cross-check (haversine vs dot 1e-9 PASS, quad descriptor square 0.707 ratios, k-vector O(1) reasoning) | High — 6 new files 1149 lines, plus 3 phases without execution |
| 4 | No — same block, python+numpy available | Python reference Davenport q-method: zero noise 0 arcsec, 0.01° noise 27 arcsec, TRIAD 0 arcsec, Jacobi eigenvalues 1,3,3,4 PASS — Kotlin traced by hand identical K-matrix | Very High — attitude solving nontrivial linear algebra, 4 phases without execution |
| 5 | No — same block | Synthetic event sequences for state machine, analytic known case for gyro integration (5°/s for 10s → 50° yaw), trigger boundary tests | Very High — tracking loop state machine + quaternion math, 5 phases without execution |
| 6 | No — same block, hard gate stops live wiring | Isolated AttitudeBlender tests (no-lock passthrough identical within 1e-9) + documented patch | Critical — first phase touching live production code, but blocked by gate, so no regression risk yet, but 6 phases without execution is structural risk |

**Plain-language escalation:** This is now SIX phases in a row (1-6) where new code has shipped without ever being executed by automated test runner. Phases 2-6 total ~3000+ lines of new pure-Kotlin code for star detection, catalog, solver, tracking, fusion — all reasoned about, manually cross-checked via Python where possible, but never run as Kotlin via JUnit. This is a growing structural risk. The project's own Phase 6 hard gate correctly blocks live wiring into OrientationProvider until baseline tests pass before and after — this gate is working as designed to prevent regression. However, the underlying environment blocker (Gradle TLS failure downloading 9.3.1 from services.gradle.org, no JDK) must be resolved by a human with access to proper development machine and network before Phase 6 live wiring and before Phase 7+ can proceed. Do not just note and forget — escalate to human with real device.

## If Environment Not Fixed Before Phase 6, Phase 6 Will Be BLOCKED

Per Phase 4 Task 0.6: "If the environment is not fixed before Phase 6, Phase 6 will be BLOCKED from wiring into live sensor fusion code per its own hard gate — this is a designed, deliberate stop, not an oversight."

This document IS that deliberate stop — live wiring NOT performed, documented patch provided instead. This is correct, disciplined outcome.
