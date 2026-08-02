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
import com.example.domain.AppLanguage
import com.example.domain.CelestialObject
import com.example.ui.MainUiState
import com.example.ui.MainViewModel

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
        containerColor = Color(0xFF1C1B1F),
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
                        tint = if (uiState.isDetailFavorite) Color(0xFFFFB703) else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isFa) "مختصات و داده‌های علمی نجومی" else "Scientific Astronomical Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "RA (بعد)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(text = CoordinateEngine.formatRA(obj.raDeg), style = MaterialTheme.typography.bodyLarge)
                        }
                        Column {
                            Text(text = "Dec (میل)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(text = CoordinateEngine.formatDec(obj.decDeg), style = MaterialTheme.typography.bodyLarge)
                        }
                        Column {
                            Text(text = "Magnitude (قدر)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            val magStr = String.format("%.1f", obj.magnitude)
                            Text(text = if (isFa) TimeEngine.formatPersianNumbers(magStr) else magStr, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = if (isFa) "ارتفاع (Altitude)" else "Altitude", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            val altStr = String.format("%.1f°", horiz.altitudeDeg)
                            Text(text = if (isFa) TimeEngine.formatPersianNumbers(altStr) else altStr, style = MaterialTheme.typography.bodyLarge)
                        }
                        Column {
                            Text(text = if (isFa) "سمت (Azimuth)" else "Azimuth", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            val azStr = String.format("%.1f°", horiz.azimuthDeg)
                            Text(text = if (isFa) TimeEngine.formatPersianNumbers(azStr) else azStr, style = MaterialTheme.typography.bodyLarge)
                        }
                        Column {
                            Text(text = if (isFa) "فاصله" else "Distance", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            val distStr = if (obj.distanceLightYears < 0.001) "< 1 AU" else String.format("%,.0f ly", obj.distanceLightYears)
                            Text(text = if (isFa) TimeEngine.formatPersianNumbers(distStr) else distStr, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Description & Observation Tips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isFa) "توضیحات علمی" else "Scientific Description",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isFa) obj.descriptionFa else obj.descriptionEn,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isFa) "راهنما و راهکار رصد" else "Observation Tip",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFB703)
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
                                    tint = if (star <= ratingInput) Color(0xFFFFB703) else Color.Gray
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
