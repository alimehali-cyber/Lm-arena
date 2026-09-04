package com.alijafari.red.astronomy.fieldtrial.engine

import com.alijafari.red.astronomy.astro_engine.AstroTime
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.astro_engine.PlanetEngine
import com.alijafari.red.astronomy.astro_engine.PlanetEngine.PlanetType
import com.alijafari.red.astronomy.astro_engine.SunEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.data.catalog.StarCatalog

/**
 * G-2.4: target picker for the field trial — "brightest star above 30 deg altitude,
 * > 20 deg from the Moon, now"; the seven corrected stars (+SMC south of lat -10)
 * filtered to altitude > 15; Moon / Jupiter / Saturn availability. Uses the SAME
 * engines the app uses. Pure Kotlin (harness + CI tested on 4 dates x 3 locations).
 */
object TargetPicker {

    enum class Kind { BRIGHT_STAR, SEVEN_STAR, SMC, SUN, MOON, PLANET }

    /** Simple line-drawing asterism hint for the help sheet (polylines in 0..1 space). */
    data class AsterismHint(val title: String, val polylines: List<List<Pair<Float, Float>>>)

    data class SkyTarget(
        val id: String,
        val name: String,
        val kind: Kind,
        val azDeg: Double,
        val altDeg: Double,
        val magnitude: Double?,
        /** one-line plain-English finder */
        val howToFind: String,
        val hint: AsterismHint? = null
    )

    // ---- positions (same engines as the app) ----

    fun starAltAz(raDeg: Double, decDeg: Double, jdMs: Long, latDeg: Double, lonDeg: Double): CoordinateEngine.Horizontal {
        val jd = TimeEngine.getJulianDate(jdMs)
        val last = TimeEngine.getLAST(jd, lonDeg)
        return CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(raDeg, decDeg), last, latDeg
        )
    }

    fun sunAltAz(jdMs: Long, latDeg: Double, lonDeg: Double): CoordinateEngine.Horizontal =
        SunEngine.getSunAltAz(TimeEngine.getJulianDate(jdMs), latDeg, lonDeg)

    fun moonAltAz(jdMs: Long, latDeg: Double, lonDeg: Double): CoordinateEngine.Horizontal {
        val t = AstroTime(jdMs)
        val mp = MoonEngine.calculateMoon(t)
        val last = TimeEngine.getLAST(t.jdUtc, lonDeg)
        return CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(mp.raDeg, mp.decDeg), last, latDeg
        )
    }

    fun planetAltAz(planet: PlanetType, jdMs: Long, latDeg: Double, lonDeg: Double): CoordinateEngine.Horizontal {
        val pp = PlanetEngine.calculatePlanet(planet, AstroTime(jdMs))
        val last = TimeEngine.getLAST(AstroTime(jdMs).jdUtc, lonDeg)
        return CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(pp.raDeg, pp.decDeg), last, latDeg
        )
    }

    // ---- pickers ----

    /** Brightest (lowest magnitude) star with altitude > 30 deg and > 20 deg from the Moon. */
    fun brightestStarNow(jdMs: Long, latDeg: Double, lonDeg: Double): SkyTarget? {
        val moon = moonAltAz(jdMs, latDeg, lonDeg)
        return StarCatalog.getStars()
            .asSequence()
            .map { star ->
                val h = starAltAz(star.raDeg, star.decDeg, jdMs, latDeg, lonDeg)
                Triple(star, h, TapMeasurement.separationDeg(
                    h.azimuthDeg, h.altitudeDeg, moon.azimuthDeg, moon.altitudeDeg
                ))
            }
            .filter { (_, h, sepMoon) -> h.altitudeDeg > 30.0 && sepMoon > 20.0 }
            .minByOrNull { (star, _, _) -> star.magnitude }
            ?.let { (star, h, sepMoon) ->
                SkyTarget(
                    id = star.id, name = star.nameEn.substringBefore(" ("), kind = Kind.BRIGHT_STAR,
                    azDeg = h.azimuthDeg, altDeg = h.altitudeDeg, magnitude = star.magnitude,
                    howToFind = finderFor(star.id, star.observationTipEn)
                )
            }
    }

    private val sevenIds = listOf(
        "star_uma_mizar", "star_uma_alkaid", "star_cas_schedar", "star_cas_caph",
        "star_ori_alnilam", "star_ori_mintaka", "star_sco_shaula"
    )

    /**
     * The seven corrected stars that are up (alt > 15). Returns (up, belowHorizon names).
     * SMC is appended when lat < -10 and it is above 15 deg.
     */
    fun sevenStarsNow(jdMs: Long, latDeg: Double, lonDeg: Double): Pair<List<SkyTarget>, List<String>> {
        val stars = StarCatalog.getStars().associateBy { it.id }
        val up = ArrayList<SkyTarget>()
        val below = ArrayList<String>()
        for (id in sevenIds) {
            val star = stars[id] ?: continue
            val h = starAltAz(star.raDeg, star.decDeg, jdMs, latDeg, lonDeg)
            if (h.altitudeDeg > 15.0) {
                up.add(
                    SkyTarget(
                        id = id, name = star.nameEn.substringBefore(" ("), kind = Kind.SEVEN_STAR,
                        azDeg = h.azimuthDeg, altDeg = h.altitudeDeg, magnitude = star.magnitude,
                        howToFind = finderFor(id, star.observationTipEn), hint = hintFor(id)
                    )
                )
            } else {
                below.add(star.nameEn.substringBefore(" ("))
            }
        }
        if (latDeg < -10.0) {
            // SMC (NED J2000 13.1867h -> 197.80 deg, -72.8286 deg, Vmag 2.7)
            val h = starAltAz(197.80, -72.8286, jdMs, latDeg, lonDeg)
            if (h.altitudeDeg > 15.0) {
                up.add(
                    SkyTarget(
                        id = "dso_smc", name = "Small Magellanic Cloud", kind = Kind.SMC,
                        azDeg = h.azimuthDeg, altDeg = h.altitudeDeg, magnitude = 2.7,
                        howToFind = "A small faint cloudy patch - look for it high in the southern sky.",
                        hint = hintFor("dso_smc")
                    )
                )
            } else {
                below.add("Small Magellanic Cloud")
            }
        }
        return Pair(up, below)
    }

    fun moonTarget(jdMs: Long, latDeg: Double, lonDeg: Double): SkyTarget? {
        val h = moonAltAz(jdMs, latDeg, lonDeg)
        if (h.altitudeDeg <= 0.0) return null
        return SkyTarget(
            id = "moon", name = "Moon", kind = Kind.MOON,
            azDeg = h.azimuthDeg, altDeg = h.altitudeDeg, magnitude = null,
            howToFind = "The bright Moon - hard to miss. Cover one eye if the glare dazzles you."
        )
    }

    fun jupiterTarget(jdMs: Long, latDeg: Double, lonDeg: Double): SkyTarget? =
        planetTarget(PlanetType.JUPITER, "Jupiter", jdMs, latDeg, lonDeg)

    fun saturnTarget(jdMs: Long, latDeg: Double, lonDeg: Double): SkyTarget? =
        planetTarget(PlanetType.SATURN, "Saturn", jdMs, latDeg, lonDeg)

    private fun planetTarget(planet: PlanetType, name: String, jdMs: Long, lat: Double, lon: Double): SkyTarget? {
        val h = planetAltAz(planet, jdMs, lat, lon)
        if (h.altitudeDeg <= 0.0) return null
        return SkyTarget(
            id = "planet_${planet.name.lowercase()}", name = name, kind = Kind.PLANET,
            azDeg = h.azimuthDeg, altDeg = h.altitudeDeg,
            magnitude = PlanetEngine.calculatePlanet(planet, AstroTime(jdMs)).magnitude,
            howToFind = "$name shines steadily, without twinkling - one of the brightest 'stars' in the sky."
        )
    }

    /** L2 alternative chain: Moon if up; else Jupiter; else Saturn; else null. */
    fun l2Alternative(jdMs: Long, latDeg: Double, lonDeg: Double): SkyTarget? =
        moonTarget(jdMs, latDeg, lonDeg) ?: jupiterTarget(jdMs, latDeg, lonDeg) ?: saturnTarget(jdMs, latDeg, lonDeg)

    private fun finderFor(id: String, fallback: String): String = when (id) {
        "star_uma_mizar" -> "Mizar - the middle star of the Big Dipper's handle."
        "star_uma_alkaid" -> "Alkaid - the star at the very end of the Big Dipper's handle."
        "star_cas_schedar" -> "Schedar - the lower-left star of the W of Cassiopeia."
        "star_cas_caph" -> "Caph - the top-right star of the W of Cassiopeia."
        "star_ori_alnilam" -> "Alnilam - the middle star of Orion's Belt."
        "star_ori_mintaka" -> "Mintaka - the right-hand star of Orion's Belt (as seen facing south in the north)."
        "star_sco_shaula" -> "Shaula - the bright stinger star at the curled tail of Scorpius."
        else -> fallback.ifBlank { "A bright star - match the picture in the help sheet." }
    }

    private fun hintFor(id: String): AsterismHint? = when (id) {
        "star_uma_mizar", "star_uma_alkaid" -> AsterismHint(
            "Big Dipper",
            listOf(
                // bowl + handle as a single polyline in 0..1 box
                listOf(0.10f to 0.30f, 0.30f to 0.25f, 0.32f to 0.50f, 0.12f to 0.55f, 0.10f to 0.30f),
                listOf(0.30f to 0.25f, 0.50f to 0.20f, 0.68f to 0.16f, 0.85f to 0.12f) // handle, Alkaid at end
            )
        )
        "star_cas_schedar", "star_cas_caph" -> AsterismHint(
            "Cassiopeia W",
            listOf(listOf(0.10f to 0.60f, 0.30f to 0.30f, 0.50f to 0.65f, 0.70f to 0.32f, 0.90f to 0.58f))
        )
        "star_ori_alnilam", "star_ori_mintaka" -> AsterismHint(
            "Orion's Belt",
            listOf(
                listOf(0.30f to 0.30f, 0.50f to 0.35f, 0.70f to 0.40f), // belt
                listOf(0.25f to 0.15f, 0.35f to 0.30f), listOf(0.75f to 0.55f, 0.65f to 0.40f), // shoulders
                listOf(0.25f to 0.15f, 0.75f to 0.55f) // shoulders line
            )
        )
        "star_sco_shaula" -> AsterismHint(
            "Scorpius tail",
            listOf(
                listOf(0.10f to 0.50f, 0.30f to 0.45f, 0.45f to 0.55f, 0.60f to 0.45f, 0.75f to 0.55f, 0.85f to 0.40f, 0.90f to 0.55f)
            )
        )
        "dso_smc" -> AsterismHint("Small Magellanic Cloud", listOf(listOf(0.35f to 0.40f, 0.65f to 0.60f, 0.55f to 0.35f, 0.35f to 0.40f)))
        else -> null
    }
}

/**
 * G-1.5/1.7 support: local sun events (rise/set, civil dusk/dawn) by scanning the
 * real Sun altitude — location-correct, timezone-free (returns UTC ms).
 */
object SunEvents {
    /** Next local sunrise after nowMs, or null (polar day/night). */
    fun nextSunriseUtcMs(nowMs: Long, latDeg: Double, lonDeg: Double): Long? =
        nextAltitudeCrossingUtcMs(nowMs, latDeg, lonDeg, targetAltDeg = -0.833, rising = true)

    /** Next local sunset after nowMs, or null. */
    fun nextSunsetUtcMs(nowMs: Long, latDeg: Double, lonDeg: Double): Long? =
        nextAltitudeCrossingUtcMs(nowMs, latDeg, lonDeg, targetAltDeg = -0.833, rising = false)

    /** Next civil dusk (sun crossing -6 deg downward) after nowMs, or null. */
    fun nextCivilDuskUtcMs(nowMs: Long, latDeg: Double, lonDeg: Double): Long? =
        nextAltitudeCrossingUtcMs(nowMs, latDeg, lonDeg, targetAltDeg = -6.0, rising = false)

    fun isNightForGuide(sunAltDeg: Double): Boolean = sunAltDeg < -6.0

    private fun nextAltitudeCrossingUtcMs(
        nowMs: Long, lat: Double, lon: Double, targetAltDeg: Double, rising: Boolean
    ): Long? {
        val stepMs = 5L * 60_000L
        var t = nowMs
        var altPrev = TargetPicker.sunAltAz(t, lat, lon).altitudeDeg - targetAltDeg
        for (i in 1..(24 * 60 / 5)) { // scan up to 24 h
            t += stepMs
            val altNow = TargetPicker.sunAltAz(t, lat, lon).altitudeDeg - targetAltDeg
            val crossed = if (rising) (altPrev < 0.0 && altNow >= 0.0) else (altPrev > 0.0 && altNow <= 0.0)
            if (crossed) {
                // bisect 10x over the 5-min interval
                var lo = t - stepMs; var hi = t
                repeat(10) {
                    val mid = (lo + hi) / 2
                    val am = TargetPicker.sunAltAz(mid, lat, lon).altitudeDeg - targetAltDeg
                    val keepLo = if (rising) am < 0.0 else am > 0.0
                    if (keepLo) lo = mid else hi = mid
                }
                return (lo + hi) / 2
            }
            altPrev = altNow
        }
        return null
    }
}
