package com.zig.gravity.sim

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.physics.TrailRing

/**
 * §3.3 SimSnapshot — the single object the renderer is allowed to read.
 *
 * It is filled by copy once per frame and then read **only inside the Compose draw lambda**,
 * gated by the ViewModel's frame tick. That is what delivers locked decision 8
 * ("draw-phase-only state reads", "recomposition/frame = 0").
 *
 * Trail rings are referenced rather than copied: everything runs single-threaded on the main
 * dispatcher (locked decision 9), and copying 20 x 240 samples every frame would violate the
 * zero-allocation draw budget for no benefit. Locked decision 10 explicitly permits
 * "frame-tick + raw arrays ... if profiling demands"; the zero-allocation budget is that reason.
 */
class SimSnapshot(val capacity: Int = EngineConstants.MAX_BODIES) {

    var n: Int = 0
    val x = DoubleArray(capacity)
    val y = DoubleArray(capacity)
    val vx = DoubleArray(capacity)
    val vy = DoubleArray(capacity)
    val ax = DoubleArray(capacity)
    val ay = DoubleArray(capacity)
    val mass = DoubleArray(capacity)
    val radiusDp = DoubleArray(capacity)
    val radius = DoubleArray(capacity)
    val id = LongArray(capacity)
    val type = ByteArray(capacity)
    val partnerId = LongArray(capacity)
    val kinematic = BooleanArray(capacity)
    val catalogKey = arrayOfNulls<String>(capacity)

    var simTime: Double = 0.0
    var metersPerDp: Double = EngineConstants.metersPerDp(400.0)

    val barycenter = DoubleArray(2)
    var kineticEnergy: Double = 0.0
    var potentialEnergy: Double = 0.0

    var trails: Array<TrailRing>? = null

    fun typeOf(i: Int): BodyType = BodyType.fromCode(type[i])

    fun slotOfId(bodyId: Long): Int {
        for (i in 0 until n) if (id[i] == bodyId) return i
        return -1
    }

    fun captureFrom(s: SimArrays) {
        n = s.n
        simTime = s.simTime
        metersPerDp = s.metersPerDp
        for (i in 0 until s.n) {
            x[i] = s.x[i]; y[i] = s.y[i]
            vx[i] = s.vx[i]; vy[i] = s.vy[i]
            ax[i] = s.ax[i]; ay[i] = s.ay[i]
            mass[i] = s.mass[i]
            radiusDp[i] = s.radiusDp[i]
            radius[i] = s.radius[i]
            id[i] = s.id[i]
            type[i] = s.type[i]
            partnerId[i] = s.partnerId[i]
            kinematic[i] = s.kinematic[i]
            catalogKey[i] = s.catalogKey[i]
        }
        NBodyEngine.barycenter(s, barycenter)
        kineticEnergy = NBodyEngine.kineticEnergy(s)
        potentialEnergy = NBodyEngine.potentialEnergy(s)
        trails = s.trails
    }

    /**
     * A signature that changes only when something the brush/label caches depend on changes
     * (body set, type, size, colour). Positions are deliberately excluded.
     */
    fun visualSignature(): Int {
        var h = n
        for (i in 0 until n) {
            h = h * 31 + id[i].toInt()
            h = h * 31 + type[i].toInt()
            h = h * 31 + (radiusDp[i] * 64.0).toInt()
            h = h * 31 + (catalogKey[i]?.hashCode() ?: 0)
        }
        return h
    }
}
