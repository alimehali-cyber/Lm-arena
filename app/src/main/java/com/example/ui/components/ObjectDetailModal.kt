package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astro_engine.*
import com.example.domain.*
import com.example.ui.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailModal(
    obj: CelestialObject,
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val isFa = uiState.language == AppLanguage.PERSIAN

    val jd = remember { TimeEngine.getJulianDate() }
    val lastDeg = remember(uiState.userLocation) {
        TimeEngine.getLAST(jd, uiState.userLocation.longitude)
    }
    val horiz = remember(lastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
            lastDeg,
            uiState.userLocation.latitude
        )
    }
    val sunPos = remember { SunEngine.calculatePosition(jd) }
    val sunHoriz = remember(lastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            lastDeg,
            uiState.userLocation.latitude
        )
    }
    val moonData = remember(jd) { MoonEngine.calculateMoon(jd) }
    val obs = remember(horiz, sunHoriz, moonData, uiState.bortleClass) {
        ObservabilityEngine.calculateObservability(
            altitudeDeg = horiz.altitudeDeg,
            sunAltitudeDeg = sunHoriz.altitudeDeg,
            moonIlluminationPercent = moonData.illuminationPercent,
            objectMagnitude = obj.magnitude,
            bortleClass = uiState.bortleClass
        )
    }

    var showAddLogDialog by remember { mutableStateOf(false) }
    var notesInput by remember { mutableStateOf("") }
    var ratingInput by remember { mutableStateOf(5) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BackgroundCard,
        modifier = Modifier.testTag("object_detail_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Name & Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFa) obj.nameFa else obj.nameEn,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val constName = if (isFa) obj.constellationFa else obj.constellationEn
                    Text(
                        text = "$constName • ${obj.category}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleCurrentDetailFavorite() },
                    modifier = Modifier.testTag("modal_favorite_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isDetailFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (uiState.isDetailFavorite) AccentPrimary else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Observability Badge Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = obs.level.color.copy(alpha = 0.15f),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFa) "امتیاز و وضعیت رصدپذیری" else "Observability Status",
                            style = MaterialTheme.typography.labelSmall,
                            color = obs.level.color
                        )
                        Text(
                            text = if (isFa) obs.level.nameFa else obs.level.nameEn,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = obs.level.color
                        )
                        Text(
                            text = if (isFa) obs.bestObservationTimeFa else obs.bestObservationTimeEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${obs.scorePercent}%",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = obs.level.color
                    )
                }
            }

            // Equatorial & Horizontal Scientific Data Matrix
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isFa) "مختصات و داده‌های علمی نجومی" else "Scientific Astronomical Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "RA (بعد)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CoordinateEngine.formatRA(obj.raDeg), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text(text = "Dec (میل)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = CoordinateEngine.formatDec(obj.decDeg), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text(text = "Magnitude (قدر)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val magStr = String.format("%.1f", obj.magnitude)
                            Text(text = if (isFa) TimeEngine.formatPersianNumbers(magStr) else magStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = if (isFa) "ارتفاع (Altitude)" else "Altitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val altStr = String.format("%.1f°", horiz.altitudeDeg)
                            Text(text = if (isFa) TimeEngine.formatPersianNumbers(altStr) else altStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text(text = if (isFa) "سمت (Azimuth)" else "Azimuth", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val azStr = String.format("%.1f°", horiz.azimuthDeg)
                            Text(text = if (isFa) TimeEngine.formatPersianNumbers(azStr) else azStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text(text = if (isFa) "فاصله" else "Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val distStr = if (obj.distanceLightYears < 0.001) "< 1 AU" else String.format("%,.0f ly", obj.distanceLightYears)
                            Text(text = if (isFa) TimeEngine.formatPersianNumbers(distStr) else distStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Rise, Transit, Set Times Card
            val riseSetTransit = remember(obj, uiState.userLocation, jd, isFa) {
                CoordinateEngine.calculateRiseSetTransit(
                    raDeg = obj.raDeg,
                    decDeg = obj.decDeg,
                    latDeg = uiState.userLocation.latitude,
                    lonDeg = uiState.userLocation.longitude,
                    jd = jd,
                    isFa = isFa
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isFa) "زمان‌بندی طلوع، اوج ارتفاع و غروب امروز" else "Rise, Transit & Set Schedule Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Rise
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = if (isFa) "🌅 طلوع" else "🌅 Rise",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = riseSetTransit.riseTimeStr,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Transit (Meridian Peak)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isFa) "☀️ اوج ارتفاع (ترانزیت)" else "☀️ Peak Transit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = riseSetTransit.transitTimeStr,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccentPrimary
                            )
                        }

                        // Set
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isFa) "🌇 غروب" else "🌇 Set",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = riseSetTransit.setTimeStr,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Description & Observation Tips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isFa) "توضیحات علمی" else "Scientific Description",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentPrimary
                )
                Text(
                    text = if (isFa) obj.descriptionFa else obj.descriptionEn,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // High Precision System Feature Badges (Jupiter Moons / Moon Libration / Planet Phase Angle)
            if (obj.id == "planet_jupiter") {
                val jupSystem = remember(jd) { JupiterMoonsEngine.calculateJupiterMoons(jd) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isFa) "اقمار گالیله‌ای مشتری & لکه سرخ بزرگ" else "Galilean Moons & Great Red Spot",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentSecondary
                        )
                        val grsText = if (jupSystem.isGrsVisible) {
                            if (isFa) "لکه سرخ بزرگ (GRS): هم‌اکنون روی قرص مشتری قابل رصد است" else "Great Red Spot (GRS): Currently Visible on disk"
                        } else {
                            if (isFa) "لکه سرخ بزرگ (GRS): در پشت یا سمت دور مشتری قرار دارد" else "Great Red Spot (GRS): On far side of Jupiter"
                        }
                        Text(text = grsText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            jupSystem.moons.forEach { m ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = if (isFa) m.moon.nameFa else m.moon.nameEn, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    val statusStr = when (m.phenomenon) {
                                        JupiterMoonsEngine.MoonPhenomenon.VISIBLE -> if (isFa) "رصدپذیر" else "Visible"
                                        JupiterMoonsEngine.MoonPhenomenon.IN_TRANSIT -> if (isFa) "گذر" else "Transit"
                                        JupiterMoonsEngine.MoonPhenomenon.OCCULTED -> if (isFa) "مختفی" else "Occulted"
                                        JupiterMoonsEngine.MoonPhenomenon.IN_ECLIPSE -> if (isFa) "خسوف" else "Eclipsed"
                                        JupiterMoonsEngine.MoonPhenomenon.SHADOW_TRANSIT -> if (isFa) "سایه" else "Shadow"
                                    }
                                    Text(text = statusStr, style = MaterialTheme.typography.bodySmall, color = AccentPrimary)
                                    Text(
                                        text = String.format("%.1f Rj", m.xRJ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (obj.id == "moon_luna") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isFa) "رخ‌گردی (Libration) و زمین‌تاب ماه" else "Lunar Libration & Earthshine",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = if (isFa) "رخ‌گردی در طول" else "Lon Libration", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(text = String.format("%.2f°", moonData.librationLonDeg), style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text(text = if (isFa) "رخ‌گردی در عرض" else "Lat Libration", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(text = String.format("%.2f°", moonData.librationLatDeg), style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text(text = if (isFa) "روشنایی زمین‌تاب" else "Earthshine", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(text = String.format("%.1f%%", moonData.earthshinePercent), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isFa) "راهنما و راهکار رصد" else "Observation Tip",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentSecondary
                )
                Text(
                    text = if (isFa) obj.observationTipFa else obj.observationTipEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Add to Observation Log Button
            Button(
                onClick = { showAddLogDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_log_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.EditNote, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isFa) "ثبت در دفترچه رصد (Observation Log)" else "Add to Observation Log")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Add Observation Dialog
    if (showAddLogDialog) {
        AlertDialog(
            onDismissRequest = { showAddLogDialog = false },
            title = {
                Text(text = if (isFa) "ثبت رصد جدید" else "New Observation Entry")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isFa) "یادداشت رصد برای ${obj.nameFa}:" else "Notes for ${obj.nameEn}:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(text = if (isFa) "شرایط جوی، تجهیزات رصدی، شفافیت آسمان..." else "Weather, telescope, clarity...") }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = if (isFa) "امتیاز کیفیت رصد:" else "Rating:")
                        for (star in 1..5) {
                            IconButton(onClick = { ratingInput = star }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star $star",
                                    tint = if (star <= ratingInput) AccentPrimary else Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addObservationLog(notesInput, ratingInput)
                        showAddLogDialog = false
                    }
                ) {
                    Text(text = if (isFa) "ذخیره" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLogDialog = false }) {
                    Text(text = if (isFa) "انصراف" else "Cancel")
                }
            }
        )
    }
}
