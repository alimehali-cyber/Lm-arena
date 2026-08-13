package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

/**
 * Deep sky object engine.
 * Provides catalog queries and position calculations for deep sky objects.
 */
class DeepSkyEngine {

    companion object {
        private const val DEG2RAD = Math.PI / 180.0
        private const val RAD2DEG = 180.0 / Math.PI
    }

    data class SearchResult(
        val `object`: DeepSkyCatalog.DeepSkyObject,
        val distanceDeg: Double  // Angular distance from search center
    )

    data class ObjectPosition(
        val `object`: DeepSkyCatalog.DeepSkyObject,
        val raDeg: Double,        // Apparent RA at target time
        val decDeg: Double,       // Apparent Dec at target time
        val azDeg: Double,        // Azimuth (0=N, 90=E)
        val altDeg: Double,       // Altitude (-90 to +90)
        val isUp: Boolean,        // Above horizon
        val transitTimeMs: Long,  // Next meridian transit
        val riseTimeMs: Long,     // Next rise
        val setTimeMs: Long       // Next set
    )

    private val frameEngine = FrameTransformationEngine()

    // ============================================================
    // Catalog Queries
    // ============================================================

    /**
     * Find an object by catalog ID (e.g., "M31", "NGC 224", "C1").
     * Case-insensitive, matches either catalogId or altCatalogId.
     */
    fun findById(id: String): DeepSkyCatalog.DeepSkyObject? {
        val normalized = id.trim().uppercase()
        return DeepSkyCatalog.objects.firstOrNull {
            it.catalogId.uppercase() == normalized ||
            it.altCatalogId?.uppercase() == normalized
        }
    }

    /**
     * Search by common name (partial match, case-insensitive).
     * Returns all matches.
     */
    fun searchByName(name: String): List<DeepSkyCatalog.DeepSkyObject> {
        val normalized = name.trim().lowercase()
        return DeepSkyCatalog.objects.filter {
            it.commonName?.lowercase()?.contains(normalized) == true ||
            it.catalogId.lowercase().contains(normalized) ||
            it.altCatalogId?.lowercase()?.contains(normalized) == true
        }
    }

    /**
     * Filter objects by type.
     */
    fun filterByType(type: DeepSkyCatalog.ObjectType): List<DeepSkyCatalog.DeepSkyObject> {
        return DeepSkyCatalog.objects.filter { it.type == type }
    }

    /**
     * Filter objects by constellation (3-letter IAU code).
     */
    fun filterByConstellation(constellation: String): List<DeepSkyCatalog.DeepSkyObject> {
        val normalized = constellation.trim().uppercase()
        return DeepSkyCatalog.objects.filter { it.constellation.uppercase() == normalized }
    }

    /**
     * Filter objects brighter than a given magnitude.
     */
    fun filterByMagnitude(maxMagnitude: Double): List<DeepSkyCatalog.DeepSkyObject> {
        return DeepSkyCatalog.objects.filter { it.magnitude <= maxMagnitude }
    }

    /**
     * Cone search: find all objects within a given angular radius of a sky position.
     * @param raDeg Center RA in degrees
     * @param decDeg Center Dec in degrees
     * @param radiusDeg Search radius in degrees
     * @return Matches sorted by angular distance
     */
    fun coneSearch(
        raDeg: Double,
        decDeg: Double,
        radiusDeg: Double
    ): List<SearchResult> {
        val results = DeepSkyCatalog.objects.mapNotNull { obj ->
            val objRaDeg = obj.raHours * 15.0
            val dist = angularSeparation(raDeg, decDeg, objRaDeg, obj.decDeg)
            if (dist <= radiusDeg) {
                SearchResult(obj, dist)
            } else {
                null
            }
        }
        return results.sortedBy { it.distanceDeg }
    }

    /**
     * Get all Messier objects.
     */
    fun getAllMessier(): List<DeepSkyCatalog.DeepSkyObject> {
        return DeepSkyCatalog.objects.filter { it.catalogId.startsWith("M") }
    }

    /**
     * Get all NGC objects.
     */
    fun getAllNgc(): List<DeepSkyCatalog.DeepSkyObject> {
        return DeepSkyCatalog.objects.filter { it.catalogId.startsWith("NGC") }
    }

    // ============================================================
    // Position Calculations
    // ============================================================

    /**
     * Calculate the apparent position of an object at a given time and location.
     */
    fun calculatePosition(
        obj: DeepSkyCatalog.DeepSkyObject,
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        userAltMeters: Double = 0.0
    ): ObjectPosition {
        // Precess from J2000.0 to target epoch
        val precessed = precessJ2000ToEpoch(
            obj.raHours * 15.0, obj.decDeg, astroTime
        )

        // Convert equatorial to horizontal
        val horizontal = frameEngine.equatorialToHorizontal(
            precessed.raDeg, precessed.decDeg,
            astroTime, userLatDeg, userLonDeg, userAltMeters
        )

        // Compute transit, rise, set times
        val transitMs = nextTransitTime(obj, astroTime, userLonDeg)
        val riseMs = nextRiseTime(obj, astroTime, userLatDeg, userLonDeg)
        val setMs = nextSetTime(obj, astroTime, userLatDeg, userLonDeg)

        return ObjectPosition(
            `object` = obj,
            raDeg = precessed.raDeg,
            decDeg = precessed.decDeg,
            azDeg = horizontal.azDeg,
            altDeg = horizontal.altDeg,
            isUp = horizontal.altDeg > 0.0,
            transitTimeMs = transitMs,
            riseTimeMs = riseMs,
            setTimeMs = setMs
        )
    }

    /**
     * Precess J2000.0 coordinates to the target epoch.
     * Uses IAU 1976 precession model (Meeus Chapter 21).
     */
    private fun precessJ2000ToEpoch(
        raJ2000Deg: Double,
        decJ2000Deg: Double,
        astroTime: AstroTime
    ): PrecessedPosition {
        val t = astroTime.jcTt  // Julian centuries from J2000.0

        // Precession angles (IAU 1976, Meeus 21.2)
        val zeta = (2306.2181 * t + 0.30188 * t * t + 0.017998 * t * t * t) / 3600.0
        val z = (2306.2181 * t + 1.09468 * t * t + 0.018203 * t * t * t) / 3600.0
        val theta = (2004.3109 * t - 0.42665 * t * t - 0.041833 * t * t * t) / 3600.0

        val a = cos(decJ2000Deg * DEG2RAD) * sin((raJ2000Deg + zeta) * DEG2RAD)
        val b = cos(decJ2000Deg * DEG2RAD) * cos((raJ2000Deg + zeta) * DEG2RAD) * cos(theta * DEG2RAD) -
                sin(decJ2000Deg * DEG2RAD) * sin(theta * DEG2RAD)
        val c = cos(decJ2000Deg * DEG2RAD) * cos((raJ2000Deg + zeta) * DEG2RAD) * sin(theta * DEG2RAD) +
                sin(decJ2000Deg * DEG2RAD) * cos(theta * DEG2RAD)

        val raNewDeg = normalizeAngle(atan2(a, b) * RAD2DEG + z)
        val decNewDeg = asin(c) * RAD2DEG

        return PrecessedPosition(raNewDeg, decNewDeg)
    }

    /**
     * Compute the next meridian transit time for an object.
     */
    private fun nextTransitTime(
        obj: DeepSkyCatalog.DeepSkyObject,
        astroTime: AstroTime,
        userLonDeg: Double
    ): Long {
        val raDeg = obj.raHours * 15.0
        val lstDeg = frameEngine.calculateLAST(astroTime, userLonDeg)
        
        // Hour angle at transit = 0
        val haTransit = 0.0
        val lstTransit = raDeg + haTransit
        
        // Time until LST reaches the object's RA
        var deltaHours = (lstTransit - lstDeg) / 15.0
        if (deltaHours < 0) deltaHours += 24.0
        
        return astroTime.utcMs + (deltaHours * 3600000.0).toLong()
    }

    /**
     * Compute the next rise time for an object.
     */
    private fun nextRiseTime(
        obj: DeepSkyCatalog.DeepSkyObject,
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double
    ): Long {
        val raDeg = obj.raHours * 15.0
        val decDeg = obj.decDeg
        val latRad = userLatDeg * DEG2RAD
        val decRad = decDeg * DEG2RAD

        // Standard altitude for rise/set (refraction + semidiameter)
        val h0 = -0.5667  // degrees

        // Hour angle at rise
        val cosH = (sin(h0 * DEG2RAD) - sin(latRad) * sin(decRad)) /
                   (cos(latRad) * cos(decRad))
        
        // Object never rises or sets
        if (cosH < -1.0 || cosH > 1.0) {
            return -1  // Circumpolar or never rises
        }

        val hRise = acos(cosH) * RAD2DEG  // Positive hour angle at rise

        val lstDeg = frameEngine.calculateLAST(astroTime, userLonDeg)
        val lstRise = raDeg - hRise
        var deltaHours = (lstRise - lstDeg) / 15.0
        if (deltaHours < 0) deltaHours += 24.0

        return astroTime.utcMs + (deltaHours * 3600000.0).toLong()
    }

    /**
     * Compute the next set time for an object.
     */
    private fun nextSetTime(
        obj: DeepSkyCatalog.DeepSkyObject,
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double
    ): Long {
        val raDeg = obj.raHours * 15.0
        val decDeg = obj.decDeg
        val latRad = userLatDeg * DEG2RAD
        val decRad = decDeg * DEG2RAD

        val h0 = -0.5667

        val cosH = (sin(h0 * DEG2RAD) - sin(latRad) * sin(decRad)) /
                   (cos(latRad) * cos(decRad))

        if (cosH < -1.0 || cosH > 1.0) {
            return -1
        }

        val hSet = -acos(cosH) * RAD2DEG  // Negative hour angle at set

        val lstDeg = frameEngine.calculateLAST(astroTime, userLonDeg)
        val lstSet = raDeg - hSet
        var deltaHours = (lstSet - lstDeg) / 15.0
        if (deltaHours < 0) deltaHours += 24.0

        return astroTime.utcMs + (deltaHours * 3600000.0).toLong()
    }

    /**
     * Determine the best viewing season for an object.
     * Based on RA: object is best viewed when it transits at midnight.
     * @return Month (1-12)
     */
    fun bestViewingMonth(obj: DeepSkyCatalog.DeepSkyObject): Int {
        val raHours = obj.raHours
        // Sun is opposite object at midnight transit: Sun RA = (raHours + 12) % 24
        // Sun RA ≈ 0h in March (month 3), so Month ≈ (Sun RA / 2) + 3
        val sunRa = (raHours + 12.0) % 24.0
        val month = (sunRa / 2.0) + 3.0
        var m = month.roundToInt()
        while (m > 12) m -= 12
        while (m < 1) m += 12
        return m
    }

    /**
     * Get the full constellation name from the 3-letter code.
     */
    fun constellationName(code: String): String {
        val normalized = code.trim().uppercase()
        return constellationNames.entries.firstOrNull { it.key.equals(normalized, ignoreCase = true) }?.value ?: code
    }

    /**
     * Angular separation between two sky positions.
     */
    private fun angularSeparation(
        ra1Deg: Double, dec1Deg: Double,
        ra2Deg: Double, dec2Deg: Double
    ): Double {
        val ra1Rad = ra1Deg * DEG2RAD
        val dec1Rad = dec1Deg * DEG2RAD
        val ra2Rad = ra2Deg * DEG2RAD
        val dec2Rad = dec2Deg * DEG2RAD

        val dRa = ra1Rad - ra2Rad
        val sep = acos(
            sin(dec1Rad) * sin(dec2Rad) + cos(dec1Rad) * cos(dec2Rad) * cos(dRa)
        )
        return sep * RAD2DEG
    }

    private fun normalizeAngle(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private data class PrecessedPosition(val raDeg: Double, val decDeg: Double)

    private val constellationNames = mapOf(
        "And" to "Andromeda",
        "Ant" to "Antlia",
        "Aps" to "Apus",
        "Aqr" to "Aquarius",
        "Aql" to "Aquila",
        "Ara" to "Ara",
        "Ari" to "Aries",
        "Aur" to "Auriga",
        "Boo" to "Boötes",
        "Cae" to "Caelum",
        "Cam" to "Camelopardalis",
        "Cnc" to "Cancer",
        "CVn" to "Canes Venatici",
        "CMa" to "Canis Major",
        "CMi" to "Canis Minor",
        "Cap" to "Capricornus",
        "Car" to "Carina",
        "Cas" to "Cassiopeia",
        "Cen" to "Centaurus",
        "Cep" to "Cepheus",
        "Cet" to "Cetus",
        "Cha" to "Chamaeleon",
        "Cir" to "Circinus",
        "Col" to "Columba",
        "Com" to "Coma Berenices",
        "CrA" to "Corona Australis",
        "CrB" to "Corona Borealis",
        "Crv" to "Corvus",
        "Crt" to "Crater",
        "Cru" to "Crux",
        "Cyg" to "Cygnus",
        "Del" to "Delphinus",
        "Dor" to "Dorado",
        "Dra" to "Draco",
        "Equ" to "Equuleus",
        "Eri" to "Eridanus",
        "For" to "Fornax",
        "Gem" to "Gemini",
        "Gru" to "Grus",
        "Her" to "Hercules",
        "Hor" to "Horologium",
        "Hya" to "Hydra",
        "Hyi" to "Hydrus",
        "Ind" to "Indus",
        "Lac" to "Lacerta",
        "Leo" to "Leo",
        "LMi" to "Leo Minor",
        "Lep" to "Lepus",
        "Lib" to "Libra",
        "Lup" to "Lupus",
        "Lyn" to "Lynx",
        "Lyr" to "Lyra",
        "Men" to "Mensa",
        "Mic" to "Microscopium",
        "Mon" to "Monoceros",
        "Mus" to "Musca",
        "Nor" to "Norma",
        "Oct" to "Octans",
        "Oph" to "Ophiuchus",
        "Ori" to "Orion",
        "Pav" to "Pavo",
        "Peg" to "Pegasus",
        "Per" to "Perseus",
        "Phe" to "Phoenix",
        "Pic" to "Pictor",
        "Psc" to "Pisces",
        "PsA" to "Piscis Austrinus",
        "Pup" to "Puppis",
        "Pyx" to "Pyxis",
        "Ret" to "Reticulum",
        "Sge" to "Sagitta",
        "Sgr" to "Sagittarius",
        "Sco" to "Scorpius",
        "Scl" to "Sculptor",
        "Sct" to "Scutum",
        "Ser" to "Serpens",
        "Sex" to "Sextans",
        "Tau" to "Taurus",
        "Tel" to "Telescopium",
        "Tri" to "Triangulum",
        "TrA" to "Triangulum Australe",
        "Tuc" to "Tucana",
        "UMa" to "Ursa Major",
        "UMi" to "Ursa Minor",
        "Vel" to "Vela",
        "Vir" to "Virgo",
        "Vol" to "Volans",
        "Vul" to "Vulpecula"
    )
}
