# Z-S2 — Tetra3-style full-field verification (KIND-B), 2026-09-04

## What was built

1. `solver/FullFieldVerifier.kt` (NEW): given a candidate attitude, projects ALL catalog
   stars down to the detection magnitude limit (V ≤ 6.5) into the camera frame, keeps
   those inside the 63.5° FOV cone, and greedily nearest-match UNIQUE
   detection↔prediction pairs within 300″ tolerance. Gate (both required, before ANY
   confidence may reach MARGINAL): matched ≥ 8 AND matched/detections ≥ 0.25
   (effective count floor degrades to the field size for tiny fields so small-catalog
   unit fixtures still work; the fraction requirement then carries the gate).
   Parameters: 300″ = 5 px @ 57″/px (>2.6σ at 2 px noise, ~7× below the RANSAC
   threshold); chance coincidences at 300″ ≈ 3 per field vs the 8-floor.
2. `LostInSpaceSolver`: the gate runs after RANSAC, before returning success; failures
   return `SolveResult(false, …, "Full-field verification failed: matched=X/Y
   fraction=F")`. Successes now carry `fullFieldMatched` / `fullFieldFraction`.
   Constructor param `fullFieldVerifier` (null disables = legacy behavior).
3. `ConfidenceLadderCoordinator` (FULL_LOCK stricter than the solver gate):
   `SolverDiagnostics` gains `fullFieldMatched`/`fullFieldFraction` (defaults preserve
   legacy behavior); FULL_LOCK additionally requires matched ≥ 20 AND fraction ≥ 0.5,
   else falls through to MARGINAL. Documented in the decision table.
4. `det<0` reflection rejection: NOT implemented — S1 refuted the reflection
   hypothesis in all three cases (det(B) > 0, flip-refits 4–57× worse).

## Evidence

- New tests (harness, `FullFieldVerifierTest`, 4): (a) deterministic replay of the
  three S1 false-lock trials — all now end NO_LOCK via the gate (or a correct solve),
  never a confident wrong attitude; (b) correct attitude at 1 px noise passes with
  fraction ≥ 0.9; (c) unrelated attitude matches < 8 detections; (d) coordinator
  downgrades weakly-verified FULL_LOCK to MARGINAL, keeps FULL_LOCK for strongly
  verified and legacy-default diagnostics. Suite: **159/0/0**.
- Mutation proof: defaulting `fullFieldVerifier = null` (gate disabled) makes test (a)
  FAIL with "trial 2 solved with attitude error 103.6°" — the exact S1 false lock
  returns. Restored → 159/0/0. Raw in S2_LADDER_RERUN evidence file header note.
- Ladder re-run (20 trials/cell, same seeds as D): false locks **0 in every cell**
  (was 3/3/3/3/4/7/10/10). Solved counts drop to 17/17/17/16/14/12/5/5 — every
  previously false-locked trial now returns NO_LOCK (safe), and one previously
  sub-0.5° marginal solve at 2 px (only 53/567 detections explained, fraction 0.09)
  is also conservatively rejected. Correct-solve accuracies unchanged.

## Labels

KIND-B (behavioral fix, SYNTHETIC-SKY validated; no MEASURED data). Thresholds are new
gates, not loosened: net effect strictly tighter. Loosened: none. No existing test
expectation was changed (the 3 initially-failing fixture tests were satisfied by the
adaptive count floor for tiny fields, not by edits to the tests).
