package com.alijafari.red.astronomy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.theme.*
import com.alijafari.red.astronomy.ui.theme.squircleShape

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
    GRAVITY_SANDBOX(
        // The card's title is the feature's name and nothing else: no subtitle folded into it, no
        // product prefix. The N-body framing already lives in subtitleEn/subtitleFa below, which is
        // where a description belongs.
        titleEn = "Gravity Sandbox",
        titleFa = "میز گرانش",
        subtitleEn = "Newton-Kepler Orbital Simulator",
        subtitleFa = "آزمایشگاه مکانیک سماوی و مدارهای کپلری",
        descriptionEn = "Simulate multi-body gravitational interactions, planetary orbits, binary stars, Lagrange equilibrium points, and black hole slingshots.",
        descriptionFa = "شبیه‌سازی برهم‌کنش‌های گرانشی چندجرمی، مدارهای سیاره‌ای، سامانه‌های دوستاره‌ای، نقاط لاگرانژی و قلاب گرانشی سیاهچاله.",
        icon = Icons.Default.Public,
        isAvailable = true
    ),
    GARGANTUA(
        titleEn = "Gargantua",
        titleFa = "گارگانتوا",
        subtitleEn = "Relativistic Black Hole Laboratory",
        subtitleFa = "آزمایشگاه نسبیتی سیاه‌چاله",
        descriptionEn = "Interactive Kerr metric black hole laboratory: curved spacetime ray-marching, accretion disk relativistic beaming, gravitational redshift, and photon orbits.",
        descriptionFa = "آزمایشگاه تعاملی سیاه‌چاله چرخان کر: ردیابی پرتو در فضازمان خمیده، درخشش نسبیتی قرص برافزایشی، انتقال به سرخ گرانشی و مدار فوتون‌ها.",
        icon = Icons.Default.FilterTiltShift,
        isAvailable = true
    ),
    CHAL(
        titleEn = "Chal",
        titleFa = "چال",
        subtitleEn = "Interactive Black Hole Visualizer",
        subtitleFa = "نمایشگر تعاملی سیاه‌چاله",
        descriptionEn = "Explore a rotating black hole, accretion disk and lensing effects. Uses native rendering where supported, with an explicitly approximate OpenGL compatibility mode. Rendered physics are not independently validated.",
        descriptionFa = "کاوش تعاملی سیاه‌چالهٔ چرخان، قرص برافزایشی و همگرایی نور. رندر بومی در دستگاه‌های سازگار و حالت تخمینی OpenGL در سایر دستگاه‌ها فعال است. دقت فیزیکی تصویر به‌طور مستقل تأیید نشده است.",
        icon = Icons.Default.BlurCircular,
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
            // The screen owns the status-bar inset now that the app has no shared top band (Part 6,
            // item 8): the list starts below the clock, and keeps doing so on every device.
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("lab_screen"),
            contentPadding = PaddingValues(horizontal = RedSpacing.lg, vertical = RedSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RedSpacing.lg)
        ) {
            // The screen's title, and nothing else: no banner card introducing a list of tools that
            // already name and describe themselves. It is plain text on the screen's own surface, in the
            // theme's own ink, so it reads in every theme and over every backdrop instead of sitting in a
            // panel that exists only to repeat the word "Lab" above a list of Lab tools.
            item {
                Text(
                    text = if (isFa) "آزمایشگاه اخترفیزیک" else "Astrophysics Lab",
                    style = RedTypographyTokens.sectionHeading,
                    color = RedTheme.colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RedSpacing.xs, vertical = RedSpacing.xs)
                        .testTag("lab_title")
                )
            }

            // Section Title
            item {
                RedSectionHeader(
                    title = if (isFa) "ابزارهای علمی در دسترس" else "Available Scientific Tools",
                    subtitle = if (isFa) "شبیه‌سازها و ماشین‌حساب‌های اخترفیزیک" else "Astrophysics calculators & simulators"
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

            // Bottom spacing for floating navigation bar
            item {
                Spacer(modifier = Modifier.height(112.dp))
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
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(squircleShape(RedCornerRadius.xl))
            .clickable(enabled = feature.isAvailable, onClick = onClick)
            .testTag("lab_feature_card_${feature.name.lowercase()}"),
        shape = squircleShape(RedCornerRadius.xl),
        fallbackColor = if (feature.isAvailable) RedTheme.colors.surfaceElevated else RedTheme.colors.surfaceGrouped,
        fallbackBorder = BorderStroke(
            width = 1.dp,
            color = if (feature.isAvailable) RedTheme.colors.border else RedTheme.colors.border.copy(alpha = 0.5f)
        ),
        style = LiquidGlassDefaults.Card,
        fallbackShadowElevation = RedElevation.none,
    ) {
        Row(
            modifier = Modifier.padding(RedSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RedSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (feature.isAvailable) RedTheme.colors.accentRed.copy(alpha = 0.12f)
                        else RedTheme.colors.surfaceGrouped
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = if (feature.isAvailable) RedTheme.colors.accentRed else RedTheme.colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(RedIconSize.md)
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
                        color = if (feature.isAvailable) RedTheme.colors.textPrimary else RedTheme.colors.textSecondary
                    )

                    if (!feature.isAvailable) {
                        RedBadge(
                            text = if (isFa) "به زودی" else "Coming Soon",
                            backgroundColor = RedTheme.colors.surfaceGrouped,
                            textColor = RedTheme.colors.textSecondary,
                            borderColor = RedTheme.colors.border
                        )
                    }
                }

                Text(
                    text = if (isFa) feature.subtitleFa else feature.subtitleEn,
                    style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (feature.isAvailable) RedTheme.colors.accentRed else RedTheme.colors.textSecondary
                )

                Text(
                    text = if (isFa) feature.descriptionFa else feature.descriptionEn,
                    style = RedTypographyTokens.bodySecondary,
                    color = RedTheme.colors.textSecondary,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = if (feature.isAvailable) RedTheme.colors.accentRed else RedTheme.colors.textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(RedIconSize.md)
            )
        }
    }
}
