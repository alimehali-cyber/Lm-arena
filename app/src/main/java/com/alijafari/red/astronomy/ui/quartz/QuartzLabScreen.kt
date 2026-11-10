package com.alijafari.red.astronomy.ui.quartz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.screens.LabFeatureType
import com.alijafari.red.astronomy.ui.screens.TimeDilationCalculatorScreen
import com.zig.antar.ui.AntarRoot
import com.zig.gravity.ui.GravitySandboxRoot

/**
 * "OBSIDIAN QUARTZ & CRIMSON TELEMETRY" — Astrophysics Lab.
 *
 * The same single-column feature stack as every other Lab variant, dressed as four precision
 * instruments: each engine is a smoked-quartz plate with a dotted inner track, a dot-matrix index,
 * a hairline sparkline signature (drawn per engine) and a vector chevron. No Material icons.
 */
@Composable
fun QuartzLabScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedFeature by rememberSaveable { mutableStateOf<LabFeatureType?>(null) }
    val haptics = LocalHapticFeedback.current

    BackHandler(enabled = selectedFeature != null) {
        selectedFeature = null
    }

    when (selectedFeature) {
        LabFeatureType.TIME_DILATION -> TimeDilationCalculatorScreen(
            uiState = uiState,
            onBackToLab = { selectedFeature = null },
            modifier = modifier
        )
        LabFeatureType.GRAVITY_SANDBOX -> GravitySandboxRoot(
            onBack = { selectedFeature = null },
            modifier = modifier,
            startInPersian = isFa,
            startInDarkTheme = uiState.themeMode != com.alijafari.red.astronomy.domain.ThemeMode.LIGHT
        )
        LabFeatureType.ZIG_BLACKHOLE -> AntarRoot(
            onBack = { selectedFeature = null },
            language = uiState.language,
            uiStyle = uiState.uiStyle,
            themeMode = uiState.themeMode,
            modifier = modifier
        )
        null -> LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("quartz_lab_scroll"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "quartz_lab_header") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "آزمایشگاه اخترفیزیک" else "Astrophysics Lab",
                            color = palette.inkPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.4).sp,
                            modifier = Modifier.testTag("lab_title")
                        )
                        QuartzDotMatrix(
                            value = "0${LabFeatureType.entries.size}",
                            dotSize = 2.0.dp,
                            dotGap = 1.0.dp,
                            charGap = 3.4.dp,
                            color = palette.inkSecondary,
                            glow = false
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = if (isFa) {
                            "اتساع زمان، گرانش چندجسمی و خمش فضازمان"
                        } else {
                            "Lorentz, N-body and Kerr metric solvers"
                        },
                        color = palette.inkTertiary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(16.dp))
                    QuartzHairline(
                        color = palette.hairline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                }
            }

            itemsIndexed(LabFeatureType.entries, key = { _, f -> f.name }) { idx, feature ->
                QuartzLabPlate(
                    index = idx + 1,
                    feature = feature,
                    isFa = isFa,
                    onClick = {
                        if (feature.isAvailable) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFeature = feature
                        }
                    },
                    modifier = Modifier.testTag("quartz_lab_engine_${feature.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun QuartzLabPlate(
    index: Int,
    feature: LabFeatureType,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = QuartzTheme.palette
    val radius = 30.dp
    val accent = when (index % 3) {
        1 -> palette.accentRose
        2 -> palette.accentCobalt
        else -> palette.accentAmber
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF15181D).copy(alpha = 0.97f),
                        palette.quartzTop.copy(alpha = 0.95f),
                        palette.quartzMid.copy(alpha = 0.92f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1100f, 900f)
                )
            )
            .opticalQuartzSurface(
                cornerRadius = radius,
                refractionColor = palette.quartzRefraction,
                refractionReach = 18.dp,
                refractionAlpha = 0.22f,
                rimHighlightAlpha = 0.44f,
                grainAlpha = palette.grainAlpha
            )
            .quartzInnerDottedTrack(cornerRadius = radius, color = Color.White.copy(alpha = 0.16f), inset = 7.dp)
            .quartzTapTarget(onClick = onClick, enabled = feature.isAvailable)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuartzDotMatrix(
                        value = "0$index",
                        dotSize = 2.2.dp,
                        dotGap = 1.1.dp,
                        charGap = 3.6.dp,
                        color = accent,
                        glow = true,
                        ghost = 0.08f
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 26.dp, height = 0.8.dp)
                            .background(Color.White.copy(alpha = 0.22f))
                    )
                    Text(
                        text = if (feature.isAvailable) (if (isFa) "فعال" else "READY") else (if (isFa) "در دست ساخت" else "LOCKED"),
                        color = if (feature.isAvailable) Color.White.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.34f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.8.sp
                    )
                }
                QuartzGlyphDisc(
                    glyph = QuartzGlyph.ArrowUpRight,
                    color = Color.White.copy(alpha = 0.86f),
                    diameter = 34.dp,
                    glyphSize = 11.dp
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (isFa) feature.titleFa else feature.titleEn,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isFa) feature.subtitleFa else feature.subtitleEn,
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))

            // The plate's signature: a hairline trace unique to the engine it launches.
            QuartzEngineSignature(
                index = index,
                color = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            )
        }
    }
}

/** Per-engine hairline signature: geodesic, orbit ellipse, photon ring, or orbital decay. */
@Composable
private fun QuartzEngineSignature(
    index: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val breathe by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 180f),
        label = "quartz_signature"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val midY = h * 0.62f
        val stroke = 1.1.dp.toPx()
        drawLine(
            color = color.copy(alpha = 0.16f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 0.7.dp.toPx()
        )
        when (index % 4) {
            1 -> {
                // Lorentz curve: a photon-bending geodesic sweeping into the horizon.
                val path = Path().apply {
                    moveTo(0f, midY)
                    cubicTo(w * 0.3f, midY, w * 0.42f, h * 0.06f, w, h * 0.06f)
                }
                drawPath(path, color.copy(alpha = 0.88f * breathe), style = Stroke(width = stroke, cap = StrokeCap.Round))
                drawPath(path, Color.White.copy(alpha = 0.20f), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(color.copy(alpha = 0.5f), radius = 2.2.dp.toPx(), center = Offset(w * 0.42f, h * 0.06f))
            }
            2 -> {
                // N-body: an ellipse with two mass nodes.
                drawOval(
                    color = color.copy(alpha = 0.88f),
                    topLeft = Offset(w * 0.08f, h * 0.18f),
                    size = Size(w * 0.62f, h * 0.66f),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
                )
                drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.dp.toPx(), center = Offset(w * 0.39f, h * 0.18f))
                drawCircle(color, radius = 3.4.dp.toPx(), center = Offset(w * 0.70f, midY))
                drawCircle(color.copy(alpha = 0.25f), radius = 7.dp.toPx(), center = Offset(w * 0.70f, midY))
            }
            3 -> {
                // Kerr photon ring: concentric arcs behind a dark horizon.
                for (ring in 0..3) {
                    drawCircle(
                        color = color.copy(alpha = 0.8f - ring * 0.17f),
                        radius = (h * 0.34f) + ring * 3.2.dp.toPx(),
                        center = Offset(w * 0.24f, midY),
                        style = Stroke(width = stroke * (1f - ring * 0.12f))
                    )
                }
                drawCircle(Color.Black.copy(alpha = 0.65f), radius = h * 0.26f, center = Offset(w * 0.24f, midY))
                drawLine(
                    color = Color.White.copy(alpha = 0.30f),
                    start = Offset(w * 0.42f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 0.7.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 5f))
                )
            }
            else -> {
                // Orbital decay: a damped sinusoid.
                val path = Path().apply {
                    moveTo(0f, midY)
                    val steps = 90
                    for (i in 1..steps) {
                        val x = (i.toFloat() / steps) * w
                        val damp = 1f - (i.toFloat() / steps) * 0.82f
                        val y = midY - kotlin.math.sin(i * 0.42f) * (h * 0.30f) * damp
                        lineTo(x, y)
                    }
                }
                drawPath(path, color.copy(alpha = 0.88f), style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
        // Right-hand axis ticks: the instrument's scale.
        for (tick in 0..4) {
            val x = w - tick * 6.dp.toPx() - 1.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(x, h - 1.dp.toPx()),
                end = Offset(x, h - 5.dp.toPx() * (1f - tick * 0.16f)),
                strokeWidth = 0.7.dp.toPx()
            )
        }
    }
}
