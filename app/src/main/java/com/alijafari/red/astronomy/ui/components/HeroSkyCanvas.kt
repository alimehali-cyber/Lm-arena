package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.domain.TimeMachineMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.rendering.*
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.util.toPersianDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

data class StardustParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    val color: Color
)

data class SelectedCelestialInfo(
    val id: String = "",
    val name: String,
    val position: Offset,
    val typeName: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroSkyCanvas(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val coroutineScope = rememberCoroutineScope()

    // Dynamic Astronomical Julian Date (Tracks real system time continuously in live mode, plus simulation offset)
    var currentSystemTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Direct Finger Time Travel state
    val simulatedOffsetHoursAnim = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Stardust Particles list
    val stardustParticles = remember { mutableStateListOf<StardustParticle>() }

    // Selected Tapped Celestial Object
    var selectedCelestial by remember { mutableStateOf<SelectedCelestialInfo?>(null) }

    // Active particle & interactive animation loop (only active when interacting, returning, or selection active)
    var frameTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasParticles by remember { derivedStateOf { stardustParticles.isNotEmpty() } }
    val isAnimatingReturn = simulatedOffsetHoursAnim.isRunning
    val hasSelection = selectedCelestial != null

    // Steady state real-time clock update (every 1 second instead of every frame when idle)
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentSystemTimeMs = now
            if (!isDragging && stardustParticles.isEmpty() && !simulatedOffsetHoursAnim.isRunning && selectedCelestial == null) {
                frameTimeMs = now
            }
            delay(1000L)
        }
    }

    LaunchedEffect(isDragging, hasParticles, isAnimatingReturn, hasSelection) {
        while (isDragging || stardustParticles.isNotEmpty() || simulatedOffsetHoursAnim.isRunning || selectedCelestial != null) {
            withFrameMillis { ms ->
                frameTimeMs = ms
                // Update stardust particles
                if (stardustParticles.isNotEmpty()) {
                    val iter = stardustParticles.iterator()
                    while (iter.hasNext()) {
                        val p = iter.next()
                        p.x += p.vx
                        p.y += p.vy
                        p.alpha -= 0.025f
                        if (p.alpha <= 0f) {
                            iter.remove()
                        }
                    }
                }
            }
        }
    }

    // Current Simulated Julian Date (Honours TimeMachineState in simulation mode, plus any finger time travel offset)
    val currentOffsetHours = simulatedOffsetHoursAnim.value
    val baseTimeMs = if (uiState.timeMachineState.mode == TimeMachineMode.SIMULATION) {
        uiState.timeMachineState.simulationTimeMs
    } else {
        currentSystemTimeMs
    }
    val currentBaseJd = TimeEngine.getJulianDate(baseTimeMs)
    val simulatedJd = currentBaseJd + (currentOffsetHours / 24.0)
    val simulatedTimeMs = TimeEngine.getTimestampFromJulianDate(simulatedJd)

    // Astro computations
    val userLat = uiState.userLocation.latitude
    val userLon = uiState.userLocation.longitude
    val userElev = uiState.userLocation.elevationMeters
    val userTimeZone = remember(uiState.userLocation.timezoneId) {
        TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId)
    }
    val lastDeg = remember(simulatedJd, userLon) { TimeEngine.getLAST(simulatedJd, userLon) }

    // Sun position
    val sunPos = remember(simulatedJd) { SunEngine.calculatePosition(simulatedJd) }
    val sunHoriz = remember(sunPos, lastDeg, userLat, userElev) {
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            lastDeg,
            userLat,
            userElev
        )
    }

    // Moon calculation (skip expensive rise/set root-finding on canvas frames)
    val moonData = remember(simulatedJd, userLat, userLon, userElev) {
        MoonEngine.calculateMoon(simulatedJd, userLat, userLon, userElev, computeRiseSet = false)
    }

    // Planets calculation
    val planetPositions = remember(simulatedJd, lastDeg, userLat, userElev) {
        PlanetEngine.PlanetType.values().mapNotNull { pType ->
            if (pType == PlanetEngine.PlanetType.PLUTO) null
            else {
                val pPos = PlanetEngine.calculatePlanet(pType, simulatedJd)
                val horiz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(pPos.raDeg, pPos.decDeg),
                    lastDeg,
                    userLat,
                    userElev
                )
                if (horiz.altitudeDeg > -2.0) Triple(pType, pPos, horiz) else null
            }
        }
    }

    // Galactic plane points (reuses precomputed LAST & J2000 galactic equator coordinates)
    val galacticPlanePoints = remember(lastDeg, userLat, userElev) {
        GalacticEngine.calculateGalacticPlanePointsWithLast(lastDeg, userLat, userElev)
            .filter { it.altitudeDeg > -5.0 }
    }

    // Catalog Stars only (Deep-sky catalog objects intentionally excluded to keep the canvas uncrowded)
    val staticCatalogStars = remember {
        AstronomyCatalog.getStars().filter { it.magnitude <= 4.5 }
    }
    val catalogStars = remember(staticCatalogStars, lastDeg, userLat, userElev) {
        staticCatalogStars.mapNotNull { celestialObj ->
            val horiz = CoordinateEngine.equatorialToHorizontal(
                CoordinateEngine.Equatorial(celestialObj.raDeg, celestialObj.decDeg),
                lastDeg,
                userLat,
                userElev
            )
            if (horiz.altitudeDeg > 0.0) Pair(celestialObj, horiz) else null
        }
    }

    // Rigorous Eclipse detection
    val isSolarEclipse = remember(sunHoriz, moonData) {
        val dAzRad = Math.toRadians(HeroSkyProjection.azimuthDistanceDeg(sunHoriz.azimuthDeg, moonData.azimuthDeg))
        val sunAltRad = Math.toRadians(sunHoriz.altitudeDeg)
        val moonAltRad = Math.toRadians(moonData.altitudeDeg)
        val cosSep = (sin(sunAltRad) * sin(moonAltRad) + cos(sunAltRad) * cos(moonAltRad) * cos(dAzRad)).coerceIn(-1.0, 1.0)
        val angDistDeg = Math.toDegrees(acos(cosSep))
        val moonSemiDiamDeg = (moonData.angularDiameterArcmin / 120.0).coerceIn(0.24, 0.29)
        val solarContactLimitDeg = 0.267 + moonSemiDiamDeg
        sunHoriz.altitudeDeg > -0.5 && angDistDeg <= solarContactLimitDeg
    }

    val isLunarEclipse = remember(moonData) {
        val dKm = moonData.distanceKm.coerceIn(340000.0, 420000.0)
        // Geocentric angular distance between the Moon and the anti-solar point (Earth's umbral axis)
        val antiSolarSepDeg = Math.toDegrees(moonData.phaseAngleRad) * (1.0 + dKm / 149597870.7)
        // Earth horizontal parallax + Moon semi-diameter + Danjon 2% atmospheric umbra enlargement
        val parallaxDeg = Math.toDegrees(asin(6378.14 / dKm))
        val moonSemiDiamDeg = Math.toDegrees(asin(1737.4 / dKm))
        val umbralContactLimitDeg = 1.02 * (parallaxDeg + 0.0024 - 0.2666) + moonSemiDiamDeg
        moonData.altitudeDeg > -12.0 && antiSolarSepDeg <= umbralContactLimitDeg
    }

    // Lighting state engine
    val lightingState = remember(sunHoriz.altitudeDeg, moonData.altitudeDeg, moonData.illuminationPercent) {
        LightingEngine.computeLightingState(
            sunAltDeg = sunHoriz.altitudeDeg,
            moonAltDeg = moonData.altitudeDeg,
            moonIlluminationPercent = moonData.illuminationPercent
        )
    }

    // Auto-return job
    var autoReturnJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 5-second automatic dismiss timer for the selected celestial object pill
    LaunchedEffect(selectedCelestial) {
        if (selectedCelestial != null) {
            delay(5000)
            selectedCelestial = null
        }
    }

    // Fresh state holders for pointerInput handlers so gestures are never cancelled mid-tap or stuck with stale captures
    val currentIsFa by rememberUpdatedState(isFa)
    val currentCatalogStars by rememberUpdatedState(catalogStars)
    val currentPlanetPositions by rememberUpdatedState(planetPositions)
    val currentSunHoriz by rememberUpdatedState(sunHoriz)
    val currentMoonData by rememberUpdatedState(moonData)
    val currentUserLat by rememberUpdatedState(userLat)
    val currentSkyTheme by rememberUpdatedState(uiState.skyCanvasTheme)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(28.dp))
            .testTag("hero_sky_canvas_container")
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    val touchRadius = 36.dp.toPx()

                    val fa = currentIsFa
                    val lat = currentUserLat
                    val sun = currentSunHoriz
                    val moon = currentMoonData
                    val planets = currentPlanetPositions
                    val stars = currentCatalogStars

                    var closestObj: SelectedCelestialInfo? = null
                    var minDistance = Float.MAX_VALUE

                    // Check Sun
                    if (sun.altitudeDeg > -12.0) {
                        val sunScreenPos = HeroSkyProjection.project(sun.azimuthDeg, sun.altitudeDeg, canvasW, canvasH, lat)
                        val dist = HeroSkyProjection.screenDistance(tapOffset, sunScreenPos, canvasW)
                        if (dist < touchRadius && dist < minDistance) {
                            minDistance = dist
                            closestObj = SelectedCelestialInfo(
                                id = "sun",
                                name = if (fa) "خورشید" else "Sun",
                                position = sunScreenPos,
                                typeName = if (fa) "ستاره مرکزی منظومه شمسی" else "Central Star"
                            )
                        }
                    }

                    // Check Moon
                    if (moon.altitudeDeg > -12.0) {
                        val moonScreenPos = HeroSkyProjection.project(moon.azimuthDeg, moon.altitudeDeg, canvasW, canvasH, lat)
                        val dist = HeroSkyProjection.screenDistance(tapOffset, moonScreenPos, canvasW)
                        if (dist < touchRadius && dist < minDistance) {
                            minDistance = dist
                            closestObj = SelectedCelestialInfo(
                                id = "moon",
                                name = if (fa) "ماه" else "Moon",
                                position = moonScreenPos,
                                typeName = if (fa) moon.phaseNameFa else moon.phaseNameEn
                            )
                        }
                    }

                    // Check Planets
                    planets.forEach { (pType, _, horiz) ->
                        val pPos = HeroSkyProjection.project(horiz.azimuthDeg, horiz.altitudeDeg, canvasW, canvasH, lat)
                        val dist = HeroSkyProjection.screenDistance(tapOffset, pPos, canvasW)
                        if (dist < touchRadius && dist < minDistance) {
                            minDistance = dist
                            val planetId = "planet_${pType.name.lowercase()}"
                            closestObj = SelectedCelestialInfo(
                                id = planetId,
                                name = if (fa) pType.nameFa else pType.nameEn,
                                position = pPos,
                                typeName = if (fa) "سیاره" else "Planet"
                            )
                        }
                    }

                    // Check Catalog Stars
                    stars.forEach { (celestialObj, horiz) ->
                        val sPos = HeroSkyProjection.project(horiz.azimuthDeg, horiz.altitudeDeg, canvasW, canvasH, lat)
                        val dist = HeroSkyProjection.screenDistance(tapOffset, sPos, canvasW)
                        if (dist < touchRadius && dist < minDistance) {
                            minDistance = dist
                            closestObj = SelectedCelestialInfo(
                                id = celestialObj.id,
                                name = if (fa) celestialObj.nameFa else celestialObj.nameEn,
                                position = sPos,
                                typeName = if (fa) celestialObj.type.nameFa else celestialObj.type.nameEn
                            )
                        }
                    }

                    selectedCelestial = closestObj
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        autoReturnJob?.cancel()
                    },
                    onDragEnd = {
                        isDragging = false
                        autoReturnJob = coroutineScope.launch {
                            delay(5000)
                            simulatedOffsetHoursAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(2000, easing = FastOutSlowInEasing)
                            )
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        autoReturnJob = coroutineScope.launch {
                            delay(5000)
                            simulatedOffsetHoursAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(2000, easing = FastOutSlowInEasing)
                            )
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val canvasWidthPx = size.width.toFloat().coerceAtLeast(1f)
                        val deltaHours = -(dragAmount / canvasWidthPx) * 24.0f
                        val newOffset = (simulatedOffsetHoursAnim.value + deltaHours).coerceIn(-12.0f, 12.0f)

                        coroutineScope.launch {
                            simulatedOffsetHoursAnim.snapTo(newOffset)
                        }

                        // Emit Stardust particles along finger path styled by active theme
                        val particleColor = when (currentSkyTheme) {
                            SkyCanvasTheme.REAL_SKY -> if (Random.nextBoolean()) Color(0xFFFFF8EB) else Color(0xFF93C5FD)
                            SkyCanvasTheme.ATMOSPHERIC_SKY -> if (Random.nextBoolean()) Color(0xFF2DD4BF) else Color(0xFFFBBF24)
                            SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (currentSunHoriz.altitudeDeg > 0.0) Color(0xFF18181B) else Color.White
                            SkyCanvasTheme.KIDS_WATERCOLOR -> if (Random.nextBoolean()) Color(0xFFFF85A1) else Color(0xFF70D6FF)
                            SkyCanvasTheme.OBSERVATORY -> Color(0xFFEF4444)
                            SkyCanvasTheme.PAPERCRAFT_DIORAMA -> if (Random.nextBoolean()) Color(0xFFE07A5F) else Color(0xFF81B29A)
                        }
                        repeat(3) {
                            stardustParticles.add(
                                StardustParticle(
                                    x = change.position.x + Random.nextFloat() * 20f - 10f,
                                    y = change.position.y + Random.nextFloat() * 20f - 10f,
                                    vx = Random.nextFloat() * 4f - 2f,
                                    vy = Random.nextFloat() * -3f - 1f,
                                    size = Random.nextFloat() * 5f + 3f,
                                    alpha = 1.0f,
                                    color = particleColor
                                )
                            )
                        }
                    }
                )
            }
    ) {
        // --- GPU CANVAS RENDERING PIPELINE ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val sunPosPx = if (sunHoriz.altitudeDeg > -18.0) {
                HeroSkyProjection.project(sunHoriz.azimuthDeg, sunHoriz.altitudeDeg, canvasW, canvasH, userLat)
            } else null

            // 1. Atmosphere Renderer
            AtmosphereRenderer.drawAtmosphere(
                drawScope = this,
                lightingState = lightingState,
                sunPosPx = sunPosPx,
                theme = uiState.skyCanvasTheme,
                sunAzimuthDeg = sunHoriz.azimuthDeg,
                latitudeDeg = userLat
            )

            // 2. Milky Way Renderer
            MilkyWayRenderer.drawMilkyWay(
                drawScope = this,
                galacticPoints = galacticPlanePoints,
                lightingState = lightingState,
                frameTimeMs = frameTimeMs,
                theme = uiState.skyCanvasTheme,
                latitudeDeg = userLat,
                lastDeg = lastDeg
            )

            // 3. Star Renderer
            StarRenderer.drawStars(
                drawScope = this,
                objects = catalogStars,
                starVisibility = lightingState.starVisibility,
                frameTimeMs = frameTimeMs,
                theme = uiState.skyCanvasTheme,
                latitudeDeg = userLat,
                lastDeg = lastDeg
            )

            // 4. Sun Renderer
            if (sunPosPx != null && sunHoriz.altitudeDeg > -12.0) {
                SunRenderer.drawSun(
                    drawScope = this,
                    center = sunPosPx,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    frameTimeMs = frameTimeMs,
                    theme = uiState.skyCanvasTheme
                )
            }

            // 5. Moon Renderer
            if (moonData.altitudeDeg > -12.0) {
                val moonCenter = HeroSkyProjection.project(moonData.azimuthDeg, moonData.altitudeDeg, canvasW, canvasH, userLat)
                val baseMoonRadius = 26.dp.toPx()
                val moonPulseScale = AstronomyAnimator.computePulse(frameTimeMs, 4000f, 0.88f, 1.12f)

                val limbScreenAngleDeg = CoordinateEngine.calculateMoonLimbScreenAngleDeg(
                    moonAzimuthDeg = moonData.azimuthDeg,
                    moonAltitudeDeg = moonData.altitudeDeg,
                    sunAzimuthDeg = sunHoriz.azimuthDeg,
                    sunAltitudeDeg = sunHoriz.altitudeDeg
                )

                MoonRenderer.drawMoon(
                    drawScope = this,
                    center = moonCenter,
                    radius = baseMoonRadius,
                    illuminationPercent = moonData.illuminationPercent,
                    phaseAngleRad = moonData.phaseAngleRad,
                    isLunarEclipse = isLunarEclipse,
                    isSolarEclipse = isSolarEclipse,
                    moonPulseScale = moonPulseScale,
                    lightingState = lightingState,
                    frameTimeMs = frameTimeMs,
                    isWaxing = (moonData.ageDays < 14.765),
                    theme = uiState.skyCanvasTheme,
                    limbScreenAngleDeg = limbScreenAngleDeg
                )
            }

            // 6. Planet Renderer
            PlanetRenderer.drawPlanets(
                drawScope = this,
                planets = planetPositions,
                frameTimeMs = frameTimeMs,
                theme = uiState.skyCanvasTheme,
                latitudeDeg = userLat
            )

            // 7. Horizon Landscape Silhouette Layer
            LandscapeRenderer.drawHorizonLandscape(
                drawScope = this,
                lightingState = lightingState,
                frameTimeMs = frameTimeMs,
                theme = uiState.skyCanvasTheme
            )

            // 7. Tapped Celestial Target Ring Overlay
            selectedCelestial?.let { sel ->
                val selPos = when {
                    sel.id == "sun" -> HeroSkyProjection.project(sunHoriz.azimuthDeg, sunHoriz.altitudeDeg, canvasW, canvasH, userLat)
                    sel.id == "moon" -> HeroSkyProjection.project(moonData.azimuthDeg, moonData.altitudeDeg, canvasW, canvasH, userLat)
                    sel.id.startsWith("planet_") -> {
                        val pName = sel.id.removePrefix("planet_")
                        planetPositions.find { it.first.name.equals(pName, ignoreCase = true) }?.let {
                            HeroSkyProjection.project(it.third.azimuthDeg, it.third.altitudeDeg, canvasW, canvasH, userLat)
                        } ?: sel.position
                    }
                    else -> {
                        catalogStars.find { it.first.id == sel.id }?.let {
                            HeroSkyProjection.project(it.second.azimuthDeg, it.second.altitudeDeg, canvasW, canvasH, userLat)
                        } ?: sel.position
                    }
                }
                val pulseRing = 1.0f + 0.12f * sin(frameTimeMs * 0.005f).toFloat()
                val ringColor = when (uiState.skyCanvasTheme) {
                    SkyCanvasTheme.REAL_SKY -> Color(0xFFB4DCFF)
                    SkyCanvasTheme.ATMOSPHERIC_SKY -> Color(0xFF38BDF8)
                    SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (sunHoriz.altitudeDeg > 0.0) Color(0xFF18181B) else Color.White
                    SkyCanvasTheme.KIDS_WATERCOLOR -> Color(0xFFFF85A1)
                    SkyCanvasTheme.OBSERVATORY -> Color(0xFFEF4444)
                    SkyCanvasTheme.PAPERCRAFT_DIORAMA -> Color(0xFFE07A5F)
                }
                drawCircle(
                    color = ringColor.copy(alpha = 0.5f),
                    radius = 28.dp.toPx() * pulseRing,
                    center = selPos,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = ringColor,
                    radius = 18.dp.toPx(),
                    center = selPos,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            // 8. Particle Renderer (Stardust particles)
            ParticleRenderer.drawStardust(
                drawScope = this,
                particles = stardustParticles
            )
        }

        // --- FLOATING SELECTED CELESTIAL OBJECT NAME PILL ---
        selectedCelestial?.let { sel ->
            val canvasW = constraints.maxWidth.toFloat()
            val canvasH = constraints.maxHeight.toFloat()
            val livePos = when {
                sel.id == "sun" -> HeroSkyProjection.project(sunHoriz.azimuthDeg, sunHoriz.altitudeDeg, canvasW, canvasH, userLat)
                sel.id == "moon" -> HeroSkyProjection.project(moonData.azimuthDeg, moonData.altitudeDeg, canvasW, canvasH, userLat)
                sel.id.startsWith("planet_") -> {
                    val pName = sel.id.removePrefix("planet_")
                    planetPositions.find { it.first.name.equals(pName, ignoreCase = true) }?.let {
                        HeroSkyProjection.project(it.third.azimuthDeg, it.third.altitudeDeg, canvasW, canvasH, userLat)
                    } ?: sel.position
                }
                else -> {
                    catalogStars.find { it.first.id == sel.id }?.let {
                        HeroSkyProjection.project(it.second.azimuthDeg, it.second.altitudeDeg, canvasW, canvasH, userLat)
                    } ?: sel.position
                }
            }
            val xDp = with(LocalDensity.current) { livePos.x.toDp() }
            val yDp = with(LocalDensity.current) { livePos.y.toDp() }
            val maxPillX = (maxWidth - 160.dp).coerceAtLeast(12.dp)
            val maxPillY = (maxHeight - 84.dp).coerceAtLeast(12.dp)

            Surface(
                onClick = {
                    val idToOpen = sel.id
                    selectedCelestial = null
                    if (idToOpen.isNotEmpty()) {
                        viewModel.openObjectDetailById(idToOpen, simulatedTimeMs)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEE0F172A),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset(
                        x = (xDp - 70.dp).coerceIn(12.dp, maxPillX),
                        y = (yDp - 54.dp).coerceIn(12.dp, maxPillY)
                    )
                    .testTag("selected_celestial_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                    )
                    Text(
                        text = sel.name,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    )
                    if (sel.typeName != null) {
                        Text(
                            text = "• ${sel.typeName}",
                            style = TextStyle(
                                fontFamily = LocalAppFontFamily.current,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }
        }

        // --- BOTTOM PILLS (Current / Simulated Time Pill & Date Pill) ---
        val isTimeOffsetActive = isDragging || abs(currentOffsetHours) > 0.05f
        val timeText = TimeEngine.formatTime24h(simulatedTimeMs, isFa, userTimeZone)
        val totalOffsetMinutes = (currentOffsetHours * 60f).roundToInt()
        val offsetText = if (abs(totalOffsetMinutes) >= 1) {
            val sign = if (totalOffsetMinutes > 0) "+" else "-"
            val absMins = abs(totalOffsetMinutes)
            val hrs = absMins / 60
            val mins = absMins % 60
            val rawStr = String.format(Locale.US, "%s%d:%02dh", sign, hrs, mins)
            if (isFa) rawStr.toPersianDigits() else rawStr
        } else ""

        val accentDotColor = if (isTimeOffsetActive) {
            when (uiState.skyCanvasTheme) {
                SkyCanvasTheme.REAL_SKY -> Color(0xFFB4DCFF)
                SkyCanvasTheme.ATMOSPHERIC_SKY -> Color(0xFFFBBF24)
                SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> Color.White
                SkyCanvasTheme.KIDS_WATERCOLOR -> Color(0xFFFF85A1)
                SkyCanvasTheme.OBSERVATORY -> Color(0xFFEF4444)
                SkyCanvasTheme.PAPERCRAFT_DIORAMA -> Color(0xFFE07A5F)
            }
        } else {
            Color(0xFF2DD4BF)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stationary Current / Simulated Time pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x66000000),
                border = BorderStroke(
                    width = if (isTimeOffsetActive) 0.8.dp else 0.5.dp,
                    color = if (isTimeOffsetActive) accentDotColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.testTag("time_travel_bubble")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accentDotColor)
                    )
                    if (isTimeOffsetActive) {
                        Text(
                            text = timeText,
                            style = TextStyle(
                                fontFamily = LocalAppFontFamily.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFF9FAFB)
                            )
                        )
                        if (offsetText.isNotEmpty()) {
                            Text(
                                text = "($offsetText)",
                                style = TextStyle(
                                    fontFamily = LocalAppFontFamily.current,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            )
                        }
                    } else {
                        val livePrefix = if (uiState.timeMachineState.mode == TimeMachineMode.SIMULATION) {
                            if (isFa) "شبیه‌سازی" else "Simulated"
                        } else {
                            if (isFa) "آسمان زنده" else "Live sky"
                        }
                        Text(
                            text = "$livePrefix • $timeText",
                            style = TextStyle(
                                fontFamily = LocalAppFontFamily.current,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = Color(0xFFF9FAFB)
                            )
                        )
                    }
                }
            }

            // Minimalistic Date pill
            val formattedDate = TimeEngine.formatDate(simulatedTimeMs, uiState.calendarSystem, isFa, userTimeZone).let {
                if (isFa) it.toPersianDigits() else it
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x66000000),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Text(
                    text = formattedDate,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = Color(0xFFF9FAFB)
                    )
                )
            }
        }
    }
}


