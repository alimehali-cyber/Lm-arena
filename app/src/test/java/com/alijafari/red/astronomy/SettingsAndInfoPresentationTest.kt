package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAndInfoPresentationTest {
    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private fun resource(relative: String): String {
        val candidates = listOf("src/main/res/$relative", "app/src/main/res/$relative").map(::File)
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Resource not found: $relative")
    }

    @Test
    fun settingsIsGroupedAndAboutIsBilingualAndGroundedInBuildMetadata() {
        val settings = source("com/alijafari/red/astronomy/ui/components/SettingsDialog.kt")
        val chrome = source("com/alijafari/red/astronomy/ui/components/SettingsChrome.kt")
        val english = resource("values/strings.xml")
        val persian = resource("values-fa/strings.xml")

        listOf("GENERAL", "APPEARANCE", "SKY", "ABOUT").forEach {
            assertTrue("settings category $it is present", chrome.contains("$it,"))
        }
        assertTrue(settings.contains("AnimatedContent("))
        assertTrue(settings.contains("settings_section_about"))
        assertTrue(settings.contains("BuildConfig.VERSION_NAME"))
        assertTrue(settings.contains("Ali Jafari"))
        assertTrue(settings.contains("settings_about_description"))
        assertTrue(settings.contains("developer_credit_card"))
        for (resourceName in listOf(
            "settings_group_general",
            "settings_group_appearance",
            "settings_group_sky",
            "settings_group_about",
            "settings_about_description",
            "settings_about_creator",
            "settings_about_version",
        )) {
            assertTrue("English resource $resourceName exists", english.contains("name=\"$resourceName\""))
            assertTrue("Persian resource $resourceName exists", persian.contains("name=\"$resourceName\""))
        }
    }

    @Test
    fun settingsOpensAtItsButtonAndWaitsForTheReverseSpringToDismiss() {
        val motion = source("com/alijafari/red/astronomy/ui/components/SettingsDialogMotion.kt")
        val activity = source("com/alijafari/red/astronomy/MainActivity.kt")

        assertTrue(motion.contains("sourceBounds?.let { it.center - windowCenter }"))
        assertTrue(motion.contains("boundsInWindow()"))
        assertTrue(motion.contains("translationX = translationX"))
        assertTrue(motion.contains("translationY = translationY"))
        assertTrue(motion.contains("spring(dampingRatio = 0.82f, stiffness = 360f)"))
        assertTrue(motion.contains("if (closeRequested && !transition.isRunning) latestOnDismiss()"))
        assertTrue(motion.contains("settings_dialog_scrim"))
        assertTrue(activity.contains("modifier = settingsButtonModifier"))
        assertTrue(activity.contains("sourceBounds = settingsSourceBounds"))
    }

    @Test
    fun objectAndSatelliteDetailsKeepTheirFeaturesWithoutRepeatedOrNestedCards() {
        val objectDetails = source("com/alijafari/red/astronomy/ui/components/ObjectDetailModal.kt")
        val satellites = source("com/alijafari/red/astronomy/ui/screens/SatelliteDetailScreen.kt")

        assertEquals(1, objectDetails.countOccurrences("Derived Physical Properties"))
        assertEquals(1, objectDetails.countOccurrences("Deep-Sky Catalog Data"))
        assertEquals(1, objectDetails.countOccurrences("Precise Schedule Today"))
        assertEquals(1, objectDetails.countOccurrences("coolFacts.forEachIndexed"))
        assertEquals(1, objectDetails.countOccurrences("Astronomical Coordinates"))
        assertEquals(1, objectDetails.countOccurrences("RedSurface(")) // highlighted live observability only
        assertTrue(objectDetails.contains("locateObjectInAR(celestialObj)"))
        assertTrue(objectDetails.contains("addObservationLog(notesInput, ratingInput)"))
        assertTrue(objectDetails.contains("RedTheme.colors.textPrimary"))
        assertFalse(objectDetails.contains("MaterialTheme.colorScheme"))

        assertFalse("satellite detail telemetry/visibility/dossier no longer nest card chrome", satellites.contains("ZigSatCard("))
        assertTrue(satellites.contains("detail_telemetry_panel"))
        assertTrue(satellites.contains("detail_visibility_panel"))
        assertTrue(satellites.contains("detail_mission_dossier"))
        assertTrue(satellites.contains("detail_spot_in_ar"))
        assertTrue(satellites.contains("detail_show_on_globe"))
        assertTrue(satellites.contains("detail_remind_next"))

        val eclipses = source("com/alijafari/red/astronomy/ui/components/EclipseDetailModal.kt")
        assertEquals(3, eclipses.countOccurrences("RedSurface(")) // detail window, local-visibility banner, safety guidance
        assertEquals(1, eclipses.countOccurrences("Visible from your Location"))
        assertEquals(1, eclipses.countOccurrences("Eclipse Start:"))
        assertEquals(1, eclipses.countOccurrences("detailedInfo.safetyGuideEn"))
        assertFalse(eclipses.contains("MaterialTheme.colorScheme"))
        assertTrue(eclipses.contains("eclipse_detail_visibility_grade"))
    }

    @Test
    fun moonDetailsRemainSpaciousAndUseOnlyTheirSingleWindowSurface() {
        val moon = source("com/alijafari/red/astronomy/ui/components/MoonDetailWindow.kt")
        assertEquals(1, moon.countOccurrences("RedSurface("))
        assertTrue(moon.contains("moon_window_render"))
        assertTrue(moon.contains("moon_window_ephemeris"))
        assertTrue(moon.contains("moon_window_upcoming"))
        assertTrue(moon.contains("moonDayOffset"))
    }

    private fun String.countOccurrences(token: String): Int =
        windowed(token.length).count { it == token }
}
