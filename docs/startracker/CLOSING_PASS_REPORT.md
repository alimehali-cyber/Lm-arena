# Closing-Pass Report — Z-series, 2026-09-04

Every item below carries its own evidence file; this report only signs off. Labels used:
SYNTHETIC-SKY (simulated sky/images through the real code), MODELLED (arithmetic),
MEASURED (vs an external oracle), UNEXECUTED (Android-only, cannot run offline).

## 1. Status table (exact ids, verbatim)

| item | status | commit | evidence |
|---|---|---|---|
| V1 | DONE | cda60d1 | evidence/STANDALONE_RERUN_2026-09-04.txt |
| V2 | DONE | 0a9f080 | evidence/V2_SCOPE_DIFF_2026-09-04.md |
| V3 | DONE | 91d405f | evidence/V3_DECLINATION_PLACEMENT_2026-09-04.md |
| V4 | DONE | eaaaa62 | evidence/V4_D_METRIC_RECOMPUTE_2026-09-04.txt |
| V5 | DONE | 34a759d | evidence/V5_TEST_COUNT_RECONCILIATION_2026-09-04.md |
| V6 | DONE | 6d6c274 | data/startracker/hyg_v36_vle6.5_2026.5.csv(+sidecar) |
| V7 | DONE | 1bcc5b0 | evidence/V7_HYG_REFETCH_2026-09-04.txt |
| P1 | DONE | 0cbae9e | evidence/P1_PROJECTION_TEST_2026-09-04.txt |
| P2 | DONE | 0bed9e2 | evidence/P2_PINHOLE_CHECK_2026-09-04.md |
| P3 | DONE | 9c10099 | evidence/P3_FILL_CENTER_CHECK_2026-09-04.md |
| P4 | DONE | 12477fd | evidence/P4_SENSOR_ORIENTATION_INSPECTION_2026-09-04.md |
| S1 | DONE | 9ab6553 | evidence/FALSE_LOCK_DISSECTION_2026-09-04.md |
| S2 | DONE | 65a6f0d | evidence/S2_FULL_FIELD_VERIFICATION.md |
| S3 | DONE | 030da66 | evidence/S3_D_RERUN_SUMMARY.md (+W1 addendum) |
| S4 | DONE | a43ee2d | evidence/S4_CENTROID_DISTORTION_2026-09-04.md |
| W1 | DONE | c0073ff | evidence/W1_PIPELINE_2026-09-04.md |
| W2 | DONE | 01a30d5 | evidence/W2_ANDROID_ADAPTERS_2026-09-04.md |
| W3 | DONE | e7edce7 | REAL_DEVICE_FIELD_TEST_PROTOCOL.md (replaced) |

All 18 items DONE; none `NOT DONE`. Commits Z-S3..Z-W3 (030da66..e7edce7) are
committed locally but NOT yet pushed: the GitHub token expired mid-pass (see §10);
push them first thing after reconnecting. Every earlier commit was pushed immediately
per the standing rule.

## 2. V1 — standalone rerun

Raw: `evidence/STANDALONE_RERUN_2026-09-04.txt` (109/111 initial → 111/0/0 final;
67 app + 44 zig). Tests changed (corrected, not loosened): AstroTimeTest ΔT 2024/2026
expectations moved from the EM 1986-2005 cubic (t=year−2000 misapplication) to the
engine's actual EM 2005-2050 segment — 73.87054/75.07458 s, tolerance 0.5 s unchanged.
Three oracle-backed tests added (VSOP87 Earth-helio vs astropy, Meeus Ch.25, SMC NED).
**Loosened: none.**

## 3. V2 — scope diff KIND table

9 production paths outside startracker/tools/docs/data/tests (evidence/V2 file):
engine corrections (AstroTime d8a2e1e KIND-B + oracle + mutation proof; CoordinateEngine
d8a2e1e KIND-B + both; LunarSolarEngine d8a2e1e KIND-B + both; VSOP87Engine d8a2e1e
KIND-B + both), catalog values (60155a3, OD3 per-object tables), MagneticDeclination
(43e360e, own proof), ARCalibrationManager (43e360e — moot, reverted by Z-V3),
OrientationProvider (a3a1ca0, UNEXECUTED disclosure), CompassARScreen (mixed; UNEXECUTED
disclosure; B1 part reverted by Z-V3). **Every A5-authorized engine file carries BOTH an
oracle test and a mutation proof.**

## 4. V3 — declination answer

Declination is applied ONCE, at the attitude source: OrientationProvider.updateLocation
(WMM via GeomagneticField, called at 3 sites in CompassARScreen) rotates the sensor
world frame (R_true = R_declination·R_sensor) and the azimuth scalar is derived FROM
that corrected matrix; the overlay is matrix-driven at all 9 projection call sites.
The final-pass B1 scalar correction was therefore a DOUBLE correction (reverted with
the rebase API; MagneticDeclination.kt pure math kept; flag retired to false). No
heading-number UI exists; all displayed numbers are true-referenced. UNEXECUTED on
device — landmark check is Step 1 of the new protocol.

## 5. P2/P3 tables

**P2 (engine verified pinhole; MODELLED linear cost):** | θ | linear error | sky error |
15°: 6.71 px / 19.8′ · 30°: 58.63 px / 141.8′ · 45°: 234.09 px / 411.2′. f_view =
871.75×1.25 = 1090.79 px @63.5° tier, 1080-px width. Oracle test + mutation proof
(asin-linear fails at exactly 6.71 px). **P3:** 63.5° = SENSOR short-side FOV (not
view); FILL_CENTER scale 1.25 exact; view FOVs H 52.68°/V 95.46°/diag 100.70°; corner
(az 26.3379°/alt −44.5938°) → (1080,2400) at 50.344° (theory 50.351°); gnomonic edge
stretch +24.5%/+121.2%/+140.5% matches f·sec²θ. No KIND-B fix warranted.

## 6. S summary (final numbers, W1 refit code; SYNTHETIC-SKY)

Percentiles INCLUDE false locks; FL = solved with error > 0.5° (⇒ ≥ MARGINAL by ladder).
Baseline 0.3px: 727/1000 solved, FL 0, median 0.020′, p95 0.05′. Noise 0–3px: 750/752/
754/749/723/656/554 solved, FL 0 all, medians 0.000–0.585′. False 0–40 @0.3px: 735/586/
426/199/30 solved, FL 0 all. **JOINT MC 10,000: 4,817 solved, FL 0 (0.000%), FULL_LOCK-
grade FL 0**, median 0.066′, p95 1.15′, 46 ms/solve. ACCEPTANCE PASSED (<0.1% joint, 0
at FULL_LOCK) → **"ready for device trial"**; `StarTrackerConfig.ENABLED` verified
**false**. Gate cost proof: no-gate baseline = 727 correct + 197 false (all 4-inlier).
D3 calibration: all 4,817 confidences in [0.7,1.0], FL rate 0.0000. D4 (real chain):
moment 0.05–0.16 px at SNR 10–30 (beats Gaussian fit on tight PSFs); SNR-5 tight-PSF =
below 5σ detection floor. D5 (real DistortionModel): k1=−0.05 → −1.29/−4.36/−10.34 px
at 50/75/100% half-width; round-trip ≤ 6e−6 px; unmodelled k1 at the edge EXCEEDS the
300″ verification tolerance (5.3 px) — calibrate before trials.

## 7. Final harness (unedited) + W1

`bash tools/kotlin-harness/run_tests.sh` → **161/0/0** (raw:
evidence/R_FINAL_HARNESS_2026-09-04.txt); projection harness → **6/0/0** (raw:
evidence/R_FINAL_PROJECTION_2026-09-04.txt). Composition: 138 pre-final-pass + 3 oracle
+ 7 magnetic-declination + 6 capped-index + 1 synthetic-E2E + 4 full-field + 2 pipeline.
W1: StarTrackerPipeline E2E — clean Sgr frame (404 stars) → FULL_LOCK, ff 393/395,
arcsec attitude; lens-cap → NO_LOCK/SENSOR_ONLY. KIND-B refit + verification-confidence
changes each mutation-proofed. S3 re-run on final code keeps FL 0 everywhere with ~16×
better medians.

## 8. G9 items (i)–(vii) — MET / NOT MET

(i) Real-toolchain compile: **NOT MET (still needs device)** — B4 wiring/R2-A1 gate
remain outside the offline compile set; nothing regressed (V2 audit + 161/0/0).
(ii) Declination on device, default ON: **SUPERSEDED — item closed by Z-V3** (declination
was already ON at the source; the B1 default-ON change was a double correction and is
reverted; landmark check = protocol Step 1). (iii) Star spot-checks (Mizar…SMC):
**NOT MET on device** (offline oracle accuracy 0.418′ rms stands; 7-star check =
protocol Step 4). (iv) Sun/Moon/planet overlay on device: **NOT MET** (offline 0.35–
0.6′; protocol Step 2). (v) Star tracker stays DISABLED until false-lock blocker fixed:
**MET** — S1–S3 fixed it (FL 0/10,000 joint incl. gate + refit), ENABLED verified
false, W2/W3 device path documented. (vi) HYG attribution UI: **NOT MET in-app**
(text finalized, CC BY-SA 4.0; placement check = protocol Step 5/attribution note).
(vii) Re-fetch + SHA-verify raw HYG gz: **MET** — Z-V7, two fetches, 784fd90e…468
unchanged; J2000 extract re-derived byte-identical; PM 2026.5 extract + sidecar added.

## 9. Ordered device list (what a human does next, in order)

1. Reconnect GitHub auth; push queued commits S4/W1/W2/W3 (Z-S3..Z-W3).
2. Compile on a real toolchain (`./gradlew :app:testDebugUnitTest`,
   `:app:assembleDebug`) — first actual compile of B4/R2-A1/W2-adjacent code.
3. Execute `docs/startracker/REAL_DEVICE_FIELD_TEST_PROTOCOL.md` (30 min, Steps 1–8),
   filing DEVICE_TRIAL_<date>.md with every recorded number.
4. Decide PHASE6/PHASE7 enablement based on Step 6/7 results (OD1 discrepancies).
5. Place the HYG attribution text in the About/licence screen (Step 5 check).
6. Device calibration of k1 (D5: unmodelled k1=−0.05 breaches the verification
   tolerance at the field edge) via SelfCalibrationEngine before PHASE7.

## 10. New findings flagged (this pass)

1. **V3 retraction**: final-pass B1 was a double correction (V3); retracted + reverted.
2. **S1 mechanism**: false locks are chance 5-ratio descriptor collisions with 4-inlier
   RANSAC consistency — NOT reflections (det(B)>0, flips 4–57× worse in all 3 cases).
3. **S3 gate-cost proof**: pre-gate "92.4% solved" at 0.3px was 72.7% correct + 19.7%
   false; honest rate is 72.7%.
4. **W1 anchor-selection weakness**: flux-ranked top-10 anchors hijackable by merged
   blob pairs in dense low-diversity fields (flagged; not hit in E2E/S3 profiles).
5. **D5 vs S2 interaction**: unmodelled k1=−0.05 (10.3 px at edge) exceeds the 300″
   full-field tolerance — distortion calibration is REQUIRED before device trials.
6. **Solve-rate vs false stars**: 19.9% at 20 false stars (candidate formation
   dominated) — the biggest remaining solver weakness.
7. **Push auth outage**: GH_TOKEN expired mid-pass; commits Z-S3..Z-W3 are committed
   locally (tree clean) and push automatically once reconnected.

— END OF CLOSING-PASS REPORT —
