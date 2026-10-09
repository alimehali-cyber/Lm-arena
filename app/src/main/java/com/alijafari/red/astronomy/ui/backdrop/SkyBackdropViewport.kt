package com.alijafari.red.astronomy.ui.backdrop

import com.alijafari.red.astronomy.ui.rendering.HeroSkyProjection

/**
 * Maps the full-screen window onto the shared sky projection without distorting it.
 *
 * [HeroSkyProjection] places azimuth on x at W/360 px per degree, and altitude on y at
 * (0.85·H − 24)/90 px per degree. Those two scales only match on a card that is nearly square.
 * On a full screen the backdrop therefore uses a *virtual canvas* with the same projection
 * formula. Its width is chosen so that both axes share one scale, [pxPerDeg]. It is centred
 * horizontally on the window, so the screen centre looks toward the same azimuth as on the Home card.
 *
 * The virtual canvas is wider than the window in most portrait layouts, so the window shows a
 * horizontal slice of the sky, and that slice is what gets cropped.
 */
data class SkyBackdropViewport(
    /** Width of the window in px. */
    val windowWidthPx: Float,
    /** Width of the virtual sky canvas in px. The projection receives this as its canvas width. */
    val virtualWidthPx: Float,
    /** Height of the window, which is also the height of the virtual canvas, in px. */
    val heightPx: Float,
    /** Horizontal offset of the virtual canvas inside the window in px. Negative when the sky is wider than the window. */
    val offsetXPx: Float,
    /** Shared horizontal and vertical scale in px per degree. */
    val pxPerDeg: Float
) {
    /** False when the window is too small for a meaningful sky. Nothing should be drawn then. */
    val isDrawable: Boolean
        get() = pxPerDeg > 0f && virtualWidthPx > 0f && heightPx > 0f

    /** Azimuth span visible in the window, in degrees. */
    val visibleAzimuthSpanDeg: Float
        get() = if (pxPerDeg > 0f) windowWidthPx / pxPerDeg else 0f

    companion object {
        private const val FULL_CIRCLE_DEG = 360f
        private const val ZENITH_DEG = 90f

        /**
         * Fits the sky to a window of [windowWidthPx] × [windowHeightPx].
         *
         * Vertical placement reuses the Home projection's horizon and top margin, so zenith stays
         * at the same relative height. Only the horizontal extent changes.
         */
        fun fit(windowWidthPx: Float, windowHeightPx: Float): SkyBackdropViewport {
            if (windowWidthPx <= 0f || windowHeightPx <= 0f) {
                return SkyBackdropViewport(windowWidthPx.coerceAtLeast(0f), 0f, 0f, 0f, 0f)
            }
            val horizonY = windowHeightPx * HeroSkyProjection.HORIZON_FRACTION
            val pxPerDeg = (horizonY - HeroSkyProjection.TOP_MARGIN_PX) / ZENITH_DEG
            if (pxPerDeg <= 0f) {
                return SkyBackdropViewport(windowWidthPx, 0f, windowHeightPx, 0f, 0f)
            }
            val virtualWidth = FULL_CIRCLE_DEG * pxPerDeg
            val offsetX = (windowWidthPx - virtualWidth) / 2f
            return SkyBackdropViewport(
                windowWidthPx = windowWidthPx,
                virtualWidthPx = virtualWidth,
                heightPx = windowHeightPx,
                offsetXPx = offsetX,
                pxPerDeg = pxPerDeg
            )
        }
    }
}
