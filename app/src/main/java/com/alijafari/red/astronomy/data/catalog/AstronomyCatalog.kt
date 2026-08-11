package com.alijafari.red.astronomy.data.catalog

import com.alijafari.red.astronomy.astro_engine.AstroDispatchEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ConstellationData
import com.alijafari.red.astronomy.domain.ObjectType

object AstronomyCatalog {

    val SUN: CelestialObject
        get() = getById("sun") ?: SolarSystemCatalog.getSun(TimeEngine.getJulianDate())

    val MOON: CelestialObject
        get() = getById("moon") ?: SolarSystemCatalog.getMoon(TimeEngine.getJulianDate())

    val EARTH: CelestialObject
        get() = getById("planet_earth") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("planet_earth")!!
        )

    val ISS: CelestialObject
        get() = getById("sat_iss") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("sat_iss")!!
        )

    val MILKY_WAY: CelestialObject
        get() = getById("sagittarius_a_star") ?: getById("galaxy_milky_way") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("sagittarius_a_star")!!
        )

    val ANDROMEDA: CelestialObject
        get() = getById("dso_m31_andromeda") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("dso_m31_andromeda")!!
        )

    val ORION_NEBULA: CelestialObject
        get() = getById("dso_m42_orion_nebula") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("dso_m42_orion_nebula")!!
        )

    val PLEIADES: CelestialObject
        get() = getById("dso_m45_pleiades") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("dso_m45_pleiades")!!
        )

    val SIRIUS: CelestialObject
        get() = getById("star_cma_sirius") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("star_cma_sirius")!!
        )

    val VEGA: CelestialObject
        get() = getById("star_lyr_vega") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("star_lyr_vega")!!
        )

    val BETELGEUSE: CelestialObject
        get() = getById("star_ori_betelgeuse") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("star_ori_betelgeuse")!!
        )

    val POLARIS: CelestialObject
        get() = getById("star_umi_polaris") ?: CanonicalAstroCatalog.toCelestialObject(
            CanonicalAstroCatalog.getCanonicalObject("star_umi_polaris")!!
        )

    private val RAW_IRAN_CITIES = listOf(
        Triple("Nurabad City (NC)", "نورآباد ممسنی (NC)", 30.1141 to 51.5217),
        Triple("Tehran", "تهران", 35.6892 to 51.3890),
        Triple("Shiraz", "شیراز", 29.5918 to 52.5837),
        Triple("Isfahan", "اصفهان", 32.6546 to 51.6680),
        Triple("Tabriz", "تبریز", 38.0962 to 46.2694),
        Triple("Mashhad", "مشهد", 36.2972 to 59.6067),
        Triple("Kerman", "کرمان", 30.2839 to 57.0834),
        Triple("Ahvaz", "اهواز", 31.3183 to 48.6706),
        Triple("Rasht", "رشت", 37.2808 to 49.5832),
        Triple("Yazd", "یزد", 31.8974 to 54.3675),
        Triple("Kermanshah", "کرمانشاه", 34.3142 to 47.0650),
        Triple("Hamadan", "همدان", 34.7982 to 48.5146),
        Triple("Zahedan", "زاهدان", 29.4963 to 60.8629),
        Triple("Bandar Abbas", "بندرعباس", 27.1832 to 56.2666),
        Triple("Sanandaj", "سنندج", 35.3144 to 46.9923),
        Triple("Bushehr", "بوشهر", 28.9234 to 50.8382),
        Triple("Safashahr (Fars)", "صفاشهر (فارس)", 30.6158 to 53.1956)
    )

    val IRAN_CITIES: List<Triple<String, String, Pair<Double, Double>>>
        get() {
            val nurabad = RAW_IRAN_CITIES.firstOrNull { it.first.contains("Nurabad", ignoreCase = true) }
            val safashahr = RAW_IRAN_CITIES.firstOrNull { it.first.contains("Safashahr", ignoreCase = true) }
            val others = RAW_IRAN_CITIES.filter { it != nurabad && it != safashahr }
            return listOfNotNull(nurabad) + others + listOfNotNull(safashahr)
        }

    val DEFAULT_CONSTELLATIONS: List<ConstellationData>
        get() = ConstellationCatalog.getConstellations()

    fun getById(id: String, jd: Double = TimeEngine.getJulianDate()): CelestialObject? {
        val canonicalId = CanonicalAstroCatalog.resolveCanonicalId(id)
        val canonicalObj = CanonicalAstroCatalog.getCanonicalObject(canonicalId) ?: return null
        val timestampMs = TimeEngine.getTimestampFromJulianDate(jd)
        val state = AstroDispatchEngine.calculateState(canonicalObj.canonicalId, timestampMs)
        return if (state != null) {
            CanonicalAstroCatalog.toCelestialObject(
                canonicalObj = canonicalObj,
                dynamicRa = state.raDeg,
                dynamicDec = state.decDeg,
                dynamicMag = state.magnitude
            )
        } else {
            CanonicalAstroCatalog.toCelestialObject(canonicalObj)
        }
    }

    /**
     * Master catalog query: maps every canonical object from CanonicalAstroCatalog,
     * updating dynamic positions/magnitudes via AstroDispatchEngine.
     */
    fun getAllObjects(jd: Double = TimeEngine.getJulianDate()): List<CelestialObject> {
        val timestampMs = TimeEngine.getTimestampFromJulianDate(jd)
        return CanonicalAstroCatalog.getAllCanonicalObjects().map { canonicalObj ->
            val state = AstroDispatchEngine.calculateState(canonicalObj.canonicalId, timestampMs)
            if (state != null) {
                CanonicalAstroCatalog.toCelestialObject(
                    canonicalObj = canonicalObj,
                    dynamicRa = state.raDeg,
                    dynamicDec = state.decDeg,
                    dynamicMag = state.magnitude
                )
            } else {
                CanonicalAstroCatalog.toCelestialObject(canonicalObj)
            }
        }
    }

    fun getConstellations(): List<ConstellationData> {
        return ConstellationCatalog.getConstellations()
    }

    fun getAsterisms(): List<CelestialObject> {
        return CanonicalAstroCatalog.getAllCanonicalObjects()
            .filter { it.type == ObjectType.ASTERISM }
            .map { CanonicalAstroCatalog.toCelestialObject(it) }
    }

    fun getMeteorShowers(): List<CelestialObject> {
        return CanonicalAstroCatalog.getAllCanonicalObjects()
            .filter { it.type == ObjectType.METEOR_SHOWER }
            .map { CanonicalAstroCatalog.toCelestialObject(it) }
    }

    fun getDeepSkyObjects(): List<CelestialObject> {
        return CanonicalAstroCatalog.getAllCanonicalObjects()
            .filter {
                it.type == ObjectType.DEEP_SKY ||
                it.type == ObjectType.GALAXY ||
                it.type == ObjectType.NEBULA ||
                it.type == ObjectType.STAR_CLUSTER ||
                it.type == ObjectType.GLOBULAR_CLUSTER
            }
            .map { CanonicalAstroCatalog.toCelestialObject(it) }
    }

    fun getStars(): List<CelestialObject> {
        return CanonicalAstroCatalog.getAllCanonicalObjects()
            .filter { it.type == ObjectType.STAR }
            .map { CanonicalAstroCatalog.toCelestialObject(it) }
    }
}
