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
import com.alijafari.red.astronomy.ui.aether.AetherJewelPreset
import com.alijafari.red.astronomy.ui.aether.AetherMeshPillPreset
import com.alijafari.red.astronomy.ui.aether.AetherMutedTelemetry
import com.alijafari.red.astronomy.ui.aether.DarkAetherPalette
import com.alijafari.red.astronomy.ui.aether.aetherPaletteFor
import com.alijafari.red.astronomy.ui.theme.wcagContrast
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the Aether Telemetry UI ([AppUiStyle.AETHER]) design system:
 * frameless `1.dp` directional specular rim borders, jewel-core radial surfaces,
 * Haze frosted glass refraction, AGSL + Canvas figure-8 ambient mesh,
 * synthesizer cubic-bezier oscilloscope & spline canvases, 60-tick radial dial,
 * LED dot-matrix rolling counters, and full screen/settings wiring.
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
    fun exactAetherColorTokensAndJewelContrastsMatchSpecification() {
        assertEquals(Color(0xFF08080C), AetherCanvasVoid)
        assertEquals(Color(0xFF1C020D), AetherCrimsonCoreCenter)
        assertEquals(Color(0xFF5E052A), AetherCrimsonCoreMid)
        assertEquals(Color(0xFF9E0B45), AetherCrimsonCoreEdge)
        assertEquals(Color(0x14FFFFFF), AetherFrostedGlassFill)
        assertEquals(Color(0xFFFFFFFF), AetherGlowWhite)
        assertEquals(Color(0x80FFFFFF), AetherMutedTelemetry)
        assertEquals(Color(0xFF59092B), DarkAetherPalette.ambientBlobPrimary)
        assertEquals(Color(0xFF14385C), DarkAetherPalette.ambientBlobSecondary)

        for (preset in AetherJewelPreset.entries) {
            val contrast = wcagContrast(Color.White, preset.centerDark)
            assertTrue(
                "Jewel center ${preset.name} must exceed WCAG AAA 7:1 against white ($contrast)",
                contrast >= 7.0f
            )
        }

        val dark = aetherPaletteFor(ThemeMode.DARK)
        assertTrue(dark.isDark)
        assertEquals(AetherCanvasVoid, dark.canvasVoid)
        assertEquals(AetherFrostedGlassFill, dark.frostedGlassFill)

        val wine = aetherPaletteFor(ThemeMode.WINE)
        assertTrue(wine.isDark && wine.isWine)
        assertTrue("Wine canvas void is burgundy-dominant", wine.canvasVoid.red > wine.canvasVoid.green)

        val light = aetherPaletteFor(ThemeMode.LIGHT)
        assertFalse(light.isDark)
        assertTrue(
            "Light mode text clears WCAG AAA against alabaster canvas",
            wcagContrast(light.textPrimary, light.canvasVoid) >= 7.0f
        )

        assertEquals(4, AetherMeshPillPreset.entries.size)
        assertEquals(Color(0xFFD96B98), AetherMeshPillPreset.PINK_ROSE.darkColors.first())
        assertEquals(Color(0xFFE07A42), AetherMeshPillPreset.AMBER_COPPER.darkColors.first())
    }

    @Test
    fun designSystemExposesSpecularRimJewelFrostedDialOscilloscopeAndStepperPrimitives() {
        val ds = source("com/alijafari/red/astronomy/ui/aether/AetherDesignSystem.kt")
        val bg = source("com/alijafari/red/astronomy/ui/aether/AetherAmbientBackground.kt")
        val dm = source("com/alijafari/red/astronomy/ui/aether/AetherDotMatrix.kt")

        assertTrue(ds.contains("fun Modifier.specularRimBorder("))
        assertTrue(ds.contains("fun AetherJewelTelemetryCard("))
        assertTrue(ds.contains("fun AetherFrostedSpatialCard("))
        assertTrue(ds.contains("fun AetherRadialDialCard("))
        assertTrue(ds.contains("fun AetherGlowingMeshPillCard("))
        assertTrue(ds.contains("fun AetherFloatingStatusCapsule("))
        assertTrue(ds.contains("fun AetherOscilloscopeCanvas("))
        assertTrue(ds.contains("fun AetherSplineTelemetryCanvas("))
        assertTrue(ds.contains("fun AetherMicroStepper("))
        assertTrue(ds.contains("fun AetherLedStatusArray("))
        assertTrue(ds.contains("PathMeasure()"))
        assertTrue(ds.contains("BlurMaskFilter("))
        assertTrue(ds.contains("HapticFeedbackType.TextHandleMove"))
        assertTrue(ds.contains(".hazeChild("))

        assertTrue(bg.contains("AETHER_AGSL_MESH_SHADER"))
        assertTrue(bg.contains("RuntimeShader("))
        assertTrue(bg.contains("durationMillis = 9000"))
        assertTrue(bg.contains(".haze(state = hazeState)"))

        assertTrue(dm.contains("fun AetherDotMatrixText("))
        assertTrue(dm.contains("fun AetherRollingDotMatrixCounter("))
        assertTrue(dm.contains("fun AetherRollingMetricText("))
        assertTrue(dm.contains("AnimatedContent("))
    }

    @Test
    fun allAetherScreensAreImplementedAndLocalizedInEnglishAndPersian() {
        val home = source("com/alijafari/red/astronomy/ui/aether/AetherHomeScreen.kt")
        val sats = source("com/alijafari/red/astronomy/ui/aether/AetherSatellitesScreen.kt")
        val lab = source("com/alijafari/red/astronomy/ui/aether/AetherLabScreen.kt")
        val nav = source("com/alijafari/red/astronomy/ui/aether/AetherFloatingNavBar.kt")
        val modal = source("com/alijafari/red/astronomy/ui/aether/AetherSettingsModal.kt")

        assertTrue(home.contains("presentation = SkyCanvasPresentation.SCENE"))
        assertTrue(home.contains("AetherFloatingStatusCapsule("))
        assertTrue(home.contains("AetherJewelTelemetryCard("))
        assertTrue(home.contains("AetherOscilloscopeCanvas("))
        assertTrue(home.contains("AetherRadialDialCard("))
        assertTrue(home.contains("AetherGlowingMeshPillCard("))
        assertTrue(home.contains("AetherMoonLensCard("))
        assertTrue(home.contains("AetherEclipseLensCard("))

        assertTrue(sats.contains("AetherJewelTelemetryCard("))
        assertTrue(sats.contains("AetherRadialDialCard("))
        assertTrue(sats.contains("AetherSplineTelemetryCanvas("))
        assertTrue(sats.contains("AetherMicroStepper("))

        assertTrue(lab.contains("AetherJewelTelemetryCard("))
        assertTrue(lab.contains("AetherOscilloscopeCanvas("))
        assertTrue(lab.contains("AetherRadialDialCard("))
        assertTrue(lab.contains("LabFeatureType.TIME_DILATION ->"))
        assertTrue(lab.contains("LabFeatureType.GRAVITY_SANDBOX ->"))
        assertTrue(lab.contains("LabFeatureType.ZIG_BLACKHOLE ->"))

        assertTrue(nav.contains("specularRimBorder("))
        assertTrue(nav.contains(".hazeChild("))

        assertTrue(modal.contains("specularRimBorder("))
        assertTrue(modal.contains("AetherMicroStepper("))
        assertTrue(modal.contains("AppUiStyle.AETHER"))

        val stringsEn = resource("values/strings.xml")
        val stringsFa = resource("values-fa/strings.xml")
        assertTrue(stringsEn.contains("<string name=\"ui_style_aether\">"))
        assertTrue(stringsEn.contains("<string name=\"ui_style_aether_desc\">"))
        assertTrue(stringsFa.contains("<string name=\"ui_style_aether\">"))
        assertTrue(stringsFa.contains("<string name=\"ui_style_aether_desc\">"))
    }
}
