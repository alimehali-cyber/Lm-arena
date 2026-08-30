package com.alijafari.red.astronomy.sandbox.model

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-performance 3D Vector with double precision for SI astronomical physics.
 * Supports both immutable algebraic operations and in-place mutable accumulators
 * to eliminate object allocation during high-frequency integration loops.
 */
data class Vector3D(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {
    operator fun plus(v: Vector3D): Vector3D = Vector3D(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vector3D): Vector3D = Vector3D(x - v.x, y - v.y, z - v.z)
    operator fun times(scalar: Double): Vector3D = Vector3D(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Double): Vector3D = Vector3D(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus(): Vector3D = Vector3D(-x, -y, -z)

    fun lengthSquared(): Double = x * x + y * y + z * z

    fun length(): Double = sqrt(lengthSquared())

    fun dot(v: Vector3D): Double = x * v.x + y * v.y + z * v.z

    fun cross(v: Vector3D): Vector3D = Vector3D(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x
    )

    fun distanceTo(v: Vector3D): Double = (this - v).length()

    fun distanceSquaredTo(v: Vector3D): Double = (this - v).lengthSquared()

    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0.0) this / len else ZERO
    }

    fun isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()

    fun hasNaN(): Boolean = x.isNaN() || y.isNaN() || z.isNaN()

    companion object {
        val ZERO = Vector3D(0.0, 0.0, 0.0)
        val UNIT_X = Vector3D(1.0, 0.0, 0.0)
        val UNIT_Y = Vector3D(0.0, 1.0, 0.0)
        val UNIT_Z = Vector3D(0.0, 0.0, 1.0)
    }
}
