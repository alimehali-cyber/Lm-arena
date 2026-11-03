package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.ui.components.PremiumSplashState
import com.alijafari.red.astronomy.ui.splash.SplashArtGeometry
import com.alijafari.red.astronomy.ui.splash.SplashLayout
import com.alijafari.red.astronomy.ui.splash.SplashPalette
import com.alijafari.red.astronomy.ui.splash.SplashTimeline as T
import androidx.compose.ui.graphics.toArgb
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The splash choreography, the hand-offs at both ends, and the screen-fit rules. */
class SplashTimelineTest {

    // ---- holding for the app ------------------------------------------------------------------

    @Test
    fun withoutWaitingTheTimelineIsUnchanged() {
        val s = PremiumSplashState(reducedMotion = false)
        assertTrue(s.isContentReady)
        for (t in listOf(0f, 1000f, 2599f, 2600f, 3000f)) {
            s.seekTo(t)
            assertEquals(T.INTRO_MS, s.exitStartMs, 0f)
        }
    }

    @Test
    fun appReadyInTimeLeavesTheTimelineExactlyAsPlanned() {
        val s = PremiumSplashState(reducedMotion = false, waitForContent = true)
        s.seekTo(1800f)
        s.markContentReady()
        for (t in listOf(1800f, 2600f, 3500f)) {
            s.seekTo(t)
            assertEquals(T.INTRO_MS, s.exitStartMs, 0f)
        }
        s.seekTo(T.INTRO_MS + T.EXIT_TOTAL_MS)
        assertTrue(s.isFinished)
    }

    @Test
    fun theSplashHoldsUntilTheAppIsReadyAndThenLeavesSmoothly() {
        val s = PremiumSplashState(reducedMotion = false, waitForContent = true)
        var lastExit = 0f
        var t = 0f
        while (t <= 6000f) {
            s.seekTo(t)
            val e = s.exitStartMs
            assertTrue("exit never moves earlier", e >= lastExit)
            assertTrue("exit stays ahead while waiting (t=$t)", e > t)
            assertTrue("hairline never completes while waiting (t=$t)", T.progress(t, e) < 1f)
            assertTrue("not finished while waiting", !s.isFinished)
            lastExit = e
            t += 16f
        }
        // Continuous where the hold takes over from the planned exit.
        s.seekTo(T.INTRO_MS - T.CONTENT_LEAD_MS)
        assertEquals(T.INTRO_MS, s.exitStartMs, 0.001f)

        s.seekTo(5000f)
        s.markContentReady()
        s.markContentReady() // idempotent
        s.seekTo(5200f)
        assertEquals("exit follows the lead after ready", 5000f + T.CONTENT_LEAD_MS, s.exitStartMs, 0.001f)
        s.seekTo(5000f + T.CONTENT_LEAD_MS + T.EXIT_TOTAL_MS - 1f)
        assertTrue(!s.isFinished)
        s.seekTo(5000f + T.CONTENT_LEAD_MS + T.EXIT_TOTAL_MS)
        assertTrue(s.isFinished)
    }

    @Test
    fun aSkippedSweepNeverReplaysDuringAHold() {
        val s = PremiumSplashState(reducedMotion = false, waitForContent = true)
        s.seekTo(1000f)
        s.skip()
        assertEquals(1000f, s.plannedExitMs, 0f)
        s.seekTo(2400f) // inside the second sweep's window, still waiting for the app
        assertTrue(s.exitStartMs > 2400f)
        assertNull("the second sweep was skipped", T.sweep(2400f, s.plannedExitMs))
    }

    @Test
    fun theAppIsComposedOnlyAfterTheZHasLanded() {
        val s = PremiumSplashState(reducedMotion = false, waitForContent = true)
        s.seekTo(T.APP_COMPOSE_AFTER_MS - 1f)
        assertTrue(!s.allowsAppComposition)
        s.seekTo(T.APP_COMPOSE_AFTER_MS)
        assertTrue(s.allowsAppComposition)
        assertTrue("the Z has landed by then", T.APP_COMPOSE_AFTER_MS >= 180f + 1300f)
        val reduced = PremiumSplashState(reducedMotion = true, waitForContent = true)
        assertTrue(!reduced.allowsAppComposition)
        reduced.seekTo(16f)
        assertTrue(reduced.allowsAppComposition)
    }

    private val exit = T.INTRO_MS
    private val end = exit + T.EXIT_TOTAL_MS

    // ---- hand-off from the OS -----------------------------------------------------------------

    @Test
    fun firstFrameIsExactlyTheWindowColour() {
        assertEquals("velvet starts fully dark", 0f, T.exposure(0f), 0f)
        assertEquals("the Z is not visible yet", 0f, T.zAlpha(0f), 0f)
        assertEquals("no glow yet", 0f, T.glow(0f), 0f)
        assertEquals("no light yet", 0f, T.lightStrength(0f), 0f)
        assertNull("no sweep yet", T.sweep(0f, exit))
    }

    @Test
    fun windowBackgroundSystemSplashAndFirstFrameShareOneColour() {
        val colors = resFile("values/colors.xml").readText()
        val hex = Regex("""name="zig_splash_velvet_dark">#([0-9A-Fa-f]{8})<""").find(colors)?.groupValues?.get(1)
            ?: error("zig_splash_velvet_dark missing from colors.xml")
        assertEquals(
            "colors.xml zig_splash_velvet_dark must equal SplashPalette.VelvetDark",
            hex.uppercase(),
            "%08X".format(SplashPalette.VelvetDark.toArgb()),
        )
        val base = resFile("values/themes.xml").readText()
        assertTrue(base.contains("""<item name="android:windowBackground">@color/zig_splash_velvet_dark</item>"""))
        val v31 = resFile("values-v31/themes.xml").readText()
        assertTrue(v31.contains("""<item name="android:windowSplashScreenBackground">@color/zig_splash_velvet_dark</item>"""))
        assertTrue("system splash shows no icon", v31.contains("""android:windowSplashScreenAnimatedIcon">@android:color/transparent"""))
    }

    // ---- intro ------------------------------------------------------------------------------

    @Test
    fun zLandsAndSettlesBeforeTheExit() {
        val t = 1600f
        assertEquals(1f, T.zAlpha(t), 1e-4f)
        assertEquals(0f, T.zBlurDp(t), 1e-3f)
        assertTrue("tilt settled", abs(T.zRotationX(t)) < 0.05f)
        assertTrue("scale settled (only breathing remains)", abs(T.zScale(t) - 1f) < 0.01f)
        assertTrue("float stays subtle", abs(T.zOffsetYDp(t)) < 3f)
    }

    @Test
    fun theWaitIsNeverStill() {
        // Something on screen changes between any two frames of the intro.
        var t = 0f
        while (t < exit) {
            val a = signature(t)
            val b = signature(t + 1000f / 60f)
            assertTrue("frame at $t ms is identical to the next one", a.zip(b).any { (x, y) -> abs(x - y) > 1e-5f })
            t += 50f
        }
    }

    @Test
    fun everyChannelIsContinuousAt120Hz() {
        val frame = 1000f / 120f
        var t = 0f
        while (t < end + 200f) {
            val a = channels(t)
            val b = channels(t + frame)
            for ((name, pair) in a.keys.associateWith { a.getValue(it) to b.getValue(it) }) {
                val limit = if (name.startsWith("rot")) 1.5f else if (name == "blurDp" || name == "offsetDp") 1.2f else 0.09f
                assertTrue("$name jumps ${pair.second - pair.first} between $t and ${t + frame} ms", abs(pair.second - pair.first) <= limit)
            }
            t += 1f
        }
    }

    @Test
    fun progressAcceleratesAndCompletesAtTheExit() {
        assertEquals(0f, T.progress(300f, exit), 1e-4f)
        assertEquals(1f, T.progress(exit, exit), 1e-4f)
        var prev = -1f
        var prevStep = 0f
        var t = 300f
        while (t <= exit) {
            val p = T.progress(t, exit)
            assertTrue(p >= prev)
            if (prev >= 0f) {
                assertTrue("progress must accelerate", p - prev >= prevStep - 1e-6f)
                prevStep = p - prev
            }
            prev = p
            t += 50f
        }
    }

    @Test
    fun sweepsEnterAndLeaveOffTheGlass() {
        for (s in T.SWEEPS) {
            val start = T.sweep(s.start + 1f, exit)!!.first
            val finish = T.sweep(s.start + s.duration - 1f, exit)!!.first
            assertTrue("sweep must start outside the glass", start < -0.35f)
            assertTrue("sweep must end outside the glass", finish > 1.35f)
        }
    }

    // ---- exit / hand-off to the app ---------------------------------------------------------

    @Test
    fun uiIsHiddenUnderTheSplashAndLandsExactlyInPlace() {
        for (t in listOf(0f, 1000f, exit)) {
            assertEquals("content invisible during the intro at $t", 0f, T.contentAlpha(t, exit), 0f)
            assertEquals("nav invisible during the intro at $t", 0f, T.navAlpha(t, exit), 0f)
            assertEquals("curtain down during the intro at $t", 0f, T.curtain(t, exit), 1e-6f)
        }
        assertEquals(1f, T.curtain(end, exit), 1e-5f)
        assertEquals(1f, T.contentProgress(end, exit), 1e-5f)
        assertEquals(1f, T.contentAlpha(end, exit), 1e-5f)
        assertEquals(1f, T.navProgress(end, exit), 1e-5f)
        assertEquals(1f, T.navAlpha(end, exit), 1e-5f)
        assertTrue(T.isFinished(end, exit, reducedMotion = false))
        assertTrue(!T.isFinished(end - 1f, exit, reducedMotion = false))
    }

    @Test
    fun theSlideInIsStillTravellingWhenTheCurtainUncoversIt() {
        // When a fifth of the screen is uncovered the content must still be visibly moving, then
        // decelerate monotonically to rest - a glide, not a landing that already happened.
        var t = exit
        while (T.curtain(t, exit) < 0.2f) t += 1f
        val remaining = (1f - T.contentProgress(t, exit)) * T.CONTENT_FROM_OFFSET
        assertTrue("content offset when revealed = $remaining of its height", remaining > 0.03f)
        var prevSpeed = Float.MAX_VALUE
        var tt = t
        while (tt < end) {
            val speed = T.contentProgress(tt + 16f, exit) - T.contentProgress(tt, exit)
            assertTrue("visible slide must only decelerate", speed <= prevSpeed + 1e-5f)
            prevSpeed = speed
            tt += 16f
        }
    }

    @Test
    fun tapToSkipOnlyAfterTheMarkHasArrivedAndNeverStartsANewSweep() {
        assertTrue(T.MIN_SKIP_MS >= 600f)
        val skipAt = 2200f // just before the second sweep would have begun
        var t = skipAt
        while (t < skipAt + T.EXIT_TOTAL_MS) {
            assertNull("no sweep may begin after the exit started", T.sweep(t, skipAt))
            t += 10f
        }
    }

    // ---- fits every screen ------------------------------------------------------------------

    private val screens = listOf(
        // w, h (px), density
        Triple(720f, 1280f, 2f), Triple(1080f, 2400f, 2.625f), Triple(1440f, 3200f, 3.5f), Triple(1080f, 2640f, 2.75f),
        Triple(2400f, 1080f, 2.625f), Triple(3200f, 1440f, 3.5f),       // landscape phones
        Triple(1600f, 2560f, 2f), Triple(2560f, 1600f, 2f),             // tablets
        Triple(2208f, 1840f, 2.625f), Triple(1840f, 2208f, 2.625f),     // foldables (inner)
        Triple(1080f, 1080f, 2f), Triple(480f, 854f, 1.5f),            // square, tiny
    )

    @Test
    fun theZIsFullyOnScreenCentredAndSensiblySizedEverywhere() {
        for ((w, h, d) in screens) {
            val f = SplashLayout.zFrame(w, h, d)
            assertTrue("Z inside $w x $h", f.left >= 0f && f.top >= 0f && f.right <= w && f.bottom <= h)
            assertEquals(w / 2f, f.centerX, 0.5f)
            assertEquals(h / 2f, f.centerY, 0.5f)
            val glass = SplashLayout.glassWidth(w, h, d)
            assertTrue("Z never larger than the cap", glass <= SplashLayout.Z_MAX_WIDTH_DP * d + 0.01f)
            assertTrue("Z never tiny on $w x $h", glass >= 0.2f * minOf(w, h))
            // Room for the entrance (scale 0.86 -> 1 plus breathing) and the float without clipping.
            val glassH = glass * SplashArtGeometry.Z_GLASS_HEIGHT / SplashArtGeometry.Z_GLASS_WIDTH
            assertTrue("vertical room on $w x $h", glassH * 1.05f + 60f * d < h)
        }
    }

    @Test
    fun theVelvetAlwaysCoversTheScreenIncludingDriftAndNeverOverZooms() {
        val pw = SplashArtGeometry.MASTER_WIDTH
        val ph = SplashArtGeometry.MASTER_HEIGHT
        for ((w, h, _) in screens) {
            val rotated = SplashLayout.rotateVelvet(w, h)
            val effW = if (rotated) ph else pw
            val effH = if (rotated) pw else ph
            val s = SplashLayout.velvetCoverScale(pw, ph, w, h) * T.VELVET_OVERSCAN // zoom >= 1 only adds
            val halfW = effW * s / 2f
            val halfH = effH * s / 2f
            val maxDx = 0.010f * w
            val maxDy = 0.007f * h
            assertTrue("velvet covers horizontally on $w x $h", halfW - maxDx >= w / 2f - 0.5f)
            assertTrue("velvet covers vertically on $w x $h", halfH - maxDy >= h / 2f - 0.5f)
            // The orientation-aware rotation never needs more zoom than the other orientation would.
            val otherW = if (rotated) pw else ph
            val otherH = if (rotated) ph else pw
            val other = maxOf(w / otherW, h / otherH) * T.VELVET_OVERSCAN
            assertTrue("rotation choice over-zooms on $w x $h ($s vs $other)", s <= other + 1e-4f)
        }
    }

    // ---- helpers ----------------------------------------------------------------------------

    private fun channels(t: Float): Map<String, Float> {
        return mapOf(
            "exposure" to T.exposure(t),
            "zoom" to T.velvetZoom(t),
            "light" to T.lightStrength(t),
            "lightX" to T.lightX(t),
            "glow" to T.glow(t),
            "zAlpha" to T.zAlpha(t),
            "zScale" to T.zScale(t),
            "blurDp" to T.zBlurDp(t),
            "rotX" to T.zRotationX(t),
            "rotY" to T.zRotationY(t),
            "rotZ" to T.zRotationZ(t),
            "offsetDp" to T.zOffsetYDp(t),
            "shadow" to T.shadowAlpha(t),
            "sparkle" to T.sparkle(t, exit),
            "progressAlpha" to T.progressAlpha(t, exit),
            "curtain" to T.curtain(t, exit),
            "pop" to T.exitPop(t, exit),
            "content" to T.contentProgress(t, exit),
            "contentAlpha" to T.contentAlpha(t, exit),
            "nav" to T.navProgress(t, exit),
            "navAlpha" to T.navAlpha(t, exit),
        )
    }

    private fun signature(t: Float) = channels(t).values.toList() + listOf(T.velvetDriftX(t), T.lightY(t))

    private fun resFile(relative: String): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            listOf(File(dir, "app/src/main/res/$relative"), File(dir, "src/main/res/$relative")).forEach {
                if (it.isFile) return it
            }
            dir = dir.parentFile
        }
        error("resource not found: $relative")
    }
}
