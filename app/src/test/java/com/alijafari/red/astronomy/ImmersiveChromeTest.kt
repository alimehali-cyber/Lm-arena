package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.aurora.auroraPaletteFor
import com.alijafari.red.astronomy.ui.cyber.cyberPaletteFor
import com.alijafari.red.astronomy.ui.spatial.spatialPaletteFor
import com.alijafari.red.astronomy.ui.theme.RedDarkColorTokens
import com.alijafari.red.astronomy.ui.theme.RedLightColorTokens
import com.alijafari.red.astronomy.ui.theme.RedWineColorTokens
import com.alijafari.red.astronomy.ui.theme.readableInkOn
import java.io.File
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the immersive chrome — the layer that makes the AR sky and the Gravity Sandbox look like ZIG.
 *
 * The two screens are full-screen places with floating controls over a live scene, so they cannot wear
 * a normal page layout and they used to grow their own chrome: fixed dark navy panels, white text,
 * hand-picked ambers and greens, Material `FilterChip`s at Material's corner radius, and a teal accent
 * the app does not otherwise contain. None of that can be asserted from a screenshot in a JVM test, so
 * the assertions here are of two kinds and both matter:
 *
 *  * **Arithmetic** — the ink chosen for an accent-filled control is measured for contrast against
 *    that accent in every theme the app ships ([theAccentFillAlwaysCarriesReadableInk]).
 *  * **Source shape** — the screens *call* the shared chrome and no longer call the components or the
 *    literals it replaced. A future edit that reintroduces `MaterialTheme.typography` into the AR
 *    panel, or a literal `0xFF` into the chrome, fails here rather than in review.
 */
class ImmersiveChromeTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private val chrome by lazy { source("com/alijafari/red/astronomy/ui/theme/ImmersiveChrome.kt") }
    private val ar by lazy { source("com/alijafari/red/astronomy/ui/screens/CompassARScreen.kt") }
    private val activity by lazy { source("com/alijafari/red/astronomy/MainActivity.kt") }
    private val gravityTheme by lazy { source("com/zig/gravity/ui/theme/GravityTheme.kt") }
    private val gravityRoot by lazy { source("com/zig/gravity/ui/GravitySandboxRoot.kt") }
    private val hud by lazy { source("com/zig/gravity/ui/HudBar.kt") }
    private val slider by lazy { source("com/zig/gravity/ui/SandboxSlider.kt") }
    private val lab by lazy { source("com/alijafari/red/astronomy/ui/screens/LabScreen.kt") }

    private val gravitySheets by lazy {
        listOf(
            "com/zig/gravity/ui/AddBodySheet.kt",
            "com/zig/gravity/ui/InspectorSheet.kt",
            "com/zig/gravity/ui/PresetSheet.kt",
            "com/zig/gravity/ui/TableSurfaceSheet.kt",
            "com/zig/gravity/ui/TeachingCard.kt"
        ).associateWith(::source)
    }

    // ================================ the token set ==============================================

    @Test
    fun theAuroraChromeRetunesTheAppTokensRatherThanOwningAColour() {
        val auroraProvider = source("com/alijafari/red/astronomy/ui/aurora/AuroraDesignSystem.kt")
        assertTrue(
            "Aurora publishes its palette through the same local the other styles use",
            auroraProvider.contains("LocalAuroraPalette provides palette")
        )
        assertTrue(
            "and harmonises the shared dialogs by adapting RedTheme's colours",
            auroraProvider.contains("LocalRedColors provides adaptedRedColors")
        )
        assertTrue(
            "so the shared chrome keeps resolving ink from the theme, never from a literal",
            chrome.contains("val colors = RedTheme.colors")
        )
    }

    @Test
    fun theChromeHasNoPaletteOfItsOwn() {
        // Every colour in the chrome comes from RedTheme, which is the theme the user selected and
        // already carries the Cyber / Spatial adaptation. One literal here would be a second brand.
        assertFalse("the chrome declares no literal colour", chrome.contains("Color(0xFF"))
        assertFalse("and no translucent literal either", chrome.contains("Color(0x"))
        assertTrue("the accent is the theme's", chrome.contains("accent = colors.accentRed"))
        assertTrue("the inks are the theme's", chrome.contains("textPrimary = colors.textPrimary"))
        assertTrue(
            "the ink on an accent fill is measured, not guessed",
            chrome.contains("onAccent = readableInkOn(colors.accentRed)")
        )
        assertTrue("the panel fill is the theme's elevated surface", chrome.contains("fill = colors.surfaceElevated"))
        assertTrue("the hairline is the theme's border", chrome.contains("stroke = colors.border"))
        assertTrue("status colours are tokens too", chrome.contains("success = colors.statusSuccess"))
    }

    @Test
    fun theCornerGeometryIsTheAppsCornerGeometry() {
        // Panels, rows and pills: the same three steps every other screen uses, and CYBER's tighter
        // container only where that style asks for it.
        assertTrue("panels take the window radius", chrome.contains("else -> RedCornerRadius.window"))
        assertTrue("CYBER keeps its own tighter container", chrome.contains("AppUiStyle.CYBER -> 36.dp"))
        assertTrue("and AURORA its own", chrome.contains("AppUiStyle.AURORA -> 34.dp"))
        assertTrue("pills are capsules", chrome.contains("val pillShape: Shape get() = CircleShape"))
        assertTrue("panels squircle", chrome.contains("val panelShape: Shape get() = squircleShape(panelRadius)"))
    }

    @Test
    fun theSelectedStyleIsPublishedToTheWholeApp() {
        assertTrue(
            "MainActivity provides the selected interface style",
            activity.contains("LocalAppUiStyle provides uiState.uiStyle")
        )
        assertTrue(
            "and the chrome reads it",
            chrome.contains("val style = LocalAppUiStyle.current")
        )
    }

    // ================================ the components =============================================

    @Test
    fun theSharedComponentsExist() {
        listOf(
            "fun ChromeCapsule(",
            "fun ChromeIconDisc(",
            "fun ChromeToggleChip(",
            "fun ChromeTextAction(",
            "fun ChromePrimaryAction(",
            "fun ChromePanel(",
            "fun ChromeBanner(",
            "fun ChromeDivider(",
            "fun ChromeSectionTitle(",
            "fun ChromeCaption("
        ).forEach { assertTrue("the chrome offers $it", chrome.contains(it)) }

        // They are built out of the app's own material, not Material's defaults.
        assertTrue("panels are drawn by RedSurface", chrome.contains("RedSurface("))
        assertTrue("labels use the app type scale", chrome.contains("RedTypographyTokens.badge"))
        assertTrue("captions use the app type scale", chrome.contains("RedTypographyTokens.caption"))
    }

    // ================================ the AR screen ==============================================

    @Test
    fun theArControlsAreTheAppChrome() {
        assertTrue("the screen resolves the chrome", ar.contains("val chrome = chromeTokens()"))
        assertTrue("its accent is that chrome's", ar.contains("val accent = chrome.accent"))
        assertTrue("pills are the shared capsule", ar.contains("ChromeCapsule("))
        assertTrue("filters are the shared chip", ar.contains("ChromeToggleChip("))
        assertTrue("actions are the shared action", ar.contains("ChromePrimaryAction("))
        assertTrue("quiet actions are the shared action", ar.contains("ChromeTextAction("))
        assertTrue("round controls are the shared disc", ar.contains("ChromeIconDisc("))
        assertTrue("banners are the shared banner", ar.contains("ChromeBanner("))
    }

    @Test
    fun theArPanelKeepsNoSecondDesignSystem() {
        assertFalse("no Material text styles are left", ar.contains("MaterialTheme.typography"))
        assertFalse("no Material filter chips are left", ar.contains("FilterChip("))
        assertFalse("no Material text buttons are left", ar.contains("TextButton("))
        assertFalse("no Material button colours are left", ar.contains("ButtonDefaults"))
        assertFalse("the static accent is gone", ar.contains("AccentPrimary"))
        assertFalse("so is the static status green", ar.contains("StatusGood"))
        assertFalse("no navy panel literals", ar.contains("Color(0xDD"))
        assertFalse("no translucent panel literals", ar.contains("Color(0xCC"))
        assertFalse("and no stray sky-blue orbit path", ar.contains("0xFF38BDF8"))

        // The one literal that must stay is the night-vision filter: it tints the camera image.
        assertEquals(
            "only the photographic filter keeps a literal",
            1,
            Regex("Color\\(0x").findAll(ar).count()
        )
        assertTrue("and it is the night-vision filter", ar.contains("drawRect(color = Color(0xFFFF1E1E)"))
    }

    @Test
    fun theArReticleStaysPhotographicButItsRingFollowsTheTheme() {
        // A reticle drawn over a camera image is white on purpose; the accent ring is the theme's.
        assertTrue("the accented ring follows the theme", ar.contains("drawCircle(color = accent, radius = ring"))
        assertTrue(
            "the optical ticks stay pinned white",
            ar.contains("drawLine(Color.White.copy(alpha = 0.85f)")
        )
    }

    // ================================ the gravity sandbox ========================================

    @Test
    fun theSandboxChromeIsRebasedOnTheAppTheme() {
        assertTrue("the derivation exists", gravityTheme.contains("fun GravityColors.withAppChrome("))
        assertTrue("the accent is the app's", gravityTheme.contains("accent = chrome.accent"))
        assertTrue("the ink is the app's primary", gravityTheme.contains("onSurface = chrome.textPrimary"))
        assertTrue("the dim ink is the app's secondary", gravityTheme.contains("onSurfaceDim = chrome.textSecondary"))
        assertTrue("the panel fill is the app's", gravityTheme.contains("chrome = chrome.fill"))
        assertTrue("the hairline is the app's", gravityTheme.contains("chromeBorder = chrome.stroke"))
        assertTrue("selection follows the accent", gravityTheme.contains("selection = chrome.accent"))
        assertTrue("and the theme resolves it in composition", gravityTheme.contains("val chrome = chromeTokens()"))
        // The tabletop itself is content, and keeps its own material.
        assertTrue("the surface still comes from the tabletop", gravityTheme.contains("colorsFor(surface)"))
    }

    @Test
    fun theSandboxScreensWearTheSharedComponents() {
        assertTrue("the root resolves the chrome", gravityRoot.contains("val chrome = chromeTokens()"))
        assertTrue("scene / predict / inspector are capsules", gravityRoot.contains("ChromeCapsule("))
        assertTrue("the long-press menu and camera panel are panels", gravityRoot.contains("ChromePanel("))
        assertTrue("notices are the shared banner", gravityRoot.contains("ChromeBanner("))
        assertTrue("the add control is one accent disc", gravityRoot.contains("AddDisc("))
        assertTrue("and it fills with the accent", gravityRoot.contains("background(chrome.accent, CircleShape)"))
        assertFalse("the teal accent is gone from the root", gravityRoot.contains("0xFF2DD4BF"))
    }

    @Test
    fun theHudAndTheSliderAreTheAppChrome() {
        assertTrue("HUD controls are the shared discs", hud.contains("ChromeIconDisc("))
        assertTrue("the HUD platter is the app's fill", hud.contains("background(chrome.fill, CircleShape)"))
        assertTrue("the speed ladder uses the app type scale", hud.contains("RedTypographyTokens.badge"))
        assertTrue("the selected step is the accent", hud.contains("if (selected) chrome.accent else Color.Transparent"))
        assertFalse("no ad-hoc label sizes are left", hud.contains("fontSize = 11.sp"))
        assertFalse("and no teal", hud.contains("0xFF2DD4BF"))

        assertTrue("the slider label is a caption", slider.contains("RedTypographyTokens.caption"))
        assertTrue("its track is thin", slider.contains("val trackH = 3.dp.toPx()"))
        assertTrue("and it carries an accent halo", slider.contains("radius = haloR"))
    }

    @Test
    fun theSandboxSheetsWearTheAppsSurfaceAndInk() {
        gravitySheets.forEach { (name, text) ->
            assertTrue(
                "$name takes its sheet fill from the chrome",
                text.contains("containerColor = c.chrome")
            )
            assertTrue(
                "$name takes its ink from the chrome",
                text.contains("contentColor = c.onSurface")
            )
            assertFalse("$name names no Material text style", text.contains("MaterialTheme.typography"))
        }
    }

    @Test
    fun theLabStillOpensTheSandboxThroughOneCall() {
        assertTrue("the Lab opens the sandbox", lab.contains("com.zig.gravity.ui.GravitySandboxRoot("))
        assertTrue("with the app's language", lab.contains("startInPersian = isFa"))
        assertTrue(
            "and the app's theme, so the sheet palette matches the page that opened it",
            lab.contains("startInDarkTheme = uiState.themeMode !=")
        )
    }

    // ================================ the arithmetic =============================================

    /** WCAG relative luminance, with the sRGB transfer function applied. */
    private fun relativeLuminance(color: Color): Float {
        fun linear(channel: Float): Float =
            if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)
        return 0.2126f * linear(color.red) + 0.7152f * linear(color.green) + 0.0722f * linear(color.blue)
    }

    private fun contrastRatio(a: Color, b: Color): Float {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
    }

    @Test
    fun theAccentFillAlwaysCarriesReadableInk() {
        // A selected capsule, a selected chip and a primary action are all "accent fill, on-accent ink".
        // That pairing has to read in every palette the app ships, in all three interface styles.
        val accents = linkedMapOf(
            "default dark" to RedDarkColorTokens.accentRed,
            "default light" to RedLightColorTokens.accentRed,
            "wine" to RedWineColorTokens.accentRed,
            "cyber dark" to cyberPaletteFor(ThemeMode.DARK).primaryAccent,
            "cyber light" to cyberPaletteFor(ThemeMode.LIGHT).primaryAccent,
            "cyber wine" to cyberPaletteFor(ThemeMode.WINE).primaryAccent,
            "spatial dark" to spatialPaletteFor(ThemeMode.DARK).neonGlow,
            "spatial light" to spatialPaletteFor(ThemeMode.LIGHT).neonGlow,
            "spatial wine" to spatialPaletteFor(ThemeMode.WINE).neonGlow,
            "aurora dark" to auroraPaletteFor(ThemeMode.DARK).accent,
            "aurora light" to auroraPaletteFor(ThemeMode.LIGHT).accent,
            "aurora wine" to auroraPaletteFor(ThemeMode.WINE).accent
        )
        accents.forEach { (name, accent) ->
            val ink = readableInkOn(accent)
            val ratio = contrastRatio(ink, accent)
            assertTrue(
                "$name: the on-accent ink must clear 3:1 against the accent, measured $ratio",
                ratio >= 3.0f
            )
            assertTrue("$name: the ink is not the fill", ink != accent)
        }
    }

    @Test
    fun everyStyleHasItsOwnGeometryAndTheDefaultsAreTheAppsOwn() {
        // DEFAULT and SPATIAL share the app's window corner; CYBER and AURORA are tighter, each by
        // its own scale. The styles are distinct values, so a future style cannot silently inherit
        // another's geometry.
        assertEquals(4, AppUiStyle.entries.size)
        assertTrue("the chrome branches on CYBER", chrome.contains("AppUiStyle.CYBER ->"))
        assertTrue("and on AURORA", chrome.contains("AppUiStyle.AURORA ->"))
        assertTrue("and defaults the rest", chrome.contains("else -> RedCornerRadius.window"))
    }
}
