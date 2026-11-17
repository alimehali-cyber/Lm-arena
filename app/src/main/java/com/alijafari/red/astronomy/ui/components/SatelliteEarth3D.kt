package com.alijafari.red.astronomy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Default camera distance (in Earth radii) that frames both Earth and LEO orbits comfortably. */
const val SATELLITE_EARTH_DEFAULT_DISTANCE: Float = 3.22f

/** Full-bleed viewport aspect ratio for the interactive 3D satellite Earth across all UI styles. */
const val SATELLITE_EARTH_ASPECT_RATIO: Float = 1.16f

/** One-shot request to smoothly orient the 3D Earth camera toward a geographic coordinate. */
@Immutable
data class EarthFocusTarget(
    val latDeg: Double,
    val lonDeg: Double,
    val distance: Float? = null,
    val requestId: Long = 0L,
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
    val resetButtonContainerColor: Color,
    val popoverContainerColor: Color,
    val popoverBorderColor: Color,
    val popoverAccentColor: Color,
    val popoverShape: Shape,
    val showPopoverLeadingIcon: Boolean = true,
    val altPrefixEn: String = "Alt ",
    val altPrefixFa: String = "ارتفاع ",
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
    if (viewportWidth <= 0f || viewportHeight <= 0f || candidates.isEmpty()) return null
    var closest: SatelliteLiveState? = null
    var minDist = hitRadiusPx
    for (satState in candidates) {
        val radius = Earth3DMath.altitudeKmToRadius(satState.topocentric.satAltKm)
        val projected = Earth3DMath.projectLatLon(
            latDeg = satState.topocentric.subLatDeg,
            lonDeg = satState.topocentric.subLonDeg,
            radius = radius,
            camera = camera,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )
        if (!projected.visible) continue
        val dist = hypot(tapOffset.x - projected.screenX, tapOffset.y - projected.screenY)
        if (dist < minDist) {
            minDist = dist
            closest = satState
        }
    }
    return closest
}

/**
 * Full-featured interactive 3D Earth satellite tracker combining [Earth3D]'s native GLES 2.0
 * sphere renderer (4K Blue Marble day map, topographic bump relief, real astronomical solar
 * terminator, 2K night city lights, and sunlit clouds) with every capability of the Satellites map:
 * - Touch drag to orbit, pinch/scroll/double-tap to zoom, and top-right reset view FAB (`map_reset_view`)
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
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val canvasFontFamily = LocalAppFontFamily.current
    val scope = rememberCoroutineScope()

    val controller: EarthCameraController = rememberEarthCameraController(
        initialLatDeg = userLocation.latitude,
        initialLonDeg = userLocation.longitude,
        initialDistance = SATELLITE_EARTH_DEFAULT_DISTANCE,
    )

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var activeFocusJob by remember { mutableStateOf<Job?>(null) }

    // Cancel any programmatic focus transition the instant the user touches or zooms the globe.
    LaunchedEffect(controller) {
        controller.onUserGestureStart = {
            activeFocusJob?.cancel()
            activeFocusJob = null
        }
    }

    fun animateCameraTo(
        targetLatDeg: Double,
        targetLonDeg: Double,
        targetDistance: Float? = null,
        resetUserZoom: Boolean = false,
    ) {
        activeFocusJob?.cancel()
        val startYaw = controller.yawRad
        val startPitch = controller.pitchRad
        val startDist = controller.distance
        val (goalYaw, goalPitch) = Earth3DMath.cameraAnglesForLatLon(targetLatDeg, targetLonDeg)
        val deltaYaw = Earth3DMath.shortestAngleDeltaRad(startYaw, goalYaw)
        val deltaPitch = goalPitch - startPitch
        val endDist = targetDistance?.coerceIn(Earth3DMath.MIN_ZOOM_DISTANCE, Earth3DMath.MAX_ZOOM_DISTANCE) ?: startDist
        val markZoomed = if (resetUserZoom) false else controller.userZoomed

        activeFocusJob = scope.launch {
            val anim = Animatable(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing),
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
        animateCameraTo(req.latDeg, req.lonDeg, req.distance)
    }

    // Astronomical sub-solar point (quantized to 10s in live mode since Earth rotates only 0.04° in 10s,
    // while keeping immediate response during ±12h time scrubbing).
    val subSolarBucket = currentSimulationMs / 10_000L
    val subSolarPoint = remember(subSolarBucket) {
        SatelliteEngine.calculateSubSolarPoint(currentSimulationMs)
    }

    val earthStyle = remember(mapTheme, appThemeMode, subSolarPoint) {
        buildSatelliteEarthStyle(
            mapTheme = mapTheme,
            appThemeMode = appThemeMode,
            subSolarPoint = subSolarPoint,
            cameraDistance = SATELLITE_EARTH_DEFAULT_DISTANCE,
        )
    }

    // Precompute smooth 3D orbital track for the selected satellite (refreshed every 15s or on scrub).
    val orbitBucket = currentSimulationMs / 15_000L
    val selectedOrbitTrack = remember(selectedSatItem?.id, selectedSatItem?.noradId, orbitBucket) {
        selectedSatItem?.let { sat ->
            SatelliteEngine.calculateOrbitTrack3D(
                satellite = sat,
                currentTimestampMs = currentSimulationMs,
                stepSeconds = 60,
            )
        }.orEmpty()
    }

    val hitRadiusPx = with(density) { 32.dp.toPx() }

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
                val hit = findTappedSatellite3D(
                    tapOffset = tapOffset,
                    candidates = allInteractiveMapStates,
                    camera = controller.snapshot,
                    viewportWidth = viewportSize.width.toFloat(),
                    viewportHeight = viewportSize.height.toFloat(),
                    hitRadiusPx = hitRadiusPx,
                )
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
            val globeRadiusPx = Earth3DMath.projectedGlobeRadiusPx(cam.distance, h)

            // 2a. Delicate optical limb ring so snow-capped poles stay crisp even on light themes
            if (globeRadiusPx > 4f && overlayStyle.limbRimColor.alpha > 0f) {
                drawCircle(
                    color = overlayStyle.limbRimColor,
                    radius = globeRadiusPx,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            // 2b. Subtle 3D Lat/Lon Graticule & Equator on the visible hemisphere
            drawGraticule3D(
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
                    radius = if (isSelected) 5.dp.toPx() else 2.8.dp.toPx(),
                    center = pos,
                )
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
                        radius = if (idx == 0 || isPearlSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = pos,
                    )
                }
            }

            // 2h. Static Catalog & Curated Fleet Satellite Markers (ISS, Tiangong, Hubble, leaders)
            for (liveState in liveSatelliteStates) {
                val sat = liveState.satellite
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
                } else {
                    drawCircle(
                        color = markerColor.copy(alpha = 0.26f),
                        radius = 8.dp.toPx(),
                        center = pos,
                    )
                }

                drawCircle(
                    color = markerColor,
                    radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(),
                    center = pos,
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

        // 3. Reset View Button Overlay (Top Right)
        SmallFloatingActionButton(
            onClick = {
                animateCameraTo(
                    targetLatDeg = userLocation.latitude,
                    targetLonDeg = userLocation.longitude,
                    targetDistance = SATELLITE_EARTH_DEFAULT_DISTANCE,
                    resetUserZoom = true,
                )
            },
            containerColor = overlayStyle.resetButtonContainerColor,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(32.dp)
                .testTag("map_reset_view"),
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusWeak,
                contentDescription = "Reset",
                modifier = Modifier.size(16.dp),
            )
        }

        // 4. Live-Tracking Anchored Satellite Popover on Tap
        if (anchoredSatLabelState != null && viewportSize.width > 0 && viewportSize.height > 0) {
            val liveAnchored = allInteractiveMapStates.firstOrNull {
                it.satellite.id == anchoredSatLabelState.satellite.id
            } ?: anchoredSatLabelState
            val anchoredProj = Earth3DMath.projectLatLon(
                latDeg = liveAnchored.topocentric.subLatDeg,
                lonDeg = liveAnchored.topocentric.subLonDeg,
                radius = Earth3DMath.altitudeKmToRadius(liveAnchored.topocentric.satAltKm),
                camera = controller.snapshot,
                viewportWidth = viewportSize.width.toFloat(),
                viewportHeight = viewportSize.height.toFloat(),
            )
            if (anchoredProj.visible) {
                val popoverWidth = 154.dp
                val popoverWidthPx = with(density) { popoverWidth.toPx() }
                val popoverHeightPx = with(density) { 44.dp.toPx() }
                val marginPx = with(density) { 8.dp.toPx() }
                val gapPx = with(density) { 10.dp.toPx() }

                val clampedX = (anchoredProj.screenX - popoverWidthPx * 0.5f)
                    .coerceIn(marginPx, (viewportSize.width - popoverWidthPx - marginPx).coerceAtLeast(marginPx))
                val aboveY = anchoredProj.screenY - popoverHeightPx - gapPx
                val clampedY = if (aboveY >= marginPx) {
                    aboveY
                } else {
                    (anchoredProj.screenY + gapPx)
                        .coerceAtMost((viewportSize.height - popoverHeightPx - marginPx).coerceAtLeast(marginPx))
                }

                Surface(
                    modifier = Modifier
                        .offset(
                            x = with(density) { clampedX.toDp() },
                            y = with(density) { clampedY.toDp() },
                        )
                        .width(popoverWidth)
                        .clickable { onOpenSatelliteDetail(liveAnchored.satellite) }
                        .testTag("anchored_sat_label"),
                    shape = overlayStyle.popoverShape,
                    color = overlayStyle.popoverContainerColor,
                    border = BorderStroke(1.dp, overlayStyle.popoverBorderColor),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (overlayStyle.showPopoverLeadingIcon) {
                            Icon(
                                imageVector = Icons.Default.SatelliteAlt,
                                contentDescription = null,
                                tint = overlayStyle.popoverAccentColor,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) liveAnchored.satellite.nameFa else liveAnchored.satellite.nameEn,
                                style = RedTypographyTokens.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val altStr = String.format(Locale.US, "%.0f km", liveAnchored.topocentric.satAltKm)
                            val altText = if (isFa) {
                                TimeEngine.formatPersianNumbers("${overlayStyle.altPrefixFa}$altStr")
                            } else {
                                "${overlayStyle.altPrefixEn}$altStr"
                            }
                            Text(
                                text = altText,
                                style = RedTypographyTokens.caption.copy(fontSize = 9.5.sp),
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Detail",
                            tint = overlayStyle.popoverAccentColor,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
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
                } else {
                    futurePath.lineTo(proj.screenX, proj.screenY)
                }
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
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f)),
        ),
    )
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
