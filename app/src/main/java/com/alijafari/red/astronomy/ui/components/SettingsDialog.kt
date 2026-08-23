package com.alijafari.red.astronomy.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isFa = uiState.language == AppLanguage.PERSIAN
    var showCityPicker by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(context, if (isFa) "لطفاً خدمات مکان‌یابی (GPS) دستگاه را روشن کنید" else "Please enable device Location Services (GPS)", Toast.LENGTH_LONG).show()
                try {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    val lastLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (lastLoc != null) {
                        viewModel.setLocation("Live GPS", "GPS زنده", lastLoc.latitude, lastLoc.longitude)
                        Toast.makeText(context, if (isFa) "موقعیت زنده GPS ثبت شد" else "Live GPS location set", Toast.LENGTH_SHORT).show()
                        showCityPicker = false
                    } else {
                        viewModel.setLocation("Live GPS", "GPS زنده", uiState.userLocation.latitude, uiState.userLocation.longitude)
                        showCityPicker = false
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_dialog"),
        title = {
            Text(
                text = stringResource(R.string.app_settings),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Language Selection Segmented Switch
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.application_language),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.language == AppLanguage.PERSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) },
                            label = { Text(text = stringResource(R.string.language_persian)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text(text = stringResource(R.string.language_english)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Calendar System Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.calendar_system),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.calendarSystem == CalendarSystem.SOLAR_HIJRI,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI) },
                            label = { Text(text = stringResource(R.string.calendar_solar_hijri)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.calendarSystem == CalendarSystem.GREGORIAN,
                            onClick = { viewModel.setCalendarSystem(CalendarSystem.GREGORIAN) },
                            label = { Text(text = stringResource(R.string.calendar_gregorian)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Theme Mode Selection (OLED Black, Deep Navy, Light)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isFa) "پوسته برنامه (App Theme):" else "App Theme Mode:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.DARK_NAVY,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK_NAVY) },
                                label = { Text(text = if (isFa) "🌌 سرمه‌ای" else "🌌 Deep Navy") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.OLED_BLACK,
                                onClick = { viewModel.setThemeMode(ThemeMode.OLED_BLACK) },
                                label = { Text(text = if (isFa) "🖤 OLED" else "🖤 Pitch OLED") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.DYNAMIC_SKY,
                                onClick = { viewModel.setThemeMode(ThemeMode.DYNAMIC_SKY) },
                                label = { Text(text = if (isFa) "🌅 آسمان پویا" else "🌅 Dynamic Sky") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                label = { Text(text = if (isFa) "☀️ روشن" else "☀️ Light") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Sky Canvas Theme Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isFa) "سبک هنری آسمان (RMAE Theme):" else "Sky Canvas Theme:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.ATMOSPHERIC_SKY,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.ATMOSPHERIC_SKY) },
                                label = { Text(text = if (isFa) "🌅 آسمان جوی" else "Atmospheric") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.MONOCHROME_SCIENTIFIC,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.MONOCHROME_SCIENTIFIC) },
                                label = { Text(text = if (isFa) "🔳 تک‌رنگ" else "Monochrome") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.KIDS_WATERCOLOR,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.KIDS_WATERCOLOR) },
                                label = { Text(text = if (isFa) "🎨 آبرنگ" else "WaterColor") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.OBSERVATORY,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.OBSERVATORY) },
                                label = { Text(text = if (isFa) "🔴 رصدخانه" else "Observatory") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = uiState.skyCanvasTheme == SkyCanvasTheme.PAPERCRAFT_DIORAMA,
                                onClick = { viewModel.setSkyCanvasTheme(SkyCanvasTheme.PAPERCRAFT_DIORAMA) },
                                label = { Text(text = if (isFa) "✂️ دیوراما کاغذ برجسته" else "✂️ Papercraft Diorama") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Location / City Selector Button
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.observation_location),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedButton(
                        onClick = { showCityPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val cityName = if (isFa) uiState.userLocation.cityNameFa else uiState.userLocation.cityNameEn
                        Text(text = cityName)
                    }
                }

                // Light Pollution / Bortle Class Slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val bortleText = stringResource(R.string.bortle_class_setting, uiState.bortleClass)
                    Text(
                        text = bortleText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Slider(
                        value = uiState.bortleClass.toFloat(),
                        onValueChange = { viewModel.setBortleClass(it.toInt()) },
                        valueRange = 1f..9f,
                        steps = 7
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Luxury Glassmorphic Developer Credit Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0x25FFFFFF),
                                    Color(0x1200F0FF),
                                    Color(0x1FA855F7)
                                )
                            )
                        )
                        .border(
                            border = BorderStroke(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x90FFFFFF),
                                        Color(0x4038BDF8),
                                        Color(0x80A855F7),
                                        Color(0x60FFFFFF)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SafeAppLogo(
                                    modifier = Modifier.size(38.dp),
                                    cornerRadius = 10.dp
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isFa) "توسعه‌دهنده: " else "Developed by ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "علی جعفری",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            Text(
                                text = "Ali Jafari • RED Astronomy Engine",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Telegram Glass Badge
                            val intentContext = context
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0088CC).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF0088CC).copy(alpha = 0.6f)),
                                modifier = Modifier.clickable {
                                    try {
                                        val tgIntent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://t.me/EdisonWasAThief")
                                        )
                                        intentContext.startActivity(tgIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "✈️",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "@EdisonWasAThief",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.3.sp
                                        ),
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.ok))
            }
        }
    )

    // Iran Provincial Capitals & Live GPS Picker Modal
    if (showCityPicker) {
        AlertDialog(
            onDismissRequest = { showCityPicker = false },
            title = {
                Text(text = stringResource(R.string.select_city))
            },
            text = {
                Box(modifier = Modifier.height(320.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item {
                            Button(
                                onClick = {
                                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (!fine && !coarse) {
                                        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    } else {
                                        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                                        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
                                        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
                                        if (!isGpsEnabled && !isNetworkEnabled) {
                                            Toast.makeText(context, if (isFa) "لطفاً خدمات مکان‌یابی (GPS) دستگاه را روشن کنید" else "Please enable device Location Services (GPS)", Toast.LENGTH_LONG).show()
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        } else {
                                            try {
                                                val lastLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                                if (lastLoc != null) {
                                                    viewModel.setLocation("Live GPS", "GPS زنده", lastLoc.latitude, lastLoc.longitude)
                                                    Toast.makeText(context, if (isFa) "موقعیت زنده GPS ثبت شد" else "Live GPS location set", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.setLocation("Live GPS", "GPS زنده", uiState.userLocation.latitude, uiState.userLocation.longitude)
                                                }
                                                showCityPicker = false
                                            } catch (e: SecurityException) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (isFa) "موقعیت مکانی زنده (GPS)" else "Use Live GPS Location")
                            }
                        }
                        items(AstronomyCatalog.IRAN_CITIES) { (en, fa, coords) ->
                            TextButton(
                                onClick = {
                                    viewModel.setLocation(en, fa, coords.first, coords.second)
                                    showCityPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = if (isFa) fa else en)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityPicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}
