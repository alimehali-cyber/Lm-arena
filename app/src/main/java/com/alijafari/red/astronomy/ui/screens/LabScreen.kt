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
 */
@Composable
private fun LabFeatureTile(
    feature: LabFeatureType,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        modifier = modifier.testTag("lab_feature_card_${feature.name.lowercase()}"),
        shape = RoundedCornerShape(RedCornerRadius.tile),
        style = LiquidGlassDefaults.Card,
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
// Every tile carries a minimal picture of what the tool is — line and light only, no icon glyphs.
// The drawings share the glass ink so they stay legible in either theme: strokes use the surface's
// own contrast-safe text colors, and only the astrophysical light (disks, planets, streaks) carries
// accent color.
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

/**
 * Time dilation: a journey clock trailing speed streaks, with a small fast traveler and two stars —
 * motion and time, drawn as one tiny scene.
 */
@Composable
private fun TimeDilationArtwork(modifier: Modifier = Modifier) {
    val ink = RedTheme.colors.textPrimary
    val soft = RedTheme.colors.textTertiary
    val travel = RedTheme.colors.accentBlue
    val spark = RedTheme.colors.accentRed

    Canvas(modifier) {
        val s = size.minDimension
        val cx = center.x
        val cy = center.y
        val r = s * 0.27f

        // Speed streaks trailing to the left of the clock.
        listOf(-0.14f to 0.15f, 0.0f to 0.22f, 0.14f to 0.12f).forEach { (dy, len) ->
            drawLine(
                color = travel.copy(alpha = 0.65f),
                start = Offset(cx - s * 0.48f, cy + s * dy),
                end = Offset(cx - s * 0.48f + s * len, cy + s * dy),
                strokeWidth = s * 0.022f,
                cap = StrokeCap.Round
            )
        }

        // The clock: ring, quarter ticks and hands at ten past ten.
        drawCircle(
            color = ink.copy(alpha = 0.9f),
            radius = r,
            style = Stroke(width = s * 0.032f)
        )
        for (quarter in 0 until 4) {
            val angleRad = Math.toRadians(quarter * 90.0 - 90.0)
            val dir = Offset(cos(angleRad).toFloat(), sin(angleRad).toFloat())
            drawLine(
                color = ink.copy(alpha = 0.8f),
                start = Offset(cx + dir.x * r * 0.80f, cy + dir.y * r * 0.80f),
                end = Offset(cx + dir.x * r * 0.97f, cy + dir.y * r * 0.97f),
                strokeWidth = s * 0.024f,
                cap = StrokeCap.Round
            )
        }
        // Hour hand toward 10 o'clock, minute hand toward 2.
        listOf(-60.0 to 0.50f, 60.0 to 0.74f).forEach { (fromTwelve, reach) ->
            val angleRad = Math.toRadians(fromTwelve)
            val dir = Offset(sin(angleRad).toFloat(), -cos(angleRad).toFloat())
            drawLine(
                color = ink.copy(alpha = 0.9f),
                start = Offset(cx, cy),
                end = Offset(cx + dir.x * r * reach, cy + dir.y * r * reach),
                strokeWidth = s * 0.03f,
                cap = StrokeCap.Round
            )
        }

        // A fast traveler arcing past the clock, trailed by its own streak.
        val comet = Offset(cx + s * 0.34f, cy - s * 0.29f)
        drawLine(
            color = spark.copy(alpha = 0.55f),
            start = Offset(comet.x + s * 0.10f, comet.y + s * 0.10f),
            end = Offset(comet.x - s * 0.01f, comet.y - s * 0.01f),
            strokeWidth = s * 0.02f,
            cap = StrokeCap.Round
        )
        drawCircle(color = spark, radius = s * 0.034f, center = comet)

        // Two distant stars.
        drawCircle(color = soft.copy(alpha = 0.9f), radius = s * 0.015f, center = Offset(cx - s * 0.40f, cy - s * 0.34f))
        drawCircle(color = soft.copy(alpha = 0.9f), radius = s * 0.012f, center = Offset(cx + s * 0.16f, cy + s * 0.42f))
    }
}

/**
 * Gravity sandbox: a tiny orrery — a sun, two orbital paths, an outer planet with its moon and an
 * inner planet, seen slightly from above the orbital plane.
 */
@Composable
private fun GravitySandboxArtwork(modifier: Modifier = Modifier) {
    val ink = RedTheme.colors.textPrimary
    val soft = RedTheme.colors.textTertiary
    val sun = RedTheme.colors.accentRed
    val planet = RedTheme.colors.accentBlue
    val moon = RedTheme.colors.accentGold

    Canvas(modifier) {
        val s = size.minDimension
        val cx = center.x
        val cy = center.y
        val squash = 0.80f

        // Two orbital paths, squashed to hint a tilted tabletop view.
        drawOval(
            color = soft.copy(alpha = 0.75f),
            topLeft = Offset(cx - s * 0.20f, cy - s * 0.20f * squash),
            size = Size(s * 0.40f, s * 0.40f * squash),
            style = Stroke(width = s * 0.016f)
        )
        drawOval(
            color = soft.copy(alpha = 0.55f),
            topLeft = Offset(cx - s * 0.37f, cy - s * 0.37f * squash),
            size = Size(s * 0.74f, s * 0.74f * squash),
            style = Stroke(width = s * 0.016f)
        )

        // The sun with a faint corona ring.
        drawCircle(color = sun.copy(alpha = 0.22f), radius = s * 0.115f, style = Stroke(width = s * 0.03f))
        drawCircle(color = sun, radius = s * 0.072f)

        // Outer planet with a tiny moon beside it.
        val outerAngleRad = Math.toRadians(38.0)
        val planetCenter = Offset(
            cx + cos(outerAngleRad).toFloat() * s * 0.37f,
            cy + sin(outerAngleRad).toFloat() * s * 0.37f * squash
        )
        drawCircle(color = planet, radius = s * 0.045f, center = planetCenter)
        drawCircle(
            color = ink.copy(alpha = 0.85f),
            radius = s * 0.018f,
            center = Offset(planetCenter.x + s * 0.075f, planetCenter.y - s * 0.05f)
        )

        // Inner planet on the opposite side.
        val innerAngleRad = Math.toRadians(210.0)
        drawCircle(
            color = moon,
            radius = s * 0.032f,
            center = Offset(
                cx + cos(innerAngleRad).toFloat() * s * 0.20f,
                cy + sin(innerAngleRad).toFloat() * s * 0.20f * squash
            )
        )
    }
}

/** Shared space-black of the two black-hole shadows, so both read as a hole in either theme. */
private val BlackHoleShadow = Color(0xFF060609)

/** The hot inner light of an accretion disk — pale gold, brighter than the disk's own gold. */
private val DiskHotLight = Color(0xFFFFE7B0)

/**
 * Gargantua: the edge-on Interstellar silhouette — a black hole shadow with the far side of the
 * accretion disk lensed into a halo over the top, the fainter lensed arc below, and the near side
 * of the disk crossing in front.
 */
@Composable
private fun GargantuaArtwork(modifier: Modifier = Modifier) {
    val warm = RedTheme.colors.accentGold
    val soft = RedTheme.colors.textTertiary

    Canvas(modifier) {
        val s = size.minDimension
        val cx = center.x
        val cy = center.y
        val rH = s * 0.20f
        val haloTopLeft = Offset(cx - s * 0.40f, cy - s * 0.17f)
        val haloSize = Size(s * 0.80f, s * 0.34f)
        val diskLight = Brush.linearGradient(
            colors = listOf(warm.copy(alpha = 0.9f), DiskHotLight, warm.copy(alpha = 0.9f)),
            start = Offset(cx - s * 0.42f, cy),
            end = Offset(cx + s * 0.42f, cy)
        )

        // The faint lensed image of the far disk, under the hole.
        drawArc(
            color = warm.copy(alpha = 0.35f),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = haloTopLeft,
            size = haloSize,
            style = Stroke(width = s * 0.032f, cap = StrokeCap.Round)
        )

        // The shadow.
        drawCircle(color = BlackHoleShadow, radius = rH)

        // Photon glow, tight around the shadow.
        drawCircle(
            color = DiskHotLight.copy(alpha = 0.5f),
            radius = rH * 1.08f,
            style = Stroke(width = s * 0.018f)
        )

        // The halo: the back of the disk, lensed over the top of the shadow.
        drawArc(
            brush = diskLight,
            startAngle = 185f,
            sweepAngle = 170f,
            useCenter = false,
            topLeft = haloTopLeft,
            size = haloSize,
            style = Stroke(width = s * 0.042f, cap = StrokeCap.Round)
        )

        // The near side of the disk, crossing in front just below center.
        val bandY = cy + s * 0.035f
        val band = Path().apply {
            moveTo(cx - s * 0.44f, bandY - s * 0.012f)
            quadraticTo(cx, bandY + s * 0.055f, cx + s * 0.44f, bandY - s * 0.012f)
        }
        drawPath(
            path = band,
            brush = diskLight,
            style = Stroke(width = s * 0.05f, cap = StrokeCap.Round)
        )

        // Two distant stars.
        drawCircle(color = soft.copy(alpha = 0.9f), radius = s * 0.014f, center = Offset(cx - s * 0.42f, cy + s * 0.32f))
        drawCircle(color = soft.copy(alpha = 0.9f), radius = s * 0.011f, center = Offset(cx + s * 0.40f, cy - s * 0.36f))
    }
}

/**
 * Chal: a tilted Kerr look — the accretion disk swung around the shadow with its approaching side
 * brightened, a photon ring at the edge, and background stars warped into short lensed arcs.
 */
@Composable
private fun ChalArtwork(modifier: Modifier = Modifier) {
    val warm = RedTheme.colors.accentGold
    val flare = RedTheme.colors.accentRed
    val soft = RedTheme.colors.textTertiary

    Canvas(modifier) {
        val s = size.minDimension
        val cx = center.x
        val cy = center.y
        val rH = s * 0.185f

        // Lensed star arcs, tangent to a circle around the hole.
        for (i in 0 until 4) {
            drawArc(
                color = soft.copy(alpha = 0.8f),
                startAngle = i * 90f + 25f - 9f,
                sweepAngle = 18f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.40f, cy - s * 0.40f),
                size = Size(s * 0.80f, s * 0.80f),
                style = Stroke(width = s * 0.016f, cap = StrokeCap.Round)
            )
        }

        rotate(degrees = -18f, pivot = Offset(cx, cy)) {
            val diskTopLeft = Offset(cx - s * 0.42f, cy - s * 0.14f)
            val diskSize = Size(s * 0.84f, s * 0.28f)
            val diskLight = Brush.linearGradient(
                colors = listOf(flare, DiskHotLight, warm),
                start = Offset(cx - s * 0.42f, cy),
                end = Offset(cx + s * 0.42f, cy)
            )

            // Far half of the disk, behind the shadow.
            drawArc(
                brush = diskLight,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = diskTopLeft,
                size = diskSize,
                style = Stroke(width = s * 0.042f, cap = StrokeCap.Round)
            )

            // The shadow and its photon ring.
            drawCircle(color = BlackHoleShadow, radius = rH)
            drawCircle(
                color = DiskHotLight.copy(alpha = 0.55f),
                radius = rH * 1.07f,
                style = Stroke(width = s * 0.015f)
            )

            // Near half of the disk, in front of the shadow's lower lip.
            drawArc(
                brush = diskLight,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = diskTopLeft,
                size = diskSize,
                style = Stroke(width = s * 0.048f, cap = StrokeCap.Round)
            )
        }

        // Two distant stars.
        drawCircle(color = soft.copy(alpha = 0.9f), radius = s * 0.013f, center = Offset(cx - s * 0.36f, cy - s * 0.40f))
        drawCircle(color = soft.copy(alpha = 0.9f), radius = s * 0.011f, center = Offset(cx + s * 0.38f, cy + s * 0.38f))
    }
}
