package com.zig.gargantua.worldline

import com.zig.gargantua.geodesic.ProceduralSky

/**
 * Synthetic test object with physical finite radius and timelike Kerr worldline.
 *
 * @param worldline M8 physical timelike worldline.
 * @param radius Physical radius of the object in geometrized units M.
 * @param baseColor Distinctive emissive base color (default vivid cyan/electric azure).
 * @param emissiveRadiance Base unshifted surface radiance.
 * @param enabled Whether the object is active in the scene.
 */
data class RelativisticObject(
    val worldline: RelativisticWorldline,
    val radius: Double = 0.45,
    val baseColor: ProceduralSky.ColorRGB = ProceduralSky.ColorRGB(0.15, 0.85, 1.0),
    val emissiveRadiance: Double = 35.0,
    val enabled: Boolean = true
) {
    init {
        require(radius > 0.0) { "Object physical radius must be positive, got $radius" }
        require(emissiveRadiance >= 0.0) { "Emissive radiance must be non-negative" }
    }

    /** Position at coordinate time T. */
    fun positionAt(T: Double): DoubleArray = worldline.positionAt(T)

    /** 4-velocity at coordinate time T. */
    fun fourVelocityAt(T: Double): DoubleArray = worldline.fourVelocityAt(T)
}
