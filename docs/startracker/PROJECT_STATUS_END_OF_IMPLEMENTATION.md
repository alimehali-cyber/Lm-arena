# Project Status — End of Implementation (Phases 0-10), rewritten 2026-09-03

**This file was rewritten top-to-bottom in remediation pass 2 (item A4).** The original
2026-09-02 document is preserved verbatim at
`docs/startracker/history/PROJECT_STATUS_2026-09-02_ORIGINAL.md`; it honestly described
a state in which nothing had ever been executed, and contained claims falsified since.
This rewrite is the single current-status document; every number below traces to an
executed run or is labelled estimated/modeled/extrapolated.

**Live status authority:** environment status table = §5 (the only authoritative one;
all other docs' copies are stamped SUPERSEDED). Harness disclosure =
`docs/startracker/evidence/HARNESS_DISCLOSURE.md`. Evidence directory =
`docs/startracker/evidence/`.

---

## 1. Executive summary

- **The headline historical fact: the code as shipped did not compile.** First-ever
  compilation (2026-09-03, pass 1) found two compile-blocking errors in the startracker
  module: `ConfidenceStateMachine.kt` used `exp()` with no import, and
  `EndToEndSyntheticTestHelper.kt` called `DistortionModel.isIdentity()`, which did not
  exist (fixed in commit deb748f). Ten phases of work were delivered without either of
  these ever being caught because NO automated execution of any kind had ever run.
- **First-ever executed baseline (116 tests, 2 failures + 8 errors).** At the
  compile-fix commit, running the shipped test files unchanged: **116 tests, 2
  failures, 8 errors** (re-verified in pass 2 by re-running that exact commit). The
  number 117 that also appears in pass-1 records is the same suite AFTER the pass's
  first test rewrite (ConfidenceStateMachineTest) added one test — 116 is the baseline.
  Failures: AttitudeBlenderTest (blend math unreachable behind a `const` flag, tests
  asserted behavior they could never observe); Errors: IntrinsicsRefinerTest /
  DistortionRefinerTest (`kotlin.random.nextDouble(0,0)` throws on the zero-noise
  tests — those tests had never actually run).
- **Current state: the offline-harness suite is fully green — 137 tests, 0 failures,
  0 errors** (30 startracker test files + RefractionTest + HeroSkyProjectionTest via a
  Compose Offset stub; kotlinc 2.4.10 + jdk4py JRE 25 + JUnit shim — NOT the project's
  Gradle/Android toolchain, which remains unavailable). An additional 10 pure-Kotlin
  app test files ran standalone green (108/108) in pass 2; 13 files cannot compile
  without Android/Robolectric/Compose (per-file: HARNESS_DISCLOSURE.md §6).
- **12 audit code findings fixed** (pass 1, one commit each, regression-tested) plus 2
  new findings found by execution (zero-noise Random crash; const-flag-unreachable
  blend math). One code fix applied beyond the audit list with an instruction
  violation, disclosed and kept by owner decision: the PHASE9 HeroSky southern-
  hemisphere fix (see PHASE9_INTEGRATION_PATCH.md status header).
- **Catalog size truth (pass 2, measured):** pair index at 9,110 stars = 4,851,922
  pairs, 7.5 s build, **74.5 MiB serialized (MEASURED)**; the quad index is
  **infeasible as implemented** — measured-extrapolated ~1.28×10¹¹ quads ≈ 10.2 TB at
  9,110 stars, O(N⁴) build (~9.5 days extrapolated), heap-OOM at just 800 stars.
  Earlier claims — "10-30 MB" (unexecuted guess, ~300,000× off) and "3.79/6.69 GB"
  (estimator output under a quad model now measured to be ~2,900× optimistic) — are
  corrected everywhere. Evidence: `evidence/CATALOG_SIZE_MEASURED_2026-09-03.txt`.
- **Fabricated validation evidence replaced (pass 1):** the invented "Headline
  Accuracy Numbers" table and Tetra3 comparison in PHASE3_4_5_6_FINAL_REPORT.md were
  replaced with the real executed ValidationMatrixRunner matrix; the placeholder
  python_crosscheck_phase10.py was rewritten stdlib-only with 18 real assertions.
  Pass 2 installed numpy 2.4.6 and re-ran all five remaining phase scripts: **every
  doc-attributed number reproduced** (`evidence/A6_NUMPY_CROSSCHECK_REPRO_2026-09-03.txt`).
- **Still NOT done:** PHASE6/PHASE7 live wiring (gated patches, never applied — target
  files need the Android build); the real Gradle build and full Android test suite;
  real-device field testing; the real 9k-15k star catalog; UI guidance implementation;
  live-scope static diffs from pass 2 (item A1) await human execution.

## 2. Phase-by-phase status (with Executed? column)

"Executed" = ran in the offline harness (pure-Kotlin subset), pass 1/2. "—" = not
executable there (Android/Compose dependency); those tests have still NEVER run.

| Phase | Title | Code | Executed? (harness) | Result |
|---|---|---|---|---|
| 0 | Baseline audit | read-only | n/a | n/a |
| 1 | Refraction & FOV consolidation | 63.5° fallback constant; timestamp plumbing; camera observer wiring | RefractionTest YES (6/6 green; "4 tests" was a miscount); HeroSkyProjectionTest YES from pass 1 (7/7 after PHASE9 fix); OrientationProvider/CompassARScreen diffs NOT compiled (Android) | green |
| 2 | Star detection | 6 files | YES — all 6 test files in the 137 | green (incl. pass-1 Centroider B4 fixes) |
| 3 | Catalog | 6 files | YES — all 4 test files in the 137 | green; size truth now measured (§1) |
| 4 | Solver | 6 files + synthetic | YES — AttitudeSolverTest, SyntheticSkyObserverTest, LostInSpaceSolverTest in the 137 | green; the old "27 arcsec" python claim REPRODUCED (27.36″) with numpy 2.4.6 |
| 5 | Tracking loop | 7 files | YES — ConfidenceStateMachineTest, QuaternionIntegratorTest, RelockPolicyTest, TrackingLoopTest in the 137 | green (after B2/B3, B6/B7 fixes) |
| 6 | Fusion (gated) | StarTrackerConfig flag OFF; AttitudeBlender | YES — AttitudeBlenderTest in the 137 | green (after const-flag testability fix; acquisition behavior documented, awaiting owner decision) |
| 7 | Self-calibration | 6 files | YES — DistortionModelTest, IntrinsicsRefinerTest, DistortionRefinerTest, CameraProfileCacheTest, SelfCalibrationEngineTest in the 137 | green (after B8/B9, zero-noise guard, DistortionModelTest real cross-check) |
| 8 | Diagnostics & confidence ladder | 5 files | YES — AmbiguityDetectorTest, FrameQualityClassifierTest, ConfidenceLadderCoordinatorTest (+ RelativeBearing/BearingCrossCheck from phase 9 work) in the 137 | green (after B10) |
| 9 | HeroSky hemisphere fix | RelativeBearing, BearingCrossCheck + HeroSkyProjection fix | YES — HeroSkyProjectionTest executed pass 1 for the first time | fix applied (instruction violation disclosed), KEPT by owner decision pass 2; 7/7 green |
| 10 | Full-stack synthetic validation | ValidationMatrixRunner + EndToEnd helper | YES — ValidationMatrixRunnerTest, EndToEndSyntheticTest in the 137 | green; the real matrix is in `evidence/VALIDATION_MATRIX_2026-09-03.txt`; the phase-10 python script was fabricated and was rewritten (pass 1) |

The one **non-startracker** pure-Kotlin main dependency pair compiled into the harness:
`astro_engine/FrameTransformationEngine.kt`, `astro_engine/AstroTime.kt`.

## 3. What each "Python cross-check" actually is (6 scripts, not 4)

`python_crosscheck_phase{2,3,4,7,9,10}.py` at repo root. Phase 2/3/4/7/9 were re-run
2026-09-03 with numpy 2.4.6 and their documented numbers all reproduced (see §1);
phase 10 was the fabricated one, rewritten pass 1. Outputs verbatim:
`evidence/PYTHON_CROSSCHECK_PHASE*_OUTPUT_2026-09-03.txt`.

## 4. Forbidden scope — corrected record (pass 2, item A3(c))

Five files listed as "never touched" had small disclosed Phase-1 changes (diffed
against base 60928ba): OrientationProvider.kt (timestampNanos field + threading;
equality participation removed in pass-2 A1(b) to restore StateFlow conflation),
ARProjectionEngine.kt (fallback-FOV consolidation + tier logging), CoordinateEngineLegacy.kt
(comment rename R_bennett→R_saemundsson, no numeric change), FrameTransformationEngine.kt
(docstring only), CompassARScreen.kt (CameraFrameObserver wiring + ImageAnalysis
binding — now gated behind StarTrackerConfig.ENABLED by pass-2 A1(a), static diff,
unexecuted). Gated patches PHASE6 (OrientationProvider star-blend) and PHASE7
(ARProjectionEngine calibration tier) were NEVER applied. PHASE9 was applied pass 1
(disclosure in its doc) and kept pass 2. Everything else in the forbidden list
remains untouched.

## 5. Environment status — THE authoritative table

| Capability | Status |
|---|---|
| Gradle download / Android SDK / Compose build | BLOCKED since Phase 1 (TLS failure against services.gradle.org; no Android SDK in sandbox). Never resolved. `./gradlew :app:testDebugUnitTest` has NEVER run. |
| Offline Kotlin harness (kotlinc 2.4.10 npm + jdk4py JRE 25 + JUnit shim) | WORKING (pass 1, rebuilt + committed in-repo pass 2: `tools/kotlin-harness/`). Runs the pure-Kotlin subset only. |
| Python + numpy | python3 3.11 always available; numpy 2.4.6 INSTALLED pass 2 (`pip install --break-system-packages numpy`) — the historical "numpy 2.4.6 available" claim is now true again, and all five phase scripts reproduce their numbers. |
| Robolectric / Compose UI / Android-instrumented tests | NEVER RUN (cannot compile in sandbox; per-file reasons in HARNESS_DISCLOSURE.md §6). |
| Real device / real catalog | NEVER available in this environment. |

**Escalation that still stands:** nothing in this repo is validated by the project's
own build system. Before any release: fix the real toolchain, run the real full suite,
apply+verify the PHASE6/PHASE7 gated patches under their flags, execute the pass-2 A1
static diffs' verification steps, and do real-device field testing per
REAL_DEVICE_FIELD_TEST_PROTOCOL.md.

## 6. Deliverables (current)

- Startracker sources + tests (see MASTER_FILE_MANIFEST.md, reconciled pass 2).
- `tools/kotlin-harness/` — offline harness (shims, runner, run_tests.sh, try_test.sh,
  CatalogSizeProbe.kt).
- `docs/startracker/evidence/` — 12 evidence files (harness runs, catalog measurements,
  numpy reproductions, HeroSky before/after, unexecuted-diff disclosures).
- Phase patch docs PHASE6/7/9 (9 = fix applied + kept; 6/7 = still patches),
  CATALOG_SOURCING.md (measured size + options), UI_GUIDANCE_PROPOSAL.md,
  REAL_DEVICE_FIELD_TEST_PROTOCOL.md, ATTITUDE_BLENDER_ACQUISITION_NOTE.md (pass 2).
- Phase reports at repo root (corrected in place, corrections dated).

## 7. Conclusion

Implementation is complete as ISOLATED code and, for the pure-Kotlin subset, now
actually executed and green (137/137 offline-harness; plus 108 standalone app tests).
The audit's fabrication and correctness findings are remediated with regression tests,
and the remaining gap is precisely known: the real Android toolchain, the gated live
wiring, the catalog acquisition, and field validation. No claims in this file rest on
unexecuted code; where something is estimated or extrapolated it says so.
