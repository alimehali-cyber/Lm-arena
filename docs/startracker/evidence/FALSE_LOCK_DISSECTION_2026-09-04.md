# Z-S1 — False-lock dissection (zero-noise cell, 3 cases), 2026-09-04

Probe: `tools/kotlin-harness/probes/FalseLockDissectionProbe.kt` replays the D-ladder
cell 0 px / 0 false stars EXACTLY (seed 20260904, 20 attitudes, observer seed 1000+t),
catches every solve with attitude error > 0.5°, and instruments quad matching,
correspondences (against ground truth), RANSAC, residuals, and reflection hypotheses.
Raw output: `evidence/S1_FALSE_LOCK_RAW_2026-09-04.txt`. All numbers SYNTHETIC-SKY.

## Summary table

| trial | att. error | solver inliers | conf | quad matches (true/false) | winning corr true? | inlier residuals under returned attitude | rms | det(B) | flips rms |
|---|---|---|---|---|---|---|---|---|---|
| 2 | 103.62° | 4 | 0.169 | 3 (1 TRUE + 2 false) | 0/4 TRUE | 809–1054″ | 930.0″ | **+0.0002 (proper)** | 13,059″ (≫930) |
| 14 | 122.73° | 4 | 0.503 | 1 (0 true) | 0/4 TRUE | 192–441″ | 343.5″ | **+0.0006 (proper)** | 19,669″ (≫343) |
| 18 | 82.41° | 4 | 0.115 | 7 (0 true) | 0/4 TRUE | 533–1030″ | 740.4″ | **+0.0001 (proper)** | 3,985″ (≫740) |

## Mechanism (same in all 3)

1. **Chance descriptor collision, not reflection.** The 5-ratio descriptor
   (sorted, scale/permutation-invariant) of an observed bright quad lands within the
   hash + neighbor-bin tolerance of a DIFFERENT catalog quad: descriptor L∞ distance
   0.0147/0.0138 (t2), 0.0167 (t14), 0.0109–0.0189 (t18) vs exact 0.0000 for the one
   true match (t2 match[2]). The 6-separation pyramid check does not discriminate:
   with max-separation ≈ 0.12–0.30 rad, an L∞ ratio error ~0.014 maps to separation
   errors ≲ 0.004 rad (~800″), inside PYRAMID_CONSISTENCY_TOLERANCE.
2. **RANSAC locks onto the false quad.** The false quad supplies 4 mutually
   (approximately) consistent correspondences; a TRIAD rotation from any 2 of them maps
   all 4 within the loose inlier threshold (0.01 rad = 2062.6″) — residuals 192–1054″,
   so 4 inliers ≥ minStarsForSolve=4 and the solve SUCCEEDS with a wrong attitude.
3. **True correspondences are outliered or absent.** t2: the TRUE quad's 4 exact
   correspondences (residual 0.0″ under truth) lose to the false 4+4 (two false quads
   = 8 wrong correspondences crowd the pool; RANSAC seed 42 picks the false pair first
   and 4 inliers suffice). t14/t18: NO true quad match exists at all — the true field's
   bright-star quads weren't matched (capped index/mag 5.5 coverage or candidate
   formation), so only false hypotheses were available.

## Reflection verdict: REFUTED (all 3 cases)

- det(B) of the Wahba cross-covariance over the winning inlier set is POSITIVE
  (+0.0002, +0.0006, +0.0001) — the winning correspondence set is fit by a PROPER
  rotation (no mirror).
- Refitting after flipping one axis of the observed vectors (x/y/z) gives rms
  13,059″ / 19,669″ / 3,985″ — 4–57× WORSE than the identity fit (930″/343″/740″).
  A mirror-image match would show the opposite (flip rms ≈ 0).
- The matched catalog stars are also not a mirrored geometry of the true field: each
  false quad is simply a different, real catalog asterism with a similar ratio signature.
=> S2's conditional "det<0 rejection if S1 confirms reflection" is NOT activated; the
S2 fix must instead be **residual/full-field verification** (the winning sets' residuals
192–1054″ are 2–3 orders above the zero-noise correct-solve residuals ~0″, and a
full-field projection check would find <4 catalog stars matching detections).

## Exploitable signals for S2/S3

- Every false lock has exactly 4 inliers (the bare minimum) with LARGE rms residuals
  (≥192″ even at zero noise), while correct solves at ≤1 px noise have ≤~60″ residuals
  and many more inliers — a residual gate plus full-field star-count verification
  separates them cleanly.
- Confidence values 0.115–0.503 — all below a FULL_LOCK-grade threshold; t14 (0.503)
  shows confidence alone is not a safe gate at MARGINAL level.
