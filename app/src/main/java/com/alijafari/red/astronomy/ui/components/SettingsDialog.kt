package com.alijafari.red.astronomy.ui.components

import android.content.Context
import androidx.compose.ui.input.pointer.pointerInput
import com.alijafari.red.astronomy.ui.components.SafeAppLogo
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.backdrop.AppBackdropMode
import com.alijafari.red.astronomy.ui.theme.*
import com.alijafari.red.astronomy.ui.theme.RedSurface
import com.alijafari.red.astronomy.ui.theme.squircleShape
import kotlin.math.roundToInt

/**
 * Data structure for the Bortle Dark-Sky scale reference guide.
 */
private data class BortleClassInfo(
    val classNumber: Int,
    val nameEn: String,
    val nameFa: String,
    val descEn: String,
    val descFa: String
)

private val BORTLE_CLASSES = listOf(
    BortleClassInfo(
        classNumber = 1,
        nameEn = "Excellent Dark Sky",
        nameFa = "آسمان کاملاً تاریک و بکر",
        descEn = "Zodiacal light visible and bright, Milky Way casts shadows, M33 directly naked-eye.",
        descFa = "نور منطقه‌البروجی درخشان، کهکشان راه شیری سایه می‌اندازد، کهکشان M33 با چشم غیرمسلح پیداست."
    ),
    BortleClassInfo(
        classNumber = 2,
        nameEn = "Typical Truly Dark Sky",
        nameFa = "آسمان تاریک واقعی",
        descEn = "Milky Way highly structured, zodiacal light prominent, M33 easily seen with naked eye.",
        descFa = "راه شیری با ساختار پیچیده و پرجزئیات، نور زودیاک نمایان، M33 به سادگی با چشم غیرمسلح دیده می‌شود."
    ),
    BortleClassInfo(
        classNumber = 3,
        nameEn = "Rural Sky",
        nameFa = "آسمان روستایی",
        descEn = "Milky Way shows rich detail, light pollution faint at horizon, M33 glimpsed with averted vision.",
        descFa = "راه شیری جزئیات خوبی دارد، آلودگی نوری خفیف در افق، M33 با نگاه مایل دیده می‌شود."
    ),
    BortleClassInfo(
        classNumber = 4,
        nameEn = "Rural / Suburban Transition",
        nameFa = "مرز روستایی و حومه شهر",
        descEn = "Milky Way visible above 40°, light domes obvious in several directions, M33 in binoculars.",
        descFa = "راه شیری بالای ۴۰ درجه افق پیداست، گنبدهای نوری در چند جهت، M33 در دوچشمی پیداست."
    ),
    BortleClassInfo(
        classNumber = 5,
        nameEn = "Suburban Sky",
        nameFa = "آسمان حومه شهر",
        descEn = "Milky Way washed out and faint at zenith, light pollution visible in most directions.",
        descFa = "راه شیری در سرسو بسیار کم‌فروغ، آلودگی نوری در بیشتر جهات افق به وضوح پیداست."
    ),
    BortleClassInfo(
        classNumber = 6,
        nameEn = "Bright Suburban Sky",
        nameFa = "آسمان روشن حومه شهر",
        descEn = "Milky Way only faintly perceptible overhead, sky glow bright within 35° of horizon.",
        descFa = "راه شیری فقط با زحمت در سرسو دیده می‌شود، درخشش آسمان تا ۳۵ درجه از افق بالا آمده."
    ),
    BortleClassInfo(
        classNumber = 7,
        nameEn = "Suburban / Urban Transition",
        nameFa = "مرز حومه و شهر",
        descEn = "Milky Way invisible, sky background has strong grey/yellow tint, brightest DSOs barely visible.",
        descFa = "راه شیری اصلاً دیده نمی‌شود، زمینه آسمان خاکستری-زرد، فقط اجرام بسیار روشن با تلسکوپ پیداست."
    ),
    BortleClassInfo(
        classNumber = 8,
        nameEn = "City Sky",
        nameFa = "آسمان شهری",
        descEn = "Sky glows whitish-grey, stars forming constellation outlines hard to see, faint stars lost.",
        descFa = "آسمان سفید-خاکستری می‌درخشد، فقط ستاره‌های پرنور صور فلکی اصلی قابل تشخیص‌اند."
    ),
    BortleClassInfo(
        classNumber = 9,
        nameEn = "Inner-City Sky",
        nameFa = "مرکز کلان‌شهر",
        descEn = "Only brightest stars and planets visible, sky brilliantly illuminated by urban lighting.",
        descFa = "فقط سیارات و پرنورترین ستارگان (مانند شباهنگ) قابل رویت هستند؛ زمینه آسمان روشن است."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isFa = uiState.language == AppLanguage.PERSIAN

    var isUiStyleExpanded by remember { mutableStateOf(false) }
    var isLanguageExpanded by remember { mutableStateOf(false) }
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var isAppThemeExpanded by remember { mutableStateOf(false) }
    var isSkyCanvasThemeExpanded by remember { mutableStateOf(false) }
    var isAppBackdropExpanded by remember { mutableStateOf(false) }
    var isBortleExpanded by remember { mutableStateOf(false) }
    var isAboutExpanded by remember { mutableStateOf(false) }

    val uiStyleLabel = when (uiState.uiStyle) {
        AppUiStyle.DEFAULT -> stringResource(R.string.ui_style_default)
        AppUiStyle.SPATIAL -> stringResource(R.string.ui_style_spatial)
        AppUiStyle.AETHER -> stringResource(R.string.ui_style_aether)
    }

    val languageLabel = when (uiState.language) {
        AppLanguage.PERSIAN -> if (isFa) "فارسی" else "Persian"
        AppLanguage.ENGLISH -> if (isFa) "انگلیسی" else "English"
    }

    val calendarLabel = when (uiState.calendarSystem) {
        CalendarSystem.SOLAR_HIJRI -> if (isFa) "هجری شمسی" else "Solar Hijri"
        CalendarSystem.GREGORIAN -> if (isFa) "میلادی" else "Gregorian"
    }

    val appThemeLabel = when (uiState.themeMode) {
        ThemeMode.LIGHT -> if (isFa) "روشن" else "Light"
        ThemeMode.DARK -> if (isFa) "تاریک (OLED)" else "Dark (OLED)"
        ThemeMode.WINE -> stringResource(R.string.theme_wine)
    }

    val skyCanvasThemeLabel = when (uiState.skyCanvasTheme) {
        SkyCanvasTheme.REAL_SKY -> if (isFa) "آسمان واقعی" else "Real Sky"
        SkyCanvasTheme.ATMOSPHERIC_SKY -> if (isFa) "آسمان جوی" else "Atmospheric"
        SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (isFa) "تک‌رنگ" else "Monochrome"
        SkyCanvasTheme.KIDS_WATERCOLOR -> if (isFa) "آبرنگ" else "WaterColor"
        SkyCanvasTheme.OBSERVATORY -> if (isFa) "رصدخانه" else "Observatory"
        SkyCanvasTheme.PAPERCRAFT_DIORAMA -> if (isFa) "دیوراما کاغذ برجسته" else "Papercraft"
    }

    val liveSkyStatusLabel = when (uiState.appBackdropMode) {
        AppBackdropMode.LIVE_SKY -> stringResource(R.string.live_sky_setting)
        AppBackdropMode.NGC_1929 -> stringResource(R.string.backdrop_ngc1929_setting)
        AppBackdropMode.CEPHEUS -> stringResource(R.string.backdrop_cepheus_setting)
        AppBackdropMode.NONE -> if (isFa) "خاموش" else "Off"
    }

    val bortleLabel = if (isFa) "کلاس ${uiState.bortleClass}" else "Class ${uiState.bortleClass}"

    // Use full screen Box to overlay with custom entrance animation handled by MainActivity
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RedTheme.colors.background)
            .pointerInput(Unit) {}, // Block clicks from passing through
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
        ) {
            // Samsung style Large Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_settings),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = RedTheme.colors.textPrimary
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.ui_style_title),
                    currentValueLabel = uiStyleLabel,
                    icon = Icons.Outlined.DesignServices,
                    isExpanded = isUiStyleExpanded,
                    onToggle = { isUiStyleExpanded = !isUiStyleExpanded }
                ) {
                    SettingsOptionRow("Default Red Theme", uiState.uiStyle == AppUiStyle.DEFAULT, { viewModel.setUiStyle(AppUiStyle.DEFAULT) })
                    SettingsOptionRow("Spatial VisionOS", uiState.uiStyle == AppUiStyle.SPATIAL, { viewModel.setUiStyle(AppUiStyle.SPATIAL) })
                    SettingsOptionRow("Aether HUD", uiState.uiStyle == AppUiStyle.AETHER, { viewModel.setUiStyle(AppUiStyle.AETHER) })
                }

                ExpandableSettingsSection(
                    title = stringResource(R.string.language),
                    currentValueLabel = languageLabel,
                    icon = Icons.Outlined.Language,
                    isExpanded = isLanguageExpanded,
                    onToggle = { isLanguageExpanded = !isLanguageExpanded }
                ) {
                    SettingsOptionRow("English", uiState.language == AppLanguage.ENGLISH, { viewModel.setLanguage(AppLanguage.ENGLISH) })
                    SettingsOptionRow("فارسی", uiState.language == AppLanguage.PERSIAN, { viewModel.setLanguage(AppLanguage.PERSIAN) })
                }

                ExpandableSettingsSection(
                    title = stringResource(R.string.app_theme),
                    currentValueLabel = appThemeLabel,
                    icon = Icons.Outlined.Palette,
                    isExpanded = isAppThemeExpanded,
                    onToggle = { isAppThemeExpanded = !isAppThemeExpanded }
                ) {
                    SettingsOptionRow(if (isFa) "روشن" else "Light", uiState.themeMode == ThemeMode.LIGHT, { viewModel.setThemeMode(ThemeMode.LIGHT) })
                    SettingsOptionRow(if (isFa) "تاریک (OLED)" else "Dark (OLED)", uiState.themeMode == ThemeMode.DARK, { viewModel.setThemeMode(ThemeMode.DARK) })
                    SettingsOptionRow(stringResource(R.string.theme_wine), uiState.themeMode == ThemeMode.WINE, { viewModel.setThemeMode(ThemeMode.WINE) })
                }
                
                ExpandableSettingsSection(
                    title = stringResource(R.string.about),
                    currentValueLabel = "App Info",
                    icon = Icons.Outlined.Info,
                    isExpanded = isAboutExpanded,
                    onToggle = { isAboutExpanded = !isAboutExpanded }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SafeAppLogo(modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = RedTypographyTokens.screenTitle,
                            color = RedTheme.colors.textPrimary
                        )
                        Text(
                            text = "Created by Ali Jafari",
                            style = RedTypographyTokens.bodyPrimary,
                            color = RedTheme.colors.textSecondary
                        )
                        Text(
                            text = "ZIG Astronomy Engine",
                            style = RedTypographyTokens.caption,
                            color = RedTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "An advanced stargazing, AR, satellite tracking, and astronomical toolkit app.",
                            style = RedTypographyTokens.bodySecondary,
                            color = RedTheme.colors.textPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
private fun ExpandableSettingsSection(
    title: String,
    currentValueLabel: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    statusColor: Color? = null,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "expand_rotation"
    )

    RedSurface(
        shape = squircleShape(24.dp), // More generous rounded corners like Samsung One UI
        color = RedTheme.colors.surfaceGrouped,
        border = BorderStroke(
            1.dp,
            if (isExpanded) RedTheme.colors.accentRed.copy(alpha = 0.35f) else RedTheme.colors.border
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        elevation = RedElevation.none,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 24.dp, vertical = 20.dp), // Generous spacing
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isExpanded) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = RedTheme.colors.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currentValueLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = statusColor ?: RedTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = RedTheme.colors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationState)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(180)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(140)) + fadeOut(tween(140))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedTheme.colors.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SettingsOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    RedSurface(
        shape = squircleShape(16.dp),
        color = if (isSelected) RedTheme.colors.accentRed.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(
            0.5.dp,
            if (isSelected) RedTheme.colors.accentRed.copy(alpha = 0.4f) else RedTheme.colors.border.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = RedElevation.none,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) RedTheme.colors.accentRed else RedTheme.colors.textPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = RedTheme.colors.textSecondary
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = RedTheme.colors.accentRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
