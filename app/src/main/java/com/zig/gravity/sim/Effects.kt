package com.zig.gravity.sim

import com.zig.gravity.physics.SimEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * §14 haptic intent. The physics and ViewModel layers never touch Android APIs; they queue one of
 * these per collision event and the composable performs the actual feedback.
 */
enum class HapticCue { LIGHT, MEDIUM, HEAVY }

/** What an effect is communicating, which decides how it is drawn. */
enum class EffectKind {
    /** Elastic contact: a small ring, a handful of sparks, no debris. */
    BOUNCE,

    /** Two bodies became one: contact flash, ring, outward debris that fades quickly. */
    MERGE,

    /** Destructive impact: brighter flash, wider ring, more debris, still short-lived. */
    SHATTER,

    /** Black-hole accretion: debris spirals *inward* and is swallowed. Never an outward blast. */
    ACCRETION,

    /** Wormhole traversal: a quiet throat pulse, no debris. */
    TRAVERSAL
}

/**
 * §13 impact effects.
 *
 * A fixed-size, pre-allocated pool. Nothing here allocates after construction, nothing accumulates,
 * and everything is driven by **elapsed seconds**, never by frame counts, so the animation looks
 * identical at 30, 60 and 120 fps.
 *
 * Pure Kotlin: no Android, no Compose. The renderer reads the public arrays inside its draw lambda;
 * unit tests drive [spawn] and [update] directly.
 */
class EffectPool(
    val maxEffects: Int = MAX_EFFECTS,
    val particlesPerEffect: Int = PARTICLES_PER_EFFECT
) {

    // ---- per-effect state (structure of arrays) --------------------------------------------------
    val active = BooleanArray(maxEffects)
    val kind = arrayOfNulls<EffectKind>(maxEffects)

    /** Anchor in scene metres. For [EffectKind.ACCRETION] this is the hole, and debris falls in. */
    val originX = DoubleArray(maxEffects)
    val originY = DoubleArray(maxEffects)

    /** 0..1 impact severity: drives flash brightness, ring size, debris count and haptics. */
    val severity = DoubleArray(maxEffects)

    /** Seconds since the effect was spawned, and its total lifetime. */
    val age = DoubleArray(maxEffects)
    val life = DoubleArray(maxEffects)

    /** Ring radius in scene metres at full expansion. */
    val ringRadius = DoubleArray(maxEffects)

    /** Body tint, 0xAARRGGBB, so debris matches what was destroyed. */
    val tint = LongArray(maxEffects)

    /** How many of [particlesPerEffect] this effect actually uses. */
    val particleCount = IntArray(maxEffects)

    // ---- per-particle state ------------------------------------------------------------------
    private val capacity = maxEffects * particlesPerEffect
    val pxArr = DoubleArray(capacity)
    val pyArr = DoubleArray(capacity)
    private val pvx = DoubleArray(capacity)
    private val pvy = DoubleArray(capacity)
    val pSize = DoubleArray(capacity)

    private var cursor = 0

    /** Deterministic per-pool noise: reproducible effects, no [java.util.Random] allocation. */
    private var noise = 0x9E3779B9L

    private fun nextUnit(): Double {
        noise = noise * 6364136223846793005L + 1442695040888963407L
        return ((noise ushr 11).toDouble() / (1L shl 53).toDouble())
    }

    fun clear() {
        for (i in 0 until maxEffects) {
            active[i] = false
            kind[i] = null
            age[i] = 0.0
        }
        cursor = 0
    }

    fun activeCount(): Int {
        var c = 0
        for (i in 0 until maxEffects) if (active[i]) c++
        return c
    }

    fun particleIndex(effect: Int, particle: Int): Int = effect * particlesPerEffect + particle

    /**
     * Starts one effect, reusing the oldest slot when the pool is full so the count is bounded
     * whatever the simulation throws at it.
     *
     * @param scale the scene distance the effect should read at — normally the contact radius, so
     *        an impact between marbles is small and one between suns is large.
     */
    fun spawn(
        kindValue: EffectKind,
        x: Double,
        y: Double,
        severityValue: Double,
        scale: Double,
        tintArgb: Long
    ): Int {
        val slot = allocate()
        val sev = severityValue.coerceIn(0.0, 1.0)
        active[slot] = true
        kind[slot] = kindValue
        originX[slot] = x
        originY[slot] = y
        severity[slot] = sev
        age[slot] = 0.0
        tint[slot] = tintArgb
        life[slot] = when (kindValue) {
            EffectKind.BOUNCE -> 0.28
            EffectKind.MERGE -> 0.55
            EffectKind.SHATTER -> 0.85
            EffectKind.ACCRETION -> 1.15
            EffectKind.TRAVERSAL -> 0.45
        }
        ringRadius[slot] = scale * when (kindValue) {
            EffectKind.BOUNCE -> 1.6
            EffectKind.MERGE -> 2.4 + 1.6 * sev
            EffectKind.SHATTER -> 3.2 + 3.0 * sev
            EffectKind.ACCRETION -> 1.4
            EffectKind.TRAVERSAL -> 1.8
        }

        val count = when (kindValue) {
            EffectKind.BOUNCE -> (particlesPerEffect * 0.25).toInt()
            EffectKind.MERGE -> (particlesPerEffect * (0.35 + 0.35 * sev)).toInt()
            EffectKind.SHATTER -> particlesPerEffect
            EffectKind.ACCRETION -> (particlesPerEffect * 0.7).toInt()
            EffectKind.TRAVERSAL -> 0
        }.coerceIn(0, particlesPerEffect)
        particleCount[slot] = count

        val speedScale = scale / life[slot]
        for (p in 0 until count) {
            val idx = particleIndex(slot, p)
            val a = nextUnit() * 2.0 * Math.PI
            val jitter = 0.45 + 0.55 * nextUnit()
            if (kindValue == EffectKind.ACCRETION) {
                // Start on a shell and fall inward with a tangential component: matter is being
                // captured, so the motion must read as a spiral in, never as a blast out.
                val r = scale * (1.4 + 1.8 * nextUnit())
                pxArr[idx] = x + r * cos(a)
                pyArr[idx] = y + r * sin(a)
                val inward = -0.9 * speedScale * jitter
                val tangential = 1.5 * speedScale * jitter
                pvx[idx] = inward * cos(a) - tangential * sin(a)
                pvy[idx] = inward * sin(a) + tangential * cos(a)
            } else {
                pxArr[idx] = x
                pyArr[idx] = y
                val v = speedScale * jitter * (0.8 + 1.6 * sev)
                pvx[idx] = v * cos(a)
                pvy[idx] = v * sin(a)
            }
            pSize[idx] = scale * (0.10 + 0.16 * nextUnit())
        }
        return slot
    }

    private fun allocate(): Int {
        for (i in 0 until maxEffects) {
            val s = (cursor + i) % maxEffects
            if (!active[s]) {
                cursor = (s + 1) % maxEffects
                return s
            }
        }
        // Full: recycle the oldest.
        var oldest = 0
        var best = -1.0
        for (i in 0 until maxEffects) {
            val remaining = life[i] - age[i]
            if (best < 0.0 || remaining < best) {
                best = remaining
                oldest = i
            }
        }
        return oldest
    }

    /** Advances every live effect by [dtSeconds] of **real** time and expires the finished ones. */
    fun update(dtSeconds: Double) {
        if (dtSeconds <= 0.0) return
        val dt = if (dtSeconds > 0.1) 0.1 else dtSeconds
        for (i in 0 until maxEffects) {
            if (!active[i]) continue
            age[i] += dt
            if (age[i] >= life[i]) {
                active[i] = false
                kind[i] = null
                particleCount[i] = 0
                continue
            }
            val accretion = kind[i] == EffectKind.ACCRETION
            val drag = if (accretion) 1.0 else 0.86
            val count = particleCount[i]
            for (p in 0 until count) {
                val idx = particleIndex(i, p)
                pxArr[idx] += pvx[idx] * dt
                pyArr[idx] += pvy[idx] * dt
                if (accretion) {
                    // Pull the debris towards the hole so the spiral actually closes.
                    val dx = originX[i] - pxArr[idx]
                    val dy = originY[i] - pyArr[idx]
                    val d = sqrt(dx * dx + dy * dy)
                    if (d > 1.0e-9) {
                        val pull = 4.5 * dt
                        pvx[idx] += dx / d * pull * (ringRadius[i] / life[i])
                        pvy[idx] += dy / d * pull * (ringRadius[i] / life[i])
                    }
                } else {
                    val f = Math.pow(drag, dt * 60.0)
                    pvx[idx] *= f
                    pvy[idx] *= f
                }
            }
        }
    }

    /** 0..1 progress, for the renderer's easing. */
    fun progress(i: Int): Double = if (life[i] <= 0.0) 1.0 else (age[i] / life[i]).coerceIn(0.0, 1.0)

    companion object {
        const val MAX_EFFECTS: Int = 10
        const val PARTICLES_PER_EFFECT: Int = 22

        /**
         * Maps a physics event onto an effect kind and a 0..1 severity.
         *
         * Severity is the impact speed measured against the pair's mutual escape speed, which is
         * the natural energy scale of a gravitational collision: below it the encounter is gentle,
         * a few times above it the impact is genuinely destructive. Returns null for events that
         * are not impacts.
         */
        fun classify(event: SimEvent): EffectKind? = when (event) {
            is SimEvent.BodyBounced -> EffectKind.BOUNCE
            is SimEvent.BodyMerged -> null // decided by the caller, which knows the impact speed
            is SimEvent.BlackHoleCapture -> EffectKind.ACCRETION
            is SimEvent.WormholeTraversal -> EffectKind.TRAVERSAL
            else -> null
        }
    }
}
