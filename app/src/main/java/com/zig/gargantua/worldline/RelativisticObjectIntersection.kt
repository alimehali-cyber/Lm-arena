package com.zig.gargantua.worldline

import com.zig.gargantua.geodesic.PhotonState4D
import com.zig.gargantua.geodesic.ProceduralSky
import com.zig.gargantua.physics.KerrSchildSpacetime
import kotlin.math.*

/**
 * Detects and evaluates relativistic photon ray intersections with the finite world-tube
 * of a synthetic test object moving along an M8 timelike Kerr worldline.
 *
 * PHYSICAL PRINCIPLES:
 * 1. Finite-speed light travel: The emission event (T_emit, X_emit) occurs in the past of the
 *    camera observation time T_cam, satisfying T_emit = T_cam - Δt_flight.
 * 2. World-tube intersection: Tested via closest approach between the backward ray segment and
 *    the object's worldline state at that exact coordinate time.
 * 3. Relativistic frequency shift: Invariant g = (-p_μ u_obs^μ) / (-p_μ u_emit^μ).
 * 4. Relativistic intensity transfer: I_obs = g^4 I_emit (Liouville's theorem / relativistic beaming).
 */
object RelativisticObjectIntersection {

    data class ObjectHitResult(
        val hitX: Double,
        val hitY: Double,
        val hitZ: Double,
        val hitT: Double,
        val frequencyShift: Double,
        val observedRadiance: Double,
        val observedColor: ProceduralSky.ColorRGB,
        val hitDistance: Double,
        val objectRadius: Double
    )

    /**
     * Checks whether the segment connecting previous and current photon states intersects
     * the object's finite world-tube at the corresponding emission coordinate time.
     *
     * @param previous Photon state at start of integration step.
     * @param current Photon state at end of integration step.
     * @param spacetime Production Kerr-Schild metric.
     * @param obj Relativistic test object.
     * @param camX Observer X position.
     * @param camY Observer Y position.
     * @param camZ Observer Z position.
     * @return [ObjectHitResult] if an intersection occurred, or null otherwise.
     */
    fun checkIntersection(
        previous: PhotonState4D,
        current: PhotonState4D,
        spacetime: KerrSchildSpacetime,
        obj: RelativisticObject,
        camX: Double,
        camY: Double,
        camZ: Double
    ): ObjectHitResult? {
        if (!obj.enabled) return null

        val rCurrent = current.computeR(spacetime.a)
        val rObj = obj.positionAt(current.t)
        val rObjRadius = sqrt(rObj[0] * rObj[0] + rObj[1] * rObj[1] + rObj[2] * rObj[2])

        // Fast bounding shell rejection: skip detailed intersection if far from object radius
        if (abs(rCurrent - rObjRadius) > obj.radius + 1.2) {
            return null
        }

        val obj0 = obj.positionAt(previous.t)
        val obj1 = obj.positionAt(current.t)

        val dp0X = previous.x - obj0[0]
        val dp0Y = previous.y - obj0[1]
        val dp0Z = previous.z - obj0[2]

        val dp1X = current.x - obj1[0]
        val dp1Y = current.y - obj1[1]
        val dp1Z = current.z - obj1[2]

        val vRelX = dp1X - dp0X
        val vRelY = dp1Y - dp0Y
        val vRelZ = dp1Z - dp0Z

        val vRel2 = vRelX * vRelX + vRelY * vRelY + vRelZ * vRelZ
        val sStar = if (vRel2 > 1e-12) {
            (-(dp0X * vRelX + dp0Y * vRelY + dp0Z * vRelZ) / vRel2).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        val hitX = previous.x + sStar * (current.x - previous.x)
        val hitY = previous.y + sStar * (current.y - previous.y)
        val hitZ = previous.z + sStar * (current.z - previous.z)
        val hitT = previous.t + sStar * (current.t - previous.t)

        val objHit = obj.positionAt(hitT)
        val deltaX = hitX - objHit[0]
        val deltaY = hitY - objHit[1]
        val deltaZ = hitZ - objHit[2]
        val hitDist = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        if (hitDist > obj.radius) {
            return null
        }

        // Interpolate photon momentum at intersection
        val hitPx = previous.p_x + sStar * (current.p_x - previous.p_x)
        val hitPy = previous.p_y + sStar * (current.p_y - previous.p_y)
        val hitPz = previous.p_z + sStar * (current.p_z - previous.p_z)

        // Object 4-velocity at emission event
        val uEmit = obj.fourVelocityAt(hitT)
        val u0 = uEmit[0]
        val vx = if (abs(u0) > 1e-12) uEmit[1] / u0 else 0.0
        val vy = if (abs(u0) > 1e-12) uEmit[2] / u0 else 0.0
        val vz = if (abs(u0) > 1e-12) uEmit[3] / u0 else 0.0

        // Denominator of frequency shift: -p_μ u_emit^μ = u^0 (1 - p · v) with p_0 = -1
        val pDotV = hitPx * vx + hitPy * vy + hitPz * vz
        val denomEmit = u0 * (1.0 - pDotV)

        // Static observer at camera position
        val gCam = spacetime.metric(camX, camY, camZ)
        val uObs0 = 1.0 / sqrt(max(1e-12, -gCam[0, 0]))

        val gShift = if (abs(denomEmit) > 1e-12) {
            (uObs0 / denomEmit).coerceIn(0.05, 5.0)
        } else {
            1.0
        }

        // Relativistic beaming g^4
        val g2 = gShift * gShift
        val g4 = g2 * g2

        // Distinctive procedural appearance:
        val distNorm = hitDist / obj.radius
        val localNormalZ = if (hitDist > 1e-12) deltaZ / hitDist else 0.0
        val localNormalX = if (hitDist > 1e-12) deltaX / hitDist else 1.0
        val localNormalY = if (hitDist > 1e-12) deltaY / hitDist else 0.0

        val lat = acos(localNormalZ.coerceIn(-1.0, 1.0))
        val lon = atan2(localNormalY, localNormalX)
        val bands = 0.85 + 0.15 * cos(lat * 12.0) * cos(lon * 8.0)
        val coreGlow = exp(-distNorm * distNorm * 2.0)
        val limb = 0.7 + 0.3 * (1.0 - distNorm)

        val patternFactor = bands * limb + 0.5 * coreGlow
        val observedRadiance = g4 * obj.emissiveRadiance * patternFactor

        // Doppler spectral tint
        val baseR = obj.baseColor.r
        val baseG = obj.baseColor.g
        val baseB = obj.baseColor.b

        val finalR: Double
        val finalG: Double
        val finalB: Double
        if (gShift > 1.0) {
            // Blueshifted: energetic cyan-white
            val shiftBlend = min(1.0, (gShift - 1.0) * 0.5)
            finalR = (1.0 - shiftBlend) * baseR + shiftBlend * 0.8
            finalG = (1.0 - shiftBlend) * baseG + shiftBlend * 0.95
            finalB = (1.0 - shiftBlend) * baseB + shiftBlend * 1.2
        } else {
            // Redshifted: shifted towards amber-orange
            val shiftBlend = min(1.0, (1.0 - gShift) * 0.5)
            finalR = (1.0 - shiftBlend) * baseR + shiftBlend * 1.2
            finalG = (1.0 - shiftBlend) * baseG + shiftBlend * 0.7
            finalB = (1.0 - shiftBlend) * baseB + shiftBlend * 0.5
        }

        val observedColor = ProceduralSky.ColorRGB(
            r = finalR * observedRadiance,
            g = finalG * observedRadiance,
            b = finalB * observedRadiance
        )

        return ObjectHitResult(
            hitX = hitX,
            hitY = hitY,
            hitZ = hitZ,
            hitT = hitT,
            frequencyShift = gShift,
            observedRadiance = observedRadiance,
            observedColor = observedColor,
            hitDistance = hitDist,
            objectRadius = obj.radius
        )
    }
}
