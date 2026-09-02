package com.alijafari.red.astronomy.startracker.diagnostics

import kotlin.math.*

/**
 * Phase 9 Task 3: BearingCrossCheck vs ARProjectionEngine read-only.
 * This file does NOT modify ARProjectionEngine, only reads its logic conceptually and cross-checks HeroSkyProjection.
 * Pure Kotlin, no Android dependency.
 *
 * ARProjectionEngine uses 3D pinhole projection with rotation matrix, but for bearing ordering,
 * the relative azimuth concept should be consistent:
 * - Facing South (180°): East (90°) should be left of center (negative relative bearing)
 * - Facing North (0°): East (90°) should be right of center (positive relative bearing)
 *
 * This cross-check verifies HeroSkyProjection's fixed logic matches ARProjectionEngine's expected East/West ordering.
 */

data class CrossCheckCase(
    val objectAz: Double,
    val facingAz: Double,
    val expectedRelativeBearing: Double,
    val description: String
)

class BearingCrossCheck {

    /**
     * Generates cross-check cases comparing HeroSkyProjection (fixed) vs ARProjectionEngine expected ordering.
     */
    fun generateCases(): List<CrossCheckCase> {
        return listOf(
            CrossCheckCase(90.0, 180.0, -90.0, "East relative to South (north hemisphere) -> left"),
            CrossCheckCase(270.0, 180.0, 90.0, "West relative to South (north hemisphere) -> right"),
            CrossCheckCase(90.0, 0.0, 90.0, "East relative to North (south hemisphere) -> right"),
            CrossCheckCase(270.0, 0.0, -90.0, "West relative to North (south hemisphere) -> left"),
            CrossCheckCase(180.0, 180.0, 0.0, "South relative to South -> center"),
            CrossCheckCase(0.0, 0.0, 0.0, "North relative to North -> center"),
            CrossCheckCase(0.0, 180.0, -180.0, "North relative to South -> behind (seam)"),
            CrossCheckCase(180.0, 0.0, -180.0, "South relative to North -> behind (seam)")
        )
    }

    fun check(): List<Pair<CrossCheckCase, Boolean>> {
        val cases = generateCases()
        return cases.map { case ->
            val computed = RelativeBearing.relativeBearing(case.objectAz, case.facingAz)
            // Allow wrap: -180 and 180 are equivalent
            val expected = case.expectedRelativeBearing
            val matches = abs(computed - expected) < 1e-6 || (abs(abs(computed) - 180.0) < 1e-6 && abs(abs(expected) - 180.0) < 1e-6)
            Pair(case, matches)
        }
    }

    /**
     * Checks HeroSkyProjection current vs fixed for southern hemisphere.
     * Returns true if current matches expected (should be false for buggy southern).
     */
    fun checkHeroSkyCurrentVsFixed(): Map<String, Any> {
        val results = mutableMapOf<String, Any>()

        // Northern hemisphere: current and fixed should match
        val northEastCurrent = -90.0 // az 90 - 180 = -90
        val northEastFixed = RelativeBearing.relativeBearing(90.0, 180.0)
        results["north_east_current_vs_fixed_match"] = abs(northEastCurrent - northEastFixed) < 1e-6

        // Southern hemisphere: current buggy is 0-90=-90, fixed is 90-0=90
        val southEastCurrentBuggy = RelativeBearing.wrap180(0.0 - 90.0) // -90
        val southEastFixed = RelativeBearing.relativeBearing(90.0, 0.0) // 90
        results["south_east_current"] = southEastCurrentBuggy
        results["south_east_fixed"] = southEastFixed
        results["south_east_bug_confirmed"] = abs(southEastCurrentBuggy - southEastFixed) > 1e-6 && southEastCurrentBuggy == -90.0 && southEastFixed == 90.0

        val southWestCurrentBuggy = RelativeBearing.wrap180(0.0 - 270.0) // 90 (after normalize)
        val southWestFixed = RelativeBearing.relativeBearing(270.0, 0.0) // -90
        results["south_west_current"] = southWestCurrentBuggy
        results["south_west_fixed"] = southWestFixed
        results["south_west_bug_confirmed"] = abs(southWestCurrentBuggy - southWestFixed) > 1e-6

        return results
    }
}
