package com.alijafari.red.astronomy.ui.rendering

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Draws the Home scene's Marvdasht landscape — the real `SceneGroundRenderer` path, with the river
 * baked exactly as the app bakes it — on this JVM with real Android graphics.
 *
 * This exists because the landscape's river is the one part of the Home screen that cannot be seen
 * in any unit test: it bakes a bitmap with `Canvas.drawVertices` on the draw thread. If that throw
 * or takes longer than a frame, the app dies at startup and nothing in the build catches it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class MarvdashtLandscapeDrawTest {

    private val width = 1080
    private val height = 1260
    private val density = Density(2.75f)

    /** One full landscape draw, exactly as `SkySceneRenderer` makes it. */
    private fun draw(nanos: LongArray): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val composition = SkySceneLayout.composition(
            widthPx = width.toFloat(),
            heightPx = height.toFloat(),
            horizonFraction = SkySceneLayout.HORIZON_FRACTION
        )
        val persepolis = PersepolisFraming.forScene(
            widthPx = width.toFloat(),
            heightPx = height.toFloat(),
            horizonYPx = composition.horizonY,
            readoutBottomPx = composition.horizonY - 24f,
            readoutRightPx = 40f,
            readoutClearancePx = 12f
        )
        val lighting = LightingEngine.computeLightingState(
            sunAltDeg = 34.0,
            moonAltDeg = -20.0,
            moonIlluminationPercent = 12.0
        )
        val canvas = ComposeCanvas(android.graphics.Canvas(bitmap))
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(width.toFloat(), height.toFloat())) {
            val t0 = System.nanoTime()
            SceneGroundRenderer.draw(
                drawScope = this,
                composition = composition,
                theme = SkyCanvasTheme.REAL_SKY,
                lighting = lighting,
                persepolis = persepolis,
                sunPosPx = Offset(width * 0.62f, height * 0.16f),
                moonPosPx = null,
                julianDay = 2461000.5
            )
            if (nanos.isNotEmpty()) nanos[0] = System.nanoTime() - t0
        }
        return bitmap
    }

    @Test
    fun `the home landscape draws without throwing`() {
        val bitmap = draw(LongArray(1))
        var drawn = 0
        for (y in 0 until height step 8) {
            for (x in 0 until width step 8) {
                if (bitmap.getPixel(x, y) != 0) drawn++
            }
        }
        assertTrue("the landscape painted nothing", drawn > 1000)
    }

    /**
     * The river is baked inside the draw pass, so its cost is the app's first-frame budget. A bake
     * that takes longer than this is a startup freeze on a phone — the app never gets its first
     * frame and Android reports it as not responding.
     *
     * Each anchor is a fresh cache key, so this measures a cold bake.
     */
    @Test
    fun `the river bake fits in the first frame's budget`() {
        val frame = PulvarRiver.Frame(width.toFloat(), height.toFloat(), 1148f)
        val t0 = System.nanoTime()
        val sprite = PulvarRiverSprite.forLayout(frame, seasonMix = 0f, sunSign = 1f)
        val ms = (System.nanoTime() - t0) / 1_000_000.0
        assertTrue("the river bake took $ms ms", ms < 400.0)
        assertTrue(sprite.frame === frame)
    }
}
