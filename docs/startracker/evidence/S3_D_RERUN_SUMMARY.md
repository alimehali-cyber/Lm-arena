# Z-S3 — D re-run with statistical power + S2 gate (2026-09-04)

Setup: real chain (HYG 8,870 → capped index C-defaults → SyntheticSkyObserver →
LostInSpaceSolver with FullFieldVerifier). Seeds disjoint from all tuning runs
(D/S1/S2: 20260904/1000+t; S3: attitudes 777001–777004, observers 5xxxxx/53xxxx).
False lock definition: solve success AND attitude error > 0.5° (any success carries
≥4 inliers → ≥ MARGINAL_LOCK by the ladder; FULL_LOCK-grade additionally conf ≥ 0.7
and fullField ≥ 20). All percentiles INCLUDE false locks. Solve time = solver only.

## Results (SYNTHETIC-SKY)

| condition | n | solved | FL | FL% | median | p95 | ms/solve |
|---|---|---|---|---|---|---|---|
| baseline 0.3px/0f | 1,000 | 727 (72.7%) | 0 | 0.000% | 0.321′ | 3.51′ | 35 |
| noise 0px | 1,000 | 750 | 0 | 0.000% | 0.000′ | 0.00′ | 36 |
| noise 0.1px | 1,000 | 752 | 0 | 0.000% | 0.124′ | 2.09′ | 35 |
| noise 0.3px | 1,000 | 754 | 0 | 0.000% | 0.365′ | 4.26′ | 35 |
| noise 0.5px | 1,000 | 749 | 0 | 0.000% | 0.601′ | 6.00′ | 35 |
| noise 1px | 1,000 | 722 | 0 | 0.000% | 1.137′ | 8.98′ | 35 |
| noise 2px | 1,000 | 656 | 0 | 0.000% | 2.063′ | 11.64′ | 38 |
| noise 3px | 1,000 | 554 | 0 | 0.000% | 3.039′ | 12.54′ | 37 |
| false 0f @0.3px | 1,000 | 735 | 0 | 0.000% | 0.343′ | 4.01′ | 34 |
| false 5f | 1,000 | 586 | 0 | 0.000% | 0.425′ | 3.95′ | 33 |
| false 10f | 1,000 | 426 | 0 | 0.000% | 0.573′ | 3.87′ | 33 |
| false 20f | 1,000 | 199 | 0 | 0.000% | 0.777′ | 4.25′ | 34 |
| false 40f | 1,000 | 30 | 0 | 0.000% | 1.781′ | 4.62′ | 34 |
| **JOINT MC** | **10,000** | **4,817 (48.2%)** | **0** | **0.000%** | 1.208′ | 9.80′ | 38 |

FULL_LOCK-grade false locks: **0** everywhere (also 0 in every one-factor cell).

## Gate cost (what the solved-rate drop buys)

No-gate comparison on the same 1,000 baseline trials: 924 pre-gate "solves" =
**727 correct + 197 false locks** (every false lock: exactly 4 inliers, errors
52–177°, confidence 0.085–0.504). The gate rejects precisely that population; zero
correct solves lost at 0.3 px. The honest correct-solve rate is 72.7%, not the
pre-gate illusion of 92.4%.

## D3 ladder calibration table (confidence bins over joint MC solves)

| solver confidence | n | FL rate |
|---|---|---|
| [0.0, 0.30) | 2,522 | 0.0000 |
| [0.30, 0.50) | 1,445 | 0.0000 |
| [0.50, 0.70) | 850 | 0.0000 |
| [0.70, 1.0] | 0 (joint) | n/a |

Calibration facts: (a) with the gate active, no confidence bin contains ANY false
lock — the ladder's ordering input is clean; (b) conf ≥ 0.7 (FULL_LOCK's confidence
threshold) requires inlier-ratio ≥ 0.7 AND inliers/observations ≥ 0.6, which
contaminated fields never reach — in the joint MC the maximum observed confidence was
< 0.7, i.e. FULL_LOCK is self-limiting to clean fields. (c) solve-time budget ~35 ms
(JVM, i7-class sandbox) — comfortably real-time on-device at reduced frame rates.

## ACCEPTANCE (S3): PASSED

- Joint MC false locks: **0 / 10,000 = 0.000% < 0.1%** ✔
- FULL_LOCK-grade false locks: **0** ✔
- ⇒ the report MAY say "ready for device trial" (with the solved-rate and
  false-star-degradation caveats below). `StarTrackerConfig.ENABLED` remains **false**
  (verified) — device trial requires the W3 protocol, flag flip is a deliberate
  operator action.

## Remaining weaknesses (flagged, not blockers)

- Solve success degrades steeply with false stars: 73.5% → 19.9% at 20 false stars
  (candidate formation/matcher gets dominated; future work: ranked candidates +
  verified re-search as in Tetra3's multi-hypothesis loop).
- ~2.7% of clean-field trials still end NO_LOCK from "RANSAC inliers < 4" /
  no-quad-match — same family.
- All numbers SYNTHETIC-SKY; no MEASURED device data.


## ADDENDUM (Z-W1, 2026-09-04): re-run on FINAL code (W1 refit + verification confidence)

Same disjoint seeds; accuracy improved ~16x median / up to 70x p95; solved rates
identical (+/-1 trial); FL 0 everywhere incl. joint 0/10,000 (0.000%) and 0
FULL_LOCK-grade; solve time 35 -> ~50 ms. Headline joint numbers now: solved 4,817,
median 0.066', p95 1.15'. ACCEPTANCE re-verified. Full table:
evidence/W1_PIPELINE_2026-09-04.md + raw evidence/S3_RERUN_FINAL_2026-09-04.txt.
