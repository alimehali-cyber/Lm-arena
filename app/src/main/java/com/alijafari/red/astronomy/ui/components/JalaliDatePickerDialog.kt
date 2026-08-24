package com.alijafari.red.astronomy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.ui.theme.AccentPrimary
import com.alijafari.red.astronomy.ui.theme.CardBorder
import com.alijafari.red.astronomy.ui.theme.CardSurface
import com.alijafari.red.astronomy.ui.theme.TextPrimary
import com.alijafari.red.astronomy.ui.theme.TextSecondary
import java.util.Calendar

private val PERSIAN_MONTHS_FA = arrayOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

@Composable
fun JalaliDatePickerDialog(
    initialTimestampMs: Long,
    onDismissRequest: () -> Unit,
    onDateConfirmed: (Long) -> Unit
) {
    val initialSh = remember(initialTimestampMs) {
        TimeEngine.toSolarHijri(initialTimestampMs)
    }

    var selectedYear by remember { mutableIntStateOf(initialSh.year) }
    var selectedMonth by remember { mutableIntStateOf(initialSh.month) } // 1..12
    var selectedDay by remember { mutableIntStateOf(initialSh.day) }

    val daysInCurrentMonth = remember(selectedYear, selectedMonth) {
        TimeEngine.getSolarHijriDaysInMonth(selectedYear, selectedMonth)
    }

    LaunchedEffect(daysInCurrentMonth) {
        if (selectedDay > daysInCurrentMonth) {
            selectedDay = daysInCurrentMonth
        }
    }

    var isSelectingYear by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("jalali_date_picker_dialog"),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "انتخاب تاریخ خورشیدی",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${TimeEngine.formatPersianNumbers(selectedDay.toString())} ${PERSIAN_MONTHS_FA[(selectedMonth - 1).coerceIn(0, 11)]} ${TimeEngine.formatPersianNumbers(selectedYear.toString())}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Year Header with Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedYear-- },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Previous Year", tint = TextPrimary)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.clickable { isSelectingYear = !isSelectingYear }
                    ) {
                        Text(
                            text = "سال ${TimeEngine.formatPersianNumbers(selectedYear.toString())}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { selectedYear++ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Next Year", tint = TextPrimary)
                    }
                }

                if (isSelectingYear) {
                    // Quick Year Selector Grid (Around current year +- 10)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val years = (selectedYear - 7..selectedYear + 8).toList()
                        items(years) { yr ->
                            val isSelected = yr == selectedYear
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AccentPrimary else CardSurface,
                                border = BorderStroke(1.dp, if (isSelected) AccentPrimary else CardBorder),
                                modifier = Modifier.clickable {
                                    selectedYear = yr
                                    isSelectingYear = false
                                }
                            ) {
                                Text(
                                    text = TimeEngine.formatPersianNumbers(yr.toString()),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Month Selector (3x4 Grid)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(12) { idx ->
                            val mNum = idx + 1
                            val isSelected = mNum == selectedMonth
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AccentPrimary.copy(alpha = 0.25f) else CardSurface,
                                border = BorderStroke(1.dp, if (isSelected) AccentPrimary else CardBorder),
                                modifier = Modifier.clickable { selectedMonth = mNum }
                            ) {
                                Text(
                                    text = PERSIAN_MONTHS_FA[idx],
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AccentPrimary else TextPrimary
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    // Day Selector Grid (1..31)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(daysInCurrentMonth) { dIdx ->
                            val dNum = dIdx + 1
                            val isSelected = dNum == selectedDay
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) AccentPrimary else Color.Transparent)
                                    .clickable { selectedDay = dNum },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = TimeEngine.formatPersianNumbers(dNum.toString()),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val calOrig = Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE).apply { timeInMillis = initialTimestampMs }
                    val hour = calOrig.get(Calendar.HOUR_OF_DAY)
                    val minute = calOrig.get(Calendar.MINUTE)
                    val second = calOrig.get(Calendar.SECOND)

                    val targetMs = TimeEngine.persianToTimestamp(
                        year = selectedYear,
                        month = selectedMonth,
                        day = selectedDay,
                        hour = hour,
                        minute = minute,
                        second = second,
                        timeZone = TimeEngine.TEHRAN_TIME_ZONE
                    )
                    onDateConfirmed(targetMs)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("تأیید", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("انصراف", color = TextSecondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
