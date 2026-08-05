package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.*
import com.alijafari.red.astronomy.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeMachineControlBar(
    state: TimeMachineState,
    isFa: Boolean,
    calendarSystem: CalendarSystem,
    onSimulatedTimeChange: (Long, String?, Boolean) -> Unit,
    onModeChange: (TimeMachineMode) -> Unit,
    onTogglePlay: () -> Unit,
    onSpeedChange: (TimeSimulationSpeed) -> Unit,
    onToggleReverse: () -> Unit,
    onToggleExpanded: () -> Unit,
    onReturnToLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPresetsModal by remember { mutableStateOf(false) }
    var showBirthdayModal by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    val activeTime = state.activeTimeMs

    // Format current simulated date & time
    val formattedDate = remember(activeTime, calendarSystem, isFa) {
        TimeEngine.formatDate(activeTime, calendarSystem, isFa)
    }
    val formattedTime = remember(activeTime, isFa) {
        TimeEngine.formatTime24h(activeTime, isFa)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("time_machine_container")
    ) {
        // --- Information Banner & Main Pill ---
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (state.mode == TimeMachineMode.SIMULATION) Color(0xFF181528).copy(alpha = 0.95f) else Color(0xFF10121D).copy(alpha = 0.88f),
            border = BorderStroke(
                width = 1.dp,
                color = if (state.mode == TimeMachineMode.SIMULATION) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Top Banner Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode Indicator Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (state.mode == TimeMachineMode.SIMULATION) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else StatusGood.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (state.mode == TimeMachineMode.SIMULATION) MaterialTheme.colorScheme.primary else StatusGood)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.mode == TimeMachineMode.SIMULATION) MaterialTheme.colorScheme.primary else StatusGood)
                            )
                            Text(
                                text = if (state.mode == TimeMachineMode.SIMULATION) {
                                    if (isFa) "شبیه‌سازی" else "SIMULATION"
                                } else {
                                    if (isFa) "اکنون زنده" else "LIVE NOW"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (state.mode == TimeMachineMode.SIMULATION) MaterialTheme.colorScheme.primary else StatusGood
                            )
                        }
                    }

                    // Simulation Info Header (Date & Time display)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.isBirthdayMode) {
                                if (isFa) "✨ آسمان روز تولد شما" else "✨ Your Birthday Sky"
                            } else {
                                state.eventName ?: "$formattedDate • $formattedTime"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.mode == TimeMachineMode.SIMULATION) {
                            Text(
                                text = if (isFa) "زمان: $formattedTime • گرینویچ+۳:۳۰ (${if (state.speed.multiplier >= 3600) if (isFa) state.speed.labelFa else state.speed.labelEn else if (isFa) state.speed.labelFa else state.speed.labelEn})"
                                else "Time: $formattedTime • UTC+03:30 (${state.speed.labelEn})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Expand / Collapse Toggle Button
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (state.isExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = "Time Machine Controls",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Expanded Controls Row
                AnimatedVisibility(
                    visible = state.isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Floating Pill Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 📅 Date Pill Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clickable { showDatePicker = true }
                                    .testTag("tm_date_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Text(text = formattedDate, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            // 🕒 Time Pill Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clickable { showTimePicker = true }
                                    .testTag("tm_time_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Time", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Text(text = formattedTime, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            // ▶ / ⏸ Play/Pause Pill Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (state.isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .clickable {
                                        if (state.mode == TimeMachineMode.LIVE) {
                                            onModeChange(TimeMachineMode.SIMULATION)
                                        }
                                        onTogglePlay()
                                    }
                                    .testTag("tm_play_pause_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = if (state.isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (state.isPlaying) (if (isFa) "توقف" else "Pause") else (if (isFa) "پخش" else "Play"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        color = if (state.isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // ⚡ Speed Pill Button
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clickable { showSpeedMenu = true }
                                        .testTag("tm_speed_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (state.isReverse) Icons.Default.FastRewind else Icons.Default.FastForward,
                                            contentDescription = "Speed",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isFa) state.speed.labelFa else state.speed.labelEn,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(
                                                    imageVector = if (state.isReverse) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (state.isReverse) (if (isFa) "جهت: مستقیم" else "Direction: Forward")
                                                    else (if (isFa) "جهت: معکوس" else "Direction: Reverse")
                                                )
                                            }
                                        },
                                        onClick = {
                                            onToggleReverse()
                                            showSpeedMenu = false
                                        }
                                    )
                                    HorizontalDivider()
                                    TimeSimulationSpeed.values().forEach { spd ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isFa) spd.labelFa else spd.labelEn,
                                                    fontWeight = if (spd == state.speed) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (spd == state.speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                onSpeedChange(spd)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 🔴 LIVE Reset Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (state.mode == TimeMachineMode.LIVE) StatusGood.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (state.mode == TimeMachineMode.LIVE) StatusGood else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clickable { onReturnToLive() }
                                    .testTag("tm_live_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusGood))
                                    Text(
                                        text = if (isFa) "زنده" else "LIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        color = StatusGood
                                    )
                                }
                            }
                        }

                        // Preset Shortcuts Row (Birthday Sky & Historical Events)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ✨ Birthday Sky Preset Button
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showBirthdayModal = true }
                                    .testTag("tm_birthday_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isFa) "✨ روز تولد من" else "✨ My Birthday Sky",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // 🌌 Historical Events Catalog Button
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showPresetsModal = true }
                                    .testTag("tm_presets_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isFa) "🌌 رویدادهای نجومی" else "🌌 Astronomical Events",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        // --- Timeline Scrubber Slider ---
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "1900", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "1950", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "2000", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "2026", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.primary)
                                Text(text = "2050", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "2100", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            val currentVal = activeTime.toFloat().coerceIn(
                                TimeMachineState.MIN_TIMESTAMP_MS.toFloat(),
                                TimeMachineState.MAX_TIMESTAMP_MS.toFloat()
                            )

                            Slider(
                                value = currentVal,
                                onValueChange = { newVal ->
                                    onSimulatedTimeChange(newVal.toLong(), null, false)
                                },
                                valueRange = TimeMachineState.MIN_TIMESTAMP_MS.toFloat()..TimeMachineState.MAX_TIMESTAMP_MS.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tm_timeline_slider")
                            )

                            // Quick Step Jump Buttons (-1D, -1H, +1H, +1D)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                AssistChip(
                                    onClick = { onSimulatedTimeChange(activeTime - 86400000L, null, false) },
                                    label = { Text(if (isFa) "۱ روز-" else "-1 Day", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = { onSimulatedTimeChange(activeTime - 3600000L, null, false) },
                                    label = { Text(if (isFa) "۱ ساعت-" else "-1 Hour", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = { onSimulatedTimeChange(activeTime + 3600000L, null, false) },
                                    label = { Text(if (isFa) "۱ ساعت+" else "+1 Hour", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = { onSimulatedTimeChange(activeTime + 86400000L, null, false) },
                                    label = { Text(if (isFa) "۱ روز+" else "+1 Day", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Material DatePicker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = activeTime
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedMillis ->
                            // Preserve current hour/minute/second
                            val calCurrent = Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE).apply { timeInMillis = activeTime }
                            val hour = calCurrent.get(Calendar.HOUR_OF_DAY)
                            val min = calCurrent.get(Calendar.MINUTE)

                            val calTarget = Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE).apply {
                                timeInMillis = pickedMillis
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, min)
                            }
                            onSimulatedTimeChange(calTarget.timeInMillis, null, false)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(if (isFa) "تأیید" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (isFa) "انصراف" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- Material TimePicker Dialog ---
    if (showTimePicker) {
        val cal = remember(activeTime) { Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE).apply { timeInMillis = activeTime } }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(Calendar.MINUTE, timePickerState.minute)
                        onSimulatedTimeChange(cal.timeInMillis, null, false)
                        showTimePicker = false
                    }
                ) {
                    Text(if (isFa) "تأیید" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(if (isFa) "انصراف" else "Cancel")
                }
            },
            title = {
                Text(
                    text = if (isFa) "انتخاب ساعت" else "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- Birthday Sky Modal ---
    if (showBirthdayModal) {
        var birthDateMillis by remember { mutableStateOf<Long?>(null) }
        var showBirthDatePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBirthdayModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✨", fontSize = 22.sp)
                    Text(
                        text = if (isFa) "مشاهده آسمان روز تولد" else "View My Birthday Sky",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isFa) "تاریخ تولد خود را انتخاب کنید تا موقعیت دقیق خورشید، ماه و صور فلکی در لحظه تولدتان محاسبه شود."
                        else "Enter your birth date to observe the exact astronomical sky at the moment of your birth.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBirthDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = birthDateMillis?.let { TimeEngine.formatDate(it, calendarSystem, isFa) }
                                    ?: (if (isFa) "برای انتخاب تاریخ کلیک کنید" else "Tap to select birth date"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (birthDateMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        birthDateMillis?.let { bDate ->
                            onSimulatedTimeChange(bDate, if (isFa) "آسمان روز تولد من" else "My Birthday Sky", true)
                        }
                        showBirthdayModal = false
                    },
                    enabled = birthDateMillis != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isFa) "نمایش آسمان" else "Show Sky")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBirthdayModal = false }) {
                    Text(if (isFa) "انصراف" else "Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )

        if (showBirthDatePicker) {
            val bPickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showBirthDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            bPickerState.selectedDateMillis?.let { birthDateMillis = it }
                            showBirthDatePicker = false
                        }
                    ) {
                        Text(if (isFa) "تأیید" else "OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBirthDatePicker = false }) {
                        Text(if (isFa) "انصراف" else "Cancel")
                    }
                }
            ) {
                DatePicker(state = bPickerState)
            }
        }
    }

    // --- Astronomical Historical Events Presets Catalog Modal ---
    if (showPresetsModal) {
        AlertDialog(
            onDismissRequest = { showPresetsModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌌", fontSize = 22.sp)
                    Text(
                        text = if (isFa) "رویدادهای تاریخی و نجومی" else "Astronomical & Historical Events",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HistoricalEventCatalog.PRESETS.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSimulatedTimeChange(
                                        preset.timestampMs,
                                        if (isFa) preset.titleFa else preset.titleEn,
                                        false
                                    )
                                    showPresetsModal = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isFa) preset.titleFa else preset.titleEn,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = TimeEngine.formatDate(preset.timestampMs, calendarSystem, isFa),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isFa) preset.descriptionFa else preset.descriptionEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetsModal = false }) {
                    Text(if (isFa) "بستن" else "Close")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
