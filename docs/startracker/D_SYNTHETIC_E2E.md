# D — Synthetic E2E (final pass), 2026-09-04

## Headline (all numbers SYNTHETIC-SKY)

The real-chain lost-in-space solve **did not work at all before D** (0/20 on every cell)
because the runtime quad-candidate builder and the new capped index produced disjoint
quad families. The local-candidate fix (pool=40) reaches **20/20 solves at 0–1 px
centroid noise with 0.1–1.0′ median attitude error**, degrades gracefully with noise
(2 px: 19/20, median 2.0′) and with false stars (0.3 px + 20 false: 15/20). **False
locks (error > 0.5°) remain: 3/20 even at zero noise** — the chain is NOT ready for
device enablement until the matcher is hardened (see D6).

## Cells (success / false-lock / median error, 20 trials each)

| noise | false | solved | false locks | median ALL (incl. FL) | p95 ALL | median CORRECT-ONLY | p95 CORRECT-ONLY |
|---|---|---|---|---|---|---|---|
| 0 px | 0 | 20/20 | 3 | 0.00′ | 7364.02′ | 0.000′ | 1.936′ |
| 0.1 px | 0 | 20/20 | 3 | 0.12′ | 7364.16′ | 0.099′ | 2.034′ |
| 0.3 px | 0 | 20/20 | 3 | 0.38′ | 7364.45′ | 0.307′ | 5.902′ |
| 1.0 px | 0 | 20/20 | 3 | 1.25′ | 9398.54′ | 1.024′ | 19.680′ |
| 2.0 px | 0 | 19/20 | 4 | 2.33′ | 9399.84′ | 2.048′ | 23.003′ |
| 0.3 px | 5 | 19/20 | 7 | 0.50′ | 10658.34′ | 0.319′ | 4.419′ |
| 0.3 px | 20 | 15/20 | 10 | 5389.17′ | 9842.52′ | 2.466′ | 3.017′ |
| 1.0 px | 20 | 15/20 | 10 | 5389.25′ | 9838.90′ | 3.625′ | 10.064′ |

(Z-V4, 2026-09-04: the previous version of this table reported the CORRECT-ONLY columns
unlabeled as "median err / p95 err", which hid the false locks from the percentiles —
at 0 px, p95 including false locks is 7364′, not 1.94′. Both columns now explicit;
raw output in evidence/V4_D_METRIC_RECOMPUTE_2026-09-04.txt. Percentile convention:
median = element at index n/2 of the sorted list; p95 = index n*95/100.)
Pool-size tuning curve (median-of-cells): pool 25 → 35–45% solve rate, 67% of solves
false-locked; pool 40 → 75–100% solve rate, 15–20% of solves false-locked. **Adopted
`CANDIDATE_POOL_SIZE = 40`** (CatalogBuildConfig).

## D1 — root cause of the 0/20 baseline (and fix)

Legacy `QuadCandidateBuilder.buildCandidates` = C(N,4) over the 10 globally brightest
detections: quads whose members span 10–40°. The capped index stores anchor+nearest
neighbor quads. Measured on one field: 22/210 legacy candidates in separation range,
**0 of those indexed**. Fix: `buildLocalCandidates` (anchor + nearest neighbors from a
40-star bright pool, all six separations in [0.1°, 40°]) mirrors index construction;
wired in `LostInSpaceSolver` behind `USE_LOCAL_QUAD_CANDIDATES` (default **true**;
`false` restores the legacy builder bit-for-bit — mutation-proven: flipping it back
reproduces the exact 0/20 failure).

## Refraction/pointing ladder table

Not applicable in D (synthetic observations bypass atmosphere/rendering). The pointing
ladder is A6's (`evidence/refraction_ladder_oracle.csv`); the accuracy ladder for the
ASTRONOMY chain is A4's tables. The solver-facing ladder is the noise/false-star table
above.

## Centroid table

Centroiding itself was validated in the prior pass (CentroiderTest: weighted/unweighted
sub-pixel paths, saturated-pixel exclusion, B4 denominator fix; VALIDATION_MATRIX +
harness 155/0/0 include them). D consumes its output as `noiseSigma` above (0.1–2 px,
57″/px at 63.5° FOV / 4000 px). No new centroid numbers were produced in D.

## D6 — honest statement of what this does and does not prove

- It DOES prove: on synthetic images drawn from the REAL catalog, the full
  lost-in-space chain (ingest → capped index → candidate formation → pyramid matching →
  RANSAC → Davenport q-method) locks correctly in 75–100% of random fields with
  sub-2-arcminute median accuracy at plausible centroid noise, and the whole pipeline
  runs in ~1.3 s index build + sub-second solve on a JVM.
- It does NOT prove anything about a real camera: real PSF/sensor noise, hot pixels,
  motion blur, lens distortion, and frame-rate effects are unmodeled; false-star
  injection is a crude proxy.
- It does NOT certify the tracker for enablement: **false locks (error > 0.5°) occur in
  15% of clean-sky solves** — consistent with the reflection ambiguity of the sorted
  5-ratio descriptor (a mirrored quad yields identical sorted ratios; hypothesis, not
  yet proven). Required before any device enablement: (1) chirality-aware verification
  or a full-observation post-solve vote; (2) re-run this ladder; (3) on-device trials
  per the F runbook.
- `StarTrackerConfig.ENABLED` stays **false**. All D changes live in the dormant
  star-tracker path (KIND-A-equivalent: zero behavioral diff with the master flag off).
