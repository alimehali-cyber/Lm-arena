# Z-P2 — Pinhole-vs-linear check of the AR projection, 2026-09-04

## Finding: the engine is ALREADY an exact pinhole (gnomonic) projection — no KIND-B fix needed

Inspection (ARProjectionEngine.kt Step 4, both the intrinsics path and the legacy
focalLengthPx overload which delegates to it): `xNorm = xSensor / zCam`,
`uSensor = fx·xNorm + skew·yNorm + cx` — a true pinhole division by depth. No linear
(plate-scale) approximation exists anywhere in the projection path.

## Verification (harness, P1 shim set; NEW oracle test `ARProjectionPinholeTest`)

Setup: FALLBACK tier intrinsics via the real `getCameraIntrinsics(null)` (fx=fy=
(1080/2)/tan(63.5°/2) = 871.75 px, 1920×1080 array, sensorOrientation=90), roll-free
synthetic attitude with boresight on the horizon at az 0, targets at az θ (central
angle exactly θ along the sensor horizontal axis), portrait canvas 1080×2400,
displayRotation 0 → FILL_CENTER scale = max(1080/1080, 2400/1920) = 1.25 →
**f_view = 871.75 × 1.25 = 1090.79 px**.
Test asserts radial px = f_view·tan(θ) within 0.5 px at θ = 15°/30°/45° — PASSES
(measured 292.28 / 629.77 / 1090.79 px; run: 6/0/0 in evidence/P2 raw below).

## Error table — what a LINEAR plate-scale model would cost (MODELLED, not in the code)

| off-axis θ | pinhole px | linear px | px error | equivalent sky error |
|---|---|---|---|---|
| 15° | 292.28 | 285.57 | 6.71 | 19.8′ |
| 30° | 629.77 | 571.14 | 58.63 | 141.8′ |
| 45° | 1090.79 | 856.70 | 234.09 | 411.2′ |

(sky error = θ − atan(θ); the marker would land at the pinhole angle atan(θ) instead of θ.)

## Mutation proof

Temporarily replacing Step 4 with the linear model (`xNorm = asin(xSensor)`) makes
`ARProjectionPinholeTest` FAIL at 15° (285.57 vs expected 292.28, the exact 6.71 px
predicted above) — the test detects a linear regression. Reverted; engine byte-identical
to Z-P1 state (git diff empty); suite back to 6/0/0.

KIND: verification-only (MODELLED comparison table; measured pinhole values via
synthetic attitude — SYNTHETIC attitude, real engine). Loosened: none.
