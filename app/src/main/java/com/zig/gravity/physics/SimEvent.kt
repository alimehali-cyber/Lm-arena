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
