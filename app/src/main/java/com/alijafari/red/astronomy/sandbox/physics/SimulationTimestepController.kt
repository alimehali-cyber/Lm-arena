package com.alijafari.red.astronomy.sandbox.physics

/**
 * Manages simulation time progression, decoupling physical integration step size
 * from user-perceived playback speed multiplier.
 *
 * Implements emergency substepping for close encounters without violating fixed-step determinism.
 */
class SimulationTimestepController(
    /**
     * Base integration timestep in physical seconds (e.g., 60.0s for planetary systems).
     */
    var baseTimestepSeconds: Double = DEFAULT_BASE_TIMESTEP_SECONDS,

    /**
     * User playback time multiplier (e.g., 1.0 = real-time, 1000.0 = 1000x real-time).
     */
    var timeSpeedMultiplier: Double = 1.0,

    /**
     * Maximum number of physical sub-steps allowed per frame to protect CPU budget.
     */
    var maxSubstepsPerFrame: Int = 100
) {
    /**
     * Accumulated fractional remainder of physical time to step across frames.
     */
    private var timeAccumulatorSeconds: Double = 0.0

    /**
     * Close encounter proximity threshold (r / (R1 + R2)) triggering adaptive micro-substepping.
     */
    var closeEncounterThreshold: Double = 3.0

    /**
     * Micro-substepping division factor during close encounters.
     */
    var closeEncounterSubdivisions: Int = 5

    fun reset() {
        timeAccumulatorSeconds = 0.0
    }

    /**
     * Plans the execution step sizes for a given wall-clock delta time (dtWallClockSeconds).
     * Returns a list of discrete integration timesteps to execute.
     */
    fun computeSteps(
        dtWallClockSeconds: Double,
        currentProximityRatio: Double
    ): List<Double> {
        val targetSimDelta = dtWallClockSeconds * timeSpeedMultiplier
        timeAccumulatorSeconds += targetSimDelta

        val steps = ArrayList<Double>()
        var remainingTime = timeAccumulatorSeconds
        val baseStep = baseTimestepSeconds

        val isCloseEncounter = currentProximityRatio < closeEncounterThreshold

        while (remainingTime >= baseStep && steps.size < maxSubstepsPerFrame) {
            if (isCloseEncounter) {
                val microDt = baseStep / closeEncounterSubdivisions
                for (k in 0 until closeEncounterSubdivisions) {
                    if (steps.size < maxSubstepsPerFrame) {
                        steps.add(microDt)
                    }
                }
            } else {
                steps.add(baseStep)
            }
            remainingTime -= baseStep
        }

        // Keep unstepped remainder for next frame
        timeAccumulatorSeconds = remainingTime
        return steps
    }

    companion object {
        const val DEFAULT_BASE_TIMESTEP_SECONDS = 60.0 // 1 minute per physics step
    }
}
