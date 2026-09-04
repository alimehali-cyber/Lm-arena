package com.alijafari.red.astronomy.startracker.calibration

/**
 * Orchestrates: accumulate observations across locks -> refine -> update cached profile,
 * with minimum-sample-count gate before trusting refined profile over existing fallback.
 */

class SelfCalibrationEngine(
    val cache: CameraProfileCache = InMemoryCameraProfileCache(),
    val intrinsicsRefiner: IntrinsicsRefiner = IntrinsicsRefiner(),
    val distortionRefiner: DistortionRefiner = DistortionRefiner(),
    val minSamplesForIntrinsics: Int = 20, // minimum observations before trusting refined intrinsics
    val minSamplesForDistortion: Int = 50, // need more for distortion (unobservable near center)
    val deviceLensKey: String = "DEFAULT"
) {

    private val accumulatedIntrinsicsObservations = mutableListOf<ObservationPair>()
    private val accumulatedDistortionObservations = mutableListOf<DistortionObservation>()

    /**
     * Accumulate observation batch from successful star-solver locks.
     * Each batch contains pairs of predicted ideal pixel (from catalog+attitude+current intrinsics) and observed pixel.
     */
    fun accumulateIntrinsicsBatch(observations: List<ObservationPair>) {
        accumulatedIntrinsicsObservations.addAll(observations)
    }

    fun accumulateDistortionBatch(observations: List<DistortionObservation>) {
        accumulatedDistortionObservations.addAll(observations)
    }

    /**
     * Attempt refinement if enough samples accumulated.
     * Returns refined profile if successful and meets minimum sample count gate, else existing fallback.
     */
    fun tryRefineIntrinsics(currentProfile: CameraProfile): CameraProfile {
        if (accumulatedIntrinsicsObservations.size < minSamplesForIntrinsics) {
            return currentProfile // not enough samples, keep existing
        }

        val result = intrinsicsRefiner.refine(currentProfile, accumulatedIntrinsicsObservations)

        if (!result.success) {
            return currentProfile // refinement declined (insufficient data or poorly distributed)
        }

        // Update cache via merge (weighted by sample count)
        cache.merge(deviceLensKey, result.refinedProfile, accumulatedIntrinsicsObservations.size)

        // Audit finding B8: clear the accumulation buffer after a successful refinement.
        // Previously the buffer was never cleared here, so the next selfCalibrate() cycle
        // re-refined and re-merged the SAME observations on top of the already-updated
        // profile, double-counting stale data indefinitely.
        accumulatedIntrinsicsObservations.clear()

        return result.refinedProfile
    }

    fun tryRefineDistortion(currentProfile: CameraProfile): CameraProfile {
        if (accumulatedDistortionObservations.size < minSamplesForDistortion) {
            return currentProfile
        }

        val initialModel = DistortionModel(currentProfile.k1, currentProfile.k2, currentProfile.p1, currentProfile.p2)
        val result = distortionRefiner.refine(initialModel, accumulatedDistortionObservations)

        if (!result.success) {
            return currentProfile
        }

        val refinedProfile = currentProfile.copy(
            k1 = result.refinedModel.k1,
            k2 = result.refinedModel.k2,
            p1 = result.refinedModel.p1,
            p2 = result.refinedModel.p2,
            sampleCount = currentProfile.sampleCount + accumulatedDistortionObservations.size
        )

        cache.merge(deviceLensKey, refinedProfile, accumulatedDistortionObservations.size)

        // Audit finding B8: same clear-after-success contract as tryRefineIntrinsics above.
        accumulatedDistortionObservations.clear()

        return refinedProfile
    }

    /**
     * Full self-calibration: refine intrinsics first (distortion fixed zero), then distortion with refined intrinsics fixed.
     * More numerically stable and easier to verify than joint nonlinear solve.
     */
    fun selfCalibrate(currentProfile: CameraProfile): CameraProfile {
        var profile = currentProfile

        // Stage 1: refine intrinsics with distortion fixed at zero
        profile = tryRefineIntrinsics(profile)

        // Stage 2: refine distortion with refined intrinsics fixed
        profile = tryRefineDistortion(profile)

        return profile
    }

    fun getCachedProfile(): CameraProfile? {
        return cache.get(deviceLensKey)
    }

    fun clearAccumulated() {
        accumulatedIntrinsicsObservations.clear()
        accumulatedDistortionObservations.clear()
    }

    fun getAccumulatedCounts(): Pair<Int, Int> {
        return Pair(accumulatedIntrinsicsObservations.size, accumulatedDistortionObservations.size)
    }
}
