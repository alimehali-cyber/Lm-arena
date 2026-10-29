package com.alijafari.red.astronomy.ui.rendering.landscape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the one framing number the Home scene's foreground band depends on.
 *
 * [PulvarRiverFraming.baseFractionFromTop] is not a taste value: it is the solve that puts the 3D
 * corridor's far-bank crest on the top edge of the band, against the scene's own fixed camera. If
 * the band's top edge and the crest disagree, the strip between them is transparent and the
 * *painted* 2D Pulvar underneath shows through above the 3D water — the one visible defect this
 * integration can have. These tests pin the relation itself, its endpoints and its clamping, so a
 * later "tidy-up" of the constants cannot silently reintroduce that strip.
 */
class PulvarRiverFramingTest {

    /**
     * The relation, sampled at the aspects it was solved at (a full-width band on scene heights
     * from 560 dp down to 312 dp sits around 3–7:1 on a phone). Probed here to ±0.003, i.e. far
     * tighter than the ±0.01 the solve itself guarantees, so any edit to the coefficients fails.
     */
    @Test
    fun baseFractionFollowsTheSolvedRelation() {
        val cases = listOf(
            2.86f to 0.451f,
            3.85f to 0.591f,
            4.44f to 0.674f,
            4.76f to 0.719f,
            5.00f to 0.753f,
            5.63f to 0.842f,
            6.67f to 0.988f
        )
        for ((aspect, expected) in cases) {
            assertEquals(
                "aspect $aspect",
                expected,
                PulvarRiverFraming.baseFractionFromTop(aspect),
                0.003f
            )
        }
    }

    /** A taller band (smaller aspect) must lift the corridor, i.e. move the crest up with it. */
    @Test
    fun baseFractionIsMonotonicInAspect() {
        var previous = PulvarRiverFraming.baseFractionFromTop(2f)
        var aspect = 2.1f
        while (aspect <= 9f) {
            val value = PulvarRiverFraming.baseFractionFromTop(aspect)
            assertTrue("aspect $aspect should not invert ($value after $previous)", value > previous)
            previous = value
            aspect += 0.1f
        }
    }

    /** Extreme viewports clamp to the solved range instead of running off to absurd values. */
    @Test
    fun aspectIsClamped() {
        assertEquals(
            PulvarRiverFraming.baseFractionFromTop(2f),
            PulvarRiverFraming.baseFractionFromTop(0.4f),
            0f
        )
        assertEquals(
            PulvarRiverFraming.baseFractionFromTop(9f),
            PulvarRiverFraming.baseFractionFromTop(40f),
            0f
        )
    }

    /** Every clamped result is still a sane fraction of the view height. */
    @Test
    fun resultStaysInsideTheSolvedRange() {
        var aspect = 0.5f
        while (aspect <= 20f) {
            val value = PulvarRiverFraming.baseFractionFromTop(aspect)
            assertTrue("aspect $aspect -> $value", value in 0.24f..1.32f)
            aspect += 0.25f
        }
    }

    /** The lateral scale is the preview's, not a free parameter (see the file's notes). */
    @Test
    fun visibleWidthIsThePreviewWidth() {
        assertEquals(104f, PulvarRiverFraming.VISIBLE_WIDTH_M, 0f)
        assertEquals(1.01f, PulvarRiverFraming.PREVIEW_BASE_FRACTION_FROM_TOP, 0f)
    }
}
