# FINAL PASS REPORT — AR sky accuracy + star-tracker chain
**Repo:** `alimehali-cyber/Lm-arena`, branch `arena/01a0676f-lm-arena`, base `d9c83d2`, tip after this report: see `git log`.
**Date:** 2026-09-04 · **Governing rule:** a change is allowed iff it improves accuracy AND provably alters nothing outside the corrected quantity (KIND-A flag-gated / KIND-B oracle-verified; else report-only).
**Environment disclosure:** NO Android runtime, NO Gradle network access. Test gate = offline pure-Kotlin harness `bash tools/kotlin-harness/run_tests.sh`. Everything requiring a device/Android build is labeled UNEXECUTED.
**Incident disclosure:** mid-pass the sandbox was rebuilt (fresh clone destroyed unpushed local commits; the working tree survived). All commits were reconstructed with identical content boundaries and are labeled RECONSTRUCTED in their messages.

---

## 1. Status table

| Item | Status | KIND | Evidence pointer |
|---|---|---|---|
| A1 quad sizing model | DONE (prior pass) | MODELLED | f=0.07272, k=5 → 7.3 MB; `evidence/CATALOG_SIZE_MEASURED_2026-09-03.txt` |
| A2 oracle grid | DONE | — | 6 locations × 6 instants 2000–2030; `tools/oracle/astropy_oracle.py` |
| A3 oracle harness | DONE | — | `tools/oracle/build_probe.sh`, `tools/kotlin-harness/probes/CoordinateOracleProbe.kt` |
| A4 residuals, all routes | DONE — ALL PASS | KIND-B | §2 below; `evidence/ORACLE_RESIDUALS_{BEFORE,AFTER}_FIX.txt`, `ORACLE_CASES*.csv` |
| A5 permanent oracle test | DONE | — | `CoordinateOracleTest.kt` (3 tests) + mutation proof `evidence/ORACLE_TEST_MUTATION_PROOF.txt` |
| A6 refraction chain | DONE — no defect | — | byte-identical to base 60928ba (comments/renames only); `A_ORACLE_CHAIN_CONCLUSIONS.md` |
| B1 magnetic declination | DONE — applied, DEFAULT ON | KIND-B values, flag-gated guardrails | `B1_MAGNETIC_DECLINATION.md`, `evidence/DECLINATION_TABLE_2026-09-04.txt` |
| B2 display-catalog verification | DONE — executed as OD3/B3 | values-only corrections | `B3_CATALOG_VERIFICATION.md` |
| B3 catalog corrections | DONE — 7 stars + SMC | values-only | §4 below |
| B4 sensor-timestamp feed | DONE — applied per OD6 | KIND-A-equivalent (dormant path) | `B4_TIMESTAMP_READERS.md` + addendum; UNEXECUTED on device |
| B5 correspondence dedup | DONE — applied (prior-pass fix, carried into D commit) | dormant path | in-file comment in `LostInSpaceSolver.kt`; disclosed in commit b7fb71e |
| C capped quad index | DONE | machinery, dormant | `C_CAPPED_QUAD_INDEX.md`, `evidence/C4_CAPPED_SWEEP_2026-09-04.txt` |
| D synthetic E2E | DONE — with honest blocker (D6) | dormant path | `D_SYNTHETIC_E2E.md`, `evidence/D_SYNTHETIC_E2E_2026-09-04.txt` |
| E real catalog | DONE | data + tooling | `E1_CATALOG_PROVENANCE.md`, `evidence/E2_REAL_INGEST_2026-09-04.txt` |
| F glue + runbook | DONE | mixed (see §9) | `REAL_DEVICE_FIELD_TEST_PROTOCOL.md` addendum, `evidence/F1_HOST_PROBE_2026-09-04.txt` |
| G proofs/sweep | DONE | — | mutation proofs §10; consistency addenda in PROJECT_STATUS / HARNESS_DISCLOSURE / MASTER_FILE_MANIFEST |
| H this report | DONE | — | `FINAL_PASS_REPORT.md` |
| OD1 acquisition snap | PARKED (Option 1) — no change | — | `ATTITUDE_BLENDER_ACQUISITION_NOTE.md` |
| OD2 HeroSky southern fix | APPLIED (prior pass; landed via commit 1ec3406) | correction | `HeroSkyProjectionTest` |
| OD3 catalog values exemption | APPLIED this pass (B3) | values-only | §4 |
| OD4 declination default-on | APPLIED (B1) | flag-gated | §3 |
| OD5 real catalog | APPLIED (E) | — | §5 |
| OD6 B4 apply (supersedes pass-3) | APPLIED (B4) | KIND-A-equiv | §1 row B4 |
| OD7 report-only residuals | KEPT report-only | — | §12 |

## 2. A4 residual tables — before → after (MEASURED, astropy 8.0.1 oracle, on-sky rms/max arcmin, altitude > 10°, app-weather refraction column)

AR-overlay live routes (CompassARScreen / AstroDispatchEngine call chain):

| Route | n | before rms/max | after rms/max | acceptance | verdict |
|---|---|---|---|---|---|
| star_live (AR overlay + trails) | 651 | 15.4 / 26.1 (median 17.5′ @2026) | **0.418 / 1.681** (0.416/1.722 n=647 post-B3) | < 2′ | PASS |
| DSO subset | 227 | same defect | **0.417 / 1.795** | < 2′ | PASS |
| sun | 14 | 73.414 / 125.210 | **0.350 / 0.794** | < 1′ | PASS |
| moon (topocentric) | 16 | ~2.9 / ~5 (pre-ΔT-fix era) | **0.145 / 0.264** | < 3′ | PASS |
| mercury | 4 | 1223.584 / 2072.795 | **0.537 / 1.067** | < 2′ | PASS |
| venus | 12 | 69.869 / 107.552 | **0.417 / 0.760** | < 2′ | PASS |
| mars | 6 | 693.569 / 1120.311 | **0.450 / 0.789** | < 2′ | PASS |
| jupiter | 12 | 346.487 / 434.250 | **0.603 / 1.179** | < 2′ | PASS |
| saturn | 12 | 416.250 / 533.032 | **0.557 / 0.943** | < 2′ | PASS |
| uranus | 12 | 141.322 / 176.871 | **0.399 / 0.781** | < 2′ | PASS |
| neptune | 12 | 76.434 / 86.258 | **0.440 / 0.814** | < 2′ | PASS |

Secondary engine route (FrameTransformationEngine — not the live overlay; HeroSky render path uses the same corrected coordinates):

| Route | n | after rms/max (vs refr1010 oracle) | note |
|---|---|---|---|
| fte | 651 | **0.291 / 0.466** | best of all routes (full precession+nutation+Bennett) |

Diagnostic (not a live route): star_live vs NO-refraction oracle 2.128/5.353 rms/max = expected excess (app adds refraction); moon_geocentric 48.6/61.9′ = lunar parallax scale, confirms topocentric route correct. Root causes fixed (all KIND-B): ① Sun VSOP87 τ units + millennium L1 constant; ② planet tables regenerated from VSOP87D (1351 terms, self-check 28.5″ L/B, 1.9 mAU R vs pymeeus); ③ star J2000→of-date precession added (`precessJ2000EquatorialToDate` + `staticObjectEquatorial`, 4 call sites); ④ ΔT invented cubic → Espenak-Meeus piecewise (96.3 s → ~74 s @2025, ≤6 s residual → ≤0.05′ Moon).

## 3. B1 — magnetic declination (OD4)

**Finding (MEASURED):** rotation-vector azimuth is MAGNETIC-north referenced; all sky azimuths are TRUE-north referenced → systematic pointing error = local declination.

| Location | D (°, E+) | pointing error removed |
|---|---|---|
| Tehran | +4.966 | 298′ |
| Sydney | +12.825 | 770′ |
| Quito | −5.084 | 305′ |
| Tromsø | +11.242 | 675′ |
| Cape Town | −26.784 | 1607′ |
| Honolulu | +9.273 | 556′ |
| Frankfurt | +3.769 | 226′ |
| Nurabad (app default) | +3.570 | 214′ |

pygeomag WMM2025, cross-checked vs independent `geomag` implementation: worst |diff| 0.0017°. On-device value comes from `android.hardware.GeomagneticField` (UNEXECUTED).
**Offset-rebase decision:** subtract-once (not clear): `ARCalibrationManager.rebaseYawForDeclinationOnce` reduces the stored legacy yaw by D exactly once (versioned marker `calib_yaw_declination_rebased_v1`) — preserves the user calibration; idempotent.
**Guardrails:** flag `APPLY_MAGNETIC_DECLINATION=false` → D=0 → bit-identical azimuths; no GPS fix/permission → D=0 → bit-identical; applied at exactly ONE point (`currentAzimuth` sensor branch; manual-drag branch deliberately uncorrected — user aligns true-frame sky by eye). Pure-math tests 7/7 incl. wrap-around; mutation proof (sign flip) = 4 failures.

## 4. B3 — display-catalog corrections (OD3, values only)

Rule: correct iff off > 60″ AND two independent sources agree < 5″ (HYG v3.6 + Yale BSC5 for stars; OpenNGC + HYG-DSO/NED/Wikipedia for DSOs).

| id | before (ra,dec) | after | was-off | sources agree |
|---|---|---|---|---|
| star_ori_alnilam | 84.530, −1.200 | 84.053, −1.202 | 1715.6″ | 0.2″ |
| star_ori_mintaka | 84.050, −0.300 | 83.002, −0.299 | 3773.9″ | 0.3″ |
| star_uma_mizar | 206.884, 54.920 | 200.981, 54.925 | 12208.3″ (3.4°!) | 0.3″ |
| star_uma_alkaid | 209.800, 49.310 | 206.885, 49.313 | 6841.1″ | 0.8″ |
| star_cas_schedar | 9.880, 59.150 | 10.127, 56.537 | 9417.9″ (2.6°) | 0.8″ |
| star_cas_caph | 1.150, 59.150 | 2.293, 59.150 | 2112.9″ | 2.4″ |
| star_sco_shaula | 262.690, −37.030 | 263.402, −37.104 | 2062.7″ | 0.4″ |
| dso_smc | 14.766, −72.801 | 13.187, −72.829 | 1711″ vs BOTH sources | judgment call* |

After: worst remaining star delta 20.4″ (α Cen) — under the 60″ bar. All other DSOs ≤ 47.7″. *SMC: 5°-wide galaxy, HYG-DSO vs NED disagree 107″ on center convention → strict bar unsatisfiable for any value; corrected to NED J2000 (documented deviation). No magnitude corrections (sources disagree where app differs: Castor; DSO mags fuzzy). Verified-correct near-threshold no-actions: Hyades (61″ source disagreement), Pleiades (center conventions differ 668″), Double Cluster (sources disagree 77″), Coma/Mel 111 (single source → report-only).

## 5. E1 — real catalog provenance (OD5)

- **Derived (committed):** `data/startracker/hyg_v36_vle6.5_j2000.csv` — 8,870 stars, V ≤ 6.5, J2000, exact `CatalogIngestor` format. SHA-256 `fbfc6f8910235f16f4340401907be2eb68bce6373ed7b9deb11a5f18f4deea38`.
- **Raw:** HYG v3.6 `hyg/v3/hyg_v36.csv.gz` via gh-api raw (codeload 404s from sandbox), retrieved 2026-09-04T05:58Z. SHA-256 gz `784fd90e767c5f4fc32a084adfb41618f3f80a55e55cf2c65baf8107fa881468`, csv `8b2d5bed0abb650630af5c64ea694506dd3246e00f8775694525de9e1c826d84`. Transformation: HIP-only, mag ≤ 6.5, `ra_hrs × 15` (v3.6 format change — hours!), 6/6/3 decimals. Raw gz was kept git-ignored at `data/startracker/raw/` but lost in the sandbox rebuild — RE-FETCH + SHA-VERIFY is verifier item G2.
- **Licence:** CC BY-SA 4.0. **Attribution text that must ship in-app if the catalog is distributed:** "Star data from the HYG database (HYG v3.6), © astronexus.com, licensed under Creative Commons Attribution-ShareAlike 4.0 (CC BY-SA 4.0). Based on data from the Hipparcos, Yale Bright Star, and Gliese catalogues."
- **E2 real ingest (MEASURED):** 8,870 stars parse in 81 ms (ids unique, mag [−1.44, 6.5], N/S 4255/4615); pair index at CatalogBuildConfig defaults = 4,872,798 pairs = 77,964,768 B ≈ 78 MB → unshippable → item C.

## 6. C — capped quad index (C4 sweep + C5 defaults)

O(N⁴) brute force was infeasible > ~800 stars (OOM). New `QuadPatternIndex.capped()`: mag-eligibility cut → per-anchor K-nearest neighbors → global dedupe (60-bit keys, < 32768 stars) → deterministic ceiling. Legacy constructor untouched (opt-in param only; hand-rolled O(N⁴) spot-check test). Sweep on the REAL catalog (MEASURED):

| mag ≤ | K | eligible | quads | size (B) |
|---|---|---|---|---|
| 4.5 | 5 | 919 | 6,522 | 956,492 |
| 4.5 | 6 | 919 | 13,022 | 1,476,000 |
| 5.0 | 6 | 1,625 | 22,604 | 2,241,922 |
| **5.5** | **6** | **2,848** | **39,960** | **3,629,280 (DEFAULT)** |
| 5.5 | 8 | 2,848 | 109,900 | 9,219,706 |
| 6.0 | 6 | 5,041 | 70,754 | 6,090,550 |
| 5.5 | 10 | 2,848 | 232,750 | 19,038,911 |
| 6.5 | 6 | 8,870 | 125,416 | 10,460,793 |

79 B/quad throughout; build 0.25–9.9 s. **Defaults justified by D:** pool-25 candidates solved only 35–45% (67% of solves false-locked); pool-40 (adopted `CANDIDATE_POOL_SIZE=40`) solved 20/20 clean-sky. Default index = half the A1 7.3 MB budget with ≥ 90% eligible-star coverage (test-asserted); full V ≤ 6.5 star list still ships (435 KB); 78 MB pair index ships as 0 pairs (solver consumes quads only).

## 7. D — synthetic E2E headline (all numbers SYNTHETIC-SKY)

**D1 finding:** the legacy runtime candidate builder (global C(N,4) of top-10 brightest) produced quads nearly DISJOINT from the capped index's nearest-neighbor quads → lost-in-space solve **0/20 on every cell** ("No quad matches found"; one field measured: 22/210 candidates in-range, 0 indexed). Fix: `buildLocalCandidates` (anchor + nearest neighbors from the bright pool) behind `USE_LOCAL_QUAD_CANDIDATES=true` (false = legacy, bit-for-bit).

Ladder (20 deterministic attitudes/cell, 63.5° FOV, 57″/px, false lock = err > 0.5°):

| cell | solved | false locks | median err | p95 err |
|---|---|---|---|---|
| 0 px, 0 false | 20/20 | 3 | 0.000′ | 1.94′ |
| 0.1 px | 20/20 | 3 | 0.099′ | 2.03′ |
| 0.3 px | 20/20 | 3 | 0.307′ | 5.90′ |
| 1.0 px | 20/20 | 3 | 1.024′ | 19.68′ |
| 2.0 px | 19/20 | 4 | 2.048′ | 23.00′ |
| 0.3 px + 5 false | 19/20 | 7 | 0.319′ | 4.42′ |
| 0.3 px + 20 false | 15/20 | 10 | 2.466′ | 3.02′ |
| 1.0 px + 20 false | 15/20 | 10 | 3.625′ | 10.06′ |

Pool tuning curve: 25 → 35–45% solves; 40 → 75–100% (adopted). Refraction/pointing ladder = A6 (`refraction_ladder_oracle.csv`); astronomy-accuracy ladder = §2; centroid behaviors validated in the prior pass (CentroiderTest incl. saturated-denominator fix) — D consumes centroid output as noiseSigma (0.1–2 px); no new centroid numbers.

**D6 honest statement:** D proves the chain (ingest → capped index → candidates → pyramid → RANSAC → Davenport) locks correctly on synthetic skies drawn from the REAL catalog with sub-2′ median accuracy at plausible noise, in ~1.3 s build + sub-second solve on JVM. It does NOT model real cameras (PSF, hot pixels, blur, distortion), and does NOT certify enablement: **false locks remain at 15% of clean-sky solves** (hypothesis: sorted-5-ratio descriptor is reflection-ambiguous — unproven). `StarTrackerConfig.ENABLED` stays **false**.

## 8. Thresholds changed / loosened

| Constant | before | after | data |
|---|---|---|---|
| `CANDIDATE_POOL_SIZE` | (new) | 25 → 40 during D | §7 pool curve |
| `USE_LOCAL_QUAD_CANDIDATES` | (new flag) | true | D1: legacy builder 0/20 |
| `QUAD_BUILD_MAX_MAGNITUDE` / `QUAD_NEIGHBORS_PER_STAR` / `QUAD_MAX_QUADS` | (new) | 5.5 / 6 / 120,000 | §6 sweep |
| `APPLY_MAGNETIC_DECLINATION` | (new flag) | true (OD4-ordered default-on) | §3 |

All pre-existing `CatalogBuildConfig` thresholds unchanged (MAX_PAIR_SEPARATION 40°, MIN_PAIR_SEPARATION 0.1°, HASH_BIN_WIDTH 0.01, TOP_N_BRIGHTEST 10). **Loosened thresholds: NONE.** All new constants marked UNVALIDATED-pending-device in-code.

## 9. F1 — environment probe (MEASURED 2026-09-04)

| host | result |
|---|---|
| services.gradle.org / repo.maven.apache.org / dl.google.com / plugins.gradle.org / maven.google.com / repo1.maven.org | ALL 000 (unreachable) |

Not all Gradle hosts reachable → `./gradlew :app:testDebugUnitTest` NOT admitted as gate; offline harness remains the gate. F2 glue landed (R2-A1 camera gate behind `ENABLED`; prior-pass remediations — all harness-covered). F3 runbook addendum written.

## 10. Final harness + standalone counts

- **Offline harness (the gate): `bash tools/kotlin-harness/run_tests.sh` → 155 / 0 / 0** (`evidence/HARNESS_FINAL_RUN_2026-09-04.txt`). Composition: 141 pre-final-pass tests + CoordinateOracleTest (3) + MagneticDeclinationTest (7) + CappedQuadIndexTest (6) − recount artifacts; historical: 138/0/0 pre-final-pass, 116/2/8 pre-fix @ deb748f.
- **Standalone app tests: 108/108 green in pass 2** (13 files could not compile offline — documented `PROJECT_STATUS_END_OF_IMPLEMENTATION.md:38`).
- **Mutation proofs (all reproduced):** oracle tests vs pre-fix code (star 26.5′ max, saturn 527′, ΔT 101.27 s); B1 sign-flip (4 failures); C combo-min-sep (equivalence break) and dedupe (296 vs 74); D flag-flip (exact 0/20 signature).

## 11. G9 DONE checklist + what needs a human/device (ordered)

DONE in-repo: A1–A6, B1–B5, C, D, E, F1–F3, G proofs/sweep, H (this file). Commits d8a2e1e, 43e360e, 60155a3, b0ee15a, 2bf1d05, b7fb71e, a3a1ca0, 1ec3406, 79cd0be (+ this report's commit).

Needs a human/device, in order:
1. **Compile on a real toolchain** (`./gradlew :app:assembleDebug`): B4 wiring (OrientationProvider `sensorTimestampNanos` StateFlow + CompassARScreen collector) and the R2-A1 camera gate are OUTSIDE the offline-harness compile set — UNEXECUTED. Known cosmetic caveat: commit d8a2e1e alone carries three imports only used by later commits (unused-import warnings; compiles).
2. **Declination on device (default ON):** at a known GPS fix verify the overlay shifts by local D (e.g. +4.966° Tehran / −26.784° Cape Town); verify one-time legacy-yaw rebase fired (marker `calib_yaw_declination_rebased_v1`) and a previously calibrated device still overlays correctly.
3. **Star spot-checks:** Mizar, Alkaid, Schedar, Alnilam, Mintaka, Caph, Shaula, SMC (previously 0.6°–3.4° off).
4. **Sun/Moon/planet overlay:** should match reality to ~1′ (was up to degrees).
5. **Star tracker stays DISABLED** until the D6 false-lock blocker is fixed (chirality-aware verification or post-solve vote); then re-run the D ladder on device; wire `data/startracker/hyg_v36_vle6.5_j2000.csv` into assets and the 3.6 MB index build.
6. **HYG attribution UI text** (§5) in the About/licence screen if the catalog ships.
7. Re-fetch + SHA-verify the raw HYG gz (lost in sandbox rebuild) and re-push if desired.

## 12. New findings flagged (NEW this pass)

1. **D1 candidate/index disjointness** — the solver could never have worked on a real catalog (0/20); fixed + mutation-proven.
2. **False-lock blocker (D6)** — 15% of clean-sky solves lock wrong; reflection-ambiguity hypothesis; enablement blocked.
3. **Sandbox rebuild incident** — all commits RECONSTRUCTED (boundaries re-verified; raw gz + 3 volatile files lost, documented).
4. **HYG v3.6 `ra` in HOURS** — format change vs v3.5 docs; any consumer must ×15.
5. **Oracle-script copy bug (fixed)** — the "fte" table re-read star_live columns; real fte residual 0.291/0.466.
6. **SMC center-convention judgment call** (§4) and **Castor BSC5-JSON Vmag data error** (1.98 vs 1.58 everywhere else).
7. Report-only residuals (unchanged, OD7): app omits aberration+nutation (no-refr excess 2.1′ rms); GMST IAU-1982 vs 2006 ≤ 5.4″; Bennett-at-true-altitude nuance ≤ 0.1′ above 5°; moon_geocentric 48.6′ parallax diagnostic.

— END OF REPORT —
