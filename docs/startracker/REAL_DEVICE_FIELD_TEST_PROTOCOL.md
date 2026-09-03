# Real Device Field Test Protocol — Star Tracker End-to-End

**Purpose:** Validate star tracker improves angular accuracy vs existing sensor-only fusion, under real sky conditions, with real device camera and sensors.

**Prerequisites:**
- Environment fixed: JDK 17+, Gradle 9.3.1 works, `./gradlew :app:testDebugUnitTest` passes all ~30+ tests
- Gated patches applied per MASTER_FILE_MANIFEST.md, with flags OFF by default
- Real Android device with rear camera, gyroscope, magnetometer, GPS
- Dark sky location, away from city lights, clear night, minimal cloud

## Phase 0: Baseline (Flag OFF)

1. Install app with `StarTrackerConfig.ENABLED=false`, `SelfCalibrationConfig.ENABLED=false` (default)
2. Go to CompassARScreen / AR screen
3. Point to known bright stars (Sirius, Vega, Polaris, etc.) and record:
   - Projected position vs actual star position in camera image (pixel error)
   - Azimuth/altitude error vs known star position from catalog (using GPS time/location)
   - Time to lock, stability over 10 seconds
4. Repeat for 10 different sky regions, 5 different device orientations (portrait, landscape, tilted)
5. Record intrinsics tier selected from logcat: `Intrinsics tier selected: ...` — should be CALIBRATED_HARDWARE or ESTIMATED_PHYSICAL_SENSOR or FALLBACK_DEFAULT
6. This is baseline for comparison

## Phase 1: Star Tracker Enabled (Flag ON, No Self-Calibration)

1. Enable `StarTrackerConfig.ENABLED=true` via override (for test build only, not default)
2. Repeat same measurements as baseline
3. Expected: attitude error reduced, especially azimuth (magnetometer is noisy)
4. Measure:
   - Attitude error arcsec: compare star-solved attitude vs ground truth (known star positions)
   - Success rate: % of frames where FULL_LOCK or MARGINAL_LOCK achieved
   - Time to first lock
   - Confidence decay when moving fast or pointing to low-star region
5. Record FailureReason and UserGuidanceHint from logs

## Phase 2: Self-Calibration Enabled

1. Enable `SelfCalibrationConfig.ENABLED=true` + `StarTrackerConfig.ENABLED=true`
2. Accumulate observations across locks:
   - Point to different sky regions, accumulate 20+ observations for intrinsics, 50+ for distortion
   - Check `SelfCalibrationEngine` accumulated counts
   - Trigger refinement, check refined profile vs fallback
3. Verify tier `SELF_CALIBRATED_CACHED` selected in logcat after enough samples
4. Measure improvement in attitude error vs fallback intrinsics
5. Test undistort-centroids / forward-distort-overlay split:
   - Without undistort: attitude error higher at edges/corners where distortion large
   - With undistort: error reduced, overlay aligns with distorted image

## Phase 3: Sky Conditions

Test under different sky conditions per ValidationMatrixRunner sky-condition sweep:

- **Dark:** rural, no light pollution, clear, new moon — should get FULL_LOCK easily, low RMS error
- **Suburban:** some light pollution, but still visible stars — should get MARGINAL_LOCK or FULL_LOCK with moderate noise
- **Urban:** heavy light pollution, only brightest stars visible — may get NO_LOCK or MARGINAL_LOCK, guidance should say POINT_TO_DARK_SKY or DARKER_ENVIRONMENT
- **Cloud:** overcast, few stars — should get NO_LOCK, guidance HOLD_STEADY or POINT_TO_DARK_SKY, failure reason NO_STARS_DETECTED or TOO_FEW_STARS

For each condition, record:
- FrameQuality from FrameQualityClassifier
- FailureReason
- LockConfidence
- UserGuidanceHint (should map to appropriate UI proposal)

## Phase 4: Device/Lens Sweep

Test on different devices if available:

- Narrow FOV (telephoto lens) ~30° — fewer stars per frame, but higher resolution, should still work but need more precise pointing
- Normal FOV ~60° — typical main camera, should work well
- Wide FOV ~90° — ultrawide, more stars, but more distortion, self-calibration should help
- Ultrawide FOV ~120° — very wide, heavy distortion, need distortion refinement

For each, record:
- Success rate
- RMS error
- Distortion coefficients refined vs fallback
- Whether SELF_CALIBRATED_CACHED tier improves over FALLBACK_DEFAULT

## Phase 5: Hemisphere Mirrored

- If possible, test in southern hemisphere (or simulate by setting latitude to -35°)
- Verify HeroSkyProjection fix: East right, West left when facing North (south hemisphere)
- Before fix: East left (same as north) — wrong, not mirrored
- After fix: East right — correct, mirrored, matches ARProjectionEngine expected ordering
- Test `testSouthernHemisphereEastWestOrdering_MirroredExpectation` should PASS after fix

## Phase 6: Dynamic Motion

- Hold device steady, get FULL_LOCK
- Then move device slowly (gyro integration should maintain attitude with decaying confidence)
- Then move fast, trigger relock
- Measure:
  - QuaternionIntegrator error vs true motion
  - ConfidenceStateMachine decay
  - RelockPolicy trigger
  - Time to relock after fast motion

## Phase 7: Rotation Sweep Systematic Bias

- On tripod, rotate device 360° in yaw, 10° steps
- At each yaw, measure attitude error
- Plot error vs yaw — should be flat, no systematic bias (max-min <50 arcsec)
- If bias present, indicates bug in solver or projection

## Metrics to Record

- Attitude error arcsec: RMS, median, 95th percentile
- Success rate: % frames with FULL_LOCK or MARGINAL_LOCK
- Acquisition discrepancy magnitude (degrees) on EVERY FULL_LOCK acquisition (angle between pre-acquisition fused attitude and star-solved attitude at the moment of lock) - logged so the keep-vs-ramp decision for AttitudeBlender's 0.9x acquisition snap (see docs/startracker/ATTITUDE_BLENDER_ACQUISITION_NOTE.md, PARKED) is made from field data
- Time to first lock
- Time to relock
- FailureReason distribution
- FrameQuality distribution
- UserGuidanceHint distribution
- Intrinsics tier hit rate
- Self-calibration sample counts and refined profile convergence

## Safety Checks

- With flag OFF, behavior identical to before — zero regression
- With flag ON but NO_LOCK, attitude passthrough identical to existing fused (within 1e-9)
- AMBIGUOUS never adopts ambiguous attitude, always goes to NO_LOCK and discards
- No crash, no ANR, no excessive battery drain

## Reporting

Create report with:

- Device model, Android version, camera specs
- Location, date, time, weather, light pollution estimate
- For each test condition: table of metrics
- Comparison baseline vs star tracker enabled vs self-calibration enabled
- Screenshots of AR overlay alignment
- Logcat excerpts for intrinsics tier, failure reasons, confidence
- Conclusion: does star tracker improve angular accuracy? By how much? Under what conditions does it fail gracefully?

## Escalation

If real-device testing shows star tracker does NOT improve accuracy, or introduces regression, or fails to lock in dark sky:

- STOP, do not merge to main
- File issue with metrics and logs
- Investigate: is detection failing? Catalog matching failing? Solver failing? Calibration wrong?
- Use diagnostics: FailureReason, FrameQuality, AmbiguityDetector to pinpoint

If testing shows improvement and graceful degradation:

- Commit live wiring with flag OFF by default
- Document flag ON for future release
- Plan for gradual rollout with analytics
