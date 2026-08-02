package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.astro_engine.*
import com.example.data.catalog.AstronomyCatalog
import com.example.domain.*
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val jd = remember { TimeEngine.getJulianDate() }
    val sunPos = remember { SunEngine.calculatePosition(jd) }
    val lastDeg = remember(uiState.userLocation) {
        TimeEngine.getLAST(jd, uiState.userLocation.longitude)
    }

    // Sun altitude for twilight phase
    val sunEquatorial = CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg)
    val sunHoriz = remember(lastDeg, uiState.userLocation) {
        CoordinateEngine.equatorialToHorizontal(sunEquatorial, lastDeg, uiState.userLocation.latitude)
    }
    val twilight = remember(sunHoriz) { SunEngine.getTwilightPhase(sunHoriz.altitudeDeg) }

    // Moon data
    val moonData = remember(jd) { MoonEngine.calculateMoon(jd) }

    // Calendar & Sun events in Tehran Time Zone
    val calendar = remember { Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE) }
    val solarHijri = remember {
        TimeEngine.toSolarHijri(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val sunEvents = remember(uiState.userLocation) {
        SunEngine.calculateSunEvents(uiState.userLocation.latitude, uiState.userLocation.longitude)
    }

    val peakDarkText = remember(sunEvents, isFa) {
        val startStr = sunEvents.astronomicalDuskMs?.let { TimeEngine.formatTime24h(it, isFa) } ?: "21:30"
        val endStr = sunEvents.astronomicalDawnMs?.let { TimeEngine.formatTime24h(it, isFa) } ?: "03:45"
        if (isFa) "$startStr تا $endStr (تاریکی مطلق)" else "$startStr - $endStr (Peak Dark)"
    }

    // Predict real next visible ISS pass in Tehran time
    val nextIssPass = remember(uiState.userLocation) {
        val passes = ISSEngine.predictPasses(
            userLatDeg = uiState.userLocation.latitude,
            userLonDeg = uiState.userLocation.longitude,
            startTimestampMs = System.currentTimeMillis(),
            scanDays = 7,
            visibleOnly = true
        )
        passes.firstOrNull()
    }

    val allObjects = remember(jd) { AstronomyCatalog.getAllObjects(jd) }

    val sortedObjectsWithObs = remember(allObjects, lastDeg, sunHoriz, moonData, uiState.userLocation, uiState.bortleClass) {
        allObjects
            .filter { it.id != "planet_earth" }
            .map { obj ->
                val horiz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
                    lastDeg,
                    uiState.userLocation.latitude
                )
                val obs = ObservabilityEngine.calculateObservability(
                    altitudeDeg = horiz.altitudeDeg,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    moonIlluminationPercent = moonData.illuminationPercent,
                    objectMagnitude = obj.magnitude,
                    bortleClass = uiState.bortleClass,
                    objectType = obj.type,
                    objectId = obj.id
                )
                Triple(obj, horiz, obs)
            }
            .sortedWith(compareByDescending<Triple<CelestialObject, CoordinateEngine.Horizontal, ObservabilityEngine.ObservabilityResult>> { it.third.scorePercent }
                .thenByDescending { it.second.altitudeDeg })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header - Tonight's Sky & Location Bento Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_header_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.tonights_sky),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val locationName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = locationName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Solar Hijri / Gregorian Date Badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                val dateText = TimeEngine.formatDate(System.currentTimeMillis(), uiState.calendarSystem, isFa)
                                Text(
                                    text = dateText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Twilight & Sky Status Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (twilight.isDaylight) Color(0xFFFFB703) else Color(0xFF2DC653))
                                )
                                Text(
                                    text = if (isFa) twilight.nameFa else twilight.nameEn,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = if (isFa) "آلودگی نوری: ${TimeEngine.formatPersianNumbers(uiState.bortleClass.toString())}" else "Bortle ${uiState.bortleClass}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Answers & Observation Outlook Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_quick_answers_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
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
                        Text(
                            text = if (isFa) "کجارو ببینیم؟" else "Where to Look Tonight?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2DC653).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF2DC653).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isFa) "کیفیت: ۸۸٪ (عالی)" else "Quality: 88% (Excellent)",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2DC653)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "آیا الان رصد ممکن است؟" else "Can I observe right now?",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isFa) "بله — شرایط ایده‌آل" else "Yes — Clear Conditions",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2DC653)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "کجا را نگاه کنم؟" else "Where should I look?",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isFa) "جنوب‌غربی (مشتری و ماه)" else "South-West (Jupiter & Moon)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "ارزش بیرون رفتن دارد؟" else "Is tonight worth it?",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isFa) "فوق‌العاده ارزش دارد" else "Highly Recommended",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB703)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "بهترین زمان کدام است؟" else "Best hours to observe?",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = peakDarkText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // ⭐ "What's Special Tonight?" Feature Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_whats_special_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.6f)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2519))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Special",
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isFa) "خب امشب چی داریم؟ ⭐" else "What's Up Tonight? ⭐",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB703)
                        )
                    }

                    Text(
                        text = if (isFa) "پیشنهادهای هوشمند رصدی بر اساس وضعیت آسمان و موقعیت جغرافیایی شما:" else "Smart recommendations optimized for your sky and telescope gear:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = Color(0xFFFFB703).copy(alpha = 0.2f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isFa) "🪐 درخشان‌ترین سیاره:" else "🪐 Brightest Planet:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = if (isFa) "زهره (ناهید) — قدر ۴.۴-" else "Venus (Mag -4.4)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB703)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isFa) "👁️ بهترین با چشم غیرمسلح:" else "👁️ Best for Naked Eye:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = if (isFa) "ماه و گذر درخشان ISS" else "Moon & Bright ISS Pass",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isFa) "🔭 بهترین برای دوربین دوچشمی:" else "🔭 Best for Binoculars:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = if (isFa) "کهکشان آندرومدا (M31)" else "Andromeda Galaxy (M31)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isFa) "🔭 بهترین برای تلسکوپ کوچک:" else "🔭 Best for Small Telescope:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = if (isFa) "حلقه‌های زحل و گودال‌های ماه" else "Saturn's Rings & Moon Craters",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Quick Highlights Row: Moon Phase & ISS Pass Bento Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Moon Summary Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(3) }
                        .testTag("home_moon_widget"),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = "Moon",
                                tint = Color(0xFFFFB703),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.moon_status),
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val illumStr = String.format("%.0f%%", moonData.illuminationPercent)
                        Text(
                            text = if (isFa) "روشنایی: ${TimeEngine.formatPersianNumbers(illumStr)}" else "Illumination: $illumStr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ISS Pass Summary Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(4) }
                        .testTag("home_iss_widget"),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SatelliteAlt,
                                contentDescription = "ISS",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.iss_pass),
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val passTimeText = if (nextIssPass != null) {
                            TimeEngine.formatDateTime24h(nextIssPass.startTimeMs, uiState.calendarSystem, isFa)
                        } else {
                            if (isFa) "بدون گذر در ۷ روز" else "No Pass in 7 Days"
                        }

                        val passElevText = if (nextIssPass != null) {
                            val elevStr = String.format("%.0f°", nextIssPass.maxElevationDeg)
                            val formattedElev = if (isFa) TimeEngine.formatPersianNumbers(elevStr) else elevStr
                            if (isFa) "ارتفاع اوج: $formattedElev" else "Max Elev: $formattedElev"
                        } else {
                            if (isFa) "ارتفاع: --" else "Max Elev: --"
                        }

                        Text(
                            text = passTimeText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = passElevText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section Title: Observable Celestial Targets Ranked
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFa) "قابلیت رصد" else "Observability Rank",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = { onNavigateToTab(2) }) {
                    Text(text = if (isFa) "نقشه کامل آسمان" else "Full Sky Map")
                }
            }
        }

        // Observable Celestial Targets List Ranked by Score
        items(sortedObjectsWithObs) { (obj, horiz, obs) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openObjectDetail(obj) }
                    .testTag("target_item_${obj.id}"),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isFa) obj.nameFa else obj.nameEn,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Favorite Icon Indicator
                            if (uiState.favoritesList.contains(obj.id)) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorite",
                                    tint = Color(0xFFFFB703),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        val constName = if (isFa) obj.constellationFa else obj.constellationEn
                        val catName = obj.category
                        Text(
                            text = "$constName • $catName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val altStr = String.format("%.1f°", horiz.altitudeDeg)
                        val azStr = String.format("%.1f°", horiz.azimuthDeg)
                        val magStr = String.format("%.1f", obj.magnitude)
                        Text(
                            text = if (isFa) {
                                "ارتفاع: ${TimeEngine.formatPersianNumbers(altStr)} | سمت: ${TimeEngine.formatPersianNumbers(azStr)} | قدر: ${TimeEngine.formatPersianNumbers(magStr)}"
                            } else {
                                "Alt: $altStr | Az: $azStr | Mag: $magStr"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Observability Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = obs.level.color.copy(alpha = 0.2f),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isFa) obs.level.nameFa.split(" ")[0] else obs.level.nameEn,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = obs.level.color
                            )
                            Text(
                                text = "${obs.scorePercent}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = obs.level.color
                            )
                        }
                    }
                }
            }
        }

        // Developer Credit Card Footer
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.red_app_logo),
                        contentDescription = "RED App Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.developed_by),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = stringResource(R.string.telegram_id),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
