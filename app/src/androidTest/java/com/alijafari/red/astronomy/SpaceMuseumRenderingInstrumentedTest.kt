package com.alijafari.red.astronomy

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zig.museum.core.engine.InspectorEngine
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Fix Prompt v2, verification item 3: instrumented rendering test for the Space Museum
 * Filament viewer.
 *
 * This is the CI-based, programmatic replacement for "trust me, it renders" prose claims. It:
 *  1. Launches the real app (MainActivity), navigates through the real UI: bottom nav "Lab" tab
 *     -> Space Museum feature card -> grid -> an object tile -> the Filament viewer screen.
 *  2. Waits for a REAL rendered-frame signal — InspectorEngine.instrumentation.frameTimings
 *     (a ring buffer fed by the Choreographer-driven doFrame() callback; see
 *     InspectorEngine.startFrameLoop()/doFrame()) reaching a minimum frame count — not a fixed
 *     Thread.sleep/delay.
 *  3. Captures the actual rendered device pixels via
 *     InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() (a real
 *     platform framework API, android.app.UiAutomation, available since API 18 with zero extra
 *     Gradle dependencies — captures real composited SurfaceView pixels, unlike
 *     ComposeTestRule.captureToImage()/onRoot().captureToImage(), which cannot see through a
 *     hardware SurfaceView because it is a separate compositor layer).
 *  4. Asserts programmatically on the captured bitmap: (a) not a uniform clear color (rules out
 *     the blue/black "nothing rendered" screen from Task 2/3 of the original investigation),
 *     and (b) a luminance gradient consistent with one-sided directional lighting (rules out the
 *     missing-NORMAL-attribute bug, where a LIT material with no real normal input produces flat,
 *     non-gradient shading).
 *  5. Saves the screenshot as a PNG on device storage so the CI workflow can `adb pull` it and
 *     upload it as a build artifact for human review, in addition to the automated assertions.
 *
 * Run for two independent objects as required: Earth (id "earth") and Mars (id "mars") — both
 * solid rocky/oceanic bodies rendered via the shared loadEllipsoidObject() path, both good
 * candidates for the winding/backface-culling and NORMAL/TANGENTS regressions this pass fixes.
 */
@RunWith(AndroidJUnit4::class)
class SpaceMuseumRenderingInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun earthRendersNonUniformFrameWithDirectionalShading() {
        renderObjectAndAssert(objectId = "earth", tileTag = "space_museum_tile_earth", screenshotName = "earth_render")
    }

    @Test
    fun marsRendersNonUniformFrameWithDirectionalShading() {
        renderObjectAndAssert(objectId = "mars", tileTag = "space_museum_tile_mars", screenshotName = "mars_render")
    }

    private val logTag = "SpaceMuseumTest"

    /**
     * Wraps [androidx.compose.ui.test.junit4.ComposeTestRule.waitUntil] so a timeout dumps the
     * full semantics tree to logcat (tag "SpaceMuseumTest", picked up by the workflow's
     * "Print filtered logcat for diagnosis" step and the uploaded logcat artifact) before failing
     * with a message that names the step, instead of a bare
     * "Condition still not satisfied after N ms" with no information about what was actually on
     * screen at the time.
     */
    private fun waitUntilOrDumpTree(stepName: String, timeoutMillis: Long = 15_000) {
        try {
            composeRule.waitUntil(timeoutMillis = timeoutMillis) {
                composeRule.onAllNodesWithTag(stepName).fetchSemanticsNodes(false).isNotEmpty()
            }
        } catch (t: Throwable) {
            val tree = try {
                composeRule.onRoot().printToString()
            } catch (inner: Throwable) {
                "<failed to capture semantics tree: ${inner.message}>"
            }
            Log.e(logTag, "Timed out waiting for testTag='$stepName'. Semantics tree at timeout:\n$tree")
            fail(
                "Timed out after ${timeoutMillis}ms waiting for testTag='$stepName'. " +
                    "Semantics tree at timeout (also in logcat tag '$logTag'):\n$tree"
            )
        }
    }

    private fun renderObjectAndAssert(objectId: String, tileTag: String, screenshotName: String) {
        // --- Navigate: bottom nav "Lab" tab -> Space Museum feature card -> grid -> object tile ---
        composeRule.onNodeWithTag("nav_item_lab").performClick()
        waitUntilOrDumpTree("lab_feature_card_space_museum")
        composeRule.onNodeWithTag("lab_feature_card_space_museum").performClick()

        waitUntilOrDumpTree("space_museum_grid_screen")
        waitUntilOrDumpTree(tileTag)
        composeRule.onNodeWithTag(tileTag).performClick()

        waitUntilOrDumpTree("space_museum_viewer_screen")

        // --- Wait for a REAL rendered-frame signal: N frames actually rendered through
        // InspectorEngine's Choreographer-driven frame loop, not a fixed sleep. ---
        val engine = InspectorEngine.getInstance()
        val minFrames = 30
        val frameDeadlineMs = System.currentTimeMillis() + 15_000
        var framesSeen = 0
        while (System.currentTimeMillis() < frameDeadlineMs) {
            framesSeen = engine.instrumentation.frameTimings.getAll().size
            if (framesSeen >= minFrames && engine.hasActiveRenderable()) break
            Thread.sleep(50)
        }
        assertTrue(
            "Expected at least $minFrames real rendered frames from InspectorEngine's frame loop " +
                "for object=$objectId, but only saw $framesSeen (hasActiveRenderable=" +
                "${engine.hasActiveRenderable()}, lastError=${engine.getLastError()})",
            framesSeen >= minFrames
        )
        assertTrue(
            "Expected InspectorEngine to report an active renderable for object=$objectId " +
                "(lastError=${engine.getLastError()})",
            engine.hasActiveRenderable()
        )

        // One extra settle wait tied to a further frame-count delta (still not an arbitrary
        // sleep-and-hope: it is bounded by, and gated on, real doFrame() ticks).
        val settleTarget = framesSeen + 10
        val settleDeadlineMs = System.currentTimeMillis() + 5_000
        while (engine.instrumentation.frameTimings.getAll().size < settleTarget &&
            System.currentTimeMillis() < settleDeadlineMs
        ) {
            Thread.sleep(50)
        }

        // --- Capture the actual rendered device pixels (real SurfaceView compositor output) ---
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap: Bitmap? = instrumentation.uiAutomation.takeScreenshot()
        assertTrue("uiAutomation.takeScreenshot() returned null for object=$objectId", bitmap != null)
        val bmp = bitmap!!

        // Save PNG to device storage so the CI workflow can `adb pull` it for the upload-artifact
        // step (human-reviewable evidence in addition to the automated assertions below).
        val outDir = File(instrumentation.targetContext.getExternalFilesDir(null), "space_museum_screenshots")
        outDir.mkdirs()
        val outFile = File(outDir, "$screenshotName.png")
        FileOutputStream(outFile).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }

        // --- Assertion (a): sample a pixel grid over the rendered object area and assert NOT
        // all sampled pixels are the same color. Rules out a uniform-clear-color blue/black
        // screen regression (the exact blue-screen bug from the original investigation). ---
        // Sample the center region of the screen (where FilamentView fills the viewer screen),
        // avoiding the very edges where UI chrome (back button, info button) may sit.
        val width = bmp.width
        val height = bmp.height
        val gridSize = 12
        val marginX = (width * 0.1f).toInt()
        val marginY = (height * 0.15f).toInt()
        val sampledColors = mutableListOf<Int>()
        val samplePoints = mutableListOf<Triple<Int, Int, Int>>() // x, y, color
        for (gx in 0 until gridSize) {
            for (gy in 0 until gridSize) {
                val x = marginX + (gx * (width - 2 * marginX) / (gridSize - 1))
                val y = marginY + (gy * (height - 2 * marginY) / (gridSize - 1))
                val color = bmp.getPixel(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))
                sampledColors.add(color)
                samplePoints.add(Triple(x, y, color))
            }
        }
        val distinctColors = sampledColors.toSet()
        assertTrue(
            "Blue/black-screen regression check FAILED for object=$objectId: all ${sampledColors.size} " +
                "sampled pixels in a $gridSize x $gridSize grid over the render area were the exact same " +
                "color (0x${Integer.toHexString(sampledColors.first())}). This is exactly the symptom of " +
                "the original blue-screen bug (backface culling discarding the entire ellipsoid, leaving " +
                "only the clear color visible).",
            distinctColors.size > 1
        )

        // --- Assertion (b): luminance gradient consistent with one-sided directional lighting.
        // InspectorEngine creates one directional light at direction(0, -1, -1) (see
        // createRendererAndScene()), so a correctly lit sphere/ellipsoid must show a luminance
        // difference between its brightest and darkest sampled surface points — a flat/uniform
        // luminance across all non-background sample points is exactly what the missing-NORMAL/
        // TANGENTS bug produced (LIT shading falls back to a constant default normal). ---
        fun luminance(color: Int): Double {
            val r = Color.red(color) / 255.0
            val g = Color.green(color) / 255.0
            val b = Color.blue(color) / 255.0
            // Rec. 709 relative luminance.
            return 0.2126 * r + 0.7152 * g + 0.0722 * b
        }

        // Only consider sample points that are plausibly "on the object" (not pure background):
        // background is drawn as near-black (SurfaceView backgroundColor = BLACK per
        // FilamentView.kt) or the very dark clear color, so exclude near-zero luminance samples
        // from the gradient calculation, but keep them counted for the non-uniform check above.
        val onObjectLuminances = samplePoints
            .map { (_, _, color) -> luminance(color) }
            .filter { it > 0.03 }

        assertTrue(
            "Expected at least a few bright (non-background) sampled pixels for object=$objectId " +
                "to evaluate lighting gradient; got ${onObjectLuminances.size} of ${samplePoints.size} " +
                "samples above the background-luminance threshold. Full luminance samples: " +
                samplePoints.map { luminance(it.third) },
            onObjectLuminances.size >= 4
        )

        val maxLum = onObjectLuminances.max()
        val minLum = onObjectLuminances.min()
        val luminanceRange = maxLum - minLum
        assertTrue(
            "Directional-lighting regression check FAILED for object=$objectId: luminance range " +
                "across on-object samples was only $luminanceRange (min=$minLum, max=$maxLum), which is " +
                "too flat to be consistent with one-sided directional lighting from the engine's single " +
                "directional light at direction(0,-1,-1). A flat/uniform luminance here is exactly the " +
                "symptom of the missing-NORMAL/TANGENTS vertex attribute bug (LIT shading with no real " +
                "surface-orientation input shades every point identically).",
            luminanceRange > 0.05
        )

        println(
            "[$screenshotName] framesSeen=$framesSeen distinctColors=${distinctColors.size} " +
                "onObjectSamples=${onObjectLuminances.size} minLum=$minLum maxLum=$maxLum " +
                "range=$luminanceRange savedTo=${outFile.absolutePath}"
        )
    }
}

