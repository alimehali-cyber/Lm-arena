# Z-V5 — Exact per-file test counts at pass-3 tip and current tip, 2026-09-04

Method: `run_tests.sh` executed in an isolated `git worktree` at `edef03d` (pass-3 tip)
and at HEAD (`eaaaa62`); PASS lines counted per class from the raw runner output
(pass-3 raw kept at session evidence; both runs green). No doc numbers were trusted.

## Pass-3 tip `edef03d` — machine-verified 138/0/0

| file | tests | | file | tests |
|---|---|---|---|---|
| AngularSeparationIndexTest | 7 | | RelockPolicyTest | 4 |
| ValidationMatrixRunnerTest | 6 | | ConfidenceStateMachineTest | 4 |
| FrameQualityClassifierTest | 6 | | AttitudeSolverTest | 4 |
| ConfidenceLadderCoordinatorTest | 6 | | StarBlobDetectorTest | 4 |
| SyntheticStarFieldGeneratorTest | 6 | | QuadPatternIndexTest | 4 |
| CatalogIngestorTest | 6 | | DistortionRefinerTest | 4 |
| EndToEndSyntheticTest | 5 | | DistortionModelTest | 4 |
| AttitudeBlenderTest | 5 | | QuaternionIntegratorTest | 3 |
| RelativeBearingTest | 6 | | StarDetectionPipelineTest | 3 |
| AmbiguityDetectorTest | 5 | | BackgroundEstimatorTest | 3 |
| CentroiderTest | 5 | | CatalogSerializerTest | 3 |
| IntrinsicsRefinerTest | 5 | | TrackingLoopTest | 2 |
| CameraProfileCacheTest | 5 | | SyntheticSkyObserverTest | 2 |
| HeroSkyProjectionTest | 7 | | LostInSpaceSolverTest | 2 |
| RefractionTest | 6 | | BearingCrossCheckTest | 2 |
| | | | GrayscaleImageTest | 2 |
| | | | SelfCalibrationEngineTest | 2 |

Sum = 138 (125 startracker + 7 HeroSky + 6 Refraction). Matches the pass-3 commit
message "138/0/0" and today's fresh isolated rerun.

## Current tip `eaaaa62` — machine-verified 155/0/0

Same 32 files, byte-identical counts, PLUS 4 files added during the final pass:
+ CoordinateOracleTest 3 (`d8a2e1e`), + MagneticDeclinationTest 7 (`43e360e`),
+ CappedQuadIndexTest 6 (`2bf1d05`), + SyntheticE2ETest 1 (`b7fb71e`).
138 + 3 + 7 + 6 + 1 = **155**.

## Reconciliation of "141"

"141" was an interim hand-tally during the final pass, recorded in
A_ORACLE_CHAIN_CONCLUSIONS.md (:72, "harness now 141/0/0") and repeated in
FINAL_PASS_REPORT.md (:166, "141 pre-final-pass tests ... − recount artifacts").
It is wrong: the machine count at `edef03d` was and is 138, and 141 + 16 ≠ 155 —
the report papered over the gap with "recount artifacts". Both doc lines corrected
in this commit to the verified decomposition 138 + 17 = 155 (17 = 3 + 7 + 6 + 1).
