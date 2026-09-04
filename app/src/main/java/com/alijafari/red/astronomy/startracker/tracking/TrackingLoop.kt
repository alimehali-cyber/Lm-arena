package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.LostInSpaceSolver
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.StarObservation
import com.alijafari.red.astronomy.startracker.solver.Vec3

/**
 * Tracking loop orchestration: gyro integration between locks, re-lock triggering, confidence state machine.
 * Maintains current attitude via gyro integration, triggers local or full re-lock per RelockPolicy,
 * updates ConfidenceStateMachine, exposes current best-estimate attitude + confidence.
 */

data class TrackingState(
    val attitude: Quaternion,
    val confidence: LockConfidence,
    val confidenceValue: Double,
    val lastLockAgeSeconds: Double
)

class TrackingLoop(
    val catalogStars: List<CatalogStar>,
    val quadIndex: QuadPatternIndex,
    val fakeClock: FakeClock = FakeClock(),
    val integrator: QuaternionIntegrator = QuaternionIntegrator(),
    val confidenceMachine: ConfidenceStateMachine = ConfidenceStateMachine(fakeClock),
    val relockPolicy: RelockPolicy = RelockPolicy(fakeClock),
    val lostInSpaceSolver: LostInSpaceSolver = LostInSpaceSolver(quadIndex, catalogStars),
    val localSearch: LocalRelockSearch = LocalRelockSearch(quadIndex, catalogStars, fullBlindSolver = lostInSpaceSolver)
) {

    var currentAttitude: Quaternion = Quaternion.identity()
        private set

    var currentState: TrackingState = TrackingState(
        attitude = currentAttitude,
        confidence = LockConfidence.NO_LOCK,
        confidenceValue = 0.0,
        lastLockAgeSeconds = 0.0
    )
        private set

    /**
     * Initialize with full lock via synthetic solver at known attitude
     */
    fun initializeWithLock(attitude: Quaternion) {
        currentAttitude = attitude
        confidenceMachine.onSolveResult(
            com.alijafari.red.astronomy.startracker.solver.SolveResult(
                success = true,
                attitude = attitude,
                inlierCount = 10,
                confidence = 1.0
            )
        )
        relockPolicy.onSuccessfulLock(attitude)
        updateState()
    }

    /**
     * On gyro sample: integrate attitude.
     * Audit finding B6: this previously advanced fakeClock(dt) explicitly AND then called
     * confidenceMachine.updateWithTime(dt), which advances the shared clock again — the clock
     * advanced 2xdt per gyro sample. The explicit advance is removed; updateWithTime(dt) is
     * the single clock-advancing call.
     */
    fun onGyroSample(angularVelocity: Vec3, dtSeconds: Double) {
        val newAttitude = integrator.integrate(currentAttitude, angularVelocity, dtSeconds)
        val deltaAngle = computeDeltaAngle(currentAttitude, newAttitude)
        currentAttitude = newAttitude
        relockPolicy.onGyroIntegration(deltaAngle)
        confidenceMachine.updateWithTime(dtSeconds)
        updateState()
    }

    /**
     * On new observations: attempt re-lock per policy
     */
    fun onNewObservations(observations: List<StarObservation>): TrackingState {
        val trigger = relockPolicy.shouldTriggerRelock()

        val solveResult = if (trigger != null || confidenceMachine.currentState == LockConfidence.NO_LOCK) {
            // Try local re-lock first if we have prior close to correct.
            // Audit finding B7: this previously re-wrapped the local search outcome in a
            // fabricated SolveResult with hardcoded inlierCount = 6 ("estimate") and
            // confidence = 0.8, reporting fictitious confidence numbers to the state
            // machine. LocalRelockSearch now carries the underlying REAL SolveResult
            // (from the local solver or the full blind fallback), which is used as-is.
            val localResult = localSearch.search(observations, currentAttitude)
            localResult.solveResult
                ?: com.alijafari.red.astronomy.startracker.solver.SolveResult(
                    success = false,
                    attitude = null,
                    inlierCount = 0,
                    confidence = 0.0,
                    errorMessage = "Local search returned no underlying solve result (honest failure, no fabrication)"
                )
        } else {
            // No trigger, continue gyro
            null
        }

        if (solveResult != null) {
            confidenceMachine.onSolveResult(solveResult)
            if (solveResult.success && solveResult.attitude != null) {
                // Check disagreement
                val disagreement = computeDeltaAngle(currentAttitude, solveResult.attitude)
                val shouldTriggerFull = relockPolicy.checkDisagreement(disagreement)

                if (shouldTriggerFull) {
                    // Sustained disagreement triggers full blind, already attempted fallback
                    // If still disagrees, go to ambiguous
                    confidenceMachine.onSustainedDisagreement()
                } else {
                    // Update attitude to solved
                    currentAttitude = solveResult.attitude
                    relockPolicy.onSuccessfulLock(solveResult.attitude)
                }
            }
        }

        updateState()
        return currentState
    }

    private fun updateState() {
        val age = fakeClock.now() - relockPolicy.lastLockTimeSeconds
        currentState = TrackingState(
            attitude = currentAttitude,
            confidence = confidenceMachine.currentState,
            confidenceValue = confidenceMachine.currentConfidence,
            lastLockAgeSeconds = age
        )
    }

    private fun computeDeltaAngle(q1: Quaternion, q2: Quaternion): Double {
        val dot = kotlin.math.abs(q1.w * q2.w + q1.x * q2.x + q1.y * q2.y + q1.z * q2.z).coerceIn(-1.0, 1.0)
        return 2 * kotlin.math.acos(dot)
    }
}

/**
 * Thin adapter that WOULD connect OrientationProvider's gyro data and CameraFrameObserver's frames
 * to TrackingLoop in real app — demonstrates wiring shape, not exercised against real sensors.
 *
 * This adapter is intentionally minimal and separate, per Task 5 guardrails.
 *
 * Finding: OrientationProvider's public SkyOrientation has timestampNanos field (added Phase 1),
 * but does NOT expose raw gyro angular velocity (Vec3) directly — it only exposes fused orientation.
 * To actually connect, OrientationProvider would need to expose raw gyro data or we need to use
 * separate SensorManager for gyro. CameraFrameObserver's public getUseCase() returns ImageAnalysis,
 * but does NOT expose a callback for frames as GrayscaleImage — would need adapter function
 * converting Y-plane to GrayscaleImage (as mentioned in Phase 2 Task 0 optional smoke test).
 *
 * So Phase 6 will need minimal changes to OrientationProvider to expose gyro, and CameraFrameObserver
 * to expose frame callback, or we need separate sensor listeners.
 */
class LiveSensorAdapter(
    val trackingLoop: TrackingLoop
) {
    /**
     * Would be called from OrientationProvider's gyro path
     */
    fun onGyroData(angularVelocity: Vec3, dtSeconds: Double) {
        trackingLoop.onGyroSample(angularVelocity, dtSeconds)
    }

    /**
     * Would be called from CameraFrameObserver when new frame available as GrayscaleImage
     * (requires adapter Y-plane -> GrayscaleImage, not yet implemented)
     */
    fun onNewFrame(observations: List<StarObservation>) {
        trackingLoop.onNewObservations(observations)
    }

    fun getCurrentState(): TrackingState = trackingLoop.currentState
}
