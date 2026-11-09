package com.alijafari.red.astronomy.ui.rendering

/**
 * What a tap on the sky canvas is allowed to select.
 *
 * The tap handler and the renderers answer the same question — "is this object on screen right
 * now?" — and they used to answer it differently. [StarRenderer] draws a catalogue star only above
 * 0.8° of altitude, and only while the sky is dark enough for stars to be visible at all; every body
 * is clipped at the horizon row. The hit test, by contrast, accepted anything within 36 dp of where an
 * object *would* be if the projection simply ran on below the horizon or through daylight — so a tap on
 * empty blue sky, or on the ground band, could raise a card for an object nobody could see, and the
 * card then appeared over something else entirely.
 *
 * These predicates are the renderers' own gates, in one place and unit-tested, so a tap can only ever
 * select something the sky is actually showing. They are deliberately conservative in the *safe*
 * direction: a body is selectable while any part of its disc is still above the horizon row, which is
 * what [SunRenderer], [MoonRenderer] and [PlanetRenderer] themselves draw.
 */
object SkyTapTargets {

    /**
     * Lowest altitude at which [StarRenderer] still draws a catalogue star, in degrees.
     *
     * The renderer skips `horiz.altitudeDeg <= 0.8` before it draws or fades anything, so nothing
     * between the horizon row and 0.8° above it is on screen.
     */
    const val STAR_MIN_ALTITUDE_DEG = 0.8

    /**
     * Below this global star visibility nothing is drawn.
     *
     * [LightingEngine] reports 1.0 on a clear night and 0.0 in daylight; the renderers multiply it
     * into every star's alpha and drop anything that ends up under 0.03, so a few hundredths above
     * zero is already invisible. Taps must not select a star the sky is not showing.
     */
    const val MIN_STAR_VISIBILITY = 0.05f

    /**
     * [SunRenderer]'s disc radius in dp, at the largest theme (22 dp).
     *
     * The renderer clips at the horizon row exactly like the Moon and the planets, so the Sun's
     * selectability is the same question: is any part of the disc still above the row?
     */
    const val SUN_RADIUS_DP = 22f

    /** [MoonRenderer]'s base disc radius in dp; the Real Sky theme draws it at 0.48 of that. */
    const val MOON_RADIUS_DP = 26f

    /** [PlanetRenderer]'s halo radius in dp; the planet itself is a couple of pixels of core. */
    const val PLANET_RADIUS_DP = 8f

    /** Altitude of the horizon row: the row every renderer clips at, and altitude 0°. */
    const val HORIZON_ALTITUDE_DEG = 0.0

    /** Whether [StarRenderer] would paint a star at this altitude under this sky brightness. */
    fun starIsDrawn(altitudeDeg: Double, starVisibility: Float): Boolean =
        altitudeDeg > STAR_MIN_ALTITUDE_DEG && starVisibility > MIN_STAR_VISIBILITY

    /**
     * Whether any part of a disc of [radiusPx] still rises above the horizon row.
     *
     * The projection is linear in altitude, so a disc's top sits `radius / pxPerDegree` degrees above
     * its centre: a Moon half set is still on screen, and a Moon a few degrees below the row is not.
     */
    fun discIsDrawn(altitudeDeg: Double, radiusPx: Float, pxPerDegree: Float): Boolean =
        altitudeDeg + radiusPx / pxPerDegree.coerceAtLeast(1f) > HORIZON_ALTITUDE_DEG

    /** Whether the finger came down on the drawn sky rather than on the ground band below it. */
    fun tapIsOnSky(tapY: Float, horizonRowY: Float): Boolean = tapY <= horizonRowY
}
