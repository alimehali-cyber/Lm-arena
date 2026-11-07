package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.ui.monument.MonumentHitTargets
import com.alijafari.red.astronomy.ui.monument.MonumentId
import com.alijafari.red.astronomy.ui.rendering.PersepolisFraming
import com.alijafari.red.astronomy.ui.rendering.SkySceneLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two monuments in the Home landscape are also two tap targets, and this is the arithmetic that
 * decides which one a finger landed on.
 *
 * It is tested here, on the JVM, for the same reason [PersepolisFraming] itself is: the boxes are
 * pure numbers derived from the framing, so a test can state exactly what "a tap on the stone" means —
 * the centre of each monument's box belongs to that monument, the open plain between them belongs to
 * neither, and nothing outside the canvas can be hit. If the framing ever changes, these assertions
 * fail before a device does.
 */
class MonumentLandscapeHitTest {

    /** A phone-shaped scene: 360 × 493 dp at 3x, the shape the Home scene is composed for. */
    private fun framing(widthPx: Float = 1080f, heightPx: Float = 1480f): PersepolisFraming.Result {
        val horizon = heightPx * SkySceneLayout.HORIZON_FRACTION
        return PersepolisFraming.forScene(
            widthPx = widthPx,
            heightPx = heightPx,
            horizonYPx = horizon,
            readoutBottomPx = horizon - 6f,
            readoutRightPx = 750f,
            readoutClearancePx = 15f
        )
    }

    private fun rects(
        widthPx: Float = 1080f,
        heightPx: Float = 1480f,
        slopPx: Float = MonumentHitTargets.HIT_SLOP_DP * 3f
    ) = MonumentHitTargets.rects(
        framing = framing(widthPx, heightPx),
        canvasWidthPx = widthPx,
        canvasHeightPx = heightPx,
        slopPx = slopPx
    )

    @Test
    fun bothMonumentsGetATapBoxOnAPhoneScene() {
        val boxes = rects()
        assertEquals("each monument must be reachable", 2, boxes.size)
        assertEquals(
            setOf(MonumentId.PERSEPOLIS, MonumentId.MIL_E_AZHDAHA),
            boxes.map { it.id }.toSet()
        )
    }

    @Test
    fun eachBoxResolvesToItsOwnMonument() {
        val boxes = rects()
        boxes.forEach { box ->
            val x = box.centerX
            val y = (box.top + box.bottom) / 2f
            assertEquals(box.id, MonumentHitTargets.at(x, y, boxes))
        }
    }

    @Test
    fun theOpenPlainBetweenTheMonumentsBelongsToNeither() {
        val boxes = rects()
        val persepolis = boxes.first { it.id == MonumentId.PERSEPOLIS }
        val mil = boxes.first { it.id == MonumentId.MIL_E_AZHDAHA }
        assertTrue(
            "the two boxes must not overlap on a phone scene",
            persepolis.right < mil.left
        )
        val gapX = (persepolis.right + mil.left) / 2f
        val y = (persepolis.top + persepolis.bottom) / 2f
        assertNull(
            "a tap on the empty plain must fall through to the sky",
            MonumentHitTargets.at(gapX, y, boxes)
        )
    }

    @Test
    fun theSkyAboveTheStoneIsStillTheSky() {
        val boxes = rects()
        val highest = boxes.minOf { it.top }
        assertTrue(highest > 0f)
        assertNull(
            "a tap well above both monuments must not open a page",
            MonumentHitTargets.at(540f, highest / 2f, boxes)
        )
    }

    @Test
    fun everyBoxStaysInsideTheCanvas() {
        listOf(1080f to 1480f, 720f to 1560f, 1440f to 1120f).forEach { (w, h) ->
            val boxes = rects(widthPx = w, heightPx = h)
            boxes.forEach { box ->
                assertTrue(box.left >= 0f && box.right <= w, "box inside the width at ${w}x$h")
                assertTrue(box.top >= 0f && box.bottom <= h, "box inside the height at ${w}x$h")
            }
        }
    }

    @Test
    fun aSceneTooNarrowToHoldTheTowerDropsItsBoxInsteadOfGuessing() {
        // 220 px wide: at the fixed 178 m window the tower's slope lies off the right edge. Whatever
        // remains of its box, a point at the far left must still be Persepolis and never the tower.
        val boxes = rects(widthPx = 220f, heightPx = 900f, slopPx = 6f)
        val persepolis = boxes.firstOrNull { it.id == MonumentId.PERSEPOLIS }
        assertTrue("Persepolis must keep a box on a narrow scene", persepolis != null)
        assertEquals(
            MonumentId.PERSEPOLIS,
            MonumentHitTargets.at(persepolis!!.centerX, (persepolis.top + persepolis.bottom) / 2f, boxes)
        )
    }

    @Test
    fun theSelectionIdsAreOutsideTheCatalogueSpaceAndRoundTrip() {
        MonumentId.values().forEach { monument ->
            assertTrue(
                "monument selection ids share one prefix",
                monument.selectionId.startsWith(MonumentId.SELECTION_PREFIX)
            )
            assertEquals(monument, MonumentId.fromSelectionId(monument.selectionId))
        }
        assertNull(MonumentId.fromSelectionId("sun"))
        assertNull(MonumentId.fromSelectionId("planet_mars"))
        assertNull(MonumentId.fromSelectionId("star_bsc_hr_15"))
    }
}
