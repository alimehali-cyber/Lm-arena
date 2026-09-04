# Z-P3 — 63.5° tier geometry, FILL_CENTER consistency, edge scale error (2026-09-04)

All numbers MEASURED through the real engine in the harness (P1 shim set, probes
`P3FillCenterProbe.kt` / `P3CornerScaleProbe.kt`, run against the projection build);
setup identical to Z-P2: FALLBACK tier (fx=fy=872.63 px on 1920×1080 array,
sensorOrientation 90), canvas 1080×2400, displayRotation 0 → netRotation 90.

## Is 63.5° H, V or diagonal?

It is the SENSOR-ARRAY SHORT-SIDE FOV, by construction: fy = (1080/2)/tan(63.5°/2)
on the 1080-row (landscape-vertical) axis. It is NOT the view FOV and not diagonal.
Sensor pre-crop FOVs: short side 63.50°, long side 95.52°, diagonal 103.28°.

## FILL_CENTER consistency — engine matches PreviewView semantics exactly

Engine: `scale = max(canvasW/wRot, canvasH/hRot)` with netRotation 90 →
wRot=1080, hRot=1920 → scale = max(1.000, 1.25) = 1.25. This is exactly
PreviewView.ScaleType.FILL_CENTER (fill entirely, preserve aspect, center-crop).
Arithmetic: post-rotation sensor width 1080 px → 1350 view px, cropped to 1080
(216 px = 20% of the rotated width cropped, 108 px per side); height 1920 → 2400
exact, no crop. Resulting VIEW FOVs: **H 52.68°, V 95.46°, diagonal 100.70°**
(view corners at central angle 50.35° < sensor half-diagonal 51.64° → all four view
corners lie inside the sensor image; no black corners, no distortion).
Verified end-to-end: the sky point az 26.3379° / alt −44.5938° projects to exactly
(1080.00, 2400.00) — the bottom-right view corner — at central angle 50.344°
(theory 50.351°, residual = search tolerance).

## Edge scale error (gnomonic stretch, measured px/deg vs center 19.038)

| location | θ from boresight | measured px/deg | theory f·sec²θ / f·secθ | vs center |
|---|---|---|---|---|
| center | 0° | 19.038 | 19.038 | 1.000 |
| horizontal view edge | 26.34° | 23.705 | 23.704 (sec²) | +24.5% |
| vertical view edge | 47.75° | 42.112 | 42.112 (sec²) | +121.2% |
| corner, radial dir | 50.34° | 45.787 | 46.758 (sec²) | +140.5% |
| corner, tangential dir | 50.34° | 29.753 | 29.836 (sec) | +56.3% |

(corner radial 0.979 ratio = finite-difference averaging over ±1.15° in a sec² field;
tangential agrees to 0.3%.) This stretch is the CORRECT overlay behavior: the camera's
own image is a gnomonic projection, so the overlay must stretch identically to stay
glued to the feed.

## Verdict: KIND-B fix NOT warranted

No provable defect: the FILL_CENTER arithmetic is internally consistent and matches
CameraX PreviewView semantics; the 63.5° figure is a sensor-tier parameter (its name
does not claim to be the view FOV). The one documentation hazard — reading 63.5° as
the on-screen horizontal FOV (actual: 52.68° on 1080×2400) — is resolved by this
evidence note, not a code change. UNEXECUTED on device (real PreviewView/Bitmap path);
runbook keeps a 30/60° off-centre overlay-vs-bright-star check.
