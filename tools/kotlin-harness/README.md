# kotlin-harness — offline pure-Kotlin test harness

Built for the star-tracker remediation (2026-09) because this sandbox has **no Gradle,
no Android SDK, no network to Maven**: `./gradlew test` is impossible here (Gradle TLS
failure, see `docs/startracker/evidence/HARNESS_DISCLOSURE.md`). The harness compiles the
pure-Kotlin subset of the app with `kotlinc` and runs JUnit-4-shaped tests on a plain JVM.
Every number reported in the remediation docs comes either from this harness or is labelled
"unexecuted".

## Contents (source + scripts only — no toolchain, no binaries, ever)

| File | What it is |
|---|---|
| `src/org/junit/Test.kt`, `src/org/junit/Assert.kt` | minimal JUnit-4 API **shims** (see warning below) |
| `src/runner/Main.kt` | tiny reflection runner: `@Test` discovery, `@Before/@After/@BeforeClass/@AfterClass`, `@Ignore`, `expected=`/`timeout=` attributes, per-class and summary counts |
| `src/androidx/compose/ui/geometry/Offset.kt` | shim for the one Compose type the tested code touches |
| `run_tests.sh` | one command = full star-tracker suite run (`bash tools/kotlin-harness/run_tests.sh`) |
| `try_test.sh` | single-test-file compile probe: auto-resolves the dependency closure of one test file, reports `CANNOT COMPILE` + blockers for Android-dependent tests (used for the standalone-test inventory) |
| `CatalogSizeProbe.kt` | the catalog-size measurement probe (generates `evidence/CATALOG_SIZE_MEASURED_2026-09-03.txt`) |

## ⚠ org.junit.Assert shim classpath warning

The harness ships its **own** `org.junit.Assert` / `org.junit.Test` implementations — a
deliberately small subset (`fail, assertTrue/False, assertNull/NotNull, assertEquals`
incl. double/float-delta and Long overloads, `assertArrayEquals`, …). **Never put a real
junit-4 jar on the `kotlinc` or `java` classpath together with these sources**: you get
duplicate `org.junit.Assert` classes — either a compile-time redeclaration error or, worse,
the runner silently loading whichever version the classloader picks, whose assert semantics
may differ from what the tests were written against. The only jar on the runtime classpath
must be `$KT_HOME/lib/kotlin-stdlib.jar` (see both scripts). If a test needs an assert the
shim lacks, ADD it to `src/org/junit/Assert.kt` — do not add a jar.

## Prerequisites (external to the repo — this is why `.gitignore` lists them)

* `kotlinc` on `PATH` or at `/usr/local/lib/node_modules/kotlin-compiler` (used: 2.4.10), override with `KT_HOME`
* a JRE, resolved via the `jdk4py` pip package unless `JAVA_HOME` is already set
* build output goes to `/tmp` (`OUT` env or `mktemp`), never inside the repo

## Reproducing a run

```bash
bash tools/kotlin-harness/run_tests.sh
```

Reference outputs (unedited) live in `docs/startracker/evidence/HARNESS_FINAL_RUN_*.txt`;
what the harness can and cannot compile is disclosed in
`docs/startracker/evidence/HARNESS_DISCLOSURE.md` (§6 lists the Android-blocked tests).
