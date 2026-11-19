package com.alijafari.red.astronomy.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import com.alijafari.red.astronomy.EarthViewportProjection
import com.alijafari.red.astronomy.ui.satellites.SatSlot
import com.alijafari.red.astronomy.ui.satellites.SatelliteSilhouette
import com.alijafari.red.astronomy.ui.satellites.ZigSatTokens
import com.alijafari.red.astronomy.ui.satellites.satMaterial
import com.alijafari.red.astronomy.ui.satellites.shortNameOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.Earth3D
import com.alijafari.red.astronomy.Earth3DMath
import com.alijafari.red.astronomy.EarthCameraController
import com.alijafari.red.astronomy.EarthCameraSnapshot
import com.alijafari.red.astronomy.EarthStyle
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.SatelliteLiveState
import com.alijafari.red.astronomy.astro_engine.StarlinkTrainManager
import com.alijafari.red.astronomy.astro_engine.SubSolarPoint
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.rememberEarthCameraController
import com.alijafari.red.astronomy.ui.screens.MapTheme
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Default camera distance (in Earth radii) that frames both Earth and LEO orbits comfortably. */
const val SATELLITE_EARTH_DEFAULT_DISTANCE: Float = 3.22f

/** One-shot request to smoothly orient the 3D Earth camera toward a geographic coordinate. */
@Immutable
data class EarthFocusTarget(
    val latDeg: Double,
    val lonDeg: Double,
    val distance: Float? = null,
    val requestId: Long = 0L,
    val locate: Boolean = false,
)

/** Style tokens letting Default, Spatial, and Aether screens tint orbital telemetry in their own palette. */
@Immutable
data class SatelliteEarthOverlayStyle(
    val gridColor: Color,
    val equatorColor: Color,
    val limbRimColor: Color,
    val cityLightAccent: Color,
    val userPinColor: Color,
    val userLabelColor: Color,
    val orbitTrackColor: Color,
    val orbitGlowColor: Color,
    val selectedMarkerColor: Color,
    val selectedGlowColor: Color,
    val starlinkColor: Color,
    val trainChainColor: Color,
    val trainLeaderColor: Color,
    val trainMemberColor: Color,
    val nakedEyeMarkerColor: Color,
    val defaultMarkerColor: Color,
    val popoverContainerColor: Color,
    val popoverBorderColor: Color,
    val popoverShape: Shape,
)

/**
 * Maps the Satellites screen's persisted [MapTheme] (`AUTO` / `DARK` / `LIGHT`), app [ThemeMode],
 * and astronomical [subSolarPoint] into the [EarthStyle] consumed by [Earth3D].
 */
fun buildSatelliteEarthStyle(
    mapTheme: MapTheme,
    appThemeMode: ThemeMode,
    subSolarPoint: SubSolarPoint,
    cameraDistance: Float = SATELLITE_EARTH_DEFAULT_DISTANCE,
): EarthStyle {
    val resolvedMode = when (mapTheme) {
        MapTheme.DARK -> ThemeMode.DARK
        MapTheme.LIGHT -> ThemeMode.LIGHT
        MapTheme.AUTO -> appThemeMode
    }
    return when (resolvedMode) {
        ThemeMode.LIGHT -> EarthStyle(
            showClouds = true,
            showNightLights = true,
            autoRotate = false,
            rotationSpeed = 0f,
            ambientLight = 0.34f,
            cameraDistance = cameraDistance,
            cloudOpacity = 0.44f,
            exposureEV = 0.20f,
            saturation = 1.06f,
            subSolarLatitudeDegrees = subSolarPoint.latDeg.toFloat(),
            subSolarLongitudeDegrees = subSolarPoint.lonDeg.toFloat(),
            transparentBackground = true,
        )
        ThemeMode.WINE -> EarthStyle(
            showClouds = true,
            showNightLights = true,
            autoRotate = false,
            rotationSpeed = 0f,
            ambientLight = 0.11f,
            cameraDistance = cameraDistance,
            cloudOpacity = 0.46f,
            exposureEV = 0.08f,
            saturation = 1.12f,
            subSolarLatitudeDegrees = subSolarPoint.latDeg.toFloat(),
            subSolarLongitudeDegrees = subSolarPoint.lonDeg.toFloat(),
            transparentBackground = true,
        )
        ThemeMode.DARK -> EarthStyle(
            showClouds = true,
            showNightLights = true,
            autoRotate = false,
            rotationSpeed = 0f,
            ambientLight = 0.09f,
            cameraDistance = cameraDistance,
            cloudOpacity = 0.48f,
            exposureEV = 0.06f,
            saturation = 1.08f,
            subSolarLatitudeDegrees = subSolarPoint.latDeg.toFloat(),
            subSolarLongitudeDegrees = subSolarPoint.lonDeg.toFloat(),
            transparentBackground = true,
        )
    }
}

/**
 * Pure 3D hit-test for satellite markers projected onto the viewport. Satellites occluded behind
 * Earth's far side are strictly ignored even when their 2D projection lies right under [tapOffset].
 */
internal fun findTappedSatellite3D(
    tapOffset: Offset,
    candidates: List<SatelliteLiveState>,
    camera: EarthCameraSnapshot,
    viewportWidth: Float,
    viewportHeight: Float,
    hitRadiusPx: Float,
): SatelliteLiveState? {
    return satelliteHits(tapOffset, candidates, camera, viewportWidth, viewportHeight, hitRadiusPx).firstOrNull()
}

internal fun satelliteHits(
    tap: Offset, candidates: List<SatelliteLiveState>, camera: EarthCameraSnapshot,
    width: Float, height: Float, radiusPx: Float,
): List<SatelliteLiveState> {
    if (width <= 0f || height <= 0f) return emptyList()
    return candidates.mapNotNull { state ->
        val point = Earth3DMath.projectLatLon(state.topocentric.subLatDeg, state.topocentric.subLonDeg,
            Earth3DMath.altitudeKmToRadius(state.topocentric.satAltKm), camera, width, height)
        if (!point.visible) null else hypot(tap.x - point.screenX, tap.y - point.screenY)
            .takeIf { it <= radiusPx }?.let { it to state }
    }.sortedBy { it.first }.map { it.second }
}

internal fun satelliteViewport(width: Float, height: Float, top: Float, bottom: Float): EarthViewportProjection {
    if (width <= 0 || height <= 0) return EarthViewportProjection()
    val usableBottom = bottom.coerceIn(0f, height)
    val usableTop = top.coerceIn(0f, usableBottom)
    val diameter = minOf(width * 0.92f, usableBottom - usableTop).coerceAtLeast(1f)
    val baseDiameter = Earth3DMath.projectedGlobeRadiusPx(SATELLITE_EARTH_DEFAULT_DISTANCE, height) * 2f
    return EarthViewportProjection((usableTop + usableBottom) / (2 * height), diameter / baseDiameter)
}

/**
 * Full-featured interactive 3D Earth satellite tracker combining [Earth3D]'s native GLES 2.0
 * sphere renderer (4K Blue Marble day map, topographic bump relief, real astronomical solar
 * terminator, 2K night city lights, and sunlit clouds) with every capability of the Satellites map:
 * - Touch drag to orbit, pinch/scroll/double-tap to zoom without scroll pass-through margins
 * - 3D latitude/longitude graticule & Equator line
 * - Astronomical day/night terminator & night-side city lights synced to [currentSimulationMs]
 * - User location pin & localized city label
 * - Selected satellite 3D SGP4 orbital track (past 45m + future 90m) with nadir ground tether
 * - Real-time overhead Starlinks, active "String of Pearls" Starlink train chains, and fleet markers
 * - Satellite tap selection + live-tracking anchored popover (`anchored_sat_label`) opening detail view
 * - Dark / Light / Auto / Wine map theme support
 */
@Composable
fun SatelliteEarth3D(
    currentSimulationMs: Long,
    userLocation: UserLocation,
    isFa: Boolean,
    mapTheme: MapTheme,
    appThemeMode: ThemeMode,
    selectedSatelliteId: String?,
    selectedSatItem: SatelliteItem?,
    liveSatelliteStates: List<SatelliteLiveState>,
    mapOverheadStarlinkStates: List<SatelliteLiveState>,
    trainPearlStatesByTrain: List<Pair<StarlinkTrainManager.StarlinkTrain, List<SatelliteLiveState>>>,
    allInteractiveMapStates: List<SatelliteLiveState>,
    anchoredSatLabelState: SatelliteLiveState?,
    overlayStyle: SatelliteEarthOverlayStyle,
    onSelectSatellite: (SatelliteLiveState) -> Unit,
    onDismissAnchoredLabel: () -> Unit,
    onOpenSatelliteDetail: (SatelliteItem) -> Unit,
    modifier: Modifier = Modifier,
    focusTarget: EarthFocusTarget? = null,
    onFocusConsumed: () -> Unit = {},
    framing: EarthViewportProjection = EarthViewportProjection(),
    hudTop: Dp = 72.dp,
    hudBottom: Dp = 240.dp,
    controller: EarthCameraController = rememberEarthCameraController(
        initialLatDeg = userLocation.latitude,
        initialLonDeg = userLocation.longitude,
        initialDistance = SATELLITE_EARTH_DEFAULT_DISTANCE,
    ),
    tokens: ZigSatTokens,
    reduceMotion: Boolean = false,
    drawGrid: Boolean = true,
    drawOrbit: Boolean = true,
    onGlobeGesture: () -> Unit = {},
    onCenteredChanged: (Boolean) -> Unit = {},
    popoverModifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val canvasFontFamily = LocalAppFontFamily.current
    val scope = rememberCoroutineScope()

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var activeFocusJob by remember { mutableStateOf<Job?>(null) }

    val currentGesture by rememberUpdatedState(onGlobeGesture)
    val centered by remember(controller, userLocation) { derivedStateOf {
        val (yaw, pitch) = Earth3DMath.cameraAnglesForLatLon(userLocation.latitude, userLocation.longitude)
        val snapshot = controller.snapshot
        abs(Earth3DMath.shortestAngleDeltaRad(snapshot.yawRad, yaw)) < .025f &&
            abs(snapshot.pitchRad - pitch.coerceIn(Earth3DMath.MIN_PITCH_RAD, Earth3DMath.MAX_PITCH_RAD)) < .025f &&
            abs(snapshot.distance - SATELLITE_EARTH_DEFAULT_DISTANCE) < .05f
    } }
    LaunchedEffect(centered) { onCenteredChanged(centered) }
    androidx.compose.runtime.DisposableEffect(controller) {
        controller.onUserGestureStart = {
            activeFocusJob?.cancel()
            activeFocusJob = null
            currentGesture()
        }
        onDispose { controller.onUserGestureStart = null }
    }

    fun animateCameraTo(
        targetLatDeg: Double,
        targetLonDeg: Double,
        targetDistance: Float? = null,
        locate: Boolean = false,
    ) {
        activeFocusJob?.cancel()
        val startYaw = controller.yawRad
        val startPitch = controller.pitchRad
        val startDist = controller.distance
        val (goalYaw, goalPitch) = Earth3DMath.cameraAnglesForLatLon(targetLatDeg, targetLonDeg)
        val deltaYaw = Earth3DMath.shortestAngleDeltaRad(startYaw, goalYaw)
        val deltaPitch = goalPitch - startPitch
        val endDist = targetDistance?.coerceIn(Earth3DMath.MIN_ZOOM_DISTANCE, Earth3DMath.MAX_ZOOM_DISTANCE) ?: startDist
        val markZoomed = !locate && (controller.userZoomed || targetDistance != null)
        if (reduceMotion) {
            controller.setCamera(goalYaw, goalPitch, endDist, markZoomed)
            return
        }

        activeFocusJob = scope.launch {
            val anim = Animatable(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = if (locate) 700 else 600, easing = CubicBezierEasing(0.05f, 0.65f, 0.15f, 1f)),
            ) {
                val t = value
                controller.setCamera(
                    newYawRad = startYaw + deltaYaw * t,
                    newPitchRad = startPitch + deltaPitch * t,
                    newDistance = startDist + (endDist - startDist) * t,
                    markUserZoomed = markZoomed,
                )
            }
        }
    }

    // Re-center smoothly when the user explicitly selects a new city in the location picker.
    var locationSyncCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(userLocation.latitude, userLocation.longitude) {
        if (locationSyncCount > 0) {
            animateCameraTo(userLocation.latitude, userLocation.longitude)
        }
        locationSyncCount += 1
    }

    // Re-center smoothly when a satellite card or notification requests focus on a satellite.
    LaunchedEffect(focusTarget) {
        val req = focusTarget ?: return@LaunchedEffect
        animateCameraTo(req.latDeg, req.lonDeg, req.distance, req.locate)
        onFocusConsumed()
    }

    // Astronomical sub-solar point (quantized to 10s in live mode since Earth rotates only 0.04° in 10s,
    // while keeping immediate response during ±12h time scrubbing).
    val subSolarBucket = currentSimulationMs / 10_000L
    val subSolarPoint = remember(subSolarBucket) {
        SatelliteEngine.calculateSubSolarPoint(currentSimulationMs)
    }

    val earthStyle = remember(mapTheme, appThemeMode, subSolarPoint, framing) {
        buildSatelliteEarthStyle(
            mapTheme = mapTheme,
            appThemeMode = appThemeMode,
            subSolarPoint = subSolarPoint,
            cameraDistance = SATELLITE_EARTH_DEFAULT_DISTANCE,
        ).copy(viewportProjection = framing, fullViewportGestures = true)
    }

    // Precompute smooth 3D orbital track for the selected satellite (refreshed every 15s or on scrub).
    val orbitBucket = currentSimulationMs / 15_000L
    val selectedOrbitTrack = remember(drawOrbit, selectedSatItem?.id, selectedSatItem?.noradId, orbitBucket) {
        selectedSatItem?.takeIf { drawOrbit }?.let { sat ->
            SatelliteEngine.calculateOrbitTrack3D(
                satellite = sat,
                currentTimestampMs = currentSimulationMs,
                stepSeconds = 60,
            )
        }.orEmpty()
    }

    val hitRadiusPx = with(density) { 22.dp.toPx() }
    var previousTap by remember { mutableStateOf<Offset?>(null) }
    var previousTapMs by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var tapCohort by remember { mutableStateOf(emptyList<String>()) }
    var cohortIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .onSizeChanged { viewportSize = it }
            .testTag("satellite_earth_3d"),
    ) {
        // 1. Native GLES 2.0 3D Earth Sphere (day, night city lights, topographic bump, clouds)
        Earth3D(
            modifier = Modifier.fillMaxSize(),
            style = earthStyle,
            controller = controller,
            onTap = { tapOffset ->
                val now = SystemClock.uptimeMillis()
                val cycling = tapCohort.isNotEmpty() && now - previousTapMs <= 400L &&
                    previousTap?.let { (it - tapOffset).getDistance() <= hitRadiusPx } == true
                // Retain the physical cohort selected by the first tap while the focus animation
                // moves its markers. A tap on a different part of the globe always starts over.
                val hit = if (cycling) {
                    cohortIndex = (cohortIndex + 1) % tapCohort.size
                    allInteractiveMapStates.firstOrNull { it.satellite.id == tapCohort[cohortIndex] }
                } else {
                    val hits = satelliteHits(tapOffset, allInteractiveMapStates, controller.snapshot,
                        viewportSize.width.toFloat(), viewportSize.height.toFloat(), hitRadiusPx)
                    tapCohort = hits.map { it.satellite.id }
                    cohortIndex = 0
                    hits.firstOrNull()
                }
                previousTapMs = now
                previousTap = tapOffset
                if (hit != null) {
                    onSelectSatellite(hit)
                    true
                } else {
                    onDismissAnchoredLabel()
                    false
                }
            },
        )

        // Reusable paths so frame draws stay allocation-light while dragging the globe.
        val graticulePath = remember { Path() }
        val equatorPath = remember { Path() }
        val pastOrbitPath = remember { Path() }
        val futureOrbitPath = remember { Path() }
        val trainPath = remember { Path() }

        // 2. Synchronized 3D Scientific Telemetry Overlay (orbits, satellites, trains, user pin, grid)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val cam = controller.snapshot
            val globeRadiusPx = Earth3DMath.projectedGlobeRadiusPx(cam.distance, h, cam.projection.focalScale)

            // 2a. Delicate optical limb ring so snow-capped poles stay crisp even on light themes
            if (globeRadiusPx > 4f && overlayStyle.limbRimColor.alpha > 0f) {
                drawCircle(
                    color = overlayStyle.limbRimColor,
                    radius = globeRadiusPx,
                    center = Offset(w * 0.5f, h * cam.projection.centerY),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            // 2b. Subtle 3D Lat/Lon Graticule & Equator on the visible hemisphere
            if (drawGrid) drawGraticule3D(
                cam = cam,
                w = w,
                h = h,
                gridColor = overlayStyle.gridColor,
                equatorColor = overlayStyle.equatorColor,
                gridPath = graticulePath,
                equatorPath = equatorPath,
            )

            // 2c. Subtle night-side city light highlights for catalogued world/Iranian hubs
            drawNightCityAccents3D(
                cam = cam,
                w = w,
                h = h,
                subSolarPoint = subSolarPoint,
                cityColor = overlayStyle.cityLightAccent,
            )

            // 2d. Selected Satellite 3D LEO Orbital Track (past 45m + future 90m)
            if (selectedOrbitTrack.size >= 2) {
                drawOrbitTrack3D(
                    points = selectedOrbitTrack,
                    nowMs = currentSimulationMs,
                    cam = cam,
                    w = w,
                    h = h,
                    trackColor = overlayStyle.orbitTrackColor,
                    glowColor = overlayStyle.orbitGlowColor,
                    pastPath = pastOrbitPath,
                    futurePath = futureOrbitPath,
                )
            }

            // 2e. User Location Pin & Localized City Label on Earth's surface
            val userProj = Earth3DMath.projectLatLon(
                latDeg = userLocation.latitude,
                lonDeg = userLocation.longitude,
                radius = 1.009f,
                camera = cam,
                viewportWidth = w,
                viewportHeight = h,
            )
            if (userProj.visible) {
                val fade = userProj.limbFade
                val center = Offset(userProj.screenX, userProj.screenY)
                drawCircle(
                    color = overlayStyle.userPinColor.copy(alpha = 0.30f * fade),
                    radius = 11.dp.toPx(),
                    center = center,
                )
                drawCircle(
                    color = overlayStyle.userPinColor.copy(alpha = fade),
                    radius = 3.8.dp.toPx(),
                    center = center,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f * fade),
                    radius = 1.5.dp.toPx(),
                    center = center,
                )

                val rawCity = if (isFa) userLocation.cityNameFa else userLocation.cityNameEn
                val cityLabel = if (isFa) TimeEngine.formatPersianNumbers(rawCity) else rawCity
                if (cityLabel.isNotBlank() && fade > 0.22f) {
                    val textLayout = textMeasurer.measure(
                        text = cityLabel,
                        style = TextStyle(
                            fontFamily = canvasFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = overlayStyle.userLabelColor.copy(alpha = fade),
                        ),
                    )
                    val padX = 5.dp.toPx()
                    val padY = 2.dp.toPx()
                    val pillW = textLayout.size.width + padX * 2f
                    val pillH = textLayout.size.height + padY * 2f
                    val pillLeft = (center.x - pillW * 0.5f).coerceIn(4f, (w - pillW - 4f).coerceAtLeast(4f))
                    val pillTop = (center.y - 8.dp.toPx() - pillH).coerceAtLeast(4f)
                    drawRoundRect(
                        color = Color(0x99060B16).copy(alpha = 0.58f * fade),
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH * 0.5f, pillH * 0.5f),
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(pillLeft + padX, pillTop + padY),
                    )
                }
            }

            // 2f. Real-time Overhead Individual Starlinks in 3D LEO
            for (ovState in mapOverheadStarlinkStates) {
                val sat = ovState.satellite
                val isSelected = sat.id == selectedSatelliteId
                val proj = Earth3DMath.projectLatLon(
                    latDeg = ovState.topocentric.subLatDeg,
                    lonDeg = ovState.topocentric.subLonDeg,
                    radius = Earth3DMath.altitudeKmToRadius(ovState.topocentric.satAltKm),
                    camera = cam,
                    viewportWidth = w,
                    viewportHeight = h,
                )
                if (!proj.visible) continue
                val pos = Offset(proj.screenX, proj.screenY)
                if (pos.x !in -24f..(w + 24f) || pos.y !in -24f..(h + 24f)) continue

                if (isSelected) {
                    drawNadirTether3D(
                        latDeg = ovState.topocentric.subLatDeg,
                        lonDeg = ovState.topocentric.subLonDeg,
                        satScreenPos = pos,
                        cam = cam,
                        w = w,
                        h = h,
                        color = overlayStyle.selectedMarkerColor,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(overlayStyle.selectedGlowColor, Color.Transparent),
                            center = pos,
                            radius = 14.dp.toPx(),
                        ),
                        radius = 14.dp.toPx(),
                        center = pos,
                    )
                }
                val dotColor = when {
                    isSelected -> overlayStyle.selectedMarkerColor
                    ovState.topocentric.isSunlit -> overlayStyle.starlinkColor
                    else -> overlayStyle.starlinkColor.copy(alpha = 0.48f)
                }
                drawCircle(
                    color = dotColor,
                    radius = (if (isSelected) 5.5.dp else 2.8.dp).toPx(),
                    center = pos,
                    style = if (isSelected || ovState.topocentric.isSunlit) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.2.dp.toPx()),
                )
                if (isSelected) drawCircle(Color.White, 2.dp.toPx(), pos)
            }

            // 2g. Active Starlink "String of Pearls" Train Chains in 3D LEO
            for ((_, pearls) in trainPearlStatesByTrain) {
                if (pearls.isEmpty()) continue
                trainPath.reset()
                var segmentOpen = false
                for (pState in pearls) {
                    val proj = Earth3DMath.projectLatLon(
                        latDeg = pState.topocentric.subLatDeg,
                        lonDeg = pState.topocentric.subLonDeg,
                        radius = Earth3DMath.altitudeKmToRadius(pState.topocentric.satAltKm),
                        camera = cam,
                        viewportWidth = w,
                        viewportHeight = h,
                    )
                    if (proj.visible) {
                        if (!segmentOpen) {
                            trainPath.moveTo(proj.screenX, proj.screenY)
                            segmentOpen = true
                        } else {
                            trainPath.lineTo(proj.screenX, proj.screenY)
                        }
                    } else {
                        segmentOpen = false
                    }
                }
                drawPath(
                    path = trainPath,
                    color = overlayStyle.trainChainColor,
                    style = Stroke(width = 2.0f, cap = StrokeCap.Round),
                )

                pearls.forEachIndexed { idx, pState ->
                    val proj = Earth3DMath.projectLatLon(
                        latDeg = pState.topocentric.subLatDeg,
                        lonDeg = pState.topocentric.subLonDeg,
                        radius = Earth3DMath.altitudeKmToRadius(pState.topocentric.satAltKm),
                        camera = cam,
                        viewportWidth = w,
                        viewportHeight = h,
                    )
                    if (!proj.visible) return@forEachIndexed
                    val pos = Offset(proj.screenX, proj.screenY)
                    if (pos.x !in -24f..(w + 24f) || pos.y !in -24f..(h + 24f)) return@forEachIndexed

                    val isPearlSelected = pState.satellite.id == selectedSatelliteId
                    if (isPearlSelected) {
                        drawNadirTether3D(
                            latDeg = pState.topocentric.subLatDeg,
                            lonDeg = pState.topocentric.subLonDeg,
                            satScreenPos = pos,
                            cam = cam,
                            w = w,
                            h = h,
                            color = overlayStyle.selectedMarkerColor,
                        )
                    }
                    if (idx == 0 || isPearlSelected) {
                        val haloColor = if (isPearlSelected) {
                            overlayStyle.selectedGlowColor
                        } else {
                            overlayStyle.trainChainColor.copy(alpha = 0.55f)
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(haloColor, Color.Transparent),
                                center = pos,
                                radius = 11.dp.toPx(),
                            ),
                            radius = 11.dp.toPx(),
                            center = pos,
                        )
                    }
                    drawCircle(
                        color = when {
                            isPearlSelected -> overlayStyle.selectedMarkerColor
                            idx == 0 -> overlayStyle.trainLeaderColor
                            else -> overlayStyle.trainMemberColor
                        },
                        radius = (if (isPearlSelected) 5.5.dp else if (idx == 0) 4.dp else 2.5.dp).toPx(),
                        center = pos,
                    )
                    if (isPearlSelected) drawCircle(Color.White, 2.dp.toPx(), pos)
                }
            }

            // 2h. Static Catalog & Curated Fleet Satellite Markers (ISS, Tiangong, Hubble, leaders)
            for (liveState in liveSatelliteStates) {
                val sat = liveState.satellite
                if (mapOverheadStarlinkStates.any { it.satellite.id == sat.id } ||
                    sat.isTrain && trainPearlStatesByTrain.any { it.first.leaderItem.id == sat.id }) continue
                val isSelected = sat.id == selectedSatelliteId
                val proj = Earth3DMath.projectLatLon(
                    latDeg = liveState.topocentric.subLatDeg,
                    lonDeg = liveState.topocentric.subLonDeg,
                    radius = Earth3DMath.altitudeKmToRadius(liveState.topocentric.satAltKm),
                    camera = cam,
                    viewportWidth = w,
                    viewportHeight = h,
                )
                if (!proj.visible) continue
                val pos = Offset(proj.screenX, proj.screenY)
                if (pos.x !in -24f..(w + 24f) || pos.y !in -24f..(h + 24f)) continue

                val markerColor = when {
                    isSelected -> overlayStyle.selectedMarkerColor
                    sat.isTrain -> overlayStyle.trainMemberColor
                    sat.category == SatelliteCategory.STARLINK -> overlayStyle.starlinkColor
                    sat.isNakedEyeCandidate -> overlayStyle.nakedEyeMarkerColor
                    else -> overlayStyle.defaultMarkerColor
                }

                if (isSelected) {
                    drawNadirTether3D(
                        latDeg = liveState.topocentric.subLatDeg,
                        lonDeg = liveState.topocentric.subLonDeg,
                        satScreenPos = pos,
                        cam = cam,
                        w = w,
                        h = h,
                        color = overlayStyle.selectedMarkerColor,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(overlayStyle.selectedGlowColor, Color.Transparent),
                            center = pos,
                            radius = 16.dp.toPx(),
                        ),
                        radius = 16.dp.toPx(),
                        center = pos,
                    )
                } else if (sat.isNakedEyeCandidate && sat.category != SatelliteCategory.STARLINK) {
                    drawCircle(
                        color = markerColor.copy(alpha = 0.26f),
                        radius = 8.dp.toPx(),
                        center = pos,
                    )
                }

                drawSatelliteGlyph(
                    center = pos,
                    satellite = sat,
                    color = markerColor,
                    radiusPx = (if (isSelected) 4.3.dp else if (sat.isNakedEyeCandidate) 3.6.dp else 3.dp).toPx(),
                )
                if (isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = pos,
                    )
                }
            }
        }

        // HUD only: the projection and physical tail placement are never mirrored.
        val anchor = anchoredSatLabelState?.let { selected ->
            allInteractiveMapStates.firstOrNull { it.satellite.id == selected.satellite.id } ?: selected
        }
        val cam = controller.snapshot
        val position = anchor?.let {
            Earth3DMath.projectLatLon(it.topocentric.subLatDeg, it.topocentric.subLonDeg,
                Earth3DMath.altitudeKmToRadius(it.topocentric.satAltKm), cam,
                viewportSize.width.toFloat(), viewportSize.height.toFloat())
        }
        val limb = Earth3DMath.projectedGlobeRadiusPx(cam.distance, viewportSize.height.toFloat(), cam.projection.focalScale)
        val edgeDistance = position?.let { hypot(it.screenX - viewportSize.width * .5f,
            it.screenY - viewportSize.height * cam.projection.centerY) } ?: 0f
        val visible = anchor != null && position?.visible == true && edgeDistance <= limb * 1.06f
        var lastDisplay by remember { mutableStateOf<Pair<SatelliteLiveState, Offset>?>(null) }
        androidx.compose.runtime.SideEffect {
            if (visible && anchor != null && position != null) lastDisplay = anchor to Offset(position.screenX, position.screenY)
        }
        val display = if (visible && anchor != null && position != null) anchor to Offset(position.screenX, position.screenY) else lastDisplay
        if (display != null) {
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            val shape = overlayStyle.popoverShape
            val width = with(density) { viewportSize.width.toDp() }
            val popupWidth = (width * 0.49f).coerceIn(148.dp, 232.dp)
            val popupHeight = 48.dp
            val point = display.second
            val tail = 6.dp
            val margin = 12.dp
            val railReserve = 64.dp
            val (x, top, tailX, above) = with(density) {
                val safeStart = if (rtl) railReserve else margin
                val safeEnd = if (rtl) margin else railReserve
                val left = (point.x.toDp() - popupWidth / 2).coerceIn(safeStart, (width - safeEnd - popupWidth).coerceAtLeast(safeStart))
                val bottom = viewportSize.height.toDp() - hudBottom - popupHeight - tail
                val placeAbove = point.y.toDp() - popupHeight - tail >= hudTop
                val y = if (placeAbove) point.y.toDp() - popupHeight - tail else point.y.toDp() + tail
                PopoverPosition(left, y.coerceIn(hudTop, bottom.coerceAtLeast(hudTop)),
                    (point.x.toDp() - left).coerceIn(12.dp, popupWidth - 12.dp), placeAbove)
            }
            AnimatedVisibility(visible = visible,
                modifier = Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(x, top).width(popupWidth),
                enter = if (reduceMotion) fadeIn(tween(110)) else fadeIn(tween(160)) +
                    scaleIn(tween(160), initialScale = .92f, transformOrigin = TransformOrigin(tailX / popupWidth, if (above) 1f else 0f)),
                exit = fadeOut(tween(120))) {
                Box(Modifier.heightIn(min = popupHeight)) {
                    Canvas(Modifier.matchParentSize()) {
                        val y = if (above) size.height - 1.dp.toPx() else 1.dp.toPx()
                        val dir = if (above) 1 else -1
                        val cx = tailX.toPx()
                        val path = Path().apply {
                            moveTo(cx - tail.toPx(), y)
                            lineTo(cx, y + dir * tail.toPx())
                            lineTo(cx + tail.toPx(), y)
                            close()
                        }
                        drawPath(path, overlayStyle.popoverContainerColor)
                        drawPath(path, overlayStyle.popoverBorderColor, style = Stroke(1.dp.toPx()))
                    }
                    Row(popoverModifier.fillMaxWidth().heightIn(min = popupHeight)
                        .satMaterial(tokens, SatSlot.Popover, shape, overlayStyle.popoverContainerColor, overlayStyle.popoverBorderColor)
                        .clickable { onOpenSatelliteDetail(display.first.satellite) }.testTag("anchored_sat_label")
                        .padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SatelliteSilhouette(display.first.satellite, Color.White, Modifier.size(18.dp))
                        Text(shortNameOf(display.first.satellite, isFa), Modifier.weight(1f), color = Color.White,
                            style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!display.first.topocentric.isSunlit) Icon(Icons.Filled.DarkMode, null, Modifier.size(12.dp), tint = Color.White)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(14.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}

private data class PopoverPosition(val x: Dp, val top: Dp, val tailX: Dp, val above: Boolean)

private fun DrawScope.drawGraticule3D(
    cam: EarthCameraSnapshot,
    w: Float,
    h: Float,
    gridColor: Color,
    equatorColor: Color,
    gridPath: Path,
    equatorPath: Path,
) {
    val gridRadius = 1.003f
    if (gridColor.alpha > 0f) {
        gridPath.reset()
        // Meridians every 30°
        for (lon in -180 until 180 step 30) {
            var segmentOpen = false
            var lat = -78
            while (lat <= 78) {
                val p = Earth3DMath.projectLatLon(lat.toDouble(), lon.toDouble(), gridRadius, cam, w, h)
                if (p.visible && p.limbFade > 0.08f) {
                    if (!segmentOpen) {
                        gridPath.moveTo(p.screenX, p.screenY)
                        segmentOpen = true
                    } else {
                        gridPath.lineTo(p.screenX, p.screenY)
                    }
                } else {
                    segmentOpen = false
                }
                lat += 6
            }
        }
        // Parallels at ±30° and ±60°
        for (lat in intArrayOf(-60, -30, 30, 60)) {
            var segmentOpen = false
            var lon = -180
            while (lon <= 180) {
                val p = Earth3DMath.projectLatLon(lat.toDouble(), lon.toDouble(), gridRadius, cam, w, h)
                if (p.visible && p.limbFade > 0.08f) {
                    if (!segmentOpen) {
                        gridPath.moveTo(p.screenX, p.screenY)
                        segmentOpen = true
                    } else {
                        gridPath.lineTo(p.screenX, p.screenY)
                    }
                } else {
                    segmentOpen = false
                }
                lon += 6
            }
        }
        drawPath(path = gridPath, color = gridColor, style = Stroke(width = 0.8f))
    }

    if (equatorColor.alpha > 0f) {
        equatorPath.reset()
        var segmentOpen = false
        var lon = -180
        while (lon <= 180) {
            val p = Earth3DMath.projectLatLon(0.0, lon.toDouble(), gridRadius, cam, w, h)
            if (p.visible && p.limbFade > 0.05f) {
                if (!segmentOpen) {
                    equatorPath.moveTo(p.screenX, p.screenY)
                    segmentOpen = true
                } else {
                    equatorPath.lineTo(p.screenX, p.screenY)
                }
            } else {
                segmentOpen = false
            }
            lon += 5
        }
        drawPath(path = equatorPath, color = equatorColor, style = Stroke(width = 1.25f))
    }
}

private fun DrawScope.drawNightCityAccents3D(
    cam: EarthCameraSnapshot,
    w: Float,
    h: Float,
    subSolarPoint: SubSolarPoint,
    cityColor: Color,
) {
    if (cityColor.alpha <= 0f) return
    val subLatRad = Math.toRadians(subSolarPoint.latDeg)
    val sinSubLat = sin(subLatRad)
    val cosSubLat = cos(subLatRad)
    val zoomBoost = (SATELLITE_EARTH_DEFAULT_DISTANCE / cam.distance).coerceIn(0.75f, 1.55f)

    for (city in SatelliteEngine.majorCityLights) {
        val cLatRad = Math.toRadians(city.lat)
        val dLonRad = Math.toRadians(city.lon - subSolarPoint.lonDeg)
        val cosPsi = sin(cLatRad) * sinSubLat + cos(cLatRad) * cosSubLat * cos(dLonRad)
        if (cosPsi >= -0.02) continue

        val proj = Earth3DMath.projectLatLon(city.lat, city.lon, 1.004f, cam, w, h)
        if (!proj.visible) continue
        val nightStrength = ((-cosPsi - 0.02) / 0.16).coerceIn(0.0, 1.0).toFloat()
        val alpha = (cityColor.alpha * nightStrength * proj.limbFade).coerceIn(0f, 1f)
        if (alpha <= 0.01f) continue

        drawCircle(
            color = cityColor.copy(alpha = alpha),
            radius = city.sizeDp * 0.42f * zoomBoost,
            center = Offset(proj.screenX, proj.screenY),
        )
    }
}

private fun DrawScope.drawOrbitTrack3D(
    points: List<SatelliteEngine.OrbitTrackPoint>,
    nowMs: Long,
    cam: EarthCameraSnapshot,
    w: Float,
    h: Float,
    trackColor: Color,
    glowColor: Color,
    pastPath: Path,
    futurePath: Path,
) {
    pastPath.reset()
    futurePath.reset()
    var pastOpen = false
    var futureOpen = false
    var futureVisibleCount = 0
    var arrowTail: Offset? = null
    var arrowTip: Offset? = null

    for (pt in points) {
        val proj = Earth3DMath.projectLatLon(
            latDeg = pt.latDeg,
            lonDeg = pt.lonDeg,
            radius = Earth3DMath.altitudeKmToRadius(pt.altKm),
            camera = cam,
            viewportWidth = w,
            viewportHeight = h,
        )
        val isPast = pt.timestampMs <= nowMs
        val isFuture = pt.timestampMs >= nowMs

        if (isPast) {
            if (proj.visible) {
                if (!pastOpen) {
                    pastPath.moveTo(proj.screenX, proj.screenY)
                    pastOpen = true
                } else {
                    pastPath.lineTo(proj.screenX, proj.screenY)
                }
            } else {
                pastOpen = false
            }
        }
        if (isFuture) {
            if (proj.visible) {
                if (!futureOpen) {
                    futurePath.moveTo(proj.screenX, proj.screenY)
                    futureOpen = true
                    if (futureVisibleCount < 8) {
                        futureVisibleCount = 0
                        arrowTail = null
                        arrowTip = null
                    }
                } else {
                    futurePath.lineTo(proj.screenX, proj.screenY)
                }
                futureVisibleCount++
                if (futureVisibleCount == 4) arrowTail = Offset(proj.screenX, proj.screenY)
                if (futureVisibleCount == 8) arrowTip = Offset(proj.screenX, proj.screenY)
            } else {
                futureOpen = false
            }
        }
    }

    // Past 45-minute orbital wake
    drawPath(
        path = pastPath,
        color = trackColor.copy(alpha = 0.50f),
        style = Stroke(
            width = 1.9f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f)),
        ),
    )

    // Future 90-minute orbital trajectory with soft glow + crisp dashed line
    if (glowColor.alpha > 0f) {
        drawPath(
            path = futurePath,
            color = glowColor,
            style = Stroke(width = 5.5f, cap = StrokeCap.Round),
        )
    }
    drawPath(
        path = futurePath,
        color = trackColor,
        style = Stroke(
            width = 2.4f,
            cap = StrokeCap.Round,

        ),
    )
    if (arrowTail != null && arrowTip != null) drawDirectionArrow(arrowTail!!, arrowTip!!, trackColor)
}

private fun DrawScope.drawDirectionArrow(from: Offset, tip: Offset, color: Color) {
    val dx = tip.x - from.x
    val dy = tip.y - from.y
    val length = hypot(dx, dy)
    if (length < 2f) return
    val ux = dx / length
    val uy = dy / length
    val head = 5.dp.toPx()
    val wingA = Offset(tip.x - head * (ux * .82f - uy * .58f), tip.y - head * (uy * .82f + ux * .58f))
    val wingB = Offset(tip.x - head * (ux * .82f + uy * .58f), tip.y - head * (uy * .82f - ux * .58f))
    drawLine(color, tip, wingA, 1.6.dp.toPx(), StrokeCap.Round)
    drawLine(color, tip, wingB, 1.6.dp.toPx(), StrokeCap.Round)
}

/** Draw distinct tiny spacecraft silhouettes in screen space. */
private fun DrawScope.drawSatelliteGlyph(center: Offset, satellite: SatelliteItem, color: Color, radiusPx: Float) {
    val u = radiusPx.coerceAtLeast(1f)
    fun p(x: Float, y: Float) = Offset(center.x + x * u, center.y + y * u)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = .20f) =
        drawLine(color, p(x1, y1), p(x2, y2), u * width, StrokeCap.Round)
    fun panel(x1: Float, y1: Float, x2: Float, y2: Float) {
        val left = minOf(x1, x2); val top = minOf(y1, y2)
        val rectSize = Size(kotlin.math.abs(x2 - x1) * u, kotlin.math.abs(y2 - y1) * u)
        drawRect(color.copy(alpha = .82f), p(left, top), rectSize)
        line((x1 + x2) * .5f, y1, (x1 + x2) * .5f, y2, .07f)
        line(x1, (y1 + y2) * .5f, x2, (y1 + y2) * .5f, .07f)
    }
    when {
        satellite.category == SatelliteCategory.ISS || satellite.id == "iss_zarya" -> {
            line(-1.8f, 0f, 1.8f, 0f, .28f)
            panel(-1.65f, -.85f, -.72f, -.2f); panel(-1.65f, .2f, -.72f, .85f)
            panel(.72f, -.85f, 1.65f, -.2f); panel(.72f, .2f, 1.65f, .85f)
            drawRoundRect(color, p(-.35f, -.42f), Size(.7f * u, .84f * u), CornerRadius(.16f * u))
        }
        satellite.category == SatelliteCategory.HUBBLE || satellite.id.contains("hubble", true) -> {
            panel(-1.8f, -.58f, -.95f, .58f); panel(.95f, -.58f, 1.8f, .58f)
            line(-1f, 0f, 1f, 0f, .3f)
            drawRoundRect(color, p(-.45f, -.65f), Size(.9f * u, 1.3f * u), CornerRadius(.18f * u))
            drawCircle(color.copy(alpha = .65f), .3f * u, p(.72f, 0f))
        }
        satellite.id.contains("tiangong", true) -> {
            panel(-1.8f, -.7f, -1.0f, .1f); panel(1.0f, -.7f, 1.8f, .1f)
            line(-1.1f, -.3f, 1.1f, -.3f, .22f)
            drawRoundRect(color, p(-.3f, -.9f), Size(.6f * u, 1.6f * u), CornerRadius(.16f * u))
            drawRoundRect(color, p(-.85f, -.2f), Size(1.7f * u, .55f * u), CornerRadius(.15f * u))
        }
        satellite.category == SatelliteCategory.EARTH_OBSERVATION || satellite.id.contains("envisat", true) -> {
            panel(.30f, -.72f, 1.8f, .72f)
            line(.05f, 0f, .42f, 0f, .22f)
            drawRect(color, p(-.72f, -.52f), Size(.78f * u, 1.04f * u))
            line(-.55f, -.52f, -.55f, -.92f, .10f)
        }
        else -> {
            // Starlink flatbus with a single broad solar-array wing; identical for the fleet.
            panel(-1.8f, -.66f, -.18f, .66f)
            line(-.22f, 0f, .20f, 0f, .22f)
            drawRoundRect(color, p(.20f, -.48f), Size(1.05f * u, .96f * u), CornerRadius(.1f * u))
        }
    }
}

private fun DrawScope.drawNadirTether3D(
    latDeg: Double,
    lonDeg: Double,
    satScreenPos: Offset,
    cam: EarthCameraSnapshot,
    w: Float,
    h: Float,
    color: Color,
) {
    val surface = Earth3DMath.projectLatLon(latDeg, lonDeg, 1.004f, cam, w, h)
    if (!surface.visible) return
    val groundPos = Offset(surface.screenX, surface.screenY)
    if (hypot(satScreenPos.x - groundPos.x, satScreenPos.y - groundPos.y) > 2f) {
        drawLine(
            color = color.copy(alpha = 0.52f * surface.limbFade),
            start = groundPos,
            end = satScreenPos,
            strokeWidth = 1.2.dp.toPx(),
        )
    }
    drawCircle(
        color = color.copy(alpha = 0.58f * surface.limbFade),
        radius = 2.6.dp.toPx(),
        center = groundPos,
        style = Stroke(width = 1.dp.toPx()),
    )
}
