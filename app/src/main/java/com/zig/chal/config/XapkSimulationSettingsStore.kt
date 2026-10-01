package com.zig.chal.config

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the same physical controls and quality choice under the XAPK's `bh.params` preference
 * name, using its original keys and defaults wherever the Chal UI exposes the setting.
 */
class XapkSimulationSettingsStore(context: Context) {
    private val context: Context = context.applicationContext
    private val preferences: SharedPreferences =
        this.context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ChalSimulationParams {
        // XAPK default: Low for low-RAM/<3 GiB, Medium at 3–6 GiB, High at >=6 GiB.
        val fallbackQuality = defaultQuality()
        val quality = XapkRendererContract.chalQualityFor(preferences.getString(KEY_QUALITY, null))
            ?: fallbackQuality
        val defaults = defaultFeatures(quality)
        val features = ChalFeatureToggles(
            gravitationalLensing = preferences.getBoolean("enLens", defaults.gravitationalLensing),
            rayTracingQuality = quality,
            accretionDisk = preferences.getBoolean("enDisk", defaults.accretionDisk),
            dopplerBeaming = preferences.getBoolean("enDoppler", defaults.dopplerBeaming),
            backgroundStars = preferences.getBoolean("enStars", defaults.backgroundStars),
            photonSphereGlow = preferences.getBoolean("enPhoton", defaults.photonSphereGlow),
            // XAPK has no separate enable bit; bloom intensity is always forwarded independently.
            bloom = true,
            relativisticJets = preferences.getBoolean("enJets", defaults.relativisticJets),
            gravitationalRedshift = preferences.getBoolean("showRedshift", defaults.gravitationalRedshift),
            kerrShadow = false,
            spacetimeVisualization = false
        )

        return ChalSimulationParams(
            mass = getFloat("mass", 1.0f, 0.1f, 20.0f),
            spin = getFloat("spin", 0.5f, 0.0f, 0.99f),
            diskDensity = getFloat("diskDensity", 4.0f, 0.0f, 10.0f),
            diskTemp = getFloat("diskTemp", 9_500.0f, 2_000.0f, 25_000.0f),
            lensing = getFloat("lens", 0.7f, 0.0f, 2.0f),
            paused = false,
            zoom = ChalCameraConfig.DEFAULT_ZOOM,
            autoSpin = getFloat("autoSpin", 0.005f, -0.05f, 0.05f),
            diskSize = getFloat("diskSize", 50.0f, 10.0f, 120.0f),
            diskScaleHeight = getFloat("diskScale", 0.2f, 0.05f, 0.45f),
            frameDraggingStrength = getFloat("frameDrag", 2.0f, 0.0f, 4.0f),
            bloomThreshold = getFloat("bloomThr", 1.0f, 0.0f, 4.0f),
            bloomIntensity = getFloat("bloomInt", 0.45f, 0.0f, 2.0f),
            adaptiveResolution = false,
            renderScale = XapkRendererContract.qualityFor(quality).renderScale.toDouble(),
            features = features,
            performancePreset = ChalFeatures.matchesPreset(features),
            verticalAngle = XapkCameraState.DEFAULT_POLAR_ANGLE_DEGREES
        )
    }

    fun save(params: ChalSimulationParams) {
        val quality = XapkRendererContract.qualityFor(params.features.rayTracingQuality)
        preferences.edit()
            .putFloat("mass", params.mass.toFloat())
            .putFloat("spin", params.spin.toFloat())
            .putFloat("lens", params.lensing.toFloat())
            .putFloat("frameDrag", params.frameDraggingStrength.toFloat())
            .putFloat("diskDensity", params.diskDensity.toFloat())
            .putFloat("diskTemp", params.diskTemp.toFloat())
            .putFloat("diskSize", params.diskSize.toFloat())
            .putFloat("diskScale", params.diskScaleHeight.toFloat())
            .putFloat("bloomThr", params.bloomThreshold.toFloat())
            .putFloat("bloomInt", params.bloomIntensity.toFloat())
            .putFloat("autoSpin", params.autoSpin.toFloat())
            .putBoolean("enLens", params.features.gravitationalLensing)
            .putBoolean("enDisk", params.features.accretionDisk)
            .putBoolean("enDoppler", params.features.dopplerBeaming)
            .putBoolean("enPhoton", params.features.photonSphereGlow)
            .putBoolean("enStars", params.features.backgroundStars)
            .putBoolean("enJets", params.features.relativisticJets)
            .putBoolean("showRedshift", params.features.gravitationalRedshift)
            .putString(KEY_QUALITY, quality.preferenceName)
            .apply()
    }

    private fun getFloat(key: String, default: Float, minimum: Float, maximum: Float): Double =
        preferences.getFloat(key, default).coerceIn(minimum, maximum).toDouble()

    private fun defaultQuality(): ChalRayTracingQuality {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return ChalRayTracingQuality.LOW
        if (activityManager.isLowRamDevice) return ChalRayTracingQuality.LOW

        val memory = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)
        val totalGiB = memory.totalMem.toDouble() / BYTES_PER_GIB
        return when {
            totalGiB >= 6.0 -> ChalRayTracingQuality.HIGH
            totalGiB >= 3.0 -> ChalRayTracingQuality.MEDIUM
            else -> ChalRayTracingQuality.LOW
        }
    }

    private fun defaultFeatures(quality: ChalRayTracingQuality) = ChalFeatureToggles(
        gravitationalLensing = true,
        rayTracingQuality = quality,
        accretionDisk = true,
        dopplerBeaming = true,
        backgroundStars = true,
        photonSphereGlow = true,
        bloom = true,
        relativisticJets = false,
        gravitationalRedshift = false,
        kerrShadow = false,
        spacetimeVisualization = false
    )

    companion object {
        const val PREFERENCES_NAME = "bh.params"
        private const val KEY_QUALITY = "preset"
        private const val BYTES_PER_GIB = 1_073_741_824.0
    }
}
