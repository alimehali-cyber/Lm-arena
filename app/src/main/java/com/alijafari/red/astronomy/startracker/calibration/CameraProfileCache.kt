package com.alijafari.red.astronomy.startracker.calibration

/**
 * Pure-Kotlin interface for camera profile cache + in-memory reference implementation.
 * Real Android SharedPreferences-backed implementation is STUB/DESIGN ONLY in this phase.
 */

interface CameraProfileCache {
    fun get(deviceLensKey: String): CameraProfile?
    fun put(deviceLensKey: String, profile: CameraProfile)
    fun merge(deviceLensKey: String, newProfile: CameraProfile, newSampleCount: Int)
}

/**
 * In-memory reference implementation for testing.
 * Merge strategy: running average weighted by each batch's SAMPLE COUNT ONLY.
 * Example: existing profile has 100 samples, new batch has 20 samples, merged = (100*existing + 20*new)/120.
 *
 * HONEST scope of the weighting (audit finding B9): the weights are counts, NOT quality.
 * A "bad" (noisy) batch is down-weighted only insofar as it is SMALL; a large noisy batch
 * influences the merged profile exactly as much as an equally large clean batch, because
 * no residual/variance signal enters this computation anywhere. Claims that this merge
 * "down-weights bad batches (high noise)" were incorrect and have been corrected wherever
 * they appeared. Quality-aware weighting (e.g., folding a per-batch mean residual into the
 * weight) remains future work and would require threading a residual metric through
 * CameraProfile and the refinement pipeline.
 */
class InMemoryCameraProfileCache : CameraProfileCache {

    private val cache = mutableMapOf<String, CameraProfile>()

    override fun get(deviceLensKey: String): CameraProfile? {
        return cache[deviceLensKey]
    }

    override fun put(deviceLensKey: String, profile: CameraProfile) {
        cache[deviceLensKey] = profile
    }

    override fun merge(deviceLensKey: String, newProfile: CameraProfile, newSampleCount: Int) {
        val existing = cache[deviceLensKey]
        if (existing == null) {
            cache[deviceLensKey] = newProfile.copy(sampleCount = newSampleCount)
            return
        }

        val totalSamples = existing.sampleCount + newSampleCount
        if (totalSamples == 0) {
            cache[deviceLensKey] = newProfile
            return
        }

        // Weighted average
        val wExisting = existing.sampleCount.toDouble() / totalSamples
        val wNew = newSampleCount.toDouble() / totalSamples

        val merged = CameraProfile(
            fx = existing.fx * wExisting + newProfile.fx * wNew,
            fy = existing.fy * wExisting + newProfile.fy * wNew,
            cx = existing.cx * wExisting + newProfile.cx * wNew,
            cy = existing.cy * wExisting + newProfile.cy * wNew,
            skew = existing.skew * wExisting + newProfile.skew * wNew,
            k1 = existing.k1 * wExisting + newProfile.k1 * wNew,
            k2 = existing.k2 * wExisting + newProfile.k2 * wNew,
            p1 = existing.p1 * wExisting + newProfile.p1 * wNew,
            p2 = existing.p2 * wExisting + newProfile.p2 * wNew,
            sampleCount = totalSamples,
            lastUpdated = maxOf(existing.lastUpdated, newProfile.lastUpdated),
            deviceLensKey = deviceLensKey
        )

        cache[deviceLensKey] = merged
    }

    fun clear() {
        cache.clear()
    }

    fun size(): Int = cache.size
}

/**
 * DESIGN ONLY — Real Android implementation would back this with SharedPreferences or small file-based store,
 * keyed by device+lens identifier derived from CameraCharacteristics (e.g., lens facing + focal length + sensor size hash)
 *
 * Example design (not production code in this phase):
 *
 * class SharedPrefsCameraProfileCache(context: Context) : CameraProfileCache {
 *     private val prefs = context.getSharedPreferences("camera_profiles", Context.MODE_PRIVATE)
 *
 *     private fun deviceLensKeyFromCharacteristics(chars: CameraCharacteristics): String {
 *         val facing = chars.get(CameraCharacteristics.LENS_FACING)
 *         val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
 *         val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
 *         // Hash combination: facing + focalLength + sensorSize
 *         return "FACING_${facing}_F_${focalLengths?.firstOrNull()}_S_${sensorSize?.width}x${sensorSize?.height}"
 *     }
 *
 *     override fun get(deviceLensKey: String): CameraProfile? {
 *         val json = prefs.getString(deviceLensKey, null) ?: return null
 *         // Parse JSON to CameraProfile
 *     }
 *
 *     override fun put(deviceLensKey: String, profile: CameraProfile) {
 *         // Serialize to JSON and put in prefs
 *     }
 *
 *     override fun merge(...) { // same weighted average logic }
 * }
 *
 * This is pointer for Phase 7's gated live-wiring task or future phase, not implemented against real Android APIs now.
 */
