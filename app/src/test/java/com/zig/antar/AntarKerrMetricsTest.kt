package com.zig.antar

import com.zig.antar.physics.AntarKerrMetrics
import com.zig.antar.physics.AntarNumberFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AntarKerrMetricsTest {

    @Test
    fun schwarzschildLimitsAreExact() {
        assertEquals(2.0, AntarKerrMetrics.outerHorizonM(0.0), 1e-9)
        assertEquals(0.0, AntarKerrMetrics.innerHorizonM(0.0), 1e-9)
        assertEquals(6.0, AntarKerrMetrics.iscoRadiusM(0.0, prograde = true), 1e-8)
        assertEquals(6.0, AntarKerrMetrics.iscoRadiusM(0.0, prograde = false), 1e-8)
        assertEquals(3.0, AntarKerrMetrics.photonOrbitM(0.0, prograde = true), 1e-9)
        assertEquals(3.0, AntarKerrMetrics.photonOrbitM(0.0, prograde = false), 1e-9)
        // Novikov-Thorne radiative efficiency for Schwarzschild: 1 - sqrt(8/9) ≈ 0.05719
        assertEquals(1.0 - kotlin.math.sqrt(8.0 / 9.0), AntarKerrMetrics.radiativeEfficiency(0.0), 1e-5)
    }

    @Test
    fun highSpinKerrMatchesWebGlEngineFormulas() {
        // Verified against KerrPhysics in the bundled WebGL2 simulation for a = 0.94:
        assertEquals(1.34117444, AntarKerrMetrics.outerHorizonM(0.94), 1e-6)
        assertEquals(2.02359310, AntarKerrMetrics.iscoRadiusM(0.94, prograde = true), 1e-6)
        assertEquals(1.42524427, AntarKerrMetrics.photonOrbitM(0.94, prograde = true), 1e-6)
        assertEquals(3.94636608, AntarKerrMetrics.photonOrbitM(0.94, prograde = false), 1e-6)
        assertTrue(AntarKerrMetrics.iscoRadiusM(0.94, prograde = false) > 8.5)
    }

    @Test
    fun extremalSpinApproachesM() {
        val obs = AntarKerrMetrics.computeObservables(spin = 0.998, observerRadiusM = 18.5)
        assertTrue(obs.horizonRadiusM in 1.05..1.07)
        assertTrue(obs.iscoProgradeM in 1.20..1.26)
        assertTrue(obs.radiativeEfficiency > 0.30)
        assertTrue(obs.gravitationalTimeDilation in 0.94..0.95)
    }

    @Test
    fun timeDilationVanishesInsideStaticLimit() {
        assertNull(AntarKerrMetrics.staticTimeDilation(spin = 0.5, radiusM = 1.5, inclinationDeg = 90.0))
        val eqOutside = AntarKerrMetrics.staticTimeDilation(spin = 0.5, radiusM = 20.0, inclinationDeg = 90.0)
        assertNotNull(eqOutside)
        assertEquals(kotlin.math.sqrt(0.9), eqOutside!!, 1e-9)
    }

    @Test
    fun persianNumeralFormattingAndParsingRoundTrips() {
        val en = AntarNumberFormat.decimal(18.5, 1, isPersian = false)
        assertEquals("18.5", en)

        val fa = AntarNumberFormat.decimal(18.5, 1, isPersian = true)
        assertEquals("۱۸٫۵", fa)

        val negFa = AntarNumberFormat.decimal(-0.94, 2, isPersian = true)
        assertEquals("−۰٫۹۴", negFa)

        val parsed = AntarNumberFormat.parseFlexibleDouble("−۰٫۹۴")
        assertNotNull(parsed)
        assertEquals(-0.94, parsed!!, 1e-9)
    }
}
