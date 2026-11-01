package com.alijafari.red.astronomy.ui.spatial

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.screens.LabFeatureType
import com.alijafari.red.astronomy.ui.screens.TimeDilationCalculatorScreen
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.zig.chal.ui.ChalRoot
import com.zig.gargantua.ui.GargantuaRoot
import com.zig.gravity.ui.GravitySandboxRoot
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * VisionOS Spatial Astrophysics Laboratory Screen.
 *
 * Combines an interactive relativistic Lorentz-factor radial dot-meter widget with modular
 * G2 squircle Bento simulator launchers for Gargantua, Chal, Gravity Sandbox, and Time Dilation.
 */
@Composable
fun SpatialLabScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedFeature by rememberSaveable { mutableStateOf<LabFeatureType?>(null) }

    when (selectedFeature) {
        LabFeatureType.TIME_DILATION -> {
            TimeDilationCalculatorScreen(
                uiState = uiState,
                onBackToLab = { selectedFeature = null },
                modifier = modifier
            )
            return
        }
        LabFeatureType.GRAVITY_SANDBOX -> {
            GravitySandboxRoot(
                onBack = { selectedFeature = null },
                modifier = modifier,
                startInPersian = isFa,
                startInDarkTheme = uiState.themeMode != ThemeMode.LIGHT
            )
            return
        }
        LabFeatureType.GARGANTUA -> {
            GargantuaRoot(
                onBack = { selectedFeature = null },
                modifier = modifier,
                startInPersian = isFa,
                startInDarkTheme = uiState.themeMode != ThemeMode.LIGHT
            )
            return
        }
        LabFeatureType.CHAL -> {
            ChalRoot(
                onBack = { selectedFeature = null },
                modifier = modifier,
                startInPersian = isFa
            )
            return
        }
        null -> Unit
    }

    // Interactive Quick Lorentz Sandbox State (beta = v/c in 0.00..0.995)
    var betaVelocity by remember { mutableFloatStateOf(0.86f) }
    val properTimeRatio = remember(betaVelocity) {
        sqrt((1.0 - betaVelocity.toDouble() * betaVelocity.toDouble()).coerceAtLeast(1e-5)).toFloat()
    }
    val lorentzGamma = remember(properTimeRatio) {
        (1f / properTimeRatio).coerceAtMost(99.9f)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("lab_screen"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Spatial Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFa) "آزمایشگاه اخترفیزیک" else "Astrophysics Lab",
                        style = RedTypographyTokens.sectionHeading.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = palette.textPrimary,
                        modifier = Modifier.testTag("lab_title")
                    )
                    Text(
                        text = if (isFa) "شبیه‌سازهای فضازمان، گرانش و نسبیت" else "Spacetime lensing, N-body orbits & relativity",
                        style = RedTypographyTokens.caption.copy(fontSize = 12.sp),
                        color = palette.textSecondary
                    )
                }

                SpatialGlowIconButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings),
                    onClick = { viewModel.setShowSettingsDialog(true) }
                )
            }
        }

        // 2. Interactive Relativistic Lorentz Gauge Bento Row
        item {
            val gammaRaw = String.format(Locale.US, "%.2fX", lorentzGamma)
            val gammaText = if (isFa) TimeEngine.formatPersianNumbers(gammaRaw) else gammaRaw
            val betaPctRaw = "${(betaVelocity * 100f).roundToInt()}%C"
            val betaPctText = if (isFa) TimeEngine.formatPersianNumbers(betaPctRaw) else betaPctRaw
            val clockPctRaw = "${(properTimeRatio * 100f).roundToInt()}%"
            val clockPctText = if (isFa) TimeEngine.formatPersianNumbers(clockPctRaw) else clockPctRaw

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Squircle Card: Circular Radial Dot Meter for Lorentz Time Dilation
                    SpatialGlassCard(
                        modifier = Modifier
                            .weight(1.08f)
                            .height(228.dp),
                        cornerRadius = 32.dp,
                        contentPadding = PaddingValues(15.dp),
                        elevation = 14.dp,
                        glowAccent = SpatialNeonRose,
                        onClick = { selectedFeature = LabFeatureType.TIME_DILATION }
                    ) {
                        Text(
                            text = if (isFa) "عامل لورنتس" else "LORENTZ FACTOR",
                            style = RedTypographyTokens.caption.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = palette.textTertiary
                        )
                        Text(
                            text = if (isFa) "اتساع زمان نسبیتی" else "Relativistic Gamma",
                            style = RedTypographyTokens.bodyPrimary.copy(fontSize = 13.5.sp),
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            SpatialRadialDotMeter(
                                progress = betaVelocity.coerceIn(0f, 0.995f),
                                centerValueText = gammaText,
                                subValueText = "γ = 1/√(1−β²)",
                                statusLabel = if (isFa) "ضربه برای ماشین‌حساب کامل" else "Tap for Voyage Planner",
                                diameter = 110.dp,
                                activeColor = SpatialNeonRose,
                                secondaryActiveColor = SpatialCosmicIris,
                                dotSize = 2.1.dp
                            )
                        }
                    }

                    // Right Stacked Mesh Gradient Pills
                    Column(
                        modifier = Modifier
                            .weight(0.92f)
                            .height(228.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SpatialMeshPill(
                            preset = SpatialMeshPreset.ROSE_TO_ORANGE,
                            cornerRadius = 28.dp,
                            contentPadding = PaddingValues(13.dp),
                            onClick = { selectedFeature = LabFeatureType.TIME_DILATION },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isFa) "سرعت فضاپیما" else "SHIP VELOCITY",
                                    style = RedTypographyTokens.caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xB823111C)
                                )
                                DotMatrixText(
                                    text = betaPctText,
                                    dotSize = 2.4.dp,
                                    activeColor = Color(0xFF23111C),
                                    glowColor = Color.White
                                )
                                Text(
                                    text = if (isFa) "کسری از سرعت نور" else "Fraction of light speed",
                                    style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                    color = Color(0xCC23111C),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        SpatialMeshPill(
                            preset = SpatialMeshPreset.BLUE_TO_PURPLE,
                            cornerRadius = 28.dp,
                            contentPadding = PaddingValues(13.dp),
                            onClick = { selectedFeature = LabFeatureType.TIME_DILATION },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isFa) "آهنگ ساعت فضاپیما" else "SHIP CLOCK RATE",
                                    style = RedTypographyTokens.caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                                DotMatrixText(
                                    text = clockPctText,
                                    dotSize = 2.4.dp,
                                    activeColor = Color.White,
                                    glowColor = SpatialSkyBlue
                                )
                                Text(
                                    text = if (isFa) "نسبت به ناظر زمینی" else "Proper time vs Earth",
                                    style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Live Velocity Scrubber Card
                SpatialGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    elevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "تنظیم سرعت نسبیتی (β = v/c)" else "Scrub Relativistic Velocity (β = v/c)",
                            style = RedTypographyTokens.caption.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = palette.textSecondary
                        )
                        val rawBeta = String.format(Locale.US, "%.3fC", betaVelocity)
                        DotMatrixText(
                            text = if (isFa) TimeEngine.formatPersianNumbers(rawBeta) else rawBeta,
                            dotSize = 1.9.dp,
                            activeColor = palette.dotMatrixInk,
                            glowColor = SpatialNeonRose
                        )
                    }
                    SpatialGlowSlider(
                        value = betaVelocity,
                        onValueChange = { betaVelocity = it.coerceIn(0.0f, 0.995f) },
                        valueRange = 0.0f..0.995f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Bento Simulators Section Header
        item {
            SpatialSectionHeader(
                title = if (isFa) "ماژول‌های شبیه‌ساز فضایی" else "Interactive Physics Modules",
                subtitle = if (isFa) "موتورهای رندر سیاه‌چاله و مکانیک مداری" else "Real-time GPU black holes & orbital mechanics",
                badgeText = if (isFa) "۴ آزمایشگاه" else "4 Engines"
            )
        }

        // 4. Featured Hero Bento Card: Gargantua Kerr Black Hole Laboratory
        item {
            SpatialHeroLabBentoCard(
                feature = LabFeatureType.GARGANTUA,
                isFa = isFa,
                accentGlow = SpatialWarmPeach,
                badgeCode = "KERR GR",
                onClick = { selectedFeature = LabFeatureType.GARGANTUA }
            )
        }

        // 5. Dual Square Bento Row: Gravity Sandbox + Chal Black Hole Visualizer
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SpatialCompactLabBentoTile(
                    feature = LabFeatureType.GRAVITY_SANDBOX,
                    isFa = isFa,
                    accentGlow = SpatialMintGlow,
                    codeLabel = "N-BODY",
                    onClick = { selectedFeature = LabFeatureType.GRAVITY_SANDBOX },
                    modifier = Modifier.weight(1f)
                )

                SpatialCompactLabBentoTile(
                    feature = LabFeatureType.CHAL,
                    isFa = isFa,
                    accentGlow = SpatialCosmicIris,
                    codeLabel = "LENS 3D",
                    onClick = { selectedFeature = LabFeatureType.CHAL },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 6. Full-Width Bento Card: Relativistic Time Dilation Planner
        item {
            SpatialHeroLabBentoCard(
                feature = LabFeatureType.TIME_DILATION,
                isFa = isFa,
                accentGlow = SpatialNeonRose,
                badgeCode = "SR VOYAGE",
                onClick = { selectedFeature = LabFeatureType.TIME_DILATION }
            )
        }
    }
}

@Composable
private fun SpatialHeroLabBentoCard(
    feature: LabFeatureType,
    isFa: Boolean,
    accentGlow: Color,
    badgeCode: String,
    onClick: () -> Unit
) {
    val palette = SpatialTheme.palette
    SpatialGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lab_feature_card_${feature.name.lowercase()}"),
        cornerRadius = 32.dp,
        contentPadding = PaddingValues(18.dp),
        elevation = 14.dp,
        glowAccent = accentGlow,
        onClick = if (feature.isAvailable) onClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentGlow.copy(alpha = if (palette.isDark) 0.24f else 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = if (palette.isDark) accentGlow else palette.accentInk,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isFa) feature.titleFa else feature.titleEn,
                        style = RedTypographyTokens.sectionTitle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (isFa) feature.subtitleFa else feature.subtitleEn,
                        style = RedTypographyTokens.caption.copy(fontSize = 12.sp),
                        color = palette.accentInk
                    )
                }
            }

            DotMatrixText(
                text = badgeCode,
                dotSize = 1.8.dp,
                dotGap = 0.75.dp,
                charGap = 2.0.dp,
                activeColor = palette.dotMatrixInk,
                glowColor = accentGlow
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isFa) feature.descriptionFa else feature.descriptionEn,
            style = RedTypographyTokens.bodyPrimary.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = palette.textSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SpatialGlassCapsule(
                isActive = true,
                activeGlowColor = accentGlow,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                onClick = onClick
            ) {
                Text(
                    text = if (isFa) "ورود به آزمایشگاه" else "Open Module",
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = palette.accentInk
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = palette.accentInk,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SpatialCompactLabBentoTile(
    feature: LabFeatureType,
    isFa: Boolean,
    accentGlow: Color,
    codeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    SpatialGlassCard(
        modifier = modifier
            .height(194.dp)
            .testTag("lab_feature_card_${feature.name.lowercase()}"),
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(16.dp),
        elevation = 12.dp,
        glowAccent = accentGlow,
        onClick = if (feature.isAvailable) onClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentGlow.copy(alpha = if (palette.isDark) 0.24f else 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = if (palette.isDark) accentGlow else palette.accentInk,
                    modifier = Modifier.size(20.dp)
                )
            }

            DotMatrixText(
                text = codeLabel,
                dotSize = 1.7.dp,
                dotGap = 0.7.dp,
                charGap = 1.9.dp,
                activeColor = palette.dotMatrixInk,
                glowColor = accentGlow
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = if (isFa) feature.titleFa else feature.titleEn,
            style = RedTypographyTokens.bodyPrimary.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = if (isFa) feature.subtitleFa else feature.subtitleEn,
            style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
            color = palette.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
