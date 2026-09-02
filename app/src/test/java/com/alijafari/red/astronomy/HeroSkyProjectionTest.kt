package com.alijafari.red.astronomy

import androidx.compose.ui.geometry.Offset
import com.alijafari.red.astronomy.ui.rendering.HeroSkyProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HeroSkyProjectionTest {

    @Test
    fun testAzimuthWraparoundDistance() {
        // 359° and 1° are 2° apart, not 358° apart
        val diff1 = HeroSkyProjection.azimuthDistanceDeg(359.0, 1.0)
        assertEquals(2.0, diff1, 1e-6)

        val diff2 = HeroSkyProjection.azimuthDistanceDeg(1.0, 359.0)
        assertEquals(2.0, diff2, 1e-6)

        val diff3 = HeroSkyProjection.azimuthDistanceDeg(350.0, 10.0)
        assertEquals(20.0, diff3, 1e-6)

        val diff4 = HeroSkyProjection.azimuthDistanceDeg(0.0, 360.0)
        assertEquals(0.0, diff4, 1e-6)

        val diff5 = HeroSkyProjection.azimuthDistanceDeg(180.0, 0.0)
        assertEquals(180.0, diff5, 1e-6)
    }

    @Test
    fun testScreenDistanceCylindricalWraparound() {
        val canvasWidth = 1000f

        // Tap near x=5px and object near x=995px (same y=100px)
        val tapPos = Offset(5f, 100f)
        val objPos = Offset(995f, 100f)

        val dist = HeroSkyProjection.screenDistance(tapPos, objPos, canvasWidth)
        assertEquals(10f, dist, 1e-4f)

        // Tap near center x=500px and object near x=510px, y=100px vs 100px
        val tapCenter = Offset(500f, 100f)
        val objCenter = Offset(510f, 100f)
        val distCenter = HeroSkyProjection.screenDistance(tapCenter, objCenter, canvasWidth)
        assertEquals(10f, distCenter, 1e-4f)
    }

    @Test
    fun testHeroSkyProjectionCoordinates() {
        val width = 1000f
        val height = 500f

        // Horizon at altitude 0° -> y = height * 0.85 = 425
        val horizonPos = HeroSkyProjection.project(0.0, 0.0, width, height)
        assertEquals(0f, horizonPos.x, 1e-4f)
        assertEquals(425f, horizonPos.y, 1e-4f)

        // Zenith at altitude 90° -> y = 24px (TOP_MARGIN_PX)
        val zenithPos = HeroSkyProjection.project(180.0, 90.0, width, height)
        assertEquals(500f, zenithPos.x, 1e-4f)
        assertEquals(24f, zenithPos.y, 1e-4f)

        // Full 360° wraps seamlessly to 0
        val wrapPos = HeroSkyProjection.project(360.0, 0.0, width, height)
        assertEquals(0f, wrapPos.x, 1e-4f)
        assertEquals(425f, wrapPos.y, 1e-4f)
    }

    // Phase 1 Task 6 — Hemisphere behavior tests (Phase 0 findings C6/C8)
    // Requirement per task: northern hemisphere: east left of center, west right of center;
    // southern hemisphere: mirrored.
    // We test both interpretations and report actual result.

    @Test
    fun testNorthernHemisphereEastWestOrdering() {
        val width = 1000f
        val height = 500f
        val latNorth = 40.0 // Northern hemisphere

        val centerX = width / 2f

        // Due East (az=90°) and Due West (az=270°) at same altitude (e.g., 30°)
        val eastPos = HeroSkyProjection.project(90.0, 30.0, width, height, latNorth)
        val westPos = HeroSkyProjection.project(270.0, 30.0, width, height, latNorth)

        // Northern: East left of center, West right of center
        assertTrue("Northern: East (90°) should be left of center, got x=${eastPos.x} vs center=$centerX", eastPos.x < centerX)
        assertTrue("Northern: West (270°) should be right of center, got x=${westPos.x} vs center=$centerX", westPos.x > centerX)

        // Additionally, East should be at ~0.25*width, West at ~0.75*width per doc
        assertEquals(250f, eastPos.x, 1f)
        assertEquals(750f, westPos.x, 1f)
    }

    @Test
    fun testSouthernHemisphereEastWestOrdering_MirroredExpectation() {
        val width = 1000f
        val height = 500f
        val latSouth = -35.0 // Southern hemisphere

        val centerX = width / 2f

        val eastPos = HeroSkyProjection.project(90.0, 30.0, width, height, latSouth)
        val westPos = HeroSkyProjection.project(270.0, 30.0, width, height, latSouth)

        // According to task's "mirrored" expectation:
        // Southern should be opposite of northern: East right of center, West left of center
        // This is physically correct if viewer faces North (East is to the right when facing North).
        // However current implementation (per docstring) gives East left, West right for both hemispheres.
        // This test will FAIL if implementation is not mirrored, which is a useful finding to report.

        // We assert mirrored expectation, but we also document actual result in comments.
        // If this test fails, it indicates current implementation does NOT mirror southern hemisphere.

        // For reporting purposes, we check both possibilities and provide diagnostic:
        val isMirrored = eastPos.x > centerX && westPos.x < centerX
        val isSameAsNorth = eastPos.x < centerX && westPos.x > centerX

        // The task says to report actual result, not to fix. So we assert mirrored to see if it fails.
        // If it fails, the failure itself is the finding.
        // We will keep the assertion as mirrored expectation, per task's stated requirement.

        assertTrue(
            "Southern (mirrored expectation): East (90°) should be right of center (x > $centerX), " +
                    "West (270°) left of center. Actual: eastX=${eastPos.x}, westX=${westPos.x}, " +
                    "isMirrored=$isMirrored, isSameAsNorth=$isSameAsNorth. " +
                    "If this fails, current impl does NOT mirror southern hemisphere (both hemispheres East left).",
            isMirrored
        )
    }

    @Test
    fun testSouthernHemisphereActualBehavior_Diagnostic() {
        // Diagnostic test that always passes, but logs actual behavior for reporting
        val width = 1000f
        val height = 500f

        val eastNorth = HeroSkyProjection.project(90.0, 30.0, width, height, 40.0)
        val westNorth = HeroSkyProjection.project(270.0, 30.0, width, height, 40.0)
        val eastSouth = HeroSkyProjection.project(90.0, 30.0, width, height, -35.0)
        val westSouth = HeroSkyProjection.project(270.0, 30.0, width, height, -35.0)

        // This test documents actual behavior without asserting mirrored, so it should pass regardless
        // Northern: East left, West right
        assertTrue(eastNorth.x < 500f)
        assertTrue(westNorth.x > 500f)

        // Southern actual: check what it really does
        // Current implementation gives East left, West right for both (not mirrored)
        // So we assert actual current behavior to have a passing diagnostic
        assertTrue("Diagnostic: Southern East is at ${eastSouth.x}, West at ${westSouth.x}", eastSouth.x < 500f && westSouth.x > 500f)
    }

    @Test
    fun testHemisphereCenterFacing() {
        val width = 1000f
        val height = 500f

        // Northern hemisphere center should be South (180°)
        val southPosNorth = HeroSkyProjection.project(180.0, 30.0, width, height, 40.0)
        assertEquals(500f, southPosNorth.x, 1f) // South at center for north

        // Southern hemisphere center should be North (0°)
        val northPosSouth = HeroSkyProjection.project(0.0, 30.0, width, height, -35.0)
        assertEquals(500f, northPosSouth.x, 1f) // North at center for south

        // Northern: North at seam behind viewer (x~0 or 1000)
        val northPosNorth = HeroSkyProjection.project(0.0, 30.0, width, height, 40.0)
        // relAz = 0-180=-180 => x=0.5-0.5=0
        assertEquals(0f, northPosNorth.x, 1f)

        // Southern: South at seam behind viewer
        val southPosSouth = HeroSkyProjection.project(180.0, 30.0, width, height, -35.0)
        // relAz = normalize(0-180)=normalize(-180)= -180 or 180? Should be 180 => x=1.0*width? Let's check actual
        // normalizeSignedAngle(-180) = ((-180%360)+540)%360-180 = (180+540)%360-180=720%360=0-180=-180 => x=0
        // So South at seam for southern hemisphere is at x=0 as well (wrapping)
        // This is implementation detail, but we document it
        assertTrue(southPosSouth.x == 0f || southPosSouth.x == 1000f || kotlin.math.abs(southPosSouth.x - 0f) < 1f)
    }
}
