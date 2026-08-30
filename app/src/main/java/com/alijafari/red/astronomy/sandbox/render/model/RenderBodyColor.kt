package com.alijafari.red.astronomy.sandbox.render.model

import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType

/**
 * Visual color palette and emissive flags for diagnostic body rendering.
 */
object RenderBodyColor {
    val DEFAULT_COLOR = floatArrayOf(0.8f, 0.8f, 0.8f, 1.0f)

    fun getColorForBodyType(type: SandboxBodyType): FloatArray {
        return when (type) {
            SandboxBodyType.SUN -> floatArrayOf(1.00f, 0.85f, 0.32f, 1.0f) // Solar Golden
            SandboxBodyType.MERCURY -> floatArrayOf(0.70f, 0.74f, 0.77f, 1.0f) // Mercury Slate
            SandboxBodyType.VENUS -> floatArrayOf(0.95f, 0.82f, 0.50f, 1.0f) // Venus Pale Ochre
            SandboxBodyType.EARTH -> floatArrayOf(0.26f, 0.65f, 0.96f, 1.0f) // Earth Ocean Azure
            SandboxBodyType.MOON -> floatArrayOf(0.80f, 0.84f, 0.86f, 1.0f) // Lunar Silvery
            SandboxBodyType.MARS -> floatArrayOf(0.95f, 0.44f, 0.26f, 1.0f) // Martian Red Rust
            SandboxBodyType.JUPITER -> floatArrayOf(0.92f, 0.72f, 0.42f, 1.0f) // Jovian Amber Band
            SandboxBodyType.SATURN -> floatArrayOf(0.95f, 0.86f, 0.58f, 1.0f) // Saturn Golden
            SandboxBodyType.URANUS -> floatArrayOf(0.50f, 0.87f, 0.92f, 1.0f) // Uranian Aquamarine
            SandboxBodyType.NEPTUNE -> floatArrayOf(0.16f, 0.65f, 0.96f, 1.0f) // Neptune Deep Azure
            SandboxBodyType.BLACK_HOLE -> floatArrayOf(0.12f, 0.06f, 0.22f, 1.0f) // Singularity Dark Violet
            SandboxBodyType.THEORETICAL_WORMHOLE -> floatArrayOf(0.00f, 0.90f, 1.00f, 1.0f) // Theoretical Cyan
            SandboxBodyType.ASTEROID -> floatArrayOf(0.56f, 0.64f, 0.68f, 1.0f) // Carbonaceous Slate
            SandboxBodyType.CUSTOM_BODY -> floatArrayOf(0.89f, 0.20f, 0.20f, 1.0f) // Accent Red
        }
    }

    fun isEmissive(type: SandboxBodyType): Boolean {
        return when (type) {
            SandboxBodyType.SUN,
            SandboxBodyType.THEORETICAL_WORMHOLE -> true
            else -> false
        }
    }
}
