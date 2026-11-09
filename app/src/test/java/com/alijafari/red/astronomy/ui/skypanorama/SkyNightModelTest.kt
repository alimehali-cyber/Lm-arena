package com.alijafari.red.astronomy.ui.skypanorama

import com.alijafari.red.astronomy.ar.core.SkyRenderModel
import com.alijafari.red.astronomy.ui.rendering.NightSkyPalette
import com.alijafari.red.astronomy.ui.rendering.SkySceneModel
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * The physical night sky under the galaxy plate: the photometry, its behaviour, and the contract the
 * shader is written against. Pure JVM, no GL.
 */
class SkyNightModelTest {

    // ---- Airglow and light pollution -----------------------------------------------------------

    @Test
    fun aBortleOneSkyIsTheNaturalAirglowAndNothingElse() {
        // Bortle 1's tabulated zenith brightness (21.99) is a hair darker than the natural value the
        // airglow term uses (21.95), so the artificial excess is exactly zero there, not negative.
        assertEquals(0.0, SkyNightModel.artificialLuminance(1), 1e-12)
        assertEquals(SkyNightModel.naturalAirglow, SkyNightModel.zenithLuminance(1), SkyNightModel.naturalAirglow * 0.05)
        assertEquals(
            SkyRenderModel.magArcsec2ToLuminance(SkyRenderModel.NATURAL_ZENITH_MAG_ARCSEC2),
            SkyNightModel.naturalAirglow,
            1e-12
        )
    }

    @Test
    fun lightPollutionGrowsMonotonicallyWithTheBortleClass() {
        var previous = -1.0
        for (b in 1..9) {
            val lp = SkyNightModel.artificialLuminance(b)
            assertTrue("Bortle $b must be brighter than $previous", lp > previous)
            previous = lp
        }
        // A city sky (Bortle 8) is at least two magnitudes brighter than a pristine one.
        val pristine = SkyNightModel.zenithLuminance(1)
        val city = SkyNightModel.zenithLuminance(8)
        assertTrue("city must be much brighter", city > pristine * 20.0)
    }

    @Test
    fun airglowBrightensTowardsTheHorizonAndReddensLessThanItDims() {
        val zenith = SkyNightModel.skyBrightnessMagArcsec2(
            90.0, SkyNightModel.naturalAirglow, 0.0, SkyPanoramaSkyModel.EXTINCTION_V
        )
        val horizon = SkyNightModel.skyBrightnessMagArcsec2(
            10.0, SkyNightModel.naturalAirglow, 0.0, SkyPanoramaSkyModel.EXTINCTION_V
        )
        // Magnitude is inverted: a brighter sky has a smaller number.
        assertTrue("horizon ($horizon) must be brighter than zenith ($zenith)", horizon < zenith)
        // A dark site's zenith sits where the Bortle scale says it does.
        assertEquals(SkyRenderModel.NATURAL_ZENITH_MAG_ARCSEC2, zenith, 0.15)
    }

    @Test
    fun airmassMatchesThePublishedKastenYoungValues() {
        assertEquals(1.0, SkyNightModel.airmass(90.0), 1e-3)
        assertEquals(2.0, SkyNightModel.airmass(30.0), 0.02)
        assertEquals(38.0, SkyNightModel.airmass(0.0), 1.5)
        assertTrue(SkyNightModel.airmass(0.0) > SkyNightModel.airmass(45.0))
    }

    // ---- Moonlight ------------------------------------------------------------------------------

    @Test
    fun moonlightIsZeroWithTheMoonDownAndGrowsWithItsIllumination() {
        assertEquals(0.0, SkyNightModel.moonSkyLuminance(-20.0, 100.0), 0.0)
        val crescent = SkyNightModel.moonSkyLuminance(40.0, 10.0)
        val gibbous = SkyNightModel.moonSkyLuminance(40.0, 80.0)
        val full = SkyNightModel.moonSkyLuminance(40.0, 100.0)
        assertTrue("crescent must be dark", crescent < gibbous)
        assertTrue("gibbous must be darker than full", gibbous < full)
        assertEquals(SkyNightModel.FULL_MOON_ZENITH_SKY, SkyNightModel.moonSkyLuminance(90.0, 100.0), 1e-12)
    }

    @Test
    fun moonlightScatteringFallsOffAwayFromTheMoonAndIsNormalisedAtTheMoon() {
        assertEquals(1.0, SkyNightModel.moonScattering(0.0), 1e-9)
        assertTrue(SkyNightModel.moonScattering(30.0) < SkyNightModel.moonScattering(10.0))
        assertTrue(SkyNightModel.moonScattering(120.0) < SkyNightModel.moonScattering(30.0))
        // Never negative anywhere on the sky.
        var rho = 0.0
        while (rho <= 180.0) {
            assertTrue(SkyNightModel.moonScattering(rho) > 0.0)
            rho += 5.0
        }
    }

    // ---- Twilight -------------------------------------------------------------------------------

    @Test
    fun theNightSkyIsFullyReplacedBeforeTheNightTermsVanish() {
        assertEquals(0f, SkyNightModel.nightSkyWeight(0.0), 0f)
        assertEquals(0f, SkyNightModel.nightSkyWeight(-4.0), 0f)
        assertEquals(1f, SkyNightModel.nightSkyWeight(-10.0), 1e-6f)
        assertEquals(1f, SkyNightModel.nightSkyWeight(-40.0), 0f)
        assertEquals(1f, SkyNightModel.nightSkyWeight(Double.NaN), 0f)
    }

    @Test
    fun twilightFollowsThePublishedLadderFromDaylightToDark() {
        // The classic twilight ladder: ~1.9 mag/arcsec2 per degree of solar depression.
        assertEquals(10.5, SkyRenderModel.luminanceToMagArcsec2(SkyNightModel.twilightZenithLuminance(-12.0)), 0.05)
        assertEquals(21.9, SkyRenderModel.luminanceToMagArcsec2(SkyNightModel.twilightZenithLuminance(-18.0)), 0.6)
        // It rises strictly as the Sun goes down, and is handed back to the daytime palette above -6.
        var previous = 0.0
        var alt = -6.0
        while (alt >= -20.0) {
            val l = SkyNightModel.twilightZenithLuminance(alt)
            assertTrue("twilight must brighten as the Sun sets (at $alt)", l <= previous * 1.0001)
            previous = l
            alt -= 0.25
        }
        assertEquals(0.0, SkyNightModel.twilightZenithLuminance(3.0), 0.0)
        assertTrue(SkyNightModel.twilightZenithLuminance(-9.0) > SkyNightModel.naturalAirglow * 1000.0)
    }

    @Test
    fun theExposureAdaptsToTwilightInsteadOfBurningItOut() {
        val day = SkyNightModel.skyFor(-2.0, -30.0, 0.0, 3)
        val nautical = SkyNightModel.skyFor(-9.0, -30.0, 0.0, 3)
        val night = SkyNightModel.skyFor(-30.0, -30.0, 0.0, 3)
        assertEquals(0f, day.twilightZenith, 0f)
        assertTrue("twilight must be part of the sky", nautical.twilightZenith > 0f)
        // Without the twilight in the key, twilight would be scaled by the night exposure (tens of
        // times) and clip to white.
        assertTrue(
            "twilight exposure ${nautical.exposure} must be far below the night's ${night.exposure}",
            nautical.exposure < night.exposure * 0.05f
        )
        // ...and it must still be exposed so that the twilight sky lands below white.
        val twilightZenith = nautical.twilightZenith * nautical.exposure
        assertTrue("twilight zenith must not clip (got $twilightZenith)", twilightZenith < 1f)
        assertTrue("twilight zenith must be visible (got $twilightZenith)", twilightZenith > 0.05f)
    }

    @Test
    fun theNightSkyWeightNeverSteps() {
        var previous = SkyNightModel.nightSkyWeight(10.0)
        var maxStep = 0f
        var alt = 10.0
        while (alt >= -30.0) {
            val w = SkyNightModel.nightSkyWeight(alt)
            assertTrue("weight must rise as the Sun sets", w >= previous - 1e-7f)
            maxStep = maxOf(maxStep, abs(w - previous))
            previous = w
            alt -= 0.01
        }
        // Smoothstep over 6 degrees: 1.5 / 6 per degree = 0.0025 per 0.01 degree.
        assertTrue("largest step $maxStep", maxStep < 0.004f)
    }

    // ---- The assembled sky ----------------------------------------------------------------------

    @Test
    fun aDarkSiteWithNoMoonIsExposedForTheGalaxy() {
        val sky = SkyNightModel.skyFor(
            sunAltDeg = -35.0,
            moonAltDeg = -30.0,
            moonIlluminationPercent = 5.0,
            bortle = 2,
            cameraAltDeg = 40.0
        )
        assertEquals(0f, sky.moonSky, 0f)
        assertEquals(1f, sky.nightSkyWeight, 0f)
        // The galaxy is far brighter than the sky it is set in: that is what makes it visible.
        assertTrue(
            "galaxy peak ${sky.galaxyPeak} must exceed the airglow ${sky.airglow}",
            sky.galaxyPeak > sky.airglow * 3.0
        )
        // The exposure brightens a dark night and dims a bright one.
        val city = SkyNightModel.skyFor(
            sunAltDeg = -35.0, moonAltDeg = -30.0, moonIlluminationPercent = 5.0, bortle = 8, cameraAltDeg = 40.0
        )
        assertTrue("a city night must be exposed down", city.exposure < sky.exposure * 0.5f)
    }

    @Test
    fun aFullMoonWashesOutTheSkyAndTheGalaxyWithIt() {
        val dark = SkyNightModel.skyFor(-35.0, -30.0, 5.0, 3)
        val moonlit = SkyNightModel.skyFor(-35.0, 55.0, 100.0, 3)
        assertTrue("the Moon must light the sky", moonlit.moonSky > dark.moonSky * 100.0)
        assertTrue("a moonlit night must be exposed down", moonlit.exposure < dark.exposure)
    }

    @Test
    fun aPristineNightIsRodSeenAndACityNightIsNot() {
        val pristine = SkyNightModel.skyFor(-30.0, -30.0, 0.0, 2)
        val city = SkyNightModel.skyFor(-30.0, -30.0, 0.0, 8)
        val fullMoon = SkyNightModel.skyFor(-30.0, 60.0, 100.0, 3)
        // A dark night is seen by the rods (blue-green, desaturated); a city sky is a cone scene, so
        // it keeps the sodium/LED colours it really has instead of going grey.
        assertTrue("pristine sky must be rod-seen, got ${pristine.scotopicWeight}", pristine.scotopicWeight > 0.9f)
        assertTrue("a city night must keep its colour, got ${city.scotopicWeight}", city.scotopicWeight < pristine.scotopicWeight)
        assertTrue("a full Moon must keep its colour", fullMoon.scotopicWeight < pristine.scotopicWeight)
        // Daylight is a cone scene: the rods have nothing to do with it.
        assertEquals(0f, SkyNightModel.skyFor(30.0, -30.0, 0.0, 3).scotopicWeight, 0f)
        // ...and it is the shared SkyRenderModel curve, not a second copy of it: at the adaptation
        // luminance of a pristine sky it is essentially full rod vision.
        assertTrue(SkyRenderModel.scotopicWeight(SkyNightModel.adaptationLuminance(
            SkyNightModel.naturalAirglow, 0.0, 0.0
        )) > 0.95)
    }

    @Test
    fun extinctionReddensWhatIsSeenThroughIt() {
        val clear = SkyNightModel.skyFor(-35.0, -30.0, 5.0, 3, extinctionV = 0.83)
        val hazy = SkyNightModel.skyFor(-35.0, -30.0, 5.0, 3, extinctionV = 0.55)
        assertTrue("blue must be lost faster than red", clear.tauB > clear.tauG && clear.tauG > clear.tauR)
        assertTrue(hazy.tauR > clear.tauR)
        assertEquals(0.83f, clear.zenithTransmittance, 1e-6f)
    }

    @Test
    fun theGalaxyIsKeptInTheVisualSurfaceBrightnessRange() {
        // 18.6–19.2 mag/arcsec2 for the brightest star clouds: bright, but still a dark-sky object.
        val peak = SkyRenderModel.luminanceToMagArcsec2(SkyNightModel.galaxyPeakLuminance)
        assertTrue("galaxy peak $peak", peak in 18.4..19.4)
        assertTrue(SkyNightModel.GALAXY_GAMMA in 0.7..1.0)
    }

    // ---- The shader contract --------------------------------------------------------------------

    @Test
    fun theShaderImplementsEveryTermTheModelSupplies() {
        val fragment = SkyPanoramaShaders.FRAGMENT_SHADER
        for (uniform in listOf(
            "uZenithDir", "uNorthDir", "uEastDir", "uSunAzDeg", "uSunAltDeg", "uMoonAzDeg", "uMoonAltDeg",
            "uTau", "uAirglow", "uLightPollution", "uZenithTz", "uMoonSky", "uGalaxyPeak", "uGalaxyGamma",
            "uZodiacalPeak", "uGegenschein", "uTwilightZenith", "uExposure", "uNightSkyWeight", "uScotopicWeight", "uDitherSeed"
        )) {
            assertTrue("the fragment shader must declare $uniform", fragment.contains(uniform))
        }
        // The plate's flat sky pedestal is removed, and only the galactic band keeps it.
        assertTrue(fragment.contains("0.105"))
        assertTrue(fragment.contains("GAL_POLE"))
        // One exposure and one tone curve for the whole night, not a per-layer fade.
        assertTrue(fragment.contains("acesCurve"))
        assertTrue(fragment.contains("srgbToLinear"))
        assertTrue(fragment.contains("linearToSrgb"))
        // The retired day/night mix uniform is gone; only the night-sky blend remains.
        assertFalse(fragment.contains("uniform float uNightWeight"))
        assertTrue(fragment.contains("uniform float uNightSkyWeight;"))
    }

    @Test
    fun theShaderStillMapsThePlateToTheSameCelestialCoordinates() {
        val fragment = SkyPanoramaShaders.FRAGMENT_SHADER
        assertTrue(fragment.contains("float ra = atan(dir.y, dir.x);"))
        assertTrue(fragment.contains("vec2 uv = vec2(fract(0.5 - ra / (2.0 * PI)), 0.5 - dec / PI);"))
        // The galactic pole is the one CoordinateEngine uses.
        assertTrue(fragment.contains("0.05487556") && fragment.contains("0.87343709"))
    }

    @Test
    fun theRendererSendsEveryNightUniformItLocates() {
        val renderer = source("com/alijafari/red/astronomy/ui/skypanorama/SkyPanoramaRenderer.kt")
        for (name in listOf(
            "uNightSkyWeight", "uZenithDir", "uNorthDir", "uEastDir", "uSunAzDeg", "uSunAltDeg",
            "uMoonAzDeg", "uMoonAltDeg", "uTau", "uAirglow", "uLightPollution", "uZenithTz", "uMoonSky",
            "uGalaxyPeak", "uGalaxyGamma", "uZodiacalPeak", "uGegenschein", "uTwilightZenith", "uDitherSeed"
        )) {
            assertTrue("renderer must locate $name", renderer.contains("glGetUniformLocation(prog, \"$name\")"))
            assertTrue("renderer must set $name", renderer.contains("$name,") || renderer.contains("$name)"))
        }
        assertTrue(renderer.contains("night.exposure * cfg.exposure"))
    }

    // ---- Night palette for the Compose (procedural) sky ----------------------------------------

    @Test
    fun theProceduralNightIsOffInDaylightAndOnAtNight() {
        val tone = androidx.compose.ui.graphics.Color(0xFF050A16)
        val horizonTone = androidx.compose.ui.graphics.Color(0xFF101C33)
        assertEquals(0f, NightSkyPalette.forNight(-2.0, 30.0, 50.0, 3, tone, horizonTone).strength, 0f)
        assertEquals(1f, NightSkyPalette.forNight(-30.0, 30.0, 50.0, 3, tone, horizonTone).strength, 1e-6f)
        assertEquals(0f, NightSkyPalette.forNight(40.0, 30.0, 50.0, 3, tone, horizonTone).zenithAlpha, 0f)
    }

    @Test
    fun thePaletteKeepsEveryAlphaInARangeTheDisplayCanShow() {
        val tone = androidx.compose.ui.graphics.Color(0xFF050A16)
        val horizonTone = androidx.compose.ui.graphics.Color(0xFF101C33)
        for (bortle in 1..9) {
            for (moonAlt in listOf(-40.0, -3.0, 2.0, 20.0, 55.0, 88.0)) {
                for (illum in listOf(0.0, 12.0, 50.0, 100.0)) {
                    val p = NightSkyPalette.forNight(-25.0, moonAlt, illum, bortle, tone, horizonTone)
                    for (a in listOf(p.zenithAlpha, p.horizonAlpha, p.lightDomeAlpha, p.moonGlowAlpha)) {
                        assertTrue("alpha $a out of range", a in 0f..0.34f)
                    }
                    assertTrue(p.galaxyVisibility in 0f..1f)
                    assertTrue("the horizon is never darker than the zenith", p.horizonAlpha >= p.zenithAlpha * 0.5f)
                }
            }
        }
    }

    @Test
    fun aFullMoonLiftsTheWholeDomeRatherThanAddingASmallGlow() {
        val tone = androidx.compose.ui.graphics.Color(0xFF050A16)
        val horizonTone = androidx.compose.ui.graphics.Color(0xFF101C33)
        val dark = NightSkyPalette.forNight(-30.0, -40.0, 0.0, 3, tone, horizonTone)
        val crescent = NightSkyPalette.forNight(-30.0, 40.0, 10.0, 3, tone, horizonTone)
        val full = NightSkyPalette.forNight(-30.0, 40.0, 100.0, 3, tone, horizonTone)
        // A full Moon raises a dark sky by roughly 4 magnitudes: the zenith wash must be large.
        assertTrue("full-moon zenith alpha ${full.zenithAlpha}", full.zenithAlpha > dark.zenithAlpha * 3f)
        assertTrue(crescent.zenithAlpha < full.zenithAlpha)
        // ...and it is blue-grey, so the zenith is lifted towards the Moon, not towards the airglow.
        assertTrue(full.zenith.blue > full.zenith.red)
        // The galaxy is all but gone under it, and merely dimmed under a crescent.
        assertTrue(full.galaxyVisibility < crescent.galaxyVisibility)
        assertTrue(full.galaxyVisibility < 0.25f)
    }

    @Test
    fun aCityNightHidesTheGalaxyAndAMoonFreeDarkNightDoesNot() {
        val tone = androidx.compose.ui.graphics.Color(0xFF050A16)
        val horizonTone = androidx.compose.ui.graphics.Color(0xFF101C33)
        val rural = NightSkyPalette.forNight(-30.0, -40.0, 0.0, 2, tone, horizonTone)
        val city = NightSkyPalette.forNight(-30.0, -40.0, 0.0, 8, tone, horizonTone)
        assertTrue("Bortle 2 must show the galaxy", rural.galaxyVisibility > 0.9f)
        assertTrue("Bortle 8 must hide it", city.galaxyVisibility < 0.35f)
        assertTrue("a city sky has a light dome", city.lightDomeAlpha > rural.lightDomeAlpha)
    }

    @Test
    fun moonlightAddsAWashAndDimsTheGalaxy() {
        val tone = androidx.compose.ui.graphics.Color(0xFF050A16)
        val horizonTone = androidx.compose.ui.graphics.Color(0xFF101C33)
        val dark = NightSkyPalette.forNight(-30.0, -40.0, 0.0, 3, tone, horizonTone)
        val moon = NightSkyPalette.forNight(-30.0, 55.0, 100.0, 3, tone, horizonTone)
        assertTrue("moonlight must add a wash", moon.moonGlowAlpha > dark.moonGlowAlpha)
        assertTrue("moonlight must dim the galaxy", moon.galaxyVisibility < dark.galaxyVisibility)
    }

    @Test
    fun theProceduralNightKeepsTheThemeNightRatherThanOneHardCodedBlue() {
        val dark = androidx.compose.ui.graphics.Color(0xFF050505)
        val warm = androidx.compose.ui.graphics.Color(0xFF2A0505)
        val fromDark = NightSkyPalette.forNight(-30.0, -40.0, 0.0, 3, dark, dark)
        val fromWarm = NightSkyPalette.forNight(-30.0, -40.0, 0.0, 3, warm, warm)
        assertNotEquals(fromDark.zenith, fromWarm.zenith)
    }

    // ---- End to end through a real frame -------------------------------------------------------

    @Test
    fun aComputedFrameCarriesTheNightThePanoramaNeeds() {
        val frame = SkySceneModel.compute(
            TimeEngine.getJulianDate(1_700_000_000_000L), 35.6892, 51.389, 1100.0, bortle = 4
        )
        assertEquals(4, frame.bortle)
        val state = SkyPanoramaState.fromSceneFrame(frame)
        assertEquals(frame.moonData.altitudeDeg, state.moonAltDeg, 0.13)
        assertEquals((frame.moonData.illuminationPercent / 100.0).toFloat(), state.moonIllum, 0.006f)
        assertEquals(state.night.airglow, SkyNightModel.naturalAirglow.toFloat(), 1e-6f)
        assertTrue(state.night.exposure > 0f)
        // The night knows which way is up: the zenith, north and east are orthonormal.
        assertEquals(1.0, dot(state.zenithDir, state.zenithDir), 1e-12)
        assertEquals(0.0, dot(state.zenithDir, state.northDir), 1e-12)
        assertEquals(1.0, dot(state.northDir, state.northDir), 1e-12)
        assertEquals(0.0, dot(state.northDir, state.eastDir), 1e-12)
        // The zenith in that frame is RA = LST, Dec = latitude.
        assertEquals(Math.toRadians(frame.latitudeDeg), Math.asin(state.zenithDir[2]), 1e-9)
    }

    @Test
    fun theNightStateChangesOnlyWhenItsInputsDo() {
        val a = SkyPanoramaState.fromSkyState(100.0, 45.0, -20.0, 0f, bortle = 3, moonAltDeg = 40.0, moonIlluminationPercent = 50.0)
        val b = SkyPanoramaState.fromSkyState(100.0, 45.0, -20.0, 0f, bortle = 3, moonAltDeg = 40.1, moonIlluminationPercent = 49.9)
        assertEquals("sub-quantum changes must not redraw", a, b)
        assertNotEquals(a, a.copy(bortle = 6))
        assertNotEquals(a, a.copy(moonAltDeg = 60.0))
    }

    @Test
    fun theBortleClassReachesTheSkyThroughTheFrameAndTheState() {
        val jd = TimeEngine.getJulianDate(1_700_000_000_000L)
        val rural = SkyPanoramaState.fromSceneFrame(SkySceneModel.compute(jd, 35.0, 51.0, 1200.0, bortle = 2))
        val urban = SkyPanoramaState.fromSceneFrame(SkySceneModel.compute(jd, 35.0, 51.0, 1200.0, bortle = 7))
        assertTrue(urban.night.lightPollution > rural.night.lightPollution * 10.0)
        assertTrue(urban.night.exposure < rural.night.exposure)
    }

    private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    private fun source(relative: String): String {
        val candidates = listOf(File("src/main/java/$relative"), File("app/src/main/java/$relative"))
        return candidates.firstOrNull { it.exists() }?.readText() ?: error("Source not found: $relative")
    }
}
