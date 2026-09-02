package com.alijafari.red.astronomy.startracker.calibration

/**
 * Camera profile: fx, fy, cx, cy, skew, k1, k2, p1, p2, sampleCount, lastUpdated, deviceLensKey
 * Pure Kotlin, no Android dependency.
 *
 * @param fx focal length x in pixels
 * @param fy focal length y in pixels
 * @param cx principal point x in pixels
 * @param cy principal point y in pixels
 * @param skew skew coefficient (typically 0)
 * @param k1 radial distortion k1
 * @param k2 radial distortion k2
 * @param p1 tangential distortion p1
 * @param p2 tangential distortion p2
 * @param sampleCount number of observations used to refine this profile
 * @param lastUpdated timestamp seconds (from FakeClock or system)
 * @param deviceLensKey key identifying device+lens (e.g., "BACK_4.0_5.6x4.2")
 */
data class CameraProfile(
    val fx: Double,
    val fy: Double,
    val cx: Double,
    val cy: Double,
    val skew: Double = 0.0,
    val k1: Double = 0.0,
    val k2: Double = 0.0,
    val p1: Double = 0.0,
    val p2: Double = 0.0,
    val sampleCount: Int = 0,
    val lastUpdated: Double = 0.0,
    val deviceLensKey: String = "UNKNOWN"
) {
    companion object {
        fun fallbackDefault(width: Int = 1920, height: Int = 1080, fovYDeg: Double = 63.5): CameraProfile {
            // From Phase 1 consolidated fallback
            val fovYRad = Math.toRadians(fovYDeg)
            val fy = (height / 2.0) / kotlin.math.tan(fovYRad / 2.0)
            val fx = fy
            return CameraProfile(
                fx = fx,
                fy = fy,
                cx = width / 2.0,
                cy = height / 2.0,
                skew = 0.0,
                k1 = 0.0,
                k2 = 0.0,
                p1 = 0.0,
                p2 = 0.0,
                sampleCount = 0,
                lastUpdated = 0.0,
                deviceLensKey = "FALLBACK_DEFAULT"
            )
        }
    }
}
