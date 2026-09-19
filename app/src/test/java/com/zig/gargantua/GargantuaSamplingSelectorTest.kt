package com.zig.gargantua

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GargantuaSamplingSelectorTest {
    @Test
    fun selectorIsInMainSourceSetAndUsesAllExistingModes() {
        val mainFile = findSourceFile("app/src/main/java/com/zig/gargantua/ui/GargantuaSamplingSelector.kt")
        val debugFile = findSourceFile("app/src/debug/java/com/zig/gargantua/ui/GargantuaDebugSamplingOverlay.kt")
        val releaseFile = findSourceFile("app/src/release/java/com/zig/gargantua/ui/GargantuaDebugSamplingOverlay.kt")
        val rootFile = findSourceFile("app/src/main/java/com/zig/gargantua/ui/GargantuaRoot.kt")

        assertTrue("Selector must be compiled from the main source set", mainFile.isFile)
        assertFalse("The old DEBUG-only selector must not be present", debugFile.isFile)
        assertFalse("The old release no-op must not replace the selector", releaseFile.isFile)

        val selectorSource = mainFile.readText()
        val rootSource = rootFile.readText()
        assertTrue(selectorSource.contains("GargantuaSamplingSelector"))
        assertTrue(selectorSource.contains("BASELINE_BLOCK_SIZE"))
        assertTrue(selectorSource.contains("COARSE_2X2_BLOCK_SIZE"))
        assertTrue(selectorSource.contains("COARSE_3X3_BLOCK_SIZE"))
        assertTrue(selectorSource.contains("COARSE_4X4_BLOCK_SIZE"))
        assertTrue(selectorSource.contains("COARSE_6X6_BLOCK_SIZE"))
        assertTrue(selectorSource.contains("COARSE_8X8_BLOCK_SIZE"))
        assertTrue(selectorSource.contains("debugCoarseSamplingBlockSize = mode"))
        assertFalse("AUTO must not remain in the user-facing selector", selectorSource.contains("AUTO"))
        assertTrue(selectorSource.contains("gargantua_sampling_mode_\$mode"))
        assertFalse("The compact selector must not expose workload settings", selectorSource.contains("enableWorkloadTelemetry"))

        assertFalse(findSourceFile("app/src/main/java/com/zig/gargantua/renderer/GargantuaAdaptiveSampling.kt").isFile)
        assertFalse(findSourceFile("app/src/main/assets/shaders/gargantua_adaptive_mask.frag").isFile)
        assertFalse(findSourceFile("app/src/main/assets/shaders/gargantua_adaptive_overlay.frag").isFile)
        val productionSources = listOf(
            findSourceFile("app/src/main/java/com/zig/gargantua/renderer/GargantuaRenderer.kt").readText(),
            findSourceFile("app/src/main/java/com/zig/gargantua/renderer/GargantuaRenderState.kt").readText(),
            findSourceFile("app/src/main/java/com/zig/gargantua/renderer/GargantuaCoarseSampling.kt").readText(),
            findSourceFile("app/src/main/java/com/zig/gargantua/renderer/ShaderSource.kt").readText()
        ).joinToString("\\n")
        assertFalse(productionSources.contains("AUTO_SAMPLING_MODE"))
        assertFalse(productionSources.contains("adaptiveMask"))
        assertFalse(productionSources.contains("u_Adaptive"))

        assertTrue(rootSource.contains("GargantuaSamplingSelector("))
        assertTrue(rootSource.contains(".align(Alignment.TopEnd)"))
        assertTrue(rootSource.contains(".padding(top = 82.dp, end = 12.dp)"))
    }

    private fun findSourceFile(path: String): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, path)
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        return File(path)
    }
}
