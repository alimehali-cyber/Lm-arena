package com.alijafari.red.astronomy.ui.components

import kotlin.math.max

/**
 * Where the tap callout sits, in **canvas pixels**.
 *
 * All three of [cardLeftPx], [cardTopPx] and the wedge's corner are absolute pixel coordinates: the
 * composable draws them with `Modifier.absoluteOffset`, never with the layout-direction-aware
 * `Modifier.offset`. That distinction is the whole point of this file. `Modifier.offset` is *mirrored*
 * in a right-to-left layout (`placeRelativeWithLayer`), and ZIG runs RTL for Persian — its default
 * language — while the sky behind the card is a `Canvas` that no layout direction can mirror. So a
 * card placed with `offset` landed at `canvasWidth − x − cardWidth`: the right height, mirrored to the
 * far side of the screen, exactly correct at the canvas centre and worst at the edges.
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
}
