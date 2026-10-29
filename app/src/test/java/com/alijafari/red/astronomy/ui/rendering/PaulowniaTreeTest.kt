package com.alijafari.red.astronomy.ui.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Contracts for the landscape's Paulownia — the supplied `Paulownia.kt` design, grown once and
 * projected everywhere.
 *
 * Three things must hold, because the whole valley's trees depend on them:
 *
 *  1. the growth is **deterministic** (one render per season, reused for every planting, so a
 *     non-deterministic tree would shimmer between frames);
 *  2. the tree keeps **the design's proportions** — 13.8 m tall, a clear trunk to a swollen fork
 *     at 2.8 m, eight hand-placed scaffold limbs, a crown as wide as the tree is tall — because
 *     those are the measurements that make it a *Paulownia tomentosa* and not a generic broadleaf;
 *  3. every season **dresses** the tree, on the wood the design puts the organs on.
 *
 * The raster itself ([PaulowniaSprite]) needs a device: it is `android.graphics`.
 */
class PaulowniaTreeTest {

    @Test
    fun seasonFollowsThePersianCalendar() {
        // Noon-UT Julian days (JD n.0 is noon, JD n.5 is midnight). Spring begins at Nowruz —
        // 1 Farvardin — not on 1 March, so these are the dates an Iranian landscape is read against.
        // 2024-03-20 was Nowruz 1403: the last day of the Persian winter is the day before.
        assertEquals(PaulowniaSeason.WINTER, PaulowniaSeason.forJulianDay(2_460_389.0)) // 2024-03-19
        assertEquals(PaulowniaSeason.SPRING, PaulowniaSeason.forJulianDay(2_460_390.0)) // 2024-03-20
        assertEquals(PaulowniaSeason.SPRING, PaulowniaSeason.forJulianDay(2_460_415.0)) // 2024-04-14
        // 1 Tir 1403 = 2024-06-21; the day before is still spring.
        assertEquals(PaulowniaSeason.SPRING, PaulowniaSeason.forJulianDay(2_460_482.0)) // 2024-06-20
        assertEquals(PaulowniaSeason.SUMMER, PaulowniaSeason.forJulianDay(2_460_483.0)) // 2024-06-21
        // 1 Mehr 1403 = 2024-09-22.
        assertEquals(PaulowniaSeason.SUMMER, PaulowniaSeason.forJulianDay(2_460_575.0)) // 2024-09-21
        assertEquals(PaulowniaSeason.AUTUMN, PaulowniaSeason.forJulianDay(2_460_576.0)) // 2024-09-22
        // 1 Dey 1403 = 2024-12-21.
        assertEquals(PaulowniaSeason.AUTUMN, PaulowniaSeason.forJulianDay(2_460_665.0)) // 2024-12-20
        assertEquals(PaulowniaSeason.WINTER, PaulowniaSeason.forJulianDay(2_460_666.0)) // 2024-12-21
        // An April that is spring in the Persian calendar is spring for the tree too.
        assertEquals(PaulowniaSeason.SPRING, PaulowniaSeason.forJulianDay(2_460_405.0)) // 2024-04-04
        assertEquals(PaulowniaSeason.AUTUMN, PaulowniaSeason.forJulianDay(2_460_620.0)) // 2024-11-05
        assertEquals(PaulowniaSeason.WINTER, PaulowniaSeason.forJulianDay(2_460_677.0)) // 2025-01-01
    }

    @Test
    fun growthIsDeterministicAndBounded() {
        val a = PaulowniaTree.grow(PaulowniaTree.SEED)
        val b = PaulowniaTree.grow(PaulowniaTree.SEED)
        assertEquals("same seed, same limb count", a.limbs.size, b.limbs.size)
        for (i in a.limbs.indices) {
            val x = a.limbs[i]
            val y = b.limbs[i]
            assertEquals("limb $i points", x.pts.size, y.pts.size)
            for (k in x.pts.indices) assertEquals("limb $i point $k", x.pts[k], y.pts[k], 0f)
            for (k in x.rads.indices) assertEquals("limb $i radius $k", x.rads[k], y.rads[k], 0f)
        }
        // A different seed is a different tree (the render's per-instance variation is mirroring,
        // not re-growing, so the seed itself only has to differ).
        val other = PaulowniaTree.grow(12_345)
        var differs = other.limbs.size != a.limbs.size
        if (!differs) {
            for (i in a.limbs.indices) {
                if (!a.limbs[i].pts.contentEquals(other.limbs[i].pts)) {
                    differs = true
                    break
                }
            }
        }
        assertTrue("a different seed grows a different tree", differs)
        // Bounded: this is a landscape tree, not the design's 1,050-axis hero close-up.
        assertTrue("limb count is bounded (${a.limbs.size})", a.limbs.size in 200..1_100)
        assertTrue("there are tips to hang organs on (${a.tips.size})", a.tips.size > 100)
        // The cache hands back the same grown tree rather than regrowing it.
        assertSame(PaulowniaTree.skeleton(), PaulowniaTree.skeleton())
    }

    @Test
    fun theTreeKeepsTheDesignsProportions() {
        val skeleton = PaulowniaTree.skeleton()
        var top = 0f
        var widest = 0f
        var trunkTop = 0f
        for (limb in skeleton.limbs) {
            for (i in limb.rads.indices) {
                val x = limb.pts[i * 3]
                val y = limb.pts[i * 3 + 1]
                if (y > top) top = y
                if (abs(x) > widest) widest = abs(x)
                if (limb.order == 0 && y > trunkTop) trunkTop = y
            }
        }
        // The design is 13.8 m to the crown top and 13.4 m across at its widest growth.
        assertTrue("crown reaches the design's height ($top m)", top in 10.5f..13.8f)
        assertTrue("the crown is as wide as the tree is tall ($widest m)", widest > 4.5f)
        // A long clear trunk: the leader is thrown to 3.35 m and the swollen fork sits at 2.8 m.
        assertTrue("clear trunk to the swollen fork ($trunkTop m)", trunkTop in 3.0f..3.6f)
        // Nothing but trunk and buttress root exists below the first scaffold, and no branch wood
        // hangs near the ground (the design clamps growth to 1.2 m above the root flare).
        var lowestBranch = Float.MAX_VALUE
        for (limb in skeleton.limbs) {
            if (limb.order < 2) continue
            for (i in limb.rads.indices) lowestBranch = minOf(lowestBranch, limb.pts[i * 3 + 1])
        }
        assertTrue("branch wood stays off the ground ($lowestBranch m)", lowestBranch > 1.0f)
        // Eight hand-placed scaffold limbs, the design's signature silhouette. (Order-1 limbs are
        // exactly those eight plus the six three-point buttress roots.)
        val scaffolds = skeleton.limbs.count { it.order == 1 && it.rads.size >= 4 }
        assertEquals("eight scaffold limbs", 8, scaffolds)
    }

    @Test
    fun everySeasonDressesTheTreeOnItsWood() {
        val expectations = mapOf(
            PaulowniaSeason.SPRING to PaulowniaTree.OrganKind.PANICLE,
            PaulowniaSeason.SUMMER to PaulowniaTree.OrganKind.LEAF,
            PaulowniaSeason.AUTUMN to PaulowniaTree.OrganKind.LEAF_AUTUMN,
            PaulowniaSeason.WINTER to PaulowniaTree.OrganKind.CAPSULE_OPEN
        )
        for ((season, signature) in expectations) {
            val organs = PaulowniaTree.organs(season)
            assertTrue("$season has organs (${organs.size})", organs.size > 150)
            assertTrue(
                "$season carries its signature organ ($signature)",
                organs.count { it.kind == signature } > 20
            )
            for (o in organs) {
                assertTrue("$season organ sits on the tree (y=${o.y})", o.y in -0.5f..14.5f)
                assertTrue("$season organ has a length (${o.length})", o.length > 0.005f)
                // Every organ's axis is a unit vector in screen space: the raster rotates by it.
                val len = kotlin.math.sqrt(o.ax * o.ax + o.ay * o.ay)
                assertEquals("$season organ axis is unit", 1f, len, 1e-3f)
                assertTrue("$season exposure in range (${o.exposure})", o.exposure in 0f..1.2f)
                assertTrue("$season front in range (${o.front})", o.front in 0f..1f)
            }
        }
        // Winter wears no leaves: the tree is bare wood, split capsules and next spring's buds.
        val winter = PaulowniaTree.organs(PaulowniaSeason.WINTER)
        assertTrue(
            "winter is leafless",
            winter.none {
                it.kind == PaulowniaTree.OrganKind.LEAF || it.kind == PaulowniaTree.OrganKind.LEAF_UNDER
            }
        )
        // Spring flowers before the leaves: panicles far outnumber expanding leaves.
        val spring = PaulowniaTree.organs(PaulowniaSeason.SPRING)
        assertTrue(
            "spring is blossom, not leaves",
            spring.count { it.kind == PaulowniaTree.OrganKind.PANICLE } >
                spring.count { it.kind == PaulowniaTree.OrganKind.LEAF } * 4
        )
        // Summer is the opposite: a crown of leaves with only the first buds.
        val summer = PaulowniaTree.organs(PaulowniaSeason.SUMMER)
        assertTrue(
            "summer is leaves",
            summer.count {
                it.kind == PaulowniaTree.OrganKind.LEAF || it.kind == PaulowniaTree.OrganKind.LEAF_UNDER
            } > summer.count { it.kind == PaulowniaTree.OrganKind.PANICLE }
        )
        // Placement is deterministic (the same seed dresses the same wood the same way), which is
        // what lets the sprite be baked on one device and look the same on the next.
        val skel = PaulowniaTree.skeleton()
        assertEquals(
            PaulowniaTree.placeOrgans(PaulowniaSeason.SUMMER, skel, PaulowniaTree.SEED).size,
            PaulowniaTree.placeOrgans(PaulowniaSeason.SUMMER, skel, PaulowniaTree.SEED).size
        )
        assertNotEquals(
            PaulowniaTree.placeOrgans(PaulowniaSeason.SUMMER, skel, PaulowniaTree.SEED).size,
            PaulowniaTree.placeOrgans(PaulowniaSeason.WINTER, skel, PaulowniaTree.SEED).size
        )
    }
}
