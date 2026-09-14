package com.zig.museum.core.model

import kotlin.math.*

/**
 * Sun-direction control per M5 task 2
 * Azimuth, elevation, EV with pure function from §10.3 and unit tests; per-object presets stored as data
 */

data class SunState(
    val azimuthDeg: Float = 0f, // 0..360, 0 = +Z, 90 = +X per SunLight.kt
    val elevationDeg: Float = 45f, // -90..90, 90 = +Y (overhead)
    val exposureEV: Float = 0f // -5..5 EV
)

data class SunPresets(
    val fullDisk: SunState = SunState(azimuthDeg = 0f, elevationDeg = 45f, exposureEV = 0f),
    val terminator: SunState = SunState(azimuthDeg = 90f, elevationDeg = 5f, exposureEV = 0f),
    val grazing: SunState = SunState(azimuthDeg = 0f, elevationDeg = 2f, exposureEV = 0.5f),
    val poleOn: SunState = SunState(azimuthDeg = 0f, elevationDeg = 89f, exposureEV = -0.5f)
)

object SunControl {

    /**
     * Pure function sunDirection(azimuthDeg, elevationDeg): Vec3 per §10.3
     * Already implemented in SunLight.kt, here we provide wrapper and presets
     */

    fun directionFromState(state: SunState): Triple<Float, Float, Float> {
        val azRad = Math.toRadians(state.azimuthDeg.toDouble())
        val elRad = Math.toRadians(state.elevationDeg.toDouble())
        val cosEl = cos(elRad)
        val x = cosEl * sin(azRad)
        val y = sin(elRad)
        val z = cosEl * cos(azRad)
        return Triple(x.toFloat(), y.toFloat(), z.toFloat())
    }

    /**
     * Per-object presets stored as data per M5 task 2
     */
    val perObjectPresets: Map<String, SunPresets> = mapOf(
        "moon" to SunPresets(
            fullDisk = SunState(0f, 45f, 0f),
            terminator = SunState(90f, 5f, 0f),
            grazing = SunState(0f, 2f, 0.5f),
            poleOn = SunState(0f, 89f, -0.5f)
        ),
        "earth" to SunPresets(
            fullDisk = SunState(0f, 30f, 0f),
            terminator = SunState(90f, 10f, 0f),
            grazing = SunState(0f, 5f, 0.3f),
            poleOn = SunState(0f, 80f, 0f)
        ),
        "mars" to SunPresets(
            fullDisk = SunState(0f, 35f, 0f),
            terminator = SunState(90f, 8f, 0f),
            grazing = SunState(0f, 3f, 0.5f),
            poleOn = SunState(0f, 85f, -0.3f)
        )
        // Other objects use default presets
    )

    fun presetFor(objectId: String): SunPresets {
        return perObjectPresets[objectId.lowercase()] ?: SunPresets()
    }
}
