package com.alijafari.red.astronomy.ui.aether

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.zig.antar.ui.AntarRoot
import com.zig.gravity.ui.GravitySandboxRoot
import kotlin.math.cos
import kotlin.math.sin

/**
 * Aether Telemetry UI — Astrophysics Lab Screen:
 * Showcases all interactive physics engines inside Frameless Rim-Lit Jewel Telemetry Cards
 * (`RoundedCornerShape(36.dp)`, 1.dp directional specular rim, custom Compose `Canvas`
 * telemetry waveforms, 60-tick orbital gauges, Dot-Matrix counters, and minimal pill badges).
 */
@Composable
fun AetherLabScreen(
    uiState: MainUiState,
    @Suppress("UNUSED_PARAMETER") viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = AetherTheme.palette
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
            val engineCount = LabFeatureType.entries.size.toString().padStart(2, '0')
            val capsuleLabel = if (isFa) {
                "آزمایشگاه فیزیک  •  ${TimeEngine.formatPersianNumbers(engineCount)} موتور فعال"
            } else {
                "Physics Telemetry  •  $engineCount Engines Ready"
            }

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("lab_screen"),
                contentPadding = PaddingValues(horizontal = RedSpacing.lg, vertical = RedSpacing.md),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item(key = "aether_lab_capsule") {
                    AetherFloatingStatusCapsule(text = capsuleLabel)
                }

                item(key = "aether_lab_header") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isFa) "آزمایشگاه اخترفیزیک" else "Astrophysics Lab",
                            style = RedTypographyTokens.sectionHeading.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-0.6).sp
                            ),
                            color = palette.textPrimary,
                            modifier = Modifier.testTag("lab_title")
                        )
                        Text(
                            text = if (isFa) {
                                "محاسبه اتساع زمان نسبیتی، گرانش چندجسمی و سیاه‌چاله چرخان کر"
                            } else {
                                "Relativistic Lorentz, Newtonian N-body & Kerr geodesic telemetry"
                            },
                            style = RedTypographyTokens.caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Light
                            ),
                            color = palette.textSecondary
                        )
                    }
                }

                itemsIndexed(LabFeatureType.entries, key = { _, f -> f.name }) { idx, feature ->
                    AetherLabJewelCard(
                        index = idx + 1,
                        feature = feature,
                        isFa = isFa,
                        onClick = {
                            if (feature.isAvailable) {
                                selectedFeature = feature
                            }
                        }
                    )
                }

                item(key = "aether_lab_bottom_spacer") {
                    Spacer(modifier = Modifier.height(112.dp))
                }
            }
        }
    }
}

@Composable
private fun AetherLabJewelCard(
    index: Int,
    feature: LabFeatureType,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val (jewelTone, formulaMetric, modeLabel) = when (feature) {
        LabFeatureType.TIME_DILATION -> Triple(
            AetherJewelTone.CRIMSON,
            "γ = 1/√(1−v²/c²)",
            if (isFa) "نسبیت خاص و عام" else "SR / GR Metric"
        )
        LabFeatureType.GRAVITY_SANDBOX -> Triple(
            AetherJewelTone.SAPPHIRE,
            "F = G·m₁m₂/r²",
            if (isFa) "سیمپلتیک چندجسمی" else "Symplectic N-Body"
        )
        LabFeatureType.ZIG_BLACKHOLE -> Triple(
            AetherJewelTone.EMERALD,
            "a/M = 0.998 Kerr",
            if (isFa) "پرتویابی ژئودزیک" else "Geodesic Raytracer"
        )
    }

    val indexDigits = remember(index, isFa) {
        val raw = index.toString().padStart(2, '0')
        if (isFa) TimeEngine.formatPersianNumbers(raw) else raw
    }

    AetherJewelCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lab_feature_card_${feature.name.lowercase()}"),
        jewelTone = jewelTone,
        cornerRadius = 36.dp,
        contentPadding = PaddingValues(24.dp),
        elevation = 26.dp,
        onClick = if (feature.isAvailable) onClick else null
    ) {
        // Card Title (17.sp Medium, -0.3.sp) + Mode Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isFa) feature.titleFa else feature.titleEn,
                style = RedTypographyTokens.sectionTitle.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.3).sp
                ),
                color = Color.White.copy(alpha = 0.96f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AetherLegendDotItem(
                label = if (isFa) "فعال" else "Ready",
                filled = feature.isAvailable
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dot-Matrix Index + Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.Top) {
                AetherRollingDotMatrixCounter(
                    valueText = indexDigits,
                    dotSize = 2.75.dp,
                    dotGap = 1.0.dp,
                    charGap = 3.6.dp,
                    color = AetherGlowWhite,
                    glowColor = jewelTone.edgeColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = modeLabel,
                        style = RedTypographyTokens.caption.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color.White.copy(alpha = 0.80f)
                    )
                    Text(
                        text = if (isFa) feature.descriptionFa else feature.descriptionEn,
                        style = RedTypographyTokens.caption.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Light
                        ),
                        color = Color.White.copy(alpha = 0.58f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Custom Compose Canvas Telemetry Visualization per Simulator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
        ) {
            when (feature) {
                LabFeatureType.TIME_DILATION -> {
                    AetherLedDotArray(
                        totalDots = 5,
                        litCount = 4,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    AetherOscilloscopeWaveformCanvas(
                        activeWaves = 4,
                        progress = 0.88f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
                LabFeatureType.GRAVITY_SANDBOX -> {
                    AetherKeplerTelemetryCanvas(
                        accentColor = jewelTone.edgeColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LabFeatureType.ZIG_BLACKHOLE -> {
                    AetherKerrTelemetryCanvas(
                        accentColor = jewelTone.edgeColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Secondary Formula Readout + Minimal 1.dp Pill Launch Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formulaMetric,
                style = RedTypographyTokens.sectionHeading.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.6).sp
                ),
                color = Color.White.copy(alpha = 0.94f)
            )

            AetherPillBadge(
                text = if (isFa) "اجرا ←" else "Launch →",
                selected = true,
                onClick = if (feature.isAvailable) onClick else null
            )
        }
    }
}

@Composable
private fun AetherKeplerTelemetryCanvas(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.24f
        val cy = size.height * 0.52f
        val r = size.height * 0.44f

        // 36 radial orbital ticks + elliptical trajectory
        for (i in 0 until 36) {
            val rad = Math.toRadians((i * 10.0) - 90.0)
            val inner = if (i in 4..9) r * 0.68f else r * 0.88f
            drawLine(
                color = Color.White.copy(alpha = if (i in 4..9) 0.92f else 0.42f),
                start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                end = Offset(cx + (r * cos(rad)).toFloat(), cy + (r * sin(rad)).toFloat()),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        drawOval(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(size.width * 0.44f, size.height * 0.16f),
            size = Size(size.width * 0.50f, size.height * 0.68f),
            style = Stroke(width = 1.3.dp.toPx())
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.85f), Color.Transparent),
                center = Offset(size.width * 0.62f, cy),
                radius = 12.dp.toPx()
            ),
            radius = 12.dp.toPx(),
            center = Offset(size.width * 0.62f, cy)
        )
        drawCircle(color = AetherGlowWhite, radius = 4.dp.toPx(), center = Offset(size.width * 0.62f, cy))
        drawCircle(
            color = AetherGlowWhite,
            radius = 2.8.dp.toPx(),
            center = Offset(size.width * 0.88f, cy - size.height * 0.18f)
        )
    }
}

@Composable
private fun AetherKerrTelemetryCanvas(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.25f
        val cy = size.height * 0.50f
        val ringR = size.height * 0.36f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.65f), Color.Transparent),
                center = Offset(cx, cy),
                radius = ringR * 1.8f
            ),
            radius = ringR * 1.8f,
            center = Offset(cx, cy)
        )
        drawOval(
            color = Color.White.copy(alpha = 0.80f),
            topLeft = Offset(cx - ringR * 2.1f, cy - ringR * 0.28f),
            size = Size(ringR * 4.2f, ringR * 0.56f),
            style = Stroke(width = 2.2.dp.toPx())
        )
        drawCircle(color = Color(0xFF04060A), radius = ringR * 0.72f, center = Offset(cx, cy))
        drawCircle(
            color = AetherGlowWhite,
            radius = ringR * 0.76f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Trailing Doppler redshift/blueshift baseline on the right
        val startX = size.width * 0.52f
        val endX = size.width * 0.98f
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(startX, cy),
            end = Offset(endX, cy),
            strokeWidth = 1.2.dp.toPx()
        )
        drawCircle(
            color = AetherGlowWhite,
            radius = 3.5.dp.toPx(),
            center = Offset(startX + (endX - startX) * 0.64f, cy)
        )
    }
}
