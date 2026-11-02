package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.SpatialPalette
import com.alijafari.red.astronomy.domain.UiStyle
import com.alijafari.red.astronomy.ui.theme.SpatialGlassPalettes
import com.alijafari.red.astronomy.ui.theme.SpatialGlassSpec
import com.alijafari.red.astronomy.ui.theme.spatialGlassColorTokens
import com.alijafari.red.astronomy.ui.theme.spatialGlassSpecFor
import com.alijafari.red.astronomy.ui.theme.spatialSurfaceInk
import com.alijafari.red.astronomy.ui.theme.wcagContrast
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Spatial Glass interface: what it promises, and the shape of the code that has to keep promising it.
 *
 * Like Part 6's guards, none of this can be asserted from a screenshot — the app is Compose over a GL
 * landscape — so the file is split in two:
 *
 *  * the *arithmetic*: the palettes are what they claim to be (wine is wine, glass is translucent, the
 *    ink on a glass panel is light and legible), which is pure Kotlin and needs no device;
 *  * the *wiring*: the style is a preference rather than a theme, the default interface still draws
 *    exactly one opaque surface, and the optional one reaches every panel through that same surface.
 */
class SpatialGlassInterfaceTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private fun res(relative: String): File {
        val candidates = listOf("src/main/res/$relative", "app/src/main/res/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }
            ?: error("Resource not found: $relative")
    }

    private val aurora = spatialGlassSpecFor(UiStyle.SPATIAL, SpatialPalette.AURORA)
    private val wine = spatialGlassSpecFor(UiStyle.SPATIAL, SpatialPalette.WINE)
    private val classic = spatialGlassSpecFor(UiStyle.CLASSIC, SpatialPalette.WINE)

    // ---------------------------------------------------------------------------------------------
    // The interface is a preference, not a theme
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theDefaultInterfaceIsStillTheDefault() {
        assertEquals("the app ships on its default interface", UiStyle.CLASSIC, UiStyle.DEFAULT)
        assertEquals("and on the first palette", SpatialPalette.AURORA, SpatialPalette.DEFAULT)
        assertFalse("CLASSIC never draws glass", classic.enabled)
        assertEquals(
            "even when a palette is remembered, CLASSIC resolves to 'nothing'",
            SpatialGlassSpec.Disabled,
            classic
        )
        assertTrue("the palette itself still exists", wine.enabled)
    }

    @Test
    fun theChoiceIsPersistedAsItsOwnPreference() {
        val viewModel = source("com/alijafari/red/astronomy/ui/MainViewModel.kt")
        assertTrue("the style has a key of its own", viewModel.contains("\"ui_style\""))
        assertTrue("so does the palette", viewModel.contains("\"ambient_palette\""))
        assertTrue("and a setter for each", viewModel.contains("fun setUiStyle("))
        assertTrue(viewModel.contains("fun setAmbientPalette("))
        assertTrue("an unreadable value falls back to the default", viewModel.contains("UiStyle.DEFAULT"))
        assertTrue("the theme mode is untouched by all of this", viewModel.contains("fun setThemeMode("))
    }

    @Test
    fun theTwoInterfacesShareOneSurface() {
        val surface = source("com/alijafari/red/astronomy/ui/theme/Surface.kt")
        assertTrue("the flat fill is still the first thing painted", surface.contains(".background(color, shape)"))
        assertTrue("the flat hairline is still there", surface.contains("Modifier.border(border, shape)"))
        assertTrue("so is the clip", surface.contains(".clip(shape)"))
        assertTrue("the glass material is applied by the same surface", surface.contains(".spatialGlassMaterial("))
        assertTrue("the ink is resolved glass-aware", surface.contains("spatialSurfaceInk("))
        assertTrue("and the flat border is only skipped while glass is on", surface.contains("!glass.enabled"))
    }

    @Test
    fun theThemeOffersBothInterfaces() {
        val theme = source("com/alijafari/red/astronomy/ui/theme/Theme.kt")
        assertTrue("the theme takes the style", theme.contains("uiStyle: UiStyle = UiStyle.DEFAULT"))
        assertTrue("and the palette", theme.contains("ambientPalette: SpatialPalette = SpatialPalette.DEFAULT"))
        assertTrue("it publishes the spec", theme.contains("LocalSpatialGlass provides spatial"))
        assertTrue("it publishes the readout face", theme.contains("LocalStatFontFamily provides statFontFamily"))
        assertTrue(
            "and the palette's own tokens replace the flat ones",
            theme.contains("spatialGlassColorTokens(spatial)")
        )
        assertTrue(
            "the spatial palette is not put through the opaque-surface repair",
            theme.contains("if (spatial.enabled) redColorTokens else withReadableInk(redColorTokens)")
        )
        assertTrue("the default interface's branches are intact", theme.contains("ThemeMode.LIGHT -> LightSpaceColorScheme"))
    }

    @Test
    fun theAmbientWorldIsBehindTheScreens() {
        val activity = source("com/alijafari/red/astronomy/MainActivity.kt")
        assertTrue("the activity takes the style", activity.contains("uiStyle = uiState.uiStyle"))
        assertTrue("and the palette", activity.contains("ambientPalette = uiState.ambientPalette"))
        assertTrue("the ambient world is drawn behind everything", activity.contains("SpatialAmbientBackdrop("))
        assertFalse(
            "and it samples nothing — no capture, no render effect, no backdrop layer",
            activity.contains("RenderEffect") || activity.contains("captureToImage") || activity.contains("layerBackdrop")
        )
    }

    @Test
    fun settingsCanChooseTheInterfaceAndReturnToTheDefault() {
        val settings = source("com/alijafari/red/astronomy/ui/components/SettingsDialog.kt")
        assertTrue("there is a section for it", settings.contains("settings_section_interface"))
        assertTrue("it turns the new interface on", settings.contains("viewModel.setUiStyle(UiStyle.SPATIAL)"))
        assertTrue("and back to the default one", settings.contains("viewModel.setUiStyle(UiStyle.CLASSIC)"))
        assertTrue("both palettes are offered", settings.contains("SpatialPalette.AURORA"))
        assertTrue(settings.contains("SpatialPalette.WINE"))
        assertTrue("with a preview built from the real material", settings.contains("settings_interface_preview"))
        assertTrue("and the app theme picker is untouched next to it", settings.contains("viewModel.setThemeMode("))
    }

    // ---------------------------------------------------------------------------------------------
    // The arithmetic: translucent glass, legible ink, and a wine that is actually wine
    // ---------------------------------------------------------------------------------------------

    @Test
    fun glassIsTranslucentNotPainted() {
        SpatialGlassPalettes.forEach { spec ->
            assertTrue("${spec.nameEn}: the sheet lets the world through", spec.glassFill.alpha in 0.02f..0.30f)
            assertTrue("${spec.nameEn}: so does the denser one", spec.glassFillStrong.alpha in 0.02f..0.40f)
            assertTrue(
                "${spec.nameEn}: the edge is a light, not a dark frame",
                inked(spec.edge) > 0.8f
            )
            assertTrue("${spec.nameEn}: the ambient world is dark", inked(spec.ambientBase) < 0.20f)
            assertTrue("${spec.nameEn}: it sinks darker still at the bottom", inked(spec.ambientBaseBottom) < inked(spec.ambientBase))
            assertTrue("${spec.nameEn}: there is a mesh to refract", spec.blobs.size >= 4)
            spec.blobs.forEach { blob ->
                assertTrue("${spec.nameEn}: every blob is a soft wide field", blob.radius >= 0.4f)
                assertTrue("${spec.nameEn}: and it drifts", blob.driftX + blob.driftY > 0f)
            }
        }
    }

    @Test
    fun theInkOnAGlassPanelIsLightAndReadable() {
        SpatialGlassPalettes.forEach { spec ->
            val ink = spatialSurfaceInk(spec, spec.glassFill)
            val seen = over(spec.ambientBase, spec.glassFill)
            assertTrue("${spec.nameEn}: glass takes light ink, not black", inked(ink.primary) > 0.7f)
            assertTrue(
                "${spec.nameEn}: and it clears 4.5:1 on what the eye sees (${wcagContrast(ink.primary, seen)})",
                wcagContrast(ink.primary, seen) >= 4.5f
            )
            assertTrue("${spec.nameEn}: the secondary step stays readable", inked(ink.secondary) > 0.5f)
        }
    }

    @Test
    fun thePaletteColoursTheWholeTokenSetNotJustTheGlass() {
        SpatialGlassPalettes.forEach { spec ->
            val tokens = spatialGlassColorTokens(spec)
            assertTrue("${spec.nameEn}: surfaces are the glass sheets", tokens.surfaceElevated == spec.glassFill)
            assertTrue("${spec.nameEn}: the background is the ambient base", tokens.background == spec.ambientBase)
            assertTrue("${spec.nameEn}: the accent is the palette's", tokens.accentRed == spec.accent)
            assertTrue("${spec.nameEn}: and the ink is light", inked(tokens.textPrimary) > 0.7f)
            assertTrue("${spec.nameEn}: the nav bar is glass too", tokens.navSurface == spec.glassFillStrong)
        }
        assertNotEquals("the two palettes are not the same palette", aurora.accent, wine.accent)
    }

    @Test
    fun theWinePaletteIsWineAndBurgundy() {
        assertTrue("wine is cold-dark in the red end of the spectrum", wine.ambientBase.red > wine.ambientBase.green)
        assertTrue(wine.ambientBase.red > wine.ambientBase.blue)
        assertTrue("and it is a deep base, not a bright red", inked(wine.ambientBase) < 0.12f)

        // Every light in the wine world is a red or a gold: red leads, so the whole world reads as one
        // family of wine rather than as the aurora recoloured.
        wine.blobs.forEach { blob ->
            assertTrue(
                "blob ${blob.color} is red-led",
                blob.color.red >= blob.color.green && blob.color.red >= blob.color.blue
            )
            assertTrue("and warm, not cyan", blob.color.blue <= blob.color.green + 0.35f)
        }

        // The aurora world, by contrast, is genuinely polychromatic — this is what makes the two
        // palettes two worlds instead of two shades of one.
        assertTrue(
            "aurora has a cool light in it",
            aurora.blobs.any { it.color.blue > it.color.red }
        )
        assertTrue("wine has no cool light in it", wine.blobs.none { it.color.blue > it.color.red })
        assertTrue("and its accent is a wine red, not a coral", wine.accent.red > wine.accent.green + 0.3f)
    }

    // ---------------------------------------------------------------------------------------------
    // The dot-matrix half of the style
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theDotMatrixFaceIsBundledAndOnlyUsedWhereItCanRender() {
        val font = res("font/doto_dot_matrix.ttf")
        assertTrue("the face is bundled", font.exists() && font.length() > 10_000)
        val header = font.inputStream().use { stream -> ByteArray(4).also { stream.read(it) } }
        val tag = String(header)
        assertTrue(
            "and it is a real sfnt font ($tag)",
            tag == "\u0000\u0001\u0000\u0000" || tag == "OTTO" || tag == "true" || tag == "ttcf"
        )

        val type = source("com/alijafari/red/astronomy/ui/theme/Type.kt")
        assertTrue("declared once", type.contains("val DotoFontFamily = FontFamily("))
        assertTrue("from the bundled file", type.contains("R.font.doto_dot_matrix"))
        assertTrue("behind a local", type.contains("val LocalStatFontFamily"))
        assertTrue("used by the readout token", type.contains("val statNumber: TextStyle"))

        val theme = source("com/alijafari/red/astronomy/ui/theme/Theme.kt")
        assertTrue(
            "and only offered to Latin, and only in glass",
            theme.contains("if (spatial.enabled && !isPersian) DotoFontFamily else appFontFamily")
        )
        assertTrue(
            "a licence is shipped with it",
            listOf(File("THIRD_PARTY_LICENSES/Doto-OFL.txt"), File("../THIRD_PARTY_LICENSES/Doto-OFL.txt"))
                .any { it.exists() }
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers, matching the ink arithmetic the app itself uses
    // ---------------------------------------------------------------------------------------------

    private fun inked(color: Color): Float = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue

    private fun over(base: Color, overlay: Color): Color {
        val a = overlay.alpha
        return Color(
            red = base.red + (overlay.red - base.red) * a,
            green = base.green + (overlay.green - base.green) * a,
            blue = base.blue + (overlay.blue - base.blue) * a,
            alpha = 1f
        )
    }
}
