package com.zig.chal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Structural guards for the Chal feature.
 *
 * These are source-level checks for the same reason the rest of the project uses them: there is no
 * Compose or GL test harness on the JVM, so what can be proved is that Chal is wired into the Lab
 * screen, that the GPU pipeline keeps the reference engine's shader contract, and — most
 * importantly — that Chal shares nothing with the Gargantua feature.
 */
class ChalIntegrationGuardTest {

    @Test
    fun chalIsWiredIntoTheLabScreen() {
        val screen = readMain("java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt")

        val start = screen.indexOf("    CHAL(")
        assertTrue("the CHAL entry must be declared in LabFeatureType", start > 0)
        val end = screen.indexOf("\n    )", start)
        assertTrue("the CHAL entry does not end where this guard expects", end > start)
        val block = screen.substring(start, end)

        assertTrue("the Chal card must carry its English title", block.contains("titleEn = \"Chal\""))
        assertTrue("the Chal card must carry its Persian title", block.contains("titleFa = \"چال\""))
        assertTrue("the Chal card must be available", block.contains("isAvailable = true"))

        assertTrue(
            "the Lab screen must route CHAL to the Chal root",
            screen.contains("LabFeatureType.CHAL") && screen.contains("com.zig.chal.ui.ChalRoot")
        )
    }

    @Test
    fun chalSharesNothingWithGargantua() {
        val chalSources = chalSourceFiles()
        assertTrue("expected the Chal package to exist", chalSources.size >= 10)

        for (file in chalSources) {
            // Comments are stripped first: the port deliberately keeps the reference engine's own
            // prose (its dive sequence is literally titled after the film's black hole), and that is
            // documentation, not a dependency. What must never appear is code coupling.
            val code = stripComments(file.readText())
            assertFalse(
                "Chal must not reference the Gargantua feature in code: ${file.name}",
                code.contains("gargantua", ignoreCase = true)
            )
            assertFalse(
                "Chal must not import any other renderer: ${file.name}",
                code.contains("com.zig.gargantua") ||
                    code.contains("com.zig.gravity.renderer") ||
                    code.contains("GargantuaRenderer") ||
                    code.contains("GargantuaSurfaceView")
            )
        }

        // The Chal branch in the Lab screen must not have picked up a Gargantua dependency either.
        val screen = readMain("java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt")
        val chalStart = screen.indexOf("selectedFeature == LabFeatureType.CHAL")
        assertTrue("the CHAL routing branch must exist", chalStart > 0)
        val chalEnd = screen.indexOf("} else {", chalStart)
        val chalBranch = screen.substring(chalStart, if (chalEnd > chalStart) chalEnd else screen.length)
        assertFalse(
            "the CHAL branch must not touch Gargantua",
            chalBranch.contains("gargantua", ignoreCase = true)
        )
    }

    @Test
    fun theShaderPortKeepsTheReferencePipeline() {
        val shader = readMain("java/com/zig/chal/shader/ChalShaderSource.kt")
        val shaderManager = readMain("java/com/zig/chal/shader/ChalShaderManager.kt")
        val features = readMain("java/com/zig/chal/config/ChalFeatures.kt")

        // GLSL ES 3.00 entry point and colour output, exactly as the reference emits it.
        assertTrue(shader.contains("#version 300 es"))
        assertTrue(shader.contains("precision highp float"))
        assertTrue(shader.contains("fragColor"))

        // Kerr closed forms ported from the reference's metric chunk.
        for (symbol in listOf(
            "kerr_horizon",
            "kerr_photon_sphere",
            "kerr_isco",
            "kerr_r",
            "kerr_geodesic_accel",
            "kerr_ergosphere",
            "kerr_shadow_radius",
            "sample_accretion_disk"
        )) {
            assertTrue("the shader port lost the reference symbol '$symbol'", shader.contains(symbol))
        }

        // Uniform contract from the reference fragment shader.
        for (uniform in listOf(
            "u_resolution",
            "u_time",
            "u_mass",
            "u_spin",
            "u_disk_density",
            "u_disk_temp",
            "u_mouse",
            "u_zoom",
            "u_lensing_strength",
            "u_frame_dragging_strength",
            "u_disk_size",
            "u_disk_scale_height",
            "u_maxRaySteps",
            "u_noiseTex",
            "u_blueNoiseTex",
            "u_debug",
            "u_show_redshift",
            "u_show_kerr_shadow",
            "u_shadowShift",
            "u_shadowCurve",
            "u_shadowCount",
            "u_camPos",
            "u_camQuat"
        )) {
            assertTrue("the shader port lost the uniform '$uniform'", shader.contains(uniform))
        }

        // The quality tiers the shader branches on.
        for (quality in listOf(
            "RAY_QUALITY_OFF",
            "RAY_QUALITY_LOW",
            "RAY_QUALITY_MEDIUM",
            "RAY_QUALITY_HIGH",
            "RAY_QUALITY_ULTRA"
        )) {
            assertTrue("the quality tier '$quality' was lost", features.contains(quality))
        }
        assertTrue(
            "the manager must inject the active quality tier",
            shaderManager.contains("shaderDefine")
        )

        // Feature and quality macros used for shader recompilation.
        for (define in listOf(
            "ENABLE_LENSING",
            "ENABLE_DISK",
            "ENABLE_DOPPLER",
            "ENABLE_STARS",
            "ENABLE_PHOTON_GLOW",
            "ENABLE_BLOOM",
            "ENABLE_JETS",
            "ENABLE_REDSHIFT",
            "ENABLE_SHADOW_GUIDE",
            "ENABLE_LINEAR_OUTPUT"
        )) {
            assertTrue("the manager lost the define '$define'", shaderManager.contains(define))
        }
    }

    @Test
    fun theRendererBindsTheSamplerUniformsAsIntegers() {
        val renderer = readMain("java/com/zig/chal/render/ChalRenderer.kt")
        // Sampler uniforms set with uniform1f silently sample texture unit 0 on some drivers; the
        // reference is explicit about the integer path and so is the port.
        assertTrue(renderer.contains("u_noiseTex"))
        assertTrue(renderer.contains("u_blueNoiseTex"))
        assertTrue(
            "sampler uniforms must be set with the integer uploader",
            renderer.contains("glUniform1i")
        )
    }

    // ---------------------------------------------------------------------------------------

    /** Removes line and block comments so the independence guard only ever sees code. */
    private fun stripComments(source: String): String {
        val withoutBlocks = Regex("/\\*[\\s\\S]*?\\*/").replace(source, " ")
        return withoutBlocks.lineSequence()
            .filterNot { it.trimStart().startsWith("//") || it.trimStart().startsWith("*") }
            .joinToString("\n")
    }

    private fun chalSourceFiles(): List<File> =
        File(mainDir(), "java/com/zig/chal").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun readMain(relative: String): String {
        val f = File(mainDir(), relative)
        assertTrue("missing source file $relative", f.isFile)
        return f.readText()
    }

    private fun mainDir(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "app/src/main")
            if (File(candidate, "java/com/alijafari/red/astronomy").isDirectory) return candidate
            val direct = File(dir, "src/main")
            if (File(direct, "java/com/alijafari/red/astronomy").isDirectory) return direct
            dir = dir.parentFile
        }
        throw AssertionError("could not locate the app main source set")
    }
}
