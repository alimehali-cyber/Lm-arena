package com.alijafari.red.astronomy.startracker.catalog

import kotlin.math.PI

/**
 * Catalog build configuration — named constants with conservative defaults.
 * Every constant that depends on centroiding precision is marked as UNVALIDATED pending real test execution.
 * This is critical because Phase 2's detection module has NEVER been executed (no JVM available in Phases 1-2).
 *
 * Constants are split into:
 * - Mathematically fixed (e.g., conversion factors, not tunable)
 * - Conservative defaults, unvalidated pending real centroiding data (must be tuned once real accuracy known)
 */
object CatalogBuildConfig {

    // ===== Mathematically fixed =====

    /** Degrees to radians conversion, mathematically fixed */
    const val DEG_TO_RAD: Double = PI / 180.0

    /** Radians to degrees, mathematically fixed */
    const val RAD_TO_DEG: Double = 180.0 / PI

    /** Full circle in radians, mathematically fixed */
    const val TWO_PI: Double = 2.0 * PI

    // ===== Conservative defaults, UNVALIDATED pending real centroiding data =====

    /**
     * Maximum pairwise angular separation to include in pair index.
     * Default: 40° (0.698 rad).
     *
     * Reasoning (documented):
     * - Phone camera FOV ~60-70° diagonal, ~55° horizontal (Phase 1 consolidated fallback 63.5° vertical).
     * - Quad formed from 4 stars within FOV will have max separation <= FOV diagonal, so 40° cutoff captures
     *   most quads that could fit in a single frame while limiting combinatorial explosion.
     * - Larger cutoff (e.g., 60°) would include more pairs but increase pair count O(N^2) and quad count O(N^4).
     * - Smaller cutoff (e.g., 20°) would miss wide quads that still fit in FOV.
     *
     * This is a CONSERVATIVE DEFAULT, UNVALIDATED — may need tuning once real detection FOV and
     * star density are measured on device. Marked as unvalidated pending real centroiding data.
     */
    const val MAX_PAIR_SEPARATION_DEG: Double = 40.0
    val MAX_PAIR_SEPARATION_RAD: Double = MAX_PAIR_SEPARATION_DEG * DEG_TO_RAD

    /**
     * Minimum pairwise separation to include (avoid too close stars that are hard to resolve).
     * Default: 0.1° (6 arcminutes), conservative.
     * UNVALIDATED — depends on centroiding resolution and PSF overlap.
     */
    const val MIN_PAIR_SEPARATION_DEG: Double = 0.1
    val MIN_PAIR_SEPARATION_RAD: Double = MIN_PAIR_SEPARATION_DEG * DEG_TO_RAD

    /**
     * Hash quantization bin width for quad descriptor ratios.
     * Descriptor ratios are in [0,1] (normalized by max separation).
     * Bin width 0.01 means 1% tolerance in ratio.
     *
     * Reasoning:
     * - Phase 2 expected centroiding error ~0.1-0.3 px (UNVALIDATED, reasoning only).
     * - For typical 1° separation, 0.3 px error at ~1000 px width, FOV 60° => ~0.06°/px => 0.018° error in separation.
     * - Ratio error ~0.018° / 1° = 0.018 = 1.8%, so bin width 0.01-0.02 is conservative.
     * - Smaller bin (0.005) would be more precise but less tolerant to noise; larger (0.02) more tolerant but more collisions.
     *
     * Default 0.01 is CONSERVATIVE DEFAULT, UNVALIDATED pending real centroiding data.
     */
    const val HASH_BIN_WIDTH: Double = 0.01

    // ===== C (final pass): CAPPED quad index defaults =====
    // See QuadPatternIndex.capped(...). All three UNVALIDATED pending the D synthetic-E2E
    // sweep (solve-success / false-lock / index-size curves); starting points from the A1
    // sizing model (sky fraction 0.07272 @ 63.5-deg FOV, k=5 stars/FOV -> ~7 MB asset target).

    /**
     * Only stars brighter than or equal to this magnitude participate in quad BUILDING
     * (the full catalog is still shipped for verification/tracking). V=5.5 keeps the
     * quad builder dense enough in sparse sky regions while bounding quad count.
     */
    const val QUAD_BUILD_MAX_MAGNITUDE: Double = 5.5

    /**
     * Per-anchor neighbor cap: each quad-eligible star contributes quads only with its
     * N nearest eligible neighbors -> at most C(N,3) quads per anchor before dedupe.
     */
    const val QUAD_NEIGHBORS_PER_STAR: Int = 6

    /** Hard deterministic ceiling on total quads (safety net for dense regions). */
    const val QUAD_MAX_QUADS: Int = 120_000

    /**
     * Pyramid consistency tolerance for matching observed quad to catalog quad.
     * Angular separation must agree within this tolerance (radians).
     * Default: 0.01 rad = 0.57° = 34 arcminutes — very conservative, allows large centroiding error.
     * Real tolerance should be ~0.001 rad (0.06°) if centroiding is 0.1-0.3 px, but we use conservative default.
     * UNVALIDATED.
     */
    const val PYRAMID_CONSISTENCY_TOLERANCE_DEG: Double = 0.5
    val PYRAMID_CONSISTENCY_TOLERANCE_RAD: Double = PYRAMID_CONSISTENCY_TOLERANCE_DEG * DEG_TO_RAD

    /**
     * Magnitude cutoff for bright-star extract.
     * Target ~9,000-15,000 stars, magnitude ≤6.5-7.0 per architecture roadmap Section 6.
     * Reasoning: phone camera can detect stars down to mag ~6-7 under dark sky, brighter cutoff reduces catalog size.
     * This is a design choice, not mathematically fixed, but based on hardware capability.
     * UNVALIDATED pending real detection sensitivity measurement.
     */
    const val MAGNITUDE_CUTOFF: Double = 6.5

    /**
     * Target catalog size range.
     * Mathematically not fixed, but architecture decision: bright-star extract sized to what handheld phone can detect.
     */
    const val TARGET_CATALOG_SIZE_MIN: Int = 9000
    const val TARGET_CATALOG_SIZE_MAX: Int = 15000

    /**
     * Maximum number of stars to consider for quad building per sky region (to limit combinatorial explosion).
     * For N stars, number of quads is C(N,4). For N=50, C(50,4)=230,300 — too many.
     * So we limit to nearby stars within MAX_PAIR_SEPARATION and/or top-N brightest per region.
     * Default 50 is conservative.
     * UNVALIDATED.
     */
    const val MAX_STARS_PER_REGION_FOR_QUADS: Int = 50

    /**
     * Number of top brightest observations to consider for quad candidate formation in solver (Phase 4).
     * Default 8-10, tradeoff: N=8 → C(8,4)=70 quads, N=10 → 210 quads.
     * UNVALIDATED.
     */
    const val TOP_N_BRIGHTEST_FOR_QUAD_CANDIDATES: Int = 10
}
