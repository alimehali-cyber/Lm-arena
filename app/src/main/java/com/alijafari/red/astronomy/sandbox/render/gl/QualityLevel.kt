package com.alijafari.red.astronomy.sandbox.render.gl

/**
 * Quality levels for rendering scalability across devices.
 * High-end devices render high-tessellation meshes and full starfields;
 * lower-tier devices gracefully scale down geometry and sample counts
 * without impacting physics accuracy.
 */
enum class QualityLevel(
    val sphereRings: Int,
    val sphereSectors: Int,
    val starCount: Int,
    val maxTrailPointsPerBody: Int,
    val enableMsaa: Boolean
) {
    LOW(
        sphereRings = 16,
        sphereSectors = 16,
        starCount = 1500,
        maxTrailPointsPerBody = 60,
        enableMsaa = false
    ),
    MEDIUM(
        sphereRings = 28,
        sphereSectors = 28,
        starCount = 3500,
        maxTrailPointsPerBody = 120,
        enableMsaa = false
    ),
    HIGH(
        sphereRings = 48,
        sphereSectors = 48,
        starCount = 7000,
        maxTrailPointsPerBody = 240,
        enableMsaa = true
    )
}
