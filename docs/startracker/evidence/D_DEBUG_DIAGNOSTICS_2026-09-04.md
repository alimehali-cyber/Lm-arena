# D-pass — Debug diagnostics, 2026-09-04

Goal: make `REAL_DEVICE_FIELD_TEST_PROTOCOL.md` executable by a non-developer.
Scope exemption used: ONE new debug-only Compose overlay, compiled only when
`BuildConfig.DEBUG`, unreachable in release (CI-proven by dex inspection). No other
UI changes. Commits (prefix D-): D1 `477d286`, D2 `3ac368d` + fix `9cf637d`,
D3 `b410d05`, D4 `818961e` + ci fix `4639d13`.

## What was built

- **D1 — `StarTrackerDebugFlags`** (`startracker/fusion/StarTrackerDebugFlags.kt`):
  runtime overrides for the four gate flags — the analysis gate
  (`PIPELINE_CAMERA_FEED`), `ENABLED`, `TRACKER_TO_ORIENTATION_PHASE6`,
  `PROJECTION_SELF_CALIBRATED_PHASE7`. Each defaults to its const; `resolve(null)`
  (and release call sites, which install no provider) = the consts bit-identically.
  Only exact `"true"`/`"false"` strings override; anything else falls back to the
  const. Prerequisite: the W2 `StarTrackerConfig` hunk (the three PHASE consts, all
  false — behavior-neutral; the W2 ADAPTERS remain unapplied). 4 harness tests
  (`StarTrackerDebugFlagsTest`).
- **D2 — debug overlay** (long-press the AR screen): the ONE screen,
  `StarTrackerDebugPanelImpl`, lives in the DEBUG SOURCE SET
  (`app/src/debug/java/com/alijafari/red/astronomy/debug/`) — not compiled into
  release at all. Read-only display of: applied declination (`OrientationProvider
  .appliedDeclinationDeg`, new read-only getter; shown as text, NEVER toggleable —
  Z-V3 tombstone), intrinsics tier + fx/fy/cx/cy (`ARProjectionEngine
  .getCameraIntrinsics`), distortion tier + k1/k2 (`HardwareDistortionReader`'s
  first live call path), sensor Hz + sensorTsDelta (measured live from the dedicated
  `sensorTimestampNanos` flow, 32-sample window), tracker fields (shown n/a —
  pipeline not wired until the W2 adapter pass), plus the four D1 flag switches
  (persist to SharedPreferences, display const + resolved).
- **D3 — trial logger**: Start trial / Mark step N (with N stepper) / Stop / Share
  in the overlay. Each press appends one timestamped JSON line to app-private
  `filesDir/startracker-trials/trial-<utcstamp>.jsonl` containing ALL D2 fields +
  GPS (best last-known) + step marker + device model/SDK. Serialization is pure
  Kotlin (`startracker/diagnostics/TrialLogLine.kt`, fixed key order, explicit
  nulls, sanitize-to-[A-Za-z0-9_.:+/ -] so no escaping is ever needed; 4 harness
  tests). Share sends the file CONTENT as text (no file provider, no manifest
  change). Protocol updated: every step says which button to press; Steps 5–7
  marked BLOCKED pending the W2 adapter pass.
- **D4 — CI proof + artifacts**: build job runs `assembleDebug assembleRelease`;
  `.github/scripts/assert_debug_only.sh` counts the dex descriptor
  `Lcom/alijafari/red/astronomy/debug/` — must be ≥1 in the debug APK and **0 in
  the release APK** (job fails otherwise). Both APKs uploaded as artifacts
  (`app-debug-apk`, `app-release-apk`; debug also to the rolling `ci-debug-apk`
  pre-release). `unit-tests` job unchanged (`:app:testDebugUnitTest
  -Pgravity.ci.tests=false`).

## Release-unreachability argument

1. The ONLY main-source footprint is `StarTrackerDebugHost` (+ interface) and the
   `OrientationProvider` read-only getter. The two CompassARScreen call sites
   (long-press modifier, overlay hosting) are guarded by `BuildConfig.DEBUG` — a
   compile-time false constant in release; the modifier branch compiles to
   `Modifier` (no gesture detector) and the hosting branch does not exist.
2. The panel class is in the debug source set: the release variant does not compile
   it — no symbol, no dex entry (asserted by D4, `minifyEnabled=false` so nothing
   is stripped-or-hidden; absence is purely the source-set mechanism).
3. Panel loading is reflection (`Class.forName`) guarded by `BuildConfig.DEBUG`;
   even a hypothetical call no-ops (runCatching → null → invisible).
4. No call site reads the SharedPreferences overrides in release — flags resolve
   from consts (D1 tests pin bit-identity). No behavior of the release build
   changes: **release behaviour unchanged** (same code paths, same constants).

## Honest limits

- The overlay/logger are **UNEXECUTED on a device** (no phone in this environment);
  compile + dex-exclusion are CI-verified. First execution = the device trial.
- D1 flag toggles persist + display but do not change runtime wiring YET — the W2
  adapters (camera feed → pipeline, orientation consumption, PHASE7 tier) are
  deliberately NOT applied in this pass; that application is the separate next pass
  and is what makes protocol Steps 5–7 runnable.
- Expected-red history: runs `33897882074` (D2 1/2 — panel interface import
  missing, cascading errors) and `33898451869` (workflow YAML broken by a colon in
  a step name, zero jobs). Both fixed in-commit (`9cf637d`, `4639d13`); the
  definitive D4 run is recorded below.

## CI outcome (D4 verification) — GREEN

Definitive run **`33899685082`** (head `81fd8e3`, 2026-09-04):

- `build` job ✓ — steps: *Build APKs with Gradle (D4 — both variants)* ✓,
  **D4 - assert debug-only screen absent from release APK (dex inspection)** ✓
  (dex occurrences of `Lcom/alijafari/red/astronomy/debug/`: debug ≥ 1, release = 0 —
  the script fails the job otherwise), *Upload debug APK artifact* ✓
  (`app-debug-apk`), *Upload release APK artifact* ✓ (`app-release-apk`, unsigned —
  no keystore in CI), rolling `ci-debug-apk` pre-release updated (debug only).
- `unit-tests` job ✓ — **67 files / 466 tests / 0 failures** (458 gate-pass +
  StarTrackerDebugFlagsTest 4/4 + TrialLogLineTest 4/4; HardwareDistortionReaderTest
  remains harness-only by design).

Failure history on the way (all fixed in-commit, none hidden):
`33897882074` panel interface import missing → `9cf637d`;
`33898451869` workflow YAML broken by a colon in a step name (zero jobs) → `4639d13`;
`33898528862` SharedPreferences.getString has no 1-arg overload (×5) + TrialLogger
start() return type → `db500a2`; `33899128409` SensorRateEstimator ctor param used in
a member body (needs `val`) → `81fd8e3`.

## Harness

`tools/kotlin-harness/run_tests.sh`: **175/0/0** (171 + 4 StarTrackerDebugFlagsTest
+ 4 TrialLogLineTest). The `startracker/debug/` main-source package is excluded
from the harness compile (Android-only: Context/Compose/BuildConfig/
OrientationProvider) — CI compiles it in both variants instead.
