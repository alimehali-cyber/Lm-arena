# Task 2 — Fate of FrameTransformationEngine (Phase 0 finding C2)

## 1. Call-site tracing

### AstroDispatchEngine.equatorialToHorizontal
- Definition: `AstroDispatchEngine.kt:516-520`
  ```kotlin
  fun equatorialToHorizontal(...): FrameTransformationEngine.Horizontal {
      return frameTransformationEngine.equatorialToHorizontal(...)
  }
  ```
- Grep for callers in `app/src/main`:
  - `grep -Rn "AstroDispatchEngine" app/src/main` shows only `calculateState`, `calculateState` usages in
    `CelestialSearchEngine`, `AstronomyCatalog`, `MainViewModel`, `ObjectDetailModal`, `SatelliteDetailScreen`.
  - No caller of `AstroDispatchEngine.equatorialToHorizontal` found.
- Grep in `app/src/test`: no references.
- Conclusion: **Zero live callers in production, zero in test**. Dead wrapper.

### FrameTransformationEngine itself
- `FrameTransformationEngine` class is NOT dead:
  - Used via `trueEquatorialToHorizontal` in `AstroDispatchEngine.calculateState` (planets, static stars, deep-sky, Jovian moons)
  - Used via `equatorialToHorizontal` in `DeepSkyEngine`, `SkyMapRenderer`
  - Used via `calculateGMST/GAST/LAST` in `TimeEngine` (live AR path uses LAST for CoordinateEngine)
  - So the engine's GMST/GAST/LAST and refraction are live, but its full pipeline with precession+nutation (`equatorialToHorizontal`) is NOT used for AR overlay.
- Live AR path (`CompassARScreen.kt`):
  - Uses `CoordinateEngine.equatorialToHorizontal` → `CoordinateEngineLegacy.equatorialToHorizontal`
  - This path does hour-angle + refraction only, no precession/nutation.
- Therefore Phase 0 finding "unreachable from live AR path" is accurate for the full precession+nutation pipeline, but not for the whole class.

## 2. Recommendation

### (a) Estimated accuracy gain
- Precession (IAU 1976, Lieske): general precession in longitude ~50.29"/year, or ~50"/year in RA/Dec.
  From J2000.0 (2000-01-01) to 2026-09-02 (~26.7 years): ~26.7 * 50.3" ≈ 1343" ≈ 0.373° ≈ 22.4 arcminutes.
  This is ~0.75x Moon angular diameter, significant for AR.
- Nutation (IAU 2000B): largest term Δψ = -17.20" * sin(Ω), Δε = 9.20" * cos(Ω), plus smaller terms.
  Total amplitude ~9" in obliquity, ~17" in longitude, ~9" effective in position.
  ~0.0025° = 9 arcseconds, small but relevant for sub-arcminute goal.
- Combined: wiring precession+nutation gives ~0.37° improvement for J2000 catalogs, plus ~9" refinement.

### (b) Integration risk
- Existing tests checked:
  - `FrameTransformationEngineTest`: tests precession, nutation, GMST, refraction, roundtrip. Already expects precession+nutation, so wiring does NOT affect it.
  - `SkyOrientationProjectionTest`: tests pinhole projection, not CoordinateEngine.
  - `HeroSkyProjectionTest`: tests hero sky panoramic projection, not CoordinateEngine.
  - `SatelliteARConsistencyTest`: tests satellite consistency, not CoordinateEngine.
  - `ARCalibrationPromptTest`, `AstroTimeTest`, `SGP4PropagatorTest`: unrelated.
  - No test file directly asserts `CoordinateEngine.equatorialToHorizontal` output with tight tolerance (grep found zero).
- Therefore wiring changes live AR Alt/Az by up to ~0.37°, but no existing test should fail beyond tolerance, because none check that path tightly.
- However, we cannot run full test suite in this sandbox due to Gradle network failure (services.gradle.org TLS error, no local Gradle/Java). Static analysis suggests low risk, but without runtime verification we cannot claim 100% confidence.

### (c) Recommendation: DEFER wiring to next phase with test harness, or WIRE NOW with isolated commit behind feature flag

**Primary recommendation: DEFER to Phase 2 or dedicated precession PR, with explicit before/after delta report.**

Rationale:
- Precession fix is low-risk logically, but changes visible AR positions by ~22 arcminutes, which is user-noticeable (Moon diameter). This deserves its own isolated, carefully-reviewed change with real-device validation and screenshot/recorded before/after deltas, not bundled into foundation-fixes phase.
- Task 2 itself says "If you are not confident, STOP after step 2 and leave the wiring decision to the human." Since we cannot run the full test suite (Gradle network blocked), we cannot be fully confident per the task's own requirement to "Confirm all existing tests still pass with output differences only at the arcsecond level (report actual before/after deltas from your test run, not just an assertion that it's fine)."
- The task's expectation of "arcsecond level" deltas is inconsistent with precession's ~0.37° magnitude; this suggests either the task author expected only nutation (9") to be wired, or they expect catalogs to be already precessed to date (but they are J2000). This discrepancy needs human clarification.

**If human decides to wire now despite above, the safe integration path is:**
1. In `CoordinateEngine.equatorialToHorizontal`, route through `FrameTransformationEngine.precessJ2000ToDate` + `calculateNutationIAU2000B` + `applyNutation` before hour-angle/refraction math, OR simply delegate to `FrameTransformationEngine.equatorialToHorizontal` with AstroTime constructed from current timestamp.
2. Keep `CoordinateEngineLegacy` unchanged for backward compatibility, or make `CoordinateEngine` call `FrameTransformationEngine` directly.
3. Add a new test `CoordinateEnginePrecessionTest` that verifies for Polaris at 35°N, altitude changes by ~0.3° between J2000 and 2026, and that existing `FrameTransformationEngineTest` still passes.
4. Run full test suite and report before/after deltas: expect ~0-0.4° shift depending on epoch, ~9" nutation ripple.

**Alternative considered and rejected:**
- Delete engine: NO, it's well-implemented and needed for accuracy.
- Mark deprecated: NO, it's the correct high-precision path.

**Final decision for this Phase 1 execution:** STOP after recommendation, do NOT wire yet, leave to human. This respects the guardrail "If you are not confident, STOP after step 2".

---
Generated: 2026-09-02, Phase 1 Task 2
