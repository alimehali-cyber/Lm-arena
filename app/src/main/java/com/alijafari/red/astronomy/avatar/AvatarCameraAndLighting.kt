package com.alijafari.red.astronomy.avatar

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class AvatarLightingRig(
    val keyLightDir: FloatArray,
    val keyLightColor: FloatArray,
    val skyZenithColor: FloatArray,
    val groundBounceColor: FloatArray,
    val rimColor: FloatArray,
    val shadowAlpha: Float,
)

/**
 * Pure-Kotlin 3D camera projection & astronomical lighting math for Professor Z.
 *
 * Kept 100% free of Android framework dependencies so every matrix, NDC bound, and lighting state
 * is directly verified inside fast JVM unit tests (`ProfessorZAvatarUnitTest`).
 */
object AvatarCameraAndLighting {

    const val CAMERA_FOV_Y_DEG = 32.0f
    const val CAMERA_POS_X = 0.0f
    const val CAMERA_POS_Y = 0.96f
    const val CAMERA_POS_Z = 3.38f
    const val TARGET_X = 0.0f
    const val TARGET_Y = 0.90f
    const val TARGET_Z = 0.0f

    /** Turn Professor Z ~11° inward toward the center of the Home Sky readout & starfield. */
    fun computeModelMatrix(figureOnRight: Boolean, out: FloatArray = FloatArray(16)): FloatArray {
        val yawRad = (if (figureOnRight) -11.5f else 11.5f) * (PI.toFloat() / 180f)
        val c = cos(yawRad)
        val s = sin(yawRad)
        out.fill(0f)
        out[0] = c;  out[8] = s
        out[5] = 1f
        out[2] = -s; out[10] = c
        out[15] = 1f
        return out
    }

    fun computeViewProjMatrix(
        viewportWidth: Int,
        viewportHeight: Int,
        out: FloatArray = FloatArray(16),
    ): FloatArray {
        val aspect = viewportWidth.coerceAtLeast(1).toFloat() / viewportHeight.coerceAtLeast(1).toFloat()
        val view = lookAt(
            CAMERA_POS_X, CAMERA_POS_Y, CAMERA_POS_Z,
            TARGET_X, TARGET_Y, TARGET_Z,
            0f, 1f, 0f,
        )
        val proj = perspective(CAMERA_FOV_Y_DEG, aspect, 0.25f, 16.0f)
        return multiply4x4(proj, view, out)
    }

    /** Transforms a world-space 3D point `(x, y, z)` to OpenGL Normalized Device Coordinates `[-1, +1]^3`. */
    fun projectWorldToNdc(viewProj: FloatArray, x: Float, y: Float, z: Float): FloatArray {
        val cx = viewProj[0] * x + viewProj[4] * y + viewProj[8] * z + viewProj[12]
        val cy = viewProj[1] * x + viewProj[5] * y + viewProj[9] * z + viewProj[13]
        val cz = viewProj[2] * x + viewProj[6] * y + viewProj[10] * z + viewProj[14]
        val cw = (viewProj[3] * x + viewProj[7] * y + viewProj[11] * z + viewProj[15]).coerceAtLeast(1e-6f)
        return floatArrayOf(cx / cw, cy / cw, cz / cw)
    }

    /** Matches Professor Z's directional PBR key, rim, and hemisphere bounce to ZIG's live sky instant. */
    fun lightingForSky(sunAltitudeDeg: Double, moonGlowIntensity: Float): AvatarLightingRig {
        val alt = sunAltitudeDeg.toFloat()
        val dayT = smoothStep(-2f, 14f, alt)
        val twilightT = smoothStep(-14f, -2f, alt) * (1f - dayT)
        val nightT = (1f - smoothStep(-12f, 0f, alt)).coerceIn(0f, 1f)
        val moonBoost = moonGlowIntensity.coerceIn(0f, 1f)

        val sunElevRad = (alt.coerceIn(8f, 68f) * PI.toFloat()) / 180f
        val dirX = 0.42f
        val dirY = sin(sunElevRad).coerceAtLeast(0.32f)
        val dirZ = cos(sunElevRad).coerceAtLeast(0.45f)

        val keyR = 1.38f * dayT + 1.15f * twilightT + (0.46f + 0.22f * moonBoost) * nightT
        val keyG = 1.32f * dayT + 0.78f * twilightT + (0.52f + 0.24f * moonBoost) * nightT
        val keyB = 1.24f * dayT + 0.62f * twilightT + (0.72f + 0.28f * moonBoost) * nightT

        val zenR = 0.44f * dayT + 0.30f * twilightT + 0.20f * nightT
        val zenG = 0.52f * dayT + 0.30f * twilightT + 0.26f * nightT
        val zenB = 0.68f * dayT + 0.46f * twilightT + 0.42f * nightT

        val gndR = 0.22f * dayT + 0.16f * twilightT + 0.11f * nightT
        val gndG = 0.21f * dayT + 0.14f * twilightT + 0.11f * nightT
        val gndB = 0.19f * dayT + 0.15f * twilightT + 0.14f * nightT

        val rimR = 0.55f * dayT + 0.95f * twilightT + 0.48f * nightT
        val rimG = 0.62f * dayT + 0.58f * twilightT + 0.68f * nightT
        val rimB = 0.75f * dayT + 0.44f * twilightT + 1.05f * nightT

        val shadowAlpha = (0.36f * dayT + 0.26f * twilightT + 0.20f * nightT).coerceIn(0.16f, 0.42f)

        return AvatarLightingRig(
            keyLightDir = floatArrayOf(dirX, dirY, dirZ),
            keyLightColor = floatArrayOf(keyR, keyG, keyB),
            skyZenithColor = floatArrayOf(zenR, zenG, zenB),
            groundBounceColor = floatArrayOf(gndR, gndG, gndB),
            rimColor = floatArrayOf(rimR, rimG, rimB),
            shadowAlpha = shadowAlpha,
        )
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun perspective(fovYDeg: Float, aspect: Float, zNear: Float, zFar: Float): FloatArray {
        val f = 1f / tan((fovYDeg * PI.toFloat() / 180f) * 0.5f)
        val nf = 1f / (zNear - zFar)
        val m = FloatArray(16)
        m[0] = f / aspect
        m[5] = f
        m[10] = (zFar + zNear) * nf
        m[11] = -1f
        m[14] = (2f * zFar * zNear) * nf
        return m
    }

    private fun lookAt(
        eyeX: Float, eyeY: Float, eyeZ: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        upX: Float, upY: Float, upZ: Float,
    ): FloatArray {
        var fx = centerX - eyeX; var fy = centerY - eyeY; var fz = centerZ - eyeZ
        val fLen = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(1e-6f)
        fx /= fLen; fy /= fLen; fz /= fLen

        var sx = fy * upZ - fz * upY
        var sy = fz * upX - fx * upZ
        var sz = fx * upY - fy * upX
        val sLen = sqrt(sx * sx + sy * sy + sz * sz).coerceAtLeast(1e-6f)
        sx /= sLen; sy /= sLen; sz /= sLen

        val ux = sy * fz - sz * fy
        val uy = sz * fx - sx * fz
        val uz = sx * fy - sy * fx

        val m = FloatArray(16)
        m[0] = sx;  m[4] = sy;  m[8] = sz;   m[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ)
        m[1] = ux;  m[5] = uy;  m[9] = uz;   m[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ)
        m[2] = -fx; m[6] = -fy; m[10] = -fz; m[14] = (fx * eyeX + fy * eyeY + fz * eyeZ)
        m[3] = 0f;  m[7] = 0f;  m[11] = 0f;  m[15] = 1f
        return m
    }

    private fun multiply4x4(a: FloatArray, b: FloatArray, out: FloatArray): FloatArray {
        for (col in 0..3) {
            val b0 = b[col * 4]; val b1 = b[col * 4 + 1]; val b2 = b[col * 4 + 2]; val b3 = b[col * 4 + 3]
            for (row in 0..3) {
                out[col * 4 + row] = a[row] * b0 + a[4 + row] * b1 + a[8 + row] * b2 + a[12 + row] * b3
            }
        }
        return out
    }
}
