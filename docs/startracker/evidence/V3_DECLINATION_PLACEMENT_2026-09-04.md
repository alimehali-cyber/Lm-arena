# Z-V3 — Declination placement audit (and B1 retraction), 2026-09-04

## What drives the AR overlay projection?

`ARProjectionEngine.projectAltAz(...)` takes BOTH a `rotationMatrix: FloatArray?` and
scalar `(currentAzimuth, currentAltitude, currentRoll)`. Inspection of ALL 9 production
call sites (CompassARScreen.kt :984, :1048, :1075, :1115, :1153, :1200, :1263, :1355,
:2600): every one passes `rotationMatrix = if (isSensorActive) skyOrientation.rotationMatrix else null`.
**When sensors are active the overlay is driven by the ROTATION MATRIX**, not by the
scalar azimuth (the scalars are used only in the manual-drag fallback where the matrix
is null).

## Where is declination applied?

At the attitude SOURCE, and it was already applied before the final pass:
- `OrientationProvider.updateLocation(lat, lon, alt)` (OrientationProvider.kt :146-159)
  computes `GeomagneticField(...).declination` (Android's WMM wrapper) into
  `magneticDeclination`.
- CompassARScreen calls `updateLocation` at THREE sites (:349, :365 GPS listener; :435
  DisposableEffect) whenever a location exists.
- Every sensor update then builds `R_true = R_declination * R_sensor`
  (OrientationProvider.kt :351-372) — a yaw rotation about the local vertical (world Z)
  applied to the attitude the projection consumes — and the exposed scalar
  `skyOrientation.azimuth` (:378) is computed FROM that corrected matrix.

So both the overlay (matrix path) and the scalar azimuth were already TRUE-north
referenced. OD4's premise ("implement magnetic declination, default on") was already
satisfied by this pre-existing mechanism.

## Consequence: the final-pass B1 change was a DOUBLE correction — REVERTED (this item)

B1 (commit 43e360e) added `MagneticDeclination.trueAzimuth(skyOrientation.azimuth, D)`
at `currentAzimuth`. Since `skyOrientation.azimuth` is already declination-corrected,
this added +D a second time to every SCALAR consumer whenever GPS was active:
hit-test `dAz` (:1268), FinderEngine (`phoneAzimuthDeg`), horizon rendering loop
(:1014), direction arrows (:1318). The one-time legacy-yaw rebase would equally have
corrupted stored calibrations (they were calibrated against already-true render
azimuths). Magnitudes: +3.6° (Nurabad default), +4.97° (Tehran), −26.78° (Cape Town).

Reverted in Z-V3: CompassARScreen B1 block removed (comment documents the source
application point), `ARCalibrationManager.rebaseYawForDeclinationOnce` + marker key
removed, `StarTrackerConfig.APPLY_MAGNETIC_DECLINATION` retired to `false` with a
tombstone doc. `MagneticDeclination.kt` (pure math) + its 7 tests are KEPT (correct
math, no production caller; retained as a tested utility). KIND: the revert restores
the pre-final-pass behavior of these three files byte-for-byte in the affected regions.

## Heading-number UI inventory (true vs magnetic)

No screen renders a compass-heading number derived from `currentAzimuth` /
`skyOrientation.azimuth`. Numbers actually displayed (CompassARScreen; verified by grep
over all format sites):
- Finder "total angular distance" `%.1f°` (:1796) — a DIFFERENCE of two true azimuths;
  frame-consistent before and after the revert.
- Location lat/lon `%.2f°` (:2423) — not a heading.
- AR calibration offsets yaw/pitch/roll `%+.1f°` (:2450-2452, :2502-2503) — offsets in
  the (already-true) render frame, not headings.
- Object positions in detail screens/labels come from ephemeris horizontal coordinates
  (true-north by construction, oracle-verified in A4).
=> Every heading/position number the user sees is true-north referenced, via the
pre-existing attitude-source correction; nothing displays raw magnetic heading.

## UNEXECUTED disclosure

updateLocation's GeomagneticField value and the R_declination rotation execute only on
a device; the audit above is code-inspection of the live path (the matrix/azimuth
derivation lines quoted). Device check remains in the runbook (known-landmark bearing).
