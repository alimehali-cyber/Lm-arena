package com.alijafari.red.astronomy.ui.components

import com.alijafari.red.astronomy.ui.satellites.*
import com.alijafari.red.astronomy.ui.satellites.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
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
    // Lighting follows the Sun, not the HUD palette. Forced modes alter exposure only.
    return EarthStyle(showClouds=true,showNightLights=true,autoRotate=false,rotationSpeed=0f,
        ambientLight=when(mapTheme){MapTheme.AUTO -> .18f;MapTheme.LIGHT -> .34f;MapTheme.DARK -> .09f},
        cameraDistance=cameraDistance,cloudOpacity=.44f,exposureEV=if(mapTheme==MapTheme.LIGHT).2f else .06f,
        saturation=1f,subSolarLatitudeDegrees=subSolarPoint.latDeg.toFloat(),subSolarLongitudeDegrees=subSolarPoint.lonDeg.toFloat(),transparentBackground=true)
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
internal fun SatelliteEarth3D(
    currentSimulationMs: Long,
    userLocation: UserLocation,
    isFa: Boolean,
    mapTheme: MapTheme,
    appThemeMode: ThemeMode,
    selectedSatelliteId: String?,
    selectedSatItem: SatelliteItem?,
    candidates: List<SatelliteLiveState>, members: Set<String>?, favorites: Set<String>, layers: Set<MapLayer>,
    trainMembers: List<List<String>>,
    viewport: GlobeViewport, activePass: com.alijafari.red.astronomy.astro_engine.ISSEngine.ISSPass?,
    overlayStyle: SatelliteEarthOverlayStyle,
    onSelectSatellite: (SatelliteLiveState) -> Unit,
    onDismissAnchoredLabel: () -> Unit,
    onOpenSatelliteDetail: (SatelliteItem) -> Unit,
    modifier: Modifier = Modifier,
    focusTarget: EarthFocusTarget? = null,
    onFocusConsumed: () -> Unit = {},
    controller: EarthCameraController = rememberEarthCameraController(
        initialLatDeg = userLocation.latitude,
        initialLonDeg = userLocation.longitude,
        initialDistance = SATELLITE_EARTH_DEFAULT_DISTANCE,
    ),
    tokens: ZigSatTokens,
    reduceMotion: Boolean = false,
    onGlobeGesture: (Boolean) -> Unit = {},
    onCenteredChanged: (Boolean) -> Unit = {},
    popoverModifier: Modifier = Modifier,
    showSelectedLabel: Boolean = true,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val canvasFontFamily = LocalAppFontFamily.current
    val scope = rememberCoroutineScope()

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var activeFocusJob by remember { mutableStateOf<Job?>(null) }

    val currentGesture by rememberUpdatedState(onGlobeGesture)
    var wasCentered by remember { mutableStateOf(true) }
    val centered by remember(controller, userLocation) { derivedStateOf {
        val (yaw, pitch) = Earth3DMath.cameraAnglesForLatLon(userLocation.latitude, userLocation.longitude)
        val snapshot = controller.snapshot
        abs(Earth3DMath.shortestAngleDeltaRad(snapshot.yawRad, yaw)) < (if(wasCentered) .035f else .018f) &&
            abs(snapshot.pitchRad - pitch.coerceIn(Earth3DMath.MIN_PITCH_RAD, Earth3DMath.MAX_PITCH_RAD)) < (if(wasCentered) .035f else .018f) &&
            abs(snapshot.distance - SATELLITE_EARTH_DEFAULT_DISTANCE) < (if(wasCentered) .06f else .03f)
    } }
    LaunchedEffect(centered) { wasCentered=centered;onCenteredChanged(centered) }
    androidx.compose.runtime.DisposableEffect(controller) {
        controller.onUserGestureStart = {
            activeFocusJob?.cancel()
            activeFocusJob = null
            currentGesture(true)
        }
        controller.onUserGestureEnd = { currentGesture(false) }
        onDispose { controller.onUserGestureStart = null;controller.onUserGestureEnd=null;currentGesture(false) }
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
                animationSpec = tween(durationMillis = if (locate) 700 else 320, easing = CubicBezierEasing(0.05f, 0.65f, 0.15f, 1f)),
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

    val cam=controller.snapshot
    val frame=viewport.layout.frameAtDistance(SATELLITE_EARTH_DEFAULT_DISTANCE,cam.distance)
    val framing=EarthViewportProjection(frame.center.y/viewport.height.coerceAtLeast(1f),frame.diameter.coerceAtLeast(.01f)/(2*Earth3DMath.projectedGlobeRadiusPx(cam.distance,viewport.height.coerceAtLeast(1f))))
    val earthStyle = remember(mapTheme, appThemeMode, subSolarPoint, framing, layers) {
        buildSatelliteEarthStyle(
            mapTheme = mapTheme,
            appThemeMode = appThemeMode,
            subSolarPoint = subSolarPoint,
            cameraDistance = SATELLITE_EARTH_DEFAULT_DISTANCE,
        ).copy(viewportProjection = framing, fullViewportGestures = true, showNightLights=MapLayer.CITIES in layers)
    }

    val allInteractiveMapStates=remember(candidates,members,favorites,selectedSatelliteId,layers) {
        priorityFleet(candidates.filter { MapLayer.TRAIN in layers || !it.satellite.isTrain || it.satellite.id==selectedSatelliteId },selectedSatelliteId,members,favorites)
    }
    val fleetById = remember(allInteractiveMapStates) { allInteractiveMapStates.associateBy { it.satellite.id } }
    val orbitBucket=currentSimulationMs/15_000
    val track by produceState<Trail?>(null,selectedSatItem,orbitBucket,activePass) {
        val sat=selectedSatItem ?: return@produceState
        value=withContext(Dispatchers.Default) { sampleTrail(sat,orbitBucket*15_000,activePass,sgp4Ephemeris(sat)) }
    }
    // The 10s/30s samples are tied to the live tick rather than the 15s path bucket. Propagation
    // stays off the UI thread, including the optional member tails.
    val liveTrack by produceState<Trail?>(null, track, selectedSatItem, currentSimulationMs) {
        val baseTrack = track
        val sat = selectedSatItem
        value = if (baseTrack == null || sat == null) null else withContext(Dispatchers.Default) {
            val ephemeris = sgp4Ephemeris(sat)
            baseTrack.copy(later = ephemeris.at(currentSimulationMs + 10_000),
                longer = ephemeris.at(currentSimulationMs + 30_000))
        }
    }
    val tailCandidates = remember(orbitBucket, members, selectedSatelliteId, allInteractiveMapStates.map { it.satellite.id }) {
        if (members == null) emptyList() else allInteractiveMapStates
            .filter { it.satellite.id != selectedSatelliteId && it.satellite.id in members }
            .sortedBy { it.topocentric.rangeKm }.take(12)
    }
    val tails by produceState(emptyMap<String, Geo>(), orbitBucket, tailCandidates) {
        value = withContext(Dispatchers.Default) {
            tailCandidates.associate { it.satellite.id to sgp4Ephemeris(it.satellite).at(currentSimulationMs - 30_000) }
        }
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
            drawGraticule3D(
                cam = cam,
                w = w,
                h = h,
                gridColor = if(MapLayer.GRID in layers)overlayStyle.gridColor else Color.Transparent,
                equatorColor = if(MapLayer.EQUATOR in layers)overlayStyle.equatorColor else Color.Transparent,
                gridPath = graticulePath,
                equatorPath = equatorPath,
            )

            // 2c. Subtle night-side city light highlights for catalogued world/Iranian hubs
            if(MapLayer.CITIES in layers) drawNightCityAccents3D(
                cam = cam,
                w = w,
                h = h,
                subSolarPoint = subSolarPoint,
                cityColor = overlayStyle.cityLightAccent,
            )

            // 2e. User Location Pin & Localized City Label on Earth's surface
            val userProj = Earth3DMath.projectLatLon(
                latDeg = userLocation.latitude,
                lonDeg = userLocation.longitude,
                radius = 1.009f,
                camera = cam,
                viewportWidth = w,
                viewportHeight = h,
            )
            if (MapLayer.OBSERVER in layers && userProj.visible) {
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

            // Preserve each detected train's physical member order. The 60-marker budget can
            // omit members; a missing or occluded member breaks the line rather than inventing a
            // connection across the gap or through the Earth.
            if (MapLayer.TRAIN in layers) trainMembers.forEach { ids ->
                trainPath.reset()
                var open = false
                ids.forEach { id ->
                    val topo = fleetById[id]?.topocentric
                    val p = topo?.let { Earth3DMath.projectLatLon(it.subLatDeg, it.subLonDeg,
                        Earth3DMath.altitudeKmToRadius(it.satAltKm), cam, w, h) }
                    if (p == null || !p.visible) open = false else {
                        if (open) trainPath.lineTo(p.screenX, p.screenY) else trainPath.moveTo(p.screenX, p.screenY)
                        open = true
                    }
                }
                drawPath(trainPath, overlayStyle.trainChainColor, style = Stroke(width = 1.dp.toPx()))
            }

            if(MapLayer.TETHER in layers) allInteractiveMapStates.firstOrNull { it.satellite.id==selectedSatelliteId }?.let { selected ->
                val topo=selected.topocentric
                val p=Earth3DMath.projectLatLon(topo.subLatDeg,topo.subLonDeg,Earth3DMath.altitudeKmToRadius(topo.satAltKm),cam,w,h)
                if(p.visible)drawNadirTether3D(topo.subLatDeg,topo.subLonDeg,Offset(p.screenX,p.screenY),cam,w,h,tokens.inkSecondary)
            }
        }
        OrbitOverlay(viewport,Camera(cam.yawRad,cam.pitchRad,cam.distance),frame,allInteractiveMapStates,selectedSatelliteId,
            members,liveTrack,tails,MapLayer.ORBIT in layers,tokens,isFa,onOpenSatelliteDetail,popoverModifier,showSelectedLabel)
    }
}

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
