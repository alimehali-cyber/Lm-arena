package com.alijafari.red.astronomy.sandbox.physics

import com.alijafari.red.astronomy.sandbox.model.CollisionEvent
import com.alijafari.red.astronomy.sandbox.model.CollisionPolicy
import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import com.alijafari.red.astronomy.sandbox.model.Vector3D
import kotlin.math.cbrt
import kotlin.math.sqrt

/**
 * Handles physical body collisions and mergers according to explicit per-body collision policies.
 */
class CollisionHandler {

    val recentCollisionEvents = ArrayList<CollisionEvent>()

    fun clearEvents() {
        recentCollisionEvents.clear()
    }

    /**
     * Checks all active pairs for physical radius overlap and applies policy.
     * Returns true if any merger/collision occurred.
     */
    fun processCollisions(state: PhysicsStateBuffer, currentSimulationTimeSeconds: Double): Boolean {
        var collisionOccurred = false
        val n = state.activeCount
        val px = state.posX; val py = state.posY; val pz = state.posZ
        val vx = state.velX; val vy = state.velY; val vz = state.velZ
        val m = state.mass; val rad = state.radius
        val isActive = state.isActive
        val policy = state.collisionPolicy
        val types = state.type
        val ids = state.id

        for (i in 0 until n) {
            if (!isActive[i]) continue

            for (j in i + 1 until n) {
                if (!isActive[j]) continue

                val policyI = policy[i]
                val policyJ = policy[j]

                // Pass-through check
                if (policyI == CollisionPolicy.PASS_THROUGH || policyJ == CollisionPolicy.PASS_THROUGH) {
                    continue
                }

                val dx = px[j] - px[i]
                val dy = py[j] - py[i]
                val dz = pz[j] - pz[i]
                val distSquared = dx * dx + dy * dy + dz * dz
                val contactDist = rad[i] + rad[j]

                if (distSquared <= contactDist * contactDist) {
                    // Contact detected!
                    val dist = sqrt(distSquared)
                    val relVx = vx[j] - vx[i]
                    val relVy = vy[j] - vy[i]
                    val relVz = vz[j] - vz[i]
                    val relSpeed = sqrt(relVx * relVx + relVy * relVy + relVz * relVz)

                    val midX = (px[i] + px[j]) * 0.5
                    val midY = (py[i] + py[j]) * 0.5
                    val midZ = (pz[i] + pz[j]) * 0.5
                    val collisionPos = Vector3D(midX, midY, midZ)

                    val isBhI = types[i] == SandboxBodyType.BLACK_HOLE || policyI == CollisionPolicy.BLACK_HOLE_ACCEDE
                    val isBhJ = types[j] == SandboxBodyType.BLACK_HOLE || policyJ == CollisionPolicy.BLACK_HOLE_ACCEDE

                    if (isBhI && !isBhJ) {
                        // Body i is Black Hole, accretes body j
                        accreteIntoBlackHole(i, j, state)
                        recordEvent(currentSimulationTimeSeconds, ids[i], ids[j], collisionPos, relSpeed, ids[i], CollisionPolicy.BLACK_HOLE_ACCEDE)
                        collisionOccurred = true
                    } else if (isBhJ && !isBhI) {
                        // Body j is Black Hole, accretes body i
                        accreteIntoBlackHole(j, i, state)
                        recordEvent(currentSimulationTimeSeconds, ids[j], ids[i], collisionPos, relSpeed, ids[j], CollisionPolicy.BLACK_HOLE_ACCEDE)
                        collisionOccurred = true
                    } else if (policyI == CollisionPolicy.DESTROY_BOTH || policyJ == CollisionPolicy.DESTROY_BOTH) {
                        isActive[i] = false
                        isActive[j] = false
                        recordEvent(currentSimulationTimeSeconds, ids[i], ids[j], collisionPos, relSpeed, null, CollisionPolicy.DESTROY_BOTH)
                        collisionOccurred = true
                    } else {
                        // Default inelastic planetary merger
                        val dominantIdx = if (m[i] >= m[j]) i else j
                        val subordinateIdx = if (dominantIdx == i) j else i

                        inelasticMerge(dominantIdx, subordinateIdx, state)
                        recordEvent(currentSimulationTimeSeconds, ids[dominantIdx], ids[subordinateIdx], collisionPos, relSpeed, ids[dominantIdx], CollisionPolicy.MERGE_CONSERVE_MOMENTUM)
                        collisionOccurred = true
                    }
                }
            }
        }

        return collisionOccurred
    }

    private fun accreteIntoBlackHole(bhIdx: Int, victimIdx: Int, state: PhysicsStateBuffer) {
        val totalM = state.mass[bhIdx] + state.mass[victimIdx]
        if (totalM > 0.0) {
            // Linear momentum conservation for black hole motion
            state.velX[bhIdx] = (state.mass[bhIdx] * state.velX[bhIdx] + state.mass[victimIdx] * state.velX[victimIdx]) / totalM
            state.velY[bhIdx] = (state.mass[bhIdx] * state.velY[bhIdx] + state.mass[victimIdx] * state.velY[victimIdx]) / totalM
            state.velZ[bhIdx] = (state.mass[bhIdx] * state.velZ[bhIdx] + state.mass[victimIdx] * state.velZ[victimIdx]) / totalM
            state.mass[bhIdx] = totalM
        }
        state.isActive[victimIdx] = false
    }

    private fun inelasticMerge(domIdx: Int, subIdx: Int, state: PhysicsStateBuffer) {
        val totalM = state.mass[domIdx] + state.mass[subIdx]
        if (totalM > 0.0) {
            // Conserve linear momentum
            state.velX[domIdx] = (state.mass[domIdx] * state.velX[domIdx] + state.mass[subIdx] * state.velX[subIdx]) / totalM
            state.velY[domIdx] = (state.mass[domIdx] * state.velY[domIdx] + state.mass[subIdx] * state.velY[subIdx]) / totalM
            state.velZ[domIdx] = (state.mass[domIdx] * state.velZ[domIdx] + state.mass[subIdx] * state.velZ[subIdx]) / totalM

            // Contact Center of mass position
            state.posX[domIdx] = (state.mass[domIdx] * state.posX[domIdx] + state.mass[subIdx] * state.posX[subIdx]) / totalM
            state.posY[domIdx] = (state.mass[domIdx] * state.posY[domIdx] + state.mass[subIdx] * state.posY[subIdx]) / totalM
            state.posZ[domIdx] = (state.mass[domIdx] * state.posZ[domIdx] + state.mass[subIdx] * state.posZ[subIdx]) / totalM

            // Volume preservation radius
            val rDom = state.radius[domIdx]
            val rSub = state.radius[subIdx]
            state.radius[domIdx] = cbrt(rDom * rDom * rDom + rSub * rSub * rSub)

            state.mass[domIdx] = totalM
        }
        state.isActive[subIdx] = false
    }

    private fun recordEvent(
        timestamp: Double,
        primaryId: String,
        secondaryId: String,
        pos: Vector3D,
        relSpeed: Double,
        resId: String?,
        policy: CollisionPolicy
    ) {
        // Keep max 20 events in history
        if (recentCollisionEvents.size > 20) {
            recentCollisionEvents.removeAt(0)
        }
        recentCollisionEvents.add(
            CollisionEvent(
                timestampSeconds = timestamp,
                primaryBodyId = primaryId,
                secondaryBodyId = secondaryId,
                collisionPosition = pos,
                relativeVelocity = relSpeed,
                resultingBodyId = resId,
                policyApplied = policy
            )
        )
    }
}
