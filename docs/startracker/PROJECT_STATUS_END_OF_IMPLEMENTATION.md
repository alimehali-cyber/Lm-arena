# Project Status — End of Implementation (final rewrite, 2026-09-04, T5)

**Authoritative current-status document.** Rewritten three times, honestly each time:
2026-09-02 original (nothing had ever run — preserved at
`history/PROJECT_STATUS_2026-09-02_ORIGINAL.md`), 2026-09-03 pass 2, and this final
rewrite after the Z-series + gate pass (T1–T5). Every number below traces to an
executed run (CI run id or evidence file) or is labelled UNEXECUTED / SYNTHETIC.

**Live status authority:** CI JUnit (from T1 on) is THE gate
(`docs/startracker/evidence/T1_CI_JUNIT_2026-09-04.md`); the offline harness
(`tools/kotlin-harness/`, 167/0/0 at this tip) is a fast local pre-check.
Harness disclosure: `evidence/HARNESS_DISCLOSURE.md`. File inventory:
`MASTER_FILE_MANIFEST.md`. Closing narrative: `CLOSING_PASS_REPORT.md`.

---

## 1. The four headline facts

1. **The app never compiled on this branch until 2026-09-04.** Ten phases + the final
   pass shipped without a single successful `assembleDebug`: one compile error
   (`CameraFrameObserver.kt:58` — `Executor?` passed where `@NonNull Executor` was
   required, verbatim in `evidence/T3_HYGIENE_2026-09-04.md`) had blocked the Gradle
   build since PR #3. First green build: Z-BUILD3, commit `a8157b5`, CI run
   `33872506560`. APK: 28,019,185 B (SHA-256 `5e8a8459…`), rolling pre-release
   `ci-debug-apk`.
2. **The star tracker never compiled until pass 1 (2026-09-03):** `ConfidenceStateMachine.kt`
   used `exp()` with no import; `EndToEndSyntheticTestHelper.kt` called a nonexistent
   `DistortionModel.isIdentity()` (fixed `deb748f`). Nothing startracker-side had ever
   executed before that date.
3. **The live sky overlay was wrong, quietly, and is now correct and oracle-verified.**
   Pre-work errors (all passed green by range-only assertions — see T2 section below):
   star overlay ~17′ rms, Sun up to 125′, Mercury up to 20° / Mars 11.5°, and 7 display
   stars off by up to 3.4° (plus 1 DSO). All corrected; pinned by the astropy oracle
   (MEASURED, offline): stars **0.418′ rms**, Sun **0.350′**, Moon **0.145′**, planets
   **0.4–0.6′** end-to-end (`evidence/ORACLE_*`, regression-pinned by
   `CoordinateOracleTest`).
4. **The star tracker is synthetic-ready, device-unproven, and DISABLED.** W1 E2E on the
   real catalog: FULL_LOCK 393/395, lens-cap NO_LOCK. S3 joint Monte-Carlo (10,000
   random attitudes, noise U(0,2) px, false stars {0,5,10,20}): 4,817 solved, **0 false
   locks**, median 0.066′. `StarTrackerConfig.ENABLED = false`; everything beyond this
   point **requires a phone under a clear sky**
   (`REAL_DEVICE_FIELD_TEST_PROTOCOL.md`, 8 steps, ~30 min).

## 2. CI is the gate (T1, 2026-09-04)

- Run `33879125683` (head `d44f782`): `:app:testDebugUnitTest --continue` with
  `-Pgravity.ci.tests=false` — **65 test files, 456 tests, 0 failures, 0 errors,
  0 skipped**, Robolectric and screenshot tests included. Full per-file table:
  `evidence/T1_CI_JUNIT_2026-09-04.md`.
- All **13 test files that had never compiled** on this branch (SkyOrientationProjection,
  ARCalibrationPrompt, SGP4Propagator, ISSPassPrediction, EclipseEngine,
  ClassificationAudit, Phase4, Phase5, Relativistic, SatelliteAR, IssTleWorker,
  ExampleRobolectric, GreetingScreenshot) compile, run and pass on CI.
- **CI vs harness: discrepancies NONE** (38 shared files, identical outcomes/counts).
- T1 finding (harness-era blind spot in CI itself): `app/build.gradle.kts` :158–176
  silently restricted every CI test task to `com.zig.gravity.*` (179 tests) whenever
  `GITHUB_ACTIONS=true`; the first "full" run `33878462925` was green but gravity-only.
  Fixed in `c41e15e`.
- JUnit XML ships as CI artifacts (`junit-xml`, `unit-test-log`), never in git (T3).

## 3. Pre-existing test coverage: what it did not catch (T2, 2026-09-04)

Before this work the sky-position tests existed but were range-only, which is exactly how a 20° Mercury error, an 11.5° Mars error and a 125′ Sun error all passed green: the pre-work `VSOP87EngineTest` asserted Mercury only by DISTANCE (`"Mercury distance … should be between 0.30 and 0.47 AU"` — no angular assertion of any kind), asserted Earth/Jupiter only inside ±2.5°/±2.5° hand-picked longitude bands, and covered Mars solely through a generic loop whose positional checks were `longitudeDeg in 0.0..360.0` and `distanceAu > 0.0`; the pre-work `LunarSolarEngineTest` asserted the Sun only as `"Sun distance should be near 1 AU"`, `"Sun declination in August should be positive"` (a SIGN check) and `"Sun RA should be in [0, 360)"`. The VSOP87 tables themselves had NO test before this work — no assertion anywhere compared a table-derived series value against an external source, so the τ-unit and millennium-index defects (pass A, commit `d8a2e1e`) were invisible to the suite; they are now pinned by CoordinateOracleTest (astropy, 0.35–0.6′ end-to-end) plus the Meeus Ch.25 and Earth-heliocentric oracle tests added in Z-V1.

## 4. Distortion bootstrap (T4, 2026-09-04) — `evidence/T4_DISTORTION_2026-09-04.md`

- **(a) HARDWARE_DISTORTION tier (UNEXECUTED on device):**
  `HardwareDistortionReader.kt` reads `LENS_RADIAL_DISTORTION` (API 33+) /
  `LENS_DISTORTION` (API 30+) → Brown-Conrady k1/k2; tier order HARDWARE_DISTORTION >
  SELF_CALIBRATED > NONE (identity ⇒ T4(b) allowance). Harness-tested via new camera2
  shims (4/4); first real execution is the device trial.
- **(b) Radius-dependent tolerance when no model:** `tol(θ) = 300″ + 0.04951·tan²θ`
  envelopes |k1| ≤ 0.08 across the 63.5° tier (D5-derived); default c=0 is
  byte-identical to the old flat gate; pipeline auto-applies it only when the
  distortion model is identity. S3 joint MC re-run (same seeds, unmodelled k1):
  k1=0 → 50.2 % solved / FLfull 1; k1=−0.03 → 49.4 % / median 1.03′; k1=−0.08 →
  42.7 % / FL 4.87 % but **FLfull 0**. Honest cost at k1=0: FL 0.54 % vs 0.00 % with
  the flat gate. Loosened: none.

## 5. Hygiene (T3, 2026-09-04) — `evidence/T3_HYGIENE_2026-09-04.md`

`ci/apk` orphan branch deleted; buildlogs/ removed from the tree (27 MB repo →
GitHub recompute pending); APK + logs are CI artifacts + rolling pre-release
`ci-debug-apk` only; the `!!` non-null assertion replaced by a typed local
`Executor` hoist (the sole PR#3-era compile error, quoted verbatim in the evidence);
sizes recorded before/after.

## 6. Catalog & validation numbers (V5/V6, machine-verified)

- **Test counts** (V5, `evidence/V5_TEST_COUNT_RECONCILIATION_2026-09-04.md`):
  pass-3 tip `edef03d` = 138/0/0; final-pass tip = 155/0/0 (decomposition 138+17);
  closing pass (W1–W3 additions, incl. `StarTrackerPipelineTest`) = 161/0/0
  (`evidence/R_FINAL_HARNESS_2026-09-04.txt`); gate-pass tip = **167/0/0**
  (+2 `FullFieldVerifierTest` T4b tests, +4 `HardwareDistortionReaderTest`). CI side:
  456 tests at `d44f782`, 462 expected with T4's two verifier tests. The separate
  projection runner (`run_projection_test.sh`) holds SkyOrientationProjection 5/0/0 +
  ARProjectionPinhole 1/0/0. The "141" interim tally was wrong and is corrected in place.
- **Which extract the index uses:** the **J2000 file**
  (`data/startracker/hyg_v36_vle6.5_j2000.csv`) — referenced by CappedQuadIndexTest,
  SyntheticE2ETest, harness probes, D/S-ladder runs. The PM-propagated **2026.5**
  extract sits alongside (not swapped in): swap-in is a device-trial decision since
  median shift is <1″ at ~3.8′/px harness scale (`…2026.5.csv.sidecar.md`).
- **Ten largest 2000→2026.5 proper-motion shifts** (sidecar, full table there):
  Groombridge 1830 187.0″, 61 Cyg A/B 139.9″/137.1″, Lacaille 8760 124.7″, Keid
  108.3″, HIP 5336 100.1″, α Cen A/B 98.3″/98.3″, 82 G. Eri 82.8″, 268 G. Cet 61.3″
  — 11 stars total shift >60″ (1′); everything else moves <10″.
- Provenance: HYG v3.6 raw gz re-fetched twice 2026-09-04, SHA-256 `784fd90e…468`
  unchanged (Z-V7); attribution text finalized (CC BY-SA 4.0) — in-app placement is
  device-trial Step 5.

## 7. G9 items (i)–(vii) — corrected statuses

(i) Real-toolchain compile: **MET, and now complete** — assembleDebug green since
`33872506560`; `:app:testDebugUnitTest` green since T1 run `33879125683` (65 files /
456 / 0). (ii) Declination on device, default ON: superseded/closed by Z-V3
(landmark check = protocol Step 1). (iii) Star spot-checks on device: **NOT MET on
device** (offline 0.418′ rms; 7-star check = Step 4). (iv) Sun/Moon/planet overlay on
device: **NOT MET** (offline 0.35–0.6′; Step 2). (v) Tracker DISABLED until false-lock
blocker fixed: **MET** — S1–S3 fixed it (FL 0/10,000 flat gate; T4 allowance variant
documented separately), ENABLED verified false. (vi) HYG attribution in-app: **NOT MET
in-app** (text finalized; Step 5). (vii) HYG re-fetch + SHA: **MET** (Z-V7).

## 8. Ordered device list (what a human does next, in order)

1. ~~Push everything~~ **DONE** — all commits through T5 are on
   `arena/01a0676f-lm-arena`.
2. ~~Compile + run tests on the real toolchain~~ **DONE** (Z-BUILD1–4 + T1).
3. Install the `ci-debug-apk` pre-release APK on a phone and execute
   `REAL_DEVICE_FIELD_TEST_PROTOCOL.md` (8 steps, ~30 min, needs a clear sky),
   recording every number into `DEVICE_TRIAL_<date>.md`.
4. Decide PHASE6/PHASE7 enablement from Step 6/7 results (OD1 discrepancy log).
5. Place the HYG attribution text in the About/licence screen (Step 5 check).
6. Before PHASE7: read the device k1 via HardwareDistortionReader (T4a,
   HARDWARE_DISTORTION tier) — if the device reports none, self-calibrate via
   SelfCalibrationEngine (D5: unmodelled k1=−0.05 already breaches the flat gate at
   the field edge; T4b quantifies the unmodelled case).
7. After the trial: decide the 2026.5 catalog swap-in (sidecar; sub-pixel for 99.9 %
   of stars) and revisit `AttitudeBlender` acquisition behavior (owner decision
   pending since pass 2).

## 9. What remains not done (unchanged in kind, now precisely bounded)

Device trial (Steps 1–8) and everything downstream of it: PHASE6/PHASE7 live
enablement, on-sky tracker validation, attribution placement, k1 read/calibration,
2026.5 swap-in decision. No claims in this file rest on unexecuted code; everything
executed is cited by run id or evidence file, everything else says UNEXECUTED.
