package com.alijafari.red.astronomy.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.max

/**
 * Where the tap callout sits, in **canvas pixels**.
 *
 * All of [cardLeftPx], [cardTopPx] and the wedge's corner are pixel coordinates in the callout layer's
 * own space, and the callout composable hands them to `Placeable.place(x, y)` — the one placement in
 * Compose that no layout direction can mirror. That distinction is the whole point of this file.
 * `Modifier.offset` places with `placeRelativeWithLayer`, so in a right-to-left layout (ZIG runs RTL
 * for Persian, its default language) it lands at `canvasWidth − x − cardWidth`: the right height,
 * mirrored to the far side of the screen, exactly correct only at the canvas centre — while the sky
 * behind the card is a `Canvas` drawing that no layout direction mirrors.
 *
 * [inLayerSpace] is the other half of the same idea: the card is pinned to a position measured on the
 * sky canvas, and that position only means anything in the callout layer's pixels if the two nodes'
 * window rects are accounted for.
 *
 * The arithmetic lives here, pure and Compose-free, so the geometry this app *believes* it has is
 * unit-tested on the JVM instead of being asserted by eye on a phone:
 *
 *  * the card is centred on the object and clamped inside the canvas;
 *  * it flips below the object when there is no room above it;
 *  * the wedge keeps pointing at the object even when the card is clamped against an edge.
 */
data class SkyCalloutPlacement(
    /** Whether the card sits above the object (true) or below it (false). */
    val placeAbove: Boolean,
    /** Left edge of the card, in canvas pixels. */
    val cardLeftPx: Float,
    /** Top edge of the card, in canvas pixels. */
    val cardTopPx: Float,
    /** Left edge of the wedge, in canvas pixels. */
    val wedgeLeftPx: Float,
    /** Top edge of the wedge, in canvas pixels. */
    val wedgeTopPx: Float,
    /** Canvas x the wedge's tip points at — the object, held inside the card's corners. */
    val pointerXPx: Float
)

object SkyCalloutGeometry {

    /**
     * Canvas fraction below which a card is always placed above its object.
     *
     * On the Home scene the horizon is at 0.70 of the canvas height, so every visible object is below
     * this row: it keeps the card clear of the sky's top edge and of the header that sits over it.
     */
    const val ALWAYS_ABOVE_FRACTION = 0.55f

    /**
     * Places the callout for an object at ([pinX], [pinY]).
     *
     * @param cardWidthPx the measured card width, or `0` before the first measurement.
     * @param cardHeightPx the measured card height, or `0` before the first measurement.
     * @param gapPx clearance between the wedge's tip and the object.
     * @param edgePx how far the card is kept from the canvas edge.
     * @param tipInsetPx how far the pointer is kept from the card's rounded corners.
     */
    fun place(
        pinX: Float,
        pinY: Float,
        cardWidthPx: Float,
        cardHeightPx: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        gapPx: Float,
        wedgeWidthPx: Float,
        wedgeHeightPx: Float,
        edgePx: Float,
        tipInsetPx: Float
    ): SkyCalloutPlacement {
        val width = canvasWidthPx.coerceAtLeast(1f)
        val height = canvasHeightPx.coerceAtLeast(1f)
        val cardW = cardWidthPx.coerceAtLeast(0f)
        val cardH = cardHeightPx.coerceAtLeast(0f)

        // Room above the object for the whole card plus its wedge? Otherwise it flips under the
        // object — or is forced above anything low in the frame, where "below" would be off-canvas.
        val placeAbove = pinY - gapPx - wedgeHeightPx - cardH >= edgePx ||
            pinY > height * ALWAYS_ABOVE_FRACTION

        val cardLeft = (pinX - cardW / 2f)
            .coerceIn(edgePx, max(edgePx, width - edgePx - cardW))
        val cardTop = if (placeAbove) {
            (pinY - gapPx - wedgeHeightPx - cardH)
                .coerceIn(edgePx, max(edgePx, height - edgePx - cardH))
        } else {
            (pinY + gapPx + wedgeHeightPx)
                .coerceIn(edgePx, max(edgePx, height - edgePx - cardH - wedgeHeightPx))
        }

        // The pointer marks the object: its centre tracks the object's x, held inside the card's
        // corners, so it can never drift off the contact point it is supposed to mark.
        val pointerX = cardLeft + (pinX - cardLeft)
            .coerceIn(tipInsetPx, max(tipInsetPx, cardW - tipInsetPx))

        return SkyCalloutPlacement(
            placeAbove = placeAbove,
            cardLeftPx = cardLeft,
            cardTopPx = cardTop,
            wedgeLeftPx = pointerX - wedgeWidthPx / 2f,
            // The wedge's tip is the edge nearest the object: its bottom when the card is above it,
            // its top when the card is below.
            wedgeTopPx = if (placeAbove) cardTop + cardH else cardTop - wedgeHeightPx,
            pointerXPx = pointerX
        )
    }

    /**
     * A position measured on the sky canvas, expressed in the callout layer's own pixels.
     *
     * The object's position is projected on the canvas — the rectangle the sky renderer drew on — so it
     * is a canvas pixel. The card, however, is placed inside the callout layer, which is a different
     * node. When the two share a top-left (they are siblings, each filling the same box, which is the
     * normal case) this is the identity; when they do not — a padding, an alignment, a scrolled or
     * inset parent — the difference between their window positions is exactly the correction needed.
     * Both arguments are window pixels; the result is layer pixels.
     */
    fun inLayerSpace(
        pinOnCanvas: Offset,
        canvasTopLeft: Offset,
        layerTopLeft: Offset
    ): Offset = Offset(
        x = pinOnCanvas.x + (canvasTopLeft.x - layerTopLeft.x),
        y = pinOnCanvas.y + (canvasTopLeft.y - layerTopLeft.y)
    )
}
