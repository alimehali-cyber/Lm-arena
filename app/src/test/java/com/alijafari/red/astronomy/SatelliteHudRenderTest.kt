package com.alijafari.red.astronomy

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.satellites.SatPreview
import com.alijafari.red.astronomy.ui.satellites.SatSkin
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SatelliteHudRenderTest {
    @get:Rule val compose = createComposeRule()

    @Test
    @Config(qualifiers = "en-rGB-ldltr-w390dp-h844dp-mdpi")
    fun restTimeAndHalfPassesUseTheSameUnscrolledGlobe() {
        compose.setContent { SatPreview(ThemeMode.DARK) }
        val focus = compose.onNodeWithTag("sat_focus_card", useUnmergedTree = true)
        focus.assertIsDisplayed()
        assertTrue("card clears floating nav", focus.fetchSemanticsNode().boundsInRoot.bottom <
            compose.onNodeWithTag("main_bottom_navigation_surface").fetchSemanticsNode().boundsInRoot.top)
        compose.onNodeWithTag("sat_time").performClick()
        focus.assertDoesNotExist()
        val slider = compose.onNodeWithTag("time_scrubber_slider")
        slider.assertIsDisplayed()
        assertEquals(0f, slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current)
        compose.onNodeWithTag("sat_time").performClick()
        compose.onNodeWithTag("sat_focus_passes").performClick()
        compose.onNodeWithTag("passes_sheet").assertIsDisplayed()
        compose.onNodeWithTag("main_bottom_navigation_surface").assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/outputs/satellite-previews/dark-en-passes.png")
    }

    @Test
    @Config(qualifiers = "fa-rIR-ldrtl-w340dp-h730dp-mdpi")
    fun persianAt130PercentMirrorsHudAndSpeaksLocalizedSimTime() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.3f)) { SatPreview(ThemeMode.WINE, skin = SatSkin.SPATIAL) }
        }
        val status = compose.onNodeWithTag("satellite_status_chip", useUnmergedTree = true).assertIsDisplayed()
        val time = compose.onNodeWithTag("sat_time", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(status.fetchSemanticsNode().boundsInRoot.left > time.fetchSemanticsNode().boundsInRoot.right)
        time.performClick()
        val scrubber = compose.onNodeWithTag("time_scrubber_slider").assertIsDisplayed()
        scrubber.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { set -> set(-70f) }
        val speech = scrubber.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertTrue(speech.contains("منفی"))
        assertTrue(speech.contains("۱۰"))
        assertFalse(speech.any { it in '0'..'9' })
        compose.onRoot().captureRoboImage("build/outputs/satellite-previews/wine-fa-time-130.png")
    }
}
