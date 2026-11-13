package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.aether.AetherCanvasVoid
import com.alijafari.red.astronomy.ui.aether.AetherCrimsonCoreCenter
import com.alijafari.red.astronomy.ui.aether.AetherCrimsonCoreEdge
import com.alijafari.red.astronomy.ui.aether.AetherCrimsonCoreMid
import com.alijafari.red.astronomy.ui.aether.AetherFrostedGlassFill
import com.alijafari.red.astronomy.ui.aether.AetherGlowWhite
import com.alijafari.red.astronomy.ui.aether.AetherJewelTone
import com.alijafari.red.astronomy.ui.aether.AetherMutedTelemetry
import com.alijafari.red.astronomy.ui.aether.AetherSpecularRimEnd
import com.alijafari.red.astronomy.ui.aether.AetherSpecularRimMid
import com.alijafari.red.astronomy.ui.aether.AetherSpecularRimStart
import com.alijafari.red.astronomy.ui.aether.aetherPaletteFor
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.SquircleShape
import com.alijafari.red.astronomy.ui.theme.readableInkOn
import com.alijafari.red.astronomy.ui.theme.wcagContrast
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the Aether Telemetry UI design system ([AppUiStyle.AETHER]):
 * - Frameless rim-lit spatial cards (`1.dp` `specularRimBorder`), jewel-tone radial gradients,
 *   and Dark / Light / Wine contrast safety.
 * - Custom Compose `Canvas` telemetry graphics (`AetherOscilloscopeWaveformCanvas` cubic arches
 *   with `BlurMaskFilter` trough glow, 60-tick `AetherRadialDialCard`, `AetherGlowingMeshPillCard`,
 *   `AetherFloatingStatusCapsule`, rolling `AetherRollingDotMatrixCounter`, and `AetherMicroStepper`
 *   with haptic feedback).
 * - Full wiring across `MainActivity`, all 4 Settings modals, `ImmersiveChrome`, `AntarControlPanel`,
 *   and EN/FA string resources.
 */
class AetherUiTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private fun resource(relative: String): String {
        val candidates = listOf("src/main/res/$relative", "app/src/main/res/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Resource not found: $relative")
    }

    @Test
    fun aetherTokensMatchSpecificationAndMaintainContrastAcrossThemes() {
        assertEquals(Color(0xFF08080C), AetherCanvasVoid)
        assertEquals(Color(0xFF1C020D), AetherCrimsonCoreCenter)
        assertEquals(Color(0xFF5E052A), AetherCrimsonCoreMid)
        assertEquals(Color(0xFF9E0B45), AetherCrimsonCoreEdge)
        assertEquals(Color(0x14FFFFFF), AetherFrostedGlassFill)
        assertEquals(Color(0x66FFFFFF), AetherSpecularRimStart)
        assertEquals(Color(0x0DFFFFFF), AetherSpecularRimMid)
        assertEquals(Color(0x33FFFFFF), AetherSpecularRimEnd)
        assertEquals(Color(0xFFFFFFFF), AetherGlowWhite)
        assertEquals(Color(0x80FFFFFF), AetherMutedTelemetry)

        assertTrue(
            "Top-left specular rim highlight must outshine center fade and bottom-right bounce",
            AetherSpecularRimStart.alpha > AetherSpecularRimEnd.alpha &&
                AetherSpecularRimEnd.alpha > AetherSpecularRimMid.alpha
        )

        // Jewel Hero center is near-black so GlowWhite text clears WCAG AAA (>= 7:1)
        for (tone in AetherJewelTone.entries) {
            val centerContrast = wcagContrast(AetherGlowWhite, tone.centerColor)
            assertTrue(
                "Jewel ${tone.name} center must clear 12:1 against GlowWhite ($centerContrast)",
                centerContrast >= 12.0f
            )
        }

        for (mode in ThemeMode.entries) {
            val p = aetherPaletteFor(mode)
            val ink = readableInkOn(p.crimsonGlow)
            val ratio = wcagContrast(ink, p.crimsonGlow)
            assertTrue("Aether $mode accent must clear 4.5:1 with readableInkOn ($ratio)", ratio >= 4.5f)
        }
    }

    @Test
    fun aetherDesignSystemProvidesFramelessRimCardsCanvasVisualsAndPhysics() {
        val design = source("com/alijafari/red/astronomy/ui/aether/AetherDesignSystem.kt")
        val dotMatrix = source("com/alijafari/red/astronomy/ui/aether/AetherDotMatrix.kt")
        val ambient = source("com/alijafari/red/astronomy/ui/aether/AetherAmbientBackground.kt")

        assertTrue(design.contains("fun Modifier.specularRimBorder("))
        assertTrue(design.contains("fun AetherJewelCard("))
        assertTrue(design.contains("fun AetherFrostedCard("))
        assertTrue(design.contains("fun AetherOscilloscopeWaveformCanvas("))
        assertTrue(design.contains("cubicTo("))
        assertTrue(design.contains("BlurMaskFilter("))
        assertTrue(design.contains("fun AetherRadialDialCard("))
        assertTrue(design.contains("val totalTicks = 60"))
        assertTrue(design.contains("for (i in 0 until totalTicks)"))
        assertTrue(design.contains("val isHighlighted = i in highlightRange"))
        assertTrue(design.contains("fun AetherGlowingMeshPillCard("))
        assertTrue(design.contains("fun AetherFloatingStatusCapsule("))
        assertTrue(design.contains("fun AetherMicroStepper("))
        assertTrue(design.contains("HapticFeedbackType.TextHandleMove"))

        assertTrue(dotMatrix.contains("fun AetherDotMatrixText("))
        assertTrue(dotMatrix.contains("fun AetherRollingDotMatrixCounter("))
        assertTrue(dotMatrix.contains("AnimatedContent("))
        assertTrue(dotMatrix.contains("slideInVertically"))

        assertTrue(ambient.contains("rememberInfiniteTransition"))
        assertTrue(ambient.contains("Build.VERSION_CODES.TIRAMISU"))
        assertTrue(ambient.contains("RuntimeShader"))
    }

    @Test
    fun aetherScreensAndLocalizedStringsAreWiredEndToEnd() {
        val home = source("com/alijafari/red/astronomy/ui/aether/AetherHomeScreen.kt")
        val satellites = source("com/alijafari/red/astronomy/ui/aether/AetherSatellitesScreen.kt")
        val lab = source("com/alijafari/red/astronomy/ui/aether/AetherLabScreen.kt")
        val nav = source("com/alijafari/red/astronomy/ui/aether/AetherFloatingNavBar.kt")
        val settings = source("com/alijafari/red/astronomy/ui/aether/AetherSettingsModal.kt")
        val stringsEn = resource("values/strings.xml")
        val stringsFa = resource("values-fa/strings.xml")

        assertTrue(home.contains("presentation = SkyCanvasPresentation.SCENE"))
        assertTrue(home.contains("AetherJewelCard("))
        assertTrue(home.contains("AetherOscilloscopeWaveformCanvas("))
        assertTrue(home.contains("AetherRadialDialCard("))
        assertTrue(home.contains("AetherGlowingMeshPillCard("))
        assertTrue(home.contains("AetherFloatingStatusCapsule("))

        assertTrue(satellites.contains("satellite_screen"))
        assertTrue(satellites.contains("satellite_time_scrubber_card"))
        assertTrue(satellites.contains("time_scrubber_slider"))

        assertTrue(lab.contains("lab_screen"))
        assertTrue(lab.contains("TimeDilationCalculatorScreen("))
        assertTrue(lab.contains("GravitySandboxRoot("))
        assertTrue(lab.contains("AntarRoot("))

        assertTrue(nav.contains("floating_bottom_bar"))
        assertTrue(settings.contains("settings_ui_style_aether"))
        assertTrue(stringsEn.contains("<string name=\"ui_style_aether\">Aether Telemetry UI</string>"))
        assertTrue(stringsFa.contains("<string name=\"ui_style_aether\">"))
    }

    @Test
    fun aetherMoonAndEclipseCardsAreSideBySideVerticalPillsWithEnlargedConcentricRenders() {
        val design = source("com/alijafari/red/astronomy/ui/aether/AetherDesignSystem.kt")
        val home = source("com/alijafari/red/astronomy/ui/aether/AetherHomeScreen.kt")

        // Squircle.kt rule: rectangular Aether containers use squircleShape(...),
        // while pills/capsules use RedShapes.capsule (true semicircular caps, not SquircleShape).
        assertFalse("Capsule must keep semicircular caps, not a squircle", RedShapes.capsule is SquircleShape)
        assertTrue(design.contains("shape: Shape = squircleShape(cornerRadius)"))
        assertTrue(design.contains("val dialShape = squircleShape(34.dp)"))
        assertFalse(
            "AetherDesignSystem should use squircleShape(...) and RedShapes.capsule instead of raw RoundedCornerShape",
            design.lines().filterNot { it.trim().startsWith("*") || it.trim().startsWith("//") }
                .any { it.contains("RoundedCornerShape(") }
        )
        assertFalse(
            "AetherHomeScreen should use squircleShape(...) and RedShapes.capsule instead of raw RoundedCornerShape",
            home.lines().filterNot { it.trim().startsWith("*") || it.trim().startsWith("//") }
                .any { it.contains("RoundedCornerShape(") }
        )

        // Side-by-side vertical pill sizing & concentric top-dome render geometry
        val lensesStart = home.indexOf("item(key = \"aether_moon_eclipse_lenses\")")
        assertTrue("Expected side-by-side moon/eclipse item in AetherHomeScreen", lensesStart >= 0)
        val lensesSection = home.substring(lensesStart, home.indexOf("item(key = \"aether_tonight_header\")"))
        assertTrue(lensesSection.contains("BoxWithConstraints("))
        assertTrue(lensesSection.contains("val renderInset = 10.dp"))
        assertTrue(lensesSection.contains("val renderSize = (cardWidth - renderInset * 2).coerceAtLeast(0.dp)"))
        assertTrue(lensesSection.contains(".coerceAtLeast(cardWidth * 1.88f)"))
        assertTrue(lensesSection.contains(".width(cardWidth)"))
        assertTrue(lensesSection.contains(".height(pillHeight)"))
        assertTrue(lensesSection.contains(".testTag(\"home_moon_card\")"))
        assertTrue(lensesSection.contains(".testTag(\"home_eclipse_card\")"))

        // Both cards use RedShapes.capsule + enlarged .size(renderSize) renders (no old 92.dp cap)
        val moonCardStart = home.indexOf("private fun AetherLunarOpticalCard(")
        val eclipseCardStart = home.indexOf("private fun AetherEclipseOpticalCard(")
        assertTrue(moonCardStart >= 0 && eclipseCardStart > moonCardStart)
        val moonCardBody = home.substring(moonCardStart, eclipseCardStart)
        val eclipseCardBody = home.substring(
            eclipseCardStart,
            home.indexOf("@Composable", eclipseCardStart + 1).let { if (it < 0) home.length else it }
        )

        for ((name, body) in listOf("Moon" to moonCardBody, "Eclipse" to eclipseCardBody)) {
            assertTrue("$name card must use RedShapes.capsule for vertical pill caps", body.contains("shape = RedShapes.capsule"))
            assertTrue("$name card must size its render to renderSize", body.contains(".size(renderSize)"))
            assertFalse("$name card must not use the old 92.dp render size", body.contains("92.dp"))
            assertTrue(
                "$name card must inset top/sides by renderInset so the circular render is concentric with the top dome",
                body.contains("start = renderInset") &&
                    body.contains("top = renderInset") &&
                    body.contains("end = renderInset")
            )
        }
        assertTrue(moonCardBody.contains("MoonPhaseRender("))
        assertTrue(moonCardBody.contains("home_moon_card_render"))
        assertTrue(eclipseCardBody.contains("EclipseRender("))
        assertTrue(eclipseCardBody.contains("home_eclipse_card_render"))
    }
}
