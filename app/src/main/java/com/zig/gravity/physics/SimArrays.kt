package com.zig.gravity.physics

/**
 * §3.9 preallocated trail ring buffer. One sample per rendered frame, [EngineConstants.TRAIL_CAPACITY]
 * samples deep -> a constant ~4 s observed-motion window at 60 fps.
 *
 * Zero allocation after construction.
 */
class TrailRing(private val capacity: Int = EngineConstants.TRAIL_CAPACITY) {
    private val xs = DoubleArray(capacity)
    private val ys = DoubleArray(capacity)
    private var head = 0
    var count: Int = 0
        private set

    fun push(x: Double, y: Double) {
        xs[head] = x
        ys[head] = y
        head = (head + 1) % capacity
        if (count < capacity) count++
    }

    /** Oldest sample is index 0. */
    fun xAt(i: Int): Double = xs[(head - count + i + capacity * 2) % capacity]

    fun yAt(i: Int): Double = ys[(head - count + i + capacity * 2) % capacity]

    fun clear() {
        head = 0
        count = 0
    }

    fun copyFrom(other: TrailRing) {
        clear()
        for (i in 0 until other.count) push(other.xAt(i), other.yAt(i))
    }
}

/**
 * §Locked decision 2 — Structure of arrays, fixed capacity 20.
 *
 * Positions/velocities/accelerations are SI metres and seconds, Double throughout.
 * [radiusDp] is the authoritative user-facing size; [radius] is the derived collision/visual
 * radius in scene metres (§3.4: "Collision radius = visual radius in scene meters").
 *
 * Slots are kept contiguous: removal compacts the arrays left, so "lower array slot" in the
 * survivor tie-break rule is well defined and deterministic.
 */
class SimArrays(val capacity: Int = EngineConstants.MAX_BODIES) {

    val x = DoubleArray(capacity)
    val y = DoubleArray(capacity)
    val vx = DoubleArray(capacity)
    val vy = DoubleArray(capacity)
    val ax = DoubleArray(capacity)
    val ay = DoubleArray(capacity)
    val mass = DoubleArray(capacity)

    /** Display size in dp — the authority (§3.6a). */
    val radiusDp = DoubleArray(capacity)

    /** Derived: radiusDp * metersPerDp. Collision radius == visual radius. */
    val radius = DoubleArray(capacity)

    val id = LongArray(capacity)
    val type = ByteArray(capacity)

    /** True while a finger holds the body: integration is skipped (§3.11, test 30). */
    val kinematic = BooleanArray(capacity)

    /** Wormhole pairing + dual cooldown state (§3.13). */
    val partnerId = LongArray(capacity)
    val cooldownUntil = DoubleArray(capacity)
    val gateMouthId = LongArray(capacity)

    /** Catalog key for naming/colour lookup; not part of the numeric state. */
    val catalogKey = arrayOfNulls<String>(capacity)

    val trails = Array(capacity) { TrailRing() }

    var n: Int = 0
        private set

    var simTime: Double = 0.0

    /**
     * §3.5 synchronisation rule. Cleared by every mutating intent so the next Verlet half-kick
     * can never use a stale acceleration (Auditor B13, test 39).
     */
    var accelerationsValid: Boolean = false

    var metersPerDp: Double = EngineConstants.metersPerDp(400.0)
        private set

    private var nextId: Long = 1L

    // ---- rollback storage (§3.8 layer 2), preallocated ---------------------------------------
    private val bx = DoubleArray(capacity)
    private val by = DoubleArray(capacity)
    private val bvx = DoubleArray(capacity)
    private val bvy = DoubleArray(capacity)
    private val bax = DoubleArray(capacity)
    private val bay = DoubleArray(capacity)
    private val bmass = DoubleArray(capacity)
    private val bradius = DoubleArray(capacity)
    private val bradiusDp = DoubleArray(capacity)
    private val bid = LongArray(capacity)
    private val btype = ByteArray(capacity)
    private val bkin = BooleanArray(capacity)
    private val bpartner = LongArray(capacity)
    private val bcool = DoubleArray(capacity)
    private val bgate = LongArray(capacity)
    private var bn = 0
    private var bSimTime = 0.0

    fun typeOf(slot: Int): BodyType = BodyType.fromCode(type[slot])

    fun isFull(): Boolean = n >= capacity

    fun slotOfId(bodyId: Long): Int {
        for (i in 0 until n) if (id[i] == bodyId) return i
        return -1
    }

    /** Recomputes derived scene radii when the viewport changes. dp stays authoritative. */
    fun setMetersPerDp(value: Double) {
        if (value <= 0.0 || value == metersPerDp) return
        metersPerDp = value
        for (i in 0 until n) radius[i] = radiusDp[i] * value
        accelerationsValid = false
    }

    fun add(
        type: BodyType,
        massKg: Double,
        radiusDpValue: Double,
        posX: Double,
        posY: Double,
        velX: Double,
        velY: Double,
        catalog: String? = null,
        explicitId: Long = 0L
    ): Int {
        if (n >= capacity) return -1
        val s = n
        this.type[s] = type.code
        mass[s] = if (type.massless) 0.0 else massKg
        radiusDp[s] = radiusDpValue.coerceIn(type.minDp, type.maxDp)
        radius[s] = radiusDp[s] * metersPerDp
        x[s] = posX; y[s] = posY
        vx[s] = velX; vy[s] = velY
        ax[s] = 0.0; ay[s] = 0.0
        kinematic[s] = false
        partnerId[s] = 0L
        cooldownUntil[s] = 0.0
        gateMouthId[s] = 0L
        catalogKey[s] = catalog
        id[s] = if (explicitId != 0L) explicitId else nextId++
        if (explicitId >= nextId) nextId = explicitId + 1
        trails[s].clear()
        n = s + 1
        accelerationsValid = false
        return s
    }

    /** Compacting removal. Trail buffers follow their body so no trail is ever orphaned. */
    fun removeAt(slot: Int) {
        if (slot < 0 || slot >= n) return
        for (i in slot until n - 1) {
            x[i] = x[i + 1]; y[i] = y[i + 1]
            vx[i] = vx[i + 1]; vy[i] = vy[i + 1]
            ax[i] = ax[i + 1]; ay[i] = ay[i + 1]
            mass[i] = mass[i + 1]
            radius[i] = radius[i + 1]
            radiusDp[i] = radiusDp[i + 1]
            id[i] = id[i + 1]
            type[i] = type[i + 1]
            kinematic[i] = kinematic[i + 1]
            partnerId[i] = partnerId[i + 1]
            cooldownUntil[i] = cooldownUntil[i + 1]
            gateMouthId[i] = gateMouthId[i + 1]
            catalogKey[i] = catalogKey[i + 1]
            trails[i].copyFrom(trails[i + 1])
        }
        trails[n - 1].clear()
        catalogKey[n - 1] = null
        n--
        accelerationsValid = false
    }

    fun removeById(bodyId: Long): Boolean {
        val s = slotOfId(bodyId)
        if (s < 0) return false
        removeAt(s)
        return true
    }

    fun clear() {
        n = 0
        simTime = 0.0
        accelerationsValid = false
        for (t in trails) t.clear()
        for (i in 0 until capacity) catalogKey[i] = null
    }

    /** Clamped to the §3.6a dp band — the path every user-facing size edit takes. */
    fun setRadiusDp(slot: Int, dp: Double) {
        val t = typeOf(slot)
        setRadiusDpRaw(slot, dp.coerceIn(t.minDp, t.maxDp))
    }

    /**
     * Unclamped size write. Used only by volume-conserving merges, where the physically correct
     * result may legitimately exceed the type's default band (§3.7, test 17).
     */
    fun setRadiusDpRaw(slot: Int, dp: Double) {
        radiusDp[slot] = dp
        radius[slot] = dp * metersPerDp
    }

    fun pushTrailSample() {
        for (i in 0 until n) trails[i].push(x[i], y[i])
    }

    fun clearTrails() {
        for (i in 0 until n) trails[i].clear()
    }

    fun totalMass(): Double {
        var m = 0.0
        for (i in 0 until n) m += mass[i]
        return m
    }

    /** Index of the most massive body, or -1. Used by the orbit helper and detectors. */
    fun dominantAttractor(exceptSlot: Int = -1): Int {
        var best = -1
        var bestMass = 0.0
        for (i in 0 until n) {
            if (i == exceptSlot) continue
            if (mass[i] > bestMass) { bestMass = mass[i]; best = i }
        }
        return best
    }

    // ---- rollback -----------------------------------------------------------------------------

    fun backup() {
        bn = n
        bSimTime = simTime
        for (i in 0 until n) {
            bx[i] = x[i]; by[i] = y[i]
            bvx[i] = vx[i]; bvy[i] = vy[i]
            bax[i] = ax[i]; bay[i] = ay[i]
            bmass[i] = mass[i]; bradius[i] = radius[i]; bradiusDp[i] = radiusDp[i]
            bid[i] = id[i]; btype[i] = type[i]; bkin[i] = kinematic[i]
            bpartner[i] = partnerId[i]; bcool[i] = cooldownUntil[i]; bgate[i] = gateMouthId[i]
        }
    }

    fun restore() {
        n = bn
        simTime = bSimTime
        for (i in 0 until bn) {
            x[i] = bx[i]; y[i] = by[i]
            vx[i] = bvx[i]; vy[i] = bvy[i]
            ax[i] = bax[i]; ay[i] = bay[i]
            mass[i] = bmass[i]; radius[i] = bradius[i]; radiusDp[i] = bradiusDp[i]
            id[i] = bid[i]; type[i] = btype[i]; kinematic[i] = bkin[i]
            partnerId[i] = bpartner[i]; cooldownUntil[i] = bcool[i]; gateMouthId[i] = bgate[i]
        }
    }

    /** Deep copy used by reset (§19) and by the deterministic-replay test. */
    fun copyInto(dest: SimArrays) {
        dest.clear()
        dest.setMetersPerDp(metersPerDp)
        for (i in 0 until n) {
            val s = dest.add(
                type = typeOf(i),
                massKg = mass[i],
                radiusDpValue = radiusDp[i],
                posX = x[i], posY = y[i],
                velX = vx[i], velY = vy[i],
                catalog = catalogKey[i],
                explicitId = id[i]
            )
            if (s >= 0) {
                // Raw: a merged body may legitimately exceed its type's dp band (§3.7).
                dest.setRadiusDpRaw(s, radiusDp[i])
                dest.ax[s] = ax[i]; dest.ay[s] = ay[i]
                dest.partnerId[s] = partnerId[i]
                dest.cooldownUntil[s] = cooldownUntil[i]
                dest.gateMouthId[s] = gateMouthId[i]
                dest.trails[s].copyFrom(trails[i])
            }
        }
        dest.simTime = simTime
        dest.accelerationsValid = false
    }

    /** Order-independent state hash for the deterministic-replay test (test 12). */
    fun stateHash(): Long {
        var h = 1125899906842597L
        h = h * 31 + n
        h = h * 31 + simTime.toRawBits()
        for (i in 0 until n) {
            h = h * 31 + id[i]
            h = h * 31 + x[i].toRawBits()
            h = h * 31 + y[i].toRawBits()
            h = h * 31 + vx[i].toRawBits()
            h = h * 31 + vy[i].toRawBits()
            h = h * 31 + mass[i].toRawBits()
        }
        return h
    }
}
