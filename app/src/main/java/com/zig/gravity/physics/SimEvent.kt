package com.zig.gravity.physics

/**
 * §3.3 SimEvent. The engine never talks to the UI or the education layer directly; it appends
 * facts to an event list which pure detectors and the teaching layer consume.
 */
sealed class SimEvent {
    abstract val simTime: Double

    data class BodyAdded(override val simTime: Double, val id: Long, val type: BodyType) : SimEvent()

    data class BodyRemoved(override val simTime: Double, val id: Long, val type: BodyType) : SimEvent()

    /**
     * @param subtype BH_BH for a black-hole pair merger (§3.7 / Auditor A3), else NORMAL.
     */
    data class BodyMerged(
        override val simTime: Double,
        val survivorId: Long,
        val absorbedId: Long,
        val survivorType: BodyType,
        val absorbedType: BodyType,
        val momentumBefore: Double,
        val momentumAfter: Double,
        val massBefore: Double,
        val massAfter: Double,
        val subtype: MergeSubtype
    ) : SimEvent()

    /**
     * §12 — a physical contact, emitted for every collision *before* the outcome is applied, so
     * the effects, haptics and teaching layers see the impact energy that produced the outcome.
     *
     * @param relativeSpeed closing speed along the contact normal, m/s.
     * @param mutualEscapeSpeed sqrt(2G(m1+m2)/(r1+r2)) — the natural energy scale of a
     *        gravitational collision, and what [tier] is measured against.
     */
    data class CollisionImpact(
        override val simTime: Double,
        val aId: Long,
        val bId: Long,
        val x: Double,
        val y: Double,
        val relativeSpeed: Double,
        val mutualEscapeSpeed: Double,
        val contactRadius: Double,
        val tier: ImpactTier,
        val merged: Boolean
    ) : SimEvent()

    data class BodyBounced(
        override val simTime: Double,
        val aId: Long,
        val bId: Long,
        val restitution: Double
    ) : SimEvent()

    data class BlackHoleCapture(
        override val simTime: Double,
        val holeId: Long,
        val capturedId: Long,
        val holeMassAfter: Double
    ) : SimEvent()

    data class WormholeTraversal(
        override val simTime: Double,
        val bodyId: Long,
        val fromMouthId: Long,
        val toMouthId: Long,
        val speed: Double
    ) : SimEvent()

    data class BodyEscaped(
        override val simTime: Double,
        val bodyId: Long,
        val specificEnergy: Double
    ) : SimEvent()

    data class OrbitStabilized(
        override val simTime: Double,
        val bodyId: Long,
        val attractorId: Long,
        val periodEstimate: Double
    ) : SimEvent()

    data class OrbitDecayed(
        override val simTime: Double,
        val bodyId: Long,
        val attractorId: Long
    ) : SimEvent()

    data class TwoBodyDance(
        override val simTime: Double,
        val aId: Long,
        val bId: Long,
        val separation: Double
    ) : SimEvent()

    data class NumericalFailure(
        override val simTime: Double,
        val rolledBack: Boolean,
        val consecutiveFailures: Int,
        val quarantinedId: Long
    ) : SimEvent()
}

enum class MergeSubtype { NORMAL, BH_BH, BH_ABSORB }

/**
 * §12 impact severity, measured as closing speed / mutual escape speed.
 *
 * Below 1 the bodies barely had the energy to fall together — a gentle touch. A few times above it
 * the encounter carries far more kinetic energy than the pair's gravitational binding, which is
 * what "destructive" physically means here.
 */
enum class ImpactTier {
    LOW,
    MODERATE,
    HIGH;

    companion object {
        const val MODERATE_RATIO: Double = 1.0
        const val HIGH_RATIO: Double = 3.0

        fun of(relativeSpeed: Double, mutualEscapeSpeed: Double): ImpactTier {
            if (relativeSpeed <= 0.0) return LOW
            // Massless test bodies have no escape speed to compare against; fall back to an
            // absolute scale so marble impacts still read as gentle or violent.
            val ratio = if (mutualEscapeSpeed > 0.0) {
                relativeSpeed / mutualEscapeSpeed
            } else {
                relativeSpeed / 5.0e3
            }
            return when {
                ratio >= HIGH_RATIO -> HIGH
                ratio >= MODERATE_RATIO -> MODERATE
                else -> LOW
            }
        }

        /** 0..1 severity for effect strength and haptic intensity. */
        fun severity(relativeSpeed: Double, mutualEscapeSpeed: Double): Double {
            if (relativeSpeed <= 0.0) return 0.0
            val ratio = if (mutualEscapeSpeed > 0.0) {
                relativeSpeed / mutualEscapeSpeed
            } else {
                relativeSpeed / 5.0e3
            }
            return (ratio / HIGH_RATIO).coerceIn(0.0, 1.0)
        }
    }
}
