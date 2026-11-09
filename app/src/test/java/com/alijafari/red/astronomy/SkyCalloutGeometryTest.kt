package com.alijafari.red.astronomy

import androidx.compose.ui.geometry.Offset
import com.alijafari.red.astronomy.ui.components.SkyCalloutGeometry
import com.alijafari.red.astronomy.ui.rendering.SkyTapTargets
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The identify card that rises when the sky or the landscape is tapped must sit **on its object**.
 *
 * It did not, for two independent reasons, and both are pinned here:
 *
 *  1. **The placement was mirrored in Persian, then it drifted off the object altogether.** Moving a
 *     smaller-than-parent child with an offset modifier is a placement problem, not an arithmetic one:
 *     `Modifier.offset` is layout-direction aware (`placeRelativeWithLayer`), so in RTL the card landed
 *     at `canvasWidth − x − cardWidth` — the right height, the far side of the screen, exactly right
 *     only at the canvas centre — while the sky and the landscape behind it are `Canvas` drawing that
 *     no layout direction can mirror. The card is now *placed* with `Placeable.place(x, y)` in one
 *     layout pass, from pure arithmetic in [SkyCalloutGeometry], in the callout layer's own pixels;
 *     the pins are projected on the canvas' own window rect first ([SkyCalloutGeometry.inLayerSpace]).
 *     The source guards below keep it that way.
 *  2. **A tap could select something that was not drawn** — a star under the horizon, a star faded out
 *     by daylight, a Sun 10° below the ground — and the card then rose over empty sky. [SkyTapTargets]
 *     is the renderers' own visibility gate, applied to the hit test.
 */
class SkyCalloutGeometryTest {

    // A 1080 px wide canvas (a ~393 dp phone), a 400 x 120 px card, and the callout's real dimensions
    // in pixels at that density: a 13 dp gap, an 18 x 7 dp wedge, an 8 dp edge inset, a 14 dp tip inset.
    private val canvasW = 1080f
    private val canvasH = 1800f
    private val cardW = 400f
    private val cardH = 120f
    private val gap = 36f
    private val wedgeW = 50f
    private val wedgeH = 20f
    private val edge = 22f
    private val tipInset = 38f

    private fun place(pinX: Float, pinY: Float) = SkyCalloutGeometry.place(
        pinX = pinX,
        pinY = pinY,
        cardWidthPx = cardW,
        cardHeightPx = cardH,
        canvasWidthPx = canvasW,
        canvasHeightPx = canvasH,
        gapPx = gap,
        wedgeWidthPx = wedgeW,
        wedgeHeightPx = wedgeH,
        edgePx = edge,
        tipInsetPx = tipInset
    )

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    // ================================ placement ==================================================

    @Test
    fun theCardIsCentredOnItsObjectAndTheWedgeAimsAtIt() {
        val objectY = 900f
        val p = place(pinX = 540f, pinY = objectY)

        assertTrue("there is room above, so the card sits above the object", p.placeAbove)
        assertEquals("the card is centred on the object", 540f, p.cardLeftPx + cardW / 2f, 1e-3f)
        assertEquals("the pointer is the object's x", 540f, p.pointerXPx, 1e-3f)
        assertEquals("the wedge is centred on the pointer", 540f, p.wedgeLeftPx + wedgeW / 2f, 1e-3f)

        // Card, wedge, then the gap, then the object.
        assertEquals("the wedge's base touches the card", p.cardTopPx + cardH, p.wedgeTopPx, 1e-3f)
        assertEquals(
            "the wedge's tip clears the object by exactly the gap",
            gap,
            objectY - (p.wedgeTopPx + wedgeH),
            1e-3f
        )
    }

    @Test
    fun theCardNeverLeavesTheCanvasAndTheWedgeStaysOnTheObjectSide() {
        val pins = listOf(0f, 30f, 60f, 300f, 540f, 800f, 1020f, 1050f, 1080f)
        for (pinX in pins) {
            val p = place(pinX = pinX, pinY = 900f)

            assertTrue("card left inside the canvas at pinX=$pinX", p.cardLeftPx >= edge - 1e-3f)
            assertTrue(
                "card right inside the canvas at pinX=$pinX",
                p.cardLeftPx + cardW <= canvasW - edge + 1e-3f
            )
            assertTrue(
                "the pointer stays inside the card at pinX=$pinX",
                p.pointerXPx >= p.cardLeftPx + tipInset - 1e-3f &&
                    p.pointerXPx <= p.cardLeftPx + cardW - tipInset + 1e-3f
            )
            assertTrue(
                "the wedge is on the object's side of the card at pinX=$pinX",
                p.wedgeTopPx + wedgeH <= 900f + 1e-3f
            )
        }
    }

    @Test
    fun theCardFlipsUnderAnObjectNearTheTopEdge() {
        val p = place(pinX = 540f, pinY = 60f)

        assertFalse("no room above: the card goes under the object", p.placeAbove)
        assertTrue("the whole card is below the object", p.cardTopPx >= 60f)
        assertEquals("the wedge's top edge touches the card", p.cardTopPx, p.wedgeTopPx + wedgeH, 1e-3f)
        assertEquals(
            "the wedge's tip clears the object by exactly the gap",
            gap,
            p.wedgeTopPx - 60f,
            1e-3f
        )
    }

    @Test
    fun aLowObjectKeepsTheCardAboveItEvenOnAShortCanvas() {
        // The fullscreen viewer in landscape is only a few hundred pixels tall; a card that does not
        // fit above an object low in the frame is pinned to the top edge, but it must never flip to
        // "below" there — the object is already at the bottom, so the card would leave the screen.
        val p = SkyCalloutGeometry.place(
            pinX = 600f,
            pinY = 190f,
            cardWidthPx = cardW,
            cardHeightPx = cardH,
            canvasWidthPx = 1200f,
            canvasHeightPx = 240f,
            gapPx = gap,
            wedgeWidthPx = wedgeW,
            wedgeHeightPx = wedgeH,
            edgePx = edge,
            tipInsetPx = tipInset
        )

        assertTrue("still above the object", p.placeAbove)
        assertEquals("pinned to the top edge", edge, p.cardTopPx, 1e-3f)
        assertTrue("and the wedge still points down at the object", p.wedgeTopPx + wedgeH <= 190f)
    }

    @Test
    fun theCardFollowsTheObjectSideInsteadOfTheMirroredSide() {
        // The defect the user reported: in a right-to-left layout the offset was mirrored, so a card
        // for an object on the left third appeared on the right third (and vice versa) at the same
        // height. The placement is absolute, so it must follow the object, not the mirror of it.
        val leftThird = place(pinX = 200f, pinY = 900f)
        val rightThird = place(pinX = 880f, pinY = 900f)

        assertTrue(
            "an object on the left gets a card on the left",
            leftThird.cardLeftPx + cardW / 2f < canvasW / 2f
        )
        assertTrue(
            "an object on the right gets a card on the right",
            rightThird.cardLeftPx + cardW / 2f > canvasW / 2f
        )
        assertEquals("and the pointer is on the object", 200f, leftThird.pointerXPx, 1e-3f)
        assertEquals("and the pointer is on the object", 880f, rightThird.pointerXPx, 1e-3f)
    }

    // ================================ the tap gate ===============================================

    @Test
    fun aTapCannotSelectWhatTheSkyIsNotDrawing() {
        // The ground band is not sky: nothing below the horizon row may be identified by a tap there.
        assertTrue("a tap in the sky is on the sky", SkyTapTargets.tapIsOnSky(300f, 700f))
        assertTrue("the horizon row itself is the last sky row", SkyTapTargets.tapIsOnSky(700f, 700f))
        assertFalse("a tap in the ground band is not", SkyTapTargets.tapIsOnSky(701f, 700f))
        assertFalse("nor anywhere in the landscape", SkyTapTargets.tapIsOnSky(1200f, 700f))

        // Discs — the Sun, the Moon and the planets are all clipped at the horizon row, so all three
        // are selectable exactly while part of the drawn disc is still above it. At 7.5 px per degree
        // a 30 px radius spans 4°, so a body centred 3.9° below the row still shows an arc; 6° below
        // it is gone.
        val pxPerDegree = 7.5f
        val radius = 30f
        assertTrue("a Sun just setting is still on screen", SkyTapTargets.discIsDrawn(-3.0, radius, pxPerDegree))
        assertFalse("a Sun well under the ground is not", SkyTapTargets.discIsDrawn(-20.0, radius, pxPerDegree))
        assertTrue("and high in the sky it certainly is", SkyTapTargets.discIsDrawn(40.0, radius, pxPerDegree))
        assertTrue("a Moon half set is still on screen", SkyTapTargets.discIsDrawn(-1.0, radius, pxPerDegree))
        assertTrue("a Moon just set is still on screen", SkyTapTargets.discIsDrawn(-3.9, radius, pxPerDegree))
        assertFalse("a Moon fully set is not", SkyTapTargets.discIsDrawn(-6.0, radius, pxPerDegree))
        assertTrue("a planet above the row is", SkyTapTargets.discIsDrawn(2.0, 20f, pxPerDegree))
        assertFalse("a planet two and a half degrees under it is not", SkyTapTargets.discIsDrawn(-4.0, 20f, pxPerDegree))
        assertFalse(
            "a degenerate scale cannot make a body 40° under the horizon visible",
            SkyTapTargets.discIsDrawn(-40.0, radius, 0f)
        )

        // Stars are drawn only above 0.8° and only while the sky is dark enough to show them.
        assertFalse("a star under the horizon is not drawn", SkyTapTargets.starIsDrawn(-4.0, 1f))
        assertFalse("nor between the horizon row and 0.8°", SkyTapTargets.starIsDrawn(0.5, 1f))
        assertTrue("a star above it is", SkyTapTargets.starIsDrawn(3.0, 1f))
        assertFalse("and no star is drawn in daylight", SkyTapTargets.starIsDrawn(30.0, 0f))
        assertFalse("where even a trace of glow leaves them invisible", SkyTapTargets.starIsDrawn(30.0, 0.04f))
        assertTrue("the visible night sky is selectable", SkyTapTargets.starIsDrawn(30.0, 0.9f))
    }

    // ================================ source guards ==============================================

    @Test
    fun theCalloutIsNeverMovedWithAnOffsetModifier() {
        val hero = source("com/alijafari/red/astronomy/ui/components/HeroSkyCanvas.kt")
        val sandbox = source("com/zig/gravity/ui/GravitySandboxRoot.kt")

        assertFalse(
            "Modifier.offset is mirrored in RTL, so it must not place a card over the sky",
            hero.contains(".offset {")
        )
        assertFalse(
            "and an offset modifier of any kind is still a placement the layout owns, not the callout",
            hero.contains("absoluteOffset")
        )
        assertTrue("the card is placed explicitly", hero.contains("card.place("))
        assertTrue("the placement is the shared, tested geometry", hero.contains("SkyCalloutGeometry.place("))
        assertTrue(
            "and its pin is mapped from the canvas it was measured on into this layer",
            hero.contains("SkyCalloutGeometry.inLayerSpace(")
        )
        assertTrue(
            "the sandbox's long-press menu is anchored to raw touch pixels, so it is absolute too",
            sandbox.contains("absoluteOffset {")
        )
    }

    @Test
    fun theCalloutIsAnchoredToTheSkyCanvasItNames() {
        val hero = source("com/alijafari/red/astronomy/ui/components/HeroSkyCanvas.kt")

        assertTrue(
            "the canvas publishes its own rect in window pixels",
            hero.contains(".onGloballyPositioned { coords ->") && hero.contains("coords.boundsInWindow()")
        )
        assertTrue(
            "the callout layer publishes its own window origin, so the two can be reconciled",
            hero.contains("coords.positionInWindow()")
        )
        // A pin is projected on the canvas' own size — the size the renderer drew on — never on the
        // callout's, and never on the incoming constraints.
        assertTrue(
            "the pin is projected on the canvas' rect",
            hero.contains("pinAt(skyRect.width, skyRect.height)")
        )
        assertEquals(
            "both callouts — the sky's objects and the landscape's places — are anchored the same way",
            2,
            Regex("canvasBounds = canvasBounds,").findAll(hero).count()
        )
    }

    @Test
    fun aPinMeasuredOnTheCanvasIsExpressedInTheCalloutLayersOwnPixels() {
        val pin = Offset(300f, 500f)

        // The normal case: both nodes fill the same box, so they share a top-left and the pin is the
        // pin. This is what makes the ordinary Home and fullscreen canvases exact.
        assertEquals(
            pin,
            SkyCalloutGeometry.inLayerSpace(pin, canvasTopLeft = Offset.Zero, layerTopLeft = Offset.Zero)
        )
        assertEquals(
            pin,
            SkyCalloutGeometry.inLayerSpace(
                pin,
                canvasTopLeft = Offset(12f, 240f),
                layerTopLeft = Offset(12f, 240f)
            )
        )

        // A layer that sits 100 px lower than the canvas on screen: a canvas pixel at y = 500 is 100 px
        // *above* the layer's origin, so it must be placed at 400 — not at 500, where it would be off.
        assertEquals(
            Offset(300f, 400f),
            SkyCalloutGeometry.inLayerSpace(
                pin,
                canvasTopLeft = Offset(0f, -100f),
                layerTopLeft = Offset.Zero
            )
        )
    }

    @Test
    fun theTapHandlerUsesTheRenderersOwnVisibilityGate() {
        val hero = source("com/alijafari/red/astronomy/ui/components/HeroSkyCanvas.kt")

        assertTrue("the bodies are gated on whether their disc is drawn", hero.contains("SkyTapTargets.discIsDrawn("))
        assertEquals(
            "the Sun, the Moon and the planets all go through it",
            3,
            Regex("SkyTapTargets\\.discIsDrawn\\(").findAll(hero).count()
        )
        assertTrue("stars are gated", hero.contains("SkyTapTargets.starIsDrawn("))
        assertTrue("and a tap on the ground band selects nothing", hero.contains("SkyTapTargets.tapIsOnSky("))
        assertFalse(
            "the old -12° gate made an invisible Sun tappable in the ground band",
            hero.contains("sun.altitudeDeg > -12.0")
        )
    }
}
