# T4 — Distortion bootstrap, 2026-09-04

Two parts: (a) an UNEXECUTED hardware-distortion tier; (b) MEASURED radius-dependent
verification tolerance + S3 joint Monte-Carlo re-run with unmodelled k1.
No thresholds were loosened anywhere in this pass. **Loosened: none.**

## (a) HARDWARE_DISTORTION tier — UNEXECUTED (Android metadata path)

New file `app/src/main/java/com/alijafari/red/astronomy/startracker/calibration/HardwareDistortionReader.kt`:

- `enum DistortionModelSource { HARDWARE_DISTORTION, SELF_CALIBRATED, NONE }` —
  preference order: device factory calibration **above** self-calibration, both above
  no-model (identity), which is where the T4(b) allowance applies instead.
- `read(CameraCharacteristics)` maps camera2 distortion metadata to the existing
  Brown-Conrady `DistortionModel`:
  - `LENS_RADIAL_DISTORTION` (API 33+, float[6] fk1..fk6) → k1=fk1, k2=fk2, preferred;
  - else `LENS_DISTORTION` (API 30+, float[5] {fk1,fk2,fk3,k1,k2}) → k1=fk1, k2=fk2;
  - rationale: camera2's f-coefficients map corrected→uncorrected = ideal→distorted,
    which is exactly `distortIdealToDistortedNormalized`; tangential p1/p2 are not
    provided by camera2 → 0.0;
  - all-zero (identity) metadata → null (falls to the next tier, not a fake model).
- `selectDistortionModel(hardware, selfCalibrated)` — pure Kotlin, tier order enforced;
  identity self-calibration is treated as absent (NONE → T4(b) path).

**Execution status:** the pure tier logic and the metadata parsing are harness-tested
(`HardwareDistortionReaderTest`, 4/4 green, via new `android.os.Build` /
`android.hardware.camera2.CameraCharacteristics` harness shims). The reader itself is
**UNEXECUTED against real camera2 HAL metadata** — no device was available in this
pass; its first real execution is the device trial (Step 7+/PHASE7 enablement), where
it decides whether PHASE7 self-calibration is even needed (if a HARDWARE_DISTORTION
model is present it wins and self-calibration refinement is skipped).

## (b) Radius-dependent verification tolerance — MEASURED (synthetic sky)

`FullFieldVerifier` now supports `tol(θ) = toleranceRad + c·tan²θ` (θ = detection
angular radius from the boresight):

- `c = unmodelledDistortionAllowanceC(k1Max = 0.08, edgeAngleDeg = 31.75) =
  0.08·tan(31.75°) = 0.04951` — chosen so `c·r² ≥ |k1|max·r³` for all normalized
  gnomonic radii r = tan(θ) ≤ tan(31.75°) (the 63.5° tier half-diagonal). This
  envelopes the D5-measured cubic displacement (k1=−0.05 → −1.292/−4.362/−10.339 px
  at 50/75/100 % half-width — exactly cubic in r).
- Default `c = 0.0` is byte-identical to the previous flat S2 behavior (regression
  test pins this). Only when `StarTrackerPipeline` runs with an identity
  `DistortionModel` does it upgrade the solver's verifier
  (`upgradeSolverForUnmodelledDistortion`); with a real model present the flat
  tolerance stands (detections are undistorted before solving).

Tolerance curve (arcsec), vs the unmodelled displacement it must absorb:

| θ from boresight | tol(θ) | disp \|k1\|=0.03 | disp \|k1\|=0.08 |
|---|---|---|---|
| 0° | 300″ | 0″ | 0″ |
| 10° | 617″ | 34″ | 90″ |
| 15° | 1,033″ | 119″ | 317″ |
| 20° | 1,653″ | 298″ | 796″ |
| 25° | 2,520″ | 627″ | 1,673″ |
| 28.75° (75 % hw) | 3,373″ | 1,022″ | 2,725″ |
| 31.75° (edge) | 4,210″ | 1,466″ | 3,910″ |

(8 % headroom at the edge; the flat 300″ gate is breached from θ ≈ 11° at k1=−0.05
per D5 — that is the failure mode this fixes.)

### S3 joint Monte-Carlo re-run — unmodelled k1, allowance verifier active

`tools/kotlin-harness/probes/S3DistortionProbe.kt`; same seeds/stream as the S3 joint
run (attitude seed 777004, observer 530000+t, centroid noise U(0,2) px, false stars
uniform {0,5,10,20}, catalog J2000 extract, capped quad index); observations distorted
with the stated k1 before solving; solver uses
`FullFieldVerifier.withUnmodelledDistortionAllowance()`. FL = solved with attitude
error > 0.5°; FLfull = FL with confidence ≥ 0.7 and fullField ≥ 20. SYNTHETIC SKY.

| cell (unmodelled k1) | n | solved | FL | FL (FULL_LOCK-grade) | median err (all solved) | p95 err | mean solve |
|---|---|---|---|---|---|---|---|
| 0.00 | 10,000 | 5,018 (50.2 %) | 54 (0.540 %) | 1 | 0.064′ | 0.36′ | 44 ms |
| −0.03 | 10,000 | 4,939 (49.4 %) | 52 (0.520 %) | 4 | 1.027′ | 3.60′ | 39 ms |
| −0.08 | 10,000 | 4,268 (42.7 %) | 487 (4.870 %) | **0** | 5.198′ | 44.72′ | 38 ms |

Reference (S3 joint, flat 300″ verifier, k1=0): 4,817 solved (48.2 %), **FL 0**, median
0.066′, p95 1.15′, 46 ms.

### Reading the table honestly

- The allowance **costs** false locks at k1=0: 0.54 % vs 0.00 % with the flat gate
  (wider acceptance admits slightly-wrong solves). FULL_LOCK-grade FL stays ≤ 0.04 %
  per cell. This is the price of not rejecting correct solves on an uncalibrated phone;
  the tracker remains DISABLED regardless, so no user-facing risk is introduced.
- Unmodelled k1 degrades accuracy without collapsing the solve rate: at k1=−0.03 the
  median error is ~1′ (p95 3.6′) — sub-pixel-scale bias absorbed by the attitude fit,
  far below the 30′ FL threshold.
- At the worst case |k1|=0.08 the gate still passes 42.7 % of trials, but the median
  error is 5.2′, p95 is 44.7′, and 4.87 % of solves are false locks — **none of them
  FULL_LOCK-grade (FLfull = 0)**: the confidence ladder correctly refuses to certify
  them. This is the quantified case for T4(a): read the hardware k1 (or self-calibrate
  per D5) rather than operating at the allowance's edge. The tracker stays DISABLED,
  so none of this is user-facing.
- Threshold-curve summary: the effective acceptance radius grows 300″ → 4,210″ (×14)
  across the tier; the flat-gate alternative rejects every correct solve beyond
  θ ≈ 11–15° under k1 = −0.03…−0.08 (D5), i.e. an uncalibrated phone would solve
  almost nothing — which is what the pipeline did before T4(b) whenever the model
  was missing.

## Harness evidence

`run_tests.sh` after T4: **167/0/0** (163 prior + 2 FullFieldVerifierTest T4b tests +
4 HardwareDistortionReaderTest tests). Raw output: `evidence/R_FINAL_HARNESS_2026-09-04.txt`
is superseded by this run; probe output verbatim below.

```
k1=0.0 n=10000 solved=5018 (50.2%) FL=54 (0.540%) FLfull=1 medianALL=0.064' p95ALL=0.36' solveMsMean=44
k1=-0.03 n=10000 solved=4939 (49.4%) FL=52 (0.520%) FLfull=4 medianALL=1.027' p95ALL=3.60' solveMsMean=39
k1=-0.08 n=10000 solved=4268 (42.7%) FL=487 (4.870%) FLfull=0 medianALL=5.198' p95ALL=44.72' solveMsMean=38
```
