package com.alijafari.red.astronomy.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.alijafari.red.astronomy.data.catalog.GeoCity
import com.alijafari.red.astronomy.data.catalog.GeoLocationCatalog
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.theme.IranSans
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectorDialog(
    uiState: MainUiState,
    onDismiss: () -> Unit,
    onSelectLocation: (GeoCity) -> Unit,
    onSelectCoordinates: (lat: Double, lon: Double, elevation: Double, nameEn: String, nameFa: String) -> Unit,
    onGpsSelected: (lat: Double, lon: Double, alt: Double?) -> Unit,
    onToggleFavorite: (GeoCity) -> Unit,
    onRemoveFavorite: (String) -> Unit
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf(uiState.locationSearchQuery) }
    var isManualExpanded by remember { mutableStateOf(false) }

    // Manual coordinates state
    var manualLat by remember { mutableStateOf("") }
    var manualLon by remember { mutableStateOf("") }
    var manualElev by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }

    // GPS locating feedback
    var isGpsFetching by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }

    val filteredCities = remember(searchQuery) {
        GeoLocationCatalog.search(searchQuery, limit = 50)
    }

    // Permission launcher for GPS
    @SuppressLint("MissingPermission")
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            isGpsFetching = true
            gpsError = null
            fetchDeviceLocation(context) { loc ->
                isGpsFetching = false
                if (loc != null) {
                    onGpsSelected(loc.latitude, loc.longitude, if (loc.hasAltitude()) loc.altitude else null)
                } else {
                    gpsError = if (isFa) "موقعیت GPS دریافت نشد. لطفاً مکان‌یاب دستگاه را روشن کنید." else "Could not acquire GPS fix. Please turn on device location."
                }
            }
        } else {
            gpsError = if (isFa) "مجوز دسترسی به موقعیت مکانی داده نشد." else "Location permission was denied."
        }
    }

    // Glowing animation for Nurabad City (NC)
    val infiniteTransition = rememberInfiniteTransition(label = "nurabadGlow")
    val cornflowerGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cornflowerGlow"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Header with Title and Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFa) "انتخاب موقعیت رصد" else "Observation Location",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = if (isFa) IranSans else null,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFa) "پایگاه داده آفلاین رصد و شهرهای جهان" else "Offline-first Astronomy Geo Database",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = if (isFa) IranSans else null
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_location_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isFa) "بستن" else "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. SEARCH BAR (Sticky at Top)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("location_search_input"),
                    placeholder = {
                        Text(
                            text = if (isFa) "جستجوی شهر، استان، کشور (فارسی / انگلیسی)..." else "Search city, province, country...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = if (isFa) IranSans else null
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = if (isFa) "پاک کردن" else "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 2. NURABAD CITY (NC) - PERMANENTLY PINNED BELOW SEARCH BAR
                    item(key = "nurabad_pinned_base") {
                        val isNurabadSelected = isLocationMatching(uiState.userLocation, GeoLocationCatalog.NURABAD_CITY)
                        val cornflowerBlue = Color(0xFF6495ED)
                        val cornflowerGlowColor = cornflowerBlue.copy(alpha = cornflowerGlowAlpha)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    ambientColor = cornflowerBlue,
                                    spotColor = cornflowerBlue
                                )
                                .border(
                                    width = if (isNurabadSelected) 2.5.dp else 1.8.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            cornflowerGlowColor,
                                            cornflowerBlue,
                                            Color(0xFF4169E1)
                                        )
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    onSelectLocation(GeoLocationCatalog.NURABAD_CITY)
                                }
                                .testTag("pinned_nurabad_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNurabadSelected)
                                    cornflowerBlue.copy(alpha = 0.22f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Pulsing badge icon
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(cornflowerBlue.copy(alpha = 0.25f), CircleShape)
                                        .border(1.5.dp, cornflowerGlowColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = cornflowerBlue,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isFa) GeoLocationCatalog.NURABAD_CITY.nameFa else GeoLocationCatalog.NURABAD_CITY.nameEn,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = if (isFa) IranSans else null,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            color = cornflowerBlue.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isFa) "پایگاه همیشگی" else "Permanent Base",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = if (isFa) IranSans else null,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = cornflowerBlue
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = if (isFa) "استان فارس، ایران • ۳۰.۱۱° N, ۵۱.۵۲° E • ۹۴۰ متر" else "Fars, Iran • 30.11° N, 51.52° E • 940m",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = if (isFa) IranSans else null
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isNurabadSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = cornflowerBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. GPS / LIVE DEVICE LOCATION
                    item(key = "gps_device_location") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    val fineGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val coarseGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (fineGranted || coarseGranted) {
                                        isGpsFetching = true
                                        gpsError = null
                                        fetchDeviceLocation(context) { loc ->
                                            isGpsFetching = false
                                            if (loc != null) {
                                                onGpsSelected(loc.latitude, loc.longitude, if (loc.hasAltitude()) loc.altitude else null)
                                            } else {
                                                gpsError = if (isFa) "موقعیت GPS دریافت نشد." else "Could not acquire GPS location."
                                            }
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                                .testTag("gps_location_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isGpsFetching || uiState.isGpsLocating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isFa) "موقعیت زنده GPS دستگاه" else "Use Live GPS Location",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = if (isFa) IranSans else null,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isFa) "تشخیص آفلاین نزدیک‌ترین شهر + سنسور ارتفاع" else "Offline nearest-city lookup + live coordinates",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = if (isFa) IranSans else null
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (gpsError != null) {
                                        Text(
                                            text = gpsError ?: "",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = if (isFa) IranSans else null
                                            ),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. FAVOURITES (SHOWN ONLY WHEN USER HAS FAVOURITES, MAX 5)
                    if (uiState.favoriteLocations.isNotEmpty()) {
                        item(key = "favorites_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isFa) "مکان‌های برگزیده (${uiState.favoriteLocations.size} از ۵)" else "Favourites (${uiState.favoriteLocations.size}/5)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = if (isFa) IranSans else null,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        items(uiState.favoriteLocations, key = { "fav_${it.id}" }) { favCity ->
                            val isSelected = isLocationMatching(uiState.userLocation, favCity)
                            CityItemCard(
                                city = favCity,
                                isSelected = isSelected,
                                isFavorite = true,
                                isFa = isFa,
                                onSelect = { onSelectLocation(favCity) },
                                onToggleFavorite = { onRemoveFavorite(favCity.id) }
                            )
                        }
                    }

                    // 5. SEARCH RESULTS / LOCAL CITY DATABASE
                    item(key = "catalog_header") {
                        val headerTitle = if (searchQuery.isNotBlank()) {
                            if (isFa) "نتایج جستجو (${filteredCities.size} مورد)" else "Search Results (${filteredCities.size} found)"
                        } else {
                            if (isFa) "همه شهرها و پایتخت‌ها" else "All Cities & World Capitals"
                        }
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = if (isFa) IranSans else null,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }

                    if (filteredCities.isEmpty()) {
                        item(key = "no_search_results") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isFa) "شهری با این نام پیدا نشد. می‌توانید مختصات را دستی وارد کنید." else "No cities match query. You can enter manual coordinates below.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = if (isFa) IranSans else null
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(filteredCities, key = { "city_${it.id}" }) { city ->
                            val isSelected = isLocationMatching(uiState.userLocation, city)
                            val isFav = uiState.favoriteLocations.any { it.id == city.id }
                            CityItemCard(
                                city = city,
                                isSelected = isSelected,
                                isFavorite = isFav,
                                isFa = isFa,
                                onSelect = { onSelectLocation(city) },
                                onToggleFavorite = { onToggleFavorite(city) }
                            )
                        }
                    }

                    // 6. MANUAL COORDINATES (EXPANDABLE CARD AT BOTTOM)
                    item(key = "manual_coordinates_section") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .testTag("manual_coordinates_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isManualExpanded = !isManualExpanded },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditLocation,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (isFa) "ورود دستی مختصات جغرافیایی" else "Enter Coordinates Manually",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = if (isFa) IranSans else null,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { isManualExpanded = !isManualExpanded },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isManualExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isManualExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = manualLat,
                                        onValueChange = { manualLat = it; manualError = null },
                                        label = { Text(if (isFa) "عرض جغرافیایی (-۹۰ تا +۹۰)" else "Latitude (-90.0 to +90.0)") },
                                        modifier = Modifier.fillMaxWidth().testTag("manual_lat_input"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = manualLon,
                                        onValueChange = { manualLon = it; manualError = null },
                                        label = { Text(if (isFa) "طول جغرافیایی (-۱۸۰ تا +۱۸۰)" else "Longitude (-180.0 to +180.0)") },
                                        modifier = Modifier.fillMaxWidth().testTag("manual_lon_input"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = manualElev,
                                            onValueChange = { manualElev = it },
                                            label = { Text(if (isFa) "ارتفاع (متر - اختیاری)" else "Elevation (m - opt)") },
                                            modifier = Modifier.weight(1f).testTag("manual_elev_input"),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = manualName,
                                            onValueChange = { manualName = it },
                                            label = { Text(if (isFa) "نام مکان (اختیاری)" else "Name (opt)") },
                                            modifier = Modifier.weight(1f).testTag("manual_name_input"),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    if (manualError != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = manualError ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = if (isFa) IranSans else null
                                            ),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val lat = manualLat.toDoubleOrNull()
                                            val lon = manualLon.toDoubleOrNull()
                                            val elev = manualElev.toDoubleOrNull() ?: 0.0

                                            if (lat == null || lat < -90.0 || lat > 90.0) {
                                                manualError = if (isFa) "عرض جغرافیایی باید عددی بین -۹۰ و +۹۰ باشد." else "Latitude must be between -90 and +90."
                                                return@Button
                                            }
                                            if (lon == null || lon < -180.0 || lon > 180.0) {
                                                manualError = if (isFa) "طول جغرافیایی باید عددی بین -۱۸۰ و +۱۸۰ باشد." else "Longitude must be between -180 and +180."
                                                return@Button
                                            }

                                            val customName = manualName.trim().ifEmpty {
                                                if (isFa) "موقعیت سفارشی (${String.format(Locale.US, "%.2f, %.2f", lat, lon)})"
                                                else "Custom Location (${String.format(Locale.US, "%.2f, %.2f", lat, lon)})"
                                            }

                                            onSelectCoordinates(lat, lon, elev, customName, customName)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("apply_manual_coordinates_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isFa) "ثبت و اعمال مختصات" else "Apply Coordinates",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = if (isFa) IranSans else null,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityItemCard(
    city: GeoCity,
    isSelected: Boolean,
    isFavorite: Boolean,
    isFa: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val cardBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(if (isSelected) 1.8.dp else 1.dp, cardBorderColor, RoundedCornerShape(14.dp))
            .clickable { onSelect() }
            .testTag("city_card_${city.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isFa) city.nameFa else city.nameEn,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = if (isFa) IranSans else null,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (city.isCapital) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isFa) "مرکز" else "Capital",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val subInfo = buildString {
                    if (city.provinceFa.isNotBlank() && isFa) {
                        append(city.provinceFa)
                        append("، ")
                    } else if (city.provinceEn.isNotBlank() && !isFa) {
                        append(city.provinceEn)
                        append(", ")
                    }
                    append(if (isFa) city.countryFa else city.countryEn)
                    append(" • ")
                    append(String.format(Locale.US, "%.2f°N, %.2f°E", city.latitude, city.longitude))
                    if (city.elevationMeters > 0) {
                        append(" • ")
                        append(city.elevationMeters.toInt())
                        append(if (isFa) "متر" else "m")
                    }
                }

                Text(
                    text = subInfo,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = if (isFa) IranSans else null,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp).testTag("fav_btn_${city.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFa) "نشان‌گذاری" else "Favorite",
                        tint = if (isFavorite) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(context: Context, onLocationResult: (Location?) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onLocationResult(null)
            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val lastGps = if (isGpsEnabled) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
        val lastNetwork = if (isNetworkEnabled) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
        val lastPassive = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

        val bestLastKnown = listOfNotNull(lastGps, lastNetwork, lastPassive).maxByOrNull { it.time }
        if (bestLastKnown != null) {
            onLocationResult(bestLastKnown)
            return
        }

        // If no last known location, fallback
        onLocationResult(null)
    } catch (e: Exception) {
        onLocationResult(null)
    }
}

private fun isLocationMatching(userLoc: UserLocation, city: GeoCity): Boolean {
    val dist = GeoLocationCatalog.distanceKm(userLoc.latitude, userLoc.longitude, city.latitude, city.longitude)
    return dist < 1.0 || (userLoc.cityNameEn.equals(city.nameEn, ignoreCase = true) && dist < 25.0)
}
