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


## Fix pass (2026-09-04, commit `5112eef`): long-press dead in the field -> explicit DIAGNOSTICS button

**Reported:** with the debug APK from run 33899685082, long-pressing anywhere on the
AR screen did NOT open the overlay.

**Root cause (code-traced, CompassARScreen):** the D2 long-press detector was added
as a `pointerInput` on the root AR `Box`, but three PRE-EXISTING gesture consumers
own that touch path and starve it:

1. the zoom `detectTransformGestures` (:856) chained AFTER the long-press input —
   later filters in a modifier chain receive events first, and it consumes position
   changes once past touch slop (~8 px);
2. the manual-offset `detectDragGestures` (:863) on the same chain, which explicitly
   `change.consume()`s after slop;
3. the full-screen Layer-2 AR Canvas (:918) — the actual hit-test target for nearly
   every touch — with its own `detectTapGestures` (press/tap) handler.

A long press must survive ~500 ms with nothing consumed; on a hand-held phone aimed
at the sky, micro-movement exceeds slop, the zoom/drag detectors consume the gesture,
and the parent long-press is cancelled. Interactive controls cover much of the rest.
The gesture had no visual affordance or feedback either. (Exact per-detector split
cannot be distinguished without a device, but the consumption conflicts are
structural — any one of them kills the press.)

**Fix (minimal, reversible, NO new diagnostics UI):** ONE temporary
`if (BuildConfig.DEBUG) { Button("DIAGNOSTICS") }` as the last child of the same AR
`Box` — bottom-end corner, red (`0xFFE53935`), bold label, above the nav bar, drawn
on top. Its `onClick` calls the SAME existing entry point the long-press used:
`StarTrackerDebugHost.open(context, orientationProvider)` — nothing else. The
long-press and the D2/D3 panel/logger are untouched. `open()` additionally logs
`StarTrackerDebugHost: open requested … panel=<resolved>` so field testers can
confirm invocation via logcat if ever needed.

**UI path (button -> overlay):** Button onClick -> `StarTrackerDebugHost.open`
(BuildConfig.DEBUG-guarded; sets `Access`, reflection-loads
`com.alijafari.red.astronomy.debug.StarTrackerDebugPanelImpl` — present in the debug
APK per the D4 dex assert — sets `visible=true`) -> the Box's existing hosting branch
`if (BuildConfig.DEBUG && StarTrackerDebugHost.visible.value) HostContent()` ->
`panelImpl.Content()` -> the D2 dialog (read-only rows + D1 switches + D3 trial
buttons) over the AR screen. D3 chain unchanged: Start trial / Mark step N / Stop /
Share -> `filesDir/startracker-trials/trial-*.jsonl`.

**Release:** the button branch is `BuildConfig.DEBUG`-gated (compile-time false in
release — eliminated), the panel/logger classes are absent from the release compile
entirely (debug source set; D4 dex assert still enforced in CI).

**REMOVAL NOTE (after field testing):** delete the button block in
CompassARScreen.kt (marked `TEMPORARY DIAGNOSTICS BUTTON`), the long-press
`.then(...)` block, and the hosting `if` — three small, clearly-commented regions;
no engine/projection/sensor code is involved.

**Verification:** CI run **`33906477868`** (head `5112eef`) — GREEN:
`build` ✓ (assembleDebug + assembleRelease, D4 dex assert ✓: debug ≥ 1 / release 0 —
the release APK still contains no diagnostics classes), `unit-tests` ✓
(67 files / 466 tests / 0 failures). Harness 175/0/0. The new debug APK with the
button is the `app-debug-apk` artifact / `ci-debug-apk` rolling pre-release of that
run. On-device tap verification is the field tester's first action (logcat filter
`StarTrackerDebugHost` shows `open requested … panel=true` on success).
