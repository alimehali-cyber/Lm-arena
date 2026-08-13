package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.CalculatedAstroState
import com.alijafari.red.astronomy.domain.CanonicalAstroObject
import com.alijafari.red.astronomy.domain.ConstellationData
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.domain.ObservationalInfo
import com.alijafari.red.astronomy.domain.PhysicalProperties
import com.alijafari.red.astronomy.domain.ScientificIdentifiers
import com.alijafari.red.astronomy.domain.StaticPosition
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared Astronomical Calculation and Dispatch Engine for RED Astronomy.
 *
 * Serves as the single unified dispatch layer connecting Canonical Object Identities
 * (CanonicalAstroObject / CanonicalAstroCatalog) to underlying high-precision scientific calculation
 * engines (SunEngine, MoonEngine, PlanetEngine, JupiterMoonsEngine, SatelliteEngine/ISSEngine,
 * GalacticEngine, and FrameTransformationEngine).
 */
object AstroDispatchEngine {

    private val frameTransformationEngine = FrameTransformationEngine()
    private val eclipseEngine = EclipseEngine()
    private val deepSkyEngine = DeepSkyEngine()

    fun getDeepSkyObjectById(id: String): DeepSkyCatalog.DeepSkyObject? {
        return deepSkyEngine.findById(id)
    }

    fun searchDeepSkyObjects(query: String): List<DeepSkyCatalog.DeepSkyObject> {
        return deepSkyEngine.searchByName(query)
    }

    fun getDeepSkyObjectsByType(type: DeepSkyCatalog.ObjectType): List<DeepSkyCatalog.DeepSkyObject> {
        return deepSkyEngine.filterByType(type)
    }

    fun getDeepSkyObjectsByConstellation(constellation: String): List<DeepSkyCatalog.DeepSkyObject> {
        return deepSkyEngine.filterByConstellation(constellation)
    }

    fun getBrightDeepSkyObjects(maxMagnitude: Double): List<DeepSkyCatalog.DeepSkyObject> {
        return deepSkyEngine.filterByMagnitude(maxMagnitude)
    }

    fun coneSearchDeepSky(
        raDeg: Double, decDeg: Double, radiusDeg: Double
    ): List<DeepSkyEngine.SearchResult> {
        return deepSkyEngine.coneSearch(raDeg, decDeg, radiusDeg)
    }

    fun getDeepSkyObjectPosition(
        obj: DeepSkyCatalog.DeepSkyObject,
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double
    ): DeepSkyEngine.ObjectPosition {
        return deepSkyEngine.calculatePosition(obj, astroTime, userLatDeg, userLonDeg)
    }

    fun getNextSolarEclipse(afterMs: Long = System.currentTimeMillis()): EclipseEngine.EclipseEvent? {
        return eclipseEngine.findNextSolarEclipse(afterMs)
    }

    fun getNextLunarEclipse(afterMs: Long = System.currentTimeMillis()): EclipseEngine.EclipseEvent? {
        return eclipseEngine.findNextLunarEclipse(afterMs)
    }

    fun getEclipsesInRange(startMs: Long, endMs: Long): List<EclipseEngine.EclipseEvent> {
        return eclipseEngine.findEclipses(startMs, endMs)
    }

    fun getNextNewMoon(afterMs: Long = System.currentTimeMillis()): Long {
        return eclipseEngine.findNextNewMoon(afterMs)
    }

    fun getNextFullMoon(afterMs: Long = System.currentTimeMillis()): Long {
        return eclipseEngine.findNextFullMoon(afterMs)
    }

    /**
     * Calculates the real-time dynamic astronomical state for any astronomical object using its
     * canonical ID, legacy ID, or search alias.
     */
    fun calculateState(
        idOrAlias: String,
        timestampMs: Long = System.currentTimeMillis(),
        userLatDeg: Double = 30.1141,
        userLonDeg: Double = 51.5217,
        elevationM: Double = 940.0
    ): CalculatedAstroState? {
        val canonicalId = CanonicalAstroCatalog.resolveCanonicalId(idOrAlias)
        val canonicalObj = CanonicalAstroCatalog.getCanonicalObject(canonicalId)
            ?: createDynamicFallbackObject(canonicalId, idOrAlias)
            ?: return null

        val jd = TimeEngine.getJulianDate(timestampMs)
        val lastDeg = TimeEngine.getLAST(jd, userLonDeg)

        return when (canonicalObj.canonicalId) {
            // --- 1. SUN ---
            "sun" -> {
                val sunPos = SunEngine.calculatePosition(jd)
                val horiz = SunEngine.getSunAltAz(jd, userLatDeg, userLonDeg)
                val distKm = sunPos.distanceAU * 149597870.7
                val distLy = distKm / 9.461e12

                CalculatedAstroState(
                    canonicalObject = canonicalObj,
                    jd = jd,
                    timestampMs = timestampMs,
                    userLatDeg = userLatDeg,
                    userLonDeg = userLonDeg,
                    elevationM = elevationM,
                    raDeg = sunPos.raDeg,
                    decDeg = sunPos.decDeg,
                    altitudeDeg = horiz.altitudeDeg,
                    azimuthDeg = horiz.azimuthDeg,
                    distanceKm = distKm,
                    distanceAU = sunPos.distanceAU,
                    distanceLightYears = distLy,
                    magnitude = -26.74,
                    angularDiameterArcsec = sunPos.apparentDiameterArcmin * 60.0,
                    isSunlit = true,
                    specializedData = sunPos
                )
            }

            // --- 2. EARTH ---
            "planet_earth" -> {
                CalculatedAstroState(
                    canonicalObject = canonicalObj,
                    jd = jd,
                    timestampMs = timestampMs,
                    userLatDeg = userLatDeg,
                    userLonDeg = userLonDeg,
                    elevationM = elevationM,
                    raDeg = 0.0,
                    decDeg = 0.0,
                    altitudeDeg = 90.0,
                    azimuthDeg = 0.0,
                    distanceKm = 0.0,
                    distanceAU = 0.0,
                    distanceLightYears = 0.0,
                    magnitude = -3.8,
                    isSunlit = true
                )
            }

            // --- 3. MOON ---
            "moon" -> {
                val moonData = MoonEngine.calculateMoon(jd, userLatDeg, userLonDeg, elevationM)
                val distAU = moonData.distanceKm / 149597870.7
                val distLy = moonData.distanceKm / 9.461e12

                CalculatedAstroState(
                    canonicalObject = canonicalObj,
                    jd = jd,
                    timestampMs = timestampMs,
                    userLatDeg = userLatDeg,
                    userLonDeg = userLonDeg,
                    elevationM = elevationM,
                    raDeg = moonData.raDeg,
                    decDeg = moonData.decDeg,
                    altitudeDeg = moonData.altitudeDeg,
                    azimuthDeg = moonData.azimuthDeg,
                    distanceKm = moonData.distanceKm,
                    distanceAU = distAU,
                    distanceLightYears = distLy,
                    magnitude = -12.74,
                    phaseNameEn = moonData.phaseNameEn,
                    phaseNameFa = moonData.phaseNameFa,
                    illuminationPercent = moonData.illuminationPercent,
                    angularDiameterArcsec = moonData.angularDiameterArcmin * 60.0,
                    isSunlit = moonData.illuminationPercent > 1.0,
                    parentCanonicalId = "planet_earth",
                    specializedData = moonData
                )
            }

            // --- 4. PLANETS ---
            "planet_mercury", "planet_venus", "planet_mars", "planet_jupiter",
            "planet_saturn", "planet_uranus", "planet_neptune", "planet_pluto" -> {
                val planetType = mapCanonicalToPlanetType(canonicalObj.canonicalId)
                val astroTime = AstroTime(timestampMs)
                val pos = PlanetEngine.calculatePlanet(planetType, astroTime)
                val horiz = frameTransformationEngine.trueEquatorialToHorizontal(
                    raTrueDeg = pos.raDeg,
                    decTrueDeg = pos.decDeg,
                    astroTime = astroTime,
                    latitudeDeg = userLatDeg,
                    longitudeDeg = userLonDeg,
                    elevationM = elevationM
                )
                val distKm = pos.distanceAU * 149597870.7
                val distLy = distKm / 9.461e12

                CalculatedAstroState(
                    canonicalObject = canonicalObj,
                    jd = jd,
                    timestampMs = timestampMs,
                    userLatDeg = userLatDeg,
                    userLonDeg = userLonDeg,
                    elevationM = elevationM,
                    raDeg = pos.raDeg,
                    decDeg = pos.decDeg,
                    altitudeDeg = horiz.altDeg,
                    azimuthDeg = horiz.azDeg,
                    distanceKm = distKm,
                    distanceAU = pos.distanceAU,
                    distanceLightYears = distLy,
                    magnitude = pos.magnitude,
                    illuminationPercent = pos.illuminatedFraction * 100.0,
                    angularDiameterArcsec = pos.angularDiameterArcsec,
                    isSunlit = true,
                    parentCanonicalId = "sun",
                    specializedData = pos
                )
            }

            // --- 5. JOVIAN MOONS (IO, EUROPA, GANYMEDE, CALLISTO, ELARA) ---
            "jup_io", "jup_europa", "jup_ganymede", "jup_callisto", "jup_elara" -> {
                val jupSystem = JupiterMoonsEngine.calculateJupiterMoons(jd)
                val targetMoonEnum = mapCanonicalToGalileanMoon(canonicalObj.canonicalId)
                val moonPos = jupSystem.moons.find { it.moon == targetMoonEnum }

                val jupRa = jupSystem.jupiterPos.raDeg
                val jupDec = jupSystem.jupiterPos.decDeg

                val moonRa = jupRa + ((moonPos?.offsetRaArcsec ?: 0.0) / 3600.0)
                val moonDec = jupDec + ((moonPos?.offsetDecArcsec ?: 0.0) / 3600.0)

                val astroTime = AstroTime(timestampMs)
                val horiz = frameTransformationEngine.trueEquatorialToHorizontal(
                    raTrueDeg = moonRa,
                    decTrueDeg = moonDec,
                    astroTime = astroTime,
                    latitudeDeg = userLatDeg,
                    longitudeDeg = userLonDeg,
                    elevationM = elevationM
                )
                val distKm = jupSystem.jupiterPos.distanceAU * 149597870.7
                val distLy = distKm / 9.461e12

                CalculatedAstroState(
                    canonicalObject = canonicalObj,
                    jd = jd,
                    timestampMs = timestampMs,
                    userLatDeg = userLatDeg,
                    userLonDeg = userLonDeg,
                    elevationM = elevationM,
                    raDeg = moonRa,
                    decDeg = moonDec,
                    altitudeDeg = horiz.altDeg,
                    azimuthDeg = horiz.azDeg,
                    distanceKm = distKm,
                    distanceAU = jupSystem.jupiterPos.distanceAU,
                    distanceLightYears = distLy,
                    magnitude = canonicalObj.physicalProperties.magnitude,
                    isSunlit = moonPos?.phenomenon != JupiterMoonsEngine.MoonPhenomenon.IN_ECLIPSE,
                    parentCanonicalId = "planet_jupiter",
                    specializedData = moonPos
                )
            }

            // --- 6. GALACTIC CENTER / SAGITTARIUS A* ---
            "sagittarius_a_star" -> {
                val galInfo = GalacticEngine.calculateGalacticCenter(jd, userLatDeg, userLonDeg, elevationM)
                CalculatedAstroState(
                    canonicalObject = canonicalObj,
                    jd = jd,
                    timestampMs = timestampMs,
                    userLatDeg = userLatDeg,
                    userLonDeg = userLonDeg,
                    elevationM = elevationM,
                    raDeg = 266.41683,
                    decDeg = -29.00781,
                    altitudeDeg = galInfo.altitudeDeg,
                    azimuthDeg = galInfo.azimuthDeg,
                    distanceLightYears = 26000.0,
                    magnitude = -5.0,
                    specializedData = galInfo
                )
            }

            // --- 7. SATELLITES (ISS, STARLINK, HUBBLE, JWST, TIANGONG, ETC.) ---
            else -> {
                if (canonicalObj.type == ObjectType.SATELLITE || canonicalObj.canonicalId.startsWith("sat_")) {
                    calculateSatelliteDispatch(canonicalObj, timestampMs, jd, userLatDeg, userLonDeg, elevationM, lastDeg)
                } else {
                    // Static objects (Stars, Deep Sky Objects, Radiants, Constellations, Asterisms)
                    calculateStaticObjectDispatch(canonicalObj, jd, timestampMs, userLatDeg, userLonDeg, elevationM, lastDeg)
                }
            }
        }
    }

    private fun calculateSatelliteDispatch(
        canonicalObj: CanonicalAstroObject,
        timestampMs: Long,
        jd: Double,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double,
        lastDeg: Double
    ): CalculatedAstroState {
        val noradId = canonicalObj.scientificIdentifiers.noradId ?: 25544
        val satItem = SatelliteCatalog.satellites.find { it.noradId == noradId || it.id == canonicalObj.canonicalId }
            ?: SatelliteCatalog.getById("iss_zarya")

        val liveState = SatelliteEngine.calculateSatelliteState(
            satellite = satItem,
            timestampMs = timestampMs,
            userLatDeg = userLatDeg,
            userLonDeg = userLonDeg
        )

        // Convert sub-latitude and sub-longitude to approximate RA/Dec for satellite
        val gmstDeg = TimeEngine.getGMST(jd)
        val satRaDeg = (liveState.topocentric.subLonDeg + gmstDeg) % 360.0
        val satDecDeg = liveState.topocentric.subLatDeg

        return CalculatedAstroState(
            canonicalObject = canonicalObj,
            jd = jd,
            timestampMs = timestampMs,
            userLatDeg = userLatDeg,
            userLonDeg = userLonDeg,
            elevationM = elevationM,
            raDeg = if (satRaDeg < 0) satRaDeg + 360.0 else satRaDeg,
            decDeg = satDecDeg,
            altitudeDeg = liveState.topocentric.elevationDeg,
            azimuthDeg = liveState.topocentric.azimuthDeg,
            distanceKm = liveState.topocentric.rangeKm,
            distanceAU = liveState.topocentric.rangeKm / 149597870.7,
            distanceLightYears = liveState.topocentric.rangeKm / 9.461e12,
            magnitude = liveState.apparentMagnitude,
            isSunlit = liveState.topocentric.isSunlit,
            satellitePassStatusEn = liveState.visibilityVerdictEn,
            satellitePassStatusFa = liveState.visibilityVerdictFa,
            parentCanonicalId = "planet_earth",
            specializedData = liveState
        )
    }

    private fun calculateStaticObjectDispatch(
        canonicalObj: CanonicalAstroObject,
        jd: Double,
        timestampMs: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double,
        lastDeg: Double
    ): CalculatedAstroState {
        val ra = canonicalObj.staticPosition?.raDeg ?: 0.0
        val dec = canonicalObj.staticPosition?.decDeg ?: 0.0

        val astroTime = AstroTime(timestampMs)
        val horiz = frameTransformationEngine.trueEquatorialToHorizontal(
            raTrueDeg = ra,
            decTrueDeg = dec,
            astroTime = astroTime,
            latitudeDeg = userLatDeg,
            longitudeDeg = userLonDeg,
            elevationM = elevationM
        )

        return CalculatedAstroState(
            canonicalObject = canonicalObj,
            jd = jd,
            timestampMs = timestampMs,
            userLatDeg = userLatDeg,
            userLonDeg = userLonDeg,
            elevationM = elevationM,
            raDeg = ra,
            decDeg = dec,
            altitudeDeg = horiz.altDeg,
            azimuthDeg = horiz.azDeg,
            distanceLightYears = canonicalObj.staticPosition?.distanceLightYears,
            magnitude = canonicalObj.physicalProperties.magnitude
        )
    }

    private fun mapCanonicalToPlanetType(canonicalId: String): PlanetEngine.PlanetType {
        return when (canonicalId) {
            "planet_mercury" -> PlanetEngine.PlanetType.MERCURY
            "planet_venus" -> PlanetEngine.PlanetType.VENUS
            "planet_mars" -> PlanetEngine.PlanetType.MARS
            "planet_jupiter" -> PlanetEngine.PlanetType.JUPITER
            "planet_saturn" -> PlanetEngine.PlanetType.SATURN
            "planet_uranus" -> PlanetEngine.PlanetType.URANUS
            "planet_neptune" -> PlanetEngine.PlanetType.NEPTUNE
            "planet_pluto" -> PlanetEngine.PlanetType.PLUTO
            else -> PlanetEngine.PlanetType.JUPITER
        }
    }

    private fun mapCanonicalToGalileanMoon(canonicalId: String): JupiterMoonsEngine.GalileanMoon {
        return when (canonicalId) {
            "jup_io" -> JupiterMoonsEngine.GalileanMoon.IO
            "jup_europa" -> JupiterMoonsEngine.GalileanMoon.EUROPA
            "jup_ganymede" -> JupiterMoonsEngine.GalileanMoon.GANYMEDE
            "jup_callisto" -> JupiterMoonsEngine.GalileanMoon.CALLISTO
            "jup_elara" -> JupiterMoonsEngine.GalileanMoon.ELARA
            else -> JupiterMoonsEngine.GalileanMoon.IO
        }
    }

    private fun createDynamicFallbackObject(canonicalId: String, rawInput: String): CanonicalAstroObject? {
        // Fallback for objects not explicitly registered in CanonicalAstroCatalog
        return null
    }

    data class Phase3VerificationReport(
        val sunCalculated: Boolean,
        val moonCalculated: Boolean,
        val earthCalculated: Boolean,
        val jupiterCalculated: Boolean,
        val elaraCalculated: Boolean,
        val issCalculated: Boolean,
        val sgrACalculated: Boolean,
        val elaraParentId: String?,
        val moonParentId: String?,
        val issParentId: String?,
        val isPassed: Boolean
    )

    fun verifyPhase3Integration(): Phase3VerificationReport {
        val now = System.currentTimeMillis()
        val lat = 30.1141
        val lon = 51.5217

        val sunState = calculateState("sun", now, lat, lon)
        val moonState = calculateState("moon", now, lat, lon)
        val earthState = calculateState("planet_earth", now, lat, lon)
        val jupState = calculateState("planet_jupiter", now, lat, lon)
        val elaraState = calculateState("jup_elara", now, lat, lon)
        val issState = calculateState("sat_25544", now, lat, lon)
        val sgrAState = calculateState("sagittarius_a_star", now, lat, lon)

        val sunOk = sunState != null && sunState.distanceAU != null && sunState.distanceAU > 0.9 && sunState.distanceAU < 1.1
        val moonOk = moonState != null && moonState.distanceKm != null && moonState.distanceKm > 350000.0 && moonState.distanceKm < 410000.0
        val earthOk = earthState != null && earthState.distanceKm == 0.0
        val jupOk = jupState != null && jupState.distanceAU != null && jupState.distanceAU > 4.0 && jupState.distanceAU < 6.5
        val elaraOk = elaraState != null && elaraState.parentCanonicalId == "planet_jupiter"
        val issOk = issState != null && issState.distanceKm != null && issState.distanceKm > 0.0
        val sgrAOk = sgrAState != null && sgrAState.distanceLightYears == 26000.0

        val passed = sunOk && moonOk && earthOk && jupOk && elaraOk && issOk && sgrAOk

        return Phase3VerificationReport(
            sunCalculated = sunOk,
            moonCalculated = moonOk,
            earthCalculated = earthOk,
            jupiterCalculated = jupOk,
            elaraCalculated = elaraOk,
            issCalculated = issOk,
            sgrACalculated = sgrAOk,
            elaraParentId = elaraState?.parentCanonicalId,
            moonParentId = moonState?.parentCanonicalId,
            issParentId = issState?.parentCanonicalId,
            isPassed = passed
        )
    }

    private val skyMapRenderer = SkyMapRenderer()

    fun renderSkyMap(
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: SkyMapRenderer.RenderSettings
    ): SkyMapRenderer.RenderResult {
        return skyMapRenderer.render(astroTime, userLatDeg, userLonDeg, settings)
    }

    fun identifyStarAt(
        x: Float,
        y: Float,
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: SkyMapRenderer.RenderSettings
    ): StarCatalog.Star? {
        return skyMapRenderer.identifyStarAt(x, y, astroTime, userLatDeg, userLonDeg, settings)
    }

    fun identifyDeepSkyAt(
        x: Float,
        y: Float,
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: SkyMapRenderer.RenderSettings
    ): DeepSkyCatalogExpanded.DeepSkyObject? {
        return skyMapRenderer.identifyDeepSkyAt(x, y, astroTime, userLatDeg, userLonDeg, settings)
    }

    // ============================================================
    // Unified API Surface
    // ============================================================

    // --- Time ---
    fun getCurrentAstroTime(): AstroTime = AstroTime.now()
    fun getDeltaT(year: Int, month: Int, day: Int): Double {
        val jd = TimeEngine.getJulianDate(
            AstroTime.fromUtcDate(year, month, day).utcMs
        )
        return TimeEngine.getDeltaTSeconds(jd)
    }

    // --- Coordinates ---
    fun equatorialToHorizontal(
        raDeg: Double, decDeg: Double,
        astroTime: AstroTime, latDeg: Double, lonDeg: Double
    ): FrameTransformationEngine.Horizontal {
        return frameTransformationEngine.equatorialToHorizontal(raDeg, decDeg, astroTime, latDeg, lonDeg)
    }

    fun calculateLAST(astroTime: AstroTime, longitudeDeg: Double): Double {
        return frameTransformationEngine.calculateLAST(astroTime, longitudeDeg)
    }

    // --- Ephemeris ---
    fun getSunPosition(astroTime: AstroTime): SunEngine.SunPosition {
        return SunEngine.calculateSun(astroTime)
    }

    fun getMoonPosition(astroTime: AstroTime): MoonEngine.MoonPosition {
        return MoonEngine.calculateMoon(astroTime)
    }

    fun getPlanetPositions(astroTime: AstroTime): List<PlanetEngine.PlanetPosition> {
        return PlanetEngine.PlanetType.values().map { PlanetEngine.calculatePlanet(it, astroTime) }
    }

    // --- Deep Sky ---
    fun getDeepSkyObject(id: String): DeepSkyCatalogExpanded.DeepSkyObject? {
        return DeepSkyCatalogExpanded.objects.firstOrNull {
            it.catalogId.equals(id, ignoreCase = true) || it.commonName?.equals(id, ignoreCase = true) == true
        }
    }

    fun searchDeepSky(query: String): List<DeepSkyCatalogExpanded.DeepSkyObject> {
        val q = query.lowercase()
        return DeepSkyCatalogExpanded.objects.filter {
            it.catalogId.lowercase().contains(q) ||
            it.commonName?.lowercase()?.contains(q) == true ||
            it.type.lowercase().contains(q)
        }
    }

    // --- Stars ---
    fun getStar(hipId: Int): StarCatalog.Star? {
        return StarCatalog.stars.firstOrNull { it.hipId == hipId }
    }

    fun searchStars(query: String): List<StarCatalog.Star> {
        val q = query.lowercase()
        return StarCatalog.stars.filter { it.name?.lowercase()?.contains(q) == true }
    }

    // --- Constellations ---
    fun getConstellation(code: String): ConstellationData? {
        return com.alijafari.red.astronomy.data.catalog.ConstellationCatalog.getConstellations().firstOrNull {
            it.code.equals(code, ignoreCase = true)
        }
    }

    fun getConstellationAt(raDeg: Double, decDeg: Double): ConstellationData? {
        // Use constellation center points for nearest-neighbor lookup
        // This is more accurate than using main stars
        val constellations = com.alijafari.red.astronomy.data.catalog.ConstellationCatalog.getConstellations()
        
        // Compute constellation centers from their main stars
        return constellations.minByOrNull { constellation ->
            val centerRa = constellation.mainStars.map { it.first }.average()
            val centerDec = constellation.mainStars.map { it.second }.average()
            val dRa = abs(centerRa - raDeg).coerceAtMost(360.0 - abs(centerRa - raDeg))
            val dDec = abs(centerDec - decDeg)
            dRa * dRa + dDec * dDec
        }
    }

    // --- Sky Map ---
    fun renderSky(
        astroTime: AstroTime,
        latDeg: Double,
        lonDeg: Double,
        settings: SkyMapRenderer.RenderSettings
    ): SkyMapRenderer.RenderResult {
        return skyMapRenderer.render(astroTime, latDeg, lonDeg, settings)
    }

    // --- Observability ---
    fun getSkyQuality(
        astroTime: AstroTime,
        latDeg: Double,
        lonDeg: Double,
        targetAltitudeDeg: Double? = null,
        targetMagnitude: Double? = null
    ): ObservabilityEngine.ObservabilityResult {
        val sunPos = SunEngine.calculateSun(astroTime)
        val sunHoriz = frameTransformationEngine.equatorialToHorizontal(
            sunPos.raDeg, sunPos.decDeg, astroTime, latDeg, lonDeg
        )
        val moonPos = MoonEngine.calculateMoon(astroTime)
        
        // Use provided values or sensible defaults for general sky quality
        val altDeg = targetAltitudeDeg ?: 45.0  // Default: evaluate at 45° altitude
        val mag = targetMagnitude ?: 3.0        // Default: evaluate for a typical naked-eye star
        
        return ObservabilityEngine.calculateObservability(
            altitudeDeg = altDeg,
            sunAltitudeDeg = sunHoriz.altDeg,
            moonIlluminationPercent = moonPos.illuminatedFraction * 100.0,
            objectMagnitude = mag
        )
    }

    // --- Eclipses ---
    fun getNextEclipse(astroTime: AstroTime): EclipseEngine.EclipseEvent? {
        val solar = eclipseEngine.findNextSolarEclipse(astroTime.utcMs)
        val lunar = eclipseEngine.findNextLunarEclipse(astroTime.utcMs)
        if (solar == null) return lunar
        if (lunar == null) return solar
        return if (solar.maximumMs < lunar.maximumMs) solar else lunar
    }

    // --- Satellites ---
    fun getSatellitePositions(
        astroTime: AstroTime,
        latDeg: Double,
        lonDeg: Double
    ): List<SatelliteLiveState> {
        return SatelliteCatalog.satellites.map { sat ->
            SatelliteEngine.calculateSatelliteState(sat, astroTime.utcMs, latDeg, lonDeg)
        }
    }

    // --- Jupiter Moons ---
    fun getJupiterMoons(astroTime: AstroTime): List<JupiterMoonsEngine.MoonPosition> {
        return JupiterMoonsEngine.calculateJupiterMoons(astroTime.jdTt).moons
    }
}
