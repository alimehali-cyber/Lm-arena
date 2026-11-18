package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.TleDataSource
import com.alijafari.red.astronomy.ui.screens.MapTheme
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import java.util.Calendar
import java.util.TimeZone

/** Panel chrome and detents are owned by SatelliteHud, never a second window or scrim. */
@Composable internal fun SheetBody(tag:String,title:String,header:@Composable ()->Unit,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().testTag(tag).semantics { paneTitle=title }) {
        header()
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
    }
}

@Composable
fun SatSheetTitle(title: String, subtitle: String?, tokens: ZigSatTokens, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            SatText(title, tokens.inkPrimary, size = 20, strong = true)
            if (subtitle != null) SatText(subtitle, tokens.inkSecondary, size = 12)
        }
        trailing?.invoke()
    }
}

@Composable
fun SatDialogButton(label: String, tokens: ZigSatTokens, onClick: () -> Unit, primary: Boolean = false,
    destructive: Boolean = false, modifier: Modifier = Modifier, testTag: String? = null, enabled: Boolean = true) {
    val ink = if (primary) Color.White else if (destructive) tokens.accentInk else tokens.inkPrimary
    Box(modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clip(SatShapes.capsule)
        .background(if (primary && enabled) tokens.selectedChip else tokens.cardAlt)
        .border(1.dp, if (destructive) tokens.accentInk.copy(alpha = .45f) else tokens.border, SatShapes.capsule)
        .clickable(role = Role.Button, enabled = enabled, onClick = onClick)
        .then(if (testTag == null) Modifier else Modifier.testTag(testTag)).padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        SatText(label, if (enabled) ink else tokens.inkSecondary, size = 13, strong = true)
    }
}

@Composable
internal fun SatDataStatusPopover(data: SatStatusData, tokens: ZigSatTokens, tz: TimeZone,
    onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.widthIn(max = 330.dp).fillMaxWidth().testTag("data_status_popover")
        .satMaterial(tokens, SatSlot.Popover, SatShapes.of(16.dp))
        .pointerInput(Unit) { detectTapGestures {} }.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SatSheetTitle(stringResource(R.string.sat_status), null, tokens,
            trailing = { ZigSatIconButton(Icons.Filled.Close, stringResource(R.string.sat_close), tokens, onClose) })
        val source = stringResource(when (data.tleSource) {
            TleDataSource.LIVE_NETWORK -> R.string.sat_source_live
            TleDataSource.MEMORY_CACHE, TleDataSource.PERSISTED_CACHE -> R.string.sat_source_cache
            TleDataSource.HARDCODED_FALLBACK -> R.string.sat_source_bundled
            null -> R.string.sat_unknown
        })
        StatusRow(stringResource(R.string.sat_source), source, tokens)
        StatusRow(stringResource(R.string.sat_epoch), data.tleEpochMillis?.let { SatFmt.dateTime(it, data.isFa, tz) }
            ?: stringResource(R.string.sat_unknown), tokens)
        StatusRow(stringResource(R.string.sat_age), data.tleAgeDays?.let {
            stringResource(R.string.sat_age_days, SatFmt.number(it, 1, data.isFa))
        } ?: stringResource(R.string.sat_unknown), tokens)
        StatusRow(stringResource(R.string.sat_connection), stringResource(if (data.isOnline) R.string.sat_online else R.string.sat_offline), tokens)
        if ((data.tleAgeDays ?: 0.0) > 7.0) SatText(stringResource(R.string.sat_stale), tokens.accentInk)
        StatusRow(stringResource(R.string.sat_last_fix), data.lastFixMs?.let { SatFmt.dateTime(it,data.isFa,tz) } ?: stringResource(R.string.sat_unknown),tokens)
    }
}

@Composable
private fun StatusRow(label: String, value: String, tokens: ZigSatTokens) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SatText(label, tokens.inkSecondary, Modifier.weight(.42f))
        SatText(value, tokens.inkPrimary, Modifier.weight(.58f), strong = true)
    }
}

@Composable
internal fun SatAlertsSheet(tokens: ZigSatTokens, isFa: Boolean, config: SatAlertsConfig,
    nextAlertSummary: (Set<String>, Int) -> String?, onDismiss: () -> Unit, onSave: (SatAlertsConfig) -> Unit) {
    var enabled by remember { mutableStateOf(config.enabled) }
    var lead by remember { mutableIntStateOf(config.leadMinutes) }
    var ids by remember { mutableStateOf(config.satelliteIds) }
    val title = stringResource(R.string.sat_alerts)
    val rolling = stringResource(R.string.sat_rolling)
    val choices = SatelliteCatalog.satellites.map { it.id to shortNameOf(it, isFa) } + ("starlink_train" to stringResource(R.string.sat_trains))
    SheetBody("alerts_sheet", title, header = {
        SatSheetTitle(title, rolling, tokens, trailing = {
            Switch(enabled, { enabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = tokens.selectedChip),
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("alerts_master_switch").semantics { contentDescription = title })
        })
    }) {
        SatText(stringResource(R.string.sat_lead), tokens.inkSecondary, strong = true)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            reminderLeads.forEach { minutes -> ZigSatChip(satLeadLabel(minutes, isFa), lead == minutes, tokens,
                { lead = minutes }, modifier = Modifier.weight(1f)) }
        }
        SatText(stringResource(R.string.sat_fleet), tokens.inkSecondary, strong = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SatDialogButton(stringResource(R.string.sat_select_all), tokens, { ids = choices.map { it.first }.toSet() }, modifier = Modifier.weight(1f))
            SatDialogButton(stringResource(R.string.sat_select_none), tokens, { ids = emptySet() }, modifier = Modifier.weight(1f))
        }
        choices.chunked(2).forEach { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { (id, name) -> ZigSatChip(name, id in ids, tokens, { ids = if (id in ids) ids - id else ids + id },
                modifier = Modifier.weight(1f), testTag = "alert_sat_$id", leading = {
                    if (id in ids) Icon(Icons.Filled.Check, null, Modifier.size(13.dp), tint = Color.White)
                }) }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        } }
        SatText(nextAlertSummary(ids, lead)?.let { stringResource(R.string.sat_next_alert, it) }
            ?: stringResource(R.string.sat_no_alert), tokens.inkSecondary)
        SatDialogButton(stringResource(if (config.enabled) R.string.sat_save else R.string.sat_enable), tokens,
            { onSave(SatAlertsConfig(if (config.enabled) enabled else true, lead, ids)) }, primary = true,
            modifier = Modifier.fillMaxWidth(), testTag = "alerts_save", enabled = ids.isNotEmpty() || config.enabled && !enabled)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SatDialogButton(stringResource(R.string.sat_cancel), tokens, onDismiss, modifier = Modifier.weight(1f))
            if (config.enabled) SatDialogButton(stringResource(R.string.sat_disable), tokens,
                { onSave(SatAlertsConfig(false, lead, ids)) }, destructive = true, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun SatGuideSheet(tokens: ZigSatTokens, isFa: Boolean, onDismiss: () -> Unit) {
    val title = stringResource(R.string.sat_guide)
    SheetBody("guide_sheet",title,header={ SatSheetTitle(title,null,tokens) }) {
        GuideRow(Icons.Filled.Swipe, stringResource(R.string.sat_gesture_title), stringResource(R.string.sat_gesture_body), tokens)
        GuideRow(Icons.Filled.DarkMode, stringResource(R.string.sat_lighting), stringResource(R.string.sat_lighting_body), tokens)
        GuideRow(Icons.Filled.TouchApp, stringResource(R.string.sat_focus_title), stringResource(R.string.sat_focus_body), tokens)
        GuideRow(Icons.Filled.Schedule, stringResource(R.string.sat_scrub_title), stringResource(R.string.sat_scrub_body), tokens)
        SatDialogButton(stringResource(R.string.sat_got_it), tokens, onDismiss, primary = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun GuideRow(icon: ImageVector, title: String, body: String, tokens: ZigSatTokens) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(32.dp).background(tokens.live.copy(alpha = .12f), SatShapes.capsule), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = tokens.accentInk)
        }
        Column {
            SatText(title, tokens.inkPrimary, size = 14, strong = true)
            SatText(body, tokens.inkSecondary, size = 12)
        }
    }
}

internal val reminderLeads = listOf(10, 30, 1440)

@Composable
internal fun satLeadLabel(minutes: Int, isFa: Boolean): String = stringResource(when (minutes) {
    10 -> R.string.sat_lead_10
    30 -> R.string.sat_lead_30
    else -> R.string.sat_lead_day
})

internal data class SatReminderTarget(val pass: SatPassUi, val bounds: IntRect)

/** The extra menu belongs to the Passes/Detail surface rather than to the primary HUD state. */
@Composable
internal fun SatReminderPopover(tokens: ZigSatTokens, isFa: Boolean, target: SatReminderTarget,
    selected: Int?, now: Long, tz: TimeZone, onChoose: (Int) -> Unit, onRemove: (() -> Unit)?, onClose: () -> Unit) {
    val margin = with(LocalDensity.current) { 12.dp.roundToPx() }
    val position = remember(target.bounds, margin) { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection,
            popupContentSize: IntSize): IntOffset {
            val bounds = target.bounds
            val height = popupContentSize.height
            val width = popupContentSize.width
            val left = if (layoutDirection == LayoutDirection.Rtl) bounds.left else bounds.right - width
            val above = bounds.top - height - margin
            return IntOffset(left.coerceIn(margin, (windowSize.width - width - margin).coerceAtLeast(margin)),
                if (above >= margin) above else (bounds.bottom + margin).coerceIn(margin, (windowSize.height - height - margin).coerceAtLeast(margin)))
        }
    } }
    Popup(position, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        Column(Modifier.width(232.dp).testTag("reminder_popover").satMaterial(tokens, SatSlot.Popover, SatShapes.of(20.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)) {
            reminderLeads.forEach { minutes ->
                val time = target.pass.pass.startTimeMs - minutes * 60_000L
                val clock = SatFmt.clock(time, isFa, tz)
                val actionable = time > now
                val triggerDay = Calendar.getInstance(tz).apply { timeInMillis = time }.get(Calendar.DAY_OF_YEAR)
                val today = Calendar.getInstance(tz).apply { timeInMillis = now }.get(Calendar.DAY_OF_YEAR)
                val tomorrow = Calendar.getInstance(tz).apply { timeInMillis = now; add(Calendar.DATE, 1) }.get(Calendar.DAY_OF_YEAR)
                val at = when (triggerDay) {
                    today -> stringResource(R.string.sat_reminder_at, clock)
                    tomorrow -> stringResource(R.string.sat_reminder_day_at, clock)
                    else -> SatFmt.dateTime(time, isFa, tz)
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = actionable, role = Role.Button) { onChoose(minutes) }
                    .testTag("reminder_option_$minutes").padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(Modifier.weight(1f)) {
                        SatText(stringResource(R.string.sat_reminder_before, satLeadLabel(minutes, isFa)),
                            if (actionable) tokens.inkPrimary else tokens.inkTertiary, strong = true)
                        SatText(at, tokens.inkSecondary, size = 11)
                    }
                    if (selected == minutes) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = tokens.accentInk)
                }
            }
            if (onRemove != null) SatDialogButton(stringResource(R.string.sat_reminder_remove), tokens, onRemove,
                destructive = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun SatSnackbar(message: String, tokens: ZigSatTokens, undo: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().satMaterial(tokens, SatSlot.Popover, SatShapes.of(16.dp)).padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SatText(message, tokens.inkPrimary, Modifier.weight(1f).padding(vertical = 8.dp))
        if (undo != null) Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clickable(onClick = undo), contentAlignment = Alignment.Center) {
            SatText(stringResource(R.string.sat_undo), tokens.accentInk, strong = true)
        }
    }
}
