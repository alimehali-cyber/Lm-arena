package com.alijafari.red.astronomy.ui.monument

import com.alijafari.red.astronomy.ui.rendering.PersepolisFraming
import kotlin.math.max
import kotlin.math.min

/**
 * Where the two monuments are, in canvas pixels, for the tap handler.
 *
 * The relief itself is drawn by a GL surface that knows nothing about touches, so the sky canvas has
 * to decide on its own what a finger landed on. It already knows exactly where the stone is: the same
 * [PersepolisFraming.Result] that places the camera also publishes the projected columns and rows of
 * every landmark — the Persepolis façade, the great doorway, the tallest column, the tower's slope.
 * The hit boxes are built from those, so a tap is tested against the stone as it is actually framed
 * at this scene size, on this device, at this moment.
 *
 * Pure arithmetic on pixels, no Compose and no state, so the boxes can be unit-tested on the JVM.
 */
data class MonumentHitRect(
    val id: MonumentId,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f

    val width: Float get() = right - left

    val height: Float get() = bottom - top

    fun contains(x: Float, y: Float): Boolean = x >= left && x <= right && y >= top && y <= bottom

    /**
     * Where the selection card's pointer should sit: the middle of the monument's top edge, a little
     * below it, so the card hangs over the stone it names rather than over the sky above it.
     */
    fun pinY(): Float = top + min(height * 0.18f, height * 0.5f)
}

object MonumentHitTargets {

    /**
     * Padding around each box, in dp. The stone is thin, dark and low on a phone; a fingertip is
     * wider than a column. 14 dp is enough to make the monuments easy to hit without reaching into
     * the open sky or the plain between them.
     */
    const val HIT_SLOP_DP = 14f

    /** The narrowest box worth testing; below this the tap falls through to the sky. */
    private const val MIN_BOX_PX = 12f

    /**
     * The two hit boxes for a scene of [canvasWidthPx] × [canvasHeightPx].
     *
     * The boxes are clipped to the canvas and dropped when clipping leaves nothing to hit — on a very
     * narrow scene the tower's slope can lie entirely off the right edge, and a zero-width box would
     * otherwise sit there swallowing taps meant for the sky.
     */
    fun rects(
        framing: PersepolisFraming.Result,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        slopPx: Float
    ): List<MonumentHitRect> {
        val w = canvasWidthPx.coerceAtLeast(1f)
        val h = canvasHeightPx.coerceAtLeast(1f)

        // Persepolis: from the great doorway's left jamb to the tallest column, and from the top of
        // the tallest structure down to the toe of the platform (plus a little of the earth in front).
        val persepolisLeft = min(framing.greatDoorwayXPx, framing.facadeLeftXPx) - slopPx
        val persepolisRight = max(framing.tallestColumnXPx, framing.facadeRightXPx) + slopPx
        val persepolisTop = min(framing.greatDoorwayTopYPx, framing.tallestColumnTopYPx) - slopPx
        val persepolisBottom = framing.toeYPx + slopPx * 0.75f

        // Mil-e Azhdaha: the tower's limestone slope, from its fixed top sheet down to the front step,
        // and never narrower than the tower itself even when the slope runs off the right edge.
        val towerLeft = min(framing.milSlopeLeftXPx, framing.milTowerCenterXPx - MIN_BOX_PX * 4f)
        val milLeft = towerLeft.coerceIn(0f, w - 1f)
        val milRight = max(framing.milSlopeRightXPx, milLeft + MIN_BOX_PX * 6f).coerceAtMost(w)
        val milTop = framing.milTowerTopYPx - slopPx
        val milBottom = framing.milSlopeFrontYPx + slopPx * 0.75f

        return listOfNotNull(
            clip(MonumentId.PERSEPOLIS, persepolisLeft, persepolisTop, persepolisRight, persepolisBottom, w, h),
            clip(MonumentId.MIL_E_AZHDAHA, milLeft, milTop, milRight, milBottom, w, h)
        )
    }

    /**
     * The monument a tap at ([x], [y]) belongs to, or null when it missed both.
     *
     * When the two boxes overlap — which only happens if the framing is ever squeezed far tighter
     * than the phone shapes it is built for — the nearer centre wins, so the answer is never arbitrary.
     */
    fun at(x: Float, y: Float, rects: List<MonumentHitRect>): MonumentId? = rects
        .filter { it.contains(x, y) }
        .minByOrNull { rect -> (x - rect.centerX) * (x - rect.centerX) + (y - (rect.top + rect.bottom) / 2f) * (y - (rect.top + rect.bottom) / 2f) }
        ?.id

    private fun clip(
        id: MonumentId,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        w: Float,
        h: Float
    ): MonumentHitRect? {
        val l = left.coerceIn(0f, w)
        val r = right.coerceIn(0f, w)
        val t = top.coerceIn(0f, h)
        val b = bottom.coerceIn(0f, h)
        val rect = MonumentHitRect(id, l, t, r, b)
        return if (rect.width >= MIN_BOX_PX && rect.height >= MIN_BOX_PX) rect else null
    }
}
