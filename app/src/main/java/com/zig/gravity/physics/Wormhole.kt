package com.zig.gravity.physics

import kotlin.math.sqrt

/**
 * §3.13 Wormhole — an intentional teaching abstraction, not a physics claim.
 *
 * Paired massless mouths; no gravity (m = 0 falls out of the force pass with no special case);
 * teleport on centre-entry; exit placed just outside the partner along the entry direction;
 * velocity preserved unchanged.
 *
 * Dual cooldown (Auditor B7): after a traversal a body must BOTH
 *   (a) fully exit 1.5x the partner mouth's ring radius, AND
 *   (b) wait [EngineConstants.WORMHOLE_COOLDOWN_SIM_S] simulated seconds
 * before either mouth can trigger for it again.
 */
object Wormhole {

    /**
     * @return true if any body teleported (the caller must recompute accelerations).
     */
    fun resolve(s: SimArrays, events: MutableList<SimEvent>): Boolean {
        var teleported = false
        var i = 0
        while (i < s.n) {
            if (s.typeOf(i) == BodyType.WORMHOLE_MOUTH) { i++; continue }

            // Spatial half of the cooldown: clear the gate once the body has fully left 1.5x the
            // partner ring it emerged from.
            val gate = s.gateMouthId[i]
            if (gate != 0L) {
                val gs = s.slotOfId(gate)
                if (gs < 0) {
                    s.gateMouthId[i] = 0L
                } else {
                    val dx = s.x[i] - s.x[gs]
                    val dy = s.y[i] - s.y[gs]
                    if (sqrt(dx * dx + dy * dy) >
                        EngineConstants.WORMHOLE_SPATIAL_GATE * s.radius[gs] + s.radius[i]
                    ) {
                        s.gateMouthId[i] = 0L
                    }
                }
            }

            val blocked = s.gateMouthId[i] != 0L || s.simTime < s.cooldownUntil[i]
            if (blocked) { i++; continue }

            var m = 0
            while (m < s.n) {
                if (s.typeOf(m) != BodyType.WORMHOLE_MOUTH) { m++; continue }
                val partner = s.slotOfId(s.partnerId[m])
                if (partner < 0 || partner == m) { m++; continue }

                // Centre-entry: the body's centre must be inside the mouth ring.
                val dx = s.x[i] - s.x[m]
                val dy = s.y[i] - s.y[m]
                if (dx * dx + dy * dy > s.radius[m] * s.radius[m]) { m++; continue }

                val speed = sqrt(s.vx[i] * s.vx[i] + s.vy[i] * s.vy[i])
                // Exit direction: the body's direction of travel, or the entry offset if at rest.
                var dirX = s.vx[i]
                var dirY = s.vy[i]
                var dirLen = sqrt(dirX * dirX + dirY * dirY)
                if (dirLen <= 0.0) {
                    dirX = if (dx == 0.0 && dy == 0.0) 1.0 else dx
                    dirY = dy
                    dirLen = sqrt(dirX * dirX + dirY * dirY)
                    if (dirLen <= 0.0) { dirX = 1.0; dirY = 0.0; dirLen = 1.0 }
                }
                val ux = dirX / dirLen
                val uy = dirY / dirLen

                // Just outside the partner ring, along the entry direction.
                val offset = (s.radius[partner] + s.radius[i]) * 1.05
                s.x[i] = s.x[partner] + ux * offset
                s.y[i] = s.y[partner] + uy * offset
                // Velocity preserved unchanged (documented modelling choice, §3.4 assumption 4).

                s.cooldownUntil[i] = s.simTime + EngineConstants.WORMHOLE_COOLDOWN_SIM_S
                s.gateMouthId[i] = s.id[partner]
                s.trails[i].clear() // a teleport is a discontinuity: never draw a line across it

                events.add(
                    SimEvent.WormholeTraversal(
                        simTime = s.simTime,
                        bodyId = s.id[i],
                        fromMouthId = s.id[m],
                        toMouthId = s.id[partner],
                        speed = speed
                    )
                )
                teleported = true
                break
            }
            i++
        }
        return teleported
    }

    /** Links two mouths into a pair. Both directions are set so either end can be entered. */
    fun pair(s: SimArrays, slotA: Int, slotB: Int) {
        s.partnerId[slotA] = s.id[slotB]
        s.partnerId[slotB] = s.id[slotA]
    }

    /**
     * Adds a linked pair of mouths. Returns the slot of the first mouth, or -1 if there is not
     * room for both under the 20-body cap.
     */
    fun addPair(
        s: SimArrays,
        ax: Double, ay: Double,
        bx: Double, by: Double,
        dp: Double = BodyType.WORMHOLE_MOUTH.defaultDp
    ): Int {
        if (s.n + 2 > s.capacity) return -1
        val a = s.add(BodyType.WORMHOLE_MOUTH, 0.0, dp, ax, ay, 0.0, 0.0, catalog = "wormhole")
        val b = s.add(BodyType.WORMHOLE_MOUTH, 0.0, dp, bx, by, 0.0, 0.0, catalog = "wormhole")
        if (a < 0 || b < 0) return -1
        pair(s, a, b)
        return a
    }

    /** Removing one mouth removes its partner: a lone mouth is meaningless. */
    fun removeWithPartner(s: SimArrays, slot: Int) {
        if (s.typeOf(slot) != BodyType.WORMHOLE_MOUTH) {
            s.removeAt(slot)
            return
        }
        val partnerId = s.partnerId[slot]
        s.removeAt(slot)
        val p = s.slotOfId(partnerId)
        if (p >= 0) s.removeAt(p)
    }
}
