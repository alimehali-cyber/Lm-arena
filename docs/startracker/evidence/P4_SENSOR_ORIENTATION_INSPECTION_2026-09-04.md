# Z-P4 — Sensor orientation 90/270 & azimuth-convention inspection (KIND-A, 2026-09-04)

Inspection only — no code change (KIND-A). All line refs at current tip.

## 1. remapCoordinateSystem: NOT used — and the classic bug surface is absent

`SensorManager.getOrientation()` + `remapCoordinateSystem()` are never called anywhere
in the app (grep: zero hits under app/src). The pipeline derives attitude directly from
the rotation-vector quaternion → smoothed orthonormal matrix R_sensor
(OrientationProvider :228-332), applies declination and calibration as matrix products,
and derives scalars itself:
- camera pointing vector = −(third column) of R_final (OrientationProvider :385-389),
  in world components (px=East, py=North, pz=Up);
- `azimuthDeg = atan2(px, py) normalized` (:391), `pitch = asin(pz)` (:392), roll from
  device-right/up dotted into the local level frame (:395-410).
The usual 90/270 failure mode (reading azimuth from a display-remapped/unremapped
matrix in landscape) therefore has no occurrence site. Display rotation enters ONLY at
the projection stage as `netRotation = (sensorOrientation − displayRotation + 360) % 360`
(ARProjectionEngine :345-376), which is the camera-image pipeline — the correct place.
`displayRotationDegrees` is read once per composition (CompassARScreen :270-288,
API-R-safe, exception-guarded to 0). Manifest sets no `screenOrientation` (not locked):
if the user rotates the device while the AR screen is open, the cached value can go
stale until recomposition. Mitigation note added to the W3 runbook (rotate test);
not fixed here (KIND-A: no reproducible defect offline).

## 2. sensorOrientation 90/270 handling: exact SO(2), verified numerically

ARProjectionEngine Step 4 (:316-325) maps device→sensor coordinates with the rigorous
rotation `xSensor = xDev·cosθ − yDev·sinθ; ySensor = −xDev·sinθ − yDev·cosθ` using
θ = intrinsics.sensorOrientation. At 90°/270° cosθ = 0 exactly → a pure axis
swap/reflection with no numeric degradation. Verified through the real engine in the
harness: SkyOrientationProjectionTest "cardinal directions with phone 90-degree sensor
orientation" (P1, first-ever run, PASS) and the P3 corner/edge measurements (all
gnomonic ratios 1.0000). netRotation branches (90/180/270/else) then rotate + FILL_CENTER
crop the sensor image to the view; P3 verified that arithmetic.

## 3. Clockwise-from-north vs astronomical azimuth: consistent N→E everywhere

- Android rotation-vector world frame: X = East, Y = magnetic north, Z = up (device→world).
  Provider applies R_declination (east-positive WMM D, world-Z rotation :349-365) → true north.
- Provider azimuth: atan2(East, North) → 0°=N, 90°=E — i.e. measured from north TOWARD
  EAST (counterclockwise viewed from above the horizon... equivalently: matches the
  compass reading of a phone pointed at the sky when the user turns right→azimuth
  increases toward E). [Convention note: this is astropy's AltAz convention (az measured
  N→E), NOT the classical almanac N→W convention.]
- Engine/consumers: ARProjectionEngine Step 1 uses ox = cos(alt)·sin(az), oy = cos(alt)·cos(az)
  with world East=+X/North=+Y — identical N→E convention; Sun/Moon/planet ephemerides
  were oracle-verified against astropy (0.35-0.6′) through this exact chain, which pins
  the whole chain to ONE convention end-to-end. No mixed-convention site exists
  (grep: no surveying N→W azimuth producer in the codebase).

## 4. UNEXECUTED disclosure

Sensor values, real WindowManager rotation, real PreviewView cropping execute only on a
device. W3 runbook keeps: (a) landmark-bearing declination check, (b) portrait/landscape
rotate check for the cached displayRotationDegrees, (c) 30/60° off-centre overlay-vs-
bright-star check. Loosened: none. KIND-A (inspection only).
