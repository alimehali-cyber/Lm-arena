package com.alijafari.red.astronomy.ui.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contracts for the landscape's sugar maple — the supplied `SugarMaple.kt` design, grown once and
 * projected at every planting.
 *
 * The valley's whole maple population is one render, so three things must hold:
 *
 *  1. the growth is **deterministic** (a non-deterministic tree would shimmer, and the sprite cache
 *     would hand back a tree that no longer matches its neighbours);
 *  2. the tree keeps **the design's proportions** — about 19.6 m to the crown top, an oval crown
 *     roughly 15 m across on a single leader, ten scaffolds, a canopy of thousands of sites — because
 *     those are the measurements that make it *Acer saccharum* and not a generic broadleaf;
 *  3. the **Persian calendar** picks the look: spring and summer are green, autumn and winter are
 *     the red-orange crown, as instructed.
 *
 * The raster itself ([SugarMapleSprite]) needs a device: it is `android.graphics`.
 */
class SugarMapleTreeTest {

    @Test
    fun growthIsDeterministicAndComplete() {
        val a = SugarMapleTree.grow(SugarMapleTree.SEED)
        val b = SugarMapleTree.grow(SugarMapleTree.SEED)
        assertEquals("same seed, same limb count", a.limbs.size, b.limbs.size)
        assertEquals("same seed, same tips", a.tips.size, b.tips.size)
        for (i in a.limbs.indices) {
            val x = a.limbs[i]
            val y = b.limbs[i]
            assertEquals("limb $i points", x.pts.size, y.pts.size)
            for (k in x.pts.indices) assertEquals("limb $i point $k", x.pts[k], y.pts[k], 0f)
            for (k in x.rads.indices) assertEquals("limb $i radius $k", x.rads[k], y.rads[k], 0f)
        }
        // The design's axis budget is spent whole: breadth-first growth that stopped early would
        // leave the first scaffolds half-built and the crown hollow on one side.
        assertTrue("the design's axis budget is used (${a.branchCount})", a.branchCount in 1_200..2_300)
        assertTrue("there is wood (${a.limbs.size} limbs)", a.limbs.size > 3_000)
        // The cache hands back the grown tree rather than regrowing it.
        assertSame(SugarMapleTree.skeleton(), SugarMapleTree.skeleton())
    }

    @Test
    fun theTreeKeepsTheDesignsProportions() {
        val skel = SugarMapleTree.skeleton()
        var top = 0f
        var lowest = Float.MAX_VALUE
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        for (limb in skel.limbs) {
            var i = 0
            while (i < limb.rads.size) {
                val y = limb.pts[i * 3 + 1]
                val x = limb.pts[i * 3]
                if (y > top) top = y
                if (y < lowest) lowest = y
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                i++
            }
        }
        // ~19.6 m tall: the design's crown top, with the current-year shoots in the shell.
        assertTrue("crown top is the design's ($top m)", top in 17.5f..21.5f)
        // The trunk stands on the ground.
        assertTrue("the tree stands on y = 0 ($lowest)", lowest in -0.2f..0.6f)
        // An oval crown about 15 m across, not a column and not a flat-topped umbrella.
        val spread = maxX - minX
        assertTrue("crown spread is the design's ($spread m)", spread in 11.5f..17.5f)
        assertTrue("the crown is the tallest thing about it", top > spread * 0.95f)
    }

    @Test
    fun theCanopyCarriesLeavesClustersSamarasAndBuds() {
        val organs = SugarMapleTree.organs()
        var leaves = 0
        var clusters = 0
        var samaras = 0
        var buds = 0
        var above = 0
        for (o in organs) {
            when (o.kind) {
                SugarMapleTree.OrganKind.LEAF -> leaves++
                SugarMapleTree.OrganKind.CLUSTER -> clusters++
                SugarMapleTree.OrganKind.SAMARA -> samaras++
                SugarMapleTree.OrganKind.BUD -> buds++
            }
            if (o.y > 4f) above++
        }
        // The design's canopy, thinned by the sprite's deterministic stride. The counts matter less
        // than the fact that all four organ kinds survived the stride: samaras and buds are what
        // make a maple read as a maple in spring, not just a green mass.
        assertTrue("leaves ($leaves)", leaves > 600)
        assertTrue("clusters ($clusters)", clusters > 300)
        assertTrue("samaras ($samaras)", samaras > 20)
        assertTrue("buds ($buds)", buds > 20)
        assertTrue("the canopy sits in the crown ($above of ${organs.size})", above > organs.size * 0.85)
        // Nothing of the canopy mass may leave the crown shell — a detached blob reads as an
        // island of leaves floating above the tree.
        for (o in organs) {
            if (o.kind != SugarMapleTree.OrganKind.CLUSTER) continue
            val nx = o.x / 7.55f
            val ny = (o.y - 11.05f) / 7.35f
            val nz = o.z / 4.55f
            val d = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
            assertTrue("a cluster centre stays inside the crown ($d)", d < 1.01f)
        }
    }

    @Test
    fun springAndSummerAreGreenAndAutumnAndWinterAreRedOrange() {
        // The user's rule, verbatim: green for spring and summer, the red-orange crown for autumn
        // and winter — not the design's single autumn mesh for autumn alone.
        assertEquals(SugarMapleLook.GREEN, SugarMapleLook.forSeason(PersianCalendar.Season.SPRING))
        assertEquals(SugarMapleLook.GREEN, SugarMapleLook.forSeason(PersianCalendar.Season.SUMMER))
        assertEquals(SugarMapleLook.AUTUMN, SugarMapleLook.forSeason(PersianCalendar.Season.AUTUMN))
        assertEquals(SugarMapleLook.AUTUMN, SugarMapleLook.forSeason(PersianCalendar.Season.WINTER))

        // And through the calendar: Nowruz turns the valley green, 1 Mehr turns it red.
        assertEquals(SugarMapleLook.GREEN, SugarMapleLook.forJulianDay(2_460_390.5))  // 1 Farvardin 1403
        assertEquals(SugarMapleLook.GREEN, SugarMapleLook.forJulianDay(2_460_483.5))  // 1 Tir 1403
        assertEquals(SugarMapleLook.AUTUMN, SugarMapleLook.forJulianDay(2_460_576.5)) // 1 Mehr 1403
        assertEquals(SugarMapleLook.AUTUMN, SugarMapleLook.forJulianDay(2_460_666.5)) // 1 Dey 1403
        // The same rule for the Paulownia's four wardrobes: the species differ in wardrobe, never
        // in date.
        assertEquals(PaulowniaSeason.SPRING, PaulowniaSeason.forJulianDay(2_460_390.5))
        assertEquals(PaulowniaSeason.SUMMER, PaulowniaSeason.forJulianDay(2_460_483.5))
        assertEquals(PaulowniaSeason.AUTUMN, PaulowniaSeason.forJulianDay(2_460_576.5))
        assertEquals(PaulowniaSeason.WINTER, PaulowniaSeason.forJulianDay(2_460_666.5))
    }
}
