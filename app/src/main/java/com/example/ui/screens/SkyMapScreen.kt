package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astro_engine.*
import com.example.data.catalog.AstronomyCatalog
import com.example.domain.*
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkyMapScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val isFa = uiState.language == AppLanguage.PERSIAN

    // Sky Map Layer Toggles
    var showConstellations by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var showStars by remember { mutableStateOf(true) }
    var showDeepSky by remember { mutableStateOf(true) }
    var showPlanets by remember { mutableStateOf(true) }

    // Interactive Zoom and Offset state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val jd = remember { TimeEngine.getJulianDate() }
    val lastDeg = remember(uiState.userLocation) {
        TimeEngine.getLAST(jd, uiState.userLocation.longitude)
    }

    val allObjects = remember { AstronomyCatalog.getAllObjects() }

    // Filtered search objects
    val filteredObjects = remember(uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            allObjects
        } else {
            allObjects.filter {
                it.nameEn.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.nameFa.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.constellationEn.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.constellationFa.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .testTag("skymap_screen")
    ) {
        // Search & Layer Controls Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1C1B1F)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("skymap_search_field"),
                    placeholder = {
                        Text(
                            text = if (isFa) "جستجوی ستاره، کهکشان، یا سیاره..." else "Search stars, galaxies, planets...",
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2B2930),
                        unfocusedContainerColor = Color(0xFF2B2930),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Layer Toggle Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = showConstellations,
                            onClick = { showConstellations = !showConstellations },
                            label = { Text(text = if (isFa) "صورت‌های فلکی" else "Constellations", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = showGrid,
                            onClick = { showGrid = !showGrid },
                            label = { Text(text = if (isFa) "مختصات (شبکه)" else "Grid", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Grid4x4, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = showStars,
                            onClick = { showStars = !showStars },
                            label = { Text(text = if (isFa) "ستارگان" else "Stars", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = showDeepSky,
                            onClick = { showDeepSky = !showDeepSky },
                            label = { Text(text = if (isFa) "اعماق فضا" else "Deep Sky", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = showPlanets,
                            onClick = { showPlanets = !showPlanets },
                            label = { Text(text = if (isFa) "سیارات" else "Planets", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        // 2D Interactive Celestial Map
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5.0f)
                        offset += pan
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val mapRadius = min(size.width, size.height) * 0.42f * scale
                val center = Offset(size.width / 2f + offset.x, size.height / 2f + offset.y)

                // Horizon Circle
                drawCircle(
                    color = Color(0xFF252536),
                    radius = mapRadius,
                    center = center,
                    style = Stroke(width = 3f)
                )

                // Coordinate Grid Circles (Altitude 30°, 60°)
                if (showGrid) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = mapRadius * 0.66f,
                        center = center,
                        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    )
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = mapRadius * 0.33f,
                        center = center,
                        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    )
                }

                // Render Filtered Celestial Objects
                for (obj in filteredObjects) {
                    if (obj.type == ObjectType.STAR && !showStars) continue
                    if (obj.type == ObjectType.PLANET && !showPlanets) continue
                    if (obj.type == ObjectType.DEEP_SKY && !showDeepSky) continue

                    val horiz = CoordinateEngine.equatorialToHorizontal(
                        CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
                        lastDeg,
                        uiState.userLocation.latitude
                    )

                    // Stereographic Projection to 2D Planisphere
                    if (horiz.altitudeDeg >= 0) {
                        val r = mapRadius * (1.0 - (horiz.altitudeDeg / 90.0)).toFloat()
                        val azRad = Math.toRadians(horiz.azimuthDeg)
                        val px = center.x + r * sin(azRad).toFloat()
                        val py = center.y - r * cos(azRad).toFloat()

                        val objColor = when (obj.type) {
                            ObjectType.STAR -> Color(0xFFF8F9FA)
                            ObjectType.PLANET -> Color(0xFFFFB703)
                            ObjectType.DEEP_SKY -> Color(0xFFE63946)
                            else -> Color(0xFF2DC653)
                        }

                        val nodeRadius = (7.0 - obj.magnitude).coerceIn(2.0, 10.0).toFloat()

                        drawCircle(
                            color = objColor,
                            radius = nodeRadius,
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // Quick Legend / Map Controls Overlay
            FloatingActionButton(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset View")
            }
        }
    }
}
