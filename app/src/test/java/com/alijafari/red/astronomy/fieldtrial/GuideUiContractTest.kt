package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.Gating
import com.alijafari.red.astronomy.fieldtrial.engine.LevelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G-4.1/4.2: the UI COPY contract, testable without Compose rendering —
 * per level ONE title <= 4 words, ONE instruction <= 2 sentences in plain English
 * (never "azimuth"/"intrinsics"/"confidence ladder"), tap-level set, gating wording,
 * night palette is dim red on black (distinct from the day palette).
 */
class GuideUiContractTest {

    private val banned = listOf(
        "azimuth", "intrinsics", "confidence", "quaternion", "matrix", "boresight",
        "declination", "ephemeris", "pipeline", "calibration", "vector", "radec", "solver"
    )

    private fun sentenceCount(s: String): Int =
        Regex("[.!?](\\s|$)").findAll(s.trim()).count().coerceAtLeast(1)

    @Test
    fun `every level title is at most four words`() {
        for (level in 0..12) {
            val title = GuideUi.levelCopy(level, null).title
            assertTrue("level $level title '$title' too long", title.trim().split(Regex("\\s+")).size <= 4)
        }
    }

    @Test
    fun `every level instruction is at most two sentences and non-empty`() {
        for (level in 0..12) {
            val instruction = GuideUi.levelCopy(level, null).instruction
            assertTrue("level $level instruction empty", instruction.isNotBlank())
            assertTrue("level $level instruction > 2 sentences: '$instruction'", sentenceCount(instruction) <= 2)
        }
    }

    @Test
    fun `titles and instructions never use developer jargon`() {
        for (level in 0..12) {
            val copy = GuideUi.levelCopy(level, null)
            val text = (copy.title + " " + copy.instruction).lowercase()
            for (word in banned) {
                assertTrue("level $level uses jargon '$word': '${copy.title} / ${copy.instruction}'", !text.contains(word))
            }
        }
    }

    @Test
    fun `tap levels are exactly the tap-to-measure levels`() {
        assertEquals(setOf(1, 2, 3, 4, 5, 9), GuideUi.tapLevels)
    }

    @Test
    fun `gating text carries the reason and a clock time for not-now`() {
        val text = GuideUi.gatingText(
            Gating(LevelStatus.NOT_NOW, reason = "The Moon is below the horizon.", whenUtcMs = 1_768_000_000_000, whenKind = "Back")
        )
        assertTrue(text.contains("below the horizon"))
        assertTrue("expected a HH:mm time in: $text", Regex("\\d{1,2}:\\d{2}").containsMatchIn(text))
    }

    @Test
    fun `night palette is dim red on black and clearly darker than day`() {
        val day = GuideUi.dayPalette()
        val night = GuideUi.nightPalette()
        // night background is pure black; night accent is red-dominant (r > b)
        assertEquals(0f, night.bg.red)
        assertTrue(night.accent.red > night.accent.blue)
        assertTrue(day.accent.blue > day.accent.red)
    }
}
