package com.alijafari.red.astronomy.ui.cyber

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cyber Glass UI Astrophysics Lab Screen:
 * Showcases all 4 interactive physics simulators inside thick-cut `36.dp` Spatial Glass 2.0
 * **Cutout Viewport Window Cards** (`CyberCutoutViewportCard`), pairing crisp unblurred procedural
 * relativistic & orbital viewport renders with Nothing Dot-Matrix counters, ultra-light geometric
 * typography, and sub-frosted Mesh Gradient Pill launch buttons.
 */
@Composable
fun CyberLabScreen(
    uiState: MainUiState,
    @Suppress("UNUSED_PARAMETER") viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = CyberTheme.palette
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
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("lab_screen"),
                contentPadding = PaddingValues(horizontal = RedSpacing.lg, vertical = RedSpacing.md),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item(key = "cyber_lab_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "آزمایشگاه اخترفیزیک" else "Astrophysics Lab",
                                style = RedTypographyTokens.sectionHeading.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 24.sp
                                ),
                                color = palette.textPrimary,
                                modifier = Modifier.testTag("lab_title")
                            )

                            CyberFloatingCapsuleBar(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                elevation = 6.dp,
                                isActive = true
                            ) {
                                CyberSignalDot(size = 5.5.dp)
                                CyberMatrixText(
                                    text = "0${LabFeatureType.entries.size} LAB",
                                    dotSize = 1.65.dp,
                                    dotGap = 0.65.dp,
                                    charGap = 1.9.dp,
                                    activeColor = palette.matrixLit,
                                    glowColor = palette.primaryAccent
                                )
                            }
                        }

                        CyberSectionHeader(
                            title = if (isFa) "موتورهای شبیه‌ساز فیزیک و نسبیت" else "Relativity & N-Body Simulation Engines",
                            subtitle = if (isFa) {
                                "محاسبه دقیق اتساع زمان، گرانش چندجسمی، سیاه‌چاله کر و خمش فضازمان"
                            } else {
                                "Interactive Lorentz, Newtonian N-body, Kerr photon ring & 3D metric solvers"
                            }
                        )
                    }
                }

                itemsIndexed(LabFeatureType.entries, key = { _, f -> f.name }) { idx, feature ->
                    CyberLabViewportCard(
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

                item(key = "cyber_lab_bottom_spacer") {
                    Spacer(modifier = Modifier.height(112.dp))
                }
            }
        }
    }
}

@Composable
private fun CyberLabViewportCard(
    index: Int,
    feature: LabFeatureType,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val palette = CyberTheme.palette
    val (meshPreset, accent, matrixTag) = when (feature) {
        LabFeatureType.TIME_DILATION -> Triple(CyberMeshPreset.CORAL_PEACH, palette.secondaryAccent, "0$index SR-GR")
        LabFeatureType.GRAVITY_SANDBOX -> Triple(CyberMeshPreset.CYAN_VIOLET, palette.primaryAccent, "0$index NBODY")
        LabFeatureType.ZIG_BLACKHOLE -> Triple(CyberMeshPreset.CORAL_PEACH, palette.secondaryAccent, "0$index ZIG")
    }

    CyberCutoutViewportCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lab_feature_card_${feature.name.lowercase()}"),
        viewportHeight = 116.dp,
        isCapsuleCutout = true,
        ambientTint = accent,
        onClick = if (feature.isAvailable) onClick else null,
        viewportContent = {
            CyberLabProceduralViewport(
                feature = feature,
                primaryColor = accent,
                secondaryColor = palette.secondaryAccent
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CyberMatrixText(
                text = matrixTag,
                dotSize = 1.95.dp,
                dotGap = 0.78.dp,
                charGap = 2.3.dp,
                showMatrixGrid = true,
                activeColor = palette.matrixLit,
                glowColor = accent
            )

            Text(
                text = if (isFa) feature.subtitleFa else feature.subtitleEn,
                style = RedTypographyTokens.caption.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 11.5.sp
                ),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isFa) feature.titleFa else feature.titleEn,
            style = RedTypographyTokens.sectionHeading.copy(
                fontWeight = FontWeight.Light,
                fontSize = 19.sp
            ),
            color = palette.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isFa) feature.descriptionFa else feature.descriptionEn,
            style = RedTypographyTokens.bodySecondary.copy(
                fontWeight = FontWeight.Light,
                fontSize = 13.sp
            ),
            color = palette.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isFa) "آماده اجرا" else "Interactive Engine",
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 11.sp
                    ),
                    color = palette.textMuted
                )
            }

            CyberMeshPillCard(
                preset = meshPreset,
                isCapsule = true,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                onClick = if (feature.isAvailable) onClick else null
            ) {
                Text(
                    text = if (isFa) "اجرای شبیه‌ساز" else "Launch",
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp
                    ),
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Crisp unblurred procedural astrophysics artwork rendered directly inside each Lab card's
 * Cutout Viewport Window.
 */
@Composable
private fun CyberLabProceduralViewport(
    feature: LabFeatureType,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        // Subtle starlight backdrop inside the cutout aperture
        for (i in 0 until 20) {
            val fx = (((sin(i * 23.17 + 1.3) * 43758.5) % 1.0) + 1.0) % 1.0
            val fy = (((sin(i * 59.41 + 2.7) * 29184.3) % 1.0) + 1.0) % 1.0
            drawCircle(
                color = Color.White.copy(alpha = 0.18f + (i % 3) * 0.08f),
                radius = 1.05f,
                center = Offset((fx * w).toFloat(), (fy * h).toFloat())
            )
        }

        when (feature) {
            LabFeatureType.TIME_DILATION -> {
                // Minkowski Light-Cone & Relativistic Proper-Time Wavefronts
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = h * 0.65f
                    ),
                    radius = h * 0.65f,
                    center = Offset(cx, cy)
                )
                for (ring in 1..4) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.55f / ring),
                        radius = ring * (h * 0.105f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
                drawLine(
                    color = secondaryColor.copy(alpha = 0.65f),
                    start = Offset(cx - w * 0.28f, h * 0.85f),
                    end = Offset(cx + w * 0.28f, h * 0.15f),
                    strokeWidth = 1.4.dp.toPx()
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.65f),
                    start = Offset(cx - w * 0.28f, h * 0.15f),
                    end = Offset(cx + w * 0.28f, h * 0.85f),
                    strokeWidth = 1.4.dp.toPx()
                )
                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = Offset(cx, cy))
            }

            LabFeatureType.GRAVITY_SANDBOX -> {
                // Keplerian N-Body Ellipses & Orbiting Masses
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryColor.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = h * 0.42f
                    ),
                    radius = h * 0.42f,
                    center = Offset(cx, cy)
                )
                drawOval(
                    color = primaryColor.copy(alpha = 0.48f),
                    topLeft = Offset(cx - w * 0.30f, cy - h * 0.30f),
                    size = Size(w * 0.60f, h * 0.60f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawOval(
                    color = secondaryColor.copy(alpha = 0.38f),
                    topLeft = Offset(cx - w * 0.19f, cy - h * 0.20f),
                    size = Size(w * 0.38f, h * 0.40f),
                    style = Stroke(width = 1.1.dp.toPx())
                )
                drawCircle(color = secondaryColor, radius = 6.dp.toPx(), center = Offset(cx, cy))
                drawCircle(
                    color = primaryColor,
                    radius = 4.2.dp.toPx(),
                    center = Offset(cx + w * 0.24f, cy - h * 0.16f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.0.dp.toPx(),
                    center = Offset(cx - w * 0.16f, cy + h * 0.13f)
                )
            }

            LabFeatureType.ZIG_BLACKHOLE -> {
                // Kerr–Schild D-Shadow, n=1 Photon Sub-Ring, Lensed Halo & Polar Relativistic Jet
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.52f),
                            secondaryColor.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = h * 0.62f
                    ),
                    radius = h * 0.62f,
                    center = Offset(cx, cy)
                )
                // Polar Blandford-Znajek relativistic jet spine
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryColor.copy(alpha = 0.72f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(cx, h * 0.06f),
                    end = Offset(cx, h * 0.94f),
                    strokeWidth = 2.0.dp.toPx()
                )
                // Lensed upper & lower Einstein photon arch
                drawCircle(
                    color = primaryColor.copy(alpha = 0.75f),
                    radius = h * 0.30f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                // Beamed equatorial accretion disk (brighter approaching side on the left)
                drawOval(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            primaryColor,
                            secondaryColor.copy(alpha = 0.35f)
                        ),
                        startX = cx - w * 0.36f,
                        endX = cx + w * 0.36f
                    ),
                    topLeft = Offset(cx - w * 0.36f, cy - h * 0.08f),
                    size = Size(w * 0.72f, h * 0.16f),
                    style = Stroke(width = 3.8.dp.toPx())
                )
                // Event horizon + crisp n=1 photon ring
                drawCircle(color = Color(0xFF04050B), radius = h * 0.21f, center = Offset(cx + h * 0.02f, cy))
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = h * 0.22f,
                    center = Offset(cx + h * 0.015f, cy),
                    style = Stroke(width = 1.4.dp.toPx())
                )
            }
        }
    }
}
