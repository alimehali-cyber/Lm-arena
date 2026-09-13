package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ARDistanceFormatter
import com.alijafari.red.astronomy.domain.CalculatedAstroState
import com.alijafari.red.astronomy.domain.CanonicalAstroObject
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import org.junit.Assert.assertEquals
import org.junit.Test

class ARDistanceFormatterTest {

    private fun obj(id: String, type: ObjectType, distanceLy: Double = 0.0) = CelestialObject(
        id = id,
        type = type,
        nameEn = id,
        nameFa = id,
        raDeg = 0.0,
        decDeg = 0.0,
        magnitude = 0.0,
        constellationEn = "",
        constellationFa = "",
        distanceLightYears = distanceLy,
        category = "",
        descriptionEn = "",
        descriptionFa = "",
        observationTipEn = "",
        observationTipFa = ""
    )

    private fun state(type: ObjectType, distanceKm: Double) = CalculatedAstroState(
        canonicalObject = CanonicalAstroObject(
            canonicalId = "test",
            type = type,
            nameEn = "test",
            nameFa = "test"
        ),
        jd = 0.0,
        timestampMs = 0L,
        userLatDeg = 0.0,
        userLonDeg = 0.0,
        raDeg = 0.0,
        decDeg = 0.0,
        altitudeDeg = 0.0,
        azimuthDeg = 0.0,
        distanceKm = distanceKm,
        distanceLightYears = distanceKm / ARDistanceFormatter.KM_PER_LIGHT_YEAR
    )

    @Test
    fun planetsUseDynamicEngineDistanceInsteadOfStaticFallback() {
        val mars = obj("planet_mars", ObjectType.PLANET, distanceLy = 1000.0)
        val selected = ARDistanceFormatter.selectDistanceKm(mars, state(ObjectType.PLANET, 78_000_000.0))
        assertEquals(78_000_000.0, selected!!, 1e-6)
    }

    @Test
    fun starsKeepStaticCatalogDistance() {
        val star = obj("star_test", ObjectType.STAR, distanceLy = 10.0)
        val selected = ARDistanceFormatter.selectDistanceKm(star, state(ObjectType.STAR, 123.0))
        assertEquals(10.0 * ARDistanceFormatter.KM_PER_LIGHT_YEAR, selected!!, 1e-3)
    }

    @Test
    fun satelliteUsesDynamicTopocentricRange() {
        val sat = obj("sat_25544", ObjectType.SATELLITE, distanceLy = 1.0)
        val selected = ARDistanceFormatter.selectDistanceKm(sat, state(ObjectType.SATELLITE, 421.5))
        assertEquals(421.5, selected!!, 1e-6)
    }
}
