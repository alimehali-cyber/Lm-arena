package com.alijafari.red.astronomy.ui.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contracts for the far-bank back grove — the 20 Paulownia + 20 sugar maples added behind every
 * other tree on the river's far bank, at the feet of Persepolis and Mil-e Azhdaha.
 *
 * The grove is deliberately a *plan*: [LandscapeBackGrove.slots] is pure, deterministic data that
 * [SceneGroundRenderer] resolves into plantings, so the counts the request asks for (twenty more of
 * each species) and the properties that make them read as the *back* of the bank can be asserted
 * here instead of being inferred from a screenshot.
 */
class LandscapeBackGroveTest {

    @Test
    fun groveAddsExactlyTwentyOfEachSpecies() {
        assertEquals("slots", LandscapeBackGrove.PER_SPECIES * LandscapeBackGrove.ROWS,
            LandscapeBackGrove.slots.size)
        assertEquals("Paulownias", 20, LandscapeBackGrove.paulowniaCount)
        assertEquals("sugar maples", 20, LandscapeBackGrove.sugarMapleCount)
    }

    @Test
    fun groveSpansTheWholeBankAcrossBothMonuments() {
        // Persepolis stands left of centre and Mil-e Azhdaha near the right edge, so the band has to
        // reach both of them — and it must, or the "near the monuments" half of the brief fails.
        val xs = LandscapeBackGrove.slots.map { it.xFraction }
        assertTrue("left edge reached (${xs.min()})", xs.min() < 0.10f)
        assertTrue("right edge reached (${xs.max()})", xs.max() > 0.90f)
        // ...and it must be an unbroken band, not two clumps at the edges: no gap wider than ~6 %
        // of the canvas between neighbouring slots in a row. The nearer row's half-slot stagger is
        // what keeps the *band* continuous even where one row happens to open up.
        for (row in 0 until LandscapeBackGrove.ROWS) {
            val rowXs = LandscapeBackGrove.slots.filter { it.toeOffset == toeOffsetOf(row) }
                .map { it.xFraction }.sorted()
            assertEquals("row $row size", LandscapeBackGrove.PER_SPECIES, rowXs.size)
            for (i in 1 until rowXs.size) {
                assertTrue("row $row gap at ${rowXs[i]}", rowXs[i] - rowXs[i - 1] < 0.06f)
            }
        }
        // The two rows must interleave rather than stack: every near-row slot stands closer to a
        // back-row slot than half a slot away, but not on top of one.
        val backXs = LandscapeBackGrove.slots.filter { it.toeOffset == toeOffsetOf(0) }.map { it.xFraction }
        val nearXs = LandscapeBackGrove.slots.filter { it.toeOffset == toeOffsetOf(1) }.map { it.xFraction }
        nearXs.forEach { near ->
            val nearest = backXs.minOf { kotlin.math.abs(it - near) }
            assertTrue("near slot $near is offset from the back row ($nearest)", nearest > 0.005f)
        }
        // Off-canvas slots would silently lose trees.
        LandscapeBackGrove.slots.forEach {
            assertTrue("x in range (${it.xFraction})", it.xFraction > 0.02f && it.xFraction < 0.98f)
        }
    }

    @Test
    fun groveStandsBehindTheExistingFarBankTrees() {
        // The riparian gallery's feet sit roughly 0.043–0.062 h below the toe (see the gallery loop in
        // SceneGroundRenderer), so the grove's rows must start above that to read as "at the back".
        val offsets = LandscapeBackGrove.slots.map { it.toeOffset }.distinct().sorted()
        assertEquals("rows", 2, offsets.size)
        assertTrue("back row depth (${offsets.first()})", offsets.first() in 0.020f..0.032f)
        assertTrue("near row depth (${offsets.last()})", offsets.last() in 0.030f..0.042f)
        // The far row must also be the smaller and the hazier one, or the depth order inverts.
        val back = LandscapeBackGrove.slots.filter { it.toeOffset == offsets.first() }
        val near = LandscapeBackGrove.slots.filter { it.toeOffset == offsets.last() }
        assertTrue(
            "back row hazier",
            back.map { it.aerial }.average() > near.map { it.aerial }.average()
        )
        assertTrue(
            "back row smaller",
            back.map { it.heightFraction }.average() < near.map { it.heightFraction }.average()
        )
    }

    @Test
    fun groveTreesAreSmallPerspectiveScaleTrees() {
        // These are the farthest trees in the valley: 1.4–2.3 % of the scene height, i.e. smaller
        // than the plain maples (2.6–4.0 %) and far below the near-bank frames (6.6–7.2 %).
        LandscapeBackGrove.slots.forEach {
            assertTrue("height (${it.heightFraction})", it.heightFraction in 0.013f..0.024f)
            assertTrue("aerial (${it.aerial})", it.aerial in 0.80f..1.0f)
        }
    }

    @Test
    fun groveIsDeterministic() {
        // Same plan on every construction: the valley must never shimmer between clock ticks.
        val a = LandscapeBackGrove.slots
        val b = LandscapeBackGrove.slots
        assertEquals("size", a.size, b.size)
        for (i in a.indices) {
            assertEquals("x $i", a[i].xFraction, b[i].xFraction, 0f)
            assertEquals("height $i", a[i].heightFraction, b[i].heightFraction, 0f)
            assertEquals("aerial $i", a[i].aerial, b[i].aerial, 0f)
            assertEquals("depth $i", a[i].toeOffset, b[i].toeOffset, 0f)
            assertEquals("species $i", a[i].paulownia, b[i].paulownia)
        }
    }

    @Test
    fun speciesAlternateWithinARow() {
        val offsets = LandscapeBackGrove.slots.map { it.toeOffset }.distinct()
        offsets.forEach { offset ->
            val row = LandscapeBackGrove.slots.filter { it.toeOffset == offset }
            // No three consecutive slots of one species: the row reads as mixed woodland.
            for (i in 2 until row.size) {
                val run = row[i - 2].paulownia && row[i - 1].paulownia && row[i].paulownia
                assertTrue("triple Paulownia at $i", !run)
            }
            assertEquals("half each", row.size / 2, row.count { it.paulownia })
        }
    }

    private fun toeOffsetOf(row: Int): Float =
        LandscapeBackGrove.slots.map { it.toeOffset }.distinct().sorted()[row]
}
