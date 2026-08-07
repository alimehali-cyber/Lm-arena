package com.alijafari.red.astronomy.astro_engine

import androidx.compose.ui.graphics.Color
import kotlin.math.*

object GravitySandboxEngine {

    enum class CollisionMode(val labelEn: String, val labelFa: String) {
        MERGE("Merge (Accrete)", "ادغام (پیوستن)"),
        ELASTIC("Elastic (Bounce)", "برخورد کشسان (بازگشت)"),
        DESTROY("Destroy", "نابودی"),
        IGNORE("Ignore (Pass Through)", "عبور بدون برخورد")
    }

    enum class ObjectType(val labelEn: String, val labelFa: String, val defaultColor: Color) {
        SUN("Sun", "خورشید", Color(0xFFFFD166)),
        PLANET("Planet", "سیاره", Color(0xFF4EA8DE)),
        MOON("Moon", "قمر", Color(0xFFE2E8F0)),
        COMET("Comet", "دنباله‌دار", Color(0xFF80E9FF)),
        ASTEROID("Asteroid", "سیارک", Color(0xFF94A3B8)),
        BLACK_HOLE("Black Hole", "سیاه‌چاله", Color(0xFF1E1B4B)),
        CUSTOM("Custom Mass", "جرم سفارشی", Color(0xFFF43F5E))
    }

    data class Vector2D(
        var x: Double,
        var y: Double
    ) {
        fun length(): Double = sqrt(x * x + y * y)
        fun distanceTo(other: Vector2D): Double = sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))
        fun normalized(): Vector2D {
            val len = length()
            return if (len > 0) Vector2D(x / len, y / len) else Vector2D(0.0, 0.0)
        }
    }

    data class SandboxBody(
        val id: String,
        var name: String,
        var type: ObjectType,
        var mass: Double, // Relative mass units (Sun = 1000.0, Earth = 1.0, Moon = 0.012)
        var radius: Float, // Display radius in canvas pixels
        var pos: Vector2D, // Canvas coordinates (X, Y)
        var vel: Vector2D, // Velocity vector (Vx, Vy)
        var acc: Vector2D = Vector2D(0.0, 0.0),
        var color: Color = type.defaultColor,
        val trail: MutableList<Vector2D> = mutableListOf(),
        var isFixed: Boolean = false,
        var isSelected: Boolean = false
    ) {
        fun copyBody(): SandboxBody {
            return SandboxBody(
                id = java.util.UUID.randomUUID().toString(),
                name = "$name (Copy)",
                type = type,
                mass = mass,
                radius = radius,
                pos = Vector2D(pos.x + 20.0, pos.y + 20.0),
                vel = Vector2D(vel.x, vel.y),
                color = color,
                isFixed = isFixed
            )
        }
    }

    // Gravitational constant tuned for canvas units
    private const val G = 300.0
    private const val SOFTENING = 12.0 // Softening parameter epsilon to prevent infinite singularity force

    /**
     * Calculates acceleration on body `target` due to all other bodies using Newtonian Gravitational Law.
     */
    fun calculateAcceleration(target: SandboxBody, bodies: List<SandboxBody>): Vector2D {
        var ax = 0.0
        var ay = 0.0

        for (other in bodies) {
            if (other.id == target.id) continue

            val dx = other.pos.x - target.pos.x
            val dy = other.pos.y - target.pos.y
            val distSq = dx * dx + dy * dy + SOFTENING * SOFTENING
            val dist = sqrt(distSq)

            val force = (G * other.mass) / distSq
            ax += force * (dx / dist)
            ay += force * (dy / dist)
        }

        return Vector2D(ax, ay)
    }

    /**
     * Advances N-body physics state using Velocity Verlet Integration for numerical stability.
     */
    fun stepPhysics(
        bodies: MutableList<SandboxBody>,
        dtSeconds: Double,
        collisionMode: CollisionMode,
        maxTrailPoints: Int = 120
    ) {
        if (bodies.isEmpty() || dtSeconds <= 0) return

        // 1. Half-step velocity update & full-step position update
        for (body in bodies) {
            if (body.isFixed) continue
            body.vel.x += 0.5 * body.acc.x * dtSeconds
            body.vel.y += 0.5 * body.acc.y * dtSeconds

            body.pos.x += body.vel.x * dtSeconds
            body.pos.y += body.vel.y * dtSeconds

            // Append trail point
            if (body.trail.isEmpty() || body.pos.distanceTo(body.trail.last()) > 3.0) {
                body.trail.add(Vector2D(body.pos.x, body.pos.y))
                if (body.trail.size > maxTrailPoints) {
                    body.trail.removeAt(0)
                }
            }
        }

        // 2. Compute new accelerations
        val newAccelerations = bodies.map { body -> calculateAcceleration(body, bodies) }

        // 3. Final half-step velocity update
        for (i in bodies.indices) {
            val body = bodies[i]
            if (body.isFixed) continue
            val newAcc = newAccelerations[i]
            body.vel.x += 0.5 * newAcc.x * dtSeconds
            body.vel.y += 0.5 * newAcc.y * dtSeconds
            body.acc = newAcc
        }

        // 4. Handle Collisions
        if (collisionMode != CollisionMode.IGNORE) {
            handleCollisions(bodies, collisionMode)
        }
    }

    private fun handleCollisions(bodies: MutableList<SandboxBody>, collisionMode: CollisionMode) {
        val toRemove = mutableSetOf<String>()

        for (i in 0 until bodies.size) {
            val b1 = bodies[i]
            if (toRemove.contains(b1.id)) continue

            for (j in i + 1 until bodies.size) {
                val b2 = bodies[j]
                if (toRemove.contains(b2.id)) continue

                val dist = b1.pos.distanceTo(b2.pos)
                val minDist = (b1.radius + b2.radius).toDouble() * 0.75

                if (dist < minDist) {
                    when (collisionMode) {
                        CollisionMode.MERGE -> {
                            // Merge smaller body into larger body
                            val (winner, loser) = if (b1.mass >= b2.mass) Pair(b1, b2) else Pair(b2, b1)
                            val totalMass = winner.mass + loser.mass

                            // Conservation of Momentum: V_new = (m1*v1 + m2*v2) / (m1+m2)
                            winner.vel.x = (winner.mass * winner.vel.x + loser.mass * loser.vel.x) / totalMass
                            winner.vel.y = (winner.mass * winner.vel.y + loser.mass * loser.vel.y) / totalMass
                            winner.mass = totalMass
                            winner.radius = (winner.radius * 1.15f).coerceAtMost(60f)

                            toRemove.add(loser.id)
                        }

                        CollisionMode.ELASTIC -> {
                            // Elastic bounce along collision normal
                            val nx = (b2.pos.x - b1.pos.x) / dist
                            val ny = (b2.pos.y - b1.pos.y) / dist

                            val kx = b1.vel.x - b2.vel.x
                            val ky = b1.vel.y - b2.vel.y
                            val p = 2.0 * (nx * kx + ny * ky) / (b1.mass + b2.mass)

                            if (!b1.isFixed) {
                                b1.vel.x -= p * b2.mass * nx
                                b1.vel.y -= p * b2.mass * ny
                            }
                            if (!b2.isFixed) {
                                b2.vel.x += p * b1.mass * nx
                                b2.vel.y += p * b1.mass * ny
                            }
                        }

                        CollisionMode.DESTROY -> {
                            toRemove.add(b1.id)
                            toRemove.add(b2.id)
                        }

                        CollisionMode.IGNORE -> {}
                    }
                }
            }
        }

        bodies.removeAll { toRemove.contains(it.id) }
    }

    /**
     * Pre-configured Sandbox Presets
     */
    fun getPresetScenario(presetId: String, canvasWidth: Float, canvasHeight: Float): List<SandboxBody> {
        val cx = canvasWidth / 2.0
        val cy = canvasHeight / 2.0

        return when (presetId) {
            "SOLAR_SYSTEM" -> listOf(
                SandboxBody("sun", "Sun", ObjectType.SUN, 3000.0, 26f, Vector2D(cx, cy), Vector2D(0.0, 0.0), isFixed = true),
                SandboxBody("mercury", "Mercury", ObjectType.PLANET, 0.1, 6f, Vector2D(cx + 60, cy), Vector2D(0.0, -122.0), color = Color(0xFFA3A3A3)),
                SandboxBody("venus", "Venus", ObjectType.PLANET, 0.85, 9f, Vector2D(cx + 100, cy), Vector2D(0.0, -95.0), color = Color(0xFFFFD166)),
                SandboxBody("earth", "Earth", ObjectType.PLANET, 1.0, 10f, Vector2D(cx + 150, cy), Vector2D(0.0, -77.0), color = Color(0xFF38BDF8)),
                SandboxBody("mars", "Mars", ObjectType.PLANET, 0.11, 7f, Vector2D(cx + 210, cy), Vector2D(0.0, -65.0), color = Color(0xFFEF4444)),
                SandboxBody("jupiter", "Jupiter", ObjectType.PLANET, 317.0, 20f, Vector2D(cx + 310, cy), Vector2D(0.0, -53.0), color = Color(0xFFF97316)),
                SandboxBody("saturn", "Saturn", ObjectType.PLANET, 95.0, 16f, Vector2D(cx + 410, cy), Vector2D(0.0, -46.0), color = Color(0xFFEAB308))
            )

            "EARTH_MOON" -> listOf(
                SandboxBody("earth", "Earth", ObjectType.PLANET, 1000.0, 22f, Vector2D(cx, cy), Vector2D(0.0, 0.0), color = Color(0xFF38BDF8), isFixed = true),
                SandboxBody("moon", "Moon", ObjectType.MOON, 12.0, 8f, Vector2D(cx + 160, cy), Vector2D(0.0, -43.0), color = Color(0xFFE2E8F0)),
                SandboxBody("satellite", "ISS Satellite", ObjectType.CUSTOM, 0.01, 4f, Vector2D(cx + 60, cy), Vector2D(0.0, -70.0), color = Color(0xFF10B981))
            )

            "JUPITER_MOONS" -> listOf(
                SandboxBody("jupiter", "Jupiter", ObjectType.PLANET, 2500.0, 24f, Vector2D(cx, cy), Vector2D(0.0, 0.0), color = Color(0xFFF97316), isFixed = true),
                SandboxBody("io", "Io", ObjectType.MOON, 0.8, 6f, Vector2D(cx + 80, cy), Vector2D(0.0, -96.0), color = Color(0xFFFACC15)),
                SandboxBody("europa", "Europa", ObjectType.MOON, 0.6, 5f, Vector2D(cx + 120, cy), Vector2D(0.0, -78.0), color = Color(0xFF38BDF8)),
                SandboxBody("ganymede", "Ganymede", ObjectType.MOON, 1.4, 7f, Vector2D(cx + 170, cy), Vector2D(0.0, -66.0), color = Color(0xFFA855F7)),
                SandboxBody("callisto", "Callisto", ObjectType.MOON, 1.1, 7f, Vector2D(cx + 230, cy), Vector2D(0.0, -57.0), color = Color(0xFF64748B))
            )

            "BINARY_STARS" -> listOf(
                SandboxBody("star1", "Alpha Star", ObjectType.SUN, 1800.0, 20f, Vector2D(cx - 90, cy), Vector2D(0.0, 52.0), color = Color(0xFF38BDF8)),
                SandboxBody("star2", "Beta Star", ObjectType.SUN, 1800.0, 20f, Vector2D(cx + 90, cy), Vector2D(0.0, -52.0), color = Color(0xFFF43F5E)),
                SandboxBody("planet", "Circumbinary World", ObjectType.PLANET, 2.0, 8f, Vector2D(cx + 280, cy), Vector2D(0.0, -62.0), color = Color(0xFF10B981))
            )

            "LAGRANGE_POINTS" -> listOf(
                SandboxBody("sun", "Sun", ObjectType.SUN, 2500.0, 24f, Vector2D(cx - 40, cy), Vector2D(0.0, 2.0), color = Color(0xFFFFD166)),
                SandboxBody("earth", "Earth", ObjectType.PLANET, 40.0, 10f, Vector2D(cx + 200, cy), Vector2D(0.0, -56.0), color = Color(0xFF38BDF8)),
                // L4 Trojan probe
                SandboxBody("l4_probe", "L4 Trojan Asteroid", ObjectType.ASTEROID, 0.01, 5f, Vector2D(cx + 80, cy - 200), Vector2D(48.0, -28.0), color = Color(0xFFF59E0B)),
                // L5 Trojan probe
                SandboxBody("l5_probe", "L5 Trojan Asteroid", ObjectType.ASTEROID, 0.01, 5f, Vector2D(cx + 80, cy + 200), Vector2D(-48.0, -28.0), color = Color(0xFF10B981))
            )

            "THREE_BODY_CHAOS" -> listOf(
                SandboxBody("body1", "Chaos Alpha", ObjectType.SUN, 1500.0, 18f, Vector2D(cx - 120, cy - 80), Vector2D(30.0, -20.0), color = Color(0xFFEF4444)),
                SandboxBody("body2", "Chaos Beta", ObjectType.SUN, 1500.0, 18f, Vector2D(cx + 120, cy - 80), Vector2D(-10.0, 40.0), color = Color(0xFF3B82F6)),
                SandboxBody("body3", "Chaos Gamma", ObjectType.SUN, 1500.0, 18f, Vector2D(cx, cy + 120), Vector2D(-20.0, -20.0), color = Color(0xFF10B981))
            )

            "SLINGSHOT" -> listOf(
                SandboxBody("jupiter", "Jupiter Mass", ObjectType.PLANET, 2800.0, 26f, Vector2D(cx, cy), Vector2D(0.0, 0.0), color = Color(0xFFF97316), isFixed = true),
                SandboxBody("voyager", "Voyager Probe", ObjectType.COMET, 0.001, 5f, Vector2D(cx - 280, cy + 120), Vector2D(75.0, -28.0), color = Color(0xFF38BDF8))
            )

            else -> getPresetScenario("SOLAR_SYSTEM", canvasWidth, canvasHeight)
        }
    }
}
