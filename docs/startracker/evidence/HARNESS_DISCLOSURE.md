# Offline Kotlin test harness — full disclosure

Created: 2026-09-03 (remediation pass 1), rebuilt and committed in-repo 2026-09-03
(remediation pass 2). This file is the authoritative description of what the harness
is, what it runs, how its runner behaves, and how to reproduce it.

## 1. Toolchain (exact versions and provenance)

| Component | Version | Source |
|---|---|---|
| Kotlin compiler | kotlinc-jvm **2.4.10** | npm package `kotlin-compiler@2.4.10`, installed globally (`npm install -g kotlin-compiler`), binaries at `/usr/local/lib/node_modules/kotlin-compiler` |
| Java runtime | OpenJDK **25.0.2** (JRE, `25.0.2+10-LTS`) | PyPI package `jdk4py==25.0.2.1` (`pip install --break-system-packages jdk4py`), runtime at `.../site-packages/jdk4py/java-runtime` |
| Runtime stdlib | `kotlin-stdlib.jar` shipped inside the npm kotlin-compiler package | same as compiler |

**These differ from the project's pinned toolchain.** The Android project pins its
Kotlin/AGP/Gradle versions in `gradle/libs.versions.toml` / the Gradle wrapper
(Gradle download is blocked in this sandbox: TLS failure against
services.gradle.org, documented since Phase 1). The harness compiles with Kotlin
2.4.10 on a JRE 25; the project targets Kotlin as pinned in its Gradle config with
JDK 17. No claim in this repo about "the Android build passes" is made from harness
runs — the harness validates the pure-Kotlin subset only.

## 2. What the harness is

- `tools/kotlin-harness/src/org/junit/Test.kt` — pure-Kotlin stand-ins for JUnit 4
  annotations: `@Test(expected=…, timeout=…)`, `@Ignore`, `@Before`, `@After`,
  `@BeforeClass`, `@AfterClass`, `@Rule` (declared but NOT executed).
- `tools/kotlin-harness/src/org/junit/Assert.kt` — JUnit 4-style assert functions as
  **top-level functions in package `org.junit.Assert`** (deliberately NOT an object:
  a Kotlin object cannot be star-imported, and this repo's tests use both
  `import org.junit.Assert.*` and `import org.junit.Assert.assertEquals`).
- `tools/kotlin-harness/src/runner/Main.kt` — reflection runner (semantics in §4).
- `tools/kotlin-harness/src/androidx/compose/ui/geometry/Offset.kt` — minimal stub of
  Compose `Offset` (only `x`, `y`, `Zero`, `toString`) so `HeroSkyProjection.kt`
  and its test compile without the Android toolchain.
- `tools/kotlin-harness/run_tests.sh` — the one command that reproduces the run.
- `tools/kotlin-harness/try_test.sh` — helper that tries to compile+run ONE
  additional pure-Kotlin test file by iteratively resolving `com.alijafari`
  references from `app/src/main/java` (used for the §6 per-file report).

## 3. Exactly what the main run compiles (and what it excludes, with reasons)

Main run = `bash tools/kotlin-harness/run_tests.sh` from repo root. It compiles:

1. The four shim files above.
2. ALL of `app/src/main/java/com/alijafari/red/astronomy/startracker/**` (the whole
   startracker library — pure Kotlin by design).
3. Two non-startracker main files the startracker code depends on:
   `astro_engine/FrameTransformationEngine.kt`, `astro_engine/AstroTime.kt`.
4. ALL 30 test files under `app/src/test/java/.../startracker/**`.
5. `RefractionTest.kt` (pure Kotlin).
6. `ui/rendering/HeroSkyProjection.kt` + `HeroSkyProjectionTest.kt` (via the Offset
   stub; added to the main run in pass 2 — pass 1 ran these as a one-off).

Result: 32 test classes, **137 tests, 0 failures, 0 errors** (final green run saved
verbatim at `docs/startracker/evidence/HARNESS_FINAL_RUN_2026-09-03.txt`).

Full inventory from `find app/src/test -name "*Test.kt"` is 55 files. The 23 not in
the main run, each with reason (from the §6 attempt where applicable):

| Test file | In main run? | Reason not in main run |
|---|---|---|
| 30 startracker `*Test.kt` | YES | — |
| `RefractionTest.kt` | YES | — |
| `HeroSkyProjectionTest.kt` | YES | — (needs the Offset stub, provided) |
| `ExampleRobolectricTest.kt` | NO | Robolectric/`androidx.test` — needs Android toolchain |
| `GreetingScreenshotTest.kt` | NO | Compose UI test + Robolectric |
| `ARCalibrationPromptTest.kt` | NO | Robolectric + `android.content.Context` |
| `IssTleWorkerTest.kt` | NO | depends on `data/TleRepository.kt` (Android) — CANNOT COMPILE (tried, §6) |
| `SkyOrientationProjectionTest.kt` | NO | depends on `ARProjectionEngine.kt` (Compose) — CANNOT COMPILE (tried, §6) |
| `EclipseEngineTest.kt` | NO | `EclipseEngine.kt` needs `util/LocaleHelper.kt` (Android, Persian digits) — CANNOT COMPILE (tried) |
| `ISSPassPredictionTest.kt` | NO | dep closure reaches Android files (`ISSEngine`, `TimeEngine` uses `android.util.Log`) — CANNOT COMPILE (tried) |
| `ClassificationAuditTest.kt` | NO | dep closure reaches `AstroDispatchEngine` → Android engines — CANNOT COMPILE (tried) |
| `Phase4MigrationTest.kt` | NO | same closure (`AstroDispatchEngine` → `SatelliteEngine`/`ISSEngine`/`ObservabilityEngine` are Android files) — CANNOT COMPILE (tried) |
| `Phase5FeatureMigrationTest.kt` | NO | same closure — CANNOT COMPILE (tried) |
| `RelativisticEngineTest.kt` | NO | same closure — CANNOT COMPILE (tried) |
| `SatelliteARConsistencyTest.kt` | NO | same closure — CANNOT COMPILE (tried) |
| `SGP4PropagatorTest.kt` | NO | dep closure reaches `TimeEngine.kt` (imports `android.util.Log`) — CANNOT COMPILE (tried) |
| `AstroTimeTest.kt` | (ran separately) | not in main run; RAN standalone 14/14 — could be folded in later |
| `FrameTransformationEngineTest.kt` | (ran separately) | not in main run; RAN standalone 13/13 |
| `DeepSkyEngineTest.kt` | (ran separately) | RAN standalone 11/11 |
| `LunarSolarEngineTest.kt` | (ran separately) | RAN standalone 8/8 |
| `SkyMapRendererTest.kt` | (ran separately) | RAN standalone 12/12 |
| `VSOP87EngineTest.kt` | (ran separately) | RAN standalone 5/5 |
| `ExampleUnitTest.kt` | (ran separately) | RAN standalone 1/1 |
| `com/zig/gravity/GravityCollisionTest.kt` | (ran separately) | RAN standalone 20/20 |
| `com/zig/gravity/GravityPhysicsTest.kt` | (ran separately) | RAN standalone 19/19 |
| `com/zig/gravity/GravitySheetScrollTest.kt` | (ran separately) | RAN standalone 5/5 |
| `GravityCameraFollowTest.kt`, `GravitySandboxIntegrationTest.kt`, `GravityUpgradeTest.kt` | NO | need `SimulationViewModel.kt` (Android ViewModel) — CANNOT COMPILE (tried) |

No test file was silently forgotten: every file not in the main run was either
attempted individually (§6 / table above) or is Robolectric/Compose by import.

## 4. Runner semantics (exact behavior of `runner.MainKt`)

- **Discovery:** reflection over the classes named as CLI args; instance methods
  carrying `@Test`. `@Ignore` on the method or the class => `SKIP` line, excluded
  from "Tests run".
- **Lifecycle:** `@Before` before EVERY test, `@After` after every test (run even
  when the test throws). `@BeforeClass`/`AfterClass` only for STATIC (companion
  `@JvmStatic`) methods. `@Rule` is declared but NOT executed.
- **`expected = X::class`:** PASS iff the thrown Throwable `isInstance` of X —
  i.e. subclass matches count, matching real JUnit 4 (not exact-class).
- **`timeout = n`:** body runs on a daemon worker thread; join(n ms); on expiry the
  worker is interrupted and the test reports `java.util.concurrent.TimeoutException`
  as a FAILURE. The body runs exactly once either way (worker's throwable is
  captured, not re-invoked).
- **Failure vs Error:** AssertionError => Failure; any other Throwable (incl.
  TimeoutException, InstantiationException, exceptions out of @Before/@After) =>
  Error. JUnit 4's distinction, preserved.
- **What a compile failure looks like:** the run SCRIPT stops at the kotlinc step
  and prints `error:` lines — a class that fails to compile never appears as "0
  tests"; it aborts the whole run (which is why the main run only includes files
  that compile together). `try_test.sh` isolates one file and reports CANNOT COMPILE.
- **Exit code:** 0 iff Failures == 0 && Errors == 0, else 1. `Tests run` counts
  only executed (non-skipped) tests.

## 5. Reproduce

```
pip install --break-system-packages jdk4py==25.0.2.1   # JRE 25.0.2
npm install -g kotlin-compiler@2.4.10                  # kotlinc + kotlin-stdlib.jar
bash tools/kotlin-harness/run_tests.sh
```

Raw unedited output of the final green run (137/0/0, pass 2):
`docs/startracker/evidence/HARNESS_FINAL_RUN_2026-09-03.txt`.

## 6. Per-file results for app test files outside the main run (attempted, NOT fixed)

Method: `bash tools/kotlin-harness/try_test.sh <testfile>` — iteratively adds
pure-Kotlin main-file dependencies until no unresolved references remain; gives up
when a needed dependency itself imports `android.*`/`androidx.*`.

| File | Result |
|---|---|
| AstroTimeTest | **RAN: 14/14 PASS** |
| FrameTransformationEngineTest | **RAN: 13/13 PASS** |
| DeepSkyEngineTest | **RAN: 11/11 PASS** |
| SkyMapRendererTest | **RAN: 12/12 PASS** |
| LunarSolarEngineTest | **RAN: 8/8 PASS** |
| VSOP87EngineTest | **RAN: 5/5 PASS** |
| ExampleUnitTest | **RAN: 1/1 PASS** |
| GravityCollisionTest (zig) | **RAN: 20/20 PASS** |
| GravityPhysicsTest (zig) | **RAN: 19/19 PASS** |
| GravitySheetScrollTest (zig) | **RAN: 5/5 PASS** |
| SkyOrientationProjectionTest | CANNOT COMPILE — needs `ARProjectionEngine.kt` (imports `androidx.compose.*`); `OrientationProvider.kt` itself imports `android.hardware.*` |
| SGP4PropagatorTest | CANNOT COMPILE — dep closure reaches `TimeEngine.kt` (`android.util.Log` import) |
| ISSPassPredictionTest | CANNOT COMPILE — closure reaches `ISSEngine.kt`/`TimeEngine.kt` (Android) |
| EclipseEngineTest | CANNOT COMPILE — `EclipseEngine.kt` needs `util/LocaleHelper.kt` (Android; `toPersianDigits`) |
| ClassificationAuditTest | CANNOT COMPILE — closure reaches `AstroDispatchEngine.kt` → `SatelliteEngine`/`ISSEngine`/`ObservabilityEngine` (Android files) |
| Phase4MigrationTest | CANNOT COMPILE — same closure |
| Phase5FeatureMigrationTest | CANNOT COMPILE — same closure |
| RelativisticEngineTest | CANNOT COMPILE — same closure |
| SatelliteARConsistencyTest | CANNOT COMPILE — same closure |
| IssTleWorkerTest | CANNOT COMPILE — `TleRepository.kt` is Android (`data` layer) |
| ARCalibrationPromptTest | CANNOT COMPILE — Robolectric (`@RunWith(RobolectricTestRunner)`) |
| ExampleRobolectricTest | CANNOT COMPILE — Robolectric |
| GreetingScreenshotTest | CANNOT COMPILE — Compose UI test (`createComposeRule`) + Robolectric |
| GravityCameraFollowTest / GravitySandboxIntegrationTest / GravityUpgradeTest (zig) | CANNOT COMPILE — need `SimulationViewModel.kt` (Android ViewModel) |

Aggregate of the standalone RAN files: 108 tests, 108 PASS, 0 failures, 0 errors.
Nothing found in them was modified — this section is report-only, per instructions.

## Final-pass update (2026-09-04)

Harness: 155 tests / 0 failures / 0 errors (`bash tools/kotlin-harness/run_tests.sh`).
Added since last disclosure: CoordinateOracleTest (3, reads frozen astropy oracle CSVs
from docs/startracker/evidence/ — MEASURED-oracle regression), MagneticDeclinationTest
(7, pure math), CappedQuadIndexTest (6, incl. real-catalog 8,870-star build),
SyntheticE2ETest (1, SYNTHETIC-SKY lost-in-space on the real catalog; skips silently if
data/startracker CSV absent). Main-code sources added to the compile set:
astro_engine/{CoordinateEngine,CoordinateEngineLegacy,TimeEngine,AstroTime,
FrameTransformationEngine,SunEngine,MoonEngine,LunarSolarEngine,PlanetEngine,
VSOP87Engine,MagneticDeclination}, domain/Models.kt. No new shims; existing shims
unchanged. Toolchain: kotlinc 2.4.10 (npm kotlin-compiler), JRE 25 via jdk4py.
Probes (not tests): tools/kotlin-harness/probes/{CoordinateOracleProbe,SyntheticE2EProbe}
+ CatalogSizeProbe modes ingest/cappedcsv.


## §9 Z-P1 (2026-09-04): ARProjectionEngine + SkyOrientationProjectionTest shim set

`run_projection_test.sh` compiles `astro_engine/ARProjectionEngine.kt` and the
previously-never-run `app/src/test/.../SkyOrientationProjectionTest.kt` UNMODIFIED,
and executes it (first time ever: **5/0/0**, evidence/P1_PROJECTION_TEST_2026-09-04.txt).
(Z-P2 added `ARProjectionPinholeTest.kt` to the same runner — now 6/0/0.)

The item mandated shimming ONLY `androidx.compose.ui.geometry.Offset`. That proved
INSUFFICIENT as literally worded: ARProjectionEngine.kt imports seven further Android
symbols at file scope (Context, Matrix, Rect, CameraCharacteristics, CameraManager,
Log, PreviewView) plus the Gradle-generated `BuildConfig`, and Kotlin rejects
unresolved imports at compile time — the file either compiles whole or not at all.
Resolution: those symbols are provided as COMPILE-ONLY stubs under
`tools/kotlin-harness/src/` (one file per package), each individually documented.
None carries semantics on any executed path:
- Context.getSystemService → null ⇒ getCameraIntrinsics falls to its documented
  FALLBACK_DEFAULT tier; the device-intrinsics path is uncallable by construction
  (CameraCharacteristics.get additionally returns null for every key).
- Matrix.mapPoints reached only via a non-null sensorToViewMatrix — every test passes
  null; Log.d no-op; BuildConfig.DEBUG=false; PreviewView appears only in KDoc;
  Rect/Size members never execute (their sources always return null).
- Offset remains the existing pure-value shim from the HeroSky work (x, y, ctor).

**CLASSPATH WARNING**: every shim in this section is a Kotlin source file that must
NEVER be compiled or present on a classpath together with the real Android/Compose/
CameraX artifacts (duplicate class definitions, and the stubs' no-op semantics would
silently disable the device code paths). The shims live ONLY under
`tools/kotlin-harness/src/` and are consumed ONLY by `run_projection_test.sh`
(and `run_tests.sh`, which already used the junit/Offset/ICU subset). Gradle builds
never see this directory.


## Gate-pass update (T1–T5, 2026-09-04)

Harness at gate-pass tip: **167/0/0** (`run_tests.sh`; 161 closing-pass + 2
FullFieldVerifierTest T4(b) tests + 4 HardwareDistortionReaderTest). **CI JUnit is now
THE gate** (run 33879125683: 65 files / 456 tests / 0 failures — see
T1_CI_JUNIT_2026-09-04.md); this harness is a fast local pre-check; per-file
comparison found ZERO outcome discrepancies across the 38 shared files.

Changes this pass:
- New shims: `android/os/Build.kt` (SDK_INT var), `android/hardware/camera2/
  CameraCharacteristics.kt` (metadata bag + the two distortion keys, unchecked-cast
  `get`) — added to the run_tests.sh compile set.
- New harness-only test tree `tools/kotlin-harness/tests/`:
  HardwareDistortionReaderTest lives there, NOT under app/src/test, because it assigns
  `Build.VERSION.SDK_INT` and constructs CameraCharacteristics directly — legal against
  the shims, impossible against the real android.jar (final field / package-private
  ctor). CI therefore never compiles it (deliberate).
- New probe: `probes/S3DistortionProbe.kt` (T4(b) MC, unmodelled k1, same seeds as S3).
- Main-source additions to the compile set: none beyond the startracker tree itself
  (HardwareDistortionReader.kt is inside it; its imports resolve via the new shims).
- Toolchain note: a sandbox rebuild wiped the toolchain mid-pass (both jdk4py and the
  npm kotlin-compiler were gone); the restore recipe (pip --break-system-packages
  jdk4py + npm i -g kotlin-compiler; versions unchanged: kotlinc 2.4.10, JRE 25) is
  proven; run_tests.sh re-derives JAVA_HOME from jdk4py on every run.


## D-pass update (debug diagnostics, 2026-09-04)

Harness: **175/0/0** (171 + 4 StarTrackerDebugFlagsTest + 4 TrialLogLineTest). The
main-source package `startracker/debug/` (StarTrackerDebugHost seam) is EXCLUDED from
the harness compile — Android-only (Context, Compose runtime, BuildConfig,
OrientationProvider); CI compiles it in BOTH variants (assembleDebug + assembleRelease,
run 33899685082). New debug SOURCE SET (`app/src/debug/java/com/alijafari/red/astronomy/
debug/`: panel + TrialLogger) is CI-debug-variant-only and PROVEN absent from the
release APK by dex inspection (assert_debug_only.sh step, same run). New harness tests
tree additions: none (StarTrackerDebugFlagsTest + TrialLogLineTest live in app/src/test
and run in both harness and CI).
