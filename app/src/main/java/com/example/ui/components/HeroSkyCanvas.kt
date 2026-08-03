package com.example.ui.components

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
import com.example.astro_engine.*
import com.example.data.catalog.AstronomyCatalog
import com.example.domain.AppLanguage
import com.example.domain.ObjectType
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.rendering.*
import com.example.ui.theme.IranSans
import com.example.util.toPersianDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val density = LocalDensity.current

    // Base Julian Date for now
    val baseJd = remember { TimeEngine.getJulianDate() }

    // Direct Finger Time Travel state
    val simulatedOffsetHoursAnim = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var touchPos by remember { mutableStateOf(Offset.Zero) }

    // Floating Time Bubble position spring animation
    val bubbleX by animateFloatAsState(
        targetValue = touchPos.x,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "BubbleX"
    )
    val bubbleY by animateFloatAsState(
        targetValue = touchPos.y,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "BubbleY"
    )

    // Stardust Particles list
    val stardustParticles = remember { mutableStateListOf<StardustParticle>() }

    // Continuous frame time for star twinkling & Moon pulse
    var frameTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { ms ->
                frameTimeMs = ms
                // Update stardust particles
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

    // Moon Glow Pulsing Infinite Transition
    val infiniteTransition = rememberInfiniteTransition(label = "MoonGlowTransition")
    val moonPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MoonPulse"
    )

    // Current Simulated Julian Date
    val currentOffsetHours = simulatedOffsetHoursAnim.value
    val simulatedJd = baseJd + (currentOffsetHours / 24.0)
    val simulatedTimeMs = remember(simulatedJd) {
        ((simulatedJd - 2440587.5) * 86400000.0).toLong()
    }

    // Astro computations
    val userLat = uiState.userLocation.latitude
    val userLon = uiState.userLocation.longitude
    val lastDeg = remember(simulatedJd, userLon) { TimeEngine.getLAST(simulatedJd, userLon) }

    // Sun position
    val sunPos = remember(simulatedJd) { SunEngine.calculatePosition(simulatedJd) }
    val sunHoriz = remember(sunPos, lastDeg, userLat) {
        CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            lastDeg,
            userLat
        )
    }

    // Moon calculation
    val moonData = remember(simulatedJd, userLat, userLon) {
        MoonEngine.calculateMoon(simulatedJd, userLat, userLon)
    }

    // Planets calculation
    val planetPositions = remember(simulatedJd, lastDeg, userLat) {
        PlanetEngine.PlanetType.values().mapNotNull { pType ->
            if (pType == PlanetEngine.PlanetType.PLUTO) null
            else {
                val pPos = PlanetEngine.calculatePlanet(pType, simulatedJd)
                val horiz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(pPos.raDeg, pPos.decDeg),
                    lastDeg,
                    userLat
                )
                if (horiz.altitudeDeg > -2.0) Triple(pType, pPos, horiz) else null
            }
        }
    }

    // Galactic plane points
    val galacticPlanePoints = remember(simulatedJd, userLat, userLon) {
        GalacticEngine.calculateGalacticPlanePoints(simulatedJd, userLat, userLon)
            .filter { it.altitudeDeg > -5.0 }
    }

    // Catalog Stars & Deep Sky Objects (Stars, Galaxies, Nebulae)
    val catalogStars = remember(simulatedJd, lastDeg, userLat) {
        AstronomyCatalog.getAllObjects(simulatedJd)
            .filter { (it.type == ObjectType.STAR || it.type == ObjectType.DEEP_SKY) && it.magnitude <= 4.5 }
            .mapNotNull { celestialObj ->
                val horiz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(celestialObj.raDeg, celestialObj.decDeg),
                    lastDeg,
                    userLat
                )
                if (horiz.altitudeDeg > 0.0) Pair(celestialObj, horiz) else null
            }
    }

    // Eclipse detection
    val isSolarEclipse = remember(sunHoriz, moonData) {
        val dAz = abs(sunHoriz.azimuthDeg - moonData.azimuthDeg)
        val dAlt = abs(sunHoriz.altitudeDeg - moonData.altitudeDeg)
        val angDist = sqrt(dAz * dAz + dAlt * dAlt)
        sunHoriz.altitudeDeg > 0.0 && angDist < 1.2
    }

    val isLunarEclipse = remember(moonData, sunHoriz) {
        moonData.illuminationPercent > 95.0 && abs(sunHoriz.altitudeDeg + moonData.altitudeDeg) < 2.0
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

    // Selected Tapped Celestial Object
    var selectedCelestial by remember { mutableStateOf<SelectedCelestialInfo?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(28.dp))
            .testTag("hero_sky_canvas_container")
            .pointerInput(isFa, catalogStars, planetPositions, sunHoriz, moonData) {
                detectTapGestures { tapOffset ->
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    val touchRadius = 48.dp.toPx()

                    var closestObj: SelectedCelestialInfo? = null
                    var minDistance = Float.MAX_VALUE

                    // Check Sun
                    if (sunHoriz.altitudeDeg > -10.0) {
                        val sunX = (sunHoriz.azimuthDeg / 360.0 * canvasW).toFloat()
                        val sunY = (canvasH - ((sunHoriz.altitudeDeg + 10.0) / 100.0 * canvasH)).toFloat()
                        val dist = hypot(tapOffset.x - sunX, tapOffset.y - sunY)
                        if (dist < touchRadius && dist < minDistance) {
                            minDistance = dist
                            closestObj = SelectedCelestialInfo(
                                name = if (isFa) "خورشید" else "Sun",
                                position = Offset(sunX, sunY),
                                typeName = if (isFa) "ستاره مرکزی منظومه شمسی" else "Central Star"
                            )
                        }
                    }

                    // Check Moon
                    if (moonData.altitudeDeg > -5.0) {
                        val moonX = (moonData.azimuthDeg / 360.0 * canvasW).toFloat()
                        val moonY = (canvasH - ((moonData.altitudeDeg + 5.0) / 95.0 * canvasH)).toFloat().coerceIn(50.dp.toPx(), canvasH - 40.dp.toPx())
                        val dist = hypot(tapOffset.x - moonX, tapOffset.y - moonY)
                        if (dist < touchRadius && dist < minDistance) {
                            minDistance = dist
                            closestObj = SelectedCelestialInfo(
                                name = if (isFa) "ماه" else "Moon",
                                position = Offset(moonX, moonY),
                                typeName = if (isFa) moonData.phaseNameFa else moonData.phaseNameEn
                            )
                        }
                    }

                    // Check Planets
                    planetPositions.forEach { (pType, _, horiz) ->
                        val px = (horiz.azimuthDeg / 360.0 * canvasW).toFloat()
                        val py = (canvasH - ((horiz.altitudeDeg + 5.0) / 95.0 * canvasH)).toFloat()
                        val dist = hypot(tapOffset.x - px, tapOffset.y - py)
                        if (dist < touchRadius && dist < minDistance) {
                            minDistance = dist
                            closestObj = SelectedCelestialInfo(
                                name = if (isFa) pType.nameFa else pType.nameEn,
                                position = Offset(px, py),
                                typeName = if (isFa) "سیاره" else "Planet"
                            )
                        }
                    }

                    // Check Catalog Stars / Deep Sky
                    catalogStars.forEach { (celestialObj, horiz) ->
                        val sx = (horiz.azimuthDeg / 360.0 * canvasW).toFloat()
                        val sy = (canvasH - (horiz.altitudeDeg / 90.0 * canvasH)).toFloat()
                        val dist = hypot(tapOffset.x - sx, tapOffset.y - sy)
                        if (dist < touchRadius && dist < minDistance) {
                            minDistance = dist
                            closestObj = SelectedCelestialInfo(
                                name = if (isFa) celestialObj.nameFa else celestialObj.nameEn,
                                position = Offset(sx, sy),
                                typeName = if (isFa) celestialObj.type.nameFa else celestialObj.type.nameEn
                            )
                        }
                    }

                    selectedCelestial = closestObj
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        touchPos = offset
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
                        touchPos = change.position
                        val canvasWidthPx = size.width.toFloat()
                        val deltaHours = -(dragAmount / canvasWidthPx) * 24.0f
                        val newOffset = (simulatedOffsetHoursAnim.value + deltaHours).coerceIn(-12.0f, 12.0f)

                        coroutineScope.launch {
                            simulatedOffsetHoursAnim.snapTo(newOffset)
                        }

                        // Emit Stardust particles along finger path
                        repeat(3) {
                            stardustParticles.add(
                                StardustParticle(
                                    x = change.position.x + Random.nextFloat() * 20f - 10f,
                                    y = change.position.y + Random.nextFloat() * 20f - 10f,
                                    vx = Random.nextFloat() * 4f - 2f,
                                    vy = Random.nextFloat() * -3f - 1f,
                                    size = Random.nextFloat() * 5f + 3f,
                                    alpha = 1.0f,
                                    color = if (Random.nextBoolean()) Color(0xFF2DD4BF) else Color(0xFFFBBF24)
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

            // Sun Screen Position
            val sunPosPx = if (sunHoriz.altitudeDeg > -10.0) {
                val sunX = (sunHoriz.azimuthDeg / 360.0 * canvasW).toFloat()
                val sunY = (canvasH - ((sunHoriz.altitudeDeg + 10.0) / 100.0 * canvasH)).toFloat()
                Offset(sunX, sunY)
            } else null

            // 1. Atmosphere Renderer (Procedural Rayleigh/Mie Scattering & Gradients)
            AtmosphereRenderer.drawAtmosphere(
                drawScope = this,
                lightingState = lightingState,
                sunPosPx = sunPosPx
            )

            // 2. Star Renderer (Spectral color types, independent twinkle, halos, galaxies)
            StarRenderer.drawStars(
                drawScope = this,
                objects = catalogStars,
                starVisibility = lightingState.starVisibility,
                frameTimeMs = frameTimeMs
            )

            // 3. Sun Renderer
            if (sunPosPx != null) {
                SunRenderer.drawSun(
                    drawScope = this,
                    center = sunPosPx,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    frameTimeMs = frameTimeMs
                )
            }

            // 4. Moon Renderer
            if (moonData.altitudeDeg > -5.0) {
                val moonX = (moonData.azimuthDeg / 360.0 * canvasW).toFloat()
                val moonY = (canvasH - ((moonData.altitudeDeg + 5.0) / 95.0 * canvasH)).toFloat().coerceIn(50.dp.toPx(), canvasH - 40.dp.toPx())
                val baseMoonRadius = 26.dp.toPx()

                MoonRenderer.drawMoon(
                    drawScope = this,
                    center = Offset(moonX, moonY),
                    radius = baseMoonRadius,
                    illuminationPercent = moonData.illuminationPercent,
                    phaseAngleRad = moonData.phaseAngleRad,
                    isLunarEclipse = isLunarEclipse,
                    isSolarEclipse = isSolarEclipse,
                    moonPulseScale = moonPulseScale,
                    lightingState = lightingState,
                    frameTimeMs = frameTimeMs,
                    isWaxing = (moonData.ageDays < 14.765)
                )
            }

            // 6. Planet Renderer
            PlanetRenderer.drawPlanets(
                drawScope = this,
                planets = planetPositions,
                frameTimeMs = frameTimeMs
            )

            // 7. Tapped Celestial Target Ring Overlay
            selectedCelestial?.let { sel ->
                val pulseRing = 1.0f + 0.12f * sin(frameTimeMs * 0.005f).toFloat()
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                    radius = 28.dp.toPx() * pulseRing,
                    center = sel.position,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFFDE047),
                    radius = 18.dp.toPx(),
                    center = sel.position,
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
            val xDp = with(LocalDensity.current) { sel.position.x.toDp() }
            val yDp = with(LocalDensity.current) { sel.position.y.toDp() }

            Surface(
                onClick = { selectedCelestial = null },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEE0F172A),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset(
                        x = (xDp - 70.dp).coerceIn(12.dp, 200.dp),
                        y = (yDp - 54.dp).coerceAtLeast(12.dp)
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
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    )
                    if (sel.typeName != null) {
                        Text(
                            text = "• ${sel.typeName}",
                            style = TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }
        }

        // --- SUBTLE CANVAS FOOTER BADGES ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val twilight = SunEngine.getTwilightPhase(sunHoriz.altitudeDeg)
            val statusText = when {
                twilight.isDaylight -> if (isFa) "روز / روشنایی" else "Daylight"
                sunHoriz.altitudeDeg in -6.0..0.0 -> if (isFa) "شفق / حواشی غروب" else "Civil Twilight"
                sunHoriz.altitudeDeg in -12.0..-6.0 -> if (isFa) "شفق دریایی" else "Nautical Twilight"
                sunHoriz.altitudeDeg in -18.0..-12.0 -> if (isFa) "شفق نجومی" else "Astronomical Twilight"
                else -> if (isFa) "تاریکی مطلق / شب" else "Peak Dark Night"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x44000000),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (twilight.isDaylight) Color(0xFFFBBF24) else Color(0xFF2DD4BF))
                    )
                    Text(
                        text = statusText,
                        style = TextStyle(
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    )
                }
            }

            val formattedDate = TimeEngine.formatDate(simulatedTimeMs, uiState.calendarSystem, isFa).let {
                if (isFa) it.toPersianDigits() else it
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x44000000),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = formattedDate,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = TextStyle(
                        fontFamily = IranSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color(0xFF2DD4BF)
                    )
                )
            }
        }

        // --- TIME BUBBLE (Glassmorphism Floating Pill) ---
        if (isDragging || abs(currentOffsetHours) > 0.05f) {
            val timeText = TimeEngine.formatTime24h(simulatedTimeMs, isFa)
            val offsetText = if (abs(currentOffsetHours) > 0.1f) {
                val sign = if (currentOffsetHours > 0) "+" else ""
                val hrs = currentOffsetHours.toInt()
                val mins = (abs(currentOffsetHours - hrs) * 60).toInt()
                val rawStr = String.format("%s%d:%02dh", sign, hrs, mins)
                if (isFa) rawStr.toPersianDigits() else rawStr
            } else ""

            val bubbleWidthPx = with(density) { 130.dp.toPx() }
            val clampedBubbleX = (bubbleX - bubbleWidthPx / 2f).coerceIn(10f, with(density) { 220.dp.toPx() })
            val clampedBubbleY = (bubbleY - with(density) { 70.dp.toPx() }).coerceIn(10f, with(density) { 240.dp.toPx() })

            Box(
                modifier = Modifier
                    .offset { IntOffset(clampedBubbleX.toInt(), clampedBubbleY.toInt()) }
                    .shadow(12.dp, CircleShape, spotColor = Color(0xFF2DD4BF))
                    .clip(CircleShape)
                    .background(Color(0xCC0F172A))
                    .border(1.5.dp, Color(0xFF2DD4BF), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("time_travel_bubble")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = timeText,
                        style = TextStyle(
                            fontFamily = IranSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF2DD4BF)
                        )
                    )
                    if (offsetText.isNotEmpty()) {
                        Text(
                            text = "($offsetText)",
                            style = TextStyle(
                                fontFamily = IranSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        }
    }
}

