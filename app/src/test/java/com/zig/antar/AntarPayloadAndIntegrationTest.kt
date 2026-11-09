package com.zig.antar

import com.zig.antar.config.AntarObjectMode
import com.zig.antar.config.AntarPreset
import com.zig.antar.config.AntarSimulationSettings
import com.zig.antar.render.AntarSimulationPayload
import com.zig.antar.render.parseAntarTelemetryJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AntarPayloadAndIntegrationTest {

    private fun readSource(relative: String): String {
        val candidates = listOf("src/main/$relative", "app/src/main/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source file not found: $relative")
    }

    @Test
    fun bundledKerrSimulationDecompressesAndInstallsNativeBridge() {
        val raw = AntarSimulationPayload.rawSimulationHtml
        assertTrue(raw.startsWith("<!DOCTYPE html>"))
        assertTrue(raw.contains("<canvas id=\"gl\"></canvas>"))
        assertTrue(raw.contains("window.__bh ="))
        assertTrue(raw.length > 200_000)

        val hosted = AntarSimulationPayload.buildHostedHtml()
        assertTrue(hosted.contains("<style id=\"antar-native-chrome\">"))
        assertTrue(hosted.contains("body > :not(#gl)"))
        assertTrue(hosted.contains("<script id=\"antar-native-bridge\">"))
        assertTrue(hosted.contains("window.__antarBridge"))
        assertTrue(hosted.contains("background: #000000 !important"))
        assertTrue(hosted.contains("new URLSearchParams(location.search || new URL(document.baseURI).search)"))
        assertTrue(hosted.contains("const floatLinear = !!gl.getExtension('OES_texture_float_linear')"))
        assertTrue(hosted.contains("floatLinear ? gl.LINEAR_MIPMAP_LINEAR : gl.NEAREST_MIPMAP_NEAREST"))
        assertTrue(hosted.contains("WebGL sky framebuffer incomplete"))
        assertTrue(hosted.contains("WebGL MRT framebuffer incomplete"))
        assertTrue(hosted.contains("any(isnan(col))"))
        assertTrue(hosted.contains("clamp(safeOut, vec3(0.0), vec3(60000.0))"))
        assertTrue(hosted.contains("window.AntarHostBridge.onRuntimeError"))
        assertTrue(hosted.endsWith("</html>\n") || hosted.trimEnd().endsWith("</html>"))
    }

    @Test
    fun presetsAndSettingsSerializeCleanlyForEngineAndTelemetryParserWorks() {
        for (preset in AntarPreset.entries) {
            val s = preset.applyTo(AntarSimulationSettings.DEFAULT)
            assertEquals(preset, s.activePreset)
            val url = AntarSimulationPayload.buildBaseUrl(s)
            assertTrue(url.startsWith(AntarSimulationPayload.HOST_BASE_ORIGIN + "?transparent=1&ui=0"))
            val json = AntarSimulationPayload.buildSettingsJson(s, 12.5, -8.0, 0.0, 0.1, -0.05)
            assertTrue(json.contains("\"object\":\"${s.objectMode.jsKey}\""))
            assertTrue(json.contains("\"panXDp\":12.50"))
        }

        val binary = AntarPreset.BINARY_INSPIRAL.applyTo()
        assertEquals(AntarObjectMode.BINARY_MERGER, binary.objectMode)

        val snap = parseAntarTelemetryJson(
            """{"ready":true,"error":null,"fps":59,"scale":0.75,"rw":1620,"rh":810,"dist":18.5,"incl":79.0,"az":38.0,"plunge":false,"plungeR":18.5,"plungeTau":0,"inside":false,"tour":false,"binSep":13.2,"binV":0.27,"binFgw":1.4e-5,"binTMerge":92.4,"binMerged":false}"""
        )
        assertTrue(snap.engineReady)
        assertNull(snap.errorMessage)
        assertEquals(59, snap.fps)
        assertEquals(18.5, snap.cameraDistanceM, 1e-6)
        assertEquals(13.2, snap.binarySeparationM, 1e-6)
    }

    @Test
    fun zigBlackholeIsWiredIntoAllThreeLabStylesAndLocalizedResources() {
        val labScreen = readSource("java/com/alijafari/red/astronomy/ui/screens/LabScreen.kt")
        val spatialLab = readSource("java/com/alijafari/red/astronomy/ui/spatial/SpatialLabScreen.kt")
        val cyberLab = readSource("java/com/alijafari/red/astronomy/ui/cyber/CyberLabScreen.kt")
        val stringsEn = readSource("res/values/strings.xml")
        val stringsFa = readSource("res/values-fa/strings.xml")

        assertTrue(labScreen.contains("ZIG_BLACKHOLE("))
        assertTrue(labScreen.contains("titleEn = \"ZIG's Blackhole\""))
        assertTrue(labScreen.contains("titleFa = \"سیاه‌چاله زیگ\""))
        assertTrue(labScreen.contains("selectedFeature == LabFeatureType.ZIG_BLACKHOLE"))
        assertTrue(labScreen.contains("com.zig.antar.ui.AntarRoot("))

        assertTrue(spatialLab.contains("selectedFeature == LabFeatureType.ZIG_BLACKHOLE"))
        assertTrue(spatialLab.contains("LabFeatureType.ZIG_BLACKHOLE ->"))

        assertTrue(cyberLab.contains("LabFeatureType.ZIG_BLACKHOLE ->"))
        assertTrue(cyberLab.contains("AntarRoot("))

        assertTrue(stringsEn.contains("<string name=\"zig_blackhole_title\">ZIG\\'s Blackhole</string>"))
        assertTrue(stringsFa.contains("<string name=\"zig_blackhole_title\">سیاه‌چاله زیگ</string>"))
    }

    @Test
    fun antarUiRespectsPart6ChromeAndVazirmatnTypographyRules() {
        val antarRoot = readSource("java/com/zig/antar/ui/AntarRoot.kt")
        val antarPanel = readSource("java/com/zig/antar/ui/AntarControlPanel.kt")

        assertTrue(antarRoot.contains("ImmersiveScreenState.enter()"))
        assertTrue(antarRoot.contains("ImmersiveScreenState.exit()"))
        assertTrue(antarRoot.contains("BackHandler(enabled = true)"))
        assertTrue(antarRoot.contains("rememberCosmicBackdropColor"))
        assertFalse(antarRoot.contains("Brush.radialGradient"))
        val webEngine = readSource("java/com/zig/antar/render/AntarWebEngineView.kt")
        assertTrue(webEngine.contains("ViewGroup.LayoutParams.MATCH_PARENT"))
        assertTrue(webEngine.contains("setBackgroundColor(Color.BLACK)"))
        assertTrue(webEngine.contains("View.LAYER_TYPE_NONE"))
        assertTrue(webEngine.contains("simulationUrl\n                )"))
        assertTrue(antarPanel.contains("RedSurface("))
        assertTrue(antarPanel.contains("fontFeatureSettings = \"tnum\""))

        for (src in listOf(antarRoot, antarPanel)) {
            assertFalse("No topBar scaffold band", src.contains("topBar = {"))
            assertFalse("No ArrowBack navigation icon", src.contains("ArrowBack"))
            assertFalse("No deleted LiquidGlass reference", src.contains("LiquidGlass"))
            assertFalse("No layerBackdrop sampling", src.contains("layerBackdrop"))
            assertFalse("No com.kyant import", src.contains("com.kyant"))
            assertFalse("No generic FontFamily.Monospace override", src.contains("FontFamily.Monospace"))
        }
    }
}
