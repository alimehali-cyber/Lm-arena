package com.zig.gravity

import com.zig.gravity.sim.Preset
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Structural guards for the sandbox's bottom sheets.
 *
 * These are **source-level lint checks, not behavioural tests**. This project has no Compose UI
 * test harness and no Robolectric, so there is no way from the JVM to lay a sheet out and discover
 * that its last row is off-screen. What can be checked is the property whose absence caused the
 * bug: a sheet whose content is taller than the screen must sit in a scroll container, and a plain
 * `Column` is not one.
 *
 * The bug this replaces was real and shipped: [Preset] grew from nine entries to fifteen, and
 * `PresetSheet` rendered them in a bare `Column`, so the last six scenes could not be reached or
 * tapped at all.
 */
class GravitySheetScrollTest {

    /** Every composable in the sandbox that opens a `ModalBottomSheet`. */
    private val sheetFiles = listOf(
        "PresetSheet.kt",
        "AddBodySheet.kt",
        "InspectorSheet.kt",
        "TeachingCard.kt"
    )

    private fun uiDir(): File {
        // Gradle runs unit tests with the module directory as the working directory, but walk up
        // anyway so the test does not depend on that detail.
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "app/src/main/java/com/zig/gravity/ui")
            if (candidate.isDirectory) return candidate
            val direct = File(dir, "src/main/java/com/zig/gravity/ui")
            if (direct.isDirectory) return direct
            dir = dir.parentFile
        }
        throw AssertionError("could not locate the gravity ui source directory")
    }

    private fun source(name: String): String {
        val f = File(uiDir(), name)
        assertTrue("missing source file $name", f.isFile)
        return f.readText()
    }

    @Test
    fun everyBottomSheetContentIsInAScrollContainer() {
        for (name in sheetFiles) {
            val text = source(name)
            assertTrue(
                "$name opens a ModalBottomSheet but never scrolls its content. A sheet taller " +
                    "than the screen silently clips the rows below the fold, and they cannot be " +
                    "tapped. Wrap the content in verticalScroll(rememberScrollState()) or a lazy list.",
                text.contains("verticalScroll(") || text.contains("LazyColumn(")
            )
        }
    }

    @Test
    fun theSceneListItselfScrolls() {
        // Specifically the sheet that was broken: the scroll modifier must be on the container that
        // actually holds the preset rows, not merely present somewhere in the file.
        val text = source("PresetSheet.kt")
        val listStart = text.indexOf("for (p in Preset.entries)")
        assertTrue("the preset loop moved; update this guard", listStart > 0)
        val containerStart = text.lastIndexOf("Column(", listStart)
        assertTrue("no Column wraps the preset rows", containerStart > 0)
        val container = text.substring(containerStart, listStart)
        assertTrue(
            "the Column that holds the preset rows is not scrollable",
            container.contains("verticalScroll(")
        )
    }

    @Test
    fun theSceneListIsLongEnoughToNeedScrolling() {
        // Documents why the guard above exists. Fifteen rows at roughly 60dp, plus a header, is
        // taller than a small phone's sheet; the count is what turned a latent bug into a visible
        // one. If the catalogue is ever trimmed the guard should stay anyway.
        assertTrue(
            "the preset catalogue is ${Preset.entries.size} entries",
            Preset.entries.size >= 12
        )
    }

    @Test
    fun scrollableSheetsKeepTheirBottomInsetInsideTheScrollableArea() {
        // navigationBarsPadding() applied *before* verticalScroll would sit outside the scrolling
        // viewport, so the final row would still be trapped under the system navigation bar. It
        // must come after, where it scrolls with the content.
        for (name in listOf("PresetSheet.kt", "AddBodySheet.kt")) {
            val text = source(name)
            val scroll = text.indexOf(".verticalScroll(")
            val inset = text.indexOf(".navigationBarsPadding()", scroll)
            assertTrue("$name: expected a scroll modifier", scroll > 0)
            assertTrue(
                "$name: navigationBarsPadding() must be applied after verticalScroll() so the " +
                    "last row can scroll clear of the navigation bar",
                inset > scroll
            )
        }
    }

    @Test
    fun everySceneInTheCatalogueRendersARowInTheSheet() {
        // The sheet enumerates Preset.entries rather than listing scenes by hand, so a new preset
        // can never be added to the engine and forgotten in the UI.
        val text = source("PresetSheet.kt")
        assertTrue(
            "PresetSheet must enumerate Preset.entries rather than hard-coding scenes",
            text.contains("for (p in Preset.entries)")
        )
        assertEquals(
            "every preset must be reachable from the sheet",
            Preset.entries.size,
            Preset.entries.distinctBy { it.name }.size
        )
    }
}
