package com.alijafari.red.astronomy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alijafari.red.astronomy.astro_engine.MoonEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.aether.AetherTelemetryDashboard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AetherDashboardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dashboardUsesAstronomyDataAndControls() {
        var delta = 0f
        var settings = false
        compose.setContent {
            AetherTelemetryDashboard(
                uiState = MainUiState(language = AppLanguage.ENGLISH),
                moonData = MoonEngine.MoonData(0.0, 0.0, "Waxing", "هلال", 37.0, 4.5, 0.0, 380000.0),
                sunAltitude = -12.0,
                onSettings = { settings = true }, onLocation = {}, onMoon = {},
                onNudgeMoon = { delta = it }, onSky = {}
            )
        }
        compose.onNodeWithTag("aether_dashboard").assertIsDisplayed()
        compose.onNodeWithText("Waxing").assertIsDisplayed()
        compose.onNodeWithText("4.5 days").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next day").performClick()
        compose.runOnIdle { assertEquals(1f, delta, 0f) }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.runOnIdle { assertEquals(true, settings) }
    }
}
