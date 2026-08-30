package com.alijafari.red.astronomy.sandbox.render.renderer

/**
 * Theme configuration for the OpenGL rendering pipeline, ensuring compatibility with
 * ZIG Dark and Light UI themes.
 */
data class RenderTheme(
    val clearColorR: Float = 0.02f,
    val clearColorG: Float = 0.02f,
    val clearColorB: Float = 0.04f,
    val clearColorA: Float = 1.0f,
    val gridColorR: Float = 0.35f,
    val gridColorG: Float = 0.45f,
    val gridColorB: Float = 0.65f,
    val starfieldAlpha: Float = 1.0f,
    val isDarkTheme: Boolean = true
) {
    companion object {
        val DARK = RenderTheme(
            clearColorR = 0.015f,
            clearColorG = 0.020f,
            clearColorB = 0.035f,
            clearColorA = 1.0f,
            gridColorR = 0.30f,
            gridColorG = 0.40f,
            gridColorB = 0.60f,
            starfieldAlpha = 1.0f,
            isDarkTheme = true
        )

        val LIGHT = RenderTheme(
            clearColorR = 0.92f,
            clearColorG = 0.94f,
            clearColorB = 0.97f,
            clearColorA = 1.0f,
            gridColorR = 0.50f,
            gridColorG = 0.55f,
            gridColorB = 0.65f,
            starfieldAlpha = 0.35f,
            isDarkTheme = false
        )
    }
}
