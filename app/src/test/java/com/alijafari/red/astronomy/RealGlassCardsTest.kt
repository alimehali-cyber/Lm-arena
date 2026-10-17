package com.alijafari.red.astronomy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Structural guards for the real-liquid-glass cards.
 *
 * Source-level, like the rest of the UI invariants in this project: there is no Compose test harness
 * on the JVM, so what can be proved is the wiring — the Lab tiles, the Home moon card, and the
 * tonight and eclipse cards sample the BACKGROUND capture layer (`glassBackdrop`) as real liquid
 * glass, and never the root `backdrop` whose layer they live inside (the Kyant loop that crashes the
 * render thread).
 */
class RealGlassCardsTest {

    @Test
    fun mainActivityMountsABackgroundCaptureLayerAndHandsItToTheScreens() {
        val main = readMain("java/com/alijafari/red/astronomy/MainActivity.kt")

        // The app background (theme color, Live Sky, photo) is its own capture layer...
        assertTrue(
            "MainActivity must mount the background capture layer",
            main.contains("Modifier.layerBackdrop(glassBackdrop)")
        )
        assertTrue(
            "the background layer must paint the theme background so glass always has pixels to refract",
            main.contains(".background(com.alijafari.red.astronomy.ui.theme.RedTheme.colors.background)")
        )
        // ...and it is handed to the two screens with glass cards, explicitly.
        assertTrue(
            "LabScreen must receive the background capture layer",
            main.contains("LabScreen(") && main.contains("glassBackdrop = glassBackdrop")
        )
        assertTrue(
            "HomeScreen must receive the background capture layer",
            main.contains("HomeScreen(") && main.contains("glassBackdrop = glassBackdrop")
        )
    }

    @Test
    fun theLabTilesAreRealGlass() {
        val lab = readMain("java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt")

        assertTrue("LabScreen must accept the background capture layer", lab.contains("glassBackdrop: com.kyant.backdrop.Backdrop?"))
        assertTrue(
            "every Lab tile must sample the background capture layer as real liquid glass",
            lab.contains("backdrop = glassBackdrop")
        )
    }

    @Test
    fun theMoonTonightAndEclipseCardsAreRealGlass() {
        val home = readMain("java/com/alijafari/red/astronomy/ui/screens/HomeScreen.kt")

        assertTrue("HomeScreen must accept the background capture layer", home.contains("glassBackdrop: com.kyant.backdrop.Backdrop?"))
        // Tonight hero, Moon card surface and the shared solar/lunar eclipse tile.
        val forwards = Regex("""backdrop = glassBackdrop""").findAll(home).count()
        assertTrue("the tonight, moon and eclipse tiles must all sample the glass layer (found $forwards)", forwards >= 3)
    }

    @Test
    fun inScreenCardsNeverSampleTheLayerThatCapturesThem() {
        // The screens live inside the root `backdrop`'s capture layer: drawing that layer from
        // inside it is the fatal Kyant loop. They capture nothing themselves and forward only
        // the background layer.
        for (path in listOf(
            "java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt",
            "java/com/alijafari/red/astronomy/ui/screens/HomeScreen.kt"
        )) {
            val source = readMain(path)
            assertFalse("$path must not run layerBackdrop", source.contains("layerBackdrop("))
            assertFalse("$path must not forward the root backdrop", source.contains("backdrop = backdrop"))
        }
    }

    // -----------------------------------------------------------------------------------------

    private fun readMain(relative: String): String {
        val f = File(mainDir(), relative)
        assertTrue("missing source file $relative", f.isFile)
        return f.readText()
    }

    private fun mainDir(): File {
        // Gradle runs unit tests with the module directory as the working directory, but walk up so
        // the guard does not depend on that detail.
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
