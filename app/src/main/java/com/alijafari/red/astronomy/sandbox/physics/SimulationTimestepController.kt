package com.alijafari.red.astronomy.sandbox.physics

import kotlin.math.ceil
import kotlin.math.pow

/**
 * Manages simulation time progression, decoupling physical integration step size
 * from user-perceived playback speed multiplier.
 *
 * Implements emergency substepping for close encounters without violating fixed-step determinism.
 */
class SimulationTimestepController(
    /**
     * Base integration timestep in physical seconds (e.g., 3600.0s for planetary systems).
     */
    var baseTimestepSeconds: Double = DEFAULT_BASE_TIMESTEP_SECONDS,

    /**
     * Preset recommended base speed multiplier in physical seconds per real wall second.
     */
    var presetBaseSpeedMultiplier: Double = 86400.0,

    /**
     * User speed multiplier selection (1.0, 10.0, 100.0, 1000.0).
     */
    var userSpeedMultiplier: Double = 1.0,

    /**
     * Maximum number of physical sub-steps allowed per frame to protect CPU budget.
     */
    var maxSubstepsPerFrame: Int = 120
) {
    /**
     * Total effective speed multiplier (physical seconds per real wall second).
     */
    var timeSpeedMultiplier: Double
        get() = presetBaseSpeedMultiplier * userSpeedMultiplier
        set(value) {
            presetBaseSpeedMultiplier = value
            userSpeedMultiplier = 1.0
        }

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

        val isCloseEncounter = currentProximityRatio < closeEncounterThreshold
        val steps = ArrayList<Double>()

        // When accumulator is very small (e.g. paused or tiny step), wait for more time
        val minStepThreshold = (baseTimestepSeconds * 0.05).coerceAtLeast(0.01)
        if (timeAccumulatorSeconds < minStepThreshold) {
            return steps
        }

        // Calculate max allowable step dt to maintain symplectic Verlet stability
        val maxAllowedDt = if (isCloseEncounter) {
            (baseTimestepSeconds / closeEncounterSubdivisions).coerceAtLeast(0.1)
        } else {
            val userScaleFactor = userSpeedMultiplier.coerceAtLeast(1.0).pow(0.5).coerceIn(1.0, 32.0)
            baseTimestepSeconds * userScaleFactor
        }

        val neededSteps = ceil(timeAccumulatorSeconds / maxAllowedDt).toInt().coerceIn(1, maxSubstepsPerFrame)
        val stepDt = timeAccumulatorSeconds / neededSteps

        for (k in 0 until neededSteps) {
            steps.add(stepDt)
        }

        // Clear accumulator since the full target time is distributed into discrete steps
        timeAccumulatorSeconds = 0.0
        return steps
    }

    companion object {
        const val DEFAULT_BASE_TIMESTEP_SECONDS = 3600.0 // 1 hour per physics step default
    }
}
