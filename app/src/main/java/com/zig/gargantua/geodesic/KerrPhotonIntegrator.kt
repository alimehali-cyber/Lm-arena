package com.zig.gargantua.geodesic

import com.zig.gargantua.disk.AccretionDiskModel
import com.zig.gargantua.disk.DiskIntersection
import com.zig.gargantua.physics.KerrSchildCoordinates
import com.zig.gargantua.physics.KerrSchildDerivatives
import com.zig.gargantua.physics.KerrSchildSpacetime
import kotlin.math.*

/**
 * High-precision RK4 null geodesic integrator for photon trajectories in Kerr-Schild spacetime.
 *
 * HAMILTONIAN GEODESIC EQUATIONS:
 *   dx^i / dλ = g^{iν} p_ν
 *   dp_i / dλ = -1/2 (∂_i g^{αβ}) p_α p_β
 * with p_0 = -1.0 (constant due to stationarity ∂_T g^{αβ} = 0).
 */
class KerrPhotonIntegrator(
    val spacetime: KerrSchildSpacetime,
    val escapeRadius: Double = 60.0,
    val captureHorizonMargin: Double = 0.05,
    val maxSteps: Int = 1500,
    val baseStepFactor: Double = 0.08,
    val minStepSize: Double = 0.005,
    val maxStepSize: Double = 0.5,
    val disk: AccretionDiskModel? = null
) {
    enum class TerminationReason {
        CAPTURED,
        ESCAPED,
        DISK_HIT,
        MAX_STEPS_EXCEEDED,
        NUMERICAL_ERROR
    }

    data class RayTraceResult(
        val finalState: PhotonState4D,
        val terminationReason: TerminationReason,
        val stepsTaken: Int,
        val minRadiusReached: Double,
        val maxHamiltonianResidual: Double,
        val path: List<PhotonState4D>? = null,
        val minStepSizeTaken: Double = 0.0,
        val maxStepSizeTaken: Double = 0.0,
        val stepSizes: List<Double>? = null,
        val diskHit: DiskIntersection.DiskHitResult? = null
    ) {
        val isCaptured: Boolean get() = terminationReason == TerminationReason.CAPTURED
        val isEscaped: Boolean get() = terminationReason == TerminationReason.ESCAPED
        val isDiskHit: Boolean get() = terminationReason == TerminationReason.DISK_HIT
    }

    /**
     * Evaluates the right-hand side of Hamilton's equations for the 6D phase space:
     *   state = [X, Y, Z, p_X, p_Y, p_Z]
     */
    fun evaluateDerivatives(state: DoubleArray): DoubleArray {
        val x = state[0]
        val y = state[1]
        val z = state[2]
        val px = state[3]
        val py = state[4]
        val pz = state[5]
        val p0 = -1.0

        val p = doubleArrayOf(p0, px, py, pz)

        // Contravariant metric g^μν
        val gInv = spacetime.inverseMetric(x, y, z)

        // Exact analytical spatial derivatives ∂_i g^μν from Phase M3
        val dG = KerrSchildDerivatives.compute(spacetime.params, x, y, z)
        val dg_dX = dG.d_dX
        val dg_dY = dG.d_dY
        val dg_dZ = dG.d_dZ

        // 1. dx^i / dλ = g^{iν} p_ν
        var dX = 0.0
        var dY = 0.0
        var dZ = 0.0
        for (nu in 0 until 4) {
            val pNu = p[nu]
            dX += gInv[1, nu] * pNu
            dY += gInv[2, nu] * pNu
            dZ += gInv[3, nu] * pNu
        }

        // 2. dp_i / dλ = -1/2 (∂_i g^αβ) p_α p_β
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0
        for (alpha in 0 until 4) {
            val pA = p[alpha]
            for (beta in 0 until 4) {
                val pB = p[beta]
                val pTerm = pA * pB
                sumX += dg_dX[alpha, beta] * pTerm
                sumY += dg_dY[alpha, beta] * pTerm
                sumZ += dg_dZ[alpha, beta] * pTerm
            }
        }

        return doubleArrayOf(
            dX,
            dY,
            dZ,
            -0.5 * sumX,
            -0.5 * sumY,
            -0.5 * sumZ
        )
    }

    /**
     * Executes a single 4th-order Runge-Kutta step.
     */
    fun rk4Step(state: DoubleArray, dlambda: Double): DoubleArray {
        val k1 = evaluateDerivatives(state)

        val s2 = DoubleArray(6) { i -> state[i] + 0.5 * dlambda * k1[i] }
        val k2 = evaluateDerivatives(s2)

        val s3 = DoubleArray(6) { i -> state[i] + 0.5 * dlambda * k2[i] }
        val k3 = evaluateDerivatives(s3)

        val s4 = DoubleArray(6) { i -> state[i] + dlambda * k3[i] }
        val k4 = evaluateDerivatives(s4)

        val next = DoubleArray(6)
        val sixth = dlambda / 6.0
        for (i in 0 until 6) {
            next[i] = state[i] + sixth * (k1[i] + 2.0 * k2[i] + 2.0 * k3[i] + k4[i])
        }
        return next
    }

    /**
     * Computes the adaptive step size based on local radial coordinate r.
     * In strong gravitational fields near the photon sphere and horizon, step size shrinks
     * to maintain numerical accuracy and Hamiltonian conservation.
     */
    fun computeAdaptiveStep(r: Double, stepFactorOverride: Double? = null): Double {
        val factor = stepFactorOverride ?: baseStepFactor
        val rawStep = factor * r
        return rawStep.coerceIn(minStepSize, maxStepSize)
    }

    /**
     * Traces a photon ray from initial state until capture, escape, or timeout.
     */
    fun traceRay(
        initialState: PhotonState4D,
        recordPath: Boolean = false,
        fixedStepSize: Double? = null
    ): RayTraceResult {
        var state = doubleArrayOf(
            initialState.x,
            initialState.y,
            initialState.z,
            initialState.p_x,
            initialState.p_y,
            initialState.p_z
        )

        val a = spacetime.a
        val rPlus = spacetime.rPlus
        val rCaptureThreshold = rPlus + captureHorizonMargin

        val initialR = KerrSchildCoordinates.computeR(a, state[0], state[1], state[2])
        var minR = initialR
        var maxHResidual = 0.0
        var currentLambda = initialState.affineLambda
        var currentT = initialState.t

        val pathList = if (recordPath) mutableListOf(initialState) else null
        val stepSizesList = if (recordPath) mutableListOf<Double>() else null
        var minStepTaken = Double.MAX_VALUE
        var maxStepTaken = 0.0

        var previousR = initialR
        var movingOutward = false

        for (step in 0 until maxSteps) {
            val r = KerrSchildCoordinates.computeR(a, state[0], state[1], state[2])
            if (r < minR) minR = r

            if (r > previousR) {
                movingOutward = true
            }
            previousR = r

            // Check Hamiltonian constraint
            val currentState = PhotonState4D(
                t = currentT,
                x = state[0],
                y = state[1],
                z = state[2],
                p_t = -1.0,
                p_x = state[3],
                p_y = state[4],
                p_z = state[5],
                affineLambda = currentLambda
            )
            val h = abs(currentState.hamiltonian(spacetime))
            if (h > maxHResidual) maxHResidual = h

            // 1. Termination condition: Capture by event horizon
            if (r <= rCaptureThreshold) {
                return RayTraceResult(
                    finalState = currentState,
                    terminationReason = TerminationReason.CAPTURED,
                    stepsTaken = step,
                    minRadiusReached = minR,
                    maxHamiltonianResidual = maxHResidual,
                    path = pathList,
                    minStepSizeTaken = if (minStepTaken == Double.MAX_VALUE) 0.0 else minStepTaken,
                    maxStepSizeTaken = maxStepTaken,
                    stepSizes = stepSizesList
                )
            }

            // 2. Termination condition: Escape to asymptotic background
            if (r >= escapeRadius && (movingOutward || step > 20)) {
                return RayTraceResult(
                    finalState = currentState,
                    terminationReason = TerminationReason.ESCAPED,
                    stepsTaken = step,
                    minRadiusReached = minR,
                    maxHamiltonianResidual = maxHResidual,
                    path = pathList,
                    minStepSizeTaken = if (minStepTaken == Double.MAX_VALUE) 0.0 else minStepTaken,
                    maxStepSizeTaken = maxStepTaken,
                    stepSizes = stepSizesList
                )
            }

            // 3. Numerical validity check
            if (state.any { it.isNaN() || it.isInfinite() }) {
                return RayTraceResult(
                    finalState = currentState,
                    terminationReason = TerminationReason.NUMERICAL_ERROR,
                    stepsTaken = step,
                    minRadiusReached = minR,
                    maxHamiltonianResidual = maxHResidual,
                    path = pathList,
                    minStepSizeTaken = if (minStepTaken == Double.MAX_VALUE) 0.0 else minStepTaken,
                    maxStepSizeTaken = maxStepTaken,
                    stepSizes = stepSizesList
                )
            }

            // Advance step
            val dlambda = fixedStepSize ?: computeAdaptiveStep(r)
            if (dlambda < minStepTaken) minStepTaken = dlambda
            if (dlambda > maxStepTaken) maxStepTaken = dlambda
            stepSizesList?.add(dlambda)

            val nextState = rk4Step(state, dlambda)

            // Approximate dT
            val v = currentState.velocity(spacetime)
            currentT += v[0] * dlambda
            currentLambda += dlambda

            val nextPhotonState = PhotonState4D(
                t = currentT,
                x = nextState[0],
                y = nextState[1],
                z = nextState[2],
                p_t = -1.0,
                p_x = nextState[3],
                p_y = nextState[4],
                p_z = nextState[5],
                affineLambda = currentLambda
            )

            // Check for relativistic accretion disk intersection if disk model is active
            if (disk != null && state[2] * nextState[2] <= 0.0 && state[2] != nextState[2]) {
                val hit = DiskIntersection.checkIntersection(
                    previous = currentState,
                    current = nextPhotonState,
                    spacetime = spacetime,
                    disk = disk,
                    camX = initialState.x,
                    camY = initialState.y,
                    camZ = initialState.z
                )
                if (hit != null) {
                    pathList?.add(nextPhotonState)
                    return RayTraceResult(
                        finalState = nextPhotonState,
                        terminationReason = TerminationReason.DISK_HIT,
                        stepsTaken = step + 1,
                        minRadiusReached = min(minR, hit.rHit),
                        maxHamiltonianResidual = maxHResidual,
                        path = pathList,
                        minStepSizeTaken = if (minStepTaken == Double.MAX_VALUE) 0.0 else minStepTaken,
                        maxStepSizeTaken = maxStepTaken,
                        stepSizes = stepSizesList,
                        diskHit = hit
                    )
                }
            }

            state = nextState
            pathList?.add(nextPhotonState)
        }

        val finalState = PhotonState4D(
            t = currentT,
            x = state[0],
            y = state[1],
            z = state[2],
            p_t = -1.0,
            p_x = state[3],
            p_y = state[4],
            p_z = state[5],
            affineLambda = currentLambda
        )
        return RayTraceResult(
            finalState = finalState,
            terminationReason = TerminationReason.MAX_STEPS_EXCEEDED,
            stepsTaken = maxSteps,
            minRadiusReached = minR,
            maxHamiltonianResidual = maxHResidual,
            path = pathList,
            minStepSizeTaken = if (minStepTaken == Double.MAX_VALUE) 0.0 else minStepTaken,
            maxStepSizeTaken = maxStepTaken,
            stepSizes = stepSizesList
        )
    }
}
