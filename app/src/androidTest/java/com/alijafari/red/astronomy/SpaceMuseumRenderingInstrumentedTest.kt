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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

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
 *
 * PRIORITY 0 FIX (post-mortem on CI run 34945704891): the assertions in step 4 above passed for
 * BOTH earth and mars while their captured screenshots were byte-identical in every measured
 * statistic (distinctColors, onObjectSamples, minLum, maxLum, luminanceRange) — i.e. this test
 * gave a false "PASS" while actually scoring the same non-viewer content twice (most likely a
 * stuck system dialog/splash surface, since the same CI run also logged an unrelated JobScheduler
 * ANR). Pixel-uniformity/gradient checks alone can rule out "blank screen" but cannot detect
 * "wrong screen, but not blank". Two checks were added to close this gap:
 *  6. A positive identity check against the live Compose semantics tree (the "debug_overlay"
 *     testTag's text, which SpaceMuseumViewerScreen renders as
 *     "Filament: ... id:$objectId | ...") confirming the screen actually on top is this
 *     object's viewer — checked once at screen-entry and again right before/around the
 *     screenshot capture.
 *  7. A cross-object fingerprint comparison: each object's sampled-color grid and luminance
 *     stats are recorded, and if a later object in the same test run produces an IDENTICAL
 *     fingerprint to an earlier, different object, the test fails outright. This is the direct
 *     regression test for the exact CI failure mode discovered above.
 */
@RunWith(AndroidJUnit4::class)
class SpaceMuseumRenderingInstrumentedTest {

    /**
     * Priority 0 fix (post-mortem on CI run 34945704891): the original version of this test
     * passed for BOTH earth and mars while their captured screenshots had byte-identical
     * distinctColors/onObjectSamples/minLum/maxLum/luminanceRange — i.e. it was scoring the
     * exact same non-viewer content (almost certainly a stuck system dialog/splash frame, since
     * the CI run also showed an unrelated JobScheduler ANR around the same time) as two separate
     * "passing" renders of two different planets. Pixel-uniformity/gradient checks alone cannot
     * detect "wrong screen, but not blank" — only a cross-run fingerprint comparison can. This
     * companion object persists across the two @Test methods within the same instrumentation
     * process/run and is used to assert the two objects' render fingerprints actually differ.
     */
    private companion object {
        data class RenderFingerprint(
            val objectId: String,
            val sampledColors: List<Int>,
            val minLum: Double,
            val maxLum: Double,
            val luminanceRange: Double,
            // Foundational Rebuild Phase 0.2 diagnostic (added after CI run 34970053606/
            // check-run 104383850708 reproduced the earth==mars fingerprint failure again with
            // matc-less instrumented.yml): a SHA-256 of the full captured bitmap's raw pixel
            // bytes, plus the render-loop counters (doFrame/beginFrame/render/endFrame call
            // counts) and viewport size at capture time. If two objects' fullBitmapSha256 values
            // are also identical, that proves the SAME screenshot bytes were captured twice
            // (stale/stuck screen) rather than two real, coincidentally-similar renders -- this
            // is strictly stronger evidence than the pre-existing 12x12 sampled-grid comparison
            // alone, which cannot rule out "different image, same derived luminance stats by
            // coincidence" (astronomically unlikely, but not proven impossible, without this).
            val fullBitmapSha256: String,
            val renderLoopSummaryAtCapture: String
        )

        val recordedFingerprints = mutableListOf<RenderFingerprint>()
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Foundational Rebuild Phase 1.4: real golden-image comparison for the Phase 1 object
     * (InspectorEngine.PHASE1_OBJECT_ID == "earth"), replacing the roadmap's still-unimplemented
     * "real DoD requirement" and MaterialTest.kt's tautological `0.02 < 0.05` self-comparison
     * (see docs/audit/MILESTONE_AUDIT.md, M1/M4).
     *
     * Honesty note (this pass cannot be run on a real device/emulator from this development
     * environment -- no Android SDK/emulator available in this sandbox, confirmed in the Phase
     * 0+1 report): there is no pre-existing, verified-real Filament screenshot available to check
     * in as app/src/androidTest/assets/golden/earth_golden.png before this test has ever actually
     * executed successfully in CI. Rather than fabricate a placeholder PNG and call it a "golden
     * image" (which would be exactly the kind of unverified claim this whole rebuild exists to
     * eliminate), this test runs in one of two honest modes:
     *  - BOOTSTRAP mode (golden file absent): captures the current render, writes it to device
     *    storage as the candidate golden image, logs and asserts only the cheap sanity checks
     *    already proven above (non-uniform, directional-gradient, identity, cross-object
     *    fingerprint) -- it does NOT silently pass a pixel-diff check that never ran. The CI
     *    artifact (space-museum-screenshots) from a run where this test is judged trustworthy by
     *    a human is meant to be reviewed and then copied into
     *    app/src/androidTest/assets/golden/earth_golden.png as a follow-up commit, at which point
     *    this test switches to COMPARE mode automatically.
     *  - COMPARE mode (golden file present): performs an actual per-pixel comparison (mean
     *    absolute difference across RGB channels, downsampled to a fixed grid so minor
     *    device/driver anti-aliasing differences don't cause spurious failures) against the
     *    checked-in reference and fails if the difference exceeds a documented tolerance.
     */
    private fun compareOrBootstrapGoldenImage(objectId: String, bitmap: Bitmap) {
        if (objectId != com.zig.museum.core.engine.InspectorEngine.PHASE1_OBJECT_ID) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val goldenAssetName = "golden/${objectId}_golden.png"
        val golden: Bitmap? = try {
            instrumentation.context.assets.open(goldenAssetName).use { android.graphics.BitmapFactory.decodeStream(it) }
        } catch (e: java.io.FileNotFoundException) {
            null
        }

        if (golden == null) {
            val bootstrapDir = File(instrumentation.targetContext.getExternalFilesDir(null), "space_museum_screenshots")
            bootstrapDir.mkdirs()
            val bootstrapFile = File(bootstrapDir, "${objectId}_golden_candidate.png")
            FileOutputStream(bootstrapFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            Log.w(
                logTag,
                "GOLDEN-IMAGE BOOTSTRAP MODE for object=$objectId: no checked-in reference found at " +
                    "app/src/androidTest/assets/$goldenAssetName, so no pixel-diff comparison ran for " +
                    "this object this run (only the non-uniform/gradient/identity/fingerprint checks " +
                    "above ran). Candidate golden image written to ${bootstrapFile.absolutePath} -- " +
                    "pull this CI artifact, have a human confirm it looks correct, and check it in at " +
                    "app/src/androidTest/assets/$goldenAssetName to enable real pixel-diff comparison " +
                    "on future runs. This is intentionally NOT a silent pass of a check that never ran."
            )
            return
        }

        // Downsample both images to a fixed small grid before comparing so minor device/driver
        // anti-aliasing/rounding differences between runs on the same content don't cause
        // spurious failures -- this test is judging "is this recognizably the same render", not
        // demanding bit-for-bit identical GPU output.
        val gridW = 32
        val gridH = 32
        fun sampleGrid(bmp: Bitmap): IntArray {
            val out = IntArray(gridW * gridH)
            for (gy in 0 until gridH) {
                for (gx in 0 until gridW) {
                    val x = (gx * (bmp.width - 1) / (gridW - 1)).coerceIn(0, bmp.width - 1)
                    val y = (gy * (bmp.height - 1) / (gridH - 1)).coerceIn(0, bmp.height - 1)
                    out[gy * gridW + gx] = bmp.getPixel(x, y)
                }
            }
            return out
        }

        val candidateGrid = sampleGrid(bitmap)
        val goldenGrid = sampleGrid(golden)
        var totalAbsDiff = 0L
        for (i in candidateGrid.indices) {
            val c = candidateGrid[i]
            val g = goldenGrid[i]
            totalAbsDiff += Math.abs(Color.red(c) - Color.red(g)) +
                Math.abs(Color.green(c) - Color.green(g)) +
                Math.abs(Color.blue(c) - Color.blue(g))
        }
        val meanAbsDiff = totalAbsDiff.toDouble() / (candidateGrid.size * 3)
        // Tolerance: 0-255 scale per channel. 24.0 (~9.4%) allows for legitimate render
        // differences (camera float jitter, driver AA, JPEG/PNG re-encode rounding in the golden
        // asset itself) while still catching "wrong material/color/lighting" regressions, which
        // this project's own history shows produce far larger differences (e.g. the Earth/Mars
        // byte-identical-fingerprint bug this pass fixes, or a solid-color fallback replacing a
        // gradient-shaded sphere).
        val tolerance = 24.0
        Log.i(logTag, "GOLDEN-IMAGE COMPARE for object=$objectId: meanAbsDiff=$meanAbsDiff (tolerance=$tolerance) against $goldenAssetName")
        assertTrue(
            "Golden-image regression check FAILED for object=$objectId: mean absolute per-channel " +
                "pixel difference against the checked-in reference ($goldenAssetName) was $meanAbsDiff, " +
                "exceeding tolerance $tolerance. The rendered image no longer matches the last " +
                "human-reviewed reference closely enough.",
            meanAbsDiff <= tolerance
        )
    }

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

        // --- Priority 0 fix: confirm the viewer screen actually visible RIGHT NOW is showing
        // THIS objectId, via the debug overlay text ("Filament: ... id:$objectId | ...") that
        // SpaceMuseumViewerScreen renders from the live objectId parameter (see
        // SpaceMuseumViewerScreen.kt debug_overlay Text composables). This is the fix for the
        // exact CI failure mode observed in run 34945704891: both earth and mars produced
        // byte-identical pixel-statistics fingerprints, meaning the screenshot assertions below
        // were almost certainly scoring a stuck/foreground system surface (e.g. an ANR dialog)
        // rather than the actual per-object viewer. A pixel-uniformity/gradient check cannot
        // detect "wrong screen, but not blank" -- only a positive identity check against the
        // live Compose semantics tree can. If this fails, the bug is that the viewer screen
        // itself never became visible/current for this objectId (navigation/composition issue,
        // or a foreground system window such as an ANR dialog stealing the screen) -- entirely
        // separate from, and upstream of, the Filament rendering assertions further down.
        waitUntilOrDumpTree("debug_overlay")
        val overlayTreeAtEntry = composeRule.onRoot().printToString()
        assertTrue(
            "Priority 0 check FAILED for object=$objectId: the debug overlay is present in the " +
                "semantics tree, but does not contain 'id:$objectId'. This means the screen " +
                "currently on top is NOT this object's viewer (e.g. still showing another " +
                "object, a stale composition, or the viewer never received this objectId). " +
                "Semantics tree:\n$overlayTreeAtEntry",
            overlayTreeAtEntry.contains("id:$objectId")
        )

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

        // Foundational Rebuild Phase 0.2 fix (root-caused via the fullBitmapSha256/
        // renderLoopSummaryAtCapture diagnostic added earlier this pass -- see CI run
        // 34973763477, check-run 104396199805, which proved identicalFullBitmap=true for
        // earth/mars with the OLD settle logic below): frameTimings.getAll().size counts every
        // Choreographer.doFrame() tick, INCLUDING ones where beginFrame() returned false and
        // nothing was actually drawn/presented (see InspectorEngine.doFrame() and
        // RenderLoopCounters.beginFrameFalse) -- the real CI evidence showed beginFrame()
        // returning true only 2 times out of 212 attempts for earth's capture moment. Waiting on
        // doFrame-tick count alone (the old "settleTarget" below) can therefore be satisfied
        // entirely by failed-beginFrame ticks, with zero guarantee that a NEW frame was actually
        // presented since this object's renderable was loaded. Fix: wait for real
        // endFrameCalls (Renderer.endFrame() actually completing -- the only honest "a frame was
        // presented" signal per RenderLoopCounters.hasPresentedAtLeastOneFrame()'s own doc
        // comment) to advance by a real amount since THIS object's load, not just any doFrame tick.
        val endFrameCallsAtObjectLoad = engine.instrumentation.renderLoop.endFrameCalls
        val minPresentedFramesSinceLoad = 3L
        val presentedFrameDeadlineMs = System.currentTimeMillis() + 10_000
        while (engine.instrumentation.renderLoop.endFrameCalls - endFrameCallsAtObjectLoad < minPresentedFramesSinceLoad &&
            System.currentTimeMillis() < presentedFrameDeadlineMs
        ) {
            Thread.sleep(50)
        }
        val presentedFramesSinceLoad = engine.instrumentation.renderLoop.endFrameCalls - endFrameCallsAtObjectLoad
        assertTrue(
            "Expected at least $minPresentedFramesSinceLoad REAL presented frames (Renderer." +
                "endFrame() actually completing, not just doFrame() ticks where beginFrame() " +
                "returned false) since object=$objectId's renderable was loaded, but only saw " +
                "$presentedFramesSinceLoad (renderLoop=${engine.instrumentation.renderLoop.summary()}). " +
                "This is the exact gap that let a stale/stuck screenshot slip through the old " +
                "doFrame-tick-count-only wait in CI run 34973763477 (identicalFullBitmap=true).",
            presentedFramesSinceLoad >= minPresentedFramesSinceLoad
        )

        // --- Capture the actual rendered device pixels (real SurfaceView compositor output) ---
        // UiAutomation.takeScreenshot() can transiently return null right after its accessibility
        // connection is established (observed on the first test method of a run: earth failed
        // with a null screenshot while mars, running second in the same process/connection,
        // succeeded immediately after). This is a documented, known transient condition of the
        // underlying accessibility service connection, not a rendering problem, so retry a few
        // times with a short backoff before failing -- the retry itself does not change what is
        // being asserted on (still the real device-composited pixels from the same API).
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        fun sha256Of(bitmap: Bitmap): String {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val bytes = ByteArray(pixels.size * 4)
            for (i in pixels.indices) {
                val p = pixels[i]
                bytes[i * 4] = (p shr 24).toByte()
                bytes[i * 4 + 1] = (p shr 16).toByte()
                bytes[i * 4 + 2] = (p shr 8).toByte()
                bytes[i * 4 + 3] = p.toByte()
            }
            return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        }

        var bitmap: Bitmap? = null
        var fullBitmapSha256 = ""
        var screenshotAttempts = 0
        // Foundational Rebuild Phase 0.2 fix: beyond the pre-existing null-screenshot retry
        // (transient accessibility-connection condition, see comment above), also retry if the
        // captured screenshot's hash exactly matches a DIFFERENT, already-recorded object's
        // fullBitmapSha256 -- this is the real, root-caused fix for CI run 34973763477's
        // identicalFullBitmap=true failure: UiAutomation.takeScreenshot() can return a still-valid
        // (non-null) but STALE compositor buffer immediately after navigation, even once
        // InspectorEngine has genuinely presented new frames for the new object (per the
        // presentedFramesSinceLoad wait above) -- the two are different layers (UiAutomation
        // screenshots the window manager's last composited buffer; InspectorEngine's counters
        // only prove Filament itself rendered). Retrying the actual capture until it demonstrably
        // differs from a known-different object's hash directly fixes what the render-loop wait
        // alone could not guarantee.
        while (screenshotAttempts < 8) {
            screenshotAttempts++
            val candidate = instrumentation.uiAutomation.takeScreenshot()
            if (candidate == null) {
                Log.w(logTag, "uiAutomation.takeScreenshot() returned null for object=$objectId " +
                    "(attempt $screenshotAttempts/8); retrying after a short delay.")
                Thread.sleep(500)
                continue
            }
            val candidateHash = sha256Of(candidate)
            val staleMatch = recordedFingerprints.firstOrNull {
                it.objectId != objectId && it.fullBitmapSha256 == candidateHash
            }
            if (staleMatch != null) {
                Log.w(logTag, "uiAutomation.takeScreenshot() for object=$objectId returned a " +
                    "screenshot byte-identical to previously-recorded object='${staleMatch.objectId}' " +
                    "(sha256=$candidateHash) on attempt $screenshotAttempts/8 -- this is the stale-" +
                    "compositor-buffer condition root-caused from CI run 34973763477; retrying " +
                    "after a short delay rather than accepting a screenshot already proven to be " +
                    "the wrong object's content.")
                bitmap = candidate
                fullBitmapSha256 = candidateHash
                Thread.sleep(500)
                continue
            }
            bitmap = candidate
            fullBitmapSha256 = candidateHash
            break
        }
        assertTrue(
            "uiAutomation.takeScreenshot() returned null for object=$objectId after $screenshotAttempts attempts",
            bitmap != null
        )
        val bmp = bitmap!!
        val renderLoopSummaryAtCapture = engine.instrumentation.renderLoop.summary()
        Log.i(logTag, "[$screenshotName] fullBitmapSha256=$fullBitmapSha256 renderLoop=$renderLoopSummaryAtCapture " +
            "screenshotAttempts=$screenshotAttempts")

        // Priority 0 fix, re-check at the actual capture moment (not just at screen-entry,
        // above): confirm the debug overlay still identifies THIS objectId right before/around
        // the screenshot that the pixel assertions below will judge. If the app navigated away,
        // or a system window (e.g. an ANR dialog) took over the foreground in between, this
        // catches it even if it slipped past the entry check.
        val overlayTreeAtCapture = try {
            composeRule.onRoot().printToString()
        } catch (t: Throwable) {
            "<failed to capture semantics tree at capture time: ${t.message}>"
        }
        assertTrue(
            "Priority 0 check FAILED for object=$objectId at screenshot-capture time: the debug " +
                "overlay text no longer contains 'id:$objectId' right before/around the pixel " +
                "screenshot. The screen being pixel-sampled below is not confirmed to be this " +
                "object's viewer. Semantics tree at capture time:\n$overlayTreeAtCapture",
            overlayTreeAtCapture.contains("id:$objectId")
        )

        // Save PNG to device storage so the CI workflow can `adb pull` it for the upload-artifact
        // step (human-reviewable evidence in addition to the automated assertions below).
        val outDir = File(instrumentation.targetContext.getExternalFilesDir(null), "space_museum_screenshots")
        outDir.mkdirs()
        val outFile = File(outDir, "$screenshotName.png")
        FileOutputStream(outFile).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }

        // --- Foundational Rebuild Phase 1.4: real golden-image comparison (or honest bootstrap)
        // for the Phase 1 object -- see compareOrBootstrapGoldenImage()'s doc comment above. ---
        compareOrBootstrapGoldenImage(objectId, bmp)

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

        // --- Priority 0 fix: record this object's render fingerprint (sampled colors +
        // luminance stats) and, once a second object has run in this process, assert the two
        // fingerprints are NOT identical. This is the direct regression test for the exact CI
        // failure discovered in run 34945704891: earth and mars produced byte-identical
        // distinctColors/onObjectSamples/minLum/maxLum/luminanceRange values, which is only
        // possible if both screenshots captured the same underlying (non-viewer) content. Two
        // genuinely different rendered planets, sampled over the same 12x12 grid, are extremely
        // unlikely to produce exactly equal luminance statistics by chance.
        val fingerprint = RenderFingerprint(
            objectId = objectId,
            sampledColors = sampledColors.toList(),
            minLum = minLum,
            maxLum = maxLum,
            luminanceRange = luminanceRange,
            fullBitmapSha256 = fullBitmapSha256,
            renderLoopSummaryAtCapture = renderLoopSummaryAtCapture
        )
        val priorFingerprints = recordedFingerprints.toList()
        recordedFingerprints.add(fingerprint)

        for (prior in priorFingerprints) {
            if (prior.objectId == fingerprint.objectId) continue
            val identicalPixels = prior.sampledColors == fingerprint.sampledColors
            val identicalLumStats = prior.minLum == fingerprint.minLum &&
                prior.maxLum == fingerprint.maxLum &&
                prior.luminanceRange == fingerprint.luminanceRange
            // Foundational Rebuild Phase 0.2 diagnostic: this is the conclusive check --
            // identical full-bitmap SHA-256 proves the exact same screenshot bytes were captured
            // for both objects (stale/stuck screen, not two real renders). If the hashes DIFFER
            // while the 12x12 sampled-grid stats above are identical, that instead means two
            // genuinely different images coincidentally produced the same derived luminance
            // statistics from the (small, 12x12) sample grid -- a real but different bug in the
            // test's own fingerprinting approach, not a rendering bug.
            val identicalFullBitmap = prior.fullBitmapSha256 == fingerprint.fullBitmapSha256
            assertFalse(
                "Priority 0 regression check FAILED: object='${fingerprint.objectId}' produced a " +
                    "render fingerprint IDENTICAL to previously-tested object='${prior.objectId}' " +
                    "(same 12x12 sampled color grid, and/or same minLum/maxLum/luminanceRange: " +
                    "prior=[minLum=${prior.minLum}, maxLum=${prior.maxLum}, range=${prior.luminanceRange}, " +
                    "fullBitmapSha256=${prior.fullBitmapSha256}, renderLoopAtCapture=${prior.renderLoopSummaryAtCapture}], " +
                    "current=[minLum=${fingerprint.minLum}, maxLum=${fingerprint.maxLum}, " +
                    "range=${fingerprint.luminanceRange}, fullBitmapSha256=${fingerprint.fullBitmapSha256}, " +
                    "renderLoopAtCapture=${fingerprint.renderLoopSummaryAtCapture}]. " +
                    "identicalFullBitmap=$identicalFullBitmap (true means proven same screenshot bytes " +
                    "twice -- stuck/stale screen, not a rendering bug per se; false means two DIFFERENT " +
                    "images produced identical derived luminance stats, which is a distinct bug in this " +
                    "test's 12x12-grid fingerprinting approach, not necessarily in the renderer). Two " +
                    "different celestial objects producing byte-identical pixel statistics is exactly " +
                    "the signature of both screenshots capturing the SAME non-viewer content (e.g. a " +
                    "stuck system dialog, splash frame, or stale composition) rather than two distinct " +
                    "rendered objects -- this is the exact way the pixel-uniformity/gradient checks " +
                    "above were silently defeated in CI run 34945704891.",
                identicalPixels || identicalLumStats
            )
        }
    }
}

