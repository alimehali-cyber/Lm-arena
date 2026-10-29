package com.alijafari.red.astronomy.ui.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The supplied `PulvarRiver3D.kt` design, as this landscape uses it.
 *
 * These are the invariants the landscape's planting pass and its band rely on: the reach's
 * hydraulic table, a channel that is always under its own water surface, a bank that always rises
 * above it, a corridor that fits inside the frame, deterministic scatter, and a water composite
 * whose alpha actually varies with depth. If the design is retuned, these are what should fail.
 */
class PulvarRiverTest {

    @Test
    fun `reach table is read back exactly at its knots`() {
        for (knot in PulvarRiver.REACH) {
            val reach = PulvarRiver.reachAt(knot[0])
            assertEquals(knot[1], reach.depth, 1e-3f)
            assertEquals(knot[2], reach.speed, 1e-3f)
            assertEquals(knot[3], reach.foam, 1e-3f)
            assertEquals(knot[4], reach.widthMul, 1e-3f)
            assertEquals(knot[5], reach.waterY, 1e-3f)
        }
    }

    @Test
    fun `the reach falls from its upstream pool to its downstream run`() {
        assertEquals(3.00f, PulvarRiver.waterY(PulvarRiver.X0), 1e-3f)
        assertEquals(0.28f, PulvarRiver.waterY(PulvarRiver.X1), 1e-3f)
        // Smoothstep between knots can flatten, never invert: the water level must not climb.
        var previous = PulvarRiver.waterY(PulvarRiver.X0)
        var x = PulvarRiver.X0
        while (x <= PulvarRiver.X1) {
            val level = PulvarRiver.waterY(x)
            assertTrue("water climbed at $x", level <= previous + 1e-4f)
            previous = level
            x += 0.5f
        }
    }

    @Test
    fun `the channel is always narrower than its banks and wider than its riffles`() {
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        var x = PulvarRiver.X0
        while (x <= PulvarRiver.X1) {
            val half = PulvarRiver.halfWidth(x)
            assertTrue("half width $half at $x", half in 8.2f..15.9f)
            if (half < min) min = half
            if (half > max) max = half
            x += 0.25f
        }
        // The design's own range: riffles pinch the channel, pools widen it.
        assertTrue("range $min..$max", max - min > 4f)
    }

    @Test
    fun `the channel is wet and its point bars breach by centimetres, not metres`() {
        var x = PulvarRiver.X0
        var emergent = 0
        var samples = 0
        while (x <= PulvarRiver.X1) {
            val level = PulvarRiver.waterY(x)
            var t = -1f
            while (t <= 1f) {
                val bed = PulvarRiver.bedHeight(x, t)
                samples++
                // The inner third of the section is always under water: that is the design's
                // thalweg, and it is what makes the pools read as water rather than gravel.
                if (abs(t) <= 0.5f) {
                    assertTrue("dry thalweg at x=$x t=$t: $bed vs $level", bed < level - 0.12f)
                }
                // A point bar may stand proud of the waterline — that is what a gravel bar is —
                // but it is a bar, not a wall: the design's own noise keeps it under half a metre.
                assertTrue("bar wall at x=$x t=$t: $bed vs $level", bed < level + 0.6f)
                if (bed > level) emergent++
                t += 0.1f
            }
            x += 1f
        }
        // Bars do emerge somewhere, or the point bars are not being built at all.
        assertTrue("no emergent bar", emergent > 0)
        assertTrue("too much dry bed: $emergent of $samples", emergent < samples / 5)
    }

    @Test
    fun `the thalweg is the deepest line of the section`() {
        val x = -30f
        val t0 = PulvarRiver.thalwegT(x)
        val onThread = PulvarRiver.profileDepth(x, t0)
        for (t in floatArrayOf(-1f, -0.8f, -0.5f, -0.2f, 0.2f, 0.5f, 0.8f, 1f)) {
            assertTrue(
                "depth at t=$t ($(PulvarRiver.profileDepth(x, t))) beats the thalweg ($onThread)",
                PulvarRiver.profileDepth(x, t) <= onThread + 1e-3f
            )
        }
    }

    @Test
    fun `point bars only grow on the inside of a bend`() {
        // x = -30 sits in a pool; the meander phase there decides which side is the inner bank.
        val x = -30f
        val bend = PulvarRiver.bendPhase(x)
        val inner = if (bend >= 0f) -1f else 1f
        val onInner = PulvarRiver.pointBarRaise(x, inner * 0.74f, PulvarRiver.profileDepth(x, inner * 0.74f))
        val onOuter = PulvarRiver.pointBarRaise(x, -inner * 0.74f, PulvarRiver.profileDepth(x, -inner * 0.74f))
        assertTrue("bar on the inner bank", onInner > 0.05f)
        assertEquals(0f, onOuter, 1e-6f)
    }

    @Test
    fun `banks rise above the water outside the channel`() {
        val point = FloatArray(3)
        var x = -100f
        while (x <= 100f) {
            val level = PulvarRiver.waterY(x)
            for (q in floatArrayOf(-1.5f, 1.5f)) {
                PulvarRiver.terrainPoint(x, q, point)
                assertTrue("bank ${point[1]} at x=$x q=$q vs water $level", point[1] > level)
            }
            x += 10f
        }
    }

    @Test
    fun `scatter is deterministic and keeps the design's counts`() {
        val a = PulvarRiver.scatter(0f)
        val b = PulvarRiver.scatter(0f)
        assertEquals(88, a.rocks.size)
        assertEquals(320, a.cobbles.size)
        assertTrue("reeds ${a.reedCount}", a.reedCount in 1..230)
        assertEquals(a.rocks[0].cx, b.rocks[0].cx, 0f)
        assertEquals(a.cobbles[5].cz, b.cobbles[5].cz, 0f)
        assertEquals(a.grasses.size, b.grasses.size)
        // Three riffle clusters, then the loose in-channel stones, then the bank stones.
        assertEquals(14 + 21 + 13 + 16 + 24, a.rocks.size)
        assertEquals(14 + 21 + 13 + 16, a.rocks.count { it.channel })
        assertEquals(24, a.rocks.count { !it.channel })
    }

    @Test
    fun `autumn reeds lose their seed heads`() {
        val summer = PulvarRiver.scatter(0f)
        val autumn = PulvarRiver.scatter(1f)
        assertTrue("summer seed heads", summer.reeds.sumOf { clump -> clump.count { it.seedHead } } > 0)
        assertEquals(0, autumn.reeds.sumOf { clump -> clump.count { it.seedHead } })
    }

    @Test
    fun `the frame keeps the corridor inside the canvas and inside the band`() {
        val frame = PulvarRiver.Frame(1080f, 1260f, 1011.4f + 1260f * 0.062f)
        assertTrue("centre ${frame.centerLineY}", frame.centerLineY in 1260f * 0.90f..1260f * 0.95f)
        assertTrue("top ${frame.corridorTopPx}", frame.corridorTopPx > 1260f * 0.78f)
        assertTrue("bottom ${frame.corridorBottomPx}", frame.corridorBottomPx < 1260f)
        // The meander never leaves the band it is scaled to.
        var x = 0f
        while (x <= 1080f) {
            val dy = abs(frame.centerY(x) - frame.centerLineY)
            assertTrue("swing $dy at $x", dy <= frame.swingPx + 0.5f)
            x += 2f
        }
        // The channel's half-width is the band's own fraction of the canvas height.
        assertEquals(
            1260f * PulvarRiver.HALF_WIDTH_FRACTION,
            frame.halfWidthPx(540f) * 11.3f / PulvarRiver.halfWidth(PulvarRiver.X0 + 112f),
            1f
        )
    }

    @Test
    fun `the frame scales with the canvas, not the aspect ratio`() {
        val tall = PulvarRiver.Frame(1080f, 1260f, 1160f)
        val wide = PulvarRiver.Frame(2160f, 1260f, 1160f)
        // Same height, twice the width: the same channel, twice the downstream detail per pixel.
        assertEquals(tall.halfWidthPx(540f), wide.halfWidthPx(1080f), 0.5f)
        assertEquals(tall.pxPerMetreZ, wide.pxPerMetreZ, 1e-4f)
        assertEquals(tall.pxPerMetreX * 2f, wide.pxPerMetreX, 1e-4f)
    }

    @Test
    fun `water alpha runs from clear shallows to opaque pools`() {
        val light = PulvarRiver.Light(
            sunDir = PulvarRiver.dirFromAzAlt(205f, 52f),
            sunCol = PulvarRiver.sunColourFor(52f),
            moonDir = PulvarRiver.dirFromAzAlt(128f, 34f),
            moonCol = floatArrayOf(0f, 0f, 0f),
            skyAmb = floatArrayOf(0.30f, 0.42f, 0.58f),
            groundAmb = floatArrayOf(0.16f, 0.17f, 0.13f)
        )
        val grad = FloatArray(2)
        val shallow = PulvarRiver.shadeWater(
            12f, 0.2f, 0.05f, 0f, 0.5f, 0.05f, 0.4f, grad, light
        )
        val deep = PulvarRiver.shadeWater(
            12f, 0.2f, 2.8f, 0f, 0.3f, 0.05f, 0.4f, grad, light
        )
        val shallowAlpha = (shallow ushr 24) and 0xFF
        val deepAlpha = (deep ushr 24) and 0xFF
        assertTrue("shallow $shallowAlpha", shallowAlpha in 0..250)
        assertTrue("deep $deepAlpha", deepAlpha in 0..250)
        assertTrue("deep water must hide the bed more than the shallows", deepAlpha > shallowAlpha)
        assertNotEquals(0, deep and 0x00FFFFFF)
    }

    @Test
    fun `opaque shading is opaque and responds to this scene's sun side`() {
        val sunRight = PulvarRiver.Light(
            sunDir = PulvarRiver.dirFromAzAlt(205f, 52f),
            sunCol = PulvarRiver.sunColourFor(52f),
            moonDir = PulvarRiver.dirFromAzAlt(128f, 34f),
            moonCol = floatArrayOf(0f, 0f, 0f),
            skyAmb = floatArrayOf(0.30f, 0.42f, 0.58f),
            groundAmb = floatArrayOf(0.16f, 0.17f, 0.13f)
        )
        val left = PulvarRiver.shadeOpaque(
            0.46f, 0.43f, 0.36f, -0.4f, 0.9f, 0.2f,
            10f, 1.2f, 4f, 0, 0.4f, 0f, sunRight
        )
        assertEquals(0xFF, (left ushr 24) and 0xFF)
        // Night: no sun, so the same surface must be darker.
        val night = PulvarRiver.Light(
            sunDir = PulvarRiver.dirFromAzAlt(205f, 52f),
            sunCol = PulvarRiver.sunColourFor(-8f),
            moonDir = PulvarRiver.dirFromAzAlt(128f, 34f),
            moonCol = PulvarRiver.moonColourFor(34f, 0.72f),
            skyAmb = floatArrayOf(0.018f, 0.030f, 0.065f),
            groundAmb = floatArrayOf(0.008f, 0.010f, 0.016f)
        )
        val dark = PulvarRiver.shadeOpaque(
            0.46f, 0.43f, 0.36f, -0.4f, 0.9f, 0.2f,
            10f, 1.2f, 4f, 0, 0.4f, 0f, night
        )
        val dayLum = ((left shr 16 and 0xFF) * 3 + (left shr 8 and 0xFF) * 5 + (left and 0xFF) * 2) / 10
        val nightLum = ((dark shr 16 and 0xFF) * 3 + (dark shr 8 and 0xFF) * 5 + (dark and 0xFF) * 2) / 10
        assertTrue("day $dayLum night $nightLum", dayLum > nightLum + 20)
    }

    @Test
    fun `boulder wakes only follow stones that stand in the channel`() {
        val scatter = PulvarRiver.scatter(0f)
        val wake = FloatArray(2)
        val channelRock = scatter.rocks.first { it.channel }
        // Just downstream of the stone, on its own line, there must be foam.
        PulvarRiver.rockWake(channelRock.x + channelRock.radius, channelRock.q, channelRock.cx, channelRock.cz, scatter.rocks, wake)
        assertTrue("wake foam ${wake[0]}", wake[0] > 0.1f)
        // Far away from any stone there is none.
        PulvarRiver.rockWake(0f, 0f, -500f, -500f, scatter.rocks, wake)
        assertEquals(0f, wake[0], 1e-6f)
    }
}
