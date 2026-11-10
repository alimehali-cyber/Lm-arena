package com.alijafari.red.astronomy.ui.aether

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.screens.LabFeatureType
import com.alijafari.red.astronomy.ui.screens.TimeDilationCalculatorScreen
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.zig.antar.ui.AntarRoot
import com.zig.gravity.ui.GravitySandboxRoot
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val BETA_PRESETS = floatArrayOf(0.15f, 0.45f, 0.70f, 0.87f, 0.96f, 0.99f)

private fun String.aetherDigits(isFa: Boolean): String =
    if (isFa) TimeEngine.formatPersianNumbers(this) else this

@Composable
fun AetherLabScreen(
    uiState: MainUiState,
    @Suppress("UNUSED_PARAMETER") viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedFeature by rememberSaveable { mutableStateOf<LabFeatureType?>(null) }

    BackHandler(enabled = selectedFeature != null) {
        selectedFeature = null
    }

    when (selectedFeature) {
        LabFeatureType.TIME_DILATION -> {
            TimeDilationCalculatorScreen(
                uiState = uiState,
                onBackToLab = { selectedFeature = null },
                modifier = modifier
            )
        }
        LabFeatureType.GRAVITY_SANDBOX -> {
            GravitySandboxRoot(
                onBack = { selectedFeature = null },
                modifier = modifier,
                startInPersian = isFa,
                startInDarkTheme = uiState.themeMode != ThemeMode.LIGHT
            )
        }
        LabFeatureType.ZIG_BLACKHOLE -> {
            AntarRoot(
                onBack = { selectedFeature = null },
                language = uiState.language,
                uiStyle = uiState.uiStyle,
                themeMode = uiState.themeMode,
                modifier = modifier
            )
        }
        null -> {
            AetherLabMainDeck(
                uiState = uiState,
                onOpenFeature = { selectedFeature = it },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AetherLabMainDeck(
    uiState: MainUiState,
    onOpenFeature: (LabFeatureType) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AetherTheme.palette
    val appFont = LocalAppFontFamily.current
    val isFa = uiState.language == AppLanguage.PERSIAN
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var betaIndex by rememberSaveable { mutableIntStateOf(3) } // default 0.87c -> gamma ~ 2.03
    val beta = BETA_PRESETS[betaIndex.coerceIn(0, BETA_PRESETS.lastIndex)]
    val gamma = remember(beta) { 1.0 / sqrt((1.0 - beta * beta).coerceAtLeast(0.005)) }
    val properTimePct = remember(gamma) { ((1.0 / gamma) * 100.0).roundToInt().coerceIn(1, 100) }
    val activeLeds = remember(betaIndex) { betaIndex.coerceIn(1, 5) }

    val features = LabFeatureType.entries

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("lab_screen"),
        contentPadding = PaddingValues(
            top = (statusTopPadding + 14.dp).coerceAtLeast(24.dp),
            bottom = navBottomPadding + 106.dp,
            start = 16.dp,
            end = 16.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. FLOATING TOP STATUS CAPSULE + LAB HEADER
        item(key = "aether_lab_header") {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val capsuleLabel = if (isFa) {
                        "آزمایشگاه نسبیت • ۰۳ موتور فعال"
                    } else {
                        "Relativistic Lab • 03 Engines"
                    }
                    AetherFloatingStatusCapsule(
                        statusText = capsuleLabel,
                        onClick = { betaIndex = 3 }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFa) "آزمایشگاه اخترفیزیک" else "Astrophysics Lab",
                            style = RedTypographyTokens.screenTitle.copy(
                                fontFamily = appFont,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = if (isFa) 0.sp else (-0.4).sp
                            ),
                            color = palette.textPrimary,
                            modifier = Modifier.testTag("lab_title")
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isFa) {
                                "شبیه‌سازهای تعاملی نسبیت خاص، مکانیک سماوی و سیاه‌چاله کر"
                            } else {
                                "Interactive relativistic, orbital N-body & Kerr black-hole engines"
                            },
                            style = RedTypographyTokens.bodySecondary.copy(
                                fontFamily = appFont,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Light
                            ),
                            color = palette.textMicro
                        )
                    }
                    AetherDotMatrixText(
                        text = String.format(Locale.US, "%02d", features.size).aetherDigits(isFa),
                        dotSize = 3.0.dp,
                        dotGap = 1.05.dp,
                        charGap = 3.0.dp,
                        activeColor = palette.glowWhite,
                        glowColor = palette.primaryAccent
                    )
                }
            }
        }

        // 2. HERO FRAMELESS JEWEL LORENTZ TELEMETRY CARD (36.dp CRIMSON LENS)
        item(key = "aether_lab_hero_lorentz") {
            val gammaStr = String.format(Locale.US, "%.2f", gamma).aetherDigits(isFa)
            val betaStr = String.format(Locale.US, "%.2f c", beta).aetherDigits(isFa)

            AetherJewelTelemetryCard(
                preset = AetherJewelPreset.CRIMSON,
                cornerRadius = AetherCornerGeometry.heroCard,
                contentPadding = PaddingValues(24.dp),
                onClick = { onOpenFeature(LabFeatureType.TIME_DILATION) },
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = if (isFa) "ضریب لورنتس (γ)" else "Lorentz Factor",
                            style = RedTypographyTokens.sectionTitle.copy(
                                fontFamily = appFont,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = if (isFa) 0.sp else (-0.3).sp
                            ),
                            color = Color.White.copy(alpha = 0.90f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AetherRollingDotMatrixCounter(
                            text = gammaStr,
                            dotSize = 4.2.dp,
                            dotGap = 1.35.dp,
                            charGap = 4.2.dp,
                            activeColor = Color.White,
                            glowColor = AetherCrimsonCoreEdge
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        AetherLegendDotItem(
                            label = if (isFa) "زمان ویژه" else "Proper τ",
                            filled = true
                        )
                        AetherLegendDotItem(
                            label = if (isFa) "مرجع ناظر" else "Coord t",
                            filled = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AetherLedStatusArray(
                    litCount = activeLeds,
                    totalDots = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                AetherOscilloscopeCanvas(
                    activeWaves = activeLeds,
                    maxWaves = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    AetherRollingMetricText(
                        text = betaStr,
                        isPersian = isFa,
                        fontSize = 30.sp,
                        color = Color.White
                    )

                    AetherMicroStepper(
                        onDecrement = {
                            betaIndex = (betaIndex - 1).coerceIn(0, BETA_PRESETS.lastIndex)
                        },
                        onIncrement = {
                            betaIndex = (betaIndex + 1).coerceIn(0, BETA_PRESETS.lastIndex)
                        },
                        onReset = { betaIndex = 3 }
                    )
                }
            }
        }

        // 3. SPATIAL BENTO ROW: 60-TICK PROPER TIME DIAL + GLOWING MESH PILLS
        item(key = "aether_lab_bento") {
            val regimeLabel = when {
                beta >= 0.95f -> if (isFa) "فوق نسبیتی" else "Ultra-Rel"
                beta >= 0.65f -> if (isFa) "نسبیتی" else "Relativistic"
                else -> if (isFa) "کلاسیک" else "Sub-Rel"
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AetherRadialDialCard(
                    scoreText = "$properTimePct%".aetherDigits(isFa),
                    statusLabel = regimeLabel,
                    captionTop = if (isFa) "نسبت τ/t" else "Proper τ/t",
                    highlightFraction = properTimePct / 100f,
                    onClick = { onOpenFeature(LabFeatureType.TIME_DILATION) },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    AetherGlowingMeshPillCard(
                        title = if (isFa) "شتاب گرانشی" else "N-Body RK4",
                        value = "60".aetherDigits(isFa),
                        unit = "Hz",
                        badge = if (isFa) "مدار ↑" else "Sim ↑",
                        preset = AetherMeshPillPreset.PINK_ROSE,
                        onClick = { onOpenFeature(LabFeatureType.GRAVITY_SANDBOX) }
                    )

                    AetherGlowingMeshPillCard(
                        title = if (isFa) "افق رویداد کر" else "Kerr Horizon",
                        value = "2.0".aetherDigits(isFa),
                        unit = "Rs",
                        badge = if (isFa) "پرتو ↑" else "GLSL ↑",
                        preset = AetherMeshPillPreset.AMBER_COPPER,
                        onClick = { onOpenFeature(LabFeatureType.ZIG_BLACKHOLE) }
                    )
                }
            }
        }

        // 4. FRAMELESS JEWEL-TONE SIMULATION ENGINE CARDS
        itemsIndexed(features) { index, feature ->
            val preset = when (feature) {
                LabFeatureType.TIME_DILATION -> AetherJewelPreset.CRIMSON
                LabFeatureType.GRAVITY_SANDBOX -> AetherJewelPreset.SAPPHIRE
                LabFeatureType.ZIG_BLACKHOLE -> AetherJewelPreset.EMERALD
            }
            val splineSamples = remember(feature) {
                when (feature) {
                    LabFeatureType.TIME_DILATION -> listOf(0.18f, 0.24f, 0.34f, 0.48f, 0.68f, 0.92f)
                    LabFeatureType.GRAVITY_SANDBOX -> listOf(0.55f, 0.82f, 0.36f, 0.74f, 0.42f, 0.68f)
                    LabFeatureType.ZIG_BLACKHOLE -> listOf(0.20f, 0.38f, 0.88f, 0.96f, 0.44f, 0.22f)
                }
            }
            val metricCode = if (isFa) feature.subtitleFa else feature.subtitleEn
            val titleText = if (isFa) feature.titleFa else feature.titleEn
            val descText = if (isFa) feature.descriptionFa else feature.descriptionEn

            AetherJewelTelemetryCard(
                preset = preset,
                cornerRadius = AetherCornerGeometry.heroCard,
                contentPadding = PaddingValues(22.dp),
                elevation = 20.dp,
                onClick = { onOpenFeature(feature) },
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .testTag("lab_feature_card_${feature.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AetherLegendDotItem(
                            label = metricCode,
                            filled = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = titleText,
                            style = RedTypographyTokens.sectionTitle.copy(
                                fontFamily = appFont,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = if (isFa) 0.sp else (-0.3).sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = descText,
                            style = RedTypographyTokens.bodySecondary.copy(
                                fontFamily = appFont,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Light
                            ),
                            color = Color.White.copy(alpha = 0.74f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AetherDotMatrixText(
                        text = String.format(Locale.US, "%02d", index + 1).aetherDigits(isFa),
                        dotSize = 3.0.dp,
                        dotGap = 1.0.dp,
                        charGap = 3.0.dp,
                        activeColor = Color.White,
                        glowColor = preset.edgeGlowFor(uiState.themeMode)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AetherSplineTelemetryCanvas(
                        samples = splineSamples,
                        currentFraction = 0.68f,
                        glowColor = preset.edgeGlowFor(uiState.themeMode),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .padding(end = 16.dp)
                    )

                    AetherMinimalPillBadge(
                        text = if (isFa) "اجرا ←" else "Launch →"
                    )
                }
            }
        }
    }
}
