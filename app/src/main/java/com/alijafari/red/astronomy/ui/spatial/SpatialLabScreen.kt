package com.alijafari.red.astronomy.ui.spatial

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.screens.LabFeatureType
import com.alijafari.red.astronomy.ui.screens.TimeDilationCalculatorScreen
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedIconSize
import com.alijafari.red.astronomy.ui.theme.RedSpacing
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.zig.antar.ui.AntarRoot
import com.zig.gravity.ui.GravitySandboxRoot

/**
 * VisionOS Spatial Glassmorphism Astrophysics Lab Screen — Exact Default [LabScreen] Grid Layout.
 *
 * Preserves the exact single-column `LazyColumn` hierarchy of `LabScreen` (`lab_title`,
 * section header, and full-width `LabFeatureType.entries` stack), dressing each feature card
 * in frosted translucent Spatial Glass, specular top-edge rim lighting, mesh gradient orbs,
 * and LED dot-matrix engine codes.
 */
@Composable
fun SpatialLabScreen(
    uiState: MainUiState,
    @Suppress("UNUSED_PARAMETER") viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedFeature by rememberSaveable { mutableStateOf<LabFeatureType?>(null) }

    if (selectedFeature == LabFeatureType.TIME_DILATION) {
        TimeDilationCalculatorScreen(
            uiState = uiState,
            onBackToLab = { selectedFeature = null },
            modifier = modifier
        )
    } else if (selectedFeature == LabFeatureType.GRAVITY_SANDBOX) {
        GravitySandboxRoot(
            onBack = { selectedFeature = null },
            modifier = modifier,
            startInPersian = isFa,
            startInDarkTheme = uiState.themeMode != ThemeMode.LIGHT
        )
    } else if (selectedFeature == LabFeatureType.ZIG_BLACKHOLE) {
        AntarRoot(
            onBack = { selectedFeature = null },
            language = uiState.language,
            uiStyle = uiState.uiStyle,
            themeMode = uiState.themeMode,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("lab_screen"),
            contentPadding = PaddingValues(horizontal = RedSpacing.lg, vertical = RedSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedSpacing.lg)
        ) {
            item {
                Text(
                    text = if (isFa) "آزمایشگاه اخترفیزیک" else "Astrophysics Lab",
                    style = RedTypographyTokens.sectionHeading,
                    color = palette.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.xs, vertical = RedSpacing.xs)
                        .testTag("lab_title")
                )
            }

            item {
                SpatialSectionHeader(
                    title = if (isFa) "ابزارهای علمی در دسترس" else "Available Scientific Tools",
                    subtitle = if (isFa) "شبیه‌سازها و ماشین‌حساب‌های اخترفیزیک" else "Astrophysics calculators & simulators"
                )
            }

            items(LabFeatureType.entries) { feature ->
                SpatialLabFeatureCard(
                    feature = feature,
                    isFa = isFa,
                    onClick = {
                        if (feature.isAvailable) {
                            selectedFeature = feature
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

@Composable
private fun SpatialLabFeatureCard(
    feature: LabFeatureType,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val palette = SpatialTheme.palette
    val (meshPreset, glowAccent, engineCode) = when (feature) {
        LabFeatureType.TIME_DILATION -> Triple(SpatialMeshPreset.ROSE_TO_ORANGE, SpatialNeonRose, "SR")
        LabFeatureType.GRAVITY_SANDBOX -> Triple(SpatialMeshPreset.BLUE_TO_PURPLE, SpatialSkyBlue, "NB")
        LabFeatureType.ZIG_BLACKHOLE -> Triple(SpatialMeshPreset.AURORA_PEACH, SpatialNeonRoseVibrant, "KS")
    }

    SpatialGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lab_feature_card_${feature.name.lowercase()}"),
        cornerRadius = RedCornerRadius.xl,
        contentPadding = PaddingValues(RedSpacing.lg),
        elevation = 14.dp,
        glowAccent = glowAccent,
        onClick = if (feature.isAvailable) onClick else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)
        ) {
            // Exact 44.dp leading icon slot upgraded to a Mesh Gradient Orb
            SpatialMeshPill(
                preset = meshPreset,
                isCapsule = true,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = if (meshPreset == SpatialMeshPreset.ROSE_TO_ORANGE || meshPreset == SpatialMeshPreset.AURORA_PEACH) {
                        Color(0xFF23111C)
                    } else {
                        Color.White
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(RedIconSize.md)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
                ) {
                    Text(
                        text = if (isFa) feature.titleFa else feature.titleEn,
                        style = RedTypographyTokens.sectionHeading.copy(fontSize = 17.sp),
                        color = if (feature.isAvailable) palette.textPrimary else palette.textSecondary
                    )

                    DotMatrixText(
                        text = engineCode,
                        dotSize = 1.55.dp,
                        dotGap = 0.6.dp,
                        charGap = 1.6.dp,
                        activeColor = palette.dotMatrixInk,
                        glowColor = glowAccent
                    )
                }

                Text(
                    text = if (isFa) feature.subtitleFa else feature.subtitleEn,
                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (feature.isAvailable) palette.accentInk else palette.textSecondary
                )

                Text(
                    text = if (isFa) feature.descriptionFa else feature.descriptionEn,
                    style = RedTypographyTokens.bodySecondary,
                    color = palette.textSecondary,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = if (feature.isAvailable) palette.accentInk else palette.textTertiary,
                modifier = Modifier.size(RedIconSize.md)
            )
        }
    }
}
