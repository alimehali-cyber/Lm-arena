package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.FrameTransformationEngine
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.tan

/**
 * Refraction verification against known reference values at standard atmospheric conditions.
 *
 * Reference values source:
 * - Meeus, J. "Astronomical Algorithms" 2nd Ed., Chapter 16, Table 16.A
 *   Standard refraction: ~34' at 0°, ~9.9' at 5°, ~5.3' at 10°, <1' above 45°
 * - Bennett, G.G. (1982) "The Calculation of Astronomical Refraction in Marine Navigation",
 *   Journal of Navigation 35, 255-259, and QJRAS 23, 158 (1982) — Bennett's formula:
 *   R = cot(h + 7.31/(h+4.4)) arcminutes, gives ~34.5' at 0°, ~9.9' at 5°, ~5.3' at 10°
 * - Sæmundsson, T. (1986) "Astronomical Refraction", Sky & Telescope 72, p.70:
 *   R = 1.02 * cot(h + 10.3/(h+5.11)) arcminutes, gives ~28-29' at 0°, ~9.7' at 5°, ~5.4' at 10°
 *   The ~29' vs ~34' difference at horizon is due to different empirical fits; both are valid
 *   within atmospheric variability. Standard tables often quote ~34-35' at horizon for Bennett.
 *
 * This test verifies that FrameTransformationEngine.applyRefraction (Bennett) and the
 * Sæmundsson formula used in CoordinateEngineLegacy produce values within expected ranges.
 */
class RefractionTest {

    private val engine = FrameTransformationEngine()

    private fun bennettRefractionArcmin(altDeg: Double): Double {
        // Direct Bennett formula: R = 1 / tan(h + 7.31/(h+4.4)) in arcminutes
        if (altDeg < -1.0) return 0.0
        return 1.0 / tan(Math.toRadians(altDeg + 7.31 / (altDeg + 4.4)))
    }

    private fun saemundssonRefractionArcmin(altDeg: Double): Double {
        // Sæmundsson formula: R = 1.02 / tan(h + 10.3/(h+5.11))
        if (altDeg < -1.5) return 0.0
        return 1.02 / tan(Math.toRadians(altDeg + 10.3 / (altDeg + 5.11)))
    }

    @Test
    fun `test Bennett refraction at horizon ~34-35 arcmin`() {
        val r0 = bennettRefractionArcmin(0.0)
        // Bennett gives ~34.5' at horizon
        assertTrue("Bennett at 0° should be ~34-35', got $r0", r0 in 30.0..38.0)

        val r0Engine = engine.applyRefraction(0.0) - 0.0
        assertTrue("FrameTransformationEngine at 0° should be ~34', got ${r0Engine * 60}", r0Engine * 60.0 in 30.0..38.0)
    }

    @Test
    fun `test refraction at 5 degrees ~9_9 arcmin`() {
        val rBennett = bennettRefractionArcmin(5.0)
        assertTrue("Bennett at 5° should be ~9.9', got $rBennett", rBennett in 8.5..11.0)

        val rSaem = saemundssonRefractionArcmin(5.0)
        assertTrue("Sæmundsson at 5° should be ~9.7', got $rSaem", rSaem in 8.5..11.0)

        val rEngine = (engine.applyRefraction(5.0) - 5.0) * 60.0
        assertTrue("Engine at 5° should be ~9.9', got $rEngine", rEngine in 8.5..11.0)
    }

    @Test
    fun `test refraction at 10 degrees ~5_3 arcmin`() {
        val rBennett = bennettRefractionArcmin(10.0)
        assertTrue("Bennett at 10° should be ~5.3', got $rBennett", rBennett in 4.0..6.5)

        val rSaem = saemundssonRefractionArcmin(10.0)
        assertTrue("Sæmundsson at 10° should be ~5.4', got $rSaem", rSaem in 4.0..6.5)

        val rEngine = (engine.applyRefraction(10.0) - 10.0) * 60.0
        assertTrue("Engine at 10° should be ~5.3', got $rEngine", rEngine in 4.0..6.5)
    }

    @Test
    fun `test refraction negligible above 45 degrees`() {
        val r45Bennett = bennettRefractionArcmin(45.0)
        assertTrue("Bennett at 45° should be <1.2', got $r45Bennett", r45Bennett < 1.2)

        val r45Saem = saemundssonRefractionArcmin(45.0)
        assertTrue("Sæmundsson at 45° should be <1.5', got $r45Saem", r45Saem < 1.5)

        val r45Engine = (engine.applyRefraction(45.0) - 45.0) * 60.0
        assertTrue("Engine at 45° should be <1.2', got $r45Engine", r45Engine < 1.2)

        val r60Engine = (engine.applyRefraction(60.0) - 60.0) * 60.0
        assertTrue("Engine at 60° should be <1', got $r60Engine", r60Engine < 1.0)
    }

    @Test
    fun `test Sæmundsson at horizon gives ~29 arcmin not 34`() {
        // This documents the difference between the two formulas at horizon
        val rSaem0 = saemundssonRefractionArcmin(0.0)
        assertTrue("Sæmundsson at 0° should be ~29', got $rSaem0", rSaem0 in 25.0..32.0)
    }

    @Test
    fun `test refraction direction is true to apparent`() {
        // applyRefraction should increase altitude (true -> apparent)
        // Refraction makes objects appear higher than geometric position
        val trueAlt = 10.0
        val apparentAlt = engine.applyRefraction(trueAlt)
        assertTrue("applyRefraction should increase altitude: $trueAlt -> $apparentAlt", apparentAlt > trueAlt)

        // removeRefraction should be inverse (apparent -> true)
        val recoveredTrue = engine.removeRefraction(apparentAlt)
        assertTrue("removeRefraction should invert applyRefraction, got $recoveredTrue vs $trueAlt", kotlin.math.abs(recoveredTrue - trueAlt) < 0.001)
    }
}
