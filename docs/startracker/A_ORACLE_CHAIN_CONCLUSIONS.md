# F-A — Oracle chain conclusions (final pass, 2026-09-04)

Work order item A (oracle chain). Everything below is MEASURED against an independent
astropy 8.0.1 oracle (tools/oracle/astropy_oracle.py, IERS offline, builtin ephemeris)
driving a deterministic grid:

- **A2 grid**: 6 locations × 6 instants (2000-01-01, 2015-12-31T23:59:59, 2026-03-20T12:00,
  2026-07-15T02:30, 2026-09-04T20:00, 2030-06-21T00:00 UTC) — Tehran(35.69,51.39,1200m),
  Sydney(−33.87,151.21,40m), Quito(0,−78.5,2850m), Tromsø(69.65,18.96,10m),
  CapeTown(−33.93,18.42,25m), Honolulu(21.31,−157.86,5m).
- **A4 acceptance**: stars/DSO < 2′, Sun < 1′, Moon < 3′, planets < 2′ on-sky (alt > 10°,
  app-weather refraction oracle column).

## A1–A6 status

| Item | Status | Conclusion |
|---|---|---|
| A1 quad model | DONE (prior) | f=0.07272, k=5 → 7.3 MB |
| A2 grid | DONE | 6×6 above; bodies below horizon at some cells by construction |
| A3 oracle harness | DONE | astropy_oracle.py + CoordinateOracleProbe.kt + build_probe.sh (rebuildable) |
| A4 residuals | DONE | all routes FIXED (KIND-B), tables below |
| A5 permanent test | DONE | `CoordinateOracleTest.kt` (3 tests), mutation-proofed, in run_tests.sh |
| A6 refraction chain | DONE | consolidation changed NO numbers (proof below) |

## A4 residual tables — before → after (arcmin, alt > 10°, app-weather refraction oracle)

Post-fix evidence: `evidence/ORACLE_RESIDUALS_AFTER_FIX.txt`, raw cases
`evidence/ORACLE_CASES.csv` (stars/DSO n=878 rows, 651 above 10°) and
`evidence/ORACLE_CASES_BODIES.csv` (bodies, 360 rows). Pre-fix:
`evidence/ORACLE_RESIDUALS_BEFORE_FIX.txt`, `evidence/ORACLE_CASES_BEFORE_FIX.csv`.

| Route | n | before rms/max | after rms/max | acceptance |
|---|---|---|---|---|
| star_live (AR overlay + trail) | 651 | 15.4 / 26.1 (median 17.5′ @2026) | **0.418 / 1.681** | < 2′ PASS |
| star_live vs refr1010 column | 651 | — | 0.313 / 0.714 | (model cross-check) |
| DSO (galaxies/nebulae/clusters) | 227 | same defect as stars | **0.417 / 1.795** | < 2′ PASS |
| fte (FrameTransformationEngine route) | 651 | — | **0.291 / 0.466** (vs refr1010) | < 2′ PASS |
| sun | 14 | 73.414 / 125.210 | **0.350 / 0.794** | < 1′ PASS |
| moon (topocentric, live) | 16 | ~2.9 / ~5 (ΔT-era) | **0.145 / 0.264** | < 3′ PASS |
| mercury | 4 | 1223.58 / 2072.80 | **0.537 / 1.067** | < 2′ PASS |
| venus | 12 | 69.87 / 107.55 | **0.417 / 0.760** | < 2′ PASS |
| mars | 6 | 693.57 / 1120.31 | **0.450 / 0.789** | < 2′ PASS |
| jupiter | 12 | 346.49 / 434.25 | **0.603 / 1.179** | < 2′ PASS |
| saturn | 12 | 416.25 / 533.03 | **0.557 / 0.943** | < 2′ PASS |
| uranus | 12 | 141.32 / 176.87 | **0.399 / 0.781** | < 2′ PASS |
| neptune | 12 | 76.43 / 86.26 | **0.440 / 0.814** | < 2′ PASS |
| star_live vs NO-refraction oracle | 651 | — | 2.128 / 5.353 | expected: app adds refraction |

### Root causes fixed (all KIND-B: pure correction vs independent oracle)

1. **Sun — VSOP87 time-units bug** (`LunarSolarEngine.kt`): series evaluated with τ in
   Julian centuries instead of 100-century units (phases at 1/10 speed) AND the L1 constant
   was per-century (62833196674.7) in a per-millennium table. Fixed: τ = jcTt/10, L1 =
   628331966747.09 (Meeus Table 32.A). Verified ≤ 0.36″ vs pymeeus full VSOP87D over
   1992–2030; Meeus Ch.25 worked example 199.90737 vs book 199.90895 (0.06′).
2. **Planets — hand-mangled VSOP87D tables** (`VSOP87Engine.kt`): regenerated from pymeeus'
   embedded VSOP87D (A/10^level, C/10, Double literals; 1351 terms; self-check vs pymeeus:
   worst 28.5″ L/B, 1.9 mAU R — millennium-scale truncation, documented). Regenerator:
   `tools/oracle/gen_vsop87_tables.py`.
3. **Stars/DSO — missing J2000→of-date precession** (`CoordinateEngine.kt` +
   `CompassARScreen.kt`): raw J2000 catalog coordinates were fed to the of-date LAST
   transform. Added `precessJ2000EquatorialToDate` (mean-of-date; nutation intentionally
   omitted, ≤ 17″ < 2× acceptance) and `staticObjectEquatorial(obj, jd)` applied at all 4
   call sites (overlay :949, hittest :2565, trail loops :779/:789; jd :487).
4. **Moon — invented ΔT cubic** (`AstroTime.kt`): removed (gave 96.3 s @2025 vs real
   ~69 s). Replaced with genuine Espenak–Meeus piecewise (2000–2005, 2005–2050);
   ~74 s @2025, ≤ 6 s residual → ≤ 0.05′ Moon.

## A5 — permanent oracle test

`app/src/test/java/com/alijafari/red/astronomy/CoordinateOracleTest.kt` (3 tests), wired
into `tools/kotlin-harness/run_tests.sh` (harness now 155/0/0 — the earlier '141' was a hand-tally artifact, machine count at the pass-3 tip is 138, see evidence/V5_TEST_COUNT_RECONCILIATION_2026-09-04.md;
`evidence/HARNESS_FINAL_RUN_2026-09-04.txt`). Recomputes the LIVE call sequence (same as
CompassARScreen / AstroDispatchEngine) and asserts vs the frozen oracle CSVs.

**Mutation proof (G2)** — run against the pre-fix engine code (HEAD = branch base, fixes
uncommitted at proof time): all 3 tests FAIL with the expected magnitudes
(`evidence/ORACLE_TEST_MUTATION_PROOF.txt`):

```
testStarRouteAgainstOracle: [FAILURE] star_live recomputed vs astropy(app-weather refr):
  max=26.546' rms=15.473' exceeds 2.0'
testBodiesAgainstOracle: [FAILURE] planet_saturn at CapeTown/2026-09-04T20:00:00:
  527.0399215087408' exceeds 2.0'
testDeltaTModernEra: [FAILURE] ΔT(2026) = 101.269807826489s, expected ≈69-76s
```

## A6 — refraction chain (base-commit equality check)

`git diff 60928ba HEAD` on the two consolidation-touched files: FrameTransformationEngine
and CoordinateEngineLegacy changes are **comments/variable-renames only** — the refraction
formulas are numerically unchanged. Executed proof: 60928ba `applyRefraction` ladder vs
HEAD ladder, identical to all printed digits (0° 34.477534′, 2° 18.216076′, 5° 9.883144′,
10° 5.391505′, 20° 2.703411′, 45° 0.994848′, 89° 0.016089′). Legacy inline refraction is
Sæmundsson 1986 (1.02/tan(h+10.3/(h+5.11)) with pressure/temperature from elevation) —
the earlier "Bennett" label was a comment bug, not a numeric bug. Ladder vs astropy
(5–45°, where astropy is trustworthy): no refraction defect claim. Full ladder:
`evidence/refraction_ladder_oracle.csv`.

## Report-only residuals (filed, NOT fixed — outside KIND-B provability)

- **Apparent-vs-geometric frame**: no-refr route residual 2.128/5.353′ rms/max = app omits
  aberration+nutation (apparent↔geometric of-date). Fixing would change the refraction
  baseline too — report only.
- **GMST IAU-1982 vs 2006**: −0.045/+0.799/+5.355/−1.270/+1.196/+0.134″ (2000–2030) →
  ≤ 0.09′ azimuth. Report only.
- **FTE.applyRefraction true-altitude nuance**: Bennett formula is nominally a function of
  APPARENT altitude; applying at true altitude is the standard Meeus shortcut, ≤ 0.1′
  difference above 5°. Report only.
- **moon_geocentric diagnostic**: 48.6/61.9′ vs oracle = lunar parallax ∼ 1° scale —
  confirms the live topocentric route is the correct one. Report only.
- **astropy refraction < 3–5° unreliable**: acceptance window starts at 10°; the 0°-row
  ladder numbers are informational only.

## Tooling notes (copy-bug fixed this pass)

`astropy_oracle.py` `table()` previously read the `az1/alt1` (star_live) columns for EVERY
star table, so the "ROUTE fte" line was silently re-measuring star_live (both printed
0.313/0.714). Fixed with an explicit kotlin-column parameter; real fte-vs-refr1010 =
0.291/0.466. ORACLE_CASES*.csv regenerated after the fix (deterministic grid).
