package com.alijafari.red.astronomy.sandbox.physics

import com.alijafari.red.astronomy.sandbox.model.CollisionPolicy
import com.alijafari.red.astronomy.sandbox.model.SandboxBody
import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import com.alijafari.red.astronomy.sandbox.model.Vector3D

/**
 * High-performance, zero-allocation flat array state container for N-body simulation (N <= 20).
 * Holds all kinematic and physical parameters in primitive arrays.
 */
class PhysicsStateBuffer(val capacity: Int = AstroPhysicsConstants.MAX_BODIES) {
    var activeCount: Int = 0

    // Kinematics (Double arrays)
    val posX = DoubleArray(capacity)
    val posY = DoubleArray(capacity)
    val posZ = DoubleArray(capacity)

    val velX = DoubleArray(capacity)
    val velY = DoubleArray(capacity)
    val velZ = DoubleArray(capacity)

    val accX = DoubleArray(capacity)
    val accY = DoubleArray(capacity)
    val accZ = DoubleArray(capacity)

    // Physical attributes
    val mass = DoubleArray(capacity)
    val radius = DoubleArray(capacity)
    val visualScale = DoubleArray(capacity)
    val colorHex = LongArray(capacity)
    val isFixed = BooleanArray(capacity)
    val isActive = BooleanArray(capacity)

    // Identity and policies (Object arrays sized to fixed capacity)
    val id = Array(capacity) { "" }
    val type = Array(capacity) { SandboxBodyType.CUSTOM_BODY }
    val nameEn = Array(capacity) { "" }
    val nameFa = Array(capacity) { "" }
    val collisionPolicy = Array(capacity) { CollisionPolicy.MERGE_CONSERVE_MOMENTUM }
    @Suppress("UNCHECKED_CAST")
    val theoreticalMetadata = Array<Map<String, String>>(capacity) { emptyMap() }

    fun clear() {
        activeCount = 0
        for (i in 0 until capacity) {
            isActive[i] = false
            isFixed[i] = false
            posX[i] = 0.0; posY[i] = 0.0; posZ[i] = 0.0
            velX[i] = 0.0; velY[i] = 0.0; velZ[i] = 0.0
            accX[i] = 0.0; accY[i] = 0.0; accZ[i] = 0.0
            mass[i] = 0.0; radius[i] = 0.0; visualScale[i] = 1.0
            id[i] = ""; nameEn[i] = ""; nameFa[i] = ""
            theoreticalMetadata[i] = emptyMap()
        }
    }

    fun loadFromBodies(bodies: List<SandboxBody>) {
        clear()
        val count = minOf(bodies.size, capacity)
        for (i in 0 until count) {
            val b = bodies[i]
            id[i] = b.id
            type[i] = b.type
            nameEn[i] = b.nameEn
            nameFa[i] = b.nameFa
            mass[i] = b.massKg
            radius[i] = b.radiusMeters
            posX[i] = b.position.x
            posY[i] = b.position.y
            posZ[i] = b.position.z
            velX[i] = b.velocity.x
            velY[i] = b.velocity.y
            velZ[i] = b.velocity.z
            accX[i] = b.acceleration.x
            accY[i] = b.acceleration.y
            accZ[i] = b.acceleration.z
            isFixed[i] = b.isFixed
            isActive[i] = b.isActive
            collisionPolicy[i] = b.collisionPolicy
            visualScale[i] = b.visualScaleMultiplier
            colorHex[i] = b.colorHex
            theoreticalMetadata[i] = b.theoreticalMetadata
        }
        activeCount = count
    }

    fun toBodyList(): List<SandboxBody> {
        val list = ArrayList<SandboxBody>(activeCount)
        for (i in 0 until activeCount) {
            if (!isActive[i]) continue
            list.add(
                SandboxBody(
                    id = id[i],
                    type = type[i],
                    nameEn = nameEn[i],
                    nameFa = nameFa[i],
                    massKg = mass[i],
                    radiusMeters = radius[i],
                    position = Vector3D(posX[i], posY[i], posZ[i]),
                    velocity = Vector3D(velX[i], velY[i], velZ[i]),
                    acceleration = Vector3D(accX[i], accY[i], accZ[i]),
                    isFixed = isFixed[i],
                    isActive = isActive[i],
                    collisionPolicy = collisionPolicy[i],
                    visualScaleMultiplier = visualScale[i],
                    colorHex = colorHex[i],
                    theoreticalMetadata = theoreticalMetadata[i]
                )
            )
        }
        return list
    }

    fun copyFrom(other: PhysicsStateBuffer) {
        this.activeCount = other.activeCount
        for (i in 0 until capacity) {
            this.posX[i] = other.posX[i]
            this.posY[i] = other.posY[i]
            this.posZ[i] = other.posZ[i]
            this.velX[i] = other.velX[i]
            this.velY[i] = other.velY[i]
            this.velZ[i] = other.velZ[i]
            this.accX[i] = other.accX[i]
            this.accY[i] = other.accY[i]
            this.accZ[i] = other.accZ[i]
            this.mass[i] = other.mass[i]
            this.radius[i] = other.radius[i]
            this.visualScale[i] = other.visualScale[i]
            this.colorHex[i] = other.colorHex[i]
            this.isFixed[i] = other.isFixed[i]
            this.isActive[i] = other.isActive[i]
            this.id[i] = other.id[i]
            this.type[i] = other.type[i]
            this.nameEn[i] = other.nameEn[i]
            this.nameFa[i] = other.nameFa[i]
            this.collisionPolicy[i] = other.collisionPolicy[i]
            this.theoreticalMetadata[i] = other.theoreticalMetadata[i]
        }
    }
}
