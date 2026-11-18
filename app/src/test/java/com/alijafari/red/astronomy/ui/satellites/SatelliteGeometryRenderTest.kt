package com.alijafari.red.astronomy.ui.satellites

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.domain.ThemeMode
import java.util.Locale
import kotlin.math.hypot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

internal data class ReviewScenario(
    val width: Int = 360,
    val height: Int = 800,
    val scale: Float = 1f,
    val language: String = "en",
    val skin: SatSkin = SatSkin.DEFAULT,
    val theme: ThemeMode = ThemeMode.DARK,
    val page: ReviewPage = ReviewPage.Rest,
    val offCenter: Boolean = false,
)

/** Applies both resource locale and Compose font scale; the fixture owns no live clock or SGP4. */
@Composable
internal fun ScenarioContent(s: ReviewScenario) {
    val context = LocalContext.current
    val base = LocalConfiguration.current
    val density = LocalDensity.current.density
    val config = remember(base, s.language, s.width, s.height, s.scale) {
        Configuration(base).apply {
            val locale = Locale.forLanguageTag(s.language)
            setLocale(locale)
            setLayoutDirection(locale)
            screenWidthDp = s.width
            screenHeightDp = s.height
            fontScale = s.scale
        }
    }
    val localized = remember(context, config) { context.createConfigurationContext(config) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides config,
        LocalDensity provides Density(density, s.scale),
    ) {
        Box(Modifier.size(s.width.dp, s.height.dp).testTag("review_case")) {
            key(s) { SatPreview(s.theme, skin = s.skin, page = s.page, initiallyCentered = !s.offCenter) }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "en-rUS-ldltr-w1024dp-h1600dp-mdpi")
class SatelliteGeometryRenderTest {
    @get:Rule val compose = createComposeRule()

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    private fun overlap(a: Rect, b: Rect): Boolean =
        a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom

    private fun distanceFromRect(x: Float, y: Float, rect: Rect): Float =
        hypot(x - x.coerceIn(rect.left, rect.right), y - y.coerceIn(rect.top, rect.bottom))

    @Test
    fun restAndRecenterClearTheDiscAcrossSizesScalesLocalesAndSkins() {
        val sizes = listOf(360 to 640, 360 to 800, 412 to 915, 768 to 1024)
        val initial = ReviewScenario()
        val current = mutableStateOf(initial)
        compose.setContent { ScenarioContent(current.value) }
        for ((w, h) in sizes) for (scale in listOf(1f, 1.15f, 1.3f))
            for (language in listOf("en", "fa")) for (skin in SatSkin.entries) for (off in listOf(false, true)) {
                val scenario = ReviewScenario(w, h, scale, language, skin, offCenter = off)
                compose.runOnIdle { current.value = scenario }
                compose.waitForIdle()
                val disc = bounds("globe_disc")
                val card = bounds("sat_focus_card")
                val status = bounds("satellite_status_chip")
                val buttons = listOf("sat_fleet", "sat_time", "sat_layers").map(::bounds)
                val nav = bounds(if (skin == SatSkin.AETHER) "floating_bottom_bar" else "main_bottom_navigation")
                val targets = listOf(status) + buttons + card + nav + if (off) listOf(bounds("map_reset_view")) else emptyList()
                val cx = (disc.left + disc.right) / 2
                val cy = (disc.top + disc.bottom) / 2
                val radius = disc.width / 2
                for ((index, target) in targets.withIndex()) {
                    assertTrue("$scenario disc intersects target $index", distanceFromRect(cx, cy, target) + 1f >= radius)
                }
                assertTrue("$scenario focus card collides with nav", card.bottom <= nav.top + 1f)
                for ((index, target) in (listOf(status) + buttons).withIndex()) {
                    assertTrue("$scenario target $index shorter than 48dp", target.width >= 47f && target.height >= 47f)
                }
                for (i in buttons.indices) for (j in i + 1 until buttons.size)
                    assertTrue("$scenario toolbar targets overlap", !overlap(buttons[i], buttons[j]))
                assertTrue("$scenario status/toolbar overlap", buttons.none { overlap(status, it) })
                if (language == "fa") assertTrue("$scenario RTL status placement", status.left >= buttons.maxOf { it.right } - 1f)
                else assertTrue("$scenario LTR status placement", status.right <= buttons.minOf { it.left } + 1f)
            }
    }
}
