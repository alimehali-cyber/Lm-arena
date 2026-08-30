package com.alijafari.red.astronomy.sandbox.render.camera

import kotlin.math.sqrt

/**
 * High-performance, allocation-free 3D Ray-Sphere intersection solver for touch picking.
 */
object RayCaster {

    /**
     * Tests intersection between a ray and a sphere.
     *
     * @param rayOriginX Origin X of ray
     * @param rayOriginY Origin Y of ray
     * @param rayOriginZ Origin Z of ray
     * @param rayDirX Normalized direction X of ray
     * @param rayDirY Normalized direction Y of ray
     * @param rayDirZ Normalized direction Z of ray
     * @param sphereCenterX Sphere center X
     * @param sphereCenterY Sphere center Y
     * @param sphereCenterZ Sphere center Z
     * @param sphereRadius Sphere radius (or touch-expanded hit radius)
     * @return Distance t along ray to nearest positive intersection, or Float.MAX_VALUE if no hit
     */
    fun intersectRaySphere(
        rayOriginX: Float,
        rayOriginY: Float,
        rayOriginZ: Float,
        rayDirX: Float,
        rayDirY: Float,
        rayDirZ: Float,
        sphereCenterX: Float,
        sphereCenterY: Float,
        sphereCenterZ: Float,
        sphereRadius: Float
    ): Float {
        val ocX = rayOriginX - sphereCenterX
        val ocY = rayOriginY - sphereCenterY
        val ocZ = rayOriginZ - sphereCenterZ

        val b = ocX * rayDirX + ocY * rayDirY + ocZ * rayDirZ
        val c = (ocX * ocX + ocY * ocY + ocZ * ocZ) - sphereRadius * sphereRadius
        val discriminant = b * b - c

        if (discriminant < 0.0f) {
            return Float.MAX_VALUE
        }

        val sqrtDisc = sqrt(discriminant)
        val t1 = -b - sqrtDisc
        val t2 = -b + sqrtDisc

        if (t1 > 0.0f) return t1
        if (t2 > 0.0f) return t2
        return Float.MAX_VALUE
    }
}
