package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import com.alijafari.red.astronomy.astro_engine.EarthEngine
import java.util.Random
import kotlin.math.*

class Earth3DRendererState(
    initialYawDeg: Float = 0f,
    initialPitchDeg: Float = 15f,
    initialScale: Float = 1f
) {
    var yawDeg by mutableStateOf(initialYawDeg)
    var pitchDeg by mutableStateOf(initialPitchDeg)
    var zoomScale by mutableStateOf(initialScale)
    var isAutoRotating by mutableStateOf(true)

    var showAtmosphere by mutableStateOf(true)
    var showClouds by mutableStateOf(true)
    var showNightLights by mutableStateOf(true)
    var showGrid by mutableStateOf(false)
    var showAxisLine by mutableStateOf(true)

    fun resetView() {
        yawDeg = 0f
        pitchDeg = 15f
        zoomScale = 1f
    }
}

@Composable
fun rememberEarth3DRendererState(): Earth3DRendererState {
    return remember { Earth3DRendererState() }
}

data class ProjectedPoint3D(
    val xScreen: Float,
    val yScreen: Float,
    val zDepth: Float, // Depth: z > 0 is facing viewer, z < 0 is back face
    val nx: Float,     // Normal X
    val ny: Float,     // Normal Y
    val nz: Float      // Normal Z
)

@Composable
fun Earth3DCanvas(
    subsolarPoint: EarthEngine.SubsolarPoint,
    rendererState: Earth3DRendererState,
    onLocationTapped: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Smooth auto-rotation animation loop
    LaunchedEffect(rendererState.isAutoRotating) {
        if (rendererState.isAutoRotating) {
            var lastTime = System.nanoTime()
            while (true) {
                withFrameNanos { now ->
                    val deltaSec = (now - lastTime) / 1_000_000_000f
                    lastTime = now
                    rendererState.yawDeg = (rendererState.yawDeg + deltaSec * 8f) % 360f
                }
            }
        }
    }

    val continentPolygons = remember { EarthEngine.getContinentPolygons() }
    val cityLights = remember { EarthEngine.getCityNightLights() }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    rendererState.isAutoRotating = false
                    rendererState.zoomScale = (rendererState.zoomScale * zoom).coerceIn(0.7f, 3.5f)
                    rendererState.yawDeg = (rendererState.yawDeg + pan.x * 0.4f) % 360f
                    rendererState.pitchDeg = (rendererState.pitchDeg - pan.y * 0.4f).coerceIn(-75f, 75f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val baseRadius = min(canvasWidth, canvasHeight) * 0.38f
            val earthRadius = baseRadius * rendererState.zoomScale

            val yawRad = Math.toRadians(rendererState.yawDeg.toDouble()).toFloat()
            val pitchRad = Math.toRadians(rendererState.pitchDeg.toDouble()).toFloat()
            val tiltRad = Math.toRadians(EarthEngine.AXIAL_TILT_DEG).toFloat()

            // 1. STARFIELD BACKGROUND
            drawStarfield(center, canvasWidth, canvasHeight)

            // 2. ATMOSPHERE OUTER RAYLEIGH SCATTERING GLOW (Halo around sphere)
            if (rendererState.showAtmosphere) {
                drawAtmosphericHalo(center, earthRadius)
            }

            // 3. EARTH BASE SPHERE MASK (Sphere Clip Path)
            val sphereClipPath = Path().apply {
                addOval(Rect(center.x - earthRadius, center.y - earthRadius, center.x + earthRadius, center.y + earthRadius))
            }

            clipPath(sphereClipPath) {
                // 3a. OCEAN BASE SURFACE
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1B4965), // Vibrant coastal blue
                            Color(0xFF0F2A4A), // Mid ocean blue
                            Color(0xFF071328)  // Deep abyss ocean blue
                        ),
                        center = center,
                        radius = earthRadius
                    ),
                    radius = earthRadius,
                    center = center
                )

                // Convert Subsolar Point into 3D Sunlight Vector L
                val sun3D = projectLatLonTo3D(
                    latDeg = subsolarPoint.latDeg.toFloat(),
                    lonDeg = subsolarPoint.lonDeg.toFloat(),
                    yawRad = yawRad,
                    pitchRad = pitchRad,
                    tiltRad = tiltRad,
                    earthRadius = earthRadius,
                    center = center
                )
                val sunVectorL = floatArrayOf(sun3D.nx, sun3D.ny, sun3D.nz)

                // 3b. SPECULAR SUNLIGHT REFLECTION (Glint on Oceans)
                if (sun3D.nz > 0) {
                    val specX = center.x + sun3D.nx * earthRadius * 0.65f
                    val specY = center.y - sun3D.ny * earthRadius * 0.65f
                    val specRadius = earthRadius * 0.45f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFBEB).copy(alpha = 0.55f * sun3D.nz),
                                Color(0xFFFDE68A).copy(alpha = 0.25f * sun3D.nz),
                                Color.Transparent
                            ),
                            center = Offset(specX, specY),
                            radius = specRadius
                        ),
                        radius = specRadius,
                        center = Offset(specX, specY)
                    )
                }

                // 3c. CONTINENTS & LANDMASSES (3D Mathematical Projection)
                continentPolygons.forEach { ring ->
                    draw3DPolygonRing(
                        ring = ring,
                        yawRad = yawRad,
                        pitchRad = pitchRad,
                        tiltRad = tiltRad,
                        earthRadius = earthRadius,
                        center = center,
                        sunVectorL = sunVectorL
                    )
                }

                // 3d. LATITUDE & LONGITUDE GRID
                if (rendererState.showGrid) {
                    draw3DGridLines(
                        yawRad = yawRad,
                        pitchRad = pitchRad,
                        tiltRad = tiltRad,
                        earthRadius = earthRadius,
                        center = center
                    )
                }

                // 3e. DAY / NIGHT TERMINATOR & SHADOW OVERLAY
                drawDayNightTerminatorShadow(
                    center = center,
                    earthRadius = earthRadius,
                    sun3D = sun3D
                )

                // 3f. NIGHT CITY LIGHTS (Glowing Amber Nodes on Night Hemisphere)
                if (rendererState.showNightLights) {
                    drawNightCityLights(
                        cityLights = cityLights,
                        yawRad = yawRad,
                        pitchRad = pitchRad,
                        tiltRad = tiltRad,
                        earthRadius = earthRadius,
                        center = center,
                        sunVectorL = sunVectorL
                    )
                }

                // 3g. ATMOSPHERIC CLOUD LAYER & SHADOW
                if (rendererState.showClouds) {
                    draw3DCloudBands(
                        yawRad = yawRad,
                        pitchRad = pitchRad,
                        tiltRad = tiltRad,
                        earthRadius = earthRadius,
                        center = center,
                        sunVectorL = sunVectorL
                    )
                }

                // 3h. SPHERICAL LIMB DARKENING / SHADING
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF020617).copy(alpha = 0.45f)
                        ),
                        center = center,
                        radius = earthRadius
                    ),
                    radius = earthRadius,
                    center = center
                )
            }

            // 4. AXIAL TILT AXIS LINE (Passes through North & South Poles)
            if (rendererState.showAxisLine) {
                drawAxialTiltAxis(
                    yawRad = yawRad,
                    pitchRad = pitchRad,
                    tiltRad = tiltRad,
                    earthRadius = earthRadius,
                    center = center
                )
            }
        }
    }
}

/**
 * Projects latitude/longitude on unit sphere to 3D projected point in camera space.
 */
private fun projectLatLonTo3D(
    latDeg: Float,
    lonDeg: Float,
    yawRad: Float,
    pitchRad: Float,
    tiltRad: Float,
    earthRadius: Float,
    center: Offset
): ProjectedPoint3D {
    val latRad = Math.toRadians(latDeg.toDouble()).toFloat()
    val lonRad = Math.toRadians(lonDeg.toDouble()).toFloat()

    // 0. Spherical Unit Vector
    val x0 = cos(latRad) * sin(lonRad)
    val y0 = sin(latRad)
    val z0 = cos(latRad) * cos(lonRad)

    // 1. Axial Tilt (23.44 deg around Z-axis)
    val cosTilt = cos(tiltRad)
    val sinTilt = sin(tiltRad)
    val x1 = x0 * cosTilt - y0 * sinTilt
    val y1 = x0 * sinTilt + y0 * cosTilt
    val z1 = z0

    // 2. Yaw Rotation (User longitude / auto rotate around Y-axis)
    val cosYaw = cos(yawRad)
    val sinYaw = sin(yawRad)
    val x2 = x1 * cosYaw + z1 * sinYaw
    val y2 = y1
    val z2 = -x1 * sinYaw + z1 * cosYaw

    // 3. Pitch Rotation (User latitude tilt around X-axis)
    val cosPitch = cos(pitchRad)
    val sinPitch = sin(pitchRad)
    val x3 = x2
    val y3 = y2 * cosPitch - z2 * sinPitch
    val z3 = y2 * sinPitch + z2 * cosPitch

    val xScreen = center.x + x3 * earthRadius
    val yScreen = center.y - y3 * earthRadius // Invert Y for screen coordinates

    return ProjectedPoint3D(
        xScreen = xScreen,
        yScreen = yScreen,
        zDepth = z3,
        nx = x3,
        ny = y3,
        nz = z3
    )
}

private fun DrawScope.drawStarfield(center: Offset, width: Float, height: Float) {
    val random = Random(1337)
    for (i in 0..85) {
        val sx = random.nextFloat() * width
        val sy = random.nextFloat() * height
        val radius = 0.8f + random.nextFloat() * 1.6f
        val alpha = 0.15f + random.nextFloat() * 0.35f
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = Offset(sx, sy)
        )
    }
}

private fun DrawScope.drawAtmosphericHalo(center: Offset, earthRadius: Float) {
    val haloRadius = earthRadius * 1.15f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF38BDF8).copy(alpha = 0.35f),
                Color(0xFF0284C7).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = center,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = center
    )
}

private fun DrawScope.draw3DPolygonRing(
    ring: EarthEngine.PolygonRing,
    yawRad: Float,
    pitchRad: Float,
    tiltRad: Float,
    earthRadius: Float,
    center: Offset,
    sunVectorL: FloatArray
) {
    val projectedPoints = ring.points.map { (lat, lon) ->
        projectLatLonTo3D(lat, lon, yawRad, pitchRad, tiltRad, earthRadius, center)
    }

    // Check if any point is on the front hemisphere facing viewer
    val hasVisiblePoints = projectedPoints.any { it.zDepth > -0.05f }
    if (!hasVisiblePoints) return

    val path = Path()
    var isFirst = true

    for (p in projectedPoints) {
        if (isFirst) {
            path.moveTo(p.xScreen, p.yScreen)
            isFirst = false
        } else {
            path.lineTo(p.xScreen, p.yScreen)
        }
    }
    path.close()

    val baseColor = when (ring.terrainType) {
        EarthEngine.TerrainType.LAND_GREEN -> Color(0xFF2D6A4F)  // Vibrant emerald vegetation
        EarthEngine.TerrainType.LAND_DESERT -> Color(0xFFD4A373) // Warm golden desert
        EarthEngine.TerrainType.ICE_CAP -> Color(0xFFF1F5F9)     // Pure ice cap white
        EarthEngine.TerrainType.TUNDRA -> Color(0xFF6B7280)      // Cold tundra gray-green
    }

    drawPath(
        path = path,
        color = baseColor
    )

    // Contour border outline
    drawPath(
        path = path,
        color = baseColor.copy(alpha = 0.60f),
        style = Stroke(width = 1.2f)
    )
}

private fun DrawScope.drawDayNightTerminatorShadow(
    center: Offset,
    earthRadius: Float,
    sun3D: ProjectedPoint3D
) {
    val sunAngleRad = atan2(sun3D.ny, sun3D.nx)
    val shadowOffsetRadius = earthRadius * (1f - max(0f, sun3D.nz))

    // Multi-pass soft twilight terminator blending
    val sunDist = sqrt(sun3D.nx * sun3D.nx + sun3D.ny * sun3D.ny)
    val shadowCenterX = center.x - sun3D.nx * earthRadius * 1.1f
    val shadowCenterY = center.y + sun3D.ny * earthRadius * 1.1f

    if (sun3D.nz < 0.98f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF0F172A).copy(alpha = 0.40f),
                    Color(0xFF020617).copy(alpha = 0.88f),
                    Color(0xFF020617).copy(alpha = 0.96f)
                ),
                center = Offset(shadowCenterX, shadowCenterY),
                radius = earthRadius * 1.8f
            ),
            radius = earthRadius,
            center = center
        )
    }

    // Warm Sunset Golden Penumbra Rim Along Terminator
    if (abs(sun3D.nz) < 0.35f) {
        val termX = center.x - sun3D.nx * earthRadius * 0.2f
        val termY = center.y + sun3D.ny * earthRadius * 0.2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF7700).copy(alpha = 0.35f),
                    Color(0xFFDC2626).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(termX, termY),
                radius = earthRadius * 0.9f
            ),
            radius = earthRadius * 0.9f,
            center = Offset(termX, termY)
        )
    }
}

private fun DrawScope.drawNightCityLights(
    cityLights: List<EarthEngine.CityLightNode>,
    yawRad: Float,
    pitchRad: Float,
    tiltRad: Float,
    earthRadius: Float,
    center: Offset,
    sunVectorL: FloatArray
) {
    cityLights.forEach { city ->
        val p = projectLatLonTo3D(city.latDeg, city.lonDeg, yawRad, pitchRad, tiltRad, earthRadius, center)

        // Only draw if city is on front hemisphere (zDepth > 0) AND on night side (N dot L < 0.1)
        val nDotL = p.nx * sunVectorL[0] + p.ny * sunVectorL[1] + p.nz * sunVectorL[2]
        if (p.zDepth > 0.05f && nDotL < 0.10f) {
            val darknessFactor = ((0.10f - nDotL) / 0.30f).coerceIn(0f, 1f)
            val glowAlpha = city.brightness * darknessFactor

            // Outer golden glow
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = glowAlpha * 0.50f),
                radius = 3.5f * city.brightness,
                center = Offset(p.xScreen, p.yScreen)
            )
            // Bright white-amber core node
            drawCircle(
                color = Color(0xFFFFFBEB).copy(alpha = glowAlpha),
                radius = 1.6f * city.brightness,
                center = Offset(p.xScreen, p.yScreen)
            )
        }
    }
}

private fun DrawScope.draw3DCloudBands(
    yawRad: Float,
    pitchRad: Float,
    tiltRad: Float,
    earthRadius: Float,
    center: Offset,
    sunVectorL: FloatArray
) {
    // Dynamic atmospheric cloud bands
    val cloudCount = 18
    val cloudRadius = earthRadius * 1.008f

    for (i in 0 until cloudCount) {
        val lat = (-60 + i * 7).toFloat()
        val lon = (i * 28 + (yawRad * 180 / PI).toFloat() * 0.2f) % 360f - 180f
        val p = projectLatLonTo3D(lat, lon, yawRad, pitchRad, tiltRad, cloudRadius, center)

        if (p.zDepth > 0.1f) {
            val nDotL = p.nx * sunVectorL[0] + p.ny * sunVectorL[1] + p.nz * sunVectorL[2]
            val cloudAlpha = if (nDotL > 0) 0.35f else 0.10f

            drawOval(
                color = Color.White.copy(alpha = cloudAlpha),
                topLeft = Offset(p.xScreen - earthRadius * 0.18f, p.yScreen - earthRadius * 0.05f),
                size = androidx.compose.ui.geometry.Size(earthRadius * 0.36f, earthRadius * 0.10f)
            )
        }
    }
}

private fun DrawScope.draw3DGridLines(
    yawRad: Float,
    pitchRad: Float,
    tiltRad: Float,
    earthRadius: Float,
    center: Offset
) {
    val gridColor = Color(0xFF38BDF8).copy(alpha = 0.25f)

    // Latitudes (Equator, Tropics, Arctic Circles, +/- 30)
    val lats = floatArrayOf(-66.56f, -30f, -23.44f, 0f, 23.44f, 30f, 66.56f)
    for (lat in lats) {
        val path = Path()
        var isFirst = true
        for (lonStep in -180..180 step 10) {
            val p = projectLatLonTo3D(lat, lonStep.toFloat(), yawRad, pitchRad, tiltRad, earthRadius, center)
            if (p.zDepth > 0f) {
                if (isFirst) {
                    path.moveTo(p.xScreen, p.yScreen)
                    isFirst = false
                } else {
                    path.lineTo(p.xScreen, p.yScreen)
                }
            } else {
                isFirst = true
            }
        }
        drawPath(path = path, color = gridColor, style = Stroke(width = 1f))
    }

    // Longitudes (every 30 degrees)
    for (lon in -180..150 step 30) {
        val path = Path()
        var isFirst = true
        for (latStep in -90..90 step 5) {
            val p = projectLatLonTo3D(latStep.toFloat(), lon.toFloat(), yawRad, pitchRad, tiltRad, earthRadius, center)
            if (p.zDepth > 0f) {
                if (isFirst) {
                    path.moveTo(p.xScreen, p.yScreen)
                    isFirst = false
                } else {
                    path.lineTo(p.xScreen, p.yScreen)
                }
            } else {
                isFirst = true
            }
        }
        drawPath(path = path, color = gridColor, style = Stroke(width = 1f))
    }
}

private fun DrawScope.drawAxialTiltAxis(
    yawRad: Float,
    pitchRad: Float,
    tiltRad: Float,
    earthRadius: Float,
    center: Offset
) {
    val northPole = projectLatLonTo3D(90f, 0f, yawRad, pitchRad, tiltRad, earthRadius * 1.22f, center)
    val southPole = projectLatLonTo3D(-90f, 0f, yawRad, pitchRad, tiltRad, earthRadius * 1.22f, center)

    drawLine(
        color = Color(0xFFEF4444).copy(alpha = 0.85f),
        start = Offset(southPole.xScreen, southPole.yScreen),
        end = Offset(northPole.xScreen, northPole.yScreen),
        strokeWidth = 2f
    )

    // Pole tips
    drawCircle(
        color = Color(0xFFEF4444),
        radius = 4f,
        center = Offset(northPole.xScreen, northPole.yScreen)
    )
    drawCircle(
        color = Color(0xFF3B82F6),
        radius = 4f,
        center = Offset(southPole.xScreen, southPole.yScreen)
    )
}
