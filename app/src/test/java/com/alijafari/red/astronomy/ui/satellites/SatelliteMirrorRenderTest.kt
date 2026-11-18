package com.alijafari.red.astronomy.ui.satellites

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.theme.REDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "en-rUS-ldltr-w360dp-h640dp-mdpi")
class SatelliteMirrorRenderTest {
    @get:Rule val compose = createComposeRule()

    private data class Mode(val rtl: Boolean, val polar: Boolean, val follow: Boolean = true)
    private val pass = ISSEngine.ISSPass(
        startTimeMs = 0L, maxTimeMs = 210_000L, endTimeMs = 600_000L,
        maxElevationDeg = 71.0, maxAltitudeKm = 420.0, maxAzimuthDeg = 15.0,
        startAzimuthDeg = 260.0, endAzimuthDeg = 95.0, estimatedMagnitude = -1.5,
        passDurationSec = 600L, sunAltitudeDegAtMax = -12.0, isObserverInDarkness = true,
        isIssSunlitAtMax = true, classification = ISSEngine.PassClassification.GOOD,
        visibilityScore = 65, summaryReasonEn = "", summaryReasonFa = "",
        detailedReasonsEn = emptyList(), detailedReasonsFa = emptyList(), isVisible = true,
    )

    /** Compose's PixelCopy capture waits for a real window frame and times out on Robolectric.
     * Draw the Compose root into a software bitmap on the UI thread, then crop to the chart. */
    @SuppressLint("VisibleForTests")
    private fun pixels(tag: String): PixelMap {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val view = (node.root as ViewRootForTest).view
        val bounds = node.boundsInRoot
        lateinit var image: Bitmap
        compose.runOnIdle {
            val root = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(root))
            image = Bitmap.createBitmap(root, bounds.left.toInt(), bounds.top.toInt(),
                bounds.width.toInt(), bounds.height.toInt())
        }
        return image.asImageBitmap().toPixelMap()
    }

    private fun bright(p: Color) = p.red > .84f && p.green > .84f && p.blue > .84f

    /** Compare the light plotted path, independently of localized label shaping and antialiasing. */
    private fun pathSimilarity(a: PixelMap, b: PixelMap): Pair<Int, Float> {
        assertEquals(a.width, b.width)
        assertEquals(a.height, b.height)
        var total = 0
        var matching = 0
        for (y in 1 until a.height - 1) for (x in 1 until a.width - 1) {
            if (bright(a[x, y])) {
                total++
                if ((-1..1).any { dx -> (-1..1).any { dy -> bright(b[x + dx, y + dy]) } }) matching++
            }
        }
        return total to if (total == 0) 0f else matching.toFloat() / total
    }

    @Test
    fun polarIsPhysicalWhileTimeAxisMirrorsAndTheFlagCanDisableIt() {
        val mode = mutableStateOf(Mode(rtl = false, polar = true))
        compose.setContent {
            val state = mode.value
            REDTheme(themeMode = ThemeMode.DARK, isPersian = false) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (state.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    LocalTimeAxisFollowsLayout provides state.follow,
                ) {
                    Box(Modifier.background(Color.Black)) {
                        PassChart(pass, zigSatTokens(SatSkin.DEFAULT, ThemeMode.DARK), false, state.polar,
                            Modifier.size(240.dp, 160.dp))
                    }
                }
            }
        }
        val polarLtr = pixels("polar_chart")
        assertEquals(LayoutRole.Physical, compose.onNodeWithTag("polar_chart", useUnmergedTree = true)
            .fetchSemanticsNode().config[LayoutRoleKey])
        compose.runOnIdle { mode.value = Mode(rtl = true, polar = true) }
        val polarRtl = pixels("polar_chart")
        val polarMatch = pathSimilarity(polarLtr, polarRtl)
        assertTrue("physical chart needs a visible path: $polarMatch", polarMatch.first > 30)
        assertTrue("physical polar path must not flip: $polarMatch", polarMatch.second >= .9f)

        compose.runOnIdle { mode.value = Mode(rtl = false, polar = false) }
        val timeLtr = pixels("pass_curve")
        assertEquals(LayoutRole.TimeAxis, compose.onNodeWithTag("pass_curve", useUnmergedTree = true)
            .fetchSemanticsNode().config[LayoutRoleKey])
        compose.runOnIdle { mode.value = Mode(rtl = true, polar = false) }
        val timeRtl = pixels("pass_curve")
        var bright = 0
        var mirrored = 0
        for (y in 1 until timeLtr.height - 1) for (x in 1 until timeLtr.width - 1) {
            val p = timeLtr[x, y]
            if (p.red > .7f && p.green > .7f && p.blue > .7f) {
                bright++
                val mx = timeLtr.width - 1 - x
                if ((-1..1).any { dx -> (-1..1).any { dy ->
                    val q = timeRtl[(mx + dx).coerceIn(0, timeRtl.width - 1), y + dy]
                    q.red > .7f && q.green > .7f && q.blue > .7f
                } }) mirrored++
            }
        }
        assertTrue("fixture must actually draw an arc", bright > 30)
        assertTrue("the time curve should mirror in RTL", mirrored >= bright * .8f)
        compose.runOnIdle { mode.value = Mode(rtl = true, polar = false, follow = false) }
        val forcedLtr = pathSimilarity(timeLtr, pixels("pass_curve"))
        assertTrue("axis flag forces the LTR path in either language: $forcedLtr", forcedLtr.first > 30 && forcedLtr.second >= .9f)
    }
}
