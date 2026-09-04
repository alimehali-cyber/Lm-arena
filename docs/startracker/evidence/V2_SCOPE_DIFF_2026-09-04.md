# Z-V2 — Scope diff pass-3 tip (edef03d) -> current tip, 2026-09-04

`git diff --stat edef03d..HEAD` = 64 files, +17,968/-953. Paths outside startracker/,
tools/, docs/, data/startracker/, tests (production code only):

| # | Path | Commit | KIND | Oracle test | Mutation proof |
|---|---|---|---|---|---|
| 1 | astro_engine/AstroTime.kt | d8a2e1e | KIND-B (ΔT: invented cubic -> Espenak-Meeus piecewise) | CoordinateOracleTest.testDeltaTModernEra; AstroTimeTest (Z-V1 corrected, EM segment) | evidence/ORACLE_TEST_MUTATION_PROOF.txt (ΔT=101.27 s failure vs pre-fix code) |
| 2 | astro_engine/CoordinateEngine.kt | d8a2e1e | KIND-B (precessJ2000EquatorialToDate) | CoordinateOracleTest.testStarRouteAgainstOracle (0.418′ rms vs astropy) | same file (star route 26.5′ max failure vs pre-fix code) |
| 3 | astro_engine/LunarSolarEngine.kt | d8a2e1e | KIND-B (VSOP87 τ units + millennium L1) | LunarSolarEngineTest Meeus Ch.25 worked example (Z-V1); oracle sun route 0.350′ | evidence/ORACLE_TEST_MUTATION_PROOF.txt (saturn 527′ covers the shared VSOP87 assembly; sun was 73.4′ before) |
| 4 | astro_engine/VSOP87Engine.kt | d8a2e1e | KIND-B (tables regenerated from VSOP87D) | VSOP87EngineTest Earth-helio vs astropy (Z-V1); oracle planet routes 0.4–0.6′ | evidence/ORACLE_TEST_MUTATION_PROOF.txt (planet_saturn 527.04′ failure) |
| 5 | data/catalog/StarCatalog.kt, DeepSkyCatalog.kt | 60155a3 | values-only corrections (OD3 rule: >60″ AND two sources agree <5″) | B3_CATALOG_VERIFICATION.md per-object table (HYG v3.6 + BSC5 + OpenNGC/NED); DeepSkyEngineTest SMC assertion (Z-V1); oracle DSO route 0.417′ | none (data correction with before/after table per object — the OD3 rule's evidence form) |
| 6 | astro_engine/MagneticDeclination.kt (NEW) | 43e360e | pure-math utility (values from WMM2025 tables, cross-checked ≤0.0017°) | MagneticDeclinationTest (7) | evidence/MUTATION_PROOF_B1_2026-09-04.txt (sign flip = 4 failures) |
| 7 | astro_engine/ARCalibrationManager.kt | 43e360e | B1 one-time rebase API — ⚠ premise REFUTED by Z-V3 (declination already applied at attitude source) → reverted in Z-V3 | rebase arithmetic inside MagneticDeclinationTest | via B1 proof; moot after Z-V3 revert |
| 8 | astro_engine/OrientationProvider.kt | a3a1ca0 | KIND-A-equivalent (B4: dedicated sensorTimestampNanos StateFlow; dormant while ENABLED=false) | NONE possible offline (Android file) — UNEXECUTED, disclosed in commit + B4 doc | none — disclosed as UNEXECUTED |
| 9 | ui/screens/CompassARScreen.kt | d8a2e1e + 43e360e + a3a1ca0 + 1ec3406 | mixed: precession call sites (KIND-B, mirrored by oracle test), B1 declination block (premise refuted → reverted Z-V3), B4 collector, F2 camera gate (flag-gated) | file not harness-compilable — UNEXECUTED; the precession helper mirrors CoordinateEngine (oracle-pinned) | n/a — disclosed |

Authorisation note: engine files (#1–#4) were forbidden territory for ten phases; the
final-pass authorisation was A5 (KIND-B vs astropy) — each carries an oracle test AND a
mutation proof as required, EXCEPT #8/#9 which are Android-only and therefore carry the
UNEXECUTED disclosure instead (nothing better is possible in this environment).
Also touched (context rows): .gitignore (raw-catalog ignore rule, b0ee15a) and the six
test files listed in the diff (all part of tests).
