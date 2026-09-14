package com.zig.museum.core.model

import kotlin.math.*

/**
 * Tap-to-focus with analytic surface intersection and smooth recentre per M5 task 7
 */

data class Ray(
    val origin: Triple<Float, Float, Float>,
    val direction: Triple<Float, Float, Float> // normalized
)

data class Intersection(
    val point: Triple<Float, Float, Float>,
    val normal: Triple<Float, Float, Float>,
    val distance: Float,
    val uv: Pair<Float, Float> // equirectangular UV
)

object TapToFocus {

    /**
     * Analytic surface intersection: ray vs sphere/ellipsoid
     * @param ray world space ray from camera
     * @param objectRadius object radius in scene units (1.0 per §5.3 unit-radius normalisation)
     * @param oblateness flattening ratio (a-b)/a, 0 = sphere
     * @return Intersection or null if no hit
     */
    fun intersect(ray: Ray, objectRadius: Float = 1.0f, oblateness: Float = 0.0f): Intersection? {
        // For sphere: |o + t*d|^2 = r^2
        // For ellipsoid with oblateness: scale Y axis by (1 - oblateness)
        val (ox, oy, oz) = ray.origin
        val (dx, dy, dz) = ray.direction

        // Transform to ellipsoid space: scale Y by 1/(1-oblateness)
        val invFlatten = if (oblateness != 0f) 1f / (1f - oblateness) else 1f
        val oyScaled = oy * invFlatten
        val dyScaled = dy * invFlatten

        val a = dx*dx + dyScaled*dyScaled + dz*dz
        val b = 2f * (ox*dx + oyScaled*dyScaled + oz*dz)
        val c = ox*ox + oyScaled*oyScaled + oz*oz - objectRadius*objectRadius

        val disc = b*b - 4f*a*c
        if (disc < 0) return null

        val sqrtDisc = sqrt(disc)
        val t0 = (-b - sqrtDisc) / (2f*a)
        val t1 = (-b + sqrtDisc) / (2f*a)
        val t = if (t0 > 0) t0 else if (t1 > 0) t1 else return null

        val hitX = ox + dx*t
        val hitYScaled = oyScaled + dyScaled*t
        val hitZ = oz + dz*t
        val hitY = hitYScaled / invFlatten

        // Normal for ellipsoid: (x, y*(1-oblateness)^2, z) normalized, simplified
        val nx = hitX
        val ny = hitY * (1f - oblateness)*(1f - oblateness)
        val nz = hitZ
        val len = sqrt(nx*nx + ny*ny + nz*nz)
        val normal = Triple(nx/len, ny/len, nz/len)

        // UV: equirectangular
        val lon = atan2(hitX.toDouble(), hitZ.toDouble()) // -pi..pi
        val lat = asin((hitY / objectRadius).toDouble().coerceIn(-1.0, 1.0)) // -pi/2..pi/2
        val u = ((lon / (2*PI) + 0.5).toFloat()).coerceIn(0f, 1f)
        val v = ((0.5 - lat / PI).toFloat()).coerceIn(0f, 1f)

        return Intersection(
            point = Triple(hitX, hitY, hitZ),
            normal = normal,
            distance = t,
            uv = Pair(u, v)
        )
    }

    /**
     * Smooth recentre: interpolate camera target to intersection point while preserving orientation
     * @param currentTarget current camera target
     * @param intersection hit point
     * @param t interpolation factor 0..1
     */
    fun smoothRecentre(
        currentTarget: Triple<Float, Float, Float>,
        intersection: Intersection,
        t: Float
    ): Triple<Float, Float, Float> {
        val (cx, cy, cz) = currentTarget
        val (hx, hy, hz) = intersection.point
        return Triple(
            cx + (hx - cx) * t,
            cy + (hy - cy) * t,
            cz + (hz - cz) * t
        )
    }
}
