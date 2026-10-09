package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.domain.SkyCanvasTheme

/**
 * Draws the shared Sky Canvas layers for a [SkySceneFrame]: atmosphere, Milky Way, stars, Sun, Moon,
 * planets and the horizon landscape, in that order.
 *
 * The projection comes from the [DrawScope] size through [HeroSkyProjection]. Home passes its own
 * canvas size, and the Live Sky backdrop passes its full-screen virtual canvas. Both get identical
 * layering and object styling. Interaction layers (selection ring, particles) stay in the caller.
 */
object SkySceneRenderer {

    /**
     * @param panoramaActive true while the photographic panorama supplies the sky gradient and the
     *   procedural Milky Way is replaced. Only the Home hero can set this.
     */
    fun drawSky(
        drawScope: DrawScope,
        frame: SkySceneFrame,
        theme: SkyCanvasTheme,
        frameTimeMs: Long,
        panoramaActive: Boolean
    ) {
        val canvasW = drawScope.size.width
        val canvasH = drawScope.size.height
        val userLat = frame.latitudeDeg
        val lastDeg = frame.lastDeg
        val sunHoriz = frame.sunHoriz
        val moonData = frame.moonData
        val lightingState = frame.lightingState

        val sunPosPx = if (sunHoriz.altitudeDeg > -18.0) {
            HeroSkyProjection.project(sunHoriz.azimuthDeg, sunHoriz.altitudeDeg, canvasW, canvasH, userLat)
        } else null

        // 1. Atmosphere Renderer. While the panorama is on screen it supplies the sky gradient and night
        // sky itself; this pass then draws only the Sun glow and Belt of Venus, faded by night weight.
        AtmosphereRenderer.drawAtmosphere(
            drawScope = drawScope,
            lightingState = lightingState,
            sunPosPx = sunPosPx,
            theme = theme,
            sunAzimuthDeg = sunHoriz.azimuthDeg,
            latitudeDeg = userLat,
            panoramaMode = panoramaActive
        )

        // 2. Milky Way Renderer (procedural; replaced by the photographic panorama when active)
        if (!panoramaActive) {
            MilkyWayRenderer.drawMilkyWay(
                drawScope = drawScope,
                galacticPoints = frame.galacticPlanePoints,
                lightingState = lightingState,
                frameTimeMs = frameTimeMs,
                theme = theme,
                latitudeDeg = userLat,
                lastDeg = lastDeg
            )
        }

        // 3. Star Renderer
        StarRenderer.drawStars(
            drawScope = drawScope,
            objects = frame.catalogStars,
            starVisibility = lightingState.starVisibility,
            frameTimeMs = frameTimeMs,
            theme = theme,
            latitudeDeg = userLat,
            lastDeg = lastDeg
        )

        // 4. Sun Renderer
        if (sunPosPx != null && sunHoriz.altitudeDeg > -12.0) {
            SunRenderer.drawSun(
                drawScope = drawScope,
                center = sunPosPx,
                sunAltitudeDeg = sunHoriz.altitudeDeg,
                frameTimeMs = frameTimeMs,
                theme = theme
            )
        }

        // 5. Moon Renderer
        if (moonData.altitudeDeg > -12.0) {
            val moonCenter = HeroSkyProjection.project(moonData.azimuthDeg, moonData.altitudeDeg, canvasW, canvasH, userLat)
            val baseMoonRadius = with(drawScope) { 26.dp.toPx() }
            val moonPulseScale = AstronomyAnimator.computePulse(frameTimeMs, 4000f, 0.88f, 1.12f)

            val limbScreenAngleDeg = CoordinateEngine.calculateMoonLimbScreenAngleDeg(
                moonAzimuthDeg = moonData.azimuthDeg,
                moonAltitudeDeg = moonData.altitudeDeg,
                sunAzimuthDeg = sunHoriz.azimuthDeg,
                sunAltitudeDeg = sunHoriz.altitudeDeg
            )

            MoonRenderer.drawMoon(
                drawScope = drawScope,
                center = moonCenter,
                radius = baseMoonRadius,
                illuminationPercent = moonData.illuminationPercent,
                phaseAngleRad = moonData.phaseAngleRad,
                isLunarEclipse = frame.isLunarEclipse,
                isSolarEclipse = frame.isSolarEclipse,
                moonPulseScale = moonPulseScale,
                lightingState = lightingState,
                frameTimeMs = frameTimeMs,
                isWaxing = (moonData.ageDays < 14.765),
                theme = theme,
                limbScreenAngleDeg = limbScreenAngleDeg
            )
        }

        // 6. Planet Renderer
        PlanetRenderer.drawPlanets(
            drawScope = drawScope,
            planets = frame.planetPositions,
            frameTimeMs = frameTimeMs,
            theme = theme,
            latitudeDeg = userLat
        )

        // 7. Horizon Landscape Silhouette Layer
        LandscapeRenderer.drawHorizonLandscape(
            drawScope = drawScope,
            lightingState = lightingState,
            frameTimeMs = frameTimeMs,
            theme = theme
        )
    }
}
