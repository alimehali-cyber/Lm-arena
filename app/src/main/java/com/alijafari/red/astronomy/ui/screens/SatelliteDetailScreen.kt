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
import kotlin.math.max

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
            // 1. Hero Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
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

                        // Visibility Badge (Uses "قابل مشاهده نیست" when not visible in Persian)
                        val badgeBg = if (state.isNakedEyeVisible) Color(0xFF2DC653) else Color(0xFF64748B)
                        val badgeText = if (state.isNakedEyeVisible) (if (isFa) "با چشم غیرمسلح" else "Naked Eye Visible") else (if (isFa) "قابل مشاهده نیست" else "Not Visible")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeBg.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, badgeBg.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = badgeBg,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isFa) satelliteItem.descriptionFa else satelliteItem.descriptionEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // 2. COMPACT VISUAL TIMELINE COMPONENT FOR NEXT VISIBLE PASS
            if (nextPass != null) {
                Text(
                    text = if (isFa) "خط زمانی گذر بعدی بر فراز شما" else "Next Visible Pass Timeline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                NextPassTimelineProgressBar(
                    pass = nextPass,
                    nowMs = simulationTimestampMs,
                    isFa = isFa,
                    onScheduleReminder = {
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
                    }
                )
            }

            // 3. SCIENTIFIC MISSION & SATELLITE FACTS SECTION
            Text(
                text = if (isFa) "شناسنامه ماموریت و مشخصات علمی" else "Scientific Mission & Verified Specifications",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            val operatorText = if (isFa) {
                if (satelliteItem.operatorFa.isNotBlank()) satelliteItem.operatorFa else "ناسا / بین‌المللی"
            } else {
                if (satelliteItem.operatorEn.isNotBlank()) satelliteItem.operatorEn else "NASA / International"
            }

            val launchText = if (satelliteItem.launchDate.isNotBlank()) satelliteItem.launchDate else "1998"

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mission Meta Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = if (isFa) "سازمان / اپراتور" else "Operator",
                            value = operatorText
                        )
                        TelemetryItem(
                            label = if (isFa) "تاریخ پرتاب" else "Launch Date",
                            value = if (isFa) launchText.toPersianDigits() else launchText
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryItem(
                            label = if (isFa) "کد NORAD ID" else "NORAD ID",
                            value = satelliteItem.noradId.toString()
                        )
                        TelemetryItem(
                            label = if (isFa) "کد بین‌المللی" else "Int'l Designation",
                            value = satelliteItem.designation
                        )
                    }

                    if (satelliteItem.missionPurposeFa.isNotBlank() || satelliteItem.missionPurposeEn.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isFa) "هدف ماموریت:" else "Mission Purpose:",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPrimary
                            )
                            Text(
                                text = if (isFa) satelliteItem.missionPurposeFa else satelliteItem.missionPurposeEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (satelliteItem.scientificSignificanceFa.isNotBlank() || satelliteItem.scientificSignificanceEn.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isFa) "اهمیت علمی و تاریخی:" else "Scientific & Historical Significance:",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPrimary
                            )
                            Text(
                                text = if (isFa) satelliteItem.scientificSignificanceFa else satelliteItem.scientificSignificanceEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val facts = if (isFa) satelliteItem.verifiedFactsFa else satelliteItem.verifiedFactsEn
                    if (facts.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isFa) "حقایق علمی تاییدشده:" else "Verified Key Facts:",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPrimary
                            )
                            facts.forEach { fact ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("•", color = AccentPrimary, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (isFa) fact.toPersianDigits() else fact,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Real-Time Position Telemetry
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

            // 5. Scientific Naked-Eye Visibility Assessment Card
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

                    val reasonText = if (isFa) {
                        if (state.reasonFa.contains("غیرقابل")) "قابل مشاهده نیست" else state.reasonFa
                    } else state.reasonEn

                    Text(
                        text = reasonText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Compact visual timeline component displaying duration and status of next pass relative to now.
 */
@Composable
private fun NextPassTimelineProgressBar(
    pass: ISSEngine.ISSPass,
    nowMs: Long,
    isFa: Boolean,
    onScheduleReminder: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val riseTimeStr = sdf.format(Date(pass.startTimeMs))
    val maxTimeStr = sdf.format(Date(pass.maxTimeMs))
    val setTimeStr = sdf.format(Date(pass.endTimeMs))

    val passDurationMin = pass.passDurationSec / 60
    val startDiffMs = pass.startTimeMs - nowMs
    val endDiffMs = pass.endTimeMs - nowMs

    val isCurrentlyActive = nowMs in pass.startTimeMs..pass.endTimeMs
    val isFuture = nowMs < pass.startTimeMs

    // Progress math
    val progress: Float = when {
        isCurrentlyActive -> {
            val total = (pass.endTimeMs - pass.startTimeMs).toFloat()
            val elapsed = (nowMs - pass.startTimeMs).toFloat()
            (elapsed / total).coerceIn(0f, 1f)
        }
        isFuture -> {
            val maxWindowMs = 6 * 3600 * 1000L // 6 hour horizon
            val remaining = (pass.startTimeMs - nowMs).toFloat()
            (1f - (remaining / maxWindowMs)).coerceIn(0.05f, 1f)
        }
        else -> 1f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color(pass.classification.colorHex).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(pass.classification.colorHex))
                    )
                    Text(
                        text = if (isFa) pass.classification.labelFa else pass.classification.labelEn,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(pass.classification.colorHex)
                    )
                }

                val durationText = if (isFa) "$passDurationMin دقیقه".toPersianDigits() else "$passDurationMin min pass"
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sub-status text
            val relativeStatusText = when {
                isCurrentlyActive -> {
                    val remMins = max(1L, (endDiffMs / 60000L))
                    if (isFa) "در حال انجام! $remMins دقیقه تا پایان".toPersianDigits() else "Pass in progress! $remMins min remaining"
                }
                isFuture -> {
                    val hrs = startDiffMs / (3600 * 1000L)
                    val mins = (startDiffMs % (3600 * 1000L)) / (60 * 1000L)
                    val cdStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins} min"
                    if (isFa) "شروع تا $cdStr دیگر".toPersianDigits() else "Starts in $cdStr"
                }
                else -> {
                    if (isFa) "پایان یافته" else "Completed"
                }
            }

            Text(
                text = relativeStatusText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentPrimary
            )

            // Visual Progress Track Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(pass.classification.colorHex),
                    trackColor = Color(pass.classification.colorHex).copy(alpha = 0.2f),
                )

                // Key Timeline Markers Below Track
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isFa) "طلوع: $riseTimeStr".toPersianDigits() else "Rise: $riseTimeStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isFa) "اوج: $maxTimeStr (${String.format(Locale.US, "%.0f°", pass.maxElevationDeg)})".toPersianDigits()
                            else "Max: $maxTimeStr (${String.format(Locale.US, "%.0f°", pass.maxElevationDeg)})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isFa) "غروب: $setTimeStr".toPersianDigits() else "Set: $setTimeStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Reminder Button
            Button(
                onClick = onScheduleReminder,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_pass_alert"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isFa) "تنظیم یادآوری این گذر" else "Schedule Reminder Alert")
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
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
