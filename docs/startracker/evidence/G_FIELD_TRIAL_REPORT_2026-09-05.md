# Field-Trial Guide Pass — Report (G-§5) — 2026-09-05

**Loosened: none.** All items delivered as spec'd or explicitly disclosed below.

## Exactly what I do (one paragraph, as required)

I replaced the retired developer-only diagnostics overlay with a guided, level-based
field trial a non-developer can run: on the AR screen a single red "Field Test" button
(debug builds only, proven absent from release by CI dex inspection) opens a small guide
card pinned to the bottom of the camera picture — never covering the middle of the
screen, collapsible to a one-line pill. The card walks the tester through levels 0–12,
one plain-English instruction at a time: point at the Sun/Moon/a bright star, TAP the
real thing where it appears on the screen, drag the crosshair, press Confirm — the app
itself computes the angular error from the same camera frame, records every number to a
JSON trial file that survives closing the app, killing it, or rebooting the phone, and
saves an evidence picture (target ring, crosshair, offset arrow) with each confirmed
tap. Midway the trial switches on the experimental star tracker and measures how long
it takes to lock onto the real sky, whether the tracker-based ring beats the
compass-based ring, whether it stays honest with the lens covered, and whether it makes
up directions while sweeping — all recorded automatically, with plain-English failure
sentences when something times out. At the end the tracker turns itself off, a summary
with ticks and crosses is generated, and Share sends one zip containing the trial
record, the summary, the pictures and the raw camera frames captured at first lock.

## What shipped (file:line anchors)

- **Seam (only main-source footprint):** `startracker/debug/FieldTrialHost.kt` —
  `FieldTrialGuide` interface + host object + `FrameState` + observer handoff; every
  call site guarded by `BuildConfig.DEBUG` (compile-time false in release).
- **AR screen wiring:** `ui/screens/CompassARScreen.kt` — publishes `FrameState`
  (attitude/R/zoom/intrinsics/sensor-to-view 9 values/display rotation/GPS) every
  projection pass; hands the `CameraFrameObserver` to the host; debug-guarded rebind
  with the ImageAnalysis use case when a trial opens; "Field Test" button replaces the
  old DIAGNOSTICS button + long-press (single debug entry point).
- **Guide (debug source set, `fieldtrial/`):** `GuideUi.kt` (card/pill, level copy,
  tap→crosshair→Confirm, markers incl. L9 dual rings, night palette, Details, help
  sheet, Part B watchers L8/L10/L11, L12 share), `GuideHost.kt` (reflection target),
  `FieldTrialController.kt` (document store: persist-on-every-change to
  `filesDir/fieldtrial/trial-<id>.json`, restore-newest, shots, tracker lifecycle,
  zip {trial.json, summary.md, shots/, frames/}, FileProvider share),
  `Capture.kt` (PixelCopy screenshot with deterministic marker-scene fallback),
  `StarTrackerRuntime.kt` (+ solver catalog exposure), `engine/PartBAnalysis.kt`
  (pure L10/L11 rules), `engine/TrackerProjector.kt` (green-ring projection +
  px→deg), engine state machine/MiniJson/TrialSummary/TargetPicker/SunEvents/
  InverseProjection/TapMeasurement from earlier G commits.
- **Dex proof:** `.github/scripts/assert_debug_only.sh` — debug APK must contain
  `Lcom/alijafari/red/astronomy/fieldtrial/`, release APK zero occurrences (CI job
  `build`, green on e9424f0).
- **Share authority:** debug-only manifest `app/src/debug/AndroidManifest.xml`
  (`${applicationId}.fieldtrial_files`) + `app/src/debug/res/xml/fieldtrial_paths.xml`.

## Acceptance-criteria status

| Item | Status |
|---|---|
| 1.1 Field Test DEBUG button above nav bar | DONE (replaces DIAGNOSTICS; long-press removed) |
| 1.2 card ≤22% + one-line pill, centre 60% never covered, no full-screen composable | DONE (18% cap, bottom-anchored; Robolectric-measured) |
| 1.3 JSON every change, survives death/reboot, versioned attempts, redo | DONE (controller+machine; restore test) |
| 1.4 one title ≤4 words, one instruction ≤2 sentences, ≤3 buttons, Details, "?" sheet | DONE (contract-tested) |
| 1.5 dim red ≥16sp after civil dusk + Dim toggle, thin rings | DONE (default-on palette + window dim + Brighten toggle) |
| 1.6 tester never types; tap→drag→Confirm; yes/no judgments | DONE |
| 1.7 gating Available/Not now(+reason+when)/N-A/Not-in-build | DONE (machine gating + gatingText) |
| 1.8 Confirm ⇒ evidence picture (card hidden, ring+crosshair+arrow) + JSON record | DONE (PixelCopy primary, marker-scene fallback) |
| 2.5 probes (detections, fps, first lock, confidence, solve ms, discrepancy, 3 frames, FailureReason→English) | DONE (ProbeCollector + L8/L10/L11 recording) |
| 3 L0–L12 exactly as spec'd | DONE (L6 honest "no About screen in build; No expected") |
| 4.1 unit tests as listed | DONE — harness 206/0/0 |
| 4.2 Robolectric/Compose | PARTIAL — GuideUiContractTest + GuideUiSmokeTest (render, pill, help-sheet same-state, bottom-anchoring) in CI; no full-level walkthrough automation |
| 4.3 CI dual build + dex absence of every fieldtrial class | DONE (script + green run e9424f0) |
| 4.4 emulator end-to-end run | NOT EXECUTED HERE (no emulator in sandbox) — disclosed |
| 4.5 emulator screenshots | NOT EXECUTED HERE — disclosed (Robolectric JVM render only) |
| 4.6 one-page protocol | DONE — `G_FIELD_TRIAL_PROTOCOL.md` |

## CI / harness ladder (this pass)

- CI: `906f68e` ✗ → `e018552` ✗ (2 compile batches: missing import; controller
  `document` typed `Any`; nullable target) → **`e9424f0` ✓ run 33947869959:
  77 classes / 499 tests / 0 failures / 0 errors; build job incl. dex assert PASS.**
- Offline harness: 201 → 206/0/0 (TrackerProjector +2 px→deg tests, PartBAnalysis +3).
- Commits: `906f68e` (G-2.5), `29a857e` (G-4.1), `b479dd1` (G-1+G-3),
  `159e9e7` (G-4.3), `e018552`+`e9424f0` (compile fixes), then G-4.2/4.6/§5 batch.

## Honest limits

- **UNEXECUTED on hardware:** every on-device behaviour (guide over the live camera,
  PixelCopy shots, tracker Part B numbers, night dim on OLED) is compile-verified +
  JVM-render-verified only; no emulator/device exists in this sandbox (4.4/4.5).
- The L9 green-ring px→deg comparison uses the pinhole small-angle conversion at the
  ring position (exact at centre, second-order at the edges); the JSON records both
  raw ring positions anyway.
- L1's "jump to the Moon level" completes L1 as skipped (Can't find it) then opens L2.
- Gating `trackerWired=true` is honest for this build (P0 wired the pipeline).
