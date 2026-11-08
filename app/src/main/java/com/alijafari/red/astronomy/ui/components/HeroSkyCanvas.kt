package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.domain.TimeMachineMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.monuments.MonumentCopy
import com.alijafari.red.astronomy.ui.monuments.MonumentId
import com.alijafari.red.astronomy.ui.mountain.HomeMountain
import com.alijafari.red.astronomy.ui.persepolis.PersepolisFeature
import com.alijafari.red.astronomy.ui.persepolis.PersepolisLandscape
import com.alijafari.red.astronomy.ui.rendering.*
import com.alijafari.red.astronomy.ui.rendering.landscape.PulvarRiver3D
import com.alijafari.red.astronomy.ui.skypanorama.SkyPanoramaFeature
import com.alijafari.red.astronomy.ui.skypanorama.SkyPanoramaLayer
import com.alijafari.red.astronomy.ui.skypanorama.SkyPanoramaState
import com.alijafari.red.astronomy.ui.skypanorama.SkyPanoramaStatus
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.solidSurface
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.alijafari.red.astronomy.util.toPersianDigits
import java.util.Locale
import kotlin.math.*
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Idle frame interval of the full-bleed scene: ~24 fps. Slow cloud drift and a breathing figure need no
 * more, and the sky itself is not repainted on this clock (see the three drawing layers below).
 */
private const val SCENE_MOTION_FRAME_MS = 42L

/**
 * Quantum of the instant the sky MODEL is computed for, in live mode.
 *
 * Home used to recompute the whole scene — Moon ELP2000, every planet, every drawn star, the galactic
 * plane — once per second on the main thread. The sky rotates 15°/hour, so fifteen seconds is 0.06°:
 * well under a pixel on the Home scene at any latitude. Keying the model on a 15 s bucket instead of the
 * 1 Hz clock cuts that work by 15x with no visible change. The visible clock, the motion layer and the
 * frame counters keep their own, finer clocks.
 */
private const val SKY_MODEL_QUANTUM_MS = 15_000L

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

/** How the sky canvas is presented. The rendering is identical; the framing and the chrome change. */
enum class SkyCanvasPresentation {

    /**
     * The full-bleed Home scene: edge to edge, no rounded corners, a low horizon, the ground band, the
     * cloud veils and the observer. Nothing is written on it — the instant, the date, the condition and
     * the Moon's state live on the card beneath the sky — so this is the surface that reads like a
     * weather app's hero with the numbers below the picture, drawn by ZIG's own astronomy.
     */
    SCENE,

    /** The fullscreen landscape sky viewer: the same sky, edge to edge, with its time and date pills. */
    VIEWER
}

/**
 * The interactive sky canvas: the shared sky renderer, tap-to-identify and finger time travel.
 *
 * The caller owns the size. [presentation] decides the framing: the full-bleed Home scene, or the
 * landscape viewer. [onExpand] adds the small expand affordance that opens the fullscreen viewer.
 *
 * The drawing is split into three layers so the moving parts never repaint the astronomy:
 *  1. the sky, on the clock tick (or per frame while the finger scrubs time);
 *  2. the scene motion — drifting cloud veils and the breathing observer — on a faster tick, scene only;
 *  3. the interaction layer — selection ring and stardust — only while the user is touching it.
 *
 * Between layers 1 and 2, the scene on the photographic themes also carries the combined
 * Persepolis + Mil-e Azhdaha relief (see [PersepolisLandscape]): a transparent GL surface standing
 * in layer 1's ground band, lit by the
 * same ephemeris, rendering one frame per sky-model change. It is a peer of the sky clock, not of the
 * motion clock, so it is not numbered with the repaint layers above.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroSkyCanvas(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    presentation: SkyCanvasPresentation = SkyCanvasPresentation.VIEWER,
    onExpand: (() -> Unit)? = null,
    // Scene presentation only: how far the ground's dissolved tail reaches *below* the
    // canvas's own bottom edge (see the SCENE TAIL layer below). Home passes the height
    // of the card row that follows the canvas, so the tail passes underneath the cards;
    // every other caller leaves it at zero and the canvas ends at its own edge.
    bottomTailHeight: Dp = 0.dp
) {
    val isFa = uiState.language == AppLanguage.PERSIAN
    val coroutineScope = rememberCoroutineScope()

    val isScene = presentation == SkyCanvasPresentation.SCENE
    // The scene puts the horizon low so the sky owns the frame; the viewer keeps the old 0.85.
    val horizonFraction = if (isScene) SkySceneLayout.HORIZON_FRACTION else HeroSkyProjection.HORIZON_FRACTION
    val renderTarget = if (isScene) SkyRenderTarget.HOME_SCENE else SkyRenderTarget.HOME_HERO

    // Live clock shared with the Live Sky backdrop (one timer, owned by MainViewModel, running only while collected).
    val currentSystemTimeMs by viewModel.skyLiveClock.timeMs.collectAsState()

    // Direct Finger Time Travel state
    val simulatedOffsetHoursAnim = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Publish the drag offset (including the animated return) so the Live Sky backdrop follows the same instant.
    // It is reset when Home leaves, exactly as this local offset is.
    LaunchedEffect(Unit) {
        snapshotFlow { simulatedOffsetHoursAnim.value }.collect { viewModel.setSkyDragOffsetHours(it) }
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.setSkyDragOffsetHours(0f) }
    }

    // Stardust Particles list
    val stardustParticles = remember { mutableStateListOf<StardustParticle>() }

    // Selected Tapped Celestial Object
    var selectedCelestial by remember { mutableStateOf<SelectedCelestialInfo?>(null) }

    // Selected monument standing in the landscape. Persepolis and Mil-e Azhdaha are places, not
    // catalogue objects, so the tap does not open the celestial detail sheet: it raises the place's
    // name, and tapping that name opens the monument's own page (see MonumentScreen).
    var selectedMonument by remember { mutableStateOf<MonumentId?>(null) }

    // The monument hit boxes are the resolved framing's own pixel landmarks (see PersepolisFraming),
    // so a tap lands on exactly the stone the GL relief drew. Published from inside the
    // BoxWithConstraints scope below, where the scene size is known, and read at gesture time.
    var sceneFraming by remember { mutableStateOf<PersepolisFraming.Result?>(null) }

    // Two clocks. The sky clock moves on the shared 1 Hz tick (and per frame while time is being scrubbed);
    // the motion clock runs the cloud drift and the observer at a capped frame rate. Keeping them apart is
    // what lets the cheap layer animate without repainting the stars, the Moon sprite and the Milky Way.
    var skyTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var motionTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasParticles by remember { derivedStateOf { stardustParticles.isNotEmpty() } }
    val isAnimatingReturn = simulatedOffsetHoursAnim.isRunning
    val hasSelection = selectedCelestial != null

    // Steady state real-time clock: one tick per second from the shared clock, instead of every frame when idle
    LaunchedEffect(currentSystemTimeMs) {
        if (!isDragging && !simulatedOffsetHoursAnim.isRunning) {
            skyTimeMs = currentSystemTimeMs
        }
    }

    LaunchedEffect(isDragging, hasParticles, isAnimatingReturn, hasSelection, isScene) {
        // The keys above are exactly the inputs of this flag, so it is always fresh at launch: a change
        // restarts the effect.
        val interactive = isDragging || hasParticles || isAnimatingReturn || hasSelection

        fun stepParticles() {
            if (stardustParticles.isEmpty()) return
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

        while (interactive || isScene) {
            if (interactive) {
                // Anything the finger drives runs at the display rate, sky included.
                withFrameMillis { ms ->
                    motionTimeMs = ms
                    skyTimeMs = ms
                    stepParticles()
                }
            } else {
                // The idle scene wakes at ~24 Hz rather than on every vsync, and never touches the sky
                // clock: drifting veils and a breathing figure need no more, and the astronomy keeps its
                // own slow tick.
                delay(SCENE_MOTION_FRAME_MS)
                motionTimeMs = System.currentTimeMillis()
            }
        }
    }

    // Current Simulated Julian Date (Honours TimeMachineState in simulation mode, plus any finger time travel offset)
    val currentOffsetHours = simulatedOffsetHoursAnim.value
    val isSimulationMode = uiState.timeMachineState.mode == TimeMachineMode.SIMULATION
    val baseTimeMs = SkyTimeModel.baseTimeMs(
        isSimulation = isSimulationMode,
        simulationTimeMs = uiState.timeMachineState.simulationTimeMs,
        liveTimeMs = currentSystemTimeMs
    )
    // The model instant is bucketed in live mode (see SKY_MODEL_QUANTUM_MS) and exact in simulation mode,
    // where it only moves when the user moves the time machine. The drag offset is never bucketed: the
    // finger has to move the sky as it moves.
    val modelBaseTimeMs = if (isSimulationMode) {
        baseTimeMs
    } else {
        baseTimeMs - Math.floorMod(baseTimeMs, SKY_MODEL_QUANTUM_MS)
    }
    // Same effective instant the Live Sky backdrop uses (see SkyTimeModel)
    val simulatedJd = SkyTimeModel.effectiveJd(modelBaseTimeMs, currentOffsetHours)
    // The instant the READOUT shows is exact, not bucketed: the clock must never lag its own minute. Only
    // the sky model above is quantised, and 15 s of sky rotation is far below one pixel.
    val simulatedTimeMs = TimeEngine.getTimestampFromJulianDate(
        SkyTimeModel.effectiveJd(baseTimeMs, currentOffsetHours)
    )

    // Astro computations
    val userLat = uiState.userLocation.latitude
    val userLon = uiState.userLocation.longitude
    val userElev = uiState.userLocation.elevationMeters
    val userTimeZone = remember(uiState.userLocation.timezoneId) {
        TimeEngine.resolveTimeZone(uiState.userLocation.timezoneId)
    }
    // Astro computations: one shared scene model (also used by the Live Sky app backdrop)
    val skyFrame = remember(simulatedJd, userLat, userLon, userElev) {
        SkySceneModel.compute(simulatedJd, userLat, userLon, userElev)
    }
    val lastDeg = skyFrame.lastDeg
    val sunHoriz = skyFrame.sunHoriz
    val moonData = skyFrame.moonData
    val planetPositions = skyFrame.planetPositions
    val galacticPlanePoints = skyFrame.galacticPlanePoints
    val catalogStars = skyFrame.catalogStars
    val isSolarEclipse = skyFrame.isSolarEclipse
    val isLunarEclipse = skyFrame.isLunarEclipse
    val lightingState = skyFrame.lightingState

    // Auto-return job
    var autoReturnJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Keep the identification visible long enough to read and open its detail card.
    LaunchedEffect(selectedCelestial) {
        if (selectedCelestial != null) {
            delay(4_000)
            selectedCelestial = null
        }
    }

    // The monument's name sits longer than a star's: it is an invitation to open a page, so it must
    // survive a slow read. It is only ever a name; nothing else about the scene changes.
    LaunchedEffect(selectedMonument) {
        if (selectedMonument != null) {
            delay(7_000)
            selectedMonument = null
        }
    }

    // A fresh radial bloom accompanies each selection; the continuous scene clock then carries its gentle pulse.
    val selectionBloom = remember { Animatable(1f) }
    LaunchedEffect(selectedCelestial?.id) {
        if (selectedCelestial != null) {
            selectionBloom.snapTo(0f)
            selectionBloom.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
            )
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
    val currentIsScene by rememberUpdatedState(isScene)

    // The photographic Moon: the Home Moon card's look with the true phase, libration and visibility.
    val moonSprites = rememberSkyMoonSprites()

    // Photographic panorama (Phase 1). Only active behind the internal SkyPanoramaFeature switch.
    // Until the panorama reports READY, the legacy atmosphere and procedural Milky Way below stay
    // visible as the fallback, so there is no black or empty flash. Its state is derived only when
    // the feature is enabled (see the SkyPanoramaLayer call below).
    var panoramaReady by remember { mutableStateOf(false) }
    // The panorama replaces the Real Sky background only for that theme; other themes keep their look.
    val panoramaEnabled = SkyPanoramaFeature.isEnabledFor(uiState.skyCanvasTheme)
    val panoramaActive = SkyPanoramaFeature.isPresented(uiState.skyCanvasTheme, panoramaReady)

    BoxWithConstraints(
        modifier = modifier
            // The scene is full-bleed and its tail (see SCENE TAIL below) deliberately
            // paints past the bottom edge, so the scene must not be clipped to its own
            // bounds. The card and viewer keep their squircle.
            .then(if (isScene) Modifier else Modifier.clip(squircleShape(RedCornerRadius.window)))
            .testTag("hero_sky_canvas_container")
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    val touchRadius = 36.dp.toPx()

                    // Monuments first: they occupy the ground band, below every star, so nothing in
                    // the sky can be shadowed by them. A hit raises the place's name instead of the
                    // celestial callout; a miss clears any name that was showing.
                    val framing = sceneFraming
                    if (currentIsScene && framing != null) {
                        val monumentHit = monumentHitTest(
                            tap = tapOffset,
                            framing = framing,
                            width = canvasW,
                            paddingPx = 16.dp.toPx()
                        )
                        if (monumentHit != null) {
                            selectedCelestial = null
                            selectedMonument = monumentHit
                            return@detectTapGestures
                        }
                    }
                    selectedMonument = null

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
                        val sunScreenPos = HeroSkyProjection.project(sun.azimuthDeg, sun.altitudeDeg, canvasW, canvasH, lat, horizonFraction)
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
                        val moonScreenPos = HeroSkyProjection.project(moon.azimuthDeg, moon.altitudeDeg, canvasW, canvasH, lat, horizonFraction)
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
                        val pPos = HeroSkyProjection.project(horiz.azimuthDeg, horiz.altitudeDeg, canvasW, canvasH, lat, horizonFraction)
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
                        val sPos = HeroSkyProjection.project(horiz.azimuthDeg, horiz.altitudeDeg, canvasW, canvasH, lat, horizonFraction)
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
                        selectedMonument = null
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
        if (panoramaEnabled) {
            val skyPanoramaState = remember(skyFrame) { SkyPanoramaState.fromSceneFrame(skyFrame) }
            SkyPanoramaLayer(
                state = skyPanoramaState,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(if (isScene) RectangleShape else squircleShape(RedCornerRadius.window)),
                onStatusChanged = { status -> panoramaReady = status == SkyPanoramaStatus.READY }
            )
        }

        // Combined-landscape framing for this exact scene size (see PersepolisFraming): Persepolis
        // at half the first integration's linear size, the whole group placed left and farther
        // back in the ground plane. The sky pass recomputes the same result for its
        // footprint-local grounding paint, so the stone and painted ground meet at the same toe,
        // platform edges and tower-slope front. Null unless this is the full-bleed scene on a
        // photographic theme.
        val persepolisFraming = if (isScene && PersepolisFeature.isEnabledFor(uiState.skyCanvasTheme)) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            PersepolisFraming.forScene(
                widthPx = widthPx,
                heightPx = heightPx,
                horizonYPx = heightPx * horizonFraction,
                readoutBottomPx = heightPx * horizonFraction -
                    with(density) { PersepolisFraming.READOUT_INSET_DP.dp.toPx() },
                readoutRightPx = with(density) { PersepolisFraming.READOUT_RIGHT_DP.dp.toPx() },
                readoutClearancePx = with(density) { PersepolisFraming.READOUT_CLEARANCE_DP.dp.toPx() }
            )
        } else null
        // Hand the resolved landmarks to the gesture handler above.
        SideEffect { sceneFraming = persepolisFraming }

        // --- LAYER 1: THE SKY ---
        // Atmosphere, Milky Way, stars, Sun, Moon, planets, ground. The expensive pass: it repaints on the
        // clock tick, not on every animation frame.
        Canvas(modifier = Modifier.fillMaxSize()) {
            SkySceneRenderer.drawSky(
                drawScope = this,
                frame = skyFrame,
                theme = uiState.skyCanvasTheme,
                frameTimeMs = skyTimeMs,
                panoramaActive = panoramaActive,
                target = renderTarget,
                moonSprites = moonSprites,
                bortle = uiState.bortleClass,
                foregroundVegetationInOverlay = persepolisFraming != null,
                replaceSceneMountains = persepolisFraming != null
            )
        }

        // The supplied massif is part of the photographic Home scene, behind both ruins.
        // Its foot blends into the painted field rather than covering the river and trees.
        if (persepolisFraming != null) {
            val sceneHeight = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(1f)
            HomeMountain(
                frame = skyFrame,
                toeFraction = persepolisFraming.toeYPx / sceneHeight,
                modifier = Modifier.fillMaxSize().testTag("home_mountain")
            )
        }

        // --- COMBINED LANDSCAPE: THE GROUND'S SUBJECTS ---
        // Persepolis in the left distance and Mil-e Azhdaha on its limestone hillside, as one
        // transparent GL relief set into layer 1's ground band: the ranges behind
        // them are their backdrop, the deeper field below their feet is the foreground, and the
        // localized earthwork the sky pass paints around both footprints seats them in the
        // terrain. They are lit by
        // this frame's real Sun and Moon and render on demand (per sky-model change), never on the
        // motion clock. Photographic themes only — PersepolisFeature is the gate. The relief is
        // hosted on a TextureView (see PersepolisTextureView), so it is ordinary window content:
        // layers composed after it — motion veils, in-window overlays such as the sky search —
        // cover and dim it like the painted layers, and it scrolls with the feed.
        if (persepolisFraming != null) {
            PersepolisLandscape(
                frame = skyFrame,
                framing = persepolisFraming,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("persepolis_landscape")
            )
        }

        // The procedural corridor shares the painted river's exact centerline/width.
        // Its outer banks feather into the plain rather than covering a rectangular band.
        if (persepolisFraming != null) {
            val h = with(LocalDensity.current) { maxHeight.toPx() }
            val riverY = (persepolisFraming.milSlopeFrontYPx / h + 0.062f).coerceIn(0.895f, 0.945f)
            PulvarRiver3D(
                modifier = Modifier.fillMaxSize().testTag("pulvar_river_3d"),
                sunAzimuthDeg = skyFrame.sunHoriz.azimuthDeg.toFloat(),
                sunAltitudeDeg = skyFrame.sunHoriz.altitudeDeg.toFloat(),
                moonAzimuthDeg = skyFrame.moonData.azimuthDeg.toFloat(),
                moonAltitudeDeg = skyFrame.moonData.altitudeDeg.toFloat(),
                moonIllumination = (skyFrame.moonData.illuminationPercent / 100.0).toFloat().coerceIn(0f, 1f),
                landscapeRiverY = riverY
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Preserve both existing species and their Persian-calendar seasons.
                val sunPosition = if (skyFrame.sunHoriz.altitudeDeg > -18.0) {
                    renderTarget.projectionFor(skyFrame).project(
                        skyFrame.sunHoriz.azimuthDeg, skyFrame.sunHoriz.altitudeDeg,
                        size.width, size.height, skyFrame.latitudeDeg
                    )
                } else null
                SceneGroundRenderer.drawRiverVegetation(this, skyFrame.lightingState,
                    skyFrame.jd, sunPosition != null && sunPosition.x >= size.width * 0.5f)
                // Meet the scene tail's exact edge colour, even where the near bank or a
                // pool reaches the bottom of the viewport. This is the same value the sky
                // pass's foot vignette ends on and the tail below the canvas starts from,
                // so all three agree on the canvas's last pixel row at every instant.
                val edgeColor = SceneGroundRenderer.bottomEdgeColor(
                    theme = uiState.skyCanvasTheme,
                    lighting = skyFrame.lightingState,
                    marvdasht = true // this overlay only exists for the Marvdasht scene
                )
                drawRect(Brush.verticalGradient(
                    0f to edgeColor.copy(alpha = 0f), 1f to edgeColor,
                    startY = size.height * 0.97f, endY = size.height
                ), topLeft = Offset(0f, size.height * 0.97f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.03f))
            }
        }

        // --- LAYER 2: SCENE MOTION ---
        // Drifting cloud veils and the observer on the grass line. Full-bleed scene only, and cheap enough
        // to run continuously.
        if (isScene) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                SkySceneRenderer.drawSceneMotion(
                    drawScope = this,
                    frame = skyFrame,
                    theme = uiState.skyCanvasTheme,
                    frameTimeMs = motionTimeMs
                )
            }
        }

        // --- LAYER 3: INTERACTION ---
        // Selection ring, tapped-object pill anchor and stardust: live only while the user is touching.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Tapped Celestial Target Ring Overlay (Home-only interaction layer)
            selectedCelestial?.let { sel ->
                val selPos = when {
                    sel.id == "sun" -> HeroSkyProjection.project(sunHoriz.azimuthDeg, sunHoriz.altitudeDeg, canvasW, canvasH, userLat, horizonFraction)
                    sel.id == "moon" -> HeroSkyProjection.project(moonData.azimuthDeg, moonData.altitudeDeg, canvasW, canvasH, userLat, horizonFraction)
                    sel.id.startsWith("planet_") -> {
                        val pName = sel.id.removePrefix("planet_")
                        planetPositions.find { it.first.name.equals(pName, ignoreCase = true) }?.let {
                            HeroSkyProjection.project(it.third.azimuthDeg, it.third.altitudeDeg, canvasW, canvasH, userLat, horizonFraction)
                        } ?: sel.position
                    }
                    else -> {
                        catalogStars.find { it.first.id == sel.id }?.let {
                            HeroSkyProjection.project(it.second.azimuthDeg, it.second.altitudeDeg, canvasW, canvasH, userLat, horizonFraction)
                        } ?: sel.position
                    }
                }
                // Wrapped phase: wall-clock milliseconds are too large for a meaningful Float argument to sin.
                val pulsePhase = (motionTimeMs % 4_000L).toFloat() / 4_000f * 2f * PI.toFloat()
                val pulseRing = 1.0f + 0.12f * sin(pulsePhase)
                val ringColor = when (uiState.skyCanvasTheme) {
                    SkyCanvasTheme.REAL_SKY -> Color(0xFFB4DCFF)
                    SkyCanvasTheme.ATMOSPHERIC_SKY -> Color(0xFF38BDF8)
                    SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> if (sunHoriz.altitudeDeg > 0.0) Color(0xFF18181B) else Color.White
                    SkyCanvasTheme.KIDS_WATERCOLOR -> Color(0xFFFF85A1)
                    SkyCanvasTheme.OBSERVATORY -> Color(0xFFEF4444)
                    SkyCanvasTheme.PAPERCRAFT_DIORAMA -> Color(0xFFE07A5F)
                }
                val bloom = selectionBloom.value.coerceIn(0f, 1f)
                drawCircle(
                    color = ringColor.copy(alpha = (1f - bloom) * 0.72f),
                    radius = (13.dp.toPx() + 24.dp.toPx() * bloom) * pulseRing,
                    center = selPos,
                    style = Stroke(width = (2.4f - bloom * 0.8f).dp.toPx())
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.94f),
                    radius = (12.dp.toPx() + 6.dp.toPx() * bloom),
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

        // --- SCENE TAIL (scene presentation, Home only) ---
        // The scene does not end at its bottom edge: the ground's own bottom colour
        // continues *downward and outward* below the canvas, dissolving to full
        // transparency over bottomTailHeight — the height of the Moon/eclipse card row
        // that follows the canvas on Home, so the tail passes underneath those cards.
        // Because it fades in alpha rather than toward any colour, it merges into
        // whatever backdrop is actually behind the screen — the theme background, the
        // live-sky backdrop render, a photo backdrop, or any other app backdrop — with
        // nothing per-case to adapt. The canvas itself stays crisp to its edge; nothing
        // fades upward into it. The GL monuments composite above this layer, but their
        // stone ends far above the canvas's bottom, so the tail never has stone to fade.
        if (isScene && bottomTailHeight > 0.dp) {
            // The colour the canvas's own bottom row is painted with, computed from this
            // frame's theme and lighting by the renderer that painted it. The foot vignette
            // ends on it fully opaque, so the first row of the tail is colour-identical to
            // the last row of the canvas, and the whole ramp is one colour losing alpha.
            // It therefore follows the scene at any time of day — a daylight valley's green
            // field, golden hour's warm earth, astronomical night's near-black — instead of
            // ending every frame on the same black band.
            val tailColor = SceneGroundRenderer.bottomEdgeColor(
                theme = uiState.skyCanvasTheme,
                lighting = skyFrame.lightingState,
                marvdasht = persepolisFraming != null
            )
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(bottomTailHeight)
                    .offset(y = bottomTailHeight)
            ) {
                drawRect(
                    brush = Brush.verticalGradient(
                        // An eased alpha decay: no straight-line ramp, and no stop close
                        // enough to its neighbour to band on a 10-bit display.
                        0.00f to tailColor,
                        0.08f to tailColor.copy(alpha = 0.95f),
                        0.18f to tailColor.copy(alpha = 0.84f),
                        0.30f to tailColor.copy(alpha = 0.68f),
                        0.44f to tailColor.copy(alpha = 0.50f),
                        0.58f to tailColor.copy(alpha = 0.33f),
                        0.72f to tailColor.copy(alpha = 0.19f),
                        0.85f to tailColor.copy(alpha = 0.08f),
                        1.00f to tailColor.copy(alpha = 0f)
                    ),
                    size = size
                )
            }
        }

        // --- THE SELECTION CARD (identify → detail) ---
        // Present in both Home and fullscreen: this is the affordance that turns a tap on the sky into
        // that object's page.
        //
        // It is a small, quiet card that sits *over its own subject* and points at it — not a pill
        // floating somewhere below-left of the tap. It is centred on the object's projected position, it
        // flips under the object when there is no room above it, and the wedge on its edge is aimed at the
        // object and clamped inside the card's own width, so the pointer can never drift off the contact
        // point it is supposed to mark. Dark fill, one hairline, no accent ring: the sky is the loud
        // thing here, and the only colour in the card is the object's own dot.
        AnimatedVisibility(
            visible = selectedCelestial != null,
            enter = fadeIn(tween(150)) + scaleIn(
                initialScale = 0.90f,
                transformOrigin = TransformOrigin(0.5f, 0.5f),
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.96f, animationSpec = tween(120))
        ) {
            selectedCelestial?.let { sel ->
                val canvasW = constraints.maxWidth.toFloat()
                val canvasH = constraints.maxHeight.toFloat()
                val livePos = when {
                    sel.id == "sun" -> HeroSkyProjection.project(sunHoriz.azimuthDeg, sunHoriz.altitudeDeg, canvasW, canvasH, userLat, horizonFraction)
                    sel.id == "moon" -> HeroSkyProjection.project(moonData.azimuthDeg, moonData.altitudeDeg, canvasW, canvasH, userLat, horizonFraction)
                    sel.id.startsWith("planet_") -> {
                        val pName = sel.id.removePrefix("planet_")
                        planetPositions.find { it.first.name.equals(pName, ignoreCase = true) }?.let {
                            HeroSkyProjection.project(it.third.azimuthDeg, it.third.altitudeDeg, canvasW, canvasH, userLat, horizonFraction)
                        } ?: sel.position
                    }
                    else -> {
                        catalogStars.find { it.first.id == sel.id }?.let {
                            HeroSkyProjection.project(it.second.azimuthDeg, it.second.altitudeDeg, canvasW, canvasH, userLat, horizonFraction)
                        } ?: sel.position
                    }
                }
                val accent = when (uiState.skyCanvasTheme) {
                    SkyCanvasTheme.REAL_SKY -> Color(0xFFB4DCFF)
                    SkyCanvasTheme.ATMOSPHERIC_SKY -> Color(0xFF38BDF8)
                    SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> Color.White
                    SkyCanvasTheme.KIDS_WATERCOLOR -> Color(0xFFFF85A1)
                    SkyCanvasTheme.OBSERVATORY -> Color(0xFFEF4444)
                    SkyCanvasTheme.PAPERCRAFT_DIORAMA -> Color(0xFFE07A5F)
                }

                SelectionCallout(
                    name = sel.name,
                    detail = sel.typeName ?: if (isFa) "مشاهده اطلاعات" else "View details",
                    accent = accent,
                    pinX = livePos.x,
                    pinY = livePos.y,
                    canvasWidth = canvasW,
                    canvasHeight = canvasH,
                    onOpen = {
                        val idToOpen = sel.id
                        selectedCelestial = null
                        if (idToOpen.isNotEmpty()) {
                            viewModel.openObjectDetailById(idToOpen, simulatedTimeMs)
                        }
                    }
                )
            }
        }

        // --- THE MONUMENT NAME (tap the landscape → name → page) ---
        // Persepolis and Mil-e Azhdaha are the two subjects of the ground band, so they identify
        // themselves the way the sky's objects do: the same quiet callout, pinned to the stone it
        // names, with the wedge pointing at the terrace or the tower. The only difference is where
        // the tap leads: not the celestial detail sheet, but the monument's own page. The name is
        // the button, exactly as the user asked, and it sits for seven seconds before it fades.
        AnimatedVisibility(
            visible = selectedMonument != null,
            enter = fadeIn(tween(180)) + scaleIn(
                initialScale = 0.90f,
                transformOrigin = TransformOrigin(0.5f, 0.5f),
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.96f, animationSpec = tween(140))
        ) {
            val monument = selectedMonument
            val framing = sceneFraming
            if (monument != null && framing != null) {
                val canvasW = constraints.maxWidth.toFloat()
                val canvasH = constraints.maxHeight.toFloat()
                val pin = monumentPin(framing, monument, canvasW)
                SelectionCallout(
                    name = MonumentCopy.title(monument, isFa),
                    detail = MonumentCopy.calloutDetail(monument, isFa),
                    accent = Color(0xFFC9A063),
                    pinX = pin.x,
                    pinY = pin.y,
                    canvasWidth = canvasW,
                    canvasHeight = canvasH,
                    onOpen = {
                        selectedMonument = null
                        viewModel.openMonument(monument)
                    }
                )
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

        // Only the fullscreen viewer has room for the time and date pills; the card and the scene do not.
        if (presentation == SkyCanvasPresentation.VIEWER) {
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
                    shape = RedShapes.capsule,
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
                    shape = RedShapes.capsule,
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

        // --- EXPAND AFFORDANCE: opens the fullscreen landscape sky viewer ---
        if (onExpand != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(34.dp)
                    // A dark scrim: the affordance has to read on a noon sky and on a
                    // moonless one alike, so it never takes the white card tint.
                    .solidSurface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.38f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.32f))
                    )
                    .clickable(onClick = onExpand)
                    .testTag("sky_canvas_expand_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitScreen,
                    contentDescription = if (isFa) "نمایش تمام‌صفحه آسمان" else "Open the sky fullscreen",
                    tint = Color(0xFFF8FAFF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


/**
 * The card that names a tapped object and opens it — the only piece of chrome the sky canvas shows.
 *
 * Why it is built this way:
 *  - **it belongs to the object, not to the tap.** The card is centred on the object's projected
 *    position and the wedge on its edge points *at* the object. The old pill was offset a fixed
 *    74 × 56 dp from the object, so its arrow — such as it was — pointed at nothing, and read as a
 *    label that had slid off the thing it labelled;
 *  - **it flips.** There is more sky above a low object than above a high one, so the card sits above
 *    the object with its wedge pointing down, and below it pointing up when the object is too near the
 *    top edge. Either way the wedge marks the contact point;
 *  - **it is quiet.** One dark surface at a fixed radius, one hairline, no accent-coloured ring, no
 *    arrow glyph inside. The name, a muted line saying what the object is, and the object's own colour
 *    as a 6 dp dot. The whole card is the tap target;
 *  - **it never jumps.** Its position is resolved from the card's measured size, clamped to the canvas,
 *    so a long name near an edge stays fully on screen and the pointer stays inside the card.
 */
@Composable
private fun SelectionCallout(
    name: String,
    detail: String,
    accent: Color,
    pinX: Float,
    pinY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    onOpen: () -> Unit
) {
    val density = LocalDensity.current
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val wedgeH = with(density) { 7.dp.toPx() }
    val wedgeW = with(density) { 18.dp.toPx() }
    val lift = with(density) { 13.dp.toPx() }        // gap between the wedge's tip and the object
    val edge = with(density) { 8.dp.toPx() }         // keep the card off the canvas edge
    val strokeW = with(density) { 0.7.dp.toPx() }

    val fill = Color(0xF20B0E15)
    val hairline = Color.White.copy(alpha = 0.16f)

    val w = cardSize.width.toFloat()
    val h = cardSize.height.toFloat()
    // Room above the object for the whole card plus its wedge?
    val placeAbove = pinY - lift - wedgeH - h >= edge || pinY > canvasHeight * 0.55f
    val x = (pinX - w / 2f).coerceIn(edge, (canvasWidth - edge - w).coerceAtLeast(edge))
    val y = if (placeAbove) {
        (pinY - lift - wedgeH - h).coerceIn(edge, (canvasHeight - edge - h).coerceAtLeast(edge))
    } else {
        (pinY + lift + wedgeH).coerceIn(edge, (canvasHeight - edge - h - wedgeH).coerceAtLeast(edge))
    }
    // The pointer marks the object: its centre tracks the object's x, held inside the card's corners.
    val tipX = (pinX - x).coerceIn(with(density) { 14.dp.toPx() }, (w - with(density) { 14.dp.toPx() }).coerceAtLeast(with(density) { 14.dp.toPx() }))

    // Until the card has been measured its position is a guess, so it is held invisible for the single
    // frame it takes to measure — never drawn at a guessed offset.
    val measured = cardSize.width > 0

    Box(modifier = Modifier.fillMaxSize().alpha(if (measured) 1f else 0f)) {
        // The wedge first, so the card's own edge covers its base and the two read as one shape.
        Canvas(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (x + tipX - wedgeW / 2f).roundToInt(),
                        (if (placeAbove) y + h else y - wedgeH).roundToInt()
                    )
                }
                .size(with(density) { wedgeW.toDp() }, with(density) { wedgeH.toDp() })
        ) {
            val path = Path().apply {
                moveTo(0f, if (placeAbove) 0f else size.height)
                lineTo(size.width / 2f, if (placeAbove) size.height else 0f)
                lineTo(size.width, if (placeAbove) 0f else size.height)
                close()
            }
            drawPath(path, color = fill)
            // Only the two exposed sides get the hairline.
            drawLine(
                color = hairline,
                start = Offset(0f, if (placeAbove) 0f else size.height),
                end = Offset(size.width / 2f, if (placeAbove) size.height else 0f),
                strokeWidth = strokeW
            )
            drawLine(
                color = hairline,
                start = Offset(size.width / 2f, if (placeAbove) size.height else 0f),
                end = Offset(size.width, if (placeAbove) 0f else size.height),
                strokeWidth = strokeW
            )
        }

        Surface(
            onClick = onOpen,
            shape = squircleShape(RedCornerRadius.md),
            color = fill,
            border = BorderStroke(0.7.dp, hairline),
            shadowElevation = 10.dp,
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .widthIn(max = with(density) { 210.dp })
                .onSizeChanged { cardSize = it }
                .testTag("selected_celestial_pill")
        ) {
            Row(
                modifier = Modifier.padding(start = 11.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = name,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.1.sp,
                            color = Color(0xFFF4F7FC)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = detail,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.58f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Which monument, if any, is under the finger.
 *
 * The boxes are the framing's own landmarks rather than hand-tuned percentages, so they follow the
 * stone if the scene's placement ever changes: Persepolis is the detailed façade from its far-left
 * frame to the tallest column, from that column's capital down to the platform toe; Mil-e Azhdaha is
 * the tower plus the eight metres of hillside around its base. Both get [paddingPx] of slack, because
 * a finger is wider than a carved jamb. Mil-e Azhdaha is tested first: it stands at the far right,
 * where Persepolis' forecourt never reaches.
 */
private fun monumentHitTest(
    tap: Offset,
    framing: PersepolisFraming.Result,
    width: Float,
    paddingPx: Float
): MonumentId? {
    if (width <= 0f) return null

    val towerHalfWidth = (8f * framing.pxPerMeter).coerceAtLeast(paddingPx)
    val milLeft = framing.milTowerCenterXPx - towerHalfWidth - paddingPx
    val milRight = framing.milTowerCenterXPx + towerHalfWidth + paddingPx
    val milTop = framing.milTowerTopYPx - paddingPx
    val milBottom = framing.milSlopeFrontYPx + paddingPx
    if (tap.x in milLeft..milRight && tap.y in milTop..milBottom) return MonumentId.MIL_E_AZHDAHA

    val left = framing.facadeLeftXPx - paddingPx
    val right = framing.facadeRightXPx + paddingPx
    val top = framing.tallestColumnTopYPx - paddingPx
    val bottom = framing.toeYPx + paddingPx
    if (tap.x in left..right && tap.y in top..bottom) return MonumentId.PERSEPOLIS

    return null
}

/** The point the name is pinned to: the tallest column's capital, or the tower's ruined top. */
private fun monumentPin(framing: PersepolisFraming.Result, monument: MonumentId, width: Float): Offset =
    when (monument) {
        MonumentId.PERSEPOLIS -> Offset(
            framing.tallestColumnXPx.coerceIn(24f, (width - 24f).coerceAtLeast(24f)),
            framing.tallestColumnTopYPx
        )
        MonumentId.MIL_E_AZHDAHA -> Offset(
            framing.milTowerCenterXPx.coerceIn(24f, (width - 24f).coerceAtLeast(24f)),
            framing.milTowerTopYPx
        )
    }
