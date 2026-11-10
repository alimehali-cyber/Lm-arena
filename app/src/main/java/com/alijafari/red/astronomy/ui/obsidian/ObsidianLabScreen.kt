package com.alijafari.red.astronomy.ui.obsidian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
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
import com.zig.antar.ui.AntarRoot
import com.zig.gravity.ui.GravitySandboxRoot

/**
 * Obsidian Quartz & Crimson Telemetry — Astrophysics Lab.
 *
 * Same feature host contract as the Default/Spatial/Cyber lab screens (Time Dilation,
 * Gravity Sandbox, Antar black hole), dressed as smoked-quartz plates: each row is a
 * capsule lens with a 6.dp inset dashed track, a Canvas-drawn engine mark, and a LED
 * dot-matrix engine code. No Material icons, no flat borders.
 */
@Composable
fun ObsidianLabScreen(
    uiState: MainUiState,
    @Suppress("UNUSED_PARAMETER") viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = ObsidianTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedFeature by rememberSaveable { mutableStateOf<LabFeatureType?>(null) }

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
            startInDarkTheme = uiState.themeMode != ThemeMode.LIGHT
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
                .testTag("lab_screen"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (isFa) "آزمایشگاه اخترفیزیک" else "Astrophysics Lab",
                        color = palette.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-0.6).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lab_title")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianSignalDot(modifier = Modifier.size(5.dp))
                        Text(
                            text = if (isFa) "ابزارهای علمی در دسترس" else "AVAILABLE SCIENTIFIC TOOLS",
                            color = palette.textPrimary.copy(alpha = 0.55f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.6.sp
                        )
                    }
                }
            }

            items(LabFeatureType.entries) { feature ->
                ObsidianLabFeaturePlate(
                    feature = feature,
                    isFa = isFa,
                    onClick = {
                        if (feature.isAvailable) selectedFeature = feature
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
private fun ObsidianLabFeaturePlate(
    feature: LabFeatureType,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val palette = ObsidianTheme.palette

    // Localized capsule accents only: rose, cobalt, amber. No neon salad.
    val (accent, base, engineCode) = when (feature) {
        LabFeatureType.TIME_DILATION -> Triple(ObsidianRose, Color(0xFF281924), "SR")
        LabFeatureType.GRAVITY_SANDBOX -> Triple(ObsidianCobalt, Color(0xFF192030), "NB")
        LabFeatureType.ZIG_BLACKHOLE -> Triple(ObsidianAmber, Color(0xFF2D1912), "KS")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.62f),
                        base.copy(alpha = 0.94f)
                    ),
                    center = androidx.compose.ui.geometry.Offset(90f, 40f),
                    radius = 420f
                )
            )
            .opticalQuartzSurface(
                cornerRadius = 30.dp,
                innerRefractionColor = Color.White.copy(alpha = 0.3f),
                rimHighlightAlpha = 0.52f
            )
            .obsidianInnerDottedTrack(cornerRadius = 30.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = feature.isAvailable,
                onClick = onClick
            )
            .padding(18.dp)
            .testTag("lab_feature_card_${feature.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 44.dp hairline mark slot, drawn as vector geometry on a transparent plate.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                ObsidianVectorIconGlyph(
                    icon = when (feature) {
                        LabFeatureType.TIME_DILATION -> ObsidianVectorIcon.LAB
                        LabFeatureType.GRAVITY_SANDBOX -> ObsidianVectorIcon.CROSSHAIR
                        LabFeatureType.ZIG_BLACKHOLE -> ObsidianVectorIcon.SKY
                    },
                    modifier = Modifier.size(20.dp),
                    tint = Color.White.copy(alpha = 0.92f),
                    strokeWidth = 1.2.dp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isFa) feature.titleFa else feature.titleEn,
                        color = Color.White.copy(alpha = if (feature.isAvailable) 0.96f else 0.6f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-0.2).sp
                    )
                    DotMatrixNumber(
                        value = engineCode,
                        dotSize = 1.55.dp,
                        dotSpacing = 0.6.dp,
                        charSpacing = 1.6.dp,
                        glow = false
                    )
                }

                Text(
                    text = if (isFa) feature.subtitleFa else feature.subtitleEn,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Text(
                    text = if (isFa) feature.descriptionFa else feature.descriptionEn,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 2
                )
            }

            ObsidianVectorIconGlyph(
                icon = ObsidianVectorIcon.EXPAND,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(-90f),
                tint = if (feature.isAvailable) Color.White.copy(alpha = 0.9f) else palette.textMuted,
                strokeWidth = 1.2.dp
            )
        }
    }
}
