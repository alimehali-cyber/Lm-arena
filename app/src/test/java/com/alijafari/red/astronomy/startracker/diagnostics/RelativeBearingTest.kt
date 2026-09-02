package com.alijafari.red.astronomy.startracker.diagnostics

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class RelativeBearingTest {

    @Test
    fun testWrap180() {
        assertEquals(0.0, RelativeBearing.wrap180(0.0), 1e-9)
        assertEquals(90.0, RelativeBearing.wrap180(90.0), 1e-9)
        assertEquals(-90.0, RelativeBearing.wrap180(270.0), 1e-9)
        assertEquals(-180.0, RelativeBearing.wrap180(180.0), 1e-9) // 180 maps to -180 per formula
        assertEquals(0.0, RelativeBearing.wrap180(360.0), 1e-9)
        assertEquals(179.0, RelativeBearing.wrap180(179.0), 1e-9)
        assertEquals(-179.0, RelativeBearing.wrap180(181.0), 1e-9)
    }

    @Test
    fun testRelativeBearingNorthHemisphere() {
        // Facing South 180°
        val facing = 180.0
        assertEquals(-90.0, RelativeBearing.relativeBearing(90.0, facing), 1e-9) // East left
        assertEquals(90.0, RelativeBearing.relativeBearing(270.0, facing), 1e-9) // West right
        assertEquals(0.0, RelativeBearing.relativeBearing(180.0, facing), 1e-9) // South center
        println("North hemisphere (facing South): East 90° -> ${RelativeBearing.relativeBearing(90.0, facing)} (expected -90 left), West 270° -> ${RelativeBearing.relativeBearing(270.0, facing)} (expected 90 right)")
    }

    @Test
    fun testRelativeBearingSouthHemisphere() {
        // Facing North 0°
        val facing = 0.0
        assertEquals(90.0, RelativeBearing.relativeBearing(90.0, facing), 1e-9) // East right
        assertEquals(-90.0, RelativeBearing.relativeBearing(270.0, facing), 1e-9) // West left
        assertEquals(0.0, RelativeBearing.relativeBearing(0.0, facing), 1e-9) // North center
        println("South hemisphere (facing North): East 90° -> ${RelativeBearing.relativeBearing(90.0, facing)} (expected 90 right), West 270° -> ${RelativeBearing.relativeBearing(270.0, facing)} (expected -90 left)")
    }

    @Test
    fun testFacingFromLatitude() {
        assertEquals(180.0, RelativeBearing.facingFromLatitude(40.0), 1e-9)
        assertEquals(180.0, RelativeBearing.facingFromLatitude(0.0), 1e-9)
        assertEquals(0.0, RelativeBearing.facingFromLatitude(-35.0), 1e-9)
    }

    @Test
    fun testHandArithmeticFourCases() {
        // Task1: hand arithmetic 4 cases
        val cases = listOf(
            Triple(90.0, 40.0, -90.0), // East, North lat
            Triple(270.0, 40.0, 90.0), // West, North lat
            Triple(90.0, -35.0, 90.0), // East, South lat
            Triple(270.0, -35.0, -90.0) // West, South lat
        )

        println("Hand arithmetic 4 cases:")
        for ((az, lat, expected) in cases) {
            val rel = RelativeBearing.relativeBearingFromLatitude(az, lat)
            val facing = RelativeBearing.facingFromLatitude(lat)
            val x = RelativeBearing.toScreenX(rel)
            println("Az $az°, Lat $lat° (facing $facing°) -> relAz $rel° (expected $expected°), x=$x")
            assertEquals(expected, rel, 1e-9)
        }
    }

    @Test
    fun testCurrentBuggyVsFixed() {
        // Current buggy southern: 0 - az
        fun currentBuggy(az: Double, lat: Double): Double {
            return if (lat >= 0) {
                RelativeBearing.wrap180(az - 180.0)
            } else {
                RelativeBearing.wrap180(0.0 - az)
            }
        }

        fun fixed(az: Double, lat: Double): Double {
            val facing = if (lat >= 0) 180.0 else 0.0
            return RelativeBearing.wrap180(az - facing)
        }

        val southLat = -35.0
        val eastAz = 90.0
        val westAz = 270.0

        val eastCurrent = currentBuggy(eastAz, southLat)
        val eastFixed = fixed(eastAz, southLat)
        val westCurrent = currentBuggy(westAz, southLat)
        val westFixed = fixed(westAz, southLat)

        println("Southern hemisphere bug verification:")
        println("East 90°: current $eastCurrent°, fixed $eastFixed° — bug confirmed: ${eastCurrent != eastFixed}")
        println("West 270°: current $westCurrent°, fixed $westFixed° — bug confirmed: ${westCurrent != westFixed}")

        assertEquals(-90.0, eastCurrent, 1e-9)
        assertEquals(90.0, eastFixed, 1e-9)
        assertEquals(90.0, westCurrent, 1e-9)
        assertEquals(-90.0, westFixed, 1e-9)
    }
}
