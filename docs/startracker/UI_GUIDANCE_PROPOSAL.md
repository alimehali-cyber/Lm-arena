# UI Guidance Proposal — Phase 8 (Proposal Only, No Live UI Changes)

**Status:** Proposal document only, per Task 4. No Compose UI, no icons, no Persian localization, no actual screen modifications in this phase.

This proposal describes how UserGuidanceHint (pure enum, no strings) would be surfaced in UI, without implementing it.

## Principle

- Diagnostics layer (Phase 8) produces `UserGuidanceHint` enum, no strings, no Android dependency, pure Kotlin.
- UI layer (separate, future phase) maps enum to localized strings and visual cues.
- This separation keeps diagnostics testable and avoids forbidden UI changes in Phase 8.

## Enum to UI Mapping (Proposed)

| UserGuidanceHint | Proposed UI String (English, not implemented) | Proposed Visual Cue | When Shown |
|------------------|-----------------------------------------------|---------------------|------------|
| NONE | (no message) | (no cue) | FULL_LOCK |
| HOLD_STEADY | "Hold steady" | Small vibration + steady icon | MARGINAL_LOCK or ambiguous |
| POINT_TO_DARK_SKY | "Point to a darker area of sky, away from city lights" | Arrow pointing up, dark sky illustration | NO_STARS, TOO_FEW_STARS |
| WIDEN_FIELD_OF_VIEW | "Try zooming out or moving to see more stars" | Zoom out animation | Insufficient distribution |
| DARKER_ENVIRONMENT | "Find a darker environment, avoid bright lights" | Moon with slash, light pollution warning | HIGH_NOISE, OVEREXPOSED |
| CALIBRATE_COMPASS | "Calibrate compass: move phone in figure-8" | Figure-8 animation | High residual error |
| MOVE_SLOWLY | "Move slowly" | Snail icon + slow motion cue | Gyro stale |
| TILT_UP | "Tilt phone up toward sky" | Arrow up | Frame quality suggests horizon |
| TILT_DOWN | "Tilt down slightly" | Arrow down | Zenith overexposed? |
| ROTATE_LEFT | "Rotate left" | Arrow left | Guidance for searching |
| ROTATE_RIGHT | "Rotate right" | Arrow right | Guidance for searching |
| AVOID_LIGHT_POLLUTION | "Avoid light pollution, move away from streetlights" | Streetlight with slash | High background mean |
| WAIT_FOR_DARKNESS | "Wait for darker conditions" | Sunset icon | Too bright |
| CLEAN_LENS | "Clean camera lens" | Lens with sparkle | Blur detection |
| CHECK_FOCUS | "Check focus, tap to focus" | Focus reticle | Blur |

**Note:** Strings above are proposal only, not implemented. Real implementation would use Android string resources with localization (including Persian per app's existing localization, but not in this phase).

## Placement (Proposed)

- **CompassARScreen.kt** (existing AR screen): small guidance banner at bottom, above floating nav bar, showing hint when confidence != FULL_LOCK.
- **Design:** No Liquid Glass, no new icons (per forbidden), use existing text style, existing icons only.
- **Behavior:** Banner appears when `ConfidenceLadderCoordinator` outputs guidance != NONE, disappears when FULL_LOCK.
- **Animation:** Fade in/out, no Compose visual changes beyond existing patterns.
- **Rate limiting:** Only show guidance if same hint persists for >2 seconds to avoid flicker.

## Gating

- New UI would be gated by same `StarTrackerConfig.ENABLED` flag (default false) + new `SelfCalibrationConfig.ENABLED`? Actually guidance should be gated by star tracker enabled.
- When flag OFF, no banner, zero behavioral difference.

## Why Proposal Only?

Per Phase 8 Task 4: "UI_GUIDANCE_PROPOSAL.md proposal only" — explicitly forbidden to modify Compose UI, Liquid Glass, icons, etc. in this phase. This doc satisfies Task 4 without violating scope.

## Future Work (Not in Phase 8)

- Implement string resources in `res/values/strings.xml` and `fa` localization.
- Add small composable `StarTrackerGuidanceBanner(hint: UserGuidanceHint)` in CompassARScreen, gated.
- Add analytics: log guidance hint shown, confidence, failure reason for field testing.
- Real-device tuning of thresholds for FrameQualityClassifier based on field data.

## Safety

- Guidance hints never affect attitude solving, only user messaging.
- Diagnostics layer remains pure Kotlin, testable without Android.
- No guessing: AMBIGUOUS never shows star-based attitude, only guidance to hold steady and retry.

## Tests Required Before Live UI

- All Phase 8 diagnostics tests pass (FrameQualityClassifierTest, AmbiguityDetectorTest, ConfidenceLadderCoordinatorTest).
- Existing baseline tests still pass with flag OFF.
- Real-device field test: point to dark sky, verify guidance disappears when FULL_LOCK, appears when NO_LOCK.
