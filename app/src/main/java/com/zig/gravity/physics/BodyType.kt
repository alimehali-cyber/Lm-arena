package com.zig.gravity.physics

/**
 * §3.3 locked body taxonomy. Exactly seven types — no more, no less.
 *
 * Named solar-system objects (Mercury..Neptune) are *catalog entries* that instantiate one of
 * these types with real physical mass; they are not additional enum members. See
 * [com.zig.gravity.sim.BodyCatalog].
 */
enum class BodyType(
    val code: Byte,
    val minDp: Double,
    val maxDp: Double,
    val defaultDp: Double,
    /** Inspector mass slider bounds in kg (§3.11). */
    val minMassKg: Double,
    val maxMassKg: Double,
    val defaultMassKg: Double,
    /** Massless bodies exert no gravity; this falls out of m_j = 0 with no special case (§3.4). */
    val massless: Boolean = false
) {
    SUN(
        code = 0,
        minDp = 20.0, maxDp = 32.0, defaultDp = 26.0,
        minMassKg = 0.1 * EngineConstants.M_SUN,
        maxMassKg = 10.0 * EngineConstants.M_SUN,
        defaultMassKg = EngineConstants.M_SUN
    ),
    PLANET(
        code = 1,
        minDp = 8.0, maxDp = 16.0, defaultDp = 10.0,
        minMassKg = 0.01 * EngineConstants.M_EARTH,
        maxMassKg = 100.0 * EngineConstants.M_EARTH,
        defaultMassKg = EngineConstants.M_EARTH
    ),
    MOON(
        code = 2,
        minDp = 5.0, maxDp = 8.0, defaultDp = 6.0,
        minMassKg = 0.001 * EngineConstants.M_EARTH,
        maxMassKg = 0.5 * EngineConstants.M_EARTH,
        defaultMassKg = EngineConstants.M_MOON
    ),
    ASTEROID(
        code = 3,
        minDp = 3.0, maxDp = 6.0, defaultDp = 4.0,
        minMassKg = 1.0e15,
        maxMassKg = 1.0e-3 * EngineConstants.M_EARTH,
        defaultMassKg = 1.0e18
    ),

    /** Massless probe: feels gravity, exerts none (§3.4, test 29). */
    TEST_MARBLE(
        code = 4,
        minDp = 4.0, maxDp = 7.0, defaultDp = 5.0,
        minMassKg = 0.0,
        maxMassKg = 0.0,
        defaultMassKg = 0.0,
        massless = true
    ),

    /** Newtonian point mass. The drawn ring IS the capture radius (§3.12, test 22). */
    BLACK_HOLE(
        code = 5,
        minDp = 10.0, maxDp = 20.0, defaultDp = 14.0,
        minMassKg = 1.0 * EngineConstants.M_SUN,
        maxMassKg = 50.0 * EngineConstants.M_SUN,
        defaultMassKg = 5.0 * EngineConstants.M_SUN
    ),

    /**
     * Paired massless mouth (§3.13). Never collides, never attracts; triggers teleport.
     * The dp band is not tabulated in §3.6a — chosen inside the black-hole ring band so the two
     * "ring" objects share a consistent visual language. Flagged in the implementation audit.
     */
    WORMHOLE_MOUTH(
        code = 6,
        minDp = 10.0, maxDp = 16.0, defaultDp = 12.0,
        minMassKg = 0.0,
        maxMassKg = 0.0,
        defaultMassKg = 0.0,
        massless = true
    );

    val isRingBody: Boolean get() = this == BLACK_HOLE || this == WORMHOLE_MOUTH

    /** Mass is editable only for types that actually carry mass. */
    val massEditable: Boolean get() = !massless

    companion object {
        private val BY_CODE = entries.associateBy { it.code }
        fun fromCode(code: Byte): BodyType = BY_CODE[code] ?: PLANET

        /**
         * Slider bounds, widened upward if a catalog body legitimately exceeds the locked band
         * (Jupiter is 317.8 M_EARTH against a 100 M_EARTH PLANET ceiling). Widening only ever
         * happens for the body that already holds that mass, so the locked band still governs
         * everything the user creates from scratch.
         */
        fun massRange(type: BodyType, currentMassKg: Double): ClosedFloatingPointRange<Double> {
            val hi = maxOf(type.maxMassKg, currentMassKg)
            val lo = minOf(type.minMassKg, currentMassKg)
            return lo..hi
        }
    }
}
