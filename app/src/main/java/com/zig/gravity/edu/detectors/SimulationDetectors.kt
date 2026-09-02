package com.zig.gravity.edu.detectors

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.MergeSubtype
import com.zig.gravity.physics.SimEvent
import com.zig.gravity.sim.SimSnapshot
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * §3.14 — the seven detectors. Pure Kotlin over snapshots; zero `android.*` imports so the whole
 * education trigger layer is JVM-testable (§3.3 binding rule).
 *
 * Every detector is state-based, not event-spam: [HYSTERESIS_MS] guarantees at least two seconds
 * between cards, and each concept has a per-session repeat budget.
 */
data class Detection(
    val concept: String,
    val bodyId: Long,
    val otherId: Long = 0L,
    /** Free numeric payload used by card text (period, momentum, energy, ...). */
    val value: Double = 0.0
)

class SimulationDetectors {

    companion object {
        const val HYSTERESIS_MS: Long = 2000L

        const val ORBIT_STABILIZED = "orbit_stabilized"
        const val BODY_ESCAPED = "body_escaped"
        const val BODY_MERGED = "body_merged"
        const val BH_CAPTURE = "bh_capture"
        const val WORMHOLE_TRAVERSAL = "wormhole_traversal"
        const val ORBIT_DECAYED = "orbit_decayed"
        const val TWO_BODY_DANCE = "two_body_dance"
        const val MASS_CHANGED = "mass_changed"

        /** §23 manipulation moments: raised by the ViewModel, not by a detector. */
        const val POSITION_MOVED = "position_moved"
        const val VELOCITY_CHANGED = "velocity_changed"
        const val IMPACT_ENERGY = "impact_energy"
        const val MOON_QUESTION = "moon_question"

        /** A closed sweep counts as a stable orbit at 300 degrees (§3.14). */
        const val SWEEP_THRESHOLD_DEG = 300.0

        private val REPEAT_BUDGET = mapOf(
            ORBIT_STABILIZED to 3,
            BODY_ESCAPED to 3,
            BODY_MERGED to 4,
            BH_CAPTURE to 3,
            WORMHOLE_TRAVERSAL to 2,
            ORBIT_DECAYED to 2,
            TWO_BODY_DANCE to 2,
            MASS_CHANGED to 3,
            MOON_QUESTION to 1
        )
    }

    private val lastAngle = HashMap<Long, Double>()
    private val sweptDeg = HashMap<Long, Double>()
    private val sweepStartTime = HashMap<Long, Double>()
    private val announcedStable = HashSet<Long>()
    private val announcedEscape = HashSet<Long>()
    private val lastSemiMajor = HashMap<Long, Double>()
    private val decayStreak = HashMap<Long, Int>()
    private val shownCount = HashMap<String, Int>()
    private var lastCardAtMs = 0L
    private var moonQuestionAsked = false

    fun reset() {
        lastAngle.clear()
        sweptDeg.clear()
        sweepStartTime.clear()
        announcedStable.clear()
        announcedEscape.clear()
        lastSemiMajor.clear()
        decayStreak.clear()
        shownCount.clear()
        lastCardAtMs = 0L
        moonQuestionAsked = false
    }

    /** Called by the ViewModel when the user edits a mass, so the teaching layer can react. */
    fun noteMassEdited(bodyId: Long): Detection = Detection(MASS_CHANGED, bodyId)

    /**
     * @param events events produced since the previous call; consumed, not retained.
     * @param nowMs wall-clock milliseconds, used only for hysteresis.
     * @return at most one detection per call — the calmest possible teaching cadence.
     */
    fun observe(snap: SimSnapshot, events: List<SimEvent>, nowMs: Long): Detection? {
        // Track sweeps every call, even while a card is on cooldown, so angles stay continuous.
        updateSweeps(snap)

        if (nowMs - lastCardAtMs < HYSTERESIS_MS) return null

        // 1) Hard events first: they are facts, not inferences.
        for (e in events) {
            when (e) {
                is SimEvent.BodyMerged -> {
                    val concept = if (e.subtype == MergeSubtype.BH_BH || e.subtype == MergeSubtype.BH_ABSORB) {
                        BH_CAPTURE
                    } else {
                        BODY_MERGED
                    }
                    emit(concept, e.survivorId, e.absorbedId, e.momentumAfter, nowMs)?.let { return it }
                }
                is SimEvent.WormholeTraversal ->
                    emit(WORMHOLE_TRAVERSAL, e.bodyId, e.toMouthId, e.speed, nowMs)?.let { return it }
                else -> Unit
            }
        }

        // 2) First-run question: a Moon-type body next to a much heavier body (§3.14 challenge 8).
        if (!moonQuestionAsked) {
            for (i in 0 until snap.n) {
                if (snap.typeOf(i) != BodyType.MOON) continue
                val a = dominantOther(snap, i)
                if (a >= 0 && snap.mass[a] > snap.mass[i] * 10.0) {
                    moonQuestionAsked = true
                    emit(MOON_QUESTION, snap.id[i], snap.id[a], 0.0, nowMs)?.let { return it }
                }
            }
        }

        // 3) State-based detectors.
        for (i in 0 until snap.n) {
            val bid = snap.id[i]
            val a = dominantOther(snap, i)
            if (a < 0 || snap.mass[a] <= 0.0) continue
            val dx = snap.x[i] - snap.x[a]
            val dy = snap.y[i] - snap.y[a]
            val r = sqrt(dx * dx + dy * dy)
            if (r <= 0.0) continue
            val rvx = snap.vx[i] - snap.vx[a]
            val rvy = snap.vy[i] - snap.vy[a]
            val v = sqrt(rvx * rvx + rvy * rvy)
            val eps = EngineConstants.specificOrbitalEnergy(v, snap.mass[a], r)

            if (eps >= 0.0) {
                val receding = (dx * rvx + dy * rvy) > 0.0
                if (receding && r > 40.0 * snap.radius[a] && announcedEscape.add(bid)) {
                    emit(BODY_ESCAPED, bid, snap.id[a], eps, nowMs)?.let { return it }
                }
                continue
            }
            announcedEscape.remove(bid)

            val swept = abs(sweptDeg[bid] ?: 0.0)
            if (swept >= SWEEP_THRESHOLD_DEG && announcedStable.add(bid)) {
                val period = estimatePeriod(snap, i, a, r)
                emit(ORBIT_STABILIZED, bid, snap.id[a], period, nowMs)?.let { return it }
            }

            // Orbit decay: semi-major axis shrinking on several consecutive observations.
            val semiMajor = -EngineConstants.G * snap.mass[a] / (2.0 * eps)
            val prev = lastSemiMajor[bid]
            if (prev != null && semiMajor in 0.0..(prev * 0.97)) {
                val streak = (decayStreak[bid] ?: 0) + 1
                decayStreak[bid] = streak
                if (streak >= 3) {
                    decayStreak[bid] = 0
                    emit(ORBIT_DECAYED, bid, snap.id[a], semiMajor, nowMs)?.let { return it }
                }
            } else if (prev != null && semiMajor > prev * 1.03) {
                decayStreak[bid] = 0
            }
            lastSemiMajor[bid] = semiMajor
        }

        // 4) Two-body dance: exactly two massive bodies, mutually bound, comparable masses.
        val massive = ArrayList<Int>(4)
        for (i in 0 until snap.n) if (snap.mass[i] > 0.0) massive.add(i)
        if (massive.size == 2) {
            val i = massive[0]
            val j = massive[1]
            val ratio = if (snap.mass[j] == 0.0) 0.0 else snap.mass[i] / snap.mass[j]
            if (ratio in 0.2..5.0) {
                val dx = snap.x[j] - snap.x[i]
                val dy = snap.y[j] - snap.y[i]
                val sep = sqrt(dx * dx + dy * dy)
                val sweep = maxOf(abs(sweptDeg[snap.id[i]] ?: 0.0), abs(sweptDeg[snap.id[j]] ?: 0.0))
                if (sweep >= SWEEP_THRESHOLD_DEG) {
                    emit(TWO_BODY_DANCE, snap.id[i], snap.id[j], sep, nowMs)?.let { return it }
                }
            }
        }
        return null
    }

    private fun updateSweeps(snap: SimSnapshot) {
        val live = HashSet<Long>(snap.n * 2)
        for (i in 0 until snap.n) {
            val bid = snap.id[i]
            live.add(bid)
            val a = dominantOther(snap, i)
            if (a < 0) continue
            val ang = Math.toDegrees(atan2(snap.y[i] - snap.y[a], snap.x[i] - snap.x[a]))
            val prev = lastAngle[bid]
            if (prev == null) {
                sweptDeg[bid] = 0.0
                sweepStartTime[bid] = snap.simTime
            } else {
                var d = ang - prev
                while (d > 180.0) d -= 360.0
                while (d < -180.0) d += 360.0
                val acc = sweptDeg[bid] ?: 0.0
                // Only a consistent one-way sweep counts as an orbit; a reversal restarts it.
                if (acc == 0.0 || acc * d > 0.0) {
                    sweptDeg[bid] = acc + d
                } else {
                    sweptDeg[bid] = d
                    sweepStartTime[bid] = snap.simTime
                    announcedStable.remove(bid)
                }
            }
            lastAngle[bid] = ang
        }
        lastAngle.keys.retainAll(live)
        sweptDeg.keys.retainAll(live)
        sweepStartTime.keys.retainAll(live)
        lastSemiMajor.keys.retainAll(live)
        decayStreak.keys.retainAll(live)
        announcedStable.retainAll(live)
        announcedEscape.retainAll(live)
    }

    /** Estimate from the elapsed sweep, or from Kepler's third law when the sweep is short. */
    private fun estimatePeriod(snap: SimSnapshot, i: Int, a: Int, r: Double): Double {
        val start = sweepStartTime[snap.id[i]]
        val swept = abs(sweptDeg[snap.id[i]] ?: 0.0)
        if (start != null && swept > 1.0) {
            val elapsed = snap.simTime - start
            if (elapsed > 0.0) return elapsed * 360.0 / swept
        }
        val mu = EngineConstants.G * (snap.mass[a] + snap.mass[i])
        if (mu <= 0.0) return 0.0
        return 2.0 * Math.PI * sqrt(r * r * r / mu)
    }

    /** Sweep in degrees for a body, exposed for tests and challenge evaluation. */
    fun sweptDegreesOf(bodyId: Long): Double = abs(sweptDeg[bodyId] ?: 0.0)

    private fun dominantOther(snap: SimSnapshot, slot: Int): Int {
        var best = -1
        var bestMass = 0.0
        for (j in 0 until snap.n) {
            if (j == slot) continue
            if (snap.mass[j] > bestMass) { bestMass = snap.mass[j]; best = j }
        }
        return best
    }

    private fun emit(concept: String, bodyId: Long, otherId: Long, value: Double, nowMs: Long): Detection? {
        val budget = REPEAT_BUDGET[concept] ?: 2
        val used = shownCount[concept] ?: 0
        if (used >= budget) return null
        shownCount[concept] = used + 1
        lastCardAtMs = nowMs
        return Detection(concept, bodyId, otherId, value)
    }
}
