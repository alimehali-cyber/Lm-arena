# Real-Device Field Test Protocol — 30-Minute Ordered Bring-Up (Z-W3, 2026-09-04)

**Purpose.** First on-device validation of the compass/AR accuracy chain and the
star-tracker adapters, in strict order, every step timed and logged. The offline
harness (161/0/0) validated the pure-Kotlin chain; this protocol validates the Android
halves (UNEXECUTED until now). Target duration: ~30 minutes of field time. Device: any
recent Android phone, rear camera, GPS, dark site, clear night. Bring: this checklist,
a red flashlight, a note app. Optionally binoculars/star chart for identification.

**Standing rules.**
- `StarTrackerConfig.ENABLED` stays **false** until Step 7 (and returns to false after
  the session unless a decision is recorded). All W2 flags default OFF; enable them
  only in the order of the W2 flag map.
- Every step: record time, pass/fail, and the number the step asks for. If a step
  FAILS, stop and log — do not proceed to dependent steps.
- Logcat filters to watch: `Intrinsics tier selected`, `CameraFrameObserver`,
  `ARCalibration`, `OD1`.

## Step 1 — Arrival baseline, flag OFF (5 min)

1. Cold-start the app with all flags OFF (defaults). GPS on, wait for fix.
2. Point at **two known landmark bearings** chosen in advance (e.g. a distant mast and
   a hilltop, bearings from a map; pick objects > 1 km away). For each:
   - Center the object on the crosshair; compare the app's object azimuth (AR overlay
     label or finder readout) against the map bearing. **Record the difference in
   degrees = local magnetic declination error.** With GPS active this should be ≈ 0°
     ±1–2° (declination is applied at the attitude source — see
     evidence/V3_DECLINATION_PLACEMENT_2026-09-04.md). A consistent offset ≈ the local
     declination (check: https://www.ngdc.noaa.gov/geomag/calcalc) means the
     pre-final-pass pipeline regressed — STOP and report.
   - Rotate the phone to landscape, confirm the overlay stays glued to the object
     (display-rotation staleness check from P4). Record any jump.
3. Record intrinsics tier from logcat (`Intrinsics tier selected: ...`).

## Step 2 — Ephemeris spot check: Sun set / planet line-up (3 min)

Do this at twilight BEFORE stars are visible. Compare the app's Sun/planet positions
against the sky (or an independent ephemeris app screenshot):
- **Sun**: horizon azimuth at sunset within ~1° of an ephemeris app; label position
  matches visually when on screen.
- **One bright planet** (whichever is up): separation from its true chart position
  < 1°.
Record both numbers. (Offline oracle accuracy was 0.35–0.6′; the extra degrees of
tolerance are for handheld pointing + clock/GPS errors.)

## Step 3 — Overlay-vs-bright-star geometry, 30° and 60° off-centre (5 min)

After dark, center a bright identified star (Vega/Arcturus/Sirius class):
1. **Centred**: overlay marker on the star; estimate offset in marker-diameters.
2. **Star at ~30° off-centre** (point the phone so the star sits near the screen
   edge): record offset again, convert to arcmin (screen ≈ 52.7° across the width on a
   1080-px-wide canvas → 1 px ≈ 2.9′; a marker ≈ 28 px ≈ 80′). Offsets here probe the
   FILL_CENTER crop + pinhole math (P2/P3): expected < ~60′ at 30°, < ~90′ at 60°
   (accumulated intrinsics-tier error dominates; the projection itself is exact).
3. Repeat at ~60° off-centre (star near the corner). Record arcmin.
A systematically RADIAL drift growing with off-axis angle → intrinsics scale error
(fallback tier too wide/narrow); a constant rotation → roll/declination issue.

## Step 4 — The seven corrected stars (4 min)

For each of **Sirius, Vega, Arcturus, Polaris, Betelgeuse, Aldebaran, Antares**
(or the subset above your horizon): center the star, read the app's on-screen label
vs the true star; record pass/fail each. All seven should be identified at the
correct position (they are catalog-corrected to ≤5″ per B3). Also confirm the
**Moon edge** and, if visible, **Mars/Jupiter/Saturn** labels sit on target.

## Step 5 — Lens-cap NO_LOCK (2 min)

Enable ONLY the W2 switch chain up to `PIPELINE_CAMERA_FEED` (`ENABLED=true`,
`PIPELINE_CAMERA_FEED=true`, `TRACKER_TO_ORIENTATION_PHASE6=false`,
`PROJECTION_SELF_CALIBRATED_PHASE7=false`). Cap the lens:
- Expect: no lock ever granted; UI stays sensor-only; logcat shows
  `no detections (lens cap / blank sky)`; no crash within 60 s; frame dropping visible
  (no queued backlog: pipeline log lines stop when busy).
Record: 60 s clean pass/fail. Uncap. **Return `ENABLED=false` if anything but clean
NO_LOCK.**

## Step 6 — First live sky lock + OD1 discrepancy logging (6 min)

Same flag state, lens uncapped, phone steady on a dark sky region (near the galactic
plane is easiest):
- Expect `FULL_LOCK` or `MARGINAL_LOCK` within a few seconds (solve budget ~50 ms on
  JVM; device slower — accept up to 2 s). Record time-to-lock and lock state.
- **OD1 (acquisition discrepancy)**: logcat `OrientationProvider` lines comparing the
  tracker attitude against the sensor attitude at acquisition: record the discrepancy
  in degrees for the first 5 locks. Expected ≤ ~3° (declination/frame conventions);
  a consistent offset near 90°/180° means the camera→device frame rotation is wrong —
  STOP (this is the known open PHASE6 integration point).
- Confirm the AR overlay does not jump when the lock appears (blend recommendation
  should be PREFER_SENSOR/PREFER_TRACKER, not a snap).

## Step 7 — Projection tier PHASE7 (3 min) — only if Steps 5–6 passed

Set `PROJECTION_SELF_CALIBRATED_PHASE7=true` (requires a published self-calibration
profile; if none published, the tier silently falls through — confirm from logcat
which tier is active). Re-run the 30° off-centre check from Step 3; record whether the
offset shrank. Return flags to OFF afterwards.

## Step 8 — Wrap (2 min)

- Restore ALL flags OFF (`ENABLED=false`).
- Note battery drain %, device model, Android version, sky conditions.
- File results under `docs/startracker/evidence/DEVICE_TRIAL_<date>.md` with: every
  recorded number from Steps 1–7, pass/fail, and the logcat excerpts
  (`Intrinsics tier`, `OD1`, `no detections`).

## Attribution reminder (data placement)

The in-app/legal attribution for the star data must read, at first entry point of the
star-tracker UI (About/legal screen): "Star data from the HYG database (HYG v3.6),
© astronexus.com, CC BY-SA 4.0. Based on Hipparcos, Yale Bright Star, and Gliese
catalogues." Verify it renders during Step 5 bring-up (first launch of the tracker
screen) — this is the HYG attribution placement check.

**Status header for the filed report:** device trial NOT YET PERFORMED; the offline
gate (S3: FL 0/10,000 joint, 0 at FULL_LOCK) is passed, so this protocol may be
executed. `StarTrackerConfig.ENABLED` is false in the repo and must stay false in
committed code.
