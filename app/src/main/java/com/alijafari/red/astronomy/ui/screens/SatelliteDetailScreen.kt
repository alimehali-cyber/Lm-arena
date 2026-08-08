package com.alijafari.red.astronomy.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.ui.theme.AccentPrimary
import com.alijafari.red.astronomy.util.toPersianDigits
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteDetailScreen(
    satelliteItem: SatelliteItem,
    userLocation: UserLocation,
    language: AppLanguage,
    onBack: () -> Unit,
    simulationTimestampMs: Long = System.currentTimeMillis()
) {
    val isFa = language == AppLanguage.PERSIAN
    val context = LocalContext.current

    val state = remember(satelliteItem, simulationTimestampMs, userLocation) {
        SatelliteEngine.calculateSatelliteState(
            satellite = satelliteItem,
            timestampMs = simulationTimestampMs,
            userLatDeg = userLocation.latitude,
            userLonDeg = userLocation.longitude
        )
    }

    val passes = remember(satelliteItem, userLocation, simulationTimestampMs) {
        ISSEngine.predictPasses(
            userLatDeg = userLocation.latitude,
            userLonDeg = userLocation.longitude,
            startTimestampMs = simulationTimestampMs,
            tle = satelliteItem.defaultTle,
            scanDays = 3,
            visibleOnly = false
        )
    }

    val nextPass = passes.firstOrNull()

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 2.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("sat_detail_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isFa) satelliteItem.nameFa else satelliteItem.nameEn,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "NORAD ${satelliteItem.noradId} • ${satelliteItem.designation}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F0D1B),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AccentPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SatelliteAlt,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isFa) satelliteItem.category.labelFa else satelliteItem.category.labelEn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentPrimary
                                )
                                Text(
                                    text = if (isFa) satelliteItem.nameFa else satelliteItem.nameEn,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        // Visibility Badge
                        val badgeBg = if (state.isNakedEyeVisible) Color(0xFF2DC653) else Color(0xFF4A5568)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeBg.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, badgeBg.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isFa) state.visibilityVerdictFa else state.visibilityVerdictEn,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = badgeBg,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isFa) satelliteItem.descriptionFa else satelliteItem.descriptionEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Real-Time Position Telemetry
            Text(
                text = if (isFa) "موقعیت و پارامترهای مداری زنده" else "Live Position & Orbital Parameters",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            val topo = state.topocentric
            val topoLat = String.format(Locale.US, "%.4f°", topo.subLatDeg)
            val topoLon = String.format(Locale.US, "%.4f°", topo.subLonDeg)
            val topoAlt = String.format(Locale.US, "%.1f km", topo.satAltKm)
            val topoSpeed = String.format(Locale.US, "%.2f km/s", topo.velocityKmS)
            val topoAz = String.format(Locale.US, "%.1f°", topo.azimuthDeg)
            val topoEl = String.format(Locale.US, "%.1f°", topo.elevationDeg)
            val topoDist = String.format(Locale.US, "%.1f km", topo.rangeKm)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = if (isFa) "عرض جغرافیایی" else "Sub-Lat",
                            value = if (isFa) topoLat.toPersianDigits() else topoLat
                        )
                        TelemetryItem(
                            label = if (isFa) "طول جغرافیایی" else "Sub-Lon",
                            value = if (isFa) topoLon.toPersianDigits() else topoLon
                        )
                        TelemetryItem(
                            label = if (isFa) "ارتفاع مداری" else "Altitude",
                            value = if (isFa) topoAlt.toPersianDigits() else topoAlt
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = if (isFa) "سرعت مداری" else "Velocity",
                            value = if (isFa) topoSpeed.toPersianDigits() else topoSpeed
                        )
                        TelemetryItem(
                            label = if (isFa) "زاویه سمت (Azimuth)" else "Azimuth",
                            value = if (isFa) topoAz.toPersianDigits() else topoAz
                        )
                        TelemetryItem(
                            label = if (isFa) "زاویه ارتفاع (Elevation)" else "Elevation",
                            value = if (isFa) topoEl.toPersianDigits() else topoEl
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = if (isFa) "فاصله از ناظر" else "Range to Observer",
                            value = if (isFa) topoDist.toPersianDigits() else topoDist
                        )
                        TelemetryItem(
                            label = if (isFa) "وضعیت تابش" else "Illumination",
                            value = if (topo.isSunlit) (if (isFa) "روشن (خورشید)" else "Sunlit") else (if (isFa) "در سایه زمین" else "In Shadow")
                        )
                    }
                }
            }

            // Scientific Naked-Eye Visibility Assessment Card
            Text(
                text = if (isFa) "ارزیابی علمی قابلیت رؤیت با چشم" else "Scientific Naked-Eye Visibility Assessment",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val magStr = String.format(Locale.US, "%+.1f", state.apparentMagnitude)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "قدر ظاهری محاسبه‌شده:" else "Calculated Apparent Magnitude:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isFa) "mag $magStr".toPersianDigits() else "mag $magStr",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentPrimary
                        )
                    }

                    Text(
                        text = if (isFa) state.reasonFa else state.reasonEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Next Pass Section
            if (nextPass != null) {
                Text(
                    text = if (isFa) "گذر بعدی بر فراز موقعیت شما" else "Next Visible Pass Over Your Location",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                val startStr = sdf.format(Date(nextPass.startTimeMs))
                val maxStr = sdf.format(Date(nextPass.maxTimeMs))
                val endStr = sdf.format(Date(nextPass.endTimeMs))
                val durMins = nextPass.passDurationSec / 60

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) nextPass.classification.labelFa else nextPass.classification.labelEn,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(nextPass.classification.colorHex)
                            )
                            Text(
                                text = if (isFa) "$durMins دقیقه".toPersianDigits() else "$durMins mins",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TelemetryItem(
                                label = if (isFa) "شروع گذر" else "Rise Time",
                                value = if (isFa) startStr.toPersianDigits() else startStr
                            )
                            TelemetryItem(
                                label = if (isFa) "اوج ارتفاع" else "Max Elev",
                                value = if (isFa) "${String.format(Locale.US, "%.0f°", nextPass.maxElevationDeg)}".toPersianDigits() else "${String.format(Locale.US, "%.0f°", nextPass.maxElevationDeg)}"
                            )
                            TelemetryItem(
                                label = if (isFa) "پایان گذر" else "Set Time",
                                value = if (isFa) endStr.toPersianDigits() else endStr
                            )
                        }

                        Button(
                            onClick = {
                                AstroNotificationManager.scheduleUpcomingIssPasses(
                                    context = context,
                                    userLocation = userLocation,
                                    leadMinutes = 10
                                )
                                Toast.makeText(
                                    context,
                                    if (isFa) "هشدار ۱۰ دقیقه قبل از گذر تنظیم شد!" else "Alert set 10 mins prior to pass!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("schedule_pass_alert"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isFa) "تنظیم هشدار یادآوری گذر" else "Schedule Pass Reminder Alert")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
