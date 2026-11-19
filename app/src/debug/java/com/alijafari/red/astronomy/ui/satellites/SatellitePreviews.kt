package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.Earth3DMath
import com.alijafari.red.astronomy.EarthViewportProjection
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.astro_engine.TleDataSource
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.ui.aether.AetherFloatingNavBar
import com.alijafari.red.astronomy.ui.aether.AetherThemeProvider
import com.alijafari.red.astronomy.ui.components.FloatingBottomBar
import com.alijafari.red.astronomy.ui.components.SATELLITE_EARTH_DEFAULT_DISTANCE
import com.alijafari.red.astronomy.ui.screens.SatelliteDetailContent
import com.alijafari.red.astronomy.ui.spatial.SpatialFloatingNavBar
import com.alijafari.red.astronomy.ui.spatial.SpatialThemeProvider
import com.alijafari.red.astronomy.ui.theme.REDTheme
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

@Preview(name = "English", locale = "en", widthDp = 390, heightDp = 844, showSystemUi = true)
@Preview(name = "فارسی", locale = "fa", widthDp = 390, heightDp = 844, showSystemUi = true)
private annotation class SatLocales

@SatLocales @Composable private fun DarkGlobe() = SatPreview(ThemeMode.DARK)
@SatLocales @Composable private fun DarkTime() = SatPreview(ThemeMode.DARK, SatPanel.Time)
@SatLocales @Composable private fun DarkPasses() = SatPreview(ThemeMode.DARK, SatPanel.Passes)
@SatLocales @Composable private fun SelectedAgendaPass() = SatPreview(ThemeMode.DARK, SatPanel.Passes, selectedAgendaPass = true)
@SatLocales @Composable private fun DarkDetail() = SatPreview(ThemeMode.DARK, detail = true)
@SatLocales @Composable private fun PassDetails() = SatPreview(ThemeMode.DARK, detail = true, detailPass = true)
@SatLocales @Composable private fun PassGlobePreview() = SatPreview(ThemeMode.DARK, preview = true)
@SatLocales @Composable private fun DistantPassTimeline() = SatPreview(ThemeMode.DARK, SatPanel.Time, preview = true)
@SatLocales @Composable private fun LightGlobe() = SatPreview(ThemeMode.LIGHT)
@SatLocales @Composable private fun LightTime() = SatPreview(ThemeMode.LIGHT, SatPanel.Time)
@SatLocales @Composable private fun LightPasses() = SatPreview(ThemeMode.LIGHT, SatPanel.Passes)
@SatLocales @Composable private fun LightDetail() = SatPreview(ThemeMode.LIGHT, detail = true)
@SatLocales @Composable private fun WineGlobe() = SatPreview(ThemeMode.WINE)
@SatLocales @Composable private fun WineTime() = SatPreview(ThemeMode.WINE, SatPanel.Time)
@SatLocales @Composable private fun WinePasses() = SatPreview(ThemeMode.WINE, SatPanel.Passes)
@SatLocales @Composable private fun WineDetail() = SatPreview(ThemeMode.WINE, detail = true)

@Preview(name = "Spatial · Persian 130%", locale = "fa", widthDp = 340, heightDp = 730, fontScale = 1.3f)
@Composable private fun SpatialTimeCompact() = SatPreview(ThemeMode.DARK, SatPanel.Time, skin = SatSkin.SPATIAL)

@Preview(name = "Aether · Persian 130%", locale = "fa", widthDp = 340, heightDp = 730, fontScale = 1.3f)
@Composable private fun AetherDetailCompact() = SatPreview(ThemeMode.LIGHT, detail = true, skin = SatSkin.AETHER)

@SatLocales @Composable private fun SpatialDarkGlobe() = SatPreview(ThemeMode.DARK, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialDarkTime() = SatPreview(ThemeMode.DARK, SatPanel.Time, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialDarkPasses() = SatPreview(ThemeMode.DARK, SatPanel.Passes, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialDarkDetail() = SatPreview(ThemeMode.DARK, detail = true, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialLightGlobe() = SatPreview(ThemeMode.LIGHT, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialLightTime() = SatPreview(ThemeMode.LIGHT, SatPanel.Time, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialLightPasses() = SatPreview(ThemeMode.LIGHT, SatPanel.Passes, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialLightDetail() = SatPreview(ThemeMode.LIGHT, detail = true, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialWineGlobe() = SatPreview(ThemeMode.WINE, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialWineTime() = SatPreview(ThemeMode.WINE, SatPanel.Time, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialWinePasses() = SatPreview(ThemeMode.WINE, SatPanel.Passes, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun SpatialWineDetail() = SatPreview(ThemeMode.WINE, detail = true, skin = SatSkin.SPATIAL)
@SatLocales @Composable private fun AetherDarkGlobe() = SatPreview(ThemeMode.DARK, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherDarkTime() = SatPreview(ThemeMode.DARK, SatPanel.Time, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherDarkPasses() = SatPreview(ThemeMode.DARK, SatPanel.Passes, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherDarkDetail() = SatPreview(ThemeMode.DARK, detail = true, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherLightGlobe() = SatPreview(ThemeMode.LIGHT, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherLightTime() = SatPreview(ThemeMode.LIGHT, SatPanel.Time, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherLightPasses() = SatPreview(ThemeMode.LIGHT, SatPanel.Passes, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherLightDetail() = SatPreview(ThemeMode.LIGHT, detail = true, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherWineGlobe() = SatPreview(ThemeMode.WINE, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherWineTime() = SatPreview(ThemeMode.WINE, SatPanel.Time, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherWinePasses() = SatPreview(ThemeMode.WINE, SatPanel.Passes, skin = SatSkin.AETHER)
@SatLocales @Composable private fun AetherWineDetail() = SatPreview(ThemeMode.WINE, detail = true, skin = SatSkin.AETHER)

private val tz = TimeZone.getTimeZone("Europe/London")
private val now = Calendar.getInstance(tz).apply { clear(); set(2025, Calendar.NOVEMBER, 17, 18, 42) }.timeInMillis
private val location = UserLocation(cityNameEn = "London", cityNameFa = "لندن", latitude = 51.5072, longitude = -.1276,
    timezoneId = tz.id, countryCode = "GB")
private const val MINUTE = 60_000L

private fun samplePass(minutes: Int, height: Double, score: Int) = ISSEngine.ISSPass(
    startTimeMs = now + minutes * MINUTE, maxTimeMs = now + (minutes + 3) * MINUTE, endTimeMs = now + (minutes + 6) * MINUTE,
    maxElevationDeg = height, maxAltitudeKm = 418.0, maxAzimuthDeg = 320.0, startAzimuthDeg = 225.0, endAzimuthDeg = 45.0,
    estimatedMagnitude = -3.4, passDurationSec = 360, sunAltitudeDegAtMax = -12.0, isObserverInDarkness = true,
    isIssSunlitAtMax = true, shadowEntryMs = now + (minutes + 5) * MINUTE, classification = ISSEngine.PassClassification.GOOD,
    visibilityScore = score, summaryReasonEn = "", summaryReasonFa = "", detailedReasonsEn = emptyList(),
    detailedReasonsFa = emptyList(), isVisible = true,
)

/** Fixed inputs and the production Compose UI; only the unavailable EGL surface is substituted. */
@Composable
internal fun SatPreview(theme: ThemeMode, panel: SatPanel = SatPanel.None, detail: Boolean = false, skin: SatSkin = SatSkin.DEFAULT,
    preview: Boolean = false, detailPass: Boolean = false, selectedAgendaPass: Boolean = false) {
    val fa = LocalConfiguration.current.locales[0]?.language == "fa"
    val tokens = remember(skin, theme) { zigSatTokens(skin, theme) }
    val live = remember {
        SatelliteLiveState(SatelliteCatalog.getById("iss_zarya"),
            ISSEngine.TopocentricPosition(34.0, 225.0, 679.0, 25.7, 18.2, 418.0, 7.66, true, 0.0, 0.0, 0.0),
            -3.4, true, "Naked eye visible", "قابل مشاهده با چشم",
            "The sunlit station is above your horizon, against a dark sky.", "ایستگاه روشن، بالای افق و در آسمان تاریک دیده می‌شود.")
    }
    val passes = remember(fa) {
        listOf(samplePass(134, 68.0, 78), samplePass(229, 39.0, 65), samplePass(1440, 52.0, 73), samplePass(2880, 40.0, 58))
            .map { SatPassUi(passKey(live.satellite, it), live.satellite, shortNameOf(live.satellite, fa), it, SatNights.nightKey(it.startTimeMs, tz), null, false) }
    }
    REDTheme(themeMode = theme, isPersian = fa, timestampMs = now) {
        SpatialThemeProvider(enabled = skin == SatSkin.SPATIAL, themeMode = theme) {
            AetherThemeProvider(enabled = skin == SatSkin.AETHER, themeMode = theme) {
                CompositionLocalProvider(LocalLayoutDirection provides if (fa) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    LocalZigSatTokens provides tokens, LocalSatSkin provides skin, LocalSatReduceMotion provides true) {
                    if (detail) SatelliteDetailContent(live.satellite, live, location, fa, skin, theme, true, null,
                        passes.map { it.pass }, if (detailPass) passes.first().pass else null, emptyList(), now, now, false, {}, { _, _, _ -> }, null, {}, {})
                    else Box(Modifier.fillMaxSize().background(tokens.canvas)) {
                        val actions = SatPassActions({}, {}, {}, {})
                        var state by remember(panel, preview) { mutableStateOf(SatellitesUiState(activePanel = panel,
                            timeOffsetMin = if (panel == SatPanel.Time && !preview) 70 else 0,
                            previewAtMs = if (preview) now + 2 * 24 * 60 * MINUTE else null)) }
                        val displayTime = satelliteDisplayTime(state, now)
                        val offset = ((displayTime - now) / MINUTE).toInt()
                        val markers = listOf(SatTimeMarker(137, SatQuality.EXCELLENT), SatTimeMarker(232, SatQuality.VERY_GOOD))
                        val setOffset: (Int) -> Unit = { state = state.copy(timeOffsetMin = it.coerceIn(-720, 720), previewAtMs = null) }
                        SatelliteHud(state.copy(timeOffsetMin = offset), SatStatusData(!satelliteIsPreview(state), offset, true, .8, now - 69_120_000L, TleDataSource.LIVE_NETWORK, fa),
                            tokens, SatAlertsConfig(), live, passes.first(), now, tz, markers, false, false,
                            SatTimeActions(setOffset, { setOffset(0) }, { nextSatelliteTime(markers, offset)?.let(setOffset) }, { setOffset(offset + it) }),
                            onPanel = { state = state.copy(activePanel = if (state.activePanel == it) SatPanel.None else it) },
                            onClose = { state = state.copy(activePanel = SatPanel.None) }, onLive = { setOffset(0) },
                            onLocate = {}, onCycle = {}, onDetail = {}, onLighting = {}, onLegend = {},
                            observerName = if (fa) location.cityNameFa else location.cityNameEn, onObserver = {},
                            displayTimeMs = displayTime, onReturnToPass = if (preview) ({}) else null,
                            globe = { projection, _, _ -> PreviewEarth(projection, tokens) },
                            sheet = { current, side -> if (current == SatPanel.Passes) SatPassesSheet(tokens, fa, state.scope, passes.size,
                                SatNights.group(passes, now, tz, fa), SatNights.spotlight(passes, now, tz, fa), now, now,
                                side, { state = state.copy(activePanel = SatPanel.None) }, {}, {}, { actions }, { Modifier }, tz,
                                selectedPassKey = if (selectedAgendaPass) passes.first().key else null) })
                        if (state.activePanel != SatPanel.Passes) {
                            val modifier = Modifier.align(Alignment.BottomCenter)
                            when (skin) {
                                SatSkin.DEFAULT -> FloatingBottomBar(1, {}, modifier)
                                SatSkin.SPATIAL -> SpatialFloatingNavBar(1, {}, modifier)
                                SatSkin.AETHER -> AetherFloatingNavBar(1, {}, modifier)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewEarth(projection: EarthViewportProjection, tokens: ZigSatTokens) {
    val image = ImageBitmap.imageResource(if (tokens.isDark) R.drawable.satellite_preview_night else R.drawable.satellite_preview_day)
    Canvas(Modifier.fillMaxSize()) {
        val r = Earth3DMath.projectedGlobeRadiusPx(SATELLITE_EARTH_DEFAULT_DISTANCE, size.height, projection.focalScale)
        val at = Offset(size.width / 2, size.height * projection.centerY)
        val d = (2 * r).roundToInt().coerceAtLeast(1)
        drawImage(image, dstOffset = IntOffset((at.x - r).roundToInt(), (at.y - r).roundToInt()), dstSize = IntSize(d, d))
        drawCircle(tokens.starlink.copy(alpha = .30f), r, at, style = Stroke(1.dp.toPx()))
        fun px(x: Float, y: Float) = at + Offset(x * r, y * r)
        val path = Path().apply {
            val start = px(-.94f, .47f)
            moveTo(start.x, start.y)
            val control = px(-.45f, .95f)
            val control2 = px(.54f, .12f)
            val end = px(.95f, -.62f)
            cubicTo(control.x, control.y, control2.x, control2.y, end.x, end.y)
        }
        drawPath(path, tokens.live.copy(alpha = .28f), style = Stroke(4.dp.toPx()))
        drawPath(path, tokens.live, style = Stroke(1.35.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
        val marker = px(.15f, .28f)
        drawCircle(tokens.selectedHalo.copy(alpha = .42f), 9.dp.toPx(), marker)
        drawCircle(Color.White, 3.dp.toPx(), marker)
        val you = px(-.18f, -.43f)
        drawCircle(tokens.starlink, 5.dp.toPx(), you, style = Stroke(1.dp.toPx())); drawCircle(tokens.starlink, 2.dp.toPx(), you)
        listOf(px(-.47f, .48f), px(.33f, -.26f), px(.19f, -.13f)).forEach { drawCircle(tokens.starlink.copy(alpha = .72f), 2.1.dp.toPx(), it) }
    }
}
