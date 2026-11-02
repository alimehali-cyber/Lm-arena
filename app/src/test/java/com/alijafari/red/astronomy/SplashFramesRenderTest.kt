package com.alijafari.red.astronomy

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.alijafari.red.astronomy.ui.components.PremiumSplashState
import com.alijafari.red.astronomy.ui.components.SplashRevealRole
import com.alijafari.red.astronomy.ui.components.SplashScene
import com.alijafari.red.astronomy.ui.components.splashReveal
import com.alijafari.red.astronomy.ui.splash.SplashArtLoader
import com.alijafari.red.astronomy.ui.splash.SplashTimeline
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real splash at exact moments of its timeline - with a stand-in app underneath so
 * the curtain lift and slide-in are visible - on a phone, a landscape phone and a tablet.
 *
 * In the normal unit-test run this is a smoke test (the art decodes, every layer composes and
 * draws at every moment). With Roborazzi recording on, frames are written to
 * app/build/outputs/splash-frames/ (see .github/workflows/splash-frames.yml):
 *
 *   ./gradlew recordRoborazziDebug --tests '*SplashFramesRenderTest*' -Pgravity.ci.tests=false
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SplashFramesRenderTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val moments = listOf(
        0f, 150f, 350f, 600f, 900f, 1250f, 1500f, 1700f, 2000f, 2450f, 2600f,
        2750f, 2900f, 3050f, 3200f, 3400f, 3600f, 3850f,
    )

    private fun render(prefix: String, times: List<Float>, scale: Double = 1.0) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val art = SplashArtLoader.loadBlocking(context)
        val state = PremiumSplashState(reducedMotion = false)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF0B0D16))) {
                StandInApp(Modifier.fillMaxSize().splashReveal(state))
                StandInNavBar(Modifier.align(Alignment.BottomCenter).splashReveal(state, SplashRevealRole.NavigationBar))
                SplashScene(state = state, art = art)
            }
        }
        composeTestRule.onNodeWithTag("premium_splash_screen").assertExists()
        for (t in times) {
            state.seekTo(t)
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.waitForIdle()
            composeTestRule.onRoot().captureRoboImage(
                filePath = "build/outputs/splash-frames/${prefix}_${t.toInt().toString().padStart(4, '0')}ms.png",
                roborazziOptions = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = scale)),
            )
        }
        val end = SplashTimeline.INTRO_MS + SplashTimeline.EXIT_TOTAL_MS
        state.seekTo(end)
        assertTrue("splash reports finished at the end of its exit", state.isFinished)
    }

    @Test
    fun portraitPhone() = render("phone", moments)

    @Test
    @Config(qualifiers = "+land")
    fun landscapePhone() = render("phone-land", listOf(0f, 900f, 1700f, 2900f, 3200f))

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tablet() = render("tablet", listOf(0f, 900f, 1700f, 2900f, 3200f))

    /** 30 fps sequence for the MP4 preview; only when SPLASH_FRAMES_DENSE=true (CI preview job). */
    @Test
    fun denseSequence() {
        assumeTrue(System.getenv("SPLASH_FRAMES_DENSE") == "true")
        val end = SplashTimeline.INTRO_MS + SplashTimeline.EXIT_TOTAL_MS + 200f
        val times = generateSequence(0f) { it + 1000f / 30f }.takeWhile { it <= end }.toList()
        render("dense", times, scale = 0.5)
    }

    @Test
    fun mainActivityWiresTheHandOffAtBothEnds() {
        val activity = listOf("src/main/java", "app/src/main/java")
            .map { File(it, "com/alijafari/red/astronomy/MainActivity.kt") }
            .first { it.exists() }
            .readText()
        assertTrue("art decode starts in onCreate", activity.contains("SplashArtLoader.preload(applicationContext)"))
        assertTrue("first draw waits for the art", activity.contains("addOnPreDrawListener"))
        assertTrue("system splash leaves instantly on Android 12+", activity.contains("splashScreen.setOnExitAnimationListener { it.remove() }"))
        assertTrue("the content slides in", activity.contains(".splashReveal(splashState)"))
        assertTrue("the nav bar slides in", activity.contains(".splashReveal(splashState, SplashRevealRole.NavigationBar)"))
    }

    // A stand-in for the app: header, a hero card and a row of tiles, roughly like Home.
    @Composable
    private fun StandInApp(modifier: Modifier) {
        Column(modifier.padding(horizontal = 20.dp, vertical = 56.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Tonight", color = Color.White, fontSize = 30.sp)
            Text("Vienna  ·  clear skies", color = Color(0xFF9AA3B8), fontSize = 14.sp)
            Box(
                Modifier.fillMaxWidth().height(220.dp)
                    .background(Color(0xFF1B2133), RoundedCornerShape(28.dp))
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(2) {
                    Box(Modifier.weight(1f).height(140.dp).background(Color(0xFF161B2A), RoundedCornerShape(24.dp)))
                }
            }
            Box(Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF161B2A), RoundedCornerShape(24.dp)))
        }
    }

    @Composable
    private fun StandInNavBar(modifier: Modifier) {
        Box(
            modifier.padding(bottom = 24.dp).size(width = 280.dp, height = 64.dp)
                .background(Color(0xFF232A3D), RoundedCornerShape(32.dp))
        )
    }
}
