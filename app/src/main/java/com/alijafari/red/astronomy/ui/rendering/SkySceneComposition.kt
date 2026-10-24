package com.alijafari.red.astronomy.ui.rendering

/**
 * Geometry of the full-bleed Home scene.
 *
 * One composition is shared by every layer that has to agree on where the ground is: the projection
 * ([SceneProjection]), the ground band ([SceneGroundRenderer]), the cloud veils ([CloudVeilRenderer]) and
 * the observer ([ObserverRenderer]). It is pure arithmetic on the canvas size, so it can be unit-tested
 * on the JVM and used off the main thread.
 *
 * The scene is laid out the way a photograph of a sky is framed: most of the frame is sky, the horizon
 * sits low, and a band of ground fills the bottom. The numbers below are fractions of the canvas height.
 */
object SkySceneLayout {

    /**
     * Canvas fraction where altitude 0° maps on the Home scene.
     *
     * The legacy Home card keeps [HeroSkyProjection.HORIZON_FRACTION] (0.85). The scene puts its horizon at
     * 0.70 of its own height, and the scene itself takes about 0.56 of the viewport, so altitude 0° lands at
     * roughly 39 % of the screen — high enough that the sky owns the top of the phone, low enough that the
     * ground is a strip the first card can overlap instead of an empty field.
     */
    const val HORIZON_FRACTION = 0.70f

    /** Height of the observer, as a fraction of the scene height. */
    const val FIGURE_HEIGHT_FRACTION = 0.190f

    /** How far the observer's feet sit below the grass line, as a fraction of the scene height. */
    const val FIGURE_FOOT_DROP_FRACTION = 0.008f

    /** Horizontal centre of the observer, as a fraction of width, when they stand on the right. */
    const val FIGURE_CENTER_FRACTION = 0.745f

    /** Amplitude of the distant ridge line, as a fraction of the scene height. */
    const val FAR_RIDGE_FRACTION = 0.034f

    /** Amplitude of the mid ridge line, as a fraction of the scene height. */
    const val MID_RIDGE_FRACTION = 0.020f

    /** Top of the solid ground band, below the horizon, as a fraction of the scene height. */
    const val GROUND_TOP_FRACTION = 0.052f

    /**
     * Horizontal centre of Professor Z's 3D layer, as a fraction of width, in RTL (Persian) mode: he owns
     * the LEFT of the sky canvas while the silhouette observer keeps the right. In English (LTR) mode the
     * side is mirrored (`1 - f`): Professor Z stands on the RIGHT, reading side, and the observer on the
     * left — the two never compete for the same third of the frame in either mode.
     */
    const val PROFESSOR_Z_CENTER_FRACTION = 0.175f

    /** Height of Professor Z's box as a fraction of the scene height. */
    const val PROFESSOR_Z_HEIGHT_FRACTION = 0.295f

    /** Width of Professor Z's box as a fraction of its own height. */
    const val PROFESSOR_Z_BOX_ASPECT = 0.68f

    /** How far into his box the feet sit (0.964 = feet on the grass line with a small sole margin). */
    const val PROFESSOR_Z_FEET_SEAT = 0.964f

    /** Builds the composition for a canvas of [widthPx] x [heightPx] pixels. */
    fun composition(
        widthPx: Float,
        heightPx: Float,
        horizonFraction: Float = HORIZON_FRACTION,
        figureOnRight: Boolean = true
    ): SceneComposition {
        val w = widthPx.coerceAtLeast(1f)
        val h = heightPx.coerceAtLeast(1f)
        val horizonY = h * horizonFraction.coerceIn(0.30f, 0.92f)
        val groundTopY = horizonY + h * GROUND_TOP_FRACTION
        val figureCenter = if (figureOnRight) FIGURE_CENTER_FRACTION else 1f - FIGURE_CENTER_FRACTION
        return SceneComposition(
            widthPx = w,
            heightPx = h,
            horizonY = horizonY,
            groundTopY = groundTopY,
            figureHeightPx = h * FIGURE_HEIGHT_FRACTION,
            // Feet on the grass line, the way a person stands on the near bank with the sky behind them.
            figureFeetY = groundTopY + h * FIGURE_FOOT_DROP_FRACTION,
            figureCenterX = w * figureCenter,
            farRidgeAmplitude = h * FAR_RIDGE_FRACTION,
            midRidgeAmplitude = h * MID_RIDGE_FRACTION
        )
    }

    /**
     * Rectangle for the 3D Professor Z layer: [PROFESSOR_Z_HEIGHT_FRACTION] of the scene height, centred on
     * [PROFESSOR_Z_CENTER_FRACTION] of the canvas (mirrored to `1 - f` when [figureOnRight]) and clamped so the
     * box never leaves the canvas, feet seated on the grass line. English mode places him RIGHT, Persian (RTL)
     * mode places him LEFT — the exact mirror of where the text column sits.
     */
    fun professorZBox(
        widthPx: Float,
        heightPx: Float,
        horizonFraction: Float = HORIZON_FRACTION,
        figureOnRight: Boolean = false,
    ): ProfessorZBox {
        val w = widthPx.coerceAtLeast(1f)
        val h = heightPx.coerceAtLeast(1f)
        // A tall portrait canvas must NOT stretch him across the phone: cap the box so its width always
        // fits inside its half (plus margins), the height fraction is the ceiling, not the rule.
        val boxH = minOf(h * PROFESSOR_Z_HEIGHT_FRACTION, (w * 0.46f) / PROFESSOR_Z_BOX_ASPECT)
        val boxW = boxH * PROFESSOR_Z_BOX_ASPECT
        val centerFraction = if (figureOnRight) 1f - PROFESSOR_Z_CENTER_FRACTION else PROFESSOR_Z_CENTER_FRACTION
        val centerX = (w * centerFraction)
            .coerceIn(boxW * 0.5f + 4f, (w - boxW * 0.5f - 4f).coerceAtLeast(boxW * 0.5f + 4f))
        val scene = composition(widthPx = w, heightPx = h, horizonFraction = horizonFraction, figureOnRight = true)
        return ProfessorZBox(
            leftPx = centerX - boxW * 0.5f,
            topPx = scene.figureFeetY - boxH * PROFESSOR_Z_FEET_SEAT,
            widthPx = boxW,
            heightPx = boxH,
        )
    }
}

/**
 * Resolved pixel geometry for one scene frame. Produced by [SkySceneLayout.composition].
 */
data class SceneComposition(
    val widthPx: Float,
    val heightPx: Float,
    /** Where altitude 0° sits, and where the distant ridge line is anchored. */
    val horizonY: Float,
    /** Where the solid ground band starts. Between [horizonY] and here is the ridge/mist shelf. */
    val groundTopY: Float,
    val figureHeightPx: Float,
    val figureFeetY: Float,
    val figureCenterX: Float,
    val farRidgeAmplitude: Float,
    val midRidgeAmplitude: Float
) {
    /** Vertical extent of the sky above the horizon, in pixels. */
    val skyHeightPx: Float get() = horizonY

    /** Vertical extent of everything below the horizon, in pixels. */
    val groundHeightPx: Float get() = (heightPx - horizonY).coerceAtLeast(1f)
}

/**
 * Pixel rectangle for the 3D Professor Z layer on the Home Sky canvas (left-anchored, feet on the grass).
 * Produced by [SkySceneLayout.professorZBox]; pure arithmetic so it is unit-testable on the JVM.
 */
data class ProfessorZBox(
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float,
) {
    val centerX: Float get() = leftPx + widthPx * 0.5f
    val feetY: Float get() = topPx + heightPx * SkySceneLayout.PROFESSOR_Z_FEET_SEAT
}

