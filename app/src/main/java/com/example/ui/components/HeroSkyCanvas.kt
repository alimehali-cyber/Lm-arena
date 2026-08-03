package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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

    // Catalog Stars
    val catalogStars = remember(simulatedJd, lastDeg, userLat) {
        AstronomyCatalog.getAllObjects(simulatedJd)
            .filter { it.type == ObjectType.STAR && it.magnitude <= 4.5 }
            .mapNotNull { star ->
                val horiz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(star.raDeg, star.decDeg),
                    lastDeg,
                    userLat
                )
                if (horiz.altitudeDeg > 0.0) Pair(star, horiz) else null
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(28.dp))
            .testTag("hero_sky_canvas_container")
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

            // 2. Milky Way Renderer
            MilkyWayRenderer.drawMilkyWay(
                drawScope = this,
                galacticPoints = galacticPlanePoints,
                lightingState = lightingState
            )

            // 3. Star Renderer (Spectral color types, independent twinkle, halos)
            StarRenderer.drawStars(
                drawScope = this,
                stars = catalogStars,
                starVisibility = lightingState.starVisibility,
                frameTimeMs = frameTimeMs
            )

            // 4. Sun Renderer
            if (sunPosPx != null) {
                SunRenderer.drawSun(
                    drawScope = this,
                    center = sunPosPx,
                    sunAltitudeDeg = sunHoriz.altitudeDeg,
                    frameTimeMs = frameTimeMs
                )
            }

            // 5. Moon Renderer
            val moonX = if (moonData.altitudeDeg > -5.0) {
                (moonData.azimuthDeg / 360.0 * canvasW).toFloat()
            } else {
                canvasW * 0.78f
            }
            val moonY = if (moonData.altitudeDeg > -5.0) {
                (canvasH - ((moonData.altitudeDeg + 5.0) / 95.0 * canvasH)).toFloat().coerceIn(50.dp.toPx(), canvasH - 40.dp.toPx())
            } else {
                canvasH * 0.32f
            }
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
                lightingState = lightingState
            )

            // 6. Planet Renderer
            PlanetRenderer.drawPlanets(
                drawScope = this,
                planets = planetPositions
            )

            // 7. Particle Renderer
            ParticleRenderer.drawStardust(
                drawScope = this,
                particles = stardustParticles
            )
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

