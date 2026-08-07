package com.alijafari.red.astronomy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.EarthEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.rendering.Earth3DCanvas
import com.alijafari.red.astronomy.ui.rendering.rememberEarth3DRendererState
import com.alijafari.red.astronomy.ui.theme.IranSans
import com.alijafari.red.astronomy.util.toPersianDigits
import java.util.Calendar
import kotlin.math.abs

@Composable
fun EarthScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    var selectedHourOffsetFloat by remember { mutableStateOf(0f) }

    val baseJd = remember { TimeEngine.getJulianDate() }
    val currentJd = baseJd + (selectedHourOffsetFloat / 24.0)

    val subsolarPoint = remember(currentJd) {
        EarthEngine.calculateSubsolarPoint(currentJd)
    }

    val rendererState = rememberEarth3DRendererState()

    val userSolarElevation = remember(subsolarPoint, uiState.userLocation) {
        EarthEngine.calculateSolarElevation(
            uiState.userLocation.latitude,
            uiState.userLocation.longitude,
            subsolarPoint
        )
    }

    val activeCalendar = remember(selectedHourOffsetFloat) {
        val cal = Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE)
        cal.add(Calendar.MINUTE, (selectedHourOffsetFloat * 60).toInt())
        cal
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("earth_screen"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. EARTH 3D RENDER HERO CANVAS (~440dp height)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .testTag("earth_3d_hero_card")
            ) {
                // 3D Earth Interactive Canvas
                Earth3DCanvas(
                    subsolarPoint = subsolarPoint,
                    rendererState = rendererState,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Floating Time Machine Control Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { selectedHourOffsetFloat = (selectedHourOffsetFloat - 1f).coerceIn(-24f, 24f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Previous Hour", tint = MaterialTheme.colorScheme.primary)
                    }

                    val dateStr = TimeEngine.formatTime24h(activeCalendar.timeInMillis, isFa)
                    val offsetLabel = when {
                        selectedHourOffsetFloat == 0f -> if (isFa) "زمان زنده — $dateStr" else "Live Time — $dateStr"
                        selectedHourOffsetFloat > 0 -> if (isFa) "+${selectedHourOffsetFloat.toInt()} ساعت — $dateStr".toPersianDigits() else "+${selectedHourOffsetFloat.toInt()}h — $dateStr"
                        else -> if (isFa) "${selectedHourOffsetFloat.toInt()} ساعت — $dateStr".toPersianDigits() else "${selectedHourOffsetFloat.toInt()}h — $dateStr"
                    }

                    Text(
                        text = offsetLabel,
                        style = TextStyle(
                            fontFamily = IranSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    IconButton(
                        onClick = { selectedHourOffsetFloat = (selectedHourOffsetFloat + 1f).coerceIn(-24f, 24f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Next Hour", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (selectedHourOffsetFloat != 0f) {
                        Surface(
                            onClick = { selectedHourOffsetFloat = 0f },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = if (isFa) "زنده" else "LIVE",
                                style = TextStyle(
                                    fontFamily = IranSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Top Right Quick Controls (Reset View, Auto-Rotate Toggle)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { rendererState.resetView() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset View", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { rendererState.isAutoRotating = !rendererState.isAutoRotating },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (rendererState.isAutoRotating) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (rendererState.isAutoRotating) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Auto Rotate",
                            tint = if (rendererState.isAutoRotating) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Bottom Hint Banner
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (isFa) "چرخش و بزرگ‌نمایی ۳بعدی با لمس انگشت" else "Drag & Pinch to rotate 3D Earth",
                        style = TextStyle(
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        // 2. TOGGLES & DISPLAY OPTIONS ROW
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isFa) "لایه‌ها و جلوه‌های بصری" else "Visual Layers & Effects",
                    style = TextStyle(
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        LayerToggleChip(
                            label = if (isFa) "جو" else "Atmosphere",
                            icon = Icons.Default.Public,
                            isSelected = rendererState.showAtmosphere,
                            onClick = { rendererState.showAtmosphere = !rendererState.showAtmosphere }
                        )
                    }
                    item {
                        LayerToggleChip(
                            label = if (isFa) "ابرها" else "Clouds",
                            icon = Icons.Default.Cloud,
                            isSelected = rendererState.showClouds,
                            onClick = { rendererState.showClouds = !rendererState.showClouds }
                        )
                    }
                    item {
                        LayerToggleChip(
                            label = if (isFa) "چراغ‌های شب" else "Night Lights",
                            icon = Icons.Default.NightsStay,
                            isSelected = rendererState.showNightLights,
                            onClick = { rendererState.showNightLights = !rendererState.showNightLights }
                        )
                    }
                    item {
                        LayerToggleChip(
                            label = if (isFa) "مختصات" else "Grid",
                            icon = Icons.Default.Grid4x4,
                            isSelected = rendererState.showGrid,
                            onClick = { rendererState.showGrid = !rendererState.showGrid }
                        )
                    }
                    item {
                        LayerToggleChip(
                            label = if (isFa) "محور چرخش ۲۳.۴۴°" else "Axial Tilt 23.44°",
                            icon = Icons.Default.ShowChart,
                            isSelected = rendererState.showAxisLine,
                            onClick = { rendererState.showAxisLine = !rendererState.showAxisLine }
                        )
                    }
                }
            }
        }

        // 3. FAST CITY CENTER LOCATION PICKER
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isFa) "تمرکز سریع روی نقاط جهان" else "Center Location View",
                    style = TextStyle(
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                val locations = listOf(
                    Triple(if (isFa) "تهران" else "Tehran", 35.68f, 51.38f),
                    Triple(if (isFa) "لندن" else "London", 51.50f, -0.12f),
                    Triple(if (isFa) "نیویورک" else "New York", 40.71f, -74.00f),
                    Triple(if (isFa) "توکیو" else "Tokyo", 35.67f, 139.65f),
                    Triple(if (isFa) "سیدنی" else "Sydney", -33.86f, 151.20f),
                    Triple(if (isFa) "قاهره" else "Cairo", 30.04f, 31.23f),
                    Triple(if (isFa) "نقطه زیرخورشیدی" else "Subsolar Point", subsolarPoint.latDeg.toFloat(), subsolarPoint.lonDeg.toFloat())
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(locations) { (name, lat, lon) ->
                        Surface(
                            onClick = {
                                rendererState.isAutoRotating = false
                                rendererState.yawDeg = -lon
                                rendererState.pitchDeg = lat
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = name,
                                    style = TextStyle(
                                        fontFamily = IranSans,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. SUBSOLAR & PHYSICAL PROPERTIES CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("earth_details_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isFa) "پارامترهای فیزیکی و خورشیدی زمین" else "Solar & Physical Parameters",
                        style = TextStyle(
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    val subLatStr = String.format("%.2f°", abs(subsolarPoint.latDeg)).let {
                        val dir = if (subsolarPoint.latDeg >= 0) (if (isFa) "شمالی" else "N") else (if (isFa) "جنوبی" else "S")
                        if (isFa) "$it $dir".toPersianDigits() else "$it $dir"
                    }

                    val subLonStr = String.format("%.2f°", abs(subsolarPoint.lonDeg)).let {
                        val dir = if (subsolarPoint.lonDeg >= 0) (if (isFa) "شرقی" else "E") else (if (isFa) "غربی" else "W")
                        if (isFa) "$it $dir".toPersianDigits() else "$it $dir"
                    }

                    val tiltStr = if (isFa) "۲۳.۴۴ درجه".toPersianDigits() else "23.44°"
                    val rotSpeedStr = if (isFa) "۱,۶۷۰ کیلومتر بر ساعت".toPersianDigits() else "1,670 km/h"
                    val orbSpeedStr = if (isFa) "۲۹.۷۸ کیلومتر بر ثانیه".toPersianDigits() else "29.78 km/s"

                    val userSolarAltStr = String.format("%.1f°", userSolarElevation).let {
                        val status = if (userSolarElevation > 0) (if (isFa) "روز" else "Day") else (if (isFa) "شب" else "Night")
                        if (isFa) "$it ($status)".toPersianDigits() else "$it ($status)"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EarthTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.WbSunny,
                                label = if (isFa) "عرض نقطه زیرخورشیدی" else "Subsolar Latitude",
                                value = subLatStr
                            )

                            EarthTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Explore,
                                label = if (isFa) "طول نقطه زیرخورشیدی" else "Subsolar Longitude",
                                value = subLonStr
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EarthTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.RotateRight,
                                label = if (isFa) "انحراف محوری (Tilt)" else "Axial Tilt",
                                value = tiltStr
                            )

                            EarthTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Speed,
                                label = if (isFa) "سرعت چرخش استوا" else "Equatorial Speed",
                                value = rotSpeedStr
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EarthTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Public,
                                label = if (isFa) "سرعت مداری خورشید" else "Orbital Speed",
                                value = orbSpeedStr
                            )

                            EarthTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Brightness5,
                                label = if (isFa) "ارتفاع خورشید در موقعیت شما" else "Sun Alt at Your Location",
                                value = userSolarAltStr
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerToggleChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = IranSans,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun EarthTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = value,
                    style = TextStyle(
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
