package com.alijafari.red.astronomy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The Lab features. Each one is presented exactly like the Home moon card and sky canvas card:
 * a glass picture tile carrying one minimal graphic that says what the tool is, plus its name.
 * No icon badges, no subtitles and no descriptions — the picture and the title do the naming.
 */
enum class LabFeatureType(
    val titleEn: String,
    val titleFa: String,
    val isAvailable: Boolean
) {
    TIME_DILATION(
        titleEn = "Time Dilation",
        titleFa = "انقباض زمان و نسبیت",
        isAvailable = true
    ),
    GRAVITY_SANDBOX(
        // The card's title is the feature's name and nothing else: no product prefix and no
        // subtitle folded into it.
        titleEn = "Gravity Sandbox",
        titleFa = "میز گرانش",
        isAvailable = true
    ),
    GARGANTUA(
        titleEn = "Gargantua",
        titleFa = "گارگانتوا",
        isAvailable = true
    ),
    CHAL(
        titleEn = "Chal",
        titleFa = "چال",
        isAvailable = true
    )
}

@Composable
fun LabScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    glassBackdrop: com.kyant.backdrop.Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedFeature by rememberSaveable { mutableStateOf<LabFeatureType?>(null) }

    if (selectedFeature == LabFeatureType.TIME_DILATION) {
        TimeDilationCalculatorScreen(
            uiState = uiState,
            onBackToLab = { selectedFeature = null },
            modifier = modifier
        )
    } else if (selectedFeature == LabFeatureType.GRAVITY_SANDBOX) {
        com.zig.gravity.ui.GravitySandboxRoot(
            onBack = { selectedFeature = null },
            modifier = modifier,
            startInPersian = isFa,
            startInDarkTheme = uiState.themeMode != com.alijafari.red.astronomy.domain.ThemeMode.LIGHT
        )
    } else if (selectedFeature == LabFeatureType.GARGANTUA) {
        com.zig.gargantua.ui.GargantuaRoot(
            onBack = { selectedFeature = null },
            modifier = modifier,
            startInPersian = isFa,
            startInDarkTheme = uiState.themeMode != com.alijafari.red.astronomy.domain.ThemeMode.LIGHT
        )
    } else if (selectedFeature == LabFeatureType.CHAL) {
        com.zig.chal.ui.ChalRoot(
            onBack = { selectedFeature = null },
            modifier = modifier,
            startInPersian = isFa
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("lab_screen"),
            contentPadding = PaddingValues(horizontal = RedSpacing.lg, vertical = RedSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedSpacing.lg)
        ) {
            // Section Title
            item {
                RedSectionHeader(
                    title = if (isFa) "ابزارهای علمی در دسترس" else "Available Scientific Tools",
                    subtitle = if (isFa) "شبیه‌سازها و ماشین‌حساب‌های اخترفیزیک" else "Astrophysics calculators & simulators"
                )
            }

            // The four feature tiles, two by two — the same glass picture-tile anatomy the Home
            // moon card and sky canvas card wear.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lab_feature_tiles"),
                    verticalArrangement = Arrangement.spacedBy(RedSpacing.md)
                ) {
                    LabFeatureType.entries.chunked(2).forEach { rowFeatures ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)
                        ) {
                            rowFeatures.forEach { feature ->
                                LabFeatureTile(
                                    feature = feature,
                                    isFa = isFa,
                                    glassBackdrop = glassBackdrop,
                                    onClick = {
                                        if (feature.isAvailable) {
                                            selectedFeature = feature
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing for floating navigation bar
            item {
                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

/**
 * One Lab feature, presented like the Home moon card: a glass square wearing the tile radius, the
 * feature's minimal graphic filling most of it, and just the name beneath. Nothing else rides on
 * the card — no icon badge, no subtitle, no description.
 *
 * With [glassBackdrop] the tile is REAL liquid glass: it refracts the app background behind it
 * (Live Sky, photo or theme color). Without it the tile renders the token fallback material.
 */
@Composable
private fun LabFeatureTile(
    feature: LabFeatureType,
    isFa: Boolean,
    glassBackdrop: com.kyant.backdrop.Backdrop?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        modifier = modifier.testTag("lab_feature_card_${feature.name.lowercase()}"),
        shape = RoundedCornerShape(RedCornerRadius.tile),
        style = LiquidGlassDefaults.Card,
        backdrop = glassBackdrop,
        fallbackColor = RedTheme.colors.surfaceElevated,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RedSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                LabFeatureGraphic(
                    feature = feature,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(RedSpacing.sm))

            Text(
                text = if (isFa) feature.titleFa else feature.titleEn,
                style = RedTypographyTokens.caption.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = if (feature.isAvailable) RedTheme.colors.textPrimary
                else RedTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Feature artwork
//
// Every tile carries a minimal picture of what the tool is — line art only, no icon glyphs. The
// drawings are monochrome: one glass ink with alpha steps plus solid black for shadows, so they
// stay legible in either theme and read as precise scientific diagrams, not illustrations.
// ------------------------------------------------------------------------------------------------

@Composable
private fun LabFeatureGraphic(
    feature: LabFeatureType,
    modifier: Modifier = Modifier
) {
    when (feature) {
        LabFeatureType.TIME_DILATION -> TimeDilationArtwork(modifier)
        LabFeatureType.GRAVITY_SANDBOX -> GravitySandboxArtwork(modifier)
        LabFeatureType.GARGANTUA -> GargantuaArtwork(modifier)
        LabFeatureType.CHAL -> ChalArtwork(modifier)
    }
}

/** Shared space-black of the black-hole and eclipse shadows — black is the monochrome dark. */
private val BlackHoleShadow = Color(0xFF060609)

/**
 * Time dilation: the textbook light clock — two mirrors with a photon bouncing between them while
 * the whole clock drifts. Single-ink line art; the physics is the picture.
 */
@Composable
private fun TimeDilationArtwork(modifier: Modifier = Modifier) {
    val ink = RedTheme.colors.textPrimary

    Canvas(modifier) {
        val s = size.minDimension
        val cx = center.x
        val cy = center.y
        val mirrorY = 0.23f * s
        val mirrorHalf = 0.27f * s

        // The two mirrors.
        drawLine(
            color = ink,
            start = Offset(cx - mirrorHalf, cy - mirrorY),
            end = Offset(cx + mirrorHalf, cy - mirrorY),
            strokeWidth = s * 0.022f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = ink,
            start = Offset(cx - mirrorHalf, cy + mirrorY),
            end = Offset(cx + mirrorHalf, cy + mirrorY),
            strokeWidth = s * 0.022f,
            cap = StrokeCap.Round
        )

        // The photon, bouncing between them — its vertices land exactly on the mirrors.
        val bounces = listOf(
            Offset(cx - mirrorHalf, cy - mirrorY),
            Offset(cx - mirrorHalf * 0.33f, cy + mirrorY),
            Offset(cx + mirrorHalf * 0.33f, cy - mirrorY),
            Offset(cx + mirrorHalf, cy + mirrorY)
        )
        val photonPath = Path().apply {
            moveTo(bounces[0].x, bounces[0].y)
            for (i in 1 until bounces.size) {
                lineTo(bounces[i].x, bounces[i].y)
            }
        }
        drawPath(
            path = photonPath,
            color = ink.copy(alpha = 0.75f),
            style = Stroke(width = s * 0.015f, cap = StrokeCap.Round)
        )
        drawCircle(
            color = ink,
            radius = s * 0.03f,
            center = Offset(
                (bounces[1].x + bounces[2].x) / 2f,
                (bounces[1].y + bounces[2].y) / 2f
            )
        )

        // The clock's velocity.
        val arrowY = cy + 0.43f * s
        val arrowTipX = cx + 0.24f * s
        drawLine(
            color = ink.copy(alpha = 0.55f),
            start = Offset(cx - 0.28f * s, arrowY),
            end = Offset(arrowTipX, arrowY),
            strokeWidth = s * 0.015f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = ink.copy(alpha = 0.55f),
            start = Offset(arrowTipX, arrowY),
            end = Offset(arrowTipX - 0.07f * s, arrowY - 0.05f * s),
            strokeWidth = s * 0.015f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = ink.copy(alpha = 0.55f),
            start = Offset(arrowTipX, arrowY),
            end = Offset(arrowTipX - 0.07f * s, arrowY + 0.05f * s),
            strokeWidth = s * 0.015f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Gravity sandbox: the two-body problem drawn Kepler-correct — the central mass sits at the
 * orbit's FOCUS, not its center, and the body's velocity vector is tangent to the ellipse.
 */
@Composable
private fun GravitySandboxArtwork(modifier: Modifier = Modifier) {
    val ink = RedTheme.colors.textPrimary

    Canvas(modifier) {
        val s = size.minDimension
        val cx = center.x
        val cy = center.y
        val rx = 0.36f * s
        val ry = 0.25f * s
        val focus = sqrt(rx * rx - ry * ry)
        val sun = Offset(cx - focus, cy)

        // The orbit.
        drawOval(
            color = ink.copy(alpha = 0.65f),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = s * 0.015f)
        )

        // The central mass, at the focus.
        drawCircle(color = ink, radius = s * 0.055f, center = sun)

        // The orbiting body and its velocity, tangent to the path.
        val t = Math.toRadians(55.0)
        val body = Offset(
            cx + rx * cos(t).toFloat(),
            cy - ry * sin(t).toFloat()
        )
        drawCircle(color = ink.copy(alpha = 0.9f), radius = s * 0.032f, center = body)
        val tangent = Offset(-rx * sin(t).toFloat(), -ry * cos(t).toFloat())
        val tangentLen = sqrt(tangent.x * tangent.x + tangent.y * tangent.y)
        val dir = Offset(tangent.x / tangentLen, tangent.y / tangentLen)
        val tip = Offset(body.x + dir.x * 0.15f * s, body.y + dir.y * 0.15f * s)
        drawLine(
            color = ink.copy(alpha = 0.8f),
            start = body,
            end = tip,
            strokeWidth = s * 0.014f,
            cap = StrokeCap.Round
        )
        val head = 0.055f * s
        drawLine(
            color = ink.copy(alpha = 0.8f),
            start = tip,
            end = Offset(tip.x - dir.x * head - dir.y * head * 0.6f, tip.y - dir.y * head + dir.x * head * 0.6f),
            strokeWidth = s * 0.014f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = ink.copy(alpha = 0.8f),
            start = tip,
            end = Offset(tip.x - dir.x * head + dir.y * head * 0.6f, tip.y - dir.y * head - dir.x * head * 0.6f),
            strokeWidth = s * 0.014f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Gargantua: the edge-on black hole — shadow, photon ring, the far side of the accretion disk
 * lensed over the top, the fainter lensed arc below, and the near side crossing in front. Doppler
 * beaming shows as an ink gradient along the disk: the approaching side is the brighter one.
 */
@Composable
private fun GargantuaArtwork(modifier: Modifier = Modifier) {
    val ink = RedTheme.colors.textPrimary

    Canvas(modifier) {
        val s = size.minDimension
        val cx = center.x
        val cy = center.y
        val rH = s * 0.20f
        val haloTopLeft = Offset(cx - s * 0.40f, cy - s * 0.17f)
        val haloSize = Size(s * 0.80f, s * 0.34f)
        val diskLight = Brush.linearGradient(
            colors = listOf(ink.copy(alpha = 0.2f), ink.copy(alpha = 0.95f), ink.copy(alpha = 0.2f)),
            start = Offset(cx - s * 0.42f, cy),
            end = Offset(cx + s * 0.42f, cy)
        )

        // The faint lensed image of the far disk, under the hole.
        drawArc(
            color = ink.copy(alpha = 0.28f),
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = haloTopLeft,
            size = haloSize,
            style = Stroke(width = s * 0.024f, cap = StrokeCap.Round)
        )

        // The shadow and its photon ring.
        drawCircle(color = BlackHoleShadow, radius = rH)
        drawCircle(
            color = ink.copy(alpha = 0.8f),
            radius = rH * 1.07f,
            style = Stroke(width = s * 0.012f)
        )

        // The halo: the back of the disk, lensed over the top of the shadow.
        drawArc(
            brush = diskLight,
            startAngle = 188f,
            sweepAngle = 164f,
            useCenter = false,
            topLeft = haloTopLeft,
            size = haloSize,
            style = Stroke(width = s * 0.030f, cap = StrokeCap.Round)
        )

        // The near side of the disk, crossing in front just below center.
        val band = Path().apply {
            moveTo(cx - s * 0.44f, cy + s * 0.023f)
            quadraticTo(cx, cy + s * 0.090f, cx + s * 0.44f, cy + s * 0.023f)
        }
        drawPath(
            path = band,
            brush = diskLight,
            style = Stroke(width = s * 0.034f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Chal: the lensing view — the shadow with its photon ring, and background stars drawn into short
 * tangential arcs around it (the Einstein-arc geometry), with two undisturbed stars for scale.
 */
@Composable
private fun ChalArtwork(modifier: Modifier = Modifier) {
    val ink = RedTheme.colors.textPrimary

    Canvas(modifier) {
        val s = size.minDimension
        val cx = center.x
        val cy = center.y
        val rH = s * 0.185f

        // Lensed star arcs: (angle degrees, radius fraction, ink alpha).
        listOf(
            Triple(35f, 0.40f, 0.75f),
            Triple(125f, 0.36f, 0.55f),
            Triple(215f, 0.42f, 0.8f),
            Triple(305f, 0.34f, 0.5f)
        ).forEach { (angle, radius, alpha) ->
            drawArc(
                color = ink.copy(alpha = alpha),
                startAngle = angle - 10f,
                sweepAngle = 20f,
                useCenter = false,
                topLeft = Offset(cx - radius * s, cy - radius * s),
                size = Size(radius * 2f * s, radius * 2f * s),
                style = Stroke(width = s * 0.014f, cap = StrokeCap.Round)
            )
        }

        // Two stars outside the lensing ring.
        drawCircle(
            color = ink.copy(alpha = 0.45f),
            radius = s * 0.012f,
            center = Offset(cx - s * 0.30f, cy - s * 0.32f)
        )
        drawCircle(
            color = ink.copy(alpha = 0.35f),
            radius = s * 0.010f,
            center = Offset(cx + s * 0.33f, cy + s * 0.30f)
        )

        // The shadow and its photon ring.
        drawCircle(color = BlackHoleShadow, radius = rH)
        drawCircle(
            color = ink.copy(alpha = 0.75f),
            radius = rH * 1.07f,
            style = Stroke(width = s * 0.011f)
        )
    }
}
