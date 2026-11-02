package com.alijafari.red.astronomy.ui.splash

import kotlin.math.max
import kotlin.math.min

/**
 * Screen-adaptive geometry for the splash. Pure functions of the screen size so they can be
 * verified for every form factor (phones, tall phones, landscape, tablets, foldables) in a JVM test.
 *
 *  - The velvet is drawn "cover": scaled until it fills the screen with no bars, never stretched.
 *    In landscape the plate is rotated 90 degrees first. Crushed velvet has no direction, so the
 *    rotation is invisible, and a tall portrait plate covering a wide screen needs a far smaller
 *    zoom rotated than upright (1.3x instead of 3.6x on a 20:9 phone in landscape): that's what
 *    keeps the texture sharp on every orientation.
 *  - The glass Z is sized from the screen's smaller dimension (as in the photograph on a phone),
 *    capped so it stays a mark rather than a poster on tablets, and centred.
 */
object SplashLayout {

    /** Glass width as a fraction of the screen width - matches the photograph on a phone. */
    const val Z_WIDTH_OF_SCREEN = 0.45f
    /** On wide screens the height limits instead: glass width as a fraction of screen height. */
    const val Z_WIDTH_OF_HEIGHT = 0.36f
    /** Upper bound so the mark stays a mark on tablets and desktops (dp). */
    const val Z_MAX_WIDTH_DP = 330f

    data class Frame(val left: Float, val top: Float, val width: Float, val height: Float) {
        val centerX get() = left + width / 2f
        val centerY get() = top + height / 2f
        val right get() = left + width
        val bottom get() = top + height
    }

    /** Width of the visible glass on screen, px. */
    fun glassWidth(screenW: Float, screenH: Float, density: Float): Float =
        min(min(screenW * Z_WIDTH_OF_SCREEN, screenH * Z_WIDTH_OF_HEIGHT), Z_MAX_WIDTH_DP * density)

    /** The Z crop frame (glass + its padding) on screen, centred, px. */
    fun zFrame(screenW: Float, screenH: Float, density: Float): Frame {
        val scale = glassWidth(screenW, screenH, density) / SplashArtGeometry.Z_GLASS_WIDTH
        val w = SplashArtGeometry.Z_FRAME_WIDTH * scale
        val h = SplashArtGeometry.Z_FRAME_HEIGHT * scale
        return Frame(left = (screenW - w) / 2f, top = (screenH - h) / 2f, width = w, height = h)
    }

    /** True when the velvet plate should be drawn rotated by 90 degrees. */
    fun rotateVelvet(screenW: Float, screenH: Float): Boolean = screenW > screenH

    /** Base "cover" scale for the velvet plate (before overscan and zoom). */
    fun velvetCoverScale(plateW: Float, plateH: Float, screenW: Float, screenH: Float): Float {
        val rotated = rotateVelvet(screenW, screenH)
        val effW = if (rotated) plateH else plateW
        val effH = if (rotated) plateW else plateH
        return max(screenW / effW, screenH / effH)
    }
}
