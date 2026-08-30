package com.alijafari.red.astronomy.sandbox.render.celestial

import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType

/**
 * Visual and astronomical rendering characteristics for celestial bodies in the ZIG Sandbox.
 * These parameters govern visual appearance (textures, axial tilts, atmospheres, rings, albedos)
 * while preserving SI physical simulation properties intact.
 */
data class CelestialBodyConfig(
    val bodyType: SandboxBodyType,
    val axialTiltDegrees: Float,
    val siderealRotationPeriodHours: Float,
    val hasAtmosphere: Boolean = false,
    val atmosphereColor: FloatArray = floatArrayOf(0.3f, 0.6f, 1.0f, 1.0f),
    val atmosphereScale: Float = 1.035f,
    val atmosphereDensity: Float = 1.0f,
    val hasClouds: Boolean = false,
    val cloudScale: Float = 1.015f,
    val cloudRotationMultiplier: Float = 1.12f,
    val hasRings: Boolean = false,
    val ringInnerRadiusFactor: Float = 1.25f,
    val ringOuterRadiusFactor: Float = 2.35f,
    val specularIntensity: Float = 0.2f,
    val shininess: Float = 16.0f,
    val roughness: Float = 0.5f,
    val baseAlbedoColor: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CelestialBodyConfig
        return bodyType == other.bodyType
    }

    override fun hashCode(): Int = bodyType.hashCode()
}

/**
 * Registry of accurate celestial rendering parameters.
 */
object CelestialPropertiesRegistry {

    private val configs: Map<SandboxBodyType, CelestialBodyConfig> = mapOf(
        SandboxBodyType.SUN to CelestialBodyConfig(
            bodyType = SandboxBodyType.SUN,
            axialTiltDegrees = 7.25f,
            siderealRotationPeriodHours = 600.0f, // Differential ~25-35 days
            hasAtmosphere = false,
            hasClouds = false,
            specularIntensity = 0.0f,
            shininess = 1.0f,
            baseAlbedoColor = floatArrayOf(1.00f, 0.88f, 0.45f, 1.0f)
        ),
        SandboxBodyType.MERCURY to CelestialBodyConfig(
            bodyType = SandboxBodyType.MERCURY,
            axialTiltDegrees = 0.034f,
            siderealRotationPeriodHours = 1407.6f, // 58.6 days
            hasAtmosphere = false,
            hasClouds = false,
            specularIntensity = 0.05f,
            shininess = 4.0f,
            roughness = 0.95f,
            baseAlbedoColor = floatArrayOf(0.72f, 0.70f, 0.68f, 1.0f)
        ),
        SandboxBodyType.VENUS to CelestialBodyConfig(
            bodyType = SandboxBodyType.VENUS,
            axialTiltDegrees = 177.36f, // Retrograde
            siderealRotationPeriodHours = -5832.5f, // -243 days (retrograde)
            hasAtmosphere = true,
            atmosphereColor = floatArrayOf(0.92f, 0.82f, 0.55f, 1.0f),
            atmosphereScale = 1.045f,
            atmosphereDensity = 2.5f,
            hasClouds = false, // Venus surface is entirely obscured by its sulfuric cloud deck
            specularIntensity = 0.15f,
            shininess = 12.0f,
            roughness = 0.35f,
            baseAlbedoColor = floatArrayOf(0.95f, 0.85f, 0.58f, 1.0f)
        ),
        SandboxBodyType.EARTH to CelestialBodyConfig(
            bodyType = SandboxBodyType.EARTH,
            axialTiltDegrees = 23.44f,
            siderealRotationPeriodHours = 23.934f,
            hasAtmosphere = true,
            atmosphereColor = floatArrayOf(0.35f, 0.68f, 1.00f, 1.0f),
            atmosphereScale = 1.032f,
            atmosphereDensity = 1.0f,
            hasClouds = true,
            cloudScale = 1.012f,
            cloudRotationMultiplier = 1.08f,
            specularIntensity = 0.85f, // High specular glint on oceans
            shininess = 64.0f,
            roughness = 0.25f,
            baseAlbedoColor = floatArrayOf(0.20f, 0.55f, 0.95f, 1.0f)
        ),
        SandboxBodyType.MOON to CelestialBodyConfig(
            bodyType = SandboxBodyType.MOON,
            axialTiltDegrees = 1.54f,
            siderealRotationPeriodHours = 655.7f, // Synchronous rotation ~27.3 days
            hasAtmosphere = false,
            hasClouds = false,
            specularIntensity = 0.04f,
            shininess = 4.0f,
            roughness = 0.92f,
            baseAlbedoColor = floatArrayOf(0.75f, 0.74f, 0.72f, 1.0f)
        ),
        SandboxBodyType.MARS to CelestialBodyConfig(
            bodyType = SandboxBodyType.MARS,
            axialTiltDegrees = 25.19f,
            siderealRotationPeriodHours = 24.623f,
            hasAtmosphere = true,
            atmosphereColor = floatArrayOf(0.85f, 0.55f, 0.40f, 1.0f),
            atmosphereScale = 1.020f,
            atmosphereDensity = 0.35f,
            hasClouds = false,
            specularIntensity = 0.08f,
            shininess = 8.0f,
            roughness = 0.85f,
            baseAlbedoColor = floatArrayOf(0.88f, 0.42f, 0.24f, 1.0f)
        ),
        SandboxBodyType.JUPITER to CelestialBodyConfig(
            bodyType = SandboxBodyType.JUPITER,
            axialTiltDegrees = 3.13f,
            siderealRotationPeriodHours = 9.925f, // Rapid rotation
            hasAtmosphere = true,
            atmosphereColor = floatArrayOf(0.85f, 0.70f, 0.50f, 1.0f),
            atmosphereScale = 1.025f,
            atmosphereDensity = 1.2f,
            hasClouds = false,
            specularIntensity = 0.12f,
            shininess = 16.0f,
            roughness = 0.4f,
            baseAlbedoColor = floatArrayOf(0.90f, 0.72f, 0.48f, 1.0f)
        ),
        SandboxBodyType.SATURN to CelestialBodyConfig(
            bodyType = SandboxBodyType.SATURN,
            axialTiltDegrees = 26.73f,
            siderealRotationPeriodHours = 10.7f,
            hasAtmosphere = true,
            atmosphereColor = floatArrayOf(0.90f, 0.82f, 0.58f, 1.0f),
            atmosphereScale = 1.025f,
            atmosphereDensity = 1.1f,
            hasClouds = false,
            hasRings = true,
            ringInnerRadiusFactor = 1.22f,
            ringOuterRadiusFactor = 2.38f,
            specularIntensity = 0.10f,
            shininess = 16.0f,
            roughness = 0.45f,
            baseAlbedoColor = floatArrayOf(0.92f, 0.84f, 0.62f, 1.0f)
        ),
        SandboxBodyType.URANUS to CelestialBodyConfig(
            bodyType = SandboxBodyType.URANUS,
            axialTiltDegrees = 97.77f, // Extreme sideways tilt
            siderealRotationPeriodHours = -17.24f, // Retrograde
            hasAtmosphere = true,
            atmosphereColor = floatArrayOf(0.48f, 0.82f, 0.88f, 1.0f),
            atmosphereScale = 1.035f,
            atmosphereDensity = 1.3f,
            hasClouds = false,
            specularIntensity = 0.25f,
            shininess = 32.0f,
            roughness = 0.3f,
            baseAlbedoColor = floatArrayOf(0.55f, 0.85f, 0.90f, 1.0f)
        ),
        SandboxBodyType.NEPTUNE to CelestialBodyConfig(
            bodyType = SandboxBodyType.NEPTUNE,
            axialTiltDegrees = 28.32f,
            siderealRotationPeriodHours = 16.11f,
            hasAtmosphere = true,
            atmosphereColor = floatArrayOf(0.18f, 0.45f, 0.95f, 1.0f),
            atmosphereScale = 1.035f,
            atmosphereDensity = 1.4f,
            hasClouds = false,
            specularIntensity = 0.30f,
            shininess = 36.0f,
            roughness = 0.3f,
            baseAlbedoColor = floatArrayOf(0.22f, 0.52f, 0.96f, 1.0f)
        ),
        SandboxBodyType.ASTEROID to CelestialBodyConfig(
            bodyType = SandboxBodyType.ASTEROID,
            axialTiltDegrees = 10.0f,
            siderealRotationPeriodHours = 9.07f,
            hasAtmosphere = false,
            hasClouds = false,
            specularIntensity = 0.05f,
            shininess = 4.0f,
            roughness = 0.95f,
            baseAlbedoColor = floatArrayOf(0.55f, 0.58f, 0.60f, 1.0f)
        ),
        SandboxBodyType.CUSTOM_BODY to CelestialBodyConfig(
            bodyType = SandboxBodyType.CUSTOM_BODY,
            axialTiltDegrees = 0.0f,
            siderealRotationPeriodHours = 24.0f,
            hasAtmosphere = false,
            hasClouds = false,
            specularIntensity = 0.2f,
            shininess = 16.0f,
            roughness = 0.5f,
            baseAlbedoColor = floatArrayOf(0.85f, 0.85f, 0.85f, 1.0f)
        )
    )

    fun getConfig(type: SandboxBodyType): CelestialBodyConfig {
        return configs[type] ?: configs[SandboxBodyType.CUSTOM_BODY]!!
    }
}
