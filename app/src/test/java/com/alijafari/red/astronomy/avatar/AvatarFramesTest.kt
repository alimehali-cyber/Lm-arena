package com.alijafari.red.astronomy.avatar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.ui.rendering.SkySceneLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The baked-avatar runtime, checked as arithmetic: the metadata the app ships, the clip the Sun
 * chooses, the frame the clock chooses, and where the cell lands on the scene.
 *
 * The asset is read from `src/main/assets` exactly as the app reads it, so a build that ships a broken
 * `meta.json` fails here rather than on a phone.
 */
class AvatarFramesTest {

    private fun asset(name: String): File {
        val candidates = listOf("src/main/assets/$name", "app/src/main/assets/$name")
        return candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: error("Asset not found: $name (searched ${candidates.joinToString()})")
    }

    private val meta: AvatarFramesMeta by lazy {
        val parsed = AvatarFramesMeta.parse(asset("avatar/professor_z/meta.json").readText())
        assertNotNull("meta.json must parse", parsed)
        parsed!!
    }

    @Test
    fun theShippedAvatarHasEverySceneClip() {
        val names = meta.clips.map { it.name }.toSet()
        for (required in listOf(AvatarPoses.IDLE, AvatarPoses.DAY, AvatarPoses.DUSK, AvatarPoses.NIGHT, AvatarPoses.WAVE)) {
            assertTrue("clip '$required' is missing from the shipped avatar", required in names)
        }
        assertEquals("professor_z", meta.asset)
        assertTrue("the character should be adult-sized", meta.heightM in 1.5f..2.0f)
        assertTrue("the body must dominate its cell", meta.bodyFraction in 0.7f..1f)
    }

    @Test
    fun everyClipSheetIsPresentAndLargeEnoughForItsFrames() {
        for (clip in meta.clips) {
            val sheet = asset("avatar/professor_z/${clip.sheet}")
            assertTrue("${clip.sheet} is missing", sheet.exists())
            // Six columns by four rows of 128x192 at least: a sheet too small cannot hold its frames.
            assertTrue("${clip.sheet} is smaller than a single cell — corrupt atlas?", sheet.length() > 2_000)
            assertTrue("${clip.name} declares ${clip.frames} frames but the grid holds ${meta.columns * meta.rows}",
                clip.frames <= meta.columns * meta.rows)
            assertTrue("${clip.name} plays at ${clip.fps} fps", clip.fps in 1f..30f)
            assertEquals("${clip.name} anchors on a real anchor", 0.4f, meta.anchorX, 0.2f)
        }
    }

    @Test
    fun theSunChoosesTheSameThreeBeatsTheObserverUsed() {
        assertEquals(AvatarPoses.DAY, AvatarPoses.forSunAltitude(40.0))
        assertEquals(AvatarPoses.DAY, AvatarPoses.forSunAltitude(3.1))
        assertEquals(AvatarPoses.DUSK, AvatarPoses.forSunAltitude(2.9))
        assertEquals(AvatarPoses.DUSK, AvatarPoses.forSunAltitude(-6.9))
        assertEquals(AvatarPoses.NIGHT, AvatarPoses.forSunAltitude(-7.1))
        assertEquals(AvatarPoses.NIGHT, AvatarPoses.forSunAltitude(-30.0))
    }

    @Test
    fun framesAdvanceWithTheClockAndLoopsStayInRange() {
        val clip = meta.clip(AvatarPoses.IDLE)!!
        val frameMs = 1000f / clip.fps
        assertEquals(0, AvatarFraming.frameIndex(clip, 0L))
        assertEquals(1, AvatarFraming.frameIndex(clip, (frameMs * 1.5f).toLong()))
        // A full loop plus a bit wraps to the same frame as the bit.
        val loopMs = (clip.frames * frameMs).toLong()
        assertEquals(
            AvatarFraming.frameIndex(clip, (frameMs * 2).toLong()),
            AvatarFraming.frameIndex(clip, (frameMs * 2).toLong() + loopMs)
        )
        // A very long clock never produces a frame outside the sheet.
        for (elapsed in listOf(0L, 1_000L, 86_399_999L, 86_400_000L * 3, Long.MAX_VALUE / 2)) {
            val index = AvatarFraming.frameIndex(clip, elapsed)
            assertTrue("frame $index out of range", index in 0 until clip.frames)
        }
        // A one-shot clip holds its final frame instead of looping.
        val oneShot = clip.copy(loop = false)
        assertEquals(oneShot.frames - 1, AvatarFraming.frameIndex(oneShot, 10_000_000L))
    }

    @Test
    fun cellsAreCutFromTheSheetInReadingOrder() {
        val clip = meta.clip(AvatarPoses.IDLE)!!
        val (firstOffset, firstSize) = AvatarFraming.sourceRect(meta, clip, 0)
        assertEquals(IntOffset(0, 0), firstOffset)
        assertEquals(IntSize(meta.cellWidth, meta.cellHeight), firstSize)
        val (secondRowOffset, _) = AvatarFraming.sourceRect(meta, clip, meta.columns)
        assertEquals(IntOffset(0, meta.cellHeight), secondRowOffset)
        val (lastOffset, lastSize) = AvatarFraming.sourceRect(meta, clip, clip.frames - 1)
        assertEquals(meta.cellWidth, lastSize.width)
        assertTrue("the last frame must stay inside the sheet", lastOffset.y + lastSize.height <= meta.cellHeight * meta.rows)
    }

    @Test
    fun theCharacterStandsOnTheGrassLineAtTheObserversHeight() {
        val composition = SkySceneLayout.composition(1080f, 2340f, SkySceneLayout.HORIZON_FRACTION, figureOnRight = true)
        val placement = AvatarFraming.placement(composition, meta)

        // Feet on the composition's grass line, exactly.
        val feetInCell = placement.top + meta.anchorY * placement.height
        assertEquals(composition.figureFeetY, feetInCell, 0.5f)
        // Body height equals the observer's, so swapping the character in does not resize the scene.
        val bodyHeight = meta.bodyFraction * placement.height
        assertEquals(composition.figureHeightPx, bodyHeight, 0.5f)
        // Centred on the observer's centre.
        val centre = placement.left + meta.anchorX * placement.width
        assertEquals(composition.figureCenterX, centre, 0.5f)
        // And the cell is not degenerate.
        assertTrue(placement.width > 0f && placement.height > 0f)
        assertTrue(placement.contains(Offset(centre, composition.figureFeetY - composition.figureHeightPx * 0.4f)))
        assertFalse("a tap in the sky must not hit the character", placement.contains(Offset(20f, 40f)))
    }

    @Test
    fun mirroringKeepsTheCharacterOnTheOtherSide() {
        val composition = SkySceneLayout.composition(1080f, 2340f, SkySceneLayout.HORIZON_FRACTION, figureOnRight = false)
        val mirrored = AvatarFraming.placement(composition, meta)
        // The composition owns the side; the placement must not mirror a second time.
        assertEquals(composition.figureCenterX, mirrored.left + meta.anchorX * mirrored.width, 0.5f)
        assertTrue("mirrored placement must be on the left half", mirrored.left + mirrored.width * 0.5f < 540f)
        val normal = SkySceneLayout.composition(1080f, 2340f, SkySceneLayout.HORIZON_FRACTION, figureOnRight = true)
        val right = AvatarFraming.placement(normal, meta)
        assertTrue("default placement must be on the right half", right.left + right.width * 0.5f > 540f)
    }

    @Test
    fun aBrokenManifestKeepsTheObserverInsteadOfCrashing() {
        assertNull(AvatarFramesMeta.parse(""))
        assertNull(AvatarFramesMeta.parse("{}"))
        assertNull(AvatarFramesMeta.parse("""{"cell":{"w":1,"h":1,"cols":1,"rows":1},"clips":[]}"""))
        assertNull(AvatarFramesMeta.parse("not json at all"))
    }

    @Test
    fun theGradeIsANoOpInDaylightAndDimAtNight() {
        assertNull("daylight needs no grade", AvatarGrade.colorMatrix(SkyCanvasTheme.REAL_SKY, AvatarGrade.TINT_DAY))

        val night = AvatarGrade.colorMatrix(SkyCanvasTheme.REAL_SKY, AvatarGrade.TINT_NIGHT)
        assertNotNull("night needs a grade", night)
        val n = night!!
        // Alpha row untouched: a sprite must never lose its transparency to a grade.
        assertEquals(0f, n[15], 0f)
        assertEquals(1f, n[18], 0f)
        assertEquals(0f, n[19], 0f)
        // The night grade cools: red is attenuated more than blue. Row-major 4x5, so R->R is [0], B->B is [12].
        assertTrue("night must not warm the character", n[0] < n[12])

        val mono = AvatarGrade.colorMatrix(SkyCanvasTheme.MONOCHROME_SCIENTIFIC, AvatarGrade.TINT_DAY)
        assertNotNull("Monochrome must desaturate the character", mono)
        val m = mono!!
        // A true greyscale: every channel row mixes the same luma weights, and they sum to one.
        for (row in 0 until 3) {
            assertEquals(m[0], m[row * 5], 1e-6f)
            assertEquals(m[1], m[row * 5 + 1], 1e-6f)
            assertEquals(m[2], m[row * 5 + 2], 1e-6f)
            assertEquals(1f, m[row * 5] + m[row * 5 + 1] + m[row * 5 + 2], 1e-5f)
        }
        assertEquals(1f, m[18], 0f)

        assertEquals(AvatarGrade.TINT_DAY, AvatarGrade.lightingBucket(30.0))
        assertEquals(AvatarGrade.TINT_DUSK, AvatarGrade.lightingBucket(0.0))
        assertEquals(AvatarGrade.TINT_NIGHT, AvatarGrade.lightingBucket(-20.0))
    }
}
