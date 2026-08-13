package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.data.catalog.ConstellationCatalog
import kotlin.math.*

/**
 * Supporting objects and catalogs for SkyMapRenderer.
 */
object StarCatalog {
    data class Star(
        val hipId: Int,
        val name: String?,
        val raDeg: Double,
        val decDeg: Double,
        val magnitude: Double
    )

    val stars: List<Star> = com.alijafari.red.astronomy.data.catalog.StarCatalog.getStars().map { obj ->
        Star(
            hipId = obj.hipId ?: obj.id.hashCode(),
            name = obj.nameEn.substringBefore(" ("),
            raDeg = obj.raDeg,
            decDeg = obj.decDeg,
            magnitude = obj.magnitude
        )
    }
}

class ConstellationEngine {
    data class StarConnection(
        val hipId1: Int,
        val hipId2: Int
    )

    fun getAllConnections(): List<StarConnection> {
        val starMap = com.alijafari.red.astronomy.data.catalog.StarCatalog.getStars().associateBy { it.id }
        val connections = mutableListOf<StarConnection>()
        for (constellation in ConstellationCatalog.getConstellations()) {
            for (line in constellation.starIdsLines) {
                val s1 = starMap[line.first]
                val s2 = starMap[line.second]
                if (s1 != null && s2 != null) {
                    val hip1 = s1.hipId ?: s1.id.hashCode()
                    val hip2 = s2.hipId ?: s2.id.hashCode()
                    connections.add(StarConnection(hip1, hip2))
                }
            }
        }
        return connections
    }
}

object DeepSkyCatalogExpanded {
    data class DeepSkyObject(
        val catalogId: String,
        val commonName: String?,
        val type: String,
        val raHours: Double,
        val decDeg: Double,
        val magnitude: Double
    )

    val objects: List<DeepSkyObject> = DeepSkyCatalog.objects.map { obj ->
        DeepSkyObject(
            catalogId = obj.catalogId,
            commonName = obj.commonName,
            type = obj.type.name,
            raHours = obj.raHours,
            decDeg = obj.decDeg,
            magnitude = obj.magnitude
        )
    }
}

class DeepSkyEngineV2

/**
 * Sky map renderer.
 * Projects celestial coordinates onto a 2D canvas
 * using stereographic projection (zenith-centered).
 */
class SkyMapRenderer {

    companion object {
        private const val DEG2RAD = Math.PI / 180.0
        private const val RAD2DEG = 180.0 / Math.PI
    }

    data class ScreenPoint(
        val x: Float,
        val y: Float
    )

    data class RenderSettings(
        val canvasWidth: Float,
        val canvasHeight: Float,
        val centerAzDeg: Double,     // Center azimuth
        val centerAltDeg: Double,    // Center altitude
        val fieldOfViewDeg: Double,  // Field of view (e.g., 60°)
        val showStars: Boolean = true,
        val showConstellations: Boolean = true,
        val showDeepSky: Boolean = true,
        val showGrid: Boolean = true,
        val showLabels: Boolean = true
    )

    data class RenderResult(
        val stars: List<StarRender>,
        val constellationLines: List<LineRender>,
        val deepSkyObjects: List<DeepSkyRender>,
        val gridLines: List<LineRender>,
        val labels: List<LabelRender>
    )

    data class StarRender(
        val hipId: Int,
        val name: String?,
        val x: Float,
        val y: Float,
        val radius: Float,
        val magnitude: Double
    )

    data class LineRender(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float
    )

    data class DeepSkyRender(
        val catalogId: String,
        val name: String?,
        val type: String,
        val x: Float,
        val y: Float,
        val radius: Float,
        val magnitude: Double
    )

    data class LabelRender(
        val text: String,
        val x: Float,
        val y: Float
    )

    private val frameEngine = FrameTransformationEngine()
    private val constellationEngine = ConstellationEngine()
    private val deepSkyEngine = DeepSkyEngineV2()

    // ============================================================
    // Projection
    // ============================================================

    /**
     * Project Alt/Az to screen coordinates using stereographic projection.
     * Zenith is at canvas center.
     */
    fun projectAltAz(
        altDeg: Double,
        azDeg: Double,
        settings: RenderSettings
    ): ScreenPoint {
        val centerX = settings.canvasWidth / 2.0f
        val centerY = settings.canvasHeight / 2.0f
        val radius = min(settings.canvasWidth, settings.canvasHeight) / 2.0f

        val zenithDistDeg = (90.0 - altDeg).coerceIn(0.0, 90.0)
        val fovRad = settings.fieldOfViewDeg * DEG2RAD
        val r = radius * tan(zenithDistDeg * DEG2RAD / 2.0) / tan(fovRad / 4.0)

        val azRad = azDeg * DEG2RAD
        val px = centerX + (r * cos(azRad)).toFloat()
        val py = centerY - (r * sin(azRad)).toFloat()

        return ScreenPoint(px, py)
    }

    /**
     * Convert RA/Dec to screen coordinates.
     */
    fun projectRaDec(
        raDeg: Double,
        decDeg: Double,
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: RenderSettings
    ): ScreenPoint {
        val horizontal = frameEngine.equatorialToHorizontal(
            raDeg, decDeg, astroTime, userLatDeg, userLonDeg
        )
        return projectAltAz(horizontal.altDeg, horizontal.azDeg, settings)
    }

    // ============================================================
    // Rendering
    // ============================================================

    /**
     * Render the full sky map.
     */
    fun render(
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: RenderSettings
    ): RenderResult {
        val stars = if (settings.showStars) renderStars(astroTime, userLatDeg, userLonDeg, settings) else emptyList()
        val constellationLines = if (settings.showConstellations) renderConstellationLines(astroTime, userLatDeg, userLonDeg, settings) else emptyList()
        val deepSky = if (settings.showDeepSky) renderDeepSky(astroTime, userLatDeg, userLonDeg, settings) else emptyList()
        val grid = if (settings.showGrid) renderGrid(settings) else emptyList()
        val labels = if (settings.showLabels) renderLabels(stars, deepSky, settings) else emptyList()

        return RenderResult(stars, constellationLines, deepSky, grid, labels)
    }

    /**
     * Render stars (size by magnitude).
     */
    private fun renderStars(
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: RenderSettings
    ): List<StarRender> {
        return StarCatalog.stars.mapNotNull { star ->
            val point = projectRaDec(
                star.raDeg, star.decDeg,
                astroTime, userLatDeg, userLonDeg, settings
            )
            if (point.x in -50.0f..(settings.canvasWidth + 50.0f) &&
                point.y in -50.0f..(settings.canvasHeight + 50.0f)) {
                val radius = starRadius(star.magnitude)
                StarRender(star.hipId, star.name, point.x, point.y, radius, star.magnitude)
            } else {
                null
            }
        }
    }

    /**
     * Render constellation connection lines.
     */
    private fun renderConstellationLines(
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: RenderSettings
    ): List<LineRender> {
        val lines = mutableListOf<LineRender>()
        val connections = constellationEngine.getAllConnections()

        for (conn in connections) {
            val star1 = StarCatalog.stars.firstOrNull { it.hipId == conn.hipId1 } ?: continue
            val star2 = StarCatalog.stars.firstOrNull { it.hipId == conn.hipId2 } ?: continue

            val p1 = projectRaDec(
                star1.raDeg, star1.decDeg,
                astroTime, userLatDeg, userLonDeg, settings
            )
            val p2 = projectRaDec(
                star2.raDeg, star2.decDeg,
                astroTime, userLatDeg, userLonDeg, settings
            )

            lines.add(LineRender(p1.x, p1.y, p2.x, p2.y))
        }
        return lines
    }

    /**
     * Render deep sky objects (symbol by type).
     */
    private fun renderDeepSky(
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: RenderSettings
    ): List<DeepSkyRender> {
        return DeepSkyCatalogExpanded.objects.mapNotNull { obj ->
            val point = projectRaDec(
                obj.raHours * 15.0, obj.decDeg,
                astroTime, userLatDeg, userLonDeg, settings
            )
            if (point.x in -50.0f..(settings.canvasWidth + 50.0f) &&
                point.y in -50.0f..(settings.canvasHeight + 50.0f)) {
                val radius = deepSkyRadius(obj.type, obj.magnitude)
                DeepSkyRender(obj.catalogId, obj.commonName, obj.type, point.x, point.y, radius, obj.magnitude)
            } else {
                null
            }
        }
    }

    /**
     * Render altitude/azimuth grid.
     */
    private fun renderGrid(settings: RenderSettings): List<LineRender> {
        val lines = mutableListOf<LineRender>()
        val centerX = settings.canvasWidth / 2.0f
        val centerY = settings.canvasHeight / 2.0f
        val radius = min(settings.canvasWidth, settings.canvasHeight) / 2.0f

        for (alt in intArrayOf(0, 30, 60)) {
            val zenithDistDeg = 90.0 - alt
            val fovRad = settings.fieldOfViewDeg * DEG2RAD
            val r = (radius * tan(zenithDistDeg * DEG2RAD / 2.0) / tan(fovRad / 4.0)).toFloat()
            val points = (0..360 step 10).map { az ->
                val azRad = az * DEG2RAD
                ScreenPoint(
                    centerX + (r * cos(azRad)).toFloat(),
                    centerY - (r * sin(azRad)).toFloat()
                )
            }
            for (i in 0 until points.size - 1) {
                lines.add(LineRender(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y))
            }
        }

        for (az in 0 until 360 step 30) {
            val azRad = az * DEG2RAD
            val p1 = ScreenPoint(
                centerX + (radius * cos(azRad)).toFloat(),
                centerY - (radius * sin(azRad)).toFloat()
            )
            lines.add(LineRender(centerX, centerY, p1.x, p1.y))
        }

        return lines
    }

    /**
     * Generate labels for bright stars and deep sky objects.
     */
    private fun renderLabels(
        stars: List<StarRender>,
        deepSky: List<DeepSkyRender>,
        settings: RenderSettings
    ): List<LabelRender> {
        val labels = mutableListOf<LabelRender>()

        for (star in stars) {
            if (star.magnitude < 2.0 && star.name != null) {
                labels.add(LabelRender(star.name, star.x + star.radius + 4, star.y - 4))
            }
        }

        for (obj in deepSky) {
            if (obj.magnitude < 8.0 && obj.name != null) {
                labels.add(LabelRender(obj.name, obj.x + obj.radius + 4, obj.y - 4))
            }
        }

        return labels
    }

    // ============================================================
    // Interaction
    // ============================================================

    /**
     * Find the nearest star to a screen tap position.
     */
    fun identifyStarAt(
        x: Float,
        y: Float,
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: RenderSettings,
        maxDistance: Float = 30.0f
    ): StarCatalog.Star? {
        val candidates = StarCatalog.stars.map { star ->
            val point = projectRaDec(
                star.raDeg, star.decDeg,
                astroTime, userLatDeg, userLonDeg, settings
            )
            Triple(star, point, hypot((point.x - x).toDouble(), (point.y - y).toDouble()))
        }
        val withinDistance = candidates.filter { it.third < maxDistance }.minByOrNull { it.third }
        if (withinDistance != null) return withinDistance.first
        return candidates.minByOrNull { it.third }?.first
    }

    /**
     * Find the nearest deep sky object to a screen tap position.
     */
    fun identifyDeepSkyAt(
        x: Float,
        y: Float,
        astroTime: AstroTime,
        userLatDeg: Double,
        userLonDeg: Double,
        settings: RenderSettings,
        maxDistance: Float = 30.0f
    ): DeepSkyCatalogExpanded.DeepSkyObject? {
        val candidates = DeepSkyCatalogExpanded.objects.map { obj ->
            val point = projectRaDec(
                obj.raHours * 15.0, obj.decDeg,
                astroTime, userLatDeg, userLonDeg, settings
            )
            Triple(obj, point, hypot((point.x - x).toDouble(), (point.y - y).toDouble()))
        }
        val withinDistance = candidates.filter { it.third < maxDistance }.minByOrNull { it.third }
        if (withinDistance != null) return withinDistance.first
        return candidates.minByOrNull { it.third }?.first
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    /**
     * Star radius based on magnitude (brighter = larger).
     */
    private fun starRadius(magnitude: Double): Float {
        return when {
            magnitude < 1.0 -> 4.0f
            magnitude < 2.0 -> 3.0f
            magnitude < 3.0 -> 2.5f
            magnitude < 4.0 -> 2.0f
            magnitude < 5.0 -> 1.5f
            else -> 1.0f
        }
    }

    /**
     * Deep sky object radius based on type and magnitude.
     */
    private fun deepSkyRadius(type: String, magnitude: Double): Float {
        val base = when {
            type.contains("Galaxy") -> 3.0f
            type.contains("Nebula") -> 3.5f
            type.contains("Cluster") -> 2.5f
            else -> 2.0f
        }
        return base * (1.0f + ((8.0 - magnitude).coerceIn(0.0, 6.0) / 6.0).toFloat() * 0.5f)
    }

    internal fun starRadiusForTest(magnitude: Double): Float = starRadius(magnitude)
    internal fun deepSkyRadiusForTest(type: String, magnitude: Double): Float = deepSkyRadius(type, magnitude)
}
