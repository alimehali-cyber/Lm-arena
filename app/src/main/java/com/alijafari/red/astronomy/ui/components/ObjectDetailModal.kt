package com.alijafari.red.astronomy.ui.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarBorder
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
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.data.catalog.PhysicalData
import com.alijafari.red.astronomy.domain.*
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import com.alijafari.red.astronomy.ui.*
import com.alijafari.red.astronomy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailModal(
    obj: CelestialObject,
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isFa = uiState.language == AppLanguage.PERSIAN

    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingNotificationAction?.invoke()
            pendingNotificationAction = null
        } else {
            pendingNotificationAction = null
            Toast.makeText(
                context,
                if (isFa) "برای دریافت هشدار رصد، لطفاً مجوز اعلان را فعال کنید."
                else "Notification permission is required to receive observation alerts.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val executeWithNotificationPermission: (() -> Unit) -> Unit = { action ->
        if (com.alijafari.red.astronomy.notification.NotificationPermissionHelper.hasPostNotificationPermission(context)) {
            action()
        } else {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Obtain single canonical identity from CanonicalAstroCatalog
    val canonicalObj = remember(obj) {
        CanonicalAstroCatalog.getCanonicalObject(obj.id)
            ?: CanonicalAstroCatalog.getCanonicalObject(CanonicalAstroCatalog.resolveCanonicalId(obj.id))
    }

    val timestampMs = remember(uiState.timeMachineState) {
        if (uiState.timeMachineState.mode == TimeMachineMode.SIMULATION) {
            uiState.timeMachineState.simulationTimeMs
        } else {
            System.currentTimeMillis()
        }
    }

    // Obtain dynamic astronomical state from AstroDispatchEngine
    val calculatedState = remember(canonicalObj, obj.id, uiState.userLocation, timestampMs) {
        AstroDispatchEngine.calculateState(
            idOrAlias = canonicalObj?.canonicalId ?: obj.id,
            timestampMs = timestampMs,
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude
        )
    }

    val jd = remember(timestampMs) { TimeEngine.getJulianDate(timestampMs) }

    val horizAlt = calculatedState?.altitudeDeg ?: 0.0
    val horizAz = calculatedState?.azimuthDeg ?: 0.0
    val dynamicRa = calculatedState?.raDeg ?: obj.raDeg
    val dynamicDec = calculatedState?.decDeg ?: obj.decDeg
    val dynamicMag = calculatedState?.magnitude ?: obj.magnitude

    val sunPos = remember(timestampMs) { SunEngine.calculatePosition(jd) }
    val lastDeg = remember(uiState.userLocation, jd) {
        TimeEngine.getLAST(jd, uiState.userLocation.longitude)
    }
    val sunHoriz = remember(lastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            lastDeg,
            uiState.userLocation.latitude
        )
    }
    val celestialObj = remember(canonicalObj, obj, dynamicRa, dynamicDec, dynamicMag) {
        if (canonicalObj != null) {
            CanonicalAstroCatalog.toCelestialObject(
                canonicalObj = canonicalObj,
                dynamicRa = dynamicRa,
                dynamicDec = dynamicDec,
                dynamicMag = dynamicMag
            )
        } else {
            obj
        }
    }

    val moonData = remember(jd, uiState.userLocation) {
        MoonEngine.calculateMoon(jd, uiState.userLocation.latitude, uiState.userLocation.longitude)
    }
    val obs = remember(horizAlt, sunHoriz, moonData, uiState.bortleClass, dynamicMag) {
        ObservabilityEngine.calculateObservability(
            altitudeDeg = horizAlt,
            sunAltitudeDeg = sunHoriz.altitudeDeg,
            moonIlluminationPercent = moonData.illuminationPercent,
            objectMagnitude = dynamicMag,
            bortleClass = uiState.bortleClass,
            objectType = celestialObj.type,
            objectId = celestialObj.id
        )
    }

    val physicalProps = remember(celestialObj) {
        PhysicalData.getPhysicalProperties(celestialObj)
    }

    val coolFacts = remember(celestialObj, isFa) {
        if (isFa) PhysicalData.getCoolFactsFa(celestialObj) else PhysicalData.getCoolFactsEn(celestialObj)
    }

    val riseSetTransit = remember(dynamicRa, dynamicDec, uiState.userLocation, jd, isFa) {
        CoordinateEngine.calculateRiseSetTransit(
            raDeg = dynamicRa,
            decDeg = dynamicDec,
            latDeg = uiState.userLocation.latitude,
            lonDeg = uiState.userLocation.longitude,
            jd = jd,
            isFa = isFa
        )
    }

    var showAddLogDialog by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    var showScientificCoords by remember { mutableStateOf(false) }

    var notesInput by remember { mutableStateOf("") }
    var ratingInput by remember { mutableStateOf(5) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("object_detail_modal")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Name & Favorite Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFa) obj.nameFa else obj.nameEn,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val constName = if (isFa) obj.constellationFa else obj.constellationEn
                        Text(
                            text = "$constName • ${obj.category}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showNotificationSheet = true },
                            modifier = Modifier.testTag("modal_notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Set Observation Notification",
                                tint = AccentPrimary,
                                modifier = Modifier.size(26.dp)
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
                }
            }

            // Observability Badge Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = obs.level.color.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, obs.level.color.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isFa) "امتیاز رصدپذیری لحظه‌ای" else "Live Observability Score",
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${obs.scorePercent}%",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = obs.level.color
                        )
                    }
                }
            }

            // Locate Target in Live AR Sky Button
            item {
                Button(
                    onClick = {
                        viewModel.locateObjectInAR(obj)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("locate_in_ar_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Locate in AR",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isFa) "ردیابی این جرم در آسمان‌نما (AR)" else "Locate Object in Live Sky (AR)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // MATHEMATICALLY DERIVED PHYSICAL PROPERTIES CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🌐", fontSize = 18.sp)
                            Text(
                                text = if (isFa) "مشخصات فیزیکی و محاسباتی مراجع" else "Derived Physical Properties",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PropertyRow(
                                label = if (isFa) "📏 ابعاد و قطر" else "📏 Size / Diameter",
                                value = if (isFa) physicalProps.diameterDisplayFa else physicalProps.diameterDisplayEn
                            )
                            PropertyRow(
                                label = if (isFa) "⚖️ جرم تقریبی" else "⚖️ Approximate Mass",
                                value = if (isFa) physicalProps.massKgDisplayFa else physicalProps.massKgDisplayEn
                            )
                            PropertyRow(
                                label = if (isFa) "🌍 گرانش سطحی" else "🌍 Surface Gravity",
                                value = if (isFa) physicalProps.gravityMssDisplayFa else physicalProps.gravityMssDisplayEn
                            )
                            PropertyRow(
                                label = if (isFa) "📡 فاصله از زمین" else "📡 Distance from Earth",
                                value = if (isFa) physicalProps.distanceDisplayFa else physicalProps.distanceDisplayEn
                            )
                        }
                    }
                }
            }

            // Rise, Transit, Set Schedule Card
            item {
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
                                text = if (isFa) "زمان‌بندی دقیق طلوع، ترانزیت و غروب" else "Precise Schedule Today",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
            }

            // 5 VERIFIED COOL FACTS IN FARSI
            item {
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
                            Text(text = "💡", fontSize = 18.sp)
                            Text(
                                text = if (isFa) "۵ حقیقت شگفت‌انگیز و علمی" else "5 Verified Facts & Stories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentPrimary
                            )
                        }

                        coolFacts.forEachIndexed { index, fact ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentPrimary
                                )
                                Text(
                                    text = fact,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Description
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isFa) "توضیحات تکمیلی" else "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentPrimary
                    )
                    Text(
                        text = if (isFa) obj.descriptionFa else obj.descriptionEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Scientific Coordinates (Collapsible Section - Hidden by default)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showScientificCoords = !showScientificCoords },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "مختصات علمی نجومی (RA / Dec)" else "Astronomical Coordinates",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (showScientificCoords) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Coords",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (showScientificCoords) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "RA (بعد)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(text = CoordinateEngine.formatRA(obj.raDeg), style = MaterialTheme.typography.bodyMedium)
                                }
                                Column {
                                    Text(text = "Dec (میل)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(text = CoordinateEngine.formatDec(obj.decDeg), style = MaterialTheme.typography.bodyMedium)
                                }
                                Column {
                                    Text(text = "Magnitude (قدر)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(text = String.format("%.1f", obj.magnitude), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Observation Log Button & Spacing
            item {
                OutlinedButton(
                    onClick = { showAddLogDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_log_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isFa) "ثبت در دفترچه رصد" else "Add to Observation Log")
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Observation Alert Modal Sheet
    if (showNotificationSheet) {
        AlertDialog(
            onDismissRequest = { showNotificationSheet = false },
            title = {
                Text(text = if (isFa) "🔔 تنظیم هشدار رصد ${obj.nameFa}" else "🔔 Set Observation Alert for ${obj.nameEn}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isFa) "زمان‌بندی ترانزیت (اوج ارتفاع): ${riseSetTransit.transitTimeStr}"
                        else "Transit Time: ${riseSetTransit.transitTimeStr}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            showNotificationSheet = false
                            executeWithNotificationPermission {
                                AstroNotificationManager.scheduleObjectNotification(
                                    context = context,
                                    obj = obj,
                                    targetTimeMs = System.currentTimeMillis() + 3600000L,
                                    eventTypeFa = "اوج ارتفاع (ترانزیت)",
                                    timeStr = riseSetTransit.transitTimeStr,
                                    leadTenMinutesBefore = true
                                )
                                Toast.makeText(
                                    context,
                                    if (isFa) "هشدار رصد با موفقیت تنظیم شد!" else "Observation alert scheduled!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (isFa) "تأیید و تنظیم هشدار (۱۰ دقیقه قبل)" else "Confirm Alert (10 min prior)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNotificationSheet = false }) {
                    Text(text = if (isFa) "انصراف" else "Cancel")
                }
            }
        )
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
                        placeholder = { Text(text = if (isFa) "شرایط جوی، تجهیزات رصدی..." else "Weather, telescope...") }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = if (isFa) "امتیاز رصد:" else "Rating:")
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

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
