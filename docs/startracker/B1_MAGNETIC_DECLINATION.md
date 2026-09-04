# B1 — Magnetic declination (OD4) findings and implementation, 2026-09-04

## Finding

The compass azimuth entering the AR render chain comes from `TYPE_ROTATION_VECTOR`
(OrientationProvider), whose world frame is referenced to **magnetic north**
(SensorManager.getRotationMatrix contract: world Y → magnetic north). Every sky azimuth
in the app (catalogs, ephemerides, oracle-fixed in F-A) is **true-north** referenced.
Before B1 the app compared true azimuths against a magnetic azimuth — a systematic
pointing error equal to the local declination D (roughly ±3°…±27° at the A2 grid, see
table below; the old `azimuth` doc comment claiming "True north azimuth" was wrong).

## Measured declination table (MEASURED, WMM2025; grid epoch 2026-09-04)

Full table with cross-check: `evidence/DECLINATION_TABLE_2026-09-04.txt`
(pygeomag WMM2025 primary; `geomag` package independent implementation of the same
WMM_2025.COF agrees to ≤ 0.0017° = 0.10′):

| Location | D (deg, E+) | Pointing error removed |
|---|---|---|
| Tehran | +4.966° | 298′ |
| Sydney | +12.825° | 770′ |
| Quito | −5.084° | 305′ |
| Tromsø | +11.242° | 675′ |
| Cape Town | −26.784° | 1607′ |
| Honolulu | +9.273° | 556′ |
| Frankfurt (user base) | +3.769° | 226′ |
| Nurabad (app default) | +3.570° | 214′ |

Offset-rebase decision: **subtract-once** (not clear). A legacy stored yaw calibration
absorbed D (it was measured while the app azimuth was still magnetic-referenced), so at
first sight after upgrade with a GPS fix the stored yaw is reduced by D exactly once,
guarded by the versioned marker `calib_yaw_declination_rebased_v1`
(`ARCalibrationManager.rebaseYawForDeclinationOnce`). Rationale: preserving the user's
calibration beats clearing it; the operation is idempotent via the marker and is a no-op
when no legacy offset exists.

## Implementation (all UNEXECUTED on device; pure parts harness-tested)

- `astro_engine/MagneticDeclination.kt` (NEW, pure): `trueAzimuth(mag, D)` with
  wrap-around, **bit-identical passthrough at D == 0.0** (guardrail contract);
  `rebaseLegacyYawOffset(yaw, D) = yaw - D` (signed domain — feeds R_z, wrap-free);
  `wrap360`.
- `StarTrackerConfig.APPLY_MAGNETIC_DECLINATION = true` (OD4: DEFAULT ON, sibling of
  ENABLED). Guardrails in code: flag off → D = 0.0 → bit-identical azimuths; no GPS
  fix/permission → D = 0.0 → bit-identical; applied exactly once at the single
  true-azimuth entry point.
- `CompassARScreen.kt`: `magneticDeclinationDeg` from
  `android.hardware.GeomagneticField` (WMM, framework-managed; recomputed per location
  change), applied ONLY in the sensor branch of `currentAzimuth` (:398, the single point
  where magnetic azimuth enters). The manual-drag branch is NOT corrected: the user
  aligns the rendered (true-frame) sky against the real sky by eye, so the manual value
  is already a true azimuth. One-time rebase `LaunchedEffect` next to it.
- `ARCalibrationManager.kt`: `rebaseYawForDeclinationOnce(D)` + versioned marker key.

## Tests (harness 148/0/0)

`MagneticDeclinationTest.kt` (7 tests): identity/wrap-around/east/west/out-of-range,
legacy rebase arithmetic, guardrail semantics. Mutation proof (sign flip of D):
4 failures, `evidence/MUTATION_PROOF_B1_2026-09-04.txt`.

## Disclosure

On-device behavior (GeomagneticField value, rebase effect on a real stored calibration)
is UNEXECUTED — no Android runtime in this environment. The pure math and the rebase
arithmetic are harness-verified; wiring follows the single-application-point rule.

---

## RETRACTION (Z-V3, 2026-09-04): the finding below was WRONG — read first

V3's placement audit (evidence/V3_DECLINATION_PLACEMENT_2026-09-04.md) found that
magnetic declination was ALREADY applied at the attitude source before this pass:
OrientationProvider.updateLocation() loads the WMM declination and builds
R_true = R_declination * R_sensor, and skyOrientation.azimuth is derived from that
corrected matrix. The "finding" and table below correctly describe WMM declination
VALUES but the claimed app defect (true-vs-magnetic mismatch) did not exist; the B1
scalar correction double-corrected scalar consumers by +D with GPS active. The B1
wiring (screen block, rebase API, default-on flag) was REVERTED in Z-V3;
MagneticDeclination.kt + tests are retained as pure utilities. The original (wrong)
text follows for the decision trail.
