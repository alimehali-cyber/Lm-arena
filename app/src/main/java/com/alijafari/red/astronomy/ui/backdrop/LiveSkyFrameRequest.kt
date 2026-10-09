package com.alijafari.red.astronomy.ui.backdrop

import com.alijafari.red.astronomy.ui.rendering.SkySceneModel

/**
 * The inputs that determine one Live Sky computation. This is a pure value.
 *
 * The backdrop keys its astronomy `produceState` on this value. Data-class equality therefore decides
 * whether work is redone: a clock tick inside the same quantised minute yields an equal request and no
 * recomputation. A hidden backdrop yields a request with [needsCompute] = false, so nothing is computed.
 */
data class LiveSkyFrameRequest(
    val visible: Boolean,
    val minuteMs: Long,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val elevationM: Double
) {
    val needsCompute: Boolean
        get() = visible

    companion object {
        /**
         * Chooses the instant the sky is computed for. Simulation mode uses the time-machine instant, and LIVE mode
         * uses the wall clock. The result is floored to [SkySceneModel.TIME_QUANTUM_MS].
         */
        fun resolveMinuteMs(isSimulation: Boolean, simulationTimeMs: Long, wallClockMs: Long): Long =
            SkySceneModel.quantizeTimeMs(if (isSimulation) simulationTimeMs else wallClockMs)
    }
}
