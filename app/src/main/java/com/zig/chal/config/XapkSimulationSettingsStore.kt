package com.zig.chal.config

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences

/**
 * Persisted-field key names, shared with the XAPK's own `bh.params` preference file.
 *
 * The physical-control keys are the XAPK's originals, so a device that had the reference app installed
 * restores the same values here. The camera/pause keys are Chal's own additions to that file; the
 * reference never writes them, so reading them back can never conflict with it.
 */
object XapkSettingsKeys {
    const val STORE_NAME = "bh.params"

    const val QUALITY = "preset"
    const val MASS = "mass"
    const val SPIN = "spin"
    const val LENSING = "lens"
    const val FRAME_DRAG = "frameDrag"
    const val DISK_DENSITY = "diskDensity"
    const val DISK_TEMP = "diskTemp"
    const val DISK_SIZE = "diskSize"
    const val DISK_SCALE = "diskScale"
    const val BLOOM_THRESHOLD = "bloomThr"
    const val BLOOM_INTENSITY = "bloomInt"
    const val AUTO_SPIN = "autoSpin"

    const val ENABLE_LENSING = "enLens"
    const val ENABLE_DISK = "enDisk"
    const val ENABLE_DOPPLER = "enDoppler"
    const val ENABLE_PHOTON = "enPhoton"
    const val ENABLE_STARS = "enStars"
    const val ENABLE_JETS = "enJets"
    const val SHOW_REDSHIFT = "showRedshift"

    /** Chal additions (absent in the reference app; the defaults keep it out of its way). */
    const val ENABLE_BLOOM = "enBloom"
    const val KERR_SHADOW = "kerrShadow"
    const val SPACETIME = "spacetime"
    const val CAMERA_YAW = "cameraYaw"
    const val CAMERA_PITCH = "cameraPitch"
    const val CAMERA_DISTANCE = "cameraDistance"
    const val PAUSED = "paused"
}

/**
 * Storage surface the codec writes through, so the mapping can be unit-tested on the JVM and the
 * SharedPreferences dependency stays in exactly one place.
 */
interface ChalSettingsSink {
    fun contains(key: String): Boolean
    fun getFloat(key: String, default: Float): Float
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getString(key: String): String?
    fun putFloat(key: String, value: Float)
    fun putBoolean(key: String, value: Boolean)
    fun putString(key: String, value: String)
    fun commit()
}

/**
 * Pure mapping between [ChalSimulationParams] and the persisted key set.
 *
 * Loading is layered on purpose:
 *  1. keys the session itself wrote always win;
 *  2. otherwise the per-module defaults for the resolved quality tier apply;
 *  3. the quality tier comes from the stored preference, else the device's memory class.
 *
 * That ordering is what lets a saved session move between the Vulkan backend, the GLES fallback and a
 * reinstall without silently resetting the operator's toggles.
 */
object XapkSettingsCodec {

    fun load(sink: ChalSettingsSink, defaultQuality: ChalRayTracingQuality): ChalSimulationParams {
        val storedQuality = XapkRendererContract.chalQualityFor(sink.getString(XapkSettingsKeys.QUALITY))
        val quality = storedQuality ?: defaultQuality
        val defaults = defaultFeatures(quality)

        val features = ChalFeatureToggles(
            gravitationalLensing = sink.getBoolean(XapkSettingsKeys.ENABLE_LENSING, defaults.gravitationalLensing),
            rayTracingQuality = quality,
            accretionDisk = sink.getBoolean(XapkSettingsKeys.ENABLE_DISK, defaults.accretionDisk),
            dopplerBeaming = sink.getBoolean(XapkSettingsKeys.ENABLE_DOPPLER, defaults.dopplerBeaming),
            backgroundStars = sink.getBoolean(XapkSettingsKeys.ENABLE_STARS, defaults.backgroundStars),
            photonSphereGlow = sink.getBoolean(XapkSettingsKeys.ENABLE_PHOTON, defaults.photonSphereGlow),
            bloom = sink.getBoolean(XapkSettingsKeys.ENABLE_BLOOM, defaults.bloom),
            relativisticJets = sink.getBoolean(XapkSettingsKeys.ENABLE_JETS, defaults.relativisticJets),
            gravitationalRedshift = sink.getBoolean(XapkSettingsKeys.SHOW_REDSHIFT, defaults.gravitationalRedshift),
            kerrShadow = sink.getBoolean(XapkSettingsKeys.KERR_SHADOW, defaults.kerrShadow),
            spacetimeVisualization = sink.getBoolean(XapkSettingsKeys.SPACETIME, defaults.spacetimeVisualization)
        )

        // A file written before this screen persisted its camera has no camera keys at all; those
        // sessions start framed exactly like a fresh install (double-precision defaults, so the tilt
        // round-trips through `verticalAngle` without the float constants' 4.5e-6-degree drift).
        val yaw = if (sink.contains(XapkSettingsKeys.CAMERA_YAW)) {
            sink.getFloat(XapkSettingsKeys.CAMERA_YAW, XapkCameraState.DEFAULT_YAW).toDouble()
        } else {
            XapkCameraState.DEFAULT_YAW_DOUBLE
        }
        val pitch = if (sink.contains(XapkSettingsKeys.CAMERA_PITCH)) {
            sink.getFloat(XapkSettingsKeys.CAMERA_PITCH, XapkCameraState.DEFAULT_PITCH).toDouble()
        } else {
            XapkCameraState.DEFAULT_PITCH_DOUBLE
        }
        val distance = if (sink.contains(XapkSettingsKeys.CAMERA_DISTANCE)) {
            sink.getFloat(XapkSettingsKeys.CAMERA_DISTANCE, ChalCameraConfig.DEFAULT_ZOOM.toFloat()).toDouble()
        } else {
            ChalCameraConfig.DEFAULT_ZOOM
        }

        return ChalSimulationParams(
            mass = clamp(sink.getFloat(XapkSettingsKeys.MASS, 1.0f), 0.1f, 20.0f).toDouble(),
            spin = clamp(sink.getFloat(XapkSettingsKeys.SPIN, 0.5f), 0.0f, 0.99f).toDouble(),
            diskDensity = clamp(sink.getFloat(XapkSettingsKeys.DISK_DENSITY, 4.0f), 0.0f, 10.0f).toDouble(),
            diskTemp = clamp(sink.getFloat(XapkSettingsKeys.DISK_TEMP, 9_500.0f), 2_000.0f, 25_000.0f).toDouble(),
            lensing = clamp(sink.getFloat(XapkSettingsKeys.LENSING, 0.7f), 0.0f, 2.0f).toDouble(),
            paused = sink.getBoolean(XapkSettingsKeys.PAUSED, false),
            autoSpin = clamp(sink.getFloat(XapkSettingsKeys.AUTO_SPIN, 0.005f), -0.05f, 0.05f).toDouble(),
            diskSize = clamp(sink.getFloat(XapkSettingsKeys.DISK_SIZE, 50.0f), 10.0f, 120.0f).toDouble(),
            diskScaleHeight = clamp(sink.getFloat(XapkSettingsKeys.DISK_SCALE, 0.2f), 0.05f, 0.45f).toDouble(),
            frameDraggingStrength = clamp(sink.getFloat(XapkSettingsKeys.FRAME_DRAG, 2.0f), 0.0f, 4.0f).toDouble(),
            bloomThreshold = clamp(sink.getFloat(XapkSettingsKeys.BLOOM_THRESHOLD, 1.0f), 0.0f, 4.0f).toDouble(),
            bloomIntensity = clamp(sink.getFloat(XapkSettingsKeys.BLOOM_INTENSITY, 0.45f), 0.0f, 2.0f).toDouble(),
            adaptiveResolution = false,
            renderScale = XapkRendererContract.qualityFor(quality).renderScale.toDouble(),
            features = features,
            performancePreset = ChalFeatures.matchesPreset(features)
        ).withCamera(yaw, pitch, distance)
    }

    fun save(sink: ChalSettingsSink, params: ChalSimulationParams) {
        val quality = XapkRendererContract.qualityFor(params.features.rayTracingQuality)
        sink.putFloat(XapkSettingsKeys.MASS, params.mass.toFloat())
        sink.putFloat(XapkSettingsKeys.SPIN, params.spin.toFloat())
        sink.putFloat(XapkSettingsKeys.LENSING, params.lensing.toFloat())
        sink.putFloat(XapkSettingsKeys.FRAME_DRAG, params.frameDraggingStrength.toFloat())
        sink.putFloat(XapkSettingsKeys.DISK_DENSITY, params.diskDensity.toFloat())
        sink.putFloat(XapkSettingsKeys.DISK_TEMP, params.diskTemp.toFloat())
        sink.putFloat(XapkSettingsKeys.DISK_SIZE, params.diskSize.toFloat())
        sink.putFloat(XapkSettingsKeys.DISK_SCALE, params.diskScaleHeight.toFloat())
        sink.putFloat(XapkSettingsKeys.BLOOM_THRESHOLD, params.bloomThreshold.toFloat())
        sink.putFloat(XapkSettingsKeys.BLOOM_INTENSITY, params.bloomIntensity.toFloat())
        sink.putFloat(XapkSettingsKeys.AUTO_SPIN, params.autoSpin.toFloat())
        sink.putBoolean(XapkSettingsKeys.ENABLE_LENSING, params.features.gravitationalLensing)
        sink.putBoolean(XapkSettingsKeys.ENABLE_DISK, params.features.accretionDisk)
        sink.putBoolean(XapkSettingsKeys.ENABLE_DOPPLER, params.features.dopplerBeaming)
        sink.putBoolean(XapkSettingsKeys.ENABLE_PHOTON, params.features.photonSphereGlow)
        sink.putBoolean(XapkSettingsKeys.ENABLE_STARS, params.features.backgroundStars)
        sink.putBoolean(XapkSettingsKeys.ENABLE_JETS, params.features.relativisticJets)
        sink.putBoolean(XapkSettingsKeys.SHOW_REDSHIFT, params.features.gravitationalRedshift)
        sink.putBoolean(XapkSettingsKeys.ENABLE_BLOOM, params.features.bloom)
        sink.putBoolean(XapkSettingsKeys.KERR_SHADOW, params.features.kerrShadow)
        sink.putBoolean(XapkSettingsKeys.SPACETIME, params.features.spacetimeVisualization)
        sink.putFloat(XapkSettingsKeys.CAMERA_YAW, params.cameraYaw.toFloat())
        sink.putFloat(XapkSettingsKeys.CAMERA_PITCH, params.cameraPitch.toFloat())
        sink.putFloat(XapkSettingsKeys.CAMERA_DISTANCE, params.zoom.toFloat())
        sink.putBoolean(XapkSettingsKeys.PAUSED, params.paused)
        sink.putString(XapkSettingsKeys.QUALITY, quality.preferenceName)
        sink.commit()
    }

    /**
     * First-run feature defaults for a memory tier.
     *
     * The reference app enables lensing, disk, Doppler, photon ring, stars and bloom at every tier, so
     * the high tier reproduces that exactly. The cheaper tiers drop the post-process-heavy modules -
     * the ones that cost frames without adding physics - which is what their tier labels claim.
     */
    fun defaultFeatures(quality: ChalRayTracingQuality): ChalFeatureToggles {
        val full = quality == ChalRayTracingQuality.HIGH || quality == ChalRayTracingQuality.ULTRA
        return ChalFeatureToggles(
            gravitationalLensing = true,
            rayTracingQuality = quality,
            accretionDisk = true,
            dopplerBeaming = full,
            backgroundStars = true,
            photonSphereGlow = full,
            bloom = full,
            relativisticJets = false,
            gravitationalRedshift = false,
            kerrShadow = false,
            spacetimeVisualization = false
        )
    }

    private fun clamp(value: Float, minimum: Float, maximum: Float): Float =
        if (value.isFinite()) value.coerceIn(minimum, maximum) else minimum
}

/** Persists the physical controls and the session camera under the XAPK's `bh.params` name. */
class XapkSimulationSettingsStore(context: Context) {
    private val context: Context = context.applicationContext
    private val preferences: SharedPreferences =
        this.context.getSharedPreferences(XapkSettingsKeys.STORE_NAME, Context.MODE_PRIVATE)
    private val sink = SharedPreferencesSink(preferences)

    fun load(): ChalSimulationParams = XapkSettingsCodec.load(sink, defaultQuality())

    fun save(params: ChalSimulationParams) = XapkSettingsCodec.save(sink, params)

    /**
     * The reference app's device default: Low for low-RAM/<3 GiB, Medium at 3-6 GiB, High at >=6 GiB.
     */
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

    companion object {
        const val PREFERENCES_NAME: String = XapkSettingsKeys.STORE_NAME
        private const val BYTES_PER_GIB = 1_073_741_824.0
    }
}

/** Batches one `save()` into a single editor transaction. */
private class SharedPreferencesSink(private val preferences: SharedPreferences) : ChalSettingsSink {
    private var editor: SharedPreferences.Editor? = null

    private fun edit(): SharedPreferences.Editor = editor ?: preferences.edit().also { editor = it }

    override fun contains(key: String): Boolean = preferences.contains(key)
    override fun getFloat(key: String, default: Float): Float = preferences.getFloat(key, default)
    override fun getBoolean(key: String, default: Boolean): Boolean = preferences.getBoolean(key, default)
    override fun getString(key: String): String? = preferences.getString(key, null)
    override fun putFloat(key: String, value: Float) { edit().putFloat(key, value) }
    override fun putBoolean(key: String, value: Boolean) { edit().putBoolean(key, value) }
    override fun putString(key: String, value: String) { edit().putString(key, value) }

    override fun commit() {
        editor?.apply()
        editor = null
    }
}
