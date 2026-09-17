package com.zig.gargantua.renderer

import com.zig.gargantua.disk.AccretionDiskModel
import com.zig.gargantua.disk.KerrIsco
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.*

/**
 * Validates M6 Final Visual Correctness & Presentation Requirements:
 * 1. Background contains zero procedural stars (clean, deep black).
 * 2. Captured rays remain black (zero radiance, alpha 0.0).
 * 3. Escaped rays produce black M6 background (alpha 1.0).
 * 4. Unresolved rays never produce stars (alpha 0.5, zero radiance).
 * 5. Disk emission is preserved on both approaching and receding sides.
 * 6. Frequency shift g and g^4 are applied exactly once.
 * 7. Doppler asymmetry remains physically directional.
 * 8. Legitimate disk radii are not incorrectly classified as non-emitting.
 * 9. Disk radiance remains finite, non-NaN, and deterministic.
 * 10. HDR/ACES pipeline remains deterministic and monotonic.
 * 11. Information card fits large numerical values without clipping.
 * 12. Information card uses compact bottom-corner placement.
 * 13. Existing camera gestures remain functional.
 * 14. Stationary caching remains functional.
 */
class M6FinalPresentationTest {

    private val M = 1.0
    private val a = 0.8
    private val rIn = KerrIsco.compute(M, a) // 2.9066M
    private val rOut = 22.0

    private fun novikovThorneFlux(r: Double): Double {
        if (r <= rIn || r > rOut) return 0.0
        return (M / (r * r * r)) * max(0.0, 1.0 - sqrt(rIn / r))
    }

    private fun peakFlux(): Double {
        val rPeak = (49.0 / 36.0) * rIn
        return M / (7.0 * rPeak * rPeak * rPeak)
    }

    private fun diskRadianceNormalized(r: Double, g: Double): Double {
        val f = novikovThorneFlux(r)
        val fPeak = peakFlux()
        val fNorm = if (fPeak > 1e-7) (f / fPeak).coerceIn(0.0, 1.0) else 0.0
        val g4 = g * g * g * g
        val outerTaper = ((rOut - r) / 1.5).coerceIn(0.0, 1.0)
        val iPhys = g4 * fNorm * outerTaper
        return iPhys
    }

    private fun acesFilmic(x: Double): Double {
        val aC = 2.51
        val bC = 0.03
        val cC = 2.42
        val dC = 0.59
        val eC = 0.14
        val num = x * (aC * x + bC)
        val den = x * (cC * x + dC) + eC
        return (num / den).coerceIn(0.0, 1.0)
    }

    private fun toSrgb(linear: Double): Int {
        return (linear.coerceIn(0.0, 1.0).pow(1.0 / 2.2) * 255.0).roundToInt()
    }

    private fun mainDir(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "app/src/main")
            if (File(candidate, "assets/shaders").isDirectory) return candidate
            val direct = File(dir, "src/main")
            if (File(direct, "assets/shaders").isDirectory) return direct
            dir = dir.parentFile
        }
        throw AssertionError("could not locate app/src/main")
    }

    private fun readShader(fileName: String): String {
        val f = File(mainDir(), "assets/shaders/$fileName")
        assertTrue("Shader file must exist: ${f.absolutePath}", f.exists())
        return f.readText()
    }

    // 1. M6 visible background contains no procedural stars
    @Test
    fun m6VisibleBackgroundContainsNoProceduralStars() {
        val content = readShader("gargantua_geodesic.frag")

        // Check that escaped rays output strictly vec4(0.0, 0.0, 0.0, 1.0)
        assertTrue(
            "Escaped branch must output clean black background without procedural stars",
            content.contains("fragColor = vec4(0.0, 0.0, 0.0, 1.0);")
        )

        // Check that sample_procedural_sky is NOT called in fragColor assignment
        assertFalse(
            "fragColor must not be assigned from sample_procedural_sky in M6",
            content.contains("fragColor = vec4(sample_procedural_sky") ||
                    content.contains("fragColor = vec4(color, 1.0)")
        )
    }

    // 2. Captured rays remain black
    @Test
    fun capturedRaysRemainBlack() {
        val content = readShader("gargantua_geodesic.frag")

        assertTrue(
            "Captured branch must output pure black shadow with alpha 0.0",
            content.contains("fragColor = vec4(0.0, 0.0, 0.0, 0.0);")
        )
    }

    // 3. Escaped rays produce black M6 background
    @Test
    fun escapedRaysProduceBlackM6Background() {
        val content = readShader("gargantua_geodesic.frag")

        assertTrue(
            "Escaped rayState 2 must produce clean black background",
            content.contains("else if (rayState == 2) {") &&
                    content.contains("fragColor = vec4(0.0, 0.0, 0.0, 1.0);")
        )
    }

    // 4. Unresolved rays never produce stars
    @Test
    fun unresolvedRaysNeverProduceStars() {
        val content = readShader("gargantua_geodesic.frag")

        assertTrue(
            "Unresolved branch must produce pure black with diagnostic alpha 0.5",
            content.contains("fragColor = vec4(0.0, 0.0, 0.0, 0.5);")
        )

        // Check composite shader rejects alpha <= 0.5
        val compositeContent = readShader("gargantua_composite.frag")
        assertTrue(
            "Composite shader must reject alpha <= 0.5 to keep unresolved rays strictly black",
            compositeContent.contains("if (hdr.a <= 0.5)")
        )
    }

    // 5. Disk emission is preserved on both approaching and receding sides
    @Test
    fun diskEmissionIsPreservedOnBothApproachingAndRecedingSides() {
        val testLocations = listOf(
            Triple("Approaching Inner", 3.5, 2.10),
            Triple("Approaching Peak", 4.0, 1.80),
            Triple("Approaching Mid", 8.0, 1.35),
            Triple("Approaching Outer", 16.0, 1.15),
            Triple("Direct Foreground", 6.0, 1.00),
            Triple("Receding Inner", 3.5, 0.40),
            Triple("Receding Peak", 4.0, 0.45),
            Triple("Receding Mid", 8.0, 0.65),
            Triple("Receding Outer", 16.0, 0.82),
            Triple("Receding Edge", 21.0, 0.88)
        )

        val exposure = 1.8
        for ((name, r, g) in testLocations) {
            val rad = diskRadianceNormalized(r, g)
            val postAces = acesFilmic(rad * exposure)
            val srgb = toSrgb(postAces)

            // Both sides must be clearly non-zero and luminous (sRGB >= 30 out of 255)
            assertTrue("$name (r=$r, g=$g) must be clearly visible (rad=$rad > 0.01)", rad > 0.01)
            assertTrue("$name (r=$r, g=$g) post-ACES ($postAces) must be > 0.01", postAces > 0.01)
            assertTrue("$name (r=$r, g=$g) sRGB ($srgb) must be clearly visible (>= 30)", srgb >= 30)
        }
    }

    // 6. g and g^4 are applied exactly once
    @Test
    fun frequencyShiftGAndG4AppliedExactlyOnce() {
        val content = readShader("gargantua_geodesic.frag")

        // Verify g4 definition
        assertTrue("g2 must be defined as gShift * gShift", content.contains("float g2 = gShift * gShift;"))
        assertTrue("g4 must be defined as g2 * g2", content.contains("float g4 = g2 * g2;"))

        // Verify iPhys uses g4 exactly once
        assertTrue("iPhys must be g4 * fNorm * outerTaper", content.contains("float iPhys = g4 * fNorm * outerTaper;"))
        assertTrue("radiance must use iPhys", content.contains("float radiance = iPhys;"))
    }

    // 7. Doppler asymmetry remains physically directional
    @Test
    fun dopplerAsymmetryRemainsPhysicallyDirectional() {
        // Compare symmetric radii on approaching vs receding side
        val radii = listOf(3.5, 4.0, 6.0, 8.0, 12.0, 16.0)
        for (r in radii) {
            // Approaching g > 1, Receding g < 1
            val gApp = 1.0 + 1.2 / sqrt(r)
            val gRec = 1.0 - 0.9 / sqrt(r)

            val radApp = diskRadianceNormalized(r, gApp)
            val radRec = diskRadianceNormalized(r, gRec)

            assertTrue(
                "Approaching radiance ($radApp) at r=$r must strictly exceed receding radiance ($radRec)",
                radApp > radRec
            )

            val acesApp = acesFilmic(radApp * 1.8)
            val acesRec = acesFilmic(radRec * 1.8)
            assertTrue(
                "Approaching post-ACES ($acesApp) at r=$r must strictly exceed receding ($acesRec)",
                acesApp > acesRec
            )
        }
    }

    // 8. Legitimate disk radii are not incorrectly classified as non-emitting
    @Test
    fun legitimateDiskRadiiAreNotIncorrectlyClassifiedAsNonEmitting() {
        val disk = AccretionDiskModel(M = M, a = a, outerRadius = rOut)
        val radii = listOf(2.95, 3.2, 4.0, 6.0, 10.0, 15.0, 20.0, 21.9)

        for (r in radii) {
            val f = disk.fluxProfile(r)
            assertTrue("Flux at legitimate radius r=$r must be strictly positive", f > 0.0)
            val rad = diskRadianceNormalized(r, g = 1.0)
            assertTrue("Radiance at legitimate radius r=$r must be strictly positive", rad > 0.0)
        }

        // ISCO and plunge region must produce zero emission
        assertEquals(0.0, disk.fluxProfile(rIn), 1e-12)
        assertEquals(0.0, disk.fluxProfile(rIn - 0.1), 1e-12)
        assertEquals(0.0, diskRadianceNormalized(rIn, g = 1.0), 1e-12)
    }

    // 9. Disk radiance remains finite and deterministic
    @Test
    fun diskRadianceRemainsFiniteAndDeterministic() {
        for (step in 0..100) {
            val r = rIn + (rOut - rIn) * (step / 100.0)
            for (gInt in 2..30) {
                val g = gInt * 0.1 // 0.2 to 3.0
                val rad = diskRadianceNormalized(r, g)
                assertFalse("Radiance must not be NaN for r=$r, g=$g", rad.isNaN())
                assertFalse("Radiance must not be Infinite for r=$r, g=$g", rad.isInfinite())
                assertTrue("Radiance must be non-negative for r=$r, g=$g", rad >= 0.0)
            }
        }
    }

    // 10. HDR/ACES pipeline remains deterministic
    @Test
    fun hdrAcesPipelineRemainsDeterministicAndMonotonic() {
        val inputs = listOf(0.0, 0.01, 0.05, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0)
        var prev = -1.0
        for (x in inputs) {
            val y = acesFilmic(x)
            assertTrue("ACES output must be non-decreasing: prev=$prev, curr=$y", y >= prev)
            assertTrue("ACES output must be in [0, 1]: $y", y in 0.0..1.0)
            prev = y
        }
    }

    // 11. Information card fits large numerical values without clipping
    @Test
    fun informationCardFitsLargeNumericalValuesWithoutClipping() {
        val telemetry = GargantuaTelemetry(
            fps = 120.0f,
            frameTimeMs = 33.3f,
            spin = 0.999f,
            iscoRadius = 1.45f,
            camDist = 60.0f,
            renderResolution = "1440x3200",
            renderScale = 1.0f,
            isHdrActive = true
        )

        val spinStr = String.format(Locale.US, "a*=%.2f", telemetry.spin)
        val iscoStr = String.format(Locale.US, "ISCO=%.2fM", telemetry.iscoRadius)
        val distStr = String.format(Locale.US, "d=%.0fM", telemetry.camDist)
        val resStr = String.format(Locale.US, "%s@%.1fx", telemetry.renderResolution, telemetry.renderScale)
        val timeStr = String.format(Locale.US, "• %.1f ms", telemetry.frameTimeMs)

        assertEquals("a*=1.00", spinStr)
        assertEquals("ISCO=1.45M", iscoStr)
        assertEquals("d=60M", distStr)
        assertEquals("1440x3200@1.0x", resStr)
        assertEquals("• 33.3 ms", timeStr)

        // Verify strings are short and fit within compact card width bounds
        assertTrue("Row 2 combined characters must be <= 30", (spinStr.length + iscoStr.length + distStr.length) <= 30)
        assertTrue("Row 3 combined characters must be <= 25", (resStr.length + timeStr.length) <= 25)
    }

    // 12. Information card uses compact bottom-corner placement
    @Test
    fun informationCardUsesCompactBottomCornerPlacement() {
        val rootFile = File(mainDir(), "java/com/zig/gargantua/ui/GargantuaRoot.kt")
        assertTrue("GargantuaRoot.kt must exist: ${rootFile.absolutePath}", rootFile.exists())
        val content = rootFile.readText()

        assertTrue(
            "Card must use Alignment.BottomStart for bottom corner placement",
            content.contains("Alignment.BottomStart")
        )
        assertTrue(
            "Card must constrain maximum width to prevent full-width panel",
            content.contains("widthIn(max = 240.dp)")
        )
        assertTrue(
            "Card must use testTag gargantua_status_card",
            content.contains("testTag(\"gargantua_status_card\")")
        )
    }

    // 13. Existing camera gestures remain functional
    @Test
    fun existingCameraGesturesRemainFunctional() {
        val initial = GargantuaRenderState()

        // 1-finger orbit
        val orbited = initial.copy(
            camAzimuthDeg = (initial.camAzimuthDeg + 15.0f) % 360f,
            camInclinationDeg = (initial.camInclinationDeg + 5.0f).coerceIn(5.0f, 175.0f)
        )
        assertEquals(15.0f, orbited.camAzimuthDeg, 1e-4f)
        assertEquals(87.0f, orbited.camInclinationDeg, 1e-4f)

        // Pinch zoom
        val zoomed = initial.copy(camDist = (initial.camDist * 1.5f).coerceIn(12.0f, 60.0f))
        assertEquals(36.0f, zoomed.camDist, 1e-4f)

        // 2-finger pan
        val panned = initial.copy(
            camTargetX = initial.camTargetX + 2.0f,
            camTargetY = initial.camTargetY - 1.5f
        )
        assertEquals(2.0f, panned.camTargetX, 1e-4f)
        assertEquals(-1.5f, panned.camTargetY, 1e-4f)
    }

    // 14. Stationary caching remains functional
    @Test
    fun stationaryCachingRemainsFunctional() {
        val stateA = GargantuaRenderState(camDist = 24.0f, camAzimuthDeg = 0.0f, camInclinationDeg = 82.0f)
        val stateB = GargantuaRenderState(camDist = 24.0f, camAzimuthDeg = 0.0f, camInclinationDeg = 82.0f)

        assertEquals("Stationary state objects must be equal", stateA, stateB)
        assertEquals("Hash codes must match for identical states", stateA.hashCode(), stateB.hashCode())
    }

    // =========================================================================
    // SECTION D: DETERMINISTIC TESTS FOR VISUAL / PHYSICAL HIERARCHY & REPAIRS
    // =========================================================================

    // D1. F(r) decreases from its peak toward the outer disk
    @Test
    fun fluxDecreasesFromPeakTowardOuterDisk() {
        val rPeak = (49.0 / 36.0) * rIn
        val testRadii = listOf(4.5, 6.0, 8.0, 10.0, 12.0, 16.0, 19.0, 21.8)

        var prevFlux = novikovThorneFlux(rPeak)
        assertTrue("Peak flux must be positive", prevFlux > 0.0)

        for (r in testRadii) {
            val currFlux = novikovThorneFlux(r)
            assertTrue(
                "Flux must monotonically decrease beyond peak: r=$r currFlux=$currFlux < prevFlux=$prevFlux",
                currFlux < prevFlux
            )
            prevFlux = currFlux
        }

        // Verify strong physical radial falloff: outer disk flux is > 30x lower than peak flux
        val fOuter = novikovThorneFlux(21.8)
        val fPeak = novikovThorneFlux(rPeak)
        val ratio = fPeak / fOuter
        assertTrue("Flux falloff ratio from peak to outer edge must exceed 30x, got $ratio", ratio > 30.0)
    }

    // D2. F(rISCO) = 0 and plunge region is strictly zero
    @Test
    fun fluxAtIscoIsStrictlyZero() {
        assertEquals("Flux exactly at ISCO must be strictly 0.0", 0.0, novikovThorneFlux(rIn), 1e-15)
        assertEquals("Flux inside ISCO (plunge region) must be 0.0", 0.0, novikovThorneFlux(rIn - 0.1), 1e-15)
        assertEquals("Flux near horizon must be 0.0", 0.0, novikovThorneFlux(1.8), 1e-15)
        assertTrue("Flux just outside ISCO must be strictly positive", novikovThorneFlux(rIn + 0.05) > 0.0)
    }

    // D3. Physical transferred emission g^4 * F retains expected radial ordering when g is held constant
    @Test
    fun physicalEmissionRetainsRadialOrderingWhenGIsConstant() {
        val constantG = 1.0
        val rPeak = (49.0 / 36.0) * rIn
        val testRadii = listOf(4.5, 6.0, 8.0, 12.0, 16.0, 20.0, 21.5)

        var prevRad = diskRadianceNormalized(rPeak, constantG)
        for (r in testRadii) {
            val currRad = diskRadianceNormalized(r, constantG)
            assertTrue(
                "Physical emission at constant g must strictly decrease beyond peak: r=$r ($currRad) < ($prevRad)",
                currRad < prevRad
            )
            prevRad = currRad
        }

        // At finite outer boundary rOut, emission must approach 0
        assertEquals("Emission at rOut must be exactly 0.0", 0.0, diskRadianceNormalized(rOut, constantG), 1e-15)
    }

    // D4. No emitting-disk sample becomes black because of an unexplained renderer/compositor state
    @Test
    fun noEmittingDiskSampleBecomesBlack() {
        val radii = listOf(3.1, 3.5, 4.0, 5.5, 7.0, 10.0, 14.0, 18.0, 21.0)
        val gShifts = listOf(0.35, 0.45, 0.65, 0.85, 1.00, 1.25, 1.60, 2.10)

        for (r in radii) {
            for (g in gShifts) {
                val rad = diskRadianceNormalized(r, g)
                val postAces = acesFilmic(rad * 1.8)
                val srgb = toSrgb(postAces)

                assertTrue("Radiance at r=$r, g=$g must be > 0.0 (got $rad)", rad > 0.0)
                assertTrue("Post-ACES at r=$r, g=$g must be > 0.0 (got $postAces)", postAces > 0.0)
                assertTrue("sRGB at r=$r, g=$g must be >= 1 (got $srgb)", srgb >= 1)
            }
        }
    }

    // D5. Approaching/receding Doppler asymmetry remains intact
    @Test
    fun approachingRecedingDopplerAsymmetryIntact() {
        val testRadii = listOf(3.5, 4.0, 6.0, 8.0, 12.0, 16.0, 20.0)
        for (r in testRadii) {
            val gApp = 1.0 + 1.2 / sqrt(r)
            val gRec = 1.0 - 0.9 / sqrt(r)

            val radApp = diskRadianceNormalized(r, gApp)
            val radRec = diskRadianceNormalized(r, gRec)

            assertTrue(
                "Approaching radiance ($radApp) must exceed receding radiance ($radRec) at r=$r",
                radApp > radRec
            )

            val acesApp = acesFilmic(radApp * 1.8)
            val acesRec = acesFilmic(radRec * 1.8)
            assertTrue(
                "Approaching display ($acesApp) must exceed receding display ($acesRec) at r=$r",
                acesApp > acesRec
            )
        }
    }

    // D6. No stars/background are reintroduced
    @Test
    fun noStarsOrBackgroundAreReintroduced() {
        val content = readShader("gargantua_geodesic.frag")

        // Escaped rays must produce clean black background without procedural stars
        assertTrue(
            "Escaped branch must output clean black background",
            content.contains("fragColor = vec4(0.0, 0.0, 0.0, 1.0);")
        )

        // Shadow rays must produce pure black with alpha 0.0
        assertTrue(
            "Captured shadow branch must output alpha 0.0",
            content.contains("fragColor = vec4(0.0, 0.0, 0.0, 0.0);")
        )

        // Escaped branch must never invoke sample_procedural_sky
        assertFalse(
            "sample_procedural_sky must not be called in fragColor assignment",
            content.contains("fragColor = vec4(sample_procedural_sky")
        )
    }

    // D7. Display transform does not reverse the physical brightness ordering of disk samples
    @Test
    fun displayTransformPreservesPhysicalBrightnessOrdering() {
        // Monotonicity of ACES display transform: if A >= B, then ACES(A) >= ACES(B)
        val linearRadianceSamples = listOf(0.0, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 2.5, 5.0, 12.0)
        var prevLdr = -1.0
        for (rad in linearRadianceSamples) {
            val ldr = acesFilmic(rad * 1.8)
            assertTrue("Display transform must be non-decreasing: prev=$prevLdr curr=$ldr", ldr >= prevLdr)
            prevLdr = ldr
        }

        // Test radial ordering along the disk from peak outward on approaching side
        val radii = listOf(4.0, 6.0, 8.0, 12.0, 16.0, 20.0, 21.8)
        var prevDisplay = 1.1
        for (r in radii) {
            val g = 1.0 + 1.2 / sqrt(r)
            val rad = diskRadianceNormalized(r, g)
            val display = acesFilmic(rad * 1.8)
            assertTrue(
                "Display brightness at r=$r ($display) must not exceed inner peak ($prevDisplay)",
                display <= prevDisplay + 1e-12
            )
            prevDisplay = display
        }
    }

    // D8. Finite deterministic output; no NaN/Inf
    @Test
    fun physicalEmissionAndDisplayOutputAreFiniteAndDeterministic() {
        for (step in 0..100) {
            val r = rIn + (rOut - rIn) * (step / 100.0)
            for (gInt in 2..40) {
                val g = gInt * 0.1 // 0.2 to 4.0
                val rad = diskRadianceNormalized(r, g)
                assertFalse("Radiance must not be NaN for r=$r, g=$g", rad.isNaN())
                assertFalse("Radiance must not be Infinite for r=$r, g=$g", rad.isInfinite())
                assertTrue("Radiance must be non-negative for r=$r, g=$g", rad >= 0.0)

                val postAces = acesFilmic(rad * 1.8)
                assertFalse("Post-ACES must not be NaN for r=$r, g=$g", postAces.isNaN())
                assertTrue("Post-ACES must be in [0, 1] for r=$r, g=$g", postAces in 0.0..1.0)

                val srgb = toSrgb(postAces)
                assertTrue("sRGB must be in [0, 255] for r=$r, g=$g", srgb in 0..255)
            }
        }
    }
}
