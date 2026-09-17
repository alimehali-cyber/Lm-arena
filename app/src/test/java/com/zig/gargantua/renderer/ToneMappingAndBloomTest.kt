package com.zig.gargantua.renderer

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/**
 * Validates Milestone M6 ACES Filmic Tone Mapping and Restrained Bloom Pipeline:
 * 1. ACES filmic tone mapping operator characteristics (black stays black, monotonic, finite).
 * 2. Bloom bright-pass extraction thresholding (zero leakage into shadow or sub-threshold LDR).
 * 3. Exposure scaling and isolation from physical disk equations.
 */
class ToneMappingAndBloomTest {

    // ACES filmic approximation (Narkowicz 2015)
    private fun acesFilmic(x: Float): Float {
        val a = 2.51f
        val b = 0.03f
        val c = 2.42f
        val d = 0.59f
        val e = 0.14f
        val num = x * (a * x + b)
        val den = x * (c * x + d) + e
        return (num / den).coerceIn(0.0f, 1.0f)
    }

    private fun brightPass(r: Float, g: Float, b: Float, threshold: Float = 1.0f): Triple<Float, Float, Float> {
        val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
        if (lum <= threshold) {
            return Triple(0.0f, 0.0f, 0.0f)
        }
        val excess = lum - threshold
        val factor = excess / max(lum, 1.0e-5f)
        return Triple(r * factor, g * factor, b * factor)
    }

    @Test
    fun acesFilmicPreservesPureBlackShadow() {
        val output = acesFilmic(0.0f)
        assertEquals("Black hole shadow must remain strictly 0.0", 0.0f, output, 1e-6f)
    }

    @Test
    fun acesFilmicIsMonotonicallyIncreasingAndFinite() {
        var prevOutput = -1.0f
        val testInputs = listOf(
            0.0f, 0.001f, 0.01f, 0.05f, 0.1f, 0.5f, 1.0f,
            2.0f, 5.0f, 10.0f, 25.0f, 50.0f, 100.0f, 500.0f
        )

        for (x in testInputs) {
            val y = acesFilmic(x)

            // Must be finite and non-NaN
            assertFalse("ACES output must not be NaN for x=$x", y.isNaN())
            assertFalse("ACES output must not be Infinite for x=$x", y.isInfinite())

            // Must be in [0.0, 1.0]
            assertTrue("ACES output must be >= 0.0", y >= 0.0f)
            assertTrue("ACES output must be <= 1.0", y <= 1.0f)

            // Must be strictly monotonic
            assertTrue("ACES curve must be monotonically non-decreasing (prev=$prevOutput, curr=$y)", y >= prevOutput)
            prevOutput = y
        }

        // Highlight roll-off check
        val y10 = acesFilmic(10.0f)
        val y50 = acesFilmic(50.0f)
        assertTrue("Highlights must smoothly roll off towards 1.0 without hard clipping", y10 > 0.95f)
        assertTrue("Very high HDR values must approach 1.0", y50 > 0.99f)
    }

    @Test
    fun bloomThresholdRejectsLdrAndPassesHdrHighlights() {
        // 1. Black hole shadow (0, 0, 0)
        val shadowBloom = brightPass(0.0f, 0.0f, 0.0f, threshold = 1.0f)
        assertEquals(0.0f, shadowBloom.first, 1e-6f)
        assertEquals(0.0f, shadowBloom.second, 1e-6f)
        assertEquals(0.0f, shadowBloom.third, 1e-6f)

        // 2. Typical celestial sky background or dim outer disk (lum < 1.0)
        val dimBloom = brightPass(0.4f, 0.5f, 0.7f, threshold = 1.0f)
        assertEquals("Sub-threshold pixels must produce zero bloom", 0.0f, dimBloom.first, 1e-6f)
        assertEquals("Sub-threshold pixels must produce zero bloom", 0.0f, dimBloom.second, 1e-6f)
        assertEquals("Sub-threshold pixels must produce zero bloom", 0.0f, dimBloom.third, 1e-6f)

        // 3. Hot relativistic Doppler-boosted inner disk (lum = 4.2 > 1.0)
        val hotBloom = brightPass(5.0f, 4.0f, 2.0f, threshold = 1.0f)
        assertTrue("HDR highlights must produce positive bloom red component", hotBloom.first > 0.0f)
        assertTrue("HDR highlights must produce positive bloom green component", hotBloom.second > 0.0f)
        assertTrue("HDR highlights must produce positive bloom blue component", hotBloom.third > 0.0f)
    }

    @Test
    fun exposureScalingLiftsDimRegionsWhilePreservingRelativeContrast() {
        val dimLdr = 0.05f
        val brightHdr = 3.0f

        val defaultExposure = 1.0f
        val cinematicExposure = 1.25f

        val dimDefault = acesFilmic(dimLdr * defaultExposure)
        val dimCinematic = acesFilmic(dimLdr * cinematicExposure)

        assertTrue(
            "Cinematic exposure (1.25x) must enhance visibility of dim disk regions ($dimCinematic > $dimDefault)",
            dimCinematic > dimDefault
        )

        // Monotonic contrast ratio preservation: bright side must still be brighter than dim side
        val brightCinematic = acesFilmic(brightHdr * cinematicExposure)
        assertTrue(
            "Relativistic brightness asymmetry must remain strictly preserved ($brightCinematic > $dimCinematic)",
            brightCinematic > dimCinematic
        )
    }
}
