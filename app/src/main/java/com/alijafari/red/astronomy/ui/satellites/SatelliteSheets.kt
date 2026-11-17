package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape

/**
 * A custom sheet surface: 28 dp top squircle, 36×4 handle, 20 dp padding, max 85 % height, IME
 * padding. Default is flat; Spatial and Aether keep the same geometry with their own fill.
 */
@Composable
fun SatSheetScaffold(
    tokens: ZigSatTokens,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .imePadding()
                    .clip(squircleShape(SatRadius.sheet))
                    .background(if (tokens.glassy) tokens.cardAlt.copy(alpha = 0.97f) else tokens.cardAlt)
                    .border(1.dp, tokens.border, squircleShape(SatRadius.sheet))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RedShapes.capsule)
                        .background(tokens.border),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    content()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SatSheetTitle(title: String, subtitle: String?, tokens: ZigSatTokens, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = RedTypographyTokens.sectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                color = tokens.inkPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
                    color = tokens.inkTertiary,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SatDialogButton(
    label: String,
    tokens: ZigSatTokens,
    onClick: () -> Unit,
    primary: Boolean = false,
    destructive: Boolean = false,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val tint = when {
        destructive -> Color(0xFFE53935)
        primary -> tokens.live
        else -> tokens.inkSecondary
    }
    Box(
        modifier = modifier
            .height(SatGrid.minTouchTarget)
            .clip(RedShapes.capsule)
            .background(
                when {
                    primary -> tokens.live.copy(alpha = 0.16f)
                    destructive -> Color.Transparent
                    else -> tokens.card.copy(alpha = 0.6f)
                }
            )
            .border(1.dp, if (primary || destructive) tint.copy(alpha = 0.5f) else tokens.border, RedShapes.capsule)
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = tint,
            maxLines = 1,
        )
    }
}

// ================================================================================================
// Data Status popover — reachable from the status chip
// ================================================================================================

@Composable
fun SatDataStatusSheet(
    data: SatStatusData,
    tokens: ZigSatTokens,
    onDismiss: () -> Unit,
    onReturnToLive: () -> Unit,
) {
    val isFa = data.isFa
    SatSheetScaffold(tokens = tokens, onDismiss = onDismiss, testTag = "data_status_sheet") {
        SatSheetTitle(
            title = if (isFa) "وضعیت داده" else "Data status",
            subtitle = if (isFa) "منبع، قدمت و اتصال" else "Source, age and connectivity",
            tokens = tokens,
        )
        StatusRow(
            label = if (isFa) "حالت" else "State",
            value = if (data.isLive) (if (isFa) "زنده" else "Live") else
                (if (isFa) "شبیه‌سازی" else "Simulation") + " " + SatFmt.offsetMinutes(data.offsetMinutes, isFa),
            tokens = tokens,
            accent = if (data.isLive) tokens.live else tokens.sim,
        )
        StatusRow(
            label = if (isFa) "منبع" else "Source",
            value = data.sourceLabel,
            tokens = tokens,
        )
        StatusRow(
            label = if (isFa) "قدمت TLE" else "TLE epoch",
            value = data.tleAgeDays?.let { SatFmt.digits(String.format(java.util.Locale.US, "%.1f", it), isFa) + " " + (if (isFa) "روز" else "days") }
                ?: (if (isFa) "نامشخص" else "Unknown"),
            tokens = tokens,
            accent = when {
                (data.tleAgeDays ?: 0.0) > 7.0 -> Color(0xFFE53935)
                (data.tleAgeDays ?: 0.0) > 2.0 -> tokens.sim
                else -> null
            },
        )
        StatusRow(
            label = if (isFa) "اتصال" else "Connectivity",
            value = if (data.isOnline) (if (isFa) "آنلاین" else "Online") else (if (isFa) "آفلاین" else "Offline"),
            tokens = tokens,
            accent = if (data.isOnline) null else Color(0xFFE53935),
        )
        if ((data.tleAgeDays ?: 0.0) > 7.0) {
            Text(
                text = if (isFa) "دقت پس از ۷ روز کاهش می‌یابد." else "Accuracy reduced after 7 days.",
                style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
                color = Color(0xFFE53935),
            )
        }
        if (!data.isLive) {
            SatDialogButton(
                label = if (isFa) "بازگشت به زنده" else "Return to Live",
                tokens = tokens,
                onClick = onReturnToLive,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SatDialogButton(
            label = if (isFa) "بستن" else "Close",
            tokens = tokens,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, tokens: ZigSatTokens, accent: Color? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
            color = tokens.inkTertiary,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium).tabular(),
            color = accent ?: tokens.inkPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ================================================================================================
// Automated Pass Alerts sheet (7-day rolling) — one sheet for all three skins
// ================================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SatAlertsSheet(
    tokens: ZigSatTokens,
    isFa: Boolean,
    enabled: Boolean,
    leadMinutes: Int,
    monitoredIds: Set<String>,
    nextAlertSummary: String?,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, leadMinutes: Int, ids: Set<String>) -> Unit,
    satellites: List<SatelliteItem> = SatelliteCatalog.satellites,
) {
    var localEnabled by remember(enabled) { mutableStateOf(enabled) }
    var localLead by remember(leadMinutes) { mutableStateOf(leadMinutes) }
    var localIds by remember(monitoredIds) { mutableStateOf(monitoredIds) }

    SatSheetScaffold(tokens = tokens, onDismiss = onDismiss, testTag = "alerts_sheet") {
        SatSheetTitle(
            title = if (isFa) "هشدار گذرها" else "Pass alerts",
            subtitle = if (isFa) "۷ روزه، لغزان" else "Rolling 7 days",
            tokens = tokens,
            trailing = {
                Switch(
                    checked = localEnabled,
                    onCheckedChange = { localEnabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = tokens.live),
                    modifier = Modifier.testTag("alerts_master_switch"),
                )
            },
        )

        ZigMicroCaps(text = if (isFa) "زمان یادآوری" else "Lead time", tokens = tokens, isFa = isFa)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                10 to (if (isFa) "۱۰ دقیقه" else "10 min"),
                30 to (if (isFa) "۳۰ دقیقه" else "30 min"),
                1440 to (if (isFa) "۱ روز" else "1 day"),
            ).forEach { (minutes, label) ->
                ZigSatChip(
                    label = label,
                    selected = localLead == minutes,
                    tokens = tokens,
                    onClick = { localLead = minutes },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ZigMicroCaps(text = if (isFa) "ماهواره‌ها" else "Satellites", tokens = tokens, isFa = isFa)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                SatDialogButton(
                    label = if (isFa) "انتخاب همه" else "Select all",
                    tokens = tokens,
                    onClick = { localIds = satellites.map { it.id }.toSet() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SatDialogButton(
                    label = if (isFa) "هیچ‌کدام" else "None",
                    tokens = tokens,
                    onClick = { localIds = emptySet() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            satellites.forEach { sat ->
                val checked = sat.id in localIds
                Row(
                    modifier = Modifier
                        .height(SatGrid.minTouchTarget)
                        .clip(RedShapes.capsule)
                        .background(if (checked) tokens.live.copy(alpha = 0.12f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (checked) tokens.live.copy(alpha = 0.55f) else tokens.border,
                            RedShapes.capsule,
                        )
                        .clickable {
                            localIds = if (checked) localIds - sat.id else localIds + sat.id
                        }
                        .padding(horizontal = 12.dp)
                        .testTag("alert_sat_${sat.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (checked) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = tokens.live,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = if (isFa) sat.nameFa else sat.nameEn,
                        style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
                        color = tokens.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (nextAlertSummary != null) {
            Text(
                text = (if (isFa) "هشدار بعدی: " else "Next alert: ") + nextAlertSummary,
                style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium).tabular(),
                color = tokens.inkSecondary,
            )
        }

        SatDialogButton(
            label = when {
                !localEnabled -> if (isFa) "فعال‌سازی پایش" else "Enable monitoring"
                else -> if (isFa) "ذخیره تغییرات" else "Save changes"
            },
            tokens = tokens,
            onClick = { onSave(localEnabled, localLead, localIds) },
            primary = true,
            modifier = Modifier.fillMaxWidth(),
            testTag = "alerts_save",
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SatDialogButton(
                label = if (isFa) "انصراف" else "Cancel",
                tokens = tokens,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            if (enabled) {
                SatDialogButton(
                    label = if (isFa) "غیرفعال" else "Disable alerts",
                    tokens = tokens,
                    onClick = { onSave(false, localLead, localIds) },
                    destructive = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ================================================================================================
// Guide & Legend sheet — all three skins
// ================================================================================================

@Composable
fun SatGuideSheet(tokens: ZigSatTokens, isFa: Boolean, onDismiss: () -> Unit) {
    SatSheetScaffold(tokens = tokens, onDismiss = onDismiss, testTag = "guide_sheet") {
        SatSheetTitle(
            title = if (isFa) "راهنما و راهنمای نمادها" else "Guide & legend",
            subtitle = null,
            tokens = tokens,
        )
        GuideRow(
            icon = Icons.Filled.Swipe,
            title = if (isFa) "چرخش و بزرگ‌نمایی کره" else "Drag / pinch the globe",
            body = if (isFa) "با یک انگشت بچرخانید و با دو انگشت بزرگ‌نمایی کنید."
            else "Rotate with one finger, pinch or double-tap to zoom.",
            tokens = tokens,
        )
        GuideRow(
            icon = Icons.Filled.DarkMode,
            title = if (isFa) "نورپردازی نقشه" else "Map lighting",
            body = if (isFa) "خودکار / تاریک / روشن را از ستون کنترل کره انتخاب کنید."
            else "Auto / Dark / Light, from the globe control column.",
            tokens = tokens,
        )
        GuideRow(
            icon = Icons.Filled.TouchApp,
            title = if (isFa) "ماهواره ← برچسب ← جزئیات" else "Tap a satellite → popover → Details",
            body = if (isFa) "یک لمس روی کره تمرکز می‌دهد؛ لمس دوم جزئیات را باز می‌کند."
            else "One tap focuses on the globe; a second tap opens details.",
            tokens = tokens,
        )
        GuideRow(
            icon = Icons.Filled.LightMode,
            title = if (isFa) "اسکراب ±۱۲ ساعت" else "Scrub ±12h",
            body = if (isFa) "نوار زمان، نقطه زنده و نشانگر گذرها."
            else "The time strip, its Live detent and the pass markers.",
            tokens = tokens,
        )
        ZigMicroCaps(text = if (isFa) "نمادها" else "Legend", tokens = tokens, isFa = isFa)
        SatLegendPanel(tokens = tokens, isFa = isFa)
        SatDialogButton(
            label = if (isFa) "متوجه شدم" else "Got it",
            tokens = tokens,
            onClick = onDismiss,
            primary = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GuideRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    tokens: ZigSatTokens,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tokens.live.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.live,
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Text(
                text = title,
                style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = tokens.inkPrimary,
            )
            Text(
                text = body,
                style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
                color = tokens.inkSecondary,
            )
        }
    }
}

// ================================================================================================
// Reminder row list — reused by the spotlight, the detail screen and the pass cards
// ================================================================================================

@Composable
fun SatReminderOptions(
    tokens: ZigSatTokens,
    isFa: Boolean,
    passStartMs: Long,
    selectedLeadMinutes: Int?,
    onChoose: (Int) -> Unit,
    onRemove: (() -> Unit)?,
) {
    val tz = java.util.TimeZone.getDefault()
    val options = listOf(
        10 to (if (isFa) "۱۰ دقیقه قبل" else "10 min before"),
        30 to (if (isFa) "۳۰ دقیقه قبل" else "30 min before"),
        1440 to (if (isFa) "۱ روز قبل" else "1 day before"),
    )
    options.forEach { (minutes, label) ->
        val at = passStartMs - minutes * 60_000L
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SatGrid.minTouchTarget)
                .clickable { onChoose(minutes) }
                .testTag("reminder_option_$minutes"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = RedTypographyTokens.badge.copy(fontSize = 13.sp),
                color = tokens.inkPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = SatFmt.clock(at, isFa, tz),
                style = RedTypographyTokens.badge.copy(fontSize = 12.sp).tabular(),
                color = tokens.inkTertiary,
            )
            if (selectedLeadMinutes == minutes) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = tokens.live,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
    if (onRemove != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SatGrid.minTouchTarget)
                .clickable(onClick = onRemove),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isFa) "حذف یادآوری" else "Remove reminder",
                style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = Color(0xFFE53935),
            )
        }
    }
}

// ================================================================================================
// Fleet sheet — searchable full-width rows from "See all"
// ================================================================================================

@Composable
fun SatFleetSheet(
    tokens: ZigSatTokens,
    isFa: Boolean,
    fleet: List<SatTokenUi>,
    onPick: (SatTokenUi) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, fleet) {
        if (query.isBlank()) fleet else fleet.filter {
            it.fullName.contains(query, ignoreCase = true) || it.shortName.contains(query, ignoreCase = true)
        }
    }
    SatSheetScaffold(tokens = tokens, onDismiss = onDismiss, testTag = "fleet_sheet") {
        SatSheetTitle(
            title = if (isFa) "ماهواره‌های تحت پایش" else "Tracked satellites",
            subtitle = if (isFa) "${SatFmt.int(fleet.size, true)} ماهواره" else "${fleet.size} satellites",
            tokens = tokens,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = LocalAppFontFamily.current),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = tokens.inkTertiary) },
            placeholder = { Text(if (isFa) "جستجو" else "Search", color = tokens.inkTertiary) },
        )
        filtered.forEach { tokenUi ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(squircleShape(SatRadius.tile))
                    .clickable { onPick(tokenUi) }
                    .padding(horizontal = 12.dp)
                    .testTag("fleet_sheet_row_${tokenUi.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = tokenUi.fullName,
                    style = RedTypographyTokens.badge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = tokens.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = SatFmt.signed(tokenUi.elevationDeg, 0, "°", isFa),
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp).tabular(),
                    color = tokens.inkSecondary,
                )
                Text(
                    text = if (tokenUi.isNakedEye) {
                        if (isFa) "با چشم" else "Naked eye"
                    } else {
                        if (isFa) "تلسکوپی" else "Telescopic"
                    },
                    style = RedTypographyTokens.badge.copy(fontSize = 10.sp),
                    color = if (tokenUi.isNakedEye) tokens.nakedEye else tokens.inkTertiary,
                )
            }
        }
    }
}

/** A one-line snackbar replacement that carries its own Undo, used after setting a reminder. */
@Composable
fun SatSnackbar(message: String, actionLabel: String?, tokens: ZigSatTokens, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RedShapes.capsule)
            .background(tokens.popoverFill)
            .border(1.dp, tokens.popoverBorder.copy(alpha = 0.5f), RedShapes.capsule)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = RedTypographyTokens.badge.copy(fontSize = 12.sp),
            color = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .height(SatGrid.minTouchTarget)
                    .clickable(onClick = onAction),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    style = RedTypographyTokens.badge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFB4B4),
                )
            }
        }
    }
}
