package com.alijafari.red.astronomy.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.squircleShape

/**
 * The one settings-menu skin every UI style wears: a Samsung-style, space-generous menu of grouped
 * rows on a single tall panel. Colour and tone flow in through [SettingsSkin]; structure, spacing
 * and interaction stay identical everywhere so the menu is instantly familiar on every skin.
 */

@Immutable
data class SettingsSkin(
    val accent: Color,
    val onAccent: Color,
    val panel: Color,
    val groupBg: Color,
    val selectedBg: Color,
    val chipBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color
)

/** The tall rounded panel with the large One UI-style header and a circular close button. */
@Composable
fun SettingsMenuPanel(
    skin: SettingsSkin,
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .fillMaxHeight(0.88f)
            .padding(horizontal = 12.dp, vertical = 20.dp)
            .clip(squircleShape(28.dp))
            .background(skin.panel)
            .testTag("settings_dialog")
    ) {
        // Large header, generous air around it.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 26.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = RedTypographyTokens.sectionTitle.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp
                    ),
                    color = skin.textPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.caption.copy(fontSize = 12.5.sp),
                    color = skin.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(skin.chipBg)
                    .clickable(onClick = onClose)
                    .testTag("settings_close_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.ok),
                    tint = skin.textSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        content()
    }
}

/** A small section label that gives each group of rows its One UI hierarchy. */
@Composable
fun SettingsGroupHeader(title: String, skin: SettingsSkin) {
    Text(
        text = title,
        style = RedTypographyTokens.caption.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.9.sp
        ),
        color = skin.accent,
        modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 8.dp)
    )
}

/**
 * A generous settings row — icon chip, title, current value — that unfolds its options in place
 * with a soft spring. The row itself is the whole tap target.
 */
@Composable
fun SettingsExpandableRow(
    skin: SettingsSkin,
    title: String,
    valueLabel: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val chevron by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "SettingsChevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RedShapes.capsule)
            .background(skin.groupBg)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsIconChip(skin, icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.5.sp
                    ),
                    color = skin.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = valueLabel,
                    style = RedTypographyTokens.caption.copy(fontSize = 12.5.sp),
                    color = skin.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = skin.textTertiary,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevron)
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.expandVertically(
                spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            ) + androidx.compose.animation.fadeIn(
                spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = androidx.compose.animation.shrinkVertically(
                spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
            ) + androidx.compose.animation.fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                    .clip(squircleShape(RedCornerRadius.lg))
                    .background(skin.panel)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )
        }
    }
}

/** A radio-style option row: tap anywhere to choose, checkmark slides in on the chosen one. */
@Composable
fun SettingsOptionRow(
    skin: SettingsSkin,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(squircleShape(RedCornerRadius.md))
            .background(if (isSelected) skin.selectedBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) skin.accent else Color.Transparent)
                .then(
                    if (isSelected) Modifier
                    else Modifier.border(1.5.dp, skin.textTertiary.copy(alpha = 0.45f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.ok),
                    tint = skin.onAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = RedTypographyTokens.bodySecondary.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                ),
                color = if (isSelected) skin.accent else skin.textPrimary
            )
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
                    color = skin.textTertiary
                )
            }
        }
    }
}

/** A navigational row (About): icon chip, title, subtitle, and a trailing chevron. */
@Composable
fun SettingsPlainRow(
    skin: SettingsSkin,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsIconChip(skin, icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = RedTypographyTokens.bodyPrimary.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.5.sp
                ),
                color = skin.textPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = RedTypographyTokens.caption.copy(fontSize = 12.5.sp),
                color = skin.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = skin.textTertiary,
            modifier = Modifier
                .size(22.dp)
                .rotate(-90f)
        )
    }
}

@Composable
private fun SettingsIconChip(skin: SettingsSkin, icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(squircleShape(RedCornerRadius.md))
            .background(skin.chipBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = skin.accent,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Hairline divider inset to the text column, like One UI's list separators. */
@Composable
fun SettingsDivider(skin: SettingsSkin) {
    Box(
        modifier = Modifier
            .padding(start = 72.dp, end = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(skin.divider)
    )
}

// -------------------------------------------------------------------------------------------------
// Bortle dark-sky reference data and guide
// -------------------------------------------------------------------------------------------------

/** Data structure for the Bortle Dark-Sky scale reference guide. */
data class BortleClassInfo(
    val classNumber: Int,
    val nameEn: String,
    val nameFa: String,
    val descEn: String,
    val descFa: String
)

val BORTLE_CLASSES = listOf(
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
        descFa = "راه شیری در سرسو بسیار کم‌فروغ، آلودگی نوری در بیشتر جهات افق به وضوح دیده می‌شود."
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

/** The full Bortle scale guide: nine roomy rows, the active class highlighted. */
@Composable
fun BortleGuideList(
    skin: SettingsSkin,
    isFa: Boolean,
    selectedClass: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BORTLE_CLASSES.forEach { info ->
            val isSelected = selectedClass == info.classNumber
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(squircleShape(RedCornerRadius.md))
                    .background(if (isSelected) skin.selectedBg else Color.Transparent)
                    .clickable { onSelect(info.classNumber) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(squircleShape(8.dp))
                        .background(if (isSelected) skin.accent else skin.chipBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${info.classNumber}",
                        style = RedTypographyTokens.caption.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) skin.onAccent else skin.textSecondary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFa) info.nameFa else info.nameEn,
                        style = RedTypographyTokens.caption.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = if (isSelected) skin.accent else skin.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isFa) info.descFa else info.descEn,
                        style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
                        color = skin.textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// About page
// -------------------------------------------------------------------------------------------------

/**
 * The About page: the app's identity, what it is, and the creator and developer behind it.
 * Reached from the About row; slides in over the menu.
 */
@Composable
fun AboutZigPage(
    skin: SettingsSkin,
    isFa: Boolean,
    versionName: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = skin.accent,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(90f)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.app_settings),
                style = RedTypographyTokens.bodyPrimary.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = skin.accent
            )
        }

        Spacer(Modifier.height(18.dp))

        // App identity
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(squircleShape(24.dp))
                .background(skin.chipBg),
            contentAlignment = Alignment.Center
        ) {
            SafeAppLogo(
                modifier = Modifier.size(72.dp),
                cornerRadius = 16.dp
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "ZIG",
            style = RedTypographyTokens.sectionTitle.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = skin.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.about_tagline),
            style = RedTypographyTokens.caption.copy(fontSize = 13.sp),
            color = skin.textSecondary
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(squircleShape(999.dp))
                .background(skin.selectedBg)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = "${stringResource(R.string.about_version_label)} $versionName",
                style = RedTypographyTokens.caption.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = skin.accent
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.about_desc),
            style = RedTypographyTokens.bodySecondary.copy(fontSize = 13.5.sp),
            color = skin.textSecondary,
            lineHeight = 21.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(26.dp))

        // Creator & developer card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(squircleShape(RedCornerRadius.lg))
                .background(skin.groupBg)
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag("developer_credit_card")
        ) {
            Text(
                text = stringResource(R.string.about_creator_title),
                style = RedTypographyTokens.caption.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                ),
                color = skin.textTertiary
            )
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(skin.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFa) "ع‌ج" else "AJ",
                        style = RedTypographyTokens.sectionTitle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = skin.onAccent
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.about_creator_name),
                        style = RedTypographyTokens.bodyPrimary.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = skin.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Ali Jafari • ZIG Astronomy Engine",
                        style = RedTypographyTokens.caption.copy(fontSize = 12.sp),
                        color = skin.textSecondary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Telegram contact
            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(squircleShape(RedCornerRadius.md))
                    .background(skin.selectedBg)
                    .clickable {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/EdisonWasAThief"))
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = skin.accent,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.about_contact_telegram),
                        style = RedTypographyTokens.bodySecondary.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = skin.textPrimary
                    )
                    Text(
                        text = "@EdisonWasAThief",
                        style = RedTypographyTokens.caption.copy(fontSize = 11.5.sp),
                        color = skin.accent
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.about_copyright),
            style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
            color = skin.textTertiary
        )
        Spacer(Modifier.height(18.dp))
    }
}
