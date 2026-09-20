package com.zig.gargantua

import com.zig.gargantua.renderer.ShaderSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates Gargantua canonical shader assets and roadmap lock constraints.
 * Ensures the M1 GPU health test contains NO Kerr physics or black hole approximations.
 */
class GargantuaShaderSourceTest {

    @Test
    fun canonicalShaderAssetPathsAreConfigured() {
        assertEquals("shaders/gargantua_test.vert", ShaderSource.VERTEX_SHADER_ASSET_PATH)
        assertEquals("shaders/gargantua_test.frag", ShaderSource.FRAGMENT_SHADER_ASSET_PATH)
        assertEquals("shaders/gargantua_geodesic.frag", ShaderSource.GEODESIC_FRAGMENT_SHADER_ASSET_PATH)
        assertEquals("shaders/gargantua_blit.frag", ShaderSource.BLIT_FRAGMENT_SHADER_ASSET_PATH)
        assertEquals("shaders/gargantua_brightpass.frag", ShaderSource.BRIGHTPASS_FRAGMENT_SHADER_ASSET_PATH)
        assertEquals("shaders/gargantua_blur.frag", ShaderSource.BLUR_FRAGMENT_SHADER_ASSET_PATH)
        assertEquals("shaders/gargantua_composite.frag", ShaderSource.COMPOSITE_FRAGMENT_SHADER_ASSET_PATH)
        assertEquals("shaders/gargantua_reduce.frag", ShaderSource.REDUCE_FRAGMENT_SHADER_ASSET_PATH)
    }

    @Test
    fun workloadSourceFlagOffMatchesCurrentGeneratedSourceByteForByte() {
        val asset = findAssetFile(ShaderSource.GEODESIC_FRAGMENT_SHADER_ASSET_PATH).readText()
        val expected = asset
            .replaceFirst(
                "out vec4 fragColor;",
                "layout(location = 0) out vec4 fragColor;"
            )
            .replaceFirst(
                "#version 300 es",
                "#version 300 es\n#define GARGANTUA_WORKLOAD_TELEMETRY 1"
            )
        val actual = ShaderSource.buildWorkloadTelemetryGeodesicFragmentShader(
            asset,
            includeSemanticCache = false
        )

        assertEquals(expected, actual)
        assertFalse(actual.contains("#define GARGANTUA_WORKLOAD_SEMANTIC_CACHE"))
    }

    @Test
    fun workloadSourceFlagOnPlacesSemanticDefineAfterVersion() {
        val asset = findAssetFile(ShaderSource.GEODESIC_FRAGMENT_SHADER_ASSET_PATH).readText()
        val actual = ShaderSource.buildWorkloadTelemetryGeodesicFragmentShader(
            asset,
            includeSemanticCache = true
        )
        val lines = actual.lines()

        assertEquals("#version 300 es", lines[0])
        assertEquals("#define GARGANTUA_WORKLOAD_TELEMETRY 1", lines[1])
        assertEquals("#define GARGANTUA_WORKLOAD_SEMANTIC_CACHE 1", lines[2])
    }

    @Test
    fun geodesicAssetHasBalancedIfdefBlocks() {
        val lines = findAssetFile(ShaderSource.GEODESIC_FRAGMENT_SHADER_ASSET_PATH).readLines()
        var depth = 0
        for (line in lines) {
            when {
                line.trim().startsWith("#ifdef ") -> depth++
                line.trim() == "#endif" -> {
                    assertTrue("#endif must match a preceding #ifdef", depth > 0)
                    depth--
                }
            }
        }
        assertEquals("Every #ifdef must have a matching #endif", 0, depth)
    }

    @Test
    fun canonicalM6ShadersExistAndDeclareGles3() {
        val brightPass = findAssetFile(ShaderSource.BRIGHTPASS_FRAGMENT_SHADER_ASSET_PATH).readText()
        val blur = findAssetFile(ShaderSource.BLUR_FRAGMENT_SHADER_ASSET_PATH).readText()
        val composite = findAssetFile(ShaderSource.COMPOSITE_FRAGMENT_SHADER_ASSET_PATH).readText()

        assertTrue(brightPass.contains("#version 300 es"))
        assertTrue(blur.contains("#version 300 es"))
        assertTrue(composite.contains("#version 300 es"))
        assertTrue(composite.contains("aces_filmic"))
    }

    @Test
    fun canonicalVertexShaderDeclaresExpectedGles3Layout() {
        val vertFile = findAssetFile(ShaderSource.VERTEX_SHADER_ASSET_PATH)
        assertTrue("Canonical vertex shader asset must exist", vertFile.isFile)

        val vert = vertFile.readText()
        assertTrue("Must declare #version 300 es", vert.contains("#version 300 es"))
        assertTrue("Must declare a_Position attribute", vert.contains("a_Position"))
        assertTrue("Must declare a_TexCoord attribute", vert.contains("a_TexCoord"))
        assertTrue("Must compute gl_Position", vert.contains("gl_Position"))
    }

    @Test
    fun canonicalFragmentShaderDeclaresExpectedUniformsAndOutputs() {
        val fragFile = findAssetFile(ShaderSource.FRAGMENT_SHADER_ASSET_PATH)
        assertTrue("Canonical fragment shader asset must exist", fragFile.isFile)

        val frag = fragFile.readText()
        assertTrue("Must declare #version 300 es", frag.contains("#version 300 es"))
        assertTrue("Must declare u_Resolution uniform", frag.contains("uniform vec2 u_Resolution;"))
        assertTrue("Must declare u_Time uniform", frag.contains("uniform float u_Time;"))
        assertTrue("Must declare output fragColor", frag.contains("out vec4 fragColor;"))
    }

    @Test
    fun m1CanonicalShaderDoesNotContainAnyKerrOrGeodesicPhysics() {
        // Strict M1 non-goal check: ensure no black hole math was prematurely added
        val bannedKeywords = listOf(
            "kerr", "schwarzschild", "geodesic", "isco", "ergosphere",
            "eventhorizon", "event_horizon", "christoffel", "rungekutta",
            "accretion", "redshift", "beaming", "lorentz", "\\bmetric\\b"
        )

        val vertText = findAssetFile(ShaderSource.VERTEX_SHADER_ASSET_PATH).readText()
        val fragText = findAssetFile(ShaderSource.FRAGMENT_SHADER_ASSET_PATH).readText()
        val allShaderText = (vertText + fragText).lowercase()

        for (banned in bannedKeywords) {
            val found = if (banned.startsWith("\\b")) {
                Regex(banned).containsMatchIn(allShaderText)
            } else {
                allShaderText.contains(banned)
            }
            assertFalse(
                "M1 canonical shader must not contain '$banned' (reserved for M3-M6)",
                found
            )
        }
    }

    private fun findAssetFile(relativePath: String): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "app/src/main/assets/$relativePath")
            if (candidate.isFile) return candidate
            val direct = File(dir, "src/main/assets/$relativePath")
            if (direct.isFile) return direct
            dir = dir.parentFile
        }
        return File("app/src/main/assets/$relativePath")
    }
}
