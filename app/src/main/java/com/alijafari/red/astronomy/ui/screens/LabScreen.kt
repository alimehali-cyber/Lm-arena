package com.alijafari.red.astronomy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.theme.AccentPrimary

enum class LabFeatureType(
    val titleEn: String,
    val titleFa: String,
    val subtitleEn: String,
    val subtitleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val icon: ImageVector,
    val isAvailable: Boolean
) {
    TIME_DILATION(
        titleEn = "Time Dilation",
        titleFa = "انقباض زمان و نسبیت",
        subtitleEn = "Relativistic Journey Calculator",
        subtitleFa = "محاسبه‌گر سفرهای بین‌ستاره‌ای نسبیتی",
        descriptionEn = "Simulate relativistic time dilation, proper time, Lorentz factor, and length contraction for interstellar voyages.",
        descriptionFa = "محاسبه و شبیه‌سازی انقباض زمان آینشتاین، زمان اختصاصی، عامل لورنتس و انقباض طول در سفرهای بین‌ستاره‌ای.",
        icon = Icons.Default.HourglassTop,
        isAvailable = true
    ),
    ORBITAL_RESONANCE(
        titleEn = "Orbital Resonance & Keplerian Elements",
        titleFa = "رزونانس مداری و عناصر کپلری",
        subtitleEn = "Celestial Mechanics Tool",
        subtitleFa = "مکانیک سماوی و شبیه‌ساز مدارها",
        descriptionEn = "Analyze gravitational orbital harmonics, Hill spheres, Lagrange points, and orbital resonances.",
        descriptionFa = "تحلیل رزونانس‌های گرانشی، نقاط لاگرانژی و دامنه‌های هیل در اجرام منظومه شمسی.",
        icon = Icons.Default.AllInclusive,
        isAvailable = false
    ),
    STELLAR_EVOLUTION(
        titleEn = "HR Diagram & Stellar Lifetime",
        titleFa = "نمودار هرتسپرونگ-راسل و تکامل ستارگان",
        subtitleEn = "Astrophysical Classifier",
        subtitleFa = "اخترفیزیک و حیات ستاره‌ای",
        descriptionEn = "Plot main sequence stars, red giants, white dwarfs, and compute nuclear fusion lifetimes.",
        descriptionFa = "رسم و تحلیل نمودار H-R، جایگاه تکاملی ستارگان و طول عمر همجوشی هسته‌ای.",
        icon = Icons.Default.AutoAwesome,
        isAvailable = false
    )
}

@Composable
fun LabScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedFeature by remember { mutableStateOf<LabFeatureType?>(null) }

    if (selectedFeature == LabFeatureType.TIME_DILATION) {
        TimeDilationCalculatorScreen(
            uiState = uiState,
            onBackToLab = { selectedFeature = null },
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("lab_screen"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lab Header Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lab_header_card"),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AccentPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isFa) "آزمایشگاه نجومی و فیزیک" else "Astrophysics Lab",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isFa) "مجموعه ابزارهای علمی و محاسبه‌گرهای نجومی" else "Scientific tools & computational simulators",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = if (isFa)
                                "آزمایشگاه نجومی محیطی برای آزمایش فرضیه‌ها، محاسبات نسبیتی، مکانیک سماوی و شبیه‌سازی‌های پیشرفته است."
                            else
                                "An expandable suite of advanced computational astrophysics tools and relativistic physics simulators.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Section Title
            item {
                Text(
                    text = if (isFa) "ابزارهای فعال و در حال توسعه" else "Available Scientific Tools",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Feature List Cards
            items(LabFeatureType.entries) { feature ->
                LabFeatureCard(
                    feature = feature,
                    isFa = isFa,
                    onClick = {
                        if (feature.isAvailable) {
                            selectedFeature = feature
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LabFeatureCard(
    feature: LabFeatureType,
    isFa: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = feature.isAvailable, onClick = onClick)
            .testTag("lab_feature_card_${feature.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        color = if (feature.isAvailable) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            width = if (feature.isAvailable) 1.5.dp else 1.dp,
            color = if (feature.isAvailable) AccentPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (feature.isAvailable) AccentPrimary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = if (feature.isAvailable) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (feature.isAvailable) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!feature.isAvailable) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isFa) "به زودی" else "Coming Soon",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (isFa) feature.subtitleFa else feature.subtitleEn,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = AccentPrimary
                )

                Text(
                    text = if (isFa) feature.descriptionFa else feature.descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = if (feature.isAvailable) AccentPrimary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
