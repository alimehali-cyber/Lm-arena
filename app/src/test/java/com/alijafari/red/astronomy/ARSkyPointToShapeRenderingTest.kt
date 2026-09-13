package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.CelestialObjectSizes
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shared magnitude + zoom rule that decides whether an AR object is drawn as an unresolved point
 * of light or as its full type glyph.
 *
 * These are pure-logic assertions on the single function every object type goes through
 * ([CelestialObjectSizes.resolveArRenderPlan]); the emulator/instrumented smoke test is not part of
 * the pipeline any more, so the behavioural contract is pinned here instead.
 */
class ARSkyPointToShapeRenderingTest {

    private val jd = TimeEngine.getJulianDate()

    private fun body(
        id: String,
        type: ObjectType,
        magnitude: Double,
        category: String = "",
        angularSizeArcmin: Double? = null
    ) = CelestialObject(
        id = id,
        type = type,
        nameEn = id,
        nameFa = id,
        raDeg = 0.0,
        decDeg = 0.0,
        magnitude = magnitude,
        constellationEn = "",
        constellationFa = "",
        distanceLightYears = 0.0,
        category = category,
        descriptionEn = "",
        descriptionFa = "",
        observationTipEn = "",
        observationTipFa = "",
        angularSizeArcmin = angularSizeArcmin
    )

    private fun plan(
        obj: CelestialObject,
        zoom: Float,
        proximity: Float = 1f,
        previouslyShapeTier: Boolean = false,
        dpPerDegree: Float = DP_PER_DEGREE
    ) = CelestialObjectSizes.resolveArRenderPlan(
        obj = obj,
        zoomFactor = zoom,
        proximityScale = proximity,
        density = DENSITY,
        dpPerDegree = dpPerDegree,
        previouslyShapeTier = previouslyShapeTier
    )

    private fun star(magnitude: Double, angular: Double? = null) =
        body("star_test_$magnitude", ObjectType.STAR, magnitude, angularSizeArcmin = angular)

    private companion object {
        const val DENSITY = 2.625f
        const val DP_PER_DEGREE = 7.5f
        val ZOOM_LEVELS = floatArrayOf(0.75f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f)
    }

    // -----------------------------------------------------------------------------------------
    // Bright bodies are always shape tier — no per-type special case
    // -----------------------------------------------------------------------------------------

    @Test
    fun everythingBrighterThanTheVisibilityLimitIsShapeTierAtEveryZoomLevel() {
        val magnitudes = doubleArrayOf(-26.74, -12.74, -6.0, -2.8, -1.4, 0.0, 1.5, 2.9, 4.0, 4.5, 4.79)
        for (mag in magnitudes) {
            for (zoom in ZOOM_LEVELS) {
                val result = plan(star(mag), zoom)
                assertEquals(
                    "magnitude $mag at zoom $zoom must be shape tier",
                    CelestialObjectSizes.ArRenderTier.SHAPE,
                    result.tier
                )
                assertEquals("magnitude $mag at zoom $zoom must be fully blended", 1.0f, result.blend, 1e-6f)
                assertEquals("magnitude $mag at zoom $zoom must not draw a dot", 0.0f, result.dotAlpha, 1e-6f)
                assertTrue("shape-tier objects may be labelled", result.labelEligible)
            }
        }
    }

    @Test
    fun sunAndMoonFromTheRealCatalogueAreNeverDotTier() {
        val sun = AstronomyCatalog.SUN
        val moon = AstronomyCatalog.MOON
        assertTrue("Sanity: the Sun is brighter than the limit", sun.magnitude <= CelestialObjectSizes.AR_VISIBILITY_LIMIT_MAG)
        assertTrue("Sanity: the Moon is brighter than the limit", moon.magnitude <= CelestialObjectSizes.AR_VISIBILITY_LIMIT_MAG)
        for (obj in listOf(sun, moon)) {
            for (zoom in ZOOM_LEVELS) {
                assertEquals(
                    "${obj.id} must render its full glyph at zoom $zoom",
                    CelestialObjectSizes.ArRenderTier.SHAPE,
                    plan(obj, zoom).tier
                )
            }
        }
    }

    /**
     * The planets are not hard-coded into the shape tier: the tier a planet gets is exactly the tier
     * any other type gets for the same magnitude, angular extent and zoom. The classical planets
     * simply sit brighter than the visibility limit in practice, so they resolve immediately, while
     * a body at Uranus/Neptune brightness behaves like every other faint object.
     */
    @Test
    fun planetsFollowTheSharedRuleRatherThanAHardCodedException() {
        val planets = AstronomyCatalog.getAllObjects(jd)
            .filter { it.type == ObjectType.PLANET || it.type == ObjectType.DWARF_PLANET }
        assertTrue("The catalogue must contain planets for this check", planets.isNotEmpty())

        for (planet in planets) {
            val sameLightAsAStar = body(
                id = planet.id + "_as_star",
                type = ObjectType.STAR,
                magnitude = planet.magnitude,
                angularSizeArcmin = planet.angularSizeArcmin
            )
            for (zoom in ZOOM_LEVELS) {
                for (previous in booleanArrayOf(false, true)) {
                    assertEquals(
                        "${planet.id} (mag ${planet.magnitude}) at zoom $zoom must be tiered like any other type",
                        plan(planet, zoom, previouslyShapeTier = previous).tier,
                        plan(sameLightAsAStar, zoom, previouslyShapeTier = previous).tier
                    )
                }
            }
            val bright = planet.magnitude <= CelestialObjectSizes.AR_VISIBILITY_LIMIT_MAG
            if (bright) {
                for (zoom in ZOOM_LEVELS) {
                    assertEquals(
                        "${planet.id} (mag ${planet.magnitude}) at zoom $zoom",
                        CelestialObjectSizes.ArRenderTier.SHAPE,
                        plan(planet, zoom).tier
                    )
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Faint bodies: dot by default, resolved by zoom
    // -----------------------------------------------------------------------------------------

    @Test
    fun faintObjectsStartAsUnlabeledDotsAtDefaultZoom() {
        for (mag in doubleArrayOf(5.0, 5.5, 6.0, 6.5, 7.5, 9.0, 13.0)) {
            val result = plan(star(mag), 1.0f)
            assertEquals("mag $mag must be a dot at default zoom", CelestialObjectSizes.ArRenderTier.DOT, result.tier)
            assertEquals("mag $mag must not be blended at default zoom", 0.0f, result.blend, 1e-6f)
            assertFalse("dot-tier objects are never labelled ambiently", result.labelEligible)
            assertTrue(
                "the dot must stay visually minimal (dp radius)",
                result.dotRadiusPx / DENSITY <= CelestialObjectSizes.AR_DOT_MAX_RADIUS_DP * 1.35f + 0.001f
            )
        }
    }

    @Test
    fun zoomingInResolvesFaintObjectsAndLabelsThem() {
        // A 5th magnitude star crosses the reveal threshold at a reachable pinch-zoom, and every
        // magnitude step needs more zoom than the one before it.
        var previousRevealZoom = 0f
        for (mag in doubleArrayOf(5.0, 5.5, 6.0)) {
            val found = ZOOM_LEVELS.firstOrNull { plan(star(mag), it).tier == CelestialObjectSizes.ArRenderTier.SHAPE }
            assertTrue("mag $mag must resolve somewhere within the pinch-zoom range", found != null)
            val revealZoom = found!!
            assertTrue("mag $mag must still be a dot at default zoom", revealZoom > 1.0f)
            assertTrue(
                "fainter objects must need more zoom ($previousRevealZoom -> $revealZoom for mag $mag)",
                revealZoom > previousRevealZoom
            )
            previousRevealZoom = revealZoom
            val resolved = plan(star(mag), revealZoom)
            assertTrue("resolved objects become label-eligible", resolved.labelEligible)
            assertEquals("resolved objects draw their full glyph", 1.0f, resolved.blend, 1e-6f)
        }
    }

    @Test
    fun transitionIsMonotonicAndFlipperFreeAcrossASlowZoomSweep() {
        for (mag in doubleArrayOf(4.9, 5.2, 5.6, 6.0, 6.4, 7.0)) {
            val obj = star(mag)
            var previousShape = false
            var flips = 0
            var lastRadius = -1f
            for (step in 0..325) {
                val zoom = 0.75f + step * 0.01f
                val result = plan(obj, zoom, previouslyShapeTier = previousShape)
                if ((result.tier == CelestialObjectSizes.ArRenderTier.SHAPE) != previousShape) flips++
                previousShape = result.tier == CelestialObjectSizes.ArRenderTier.SHAPE
                assertTrue(
                    "drawn size must grow monotonically while zooming in (mag $mag @ $zoom)",
                    result.drawnRadiusPx >= lastRadius - 1e-4f
                )
                lastRadius = result.drawnRadiusPx
            }
            assertTrue(
                "a monotone zoom-in may change tier at most once (mag $mag)",
                flips <= 1
            )
        }
        // And the objects the user can actually reach do change tier exactly once.
        var previousShape = false
        var flips = 0
        for (step in 0..325) {
            val result = plan(star(5.0), 0.75f + step * 0.01f, previouslyShapeTier = previousShape)
            val shape = result.tier == CelestialObjectSizes.ArRenderTier.SHAPE
            if (shape != previousShape) flips++
            previousShape = shape
        }
        assertEquals("a 5th magnitude star resolves exactly once per sweep", 1, flips)
    }

    @Test
    fun hysteresisBandKeepsBorderlineObjectsOnTheirPreviousTier() {
        val obj = star(6.0)
        val borderlineZoom = 2.7f
        val borderline = plan(obj, borderlineZoom)
        assertTrue(
            "fixture must land inside the hysteresis band",
            borderline.blend > 0.0f && borderline.blend < 1.0f
        )
        assertEquals(
            "an object that was a dot stays a dot inside the band",
            CelestialObjectSizes.ArRenderTier.DOT,
            plan(obj, borderlineZoom, previouslyShapeTier = false).tier
        )
        assertEquals(
            "an object that was resolved stays resolved inside the band",
            CelestialObjectSizes.ArRenderTier.SHAPE,
            plan(obj, borderlineZoom, previouslyShapeTier = true).tier
        )

        // The band has real width: above the reveal size the tier is shape regardless of history,
        // below the retract size it is dot regardless of history.
        assertEquals(
            CelestialObjectSizes.ArRenderTier.SHAPE,
            plan(obj, 3.4f, previouslyShapeTier = false).tier
        )
        assertEquals(
            CelestialObjectSizes.ArRenderTier.DOT,
            plan(obj, 2.0f, previouslyShapeTier = true).tier
        )
    }

    @Test
    fun extendedObjectsResolveEarlierThanPointsOfTheSameMagnitude() {
        val faintNebula = body("dso_faint", ObjectType.DEEP_SKY, 6.0, category = "Emission Nebula", angularSizeArcmin = 90.0)
        val faintStar = star(6.0)
        val nebulaPlan = plan(faintNebula, 1.0f)
        val starPlan = plan(faintStar, 1.0f)
        assertEquals("a 1.5° wide mag-6 smudge is already resolved", CelestialObjectSizes.ArRenderTier.SHAPE, nebulaPlan.tier)
        assertEquals("a mag-6 point source is not", CelestialObjectSizes.ArRenderTier.DOT, starPlan.tier)
        assertTrue("the resolved object is drawn bigger", nebulaPlan.drawnRadiusPx > starPlan.drawnRadiusPx)
    }

    @Test
    fun dotSizeGrowsSmoothlyWithZoomAndBrightness() {
        // The dot-tier size curve itself: smoother growth with zoom, larger for the brighter half
        // of the faint range, and always inside the "visually minimal" band.
        val brightAtOne = CelestialObjectSizes.arDotRadiusDp(5.0, 1.0f)
        val dimAtOne = CelestialObjectSizes.arDotRadiusDp(6.5, 1.0f)
        assertTrue("brighter faint objects get a larger dot", brightAtOne > dimAtOne)
        assertTrue(
            "dots grow with zoom",
            CelestialObjectSizes.arDotRadiusDp(5.5, 4.0f) > CelestialObjectSizes.arDotRadiusDp(5.5, 0.75f)
        )
        for (mag in doubleArrayOf(4.9, 6.0, 8.0, 10.0, 13.0)) {
            for (zoom in ZOOM_LEVELS) {
                val dp = CelestialObjectSizes.arDotRadiusDp(mag, zoom)
                assertTrue("dot radius is bounded below (mag $mag @ $zoom)", dp >= CelestialObjectSizes.AR_DOT_MIN_RADIUS_DP - 1e-4f)
                assertTrue("dot radius is bounded above (mag $mag @ $zoom)", dp <= CelestialObjectSizes.AR_DOT_MAX_RADIUS_DP * 1.35f + 1e-4f)
            }
        }
        assertTrue(
            "brighter faint objects get a brighter dot",
            CelestialObjectSizes.arDotAlpha(5.0) > CelestialObjectSizes.arDotAlpha(6.5)
        )
        assertTrue(
            "dot alpha stays translucent",
            CelestialObjectSizes.arDotAlpha(4.9) <= CelestialObjectSizes.AR_DOT_MAX_ALPHA + 1e-4f &&
                CelestialObjectSizes.arDotAlpha(15.0) >= CelestialObjectSizes.AR_DOT_MIN_ALPHA - 1e-4f
        )

        // Through the plan, a dot is never bigger than the glyph it grows into.
        val brightFaint = star(5.0)
        val dimFaint = star(6.5)
        assertTrue(
            "brighter members of the faint range still get the larger rendered dot",
            plan(brightFaint, 1.0f).dotRadiusPx >= plan(dimFaint, 1.0f).dotRadiusPx
        )
        var previous = 0f
        for (zoom in floatArrayOf(0.75f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f)) {
            val radius = plan(brightFaint, zoom).dotRadiusPx
            assertTrue("dot size must not shrink with zoom", radius >= previous - 1e-4f)
            previous = radius
        }
    }

    @Test
    fun dotIsNeverLargerThanTheGlyphItGrowsInto() {
        for (mag in doubleArrayOf(4.9, 5.5, 6.0, 7.0, 9.0, 13.0)) {
            for (zoom in ZOOM_LEVELS) {
                val result = plan(star(mag), zoom)
                assertTrue(
                    "dot radius must not exceed the resolved glyph radius (mag $mag @ $zoom)",
                    result.dotRadiusPx <= result.drawnRadiusPx + 1e-3f
                )
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Shared sizing helpers and composition with the existing magnification
    // -----------------------------------------------------------------------------------------

    /**
     * The decluttering goal, measured on the real catalogue: at the default pinch-zoom almost every
     * deep-sky object fainter than the visibility limit is an unresolved point, while the objects a
     * naked-eye observer actually knows stay fully drawn.
     */
    @Test
    fun faintDeepSkyObjectsAreDotsAtDefaultZoomOnTheRealCatalogue() {
        val faintDeepSky = AstronomyCatalog.getAllObjects(jd)
            .filter {
                it.type == ObjectType.DEEP_SKY || it.type == ObjectType.GALAXY || it.type == ObjectType.NEBULA ||
                    it.type == ObjectType.STAR_CLUSTER || it.type == ObjectType.GLOBULAR_CLUSTER
            }
            .filter { it.magnitude > CelestialObjectSizes.AR_VISIBILITY_LIMIT_MAG }
        assertTrue("The catalogue must expose faint deep-sky objects for this check", faintDeepSky.size > 50)

        val dots = faintDeepSky.count {
            plan(it, 1.0f).tier == CelestialObjectSizes.ArRenderTier.DOT
        }
        val dotShare = dots.toFloat() / faintDeepSky.size
        assertTrue(
            "at least 85% of faint deep-sky objects should be decluttered into dots at default zoom (was ${(dotShare * 100).toInt()}%)",
            dotShare >= 0.85f
        )
    }

    @Test
    fun showpieceObjectsAreFullyDrawnImmediatelyAtAnyZoom() {
        val showpieces = listOf(
            "sun", "moon", "planet_jupiter", "planet_venus", "planet_mars", "planet_saturn",
            "star_cma_sirius", "star_lyr_vega", "star_ori_betelgeuse",
            "dso_m31_andromeda", "dso_m45_pleiades", "dso_m42_orion_nebula"
        )
        for (id in showpieces) {
            val obj = AstronomyCatalog.getById(id, jd) ?: error("$id must exist in the catalogue")
            for (zoom in ZOOM_LEVELS) {
                val result = plan(obj, zoom)
                assertEquals(
                    "$id (mag ${obj.magnitude}) must keep its full glyph at zoom $zoom",
                    CelestialObjectSizes.ArRenderTier.SHAPE,
                    result.tier
                )
                assertEquals("$id must be label-eligible immediately", 1.0f, result.blend, 1e-6f)
                assertTrue("$id may carry an ambient label", result.labelEligible)
            }
        }
    }

    @Test
    fun brightnessRatioFollowsTheFluxToDiameterRelation() {
        assertEquals(1.0f, CelestialObjectSizes.arBrightnessDiameterRatio(4.8), 1e-4f)
        assertEquals(0.631f, CelestialObjectSizes.arBrightnessDiameterRatio(5.8), 1e-3f)
        assertEquals(2.512f, CelestialObjectSizes.arBrightnessDiameterRatio(2.8), 1e-3f)
        assertEquals(0.1f, CelestialObjectSizes.arBrightnessDiameterRatio(9.8), 1e-4f)
    }

    @Test
    fun centreProximityMagnificationFeedsTheSameSizeAsTheGlyph() {
        val obj = star(6.0)
        val offCentre = CelestialObjectSizes.calculateProximityScale(4.5f, 4.5f)
        assertEquals("off the proximity threshold there is no magnification", 1.0f, offCentre, 1e-6f)
        val centred = CelestialObjectSizes.calculateProximityScale(0.1f, 0.1f)
        assertEquals("at the centre the existing 2.5x magnification still applies", 2.5f, centred, 1e-6f)

        val sizeOffCentre = CelestialObjectSizes.arEffectiveSizeDp(6.0, null, 1.0f, DP_PER_DEGREE, offCentre)
        val sizeCentred = CelestialObjectSizes.arEffectiveSizeDp(6.0, null, 1.0f, DP_PER_DEGREE, centred)
        assertEquals(sizeOffCentre * 2.5f, sizeCentred, 1e-3f)

        // Aiming at a faint object resolves it sooner: at 1.5x zoom it is still a dot off the
        // centre, and already a shape when the centre-proximity magnification applies to it.
        assertEquals(
            "off-centre at 1.5x a mag-6 star is still a dot",
            CelestialObjectSizes.ArRenderTier.DOT,
            plan(obj, 1.5f, proximity = offCentre).tier
        )
        assertEquals(
            "centred at 1.5x the same star is resolved",
            CelestialObjectSizes.ArRenderTier.SHAPE,
            plan(obj, 1.5f, proximity = centred).tier
        )
    }

    @Test
    fun shapeTierGlyphSizesMatchTheExistingPerTypeLogic() {
        val moon = body("moon", ObjectType.MOON, -12.7)
        val moonPlan = plan(moon, 1.0f)
        assertEquals(
            "the Moon keeps MOON_SIZE_DP / 2 as its radius",
            CelestialObjectSizes.MOON_SIZE_DP / 2f * DENSITY,
            moonPlan.drawnRadiusPx,
            1e-3f
        )

        val jupiter = body("planet_jupiter", ObjectType.PLANET, -2.5)
        assertEquals(
            "planets keep PLANET_INNER_SIZE_DP / 2 as their radius",
            CelestialObjectSizes.PLANET_INNER_SIZE_DP / 2f * DENSITY,
            plan(jupiter, 1.0f).drawnRadiusPx,
            1e-3f
        )

        val brightStar = body("star_bright", ObjectType.STAR, 1.0)
        assertEquals(
            "stars keep the (4 - magnitude) glyph radius",
            3.0f * DENSITY,
            plan(brightStar, 1.0f).drawnRadiusPx,
            1e-3f
        )
        assertEquals(
            "and the same radius at every zoom level, as before",
            3.0f * DENSITY,
            plan(brightStar, 4.0f).drawnRadiusPx,
            1e-3f
        )

        val deepSky = body("dso_x", ObjectType.DEEP_SKY, 3.9, category = "Spiral Galaxy")
        assertEquals(
            "galaxies keep DSO_GALAXY_SIZE_DP / 2",
            CelestialObjectSizes.DSO_GALAXY_SIZE_DP / 2f * DENSITY,
            plan(deepSky, 1.0f).drawnRadiusPx,
            1e-3f
        )
    }

    @Test
    fun unknownFieldOfViewFallsBackToThePointSizeTerm() {
        val obj = star(6.0)
        val withFov = plan(obj, 2.0f, dpPerDegree = DP_PER_DEGREE)
        val withoutFov = plan(obj, 2.0f, dpPerDegree = 0f)
        assertEquals("no FOV must not crash the rule", withFov.tier, withoutFov.tier)
        val extended = body("dso_big", ObjectType.DEEP_SKY, 6.0, category = "Galaxy", angularSizeArcmin = 90.0)
        assertEquals(
            "without a usable FOV an extended object is treated as a point until zoomed",
            CelestialObjectSizes.ArRenderTier.DOT,
            plan(extended, 1.0f, dpPerDegree = 0f).tier
        )
    }

    @Test
    fun pointLikeAndExtendedGlyphTypesAreClassifiedForTheRenderer() {
        assertTrue(CelestialObjectSizes.isArPointLikeGlyph(ObjectType.STAR))
        assertTrue(CelestialObjectSizes.isArPointLikeGlyph(ObjectType.METEOR_SHOWER))
        assertTrue(CelestialObjectSizes.isArPointLikeGlyph(ObjectType.ASTERISM))
        assertFalse(CelestialObjectSizes.isArPointLikeGlyph(ObjectType.DEEP_SKY))
        assertFalse(CelestialObjectSizes.isArPointLikeGlyph(ObjectType.PLANET))
        assertFalse(CelestialObjectSizes.isArPointLikeGlyph(ObjectType.SATELLITE))
        assertEquals(
            CelestialObjectSizes.AR_POINT_GLYPH_MIN_RADIUS_DP,
            CelestialObjectSizes.getGlyphRadiusDp(star(12.0)),
            1e-4f
        )
        assertEquals(
            CelestialObjectSizes.AR_POINT_GLYPH_MAX_RADIUS_DP,
            CelestialObjectSizes.getGlyphRadiusDp(star(-3.0)),
            1e-4f
        )
    }
}
