# C — Capped quad index (final pass), 2026-09-04

## Problem

`QuadPatternIndex`'s only construction path was an O(N⁴) brute force with all-pairs
enumeration — measured infeasible beyond ~800 stars (OOM; prior evidence). The real
catalog (E, 8,870 stars) needs a bounded builder: **the capped quad index was the
missing piece** of the star-tracker chain.

## Implementation (machinery, no data)

`QuadPatternIndex.capped(stars, maxMagnitudeForQuads, neighborsPerStar, maxQuads, ...)`
(secondary opt-in path; the legacy brute-force constructor is untouched and still
bit-identical — guarded by a hand-rolled O(N⁴) spot-check test):

1. Quad-eligible = brightest stars (mag ≤ cut), ordered (magnitude, id) → deterministic.
2. O(E²) neighbor pass among eligible stars; each anchor keeps its K NEAREST neighbors
   (tie-break catalog index).
3. 4-combinations anchor+3 neighbors with all six separations in [0.1°, 40°];
   canonicalized by sorted indices and deduplicated globally (60-bit packed key,
   catalogs < 32,768 stars enforced).
4. Deterministic hard ceiling on total quads (brightest anchors first).

New `CatalogBuildConfig` constants (all UNVALIDATED pending D):
`QUAD_BUILD_MAX_MAGNITUDE = 5.5`, `QUAD_NEIGHBORS_PER_STAR = 6`, `QUAD_MAX_QUADS = 120_000`.

## C4 — index-size sweep on the REAL catalog (MEASURED)

`tools/kotlin-harness/CatalogSizeProbe.kt cappedcsv <csv> <mag> <K>`; serialized size =
stars + quads sections only (runtime solver consumes quads; the 78 MB pair index is
build-time scaffolding and ships as 0 pairs).

| mag ≤ | K | eligible | quads | hash keys | build | size (B) |
|---|---|---|---|---|---|---|
| 4.5 | 5 | 919 | 6,522 | 6,487 | 0.25 s | 956,492 |
| 4.5 | 6 | 919 | 13,022 | 12,940 | 0.27 s | 1,476,000 |
| 5.0 | 6 | 1,625 | 22,604 | 22,348 | 0.52 s | 2,241,922 |
| **5.5** | **6** | **2,848** | **39,960** | **39,393** | **1.3 s** | **3,629,280** |
| 5.5 | 8 | 2,848 | 109,900 | 107,098 | 1.6 s | 9,219,706 |
| 6.0 | 6 | 5,041 | 70,754 | 69,411 | 3.3 s | 6,090,550 |
| 5.5 | 10 | 2,848 | 232,750 | 222,969 | 2.0 s | 19,038,911 |
| 6.5 | 6 | 8,870 | 125,416 | 122,292 | 9.9 s | 10,460,793 |

Bytes/quad = 79 across the sweep.

## C5 — chosen defaults and rationale

`mag ≤ 5.5, K = 6, ceiling 120k`:
- 3.63 MB total asset — half of the A1 model budget (7.3 MB @ f=0.07272, k=5), leaving
  headroom for a larger detection catalog without an asset-size crisis.
- 1.3 s build on this sandbox's JVM — trivially offline-buildable.
- K=6 ⇒ per anchor ≤ C(6,3) = 20 quads before dedupe — dense enough that 91%+ of
  eligible stars appear in ≥ 1 quad on the real catalog (test-asserted ≥ 90%).
- The full V ≤ 6.5 catalog still ships (star section 435 KB) for tracking/verification;
  only quad BUILDING is restricted to mag ≤ 5.5.
- FINAL TUNING AUTHORITY: item D's synthetic-E2E solve-success/false-lock curves. If D
  shows sparse-field solve failures at these defaults, raise K first (quads grow ~K³:
  K=8 → 9.2 MB still fits).

## Tests (harness 154/0/0)

`CappedQuadIndexTest` (6 tests): full-budget equivalence with brute force (exact set +
descriptors), magnitude filter, neighbor cap + determinism + separation bounds, ceiling
respect + determinism, legacy brute-force constructor unchanged (hand-rolled O(N⁴)
cross-check), real-catalog 8,870-star build feasibility + ≥90% eligible-star coverage.

**Mutation proofs** (evidence/MUTATION_PROOF_C_2026-09-04.txt):
- combo min-separation check dropped → equivalence fails (quad set diverges, e.g.
  includes the injected 0.05°-separated pair)
- dedupe dropped → "no duplicates allowed" fails (296 vs 74 quads at N=43 fixture)
- (neighbor-filter min-sep drop alone is unobservable — the combo check is the
  authoritative guard; noted, not separately provable)
