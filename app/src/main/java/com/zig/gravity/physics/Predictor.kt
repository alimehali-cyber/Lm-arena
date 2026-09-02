package com.zig.gravity.physics

import kotlin.math.sqrt

/**
 * §3.9 trajectory preview — **test-particle model** (Auditor B16).
 *
 * The candidate body is integrated through the *frozen* field of the other bodies:
 * ~600 steps x 20 sources ~= 0.5 MFLOP (~0.2–1 ms). Full mutual integration
 * (~5.7 MFLOP, 3–11 ms) is explicitly forbidden — it would blow the frame budget.
 *
 * The preview is approximate whenever the previewed body carries a significant share of the
 * system mass; [isApproximate] decides when the UI must say so.
 *
 * Never mutates [SimArrays].
 */
object Predictor {

    /** Output stride: keep the drawn polyline light without shortening the horizon. */
    const val SAMPLE_STRIDE: Int = 3

    /** Preview is labelled approximate above this share of total system mass (§3.9). */
    const val APPROXIMATE_MASS_FRACTION: Double = 0.01

    fun isApproximate(s: SimArrays, bodyMass: Double): Boolean {
        val total = s.totalMass()
        if (total <= 0.0) return false
        return bodyMass / total > APPROXIMATE_MASS_FRACTION
    }

    /**
     * @param excludeSlot the previewed body's slot (excluded from the source set), or -1 when
     *        previewing a body that is not in the array yet.
     * @param outXY receives x,y pairs in scene metres; must hold at least 2*maxSamples doubles.
     * @param boundsRadius stop once the particle is this far from the origin (off-screen).
     * @return number of samples written (pairs), always >= 1 when maxSamples >= 1.
     */
    fun predict(
        s: SimArrays,
        excludeSlot: Int,
        startX: Double,
        startY: Double,
        startVx: Double,
        startVy: Double,
        particleRadius: Double,
        outXY: DoubleArray,
        maxSamples: Int,
        steps: Int = EngineConstants.PREDICTION_STEPS,
        dt: Double = EngineConstants.DT,
        boundsRadius: Double = 40.0 * EngineConstants.AU
    ): Int {
        if (maxSamples <= 0) return 0
        var px = startX
        var py = startY
        var pvx = startVx
        var pvy = startVy

        var ax = 0.0
        var ay = 0.0
        accelerationAt(s, excludeSlot, px, py).let { ax = it[0]; ay = it[1] }

        var written = 0
        outXY[0] = px
        outXY[1] = py
        written = 1

        var stepIndex = 0
        while (stepIndex < steps && written < maxSamples) {
            val half = dt * 0.5
            pvx += ax * half
            pvy += ay * half
            px += pvx * dt
            py += pvy * dt
            val a = accelerationAt(s, excludeSlot, px, py)
            ax = a[0]
            ay = a[1]
            pvx += ax * half
            pvy += ay * half

            // Engine clamp applies to the preview too, so the dotted line cannot promise a
            // trajectory the engine would refuse to run.
            val v2 = pvx * pvx + pvy * pvy
            if (v2 > EngineConstants.V_MAX * EngineConstants.V_MAX) {
                val k = EngineConstants.V_MAX / sqrt(v2)
                pvx *= k
                pvy *= k
            }

            stepIndex++

            if (stepIndex % SAMPLE_STRIDE == 0) {
                outXY[written * 2] = px
                outXY[written * 2 + 1] = py
                written++
            }

            // Stop conditions: off-screen, or contact with a source body.
            if (px * px + py * py > boundsRadius * boundsRadius) break
            if (hitsSource(s, excludeSlot, px, py, particleRadius)) break
        }
        return written
    }

    private val scratch = DoubleArray(2)

    private fun accelerationAt(s: SimArrays, excludeSlot: Int, px: Double, py: Double): DoubleArray {
        var ax = 0.0
        var ay = 0.0
        for (j in 0 until s.n) {
            if (j == excludeSlot) continue
            val m = s.mass[j]
            if (m <= 0.0) continue
            val dx = s.x[j] - px
            val dy = s.y[j] - py
            val d2 = dx * dx + dy * dy + EngineConstants.EPS_SOFT_SQ
            val d = sqrt(d2)
            val f = EngineConstants.G * m / (d2 * d)
            ax += f * dx
            ay += f * dy
        }
        scratch[0] = ax
        scratch[1] = ay
        return scratch
    }

    private fun hitsSource(
        s: SimArrays,
        excludeSlot: Int,
        px: Double,
        py: Double,
        particleRadius: Double
    ): Boolean {
        for (j in 0 until s.n) {
            if (j == excludeSlot) continue
            if (s.typeOf(j) == BodyType.WORMHOLE_MOUTH) continue
            val dx = s.x[j] - px
            val dy = s.y[j] - py
            val contact = s.radius[j] + particleRadius
            if (dx * dx + dy * dy < contact * contact) return true
        }
        return false
    }
}
