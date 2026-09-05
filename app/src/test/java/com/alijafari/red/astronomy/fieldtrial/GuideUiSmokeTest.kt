package com.alijafari.red.astronomy.fieldtrial

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.alijafari.red.astronomy.astro_engine.OrientationProvider
import com.alijafari.red.astronomy.startracker.debug.FieldTrialHost
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * G-4.2: Robolectric + Compose smoke of the guide card itself — it composes at level 0,
 * shows the plain-English title/instruction, collapses to the one-line pill and back,
 * opens/closes the "?" help sheet returning to the same level state, and stays
 * bottom-anchored (the card content never rises above the bottom ~third of the
 * screen, enforcing the "centre 60% never covered" rule with margin).
 * UNEXECUTED on a real device here — this is the JVM-rendered equivalent.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GuideUiSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun seed(): Pair<FieldTrialController, FieldTrialHost.Access> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = FieldTrialController(context)
        controller.newDocument()
        val provider = OrientationProvider(context)
        FieldTrialHost.open(context, provider)
        return controller to FieldTrialHost.Access(context, provider)
    }

    @Test
    fun `level 0 card renders, collapses to a pill, and the help sheet returns to the same state`() {
        val (controller, access) = seed()
        composeTestRule.setContent { GuideUi.Guide(controller, access) }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Level 0 - Get ready").assertExists()
        composeTestRule.onNodeWithText(
            "Allow camera and location. Then wave the phone in a figure-8 for 5 seconds."
        ).assertExists()

        // collapse to the one-line pill and back
        composeTestRule.onNodeWithText("Hide").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Level 0 - Get ready").assertExists() // pill keeps the level line
        composeTestRule.onNodeWithText("Open").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Allow camera and location. Then wave the phone in a figure-8 for 5 seconds.").assertExists()

        // "?" help sheet: same-state return
        composeTestRule.onNodeWithText("?").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Level 0 - Get ready").assertExists()
    }

    @Test
    fun `card stays bottom-anchored - centre of the screen is never covered`() {
        val (controller, access) = seed()
        composeTestRule.setContent { GuideUi.Guide(controller, access) }
        composeTestRule.waitForIdle()

        val rootHeight = composeTestRule.onRoot().fetchSemanticsNode().size.height.toFloat()
        val cardTop = composeTestRule.onNodeWithText("Level 0 - Get ready")
            .fetchSemanticsNode().positionInRoot.y
        assertTrue("card top $cardTop not bottom-anchored (root $rootHeight)", cardTop > rootHeight * 0.6f)

        // collapsed pill must sit even lower (one line at the very bottom)
        composeTestRule.onNodeWithText("Hide").performClick()
        composeTestRule.waitForIdle()
        val pillTop = composeTestRule.onNodeWithText("Level 0 - Get ready")
            .fetchSemanticsNode().positionInRoot.y
        assertTrue("pill top $pillTop too high (root $rootHeight)", pillTop > rootHeight * 0.75f)
    }
}
