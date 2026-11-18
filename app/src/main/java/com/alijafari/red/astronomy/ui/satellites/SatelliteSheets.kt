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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
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
import com.alijafari.red.astronomy.ui.theme.squircleShape
import java.util.Calendar
import java.util.TimeZone

/** In-place modal surface; no platform dialog, window blur, or elevation. Drag its handle for detents. */
@Composable
internal fun SatBottomSheet(
    tokens: ZigSatTokens,
    title: String,
    tag: String,
    onDismiss: () -> Unit,
    half: Boolean = false,
    side: Boolean = false,
    initialFull: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val reduce = LocalSatReduceMotion.current
        var expanded by remember(tag) { mutableStateOf(initialFull || !half) }
        val close by rememberUpdatedState(onDismiss)
        val shape = if (side) squircleShape(SatRadius.sheet) else squircleShape(topStart = 28.dp, topEnd = 28.dp)
        val targetHeight = maxHeight * if (expanded || side) .85f else .50f
        val height by animateDpAsState(targetHeight, tween(if (reduce) 0 else 240), label = "sheet-detent")
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .40f)).pointerInput(Unit) {
            detectTapGestures { close() }
        })
        Column(Modifier.align(if (side) Alignment.CenterEnd else Alignment.BottomCenter)
            .then(if (side) Modifier.padding(end = 12.dp).width(380.dp) else Modifier.fillMaxWidth())
            .height(height).testTag(tag).semantics { paneTitle = title; isTraversalGroup = true }
            .satMaterial(tokens, SatSlot.Sheet, shape, backing = tokens.card)
            .pointerInput(Unit) { detectTapGestures { /* Do not send taps on sheet chrome to the scrim. */ } }
            .navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
            val handleDescription = stringResource(if (!half || side) R.string.sat_close else if (expanded) R.string.sat_collapse else R.string.sat_expand)
            Box(Modifier.fillMaxWidth().height(48.dp)
                .clickable(role = Role.Button) { if (half && !side) expanded = !expanded else onDismiss() }
                .semantics { contentDescription = handleDescription }
                .pointerInput(half, side) {
                    var travel = 0f
                    detectVerticalDragGestures(onDragStart = { travel = 0f }, onDragEnd = {
                        if (travel < -24.dp.toPx() && half && !side) expanded = true
                        if (travel > 24.dp.toPx()) {
                            if (expanded && half && !side) expanded = false else close()
                        }
                    }) { change, delta -> change.consume(); travel += delta }
                }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(36.dp, 4.dp).background(tokens.inkTertiary.copy(alpha = .6f), CircleShape))
            }
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
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
    Box(modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clip(RedShapes.capsule)
        .background(if (primary && enabled) tokens.selectedChip else tokens.cardAlt)
        .border(1.dp, if (destructive) tokens.accentInk.copy(alpha = .45f) else tokens.border, RedShapes.capsule)
        .clickable(role = Role.Button, enabled = enabled, onClick = onClick)
        .then(if (testTag == null) Modifier else Modifier.testTag(testTag)).padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        SatText(label, if (enabled) ink else tokens.inkSecondary, size = 13, strong = true)
    }
}

@Composable
internal fun SatDataStatusPopover(data: SatStatusData, tokens: ZigSatTokens, tz: TimeZone,
    onClose: () -> Unit, onReturnToLive: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.widthIn(max = 330.dp).fillMaxWidth().testTag("data_status_popover")
        .satMaterial(tokens, SatSlot.Popover, squircleShape(16.dp))
        .pointerInput(Unit) { detectTapGestures {} }.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SatSheetTitle(stringResource(R.string.sat_status), null, tokens,
            trailing = { ZigSatIconButton(Icons.Filled.Close, stringResource(R.string.sat_close), tokens, onClose) })
        val source = stringResource(when (data.tleSource) {
            TleDataSource.LIVE_NETWORK -> R.string.sat_source_live
            TleDataSource.MEMORY_CACHE, TleDataSource.PERSISTED_CACHE -> R.string.sat_source_cache
            else -> R.string.sat_source_bundled
        })
        StatusRow(stringResource(R.string.sat_source), "${SatFmt.digits("SGP4", data.isFa)} · $source", tokens)
        StatusRow(stringResource(R.string.sat_epoch), data.tleEpochMillis?.let { SatFmt.dateTime(it, data.isFa, tz) }
            ?: stringResource(R.string.sat_unknown), tokens)
        StatusRow(stringResource(R.string.sat_age), data.tleAgeDays?.let {
            stringResource(R.string.sat_age_days, SatFmt.number(it, 1, data.isFa))
        } ?: stringResource(R.string.sat_unknown), tokens)
        StatusRow(stringResource(R.string.sat_connection), stringResource(if (data.isOnline) R.string.sat_online else R.string.sat_offline), tokens)
        if ((data.tleAgeDays ?: 0.0) > 7.0) SatText(stringResource(R.string.sat_stale), tokens.accentInk)
        if (!data.isLive) SatDialogButton(stringResource(R.string.sat_return_live), tokens, onReturnToLive,
            primary = true, modifier = Modifier.fillMaxWidth())
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
internal fun SatLayersPopover(tokens: ZigSatTokens, lighting: MapTheme, legend: Boolean, onLighting: (MapTheme) -> Unit,
    onLegend: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.width(224.dp).testTag("map_theme_selector").satMaterial(tokens, SatSlot.Popover, squircleShape(16.dp))
        .pointerInput(Unit) { detectTapGestures {} }.padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        SatText(stringResource(R.string.sat_lighting), tokens.inkPrimary, size = 13, strong = true)
        Row(Modifier.background(tokens.cardAlt, CircleShape), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MapTheme.entries.forEach { mode ->
                val (label, icon) = when (mode) {
                    MapTheme.AUTO -> R.string.sat_auto to Icons.Filled.BrightnessAuto
                    MapTheme.DARK -> R.string.sat_dark to Icons.Filled.DarkMode
                    MapTheme.LIGHT -> R.string.sat_light to Icons.Filled.LightMode
                }
                Column(Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape)
                    .background(if (lighting == mode) tokens.selectedChip else Color.Transparent)
                    .clickable(role = Role.Button) { onLighting(mode) }.padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val color = if (lighting == mode) Color.White else tokens.inkPrimary
                    Icon(icon, null, Modifier.size(14.dp), color)
                    SatText(stringResource(label), color, size = 11)
                }
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            val label = stringResource(R.string.sat_legend)
            SatText(label, tokens.inkPrimary, Modifier.weight(1f), strong = true)
            Switch(legend, onLegend, colors = SwitchDefaults.colors(checkedTrackColor = tokens.selectedChip),
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = label })
        }
    }
}

@Composable
internal fun SatFleetSheet(fleet: List<SatTokenUi>, category: SatelliteCategory, tokens: ZigSatTokens, isFa: Boolean,
    onScope: (SatelliteCategory) -> Unit, onPick: (SatTokenUi) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val filterPrefs = remember(context) { context.getSharedPreferences("astro_app_prefs", android.content.Context.MODE_PRIVATE) }
    var query by rememberSaveable { mutableStateOf("") }
    var aboveHorizonOnly by rememberSaveable { mutableStateOf(filterPrefs.getBoolean("sat_filter_above_horizon", false)) }
    var nakedEyeCapableOnly by rememberSaveable { mutableStateOf(filterPrefs.getBoolean("sat_filter_naked_eye", false)) }
    val title = stringResource(R.string.sat_fleet)
    SatBottomSheet(tokens, title, "fleet_sheet", onDismiss, half = true) {
        SatSheetTitle("$title · ${SatFmt.int(fleet.size, isFa)}", null, tokens)
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().testTag("fleet_search_field"),
            singleLine = true, label = { Text(stringResource(R.string.sat_search), fontFamily = LocalAppFontFamily.current) },
            textStyle = RedTypographyTokens.bodySecondary.copy(fontFamily = LocalAppFontFamily.current),
            trailingIcon = { if (query.isNotEmpty()) Icon(Icons.Filled.Close, stringResource(R.string.sat_clear_search),
                Modifier.size(48.dp).clickable { query = "" }.padding(15.dp), tint = tokens.inkSecondary) })
        SatScopeRow(category, tokens, isFa, onScope)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ZigSatChip(stringResource(R.string.sat_filter_above), aboveHorizonOnly, tokens,
                { aboveHorizonOnly = !aboveHorizonOnly; filterPrefs.edit().putBoolean("sat_filter_above_horizon", aboveHorizonOnly).apply() },
                testTag = "filter_above_horizon")
            ZigSatChip(stringResource(R.string.sat_filter_naked_eye), nakedEyeCapableOnly, tokens,
                { nakedEyeCapableOnly = !nakedEyeCapableOnly; filterPrefs.edit().putBoolean("sat_filter_naked_eye", nakedEyeCapableOnly).apply() },
                testTag = "filter_naked_eye")
        }
        fun normalizeSearchText(value: String): String = value.lowercase()
            .replace('ي', 'ی').replace('ك', 'ک').replace(0x200C.toChar(), ' ')
            .map { ch -> when (ch) {
                in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                else -> ch
            } }.joinToString("")
        val queryKey = normalizeSearchText(query.trim())
        val filtered = fleet.asSequence()
            .filter { row ->
                queryKey.isBlank() || listOf(row.shortName, row.fullName, row.id, row.satellite.noradId.toString(),
                    row.satellite.designation, row.satellite.operatorEn, row.satellite.operatorFa,
                    row.satellite.category.labelEn, row.satellite.category.labelFa)
                    .any { normalizeSearchText(it).contains(queryKey) }
            }
            .filter { !aboveHorizonOnly || it.elevationDeg > 0.0 }
            .filter { !nakedEyeCapableOnly || it.isNakedEye }
            .sortedByDescending { it.elevationDeg }.toList()
        val above = fleet.count { it.elevationDeg > 0 }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val segments = fleet.size.coerceIn(4, 10)
            val filled = kotlin.math.ceil(segments * above.toDouble() / fleet.size.coerceAtLeast(1)).toInt()
            repeat(segments) { index ->
                ZigStatusDot(if (index < filled) tokens.nakedEye else tokens.border, false, 5.dp)
            }
            SatText(stringResource(R.string.sat_above, SatFmt.int(above, isFa)), tokens.inkSecondary)
            Spacer(Modifier.weight(1f))
            SatText(stringResource(R.string.sat_results_count, SatFmt.int(filtered.size, isFa)), tokens.inkTertiary, size = 10)
        }
        if (filtered.isEmpty()) SatText(stringResource(R.string.sat_no_fleet), tokens.inkSecondary)
        filtered.forEach { row -> key(row.id) {
            val shape = squircleShape(16.dp)
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
                .background(if (row.isSelected) tokens.selectedFill else tokens.cardAlt)
                .border(if (row.isSelected) 2.dp else 1.dp, if (row.isSelected) tokens.live else tokens.border, shape)
                .clickable(role = Role.Button) { onPick(row) }.testTag("fleet_sat_${row.id}")
                .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    SatAvatar(row.satellite, tokens, 32.dp)
                    Box(Modifier.align(Alignment.BottomEnd).size(7.dp).background(tokens.cardAlt, CircleShape)
                        .then(if (row.isNakedEye) Modifier.background(tokens.nakedEye, CircleShape) else Modifier.border(1.dp, tokens.inkSecondary, CircleShape)))
                }
                Column(Modifier.weight(1f)) {
                    Text(row.shortName, color = tokens.inkPrimary, style = RedTypographyTokens.badge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (row.fullName != row.shortName) SatText(row.fullName, tokens.inkSecondary, size = 10)
                    SatText(SatFmt.degrees(row.elevationDeg, isFa, signed = true),
                        if (row.elevationDeg >= 0) tokens.inkPrimary else tokens.inkTertiary, size = 11)
                }
                val label = if (row.isTrain) stringResource(R.string.sat_train_short, SatFmt.int(row.trainCount, isFa))
                    else SatFmt.categoryLabel(row.satellite.category, isFa)
                SatText(label, tokens.inkPrimary, Modifier.background(tokens.border, squircleShape(10.dp)).padding(horizontal = 7.dp, vertical = 5.dp), size = 10)
            }
        } }
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
    SatBottomSheet(tokens, title, "alerts_sheet", onDismiss) {
        SatSheetTitle(title, rolling, tokens, trailing = {
            Switch(enabled, { enabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = tokens.selectedChip),
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("alerts_master_switch").semantics { contentDescription = title })
        })
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
    SatBottomSheet(tokens, title, "guide_sheet", onDismiss) {
        SatSheetTitle(title, null, tokens)
        GuideRow(Icons.Filled.Swipe, stringResource(R.string.sat_gesture_title), stringResource(R.string.sat_gesture_body), tokens)
        GuideRow(Icons.Filled.DarkMode, stringResource(R.string.sat_lighting), stringResource(R.string.sat_lighting_body), tokens)
        GuideRow(Icons.Filled.TouchApp, stringResource(R.string.sat_focus_title), stringResource(R.string.sat_focus_body), tokens)
        GuideRow(Icons.Filled.Schedule, stringResource(R.string.sat_scrub_title), stringResource(R.string.sat_scrub_body), tokens)
        SatText(stringResource(R.string.sat_legend), tokens.inkSecondary, strong = true)
        SatLegendPanel(tokens, isFa, Modifier.fillMaxWidth())
        SatDialogButton(stringResource(R.string.sat_got_it), tokens, onDismiss, primary = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun GuideRow(icon: ImageVector, title: String, body: String, tokens: ZigSatTokens) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(32.dp).background(tokens.live.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
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
        Column(Modifier.width(232.dp).testTag("reminder_popover").satMaterial(tokens, SatSlot.Popover, squircleShape(20.dp))
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
    Row(modifier.fillMaxWidth().satMaterial(tokens, SatSlot.Popover, squircleShape(16.dp)).padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SatText(message, tokens.inkPrimary, Modifier.weight(1f).padding(vertical = 8.dp))
        if (undo != null) Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clickable(onClick = undo), contentAlignment = Alignment.Center) {
            SatText(stringResource(R.string.sat_undo), tokens.accentInk, strong = true)
        }
    }
}
