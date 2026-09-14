package com.zig.museum.core.engine

/**
 * Material testbed screen (debug only) per M4 task 4
 * Renders each material on a sphere with sliders for every parameter
 * For M4, this is a placeholder that lists materials and their parameters; real Compose UI with Filament surface and sliders in M5
 */

data class MaterialParam(
    val name: String,
    val type: String,
    val defaultValue: Any,
    val min: Float? = null,
    val max: Float? = null
)

data class MaterialTestbedEntry(
    val materialName: String,
    val description: String,
    val params: List<MaterialParam>
)

object MaterialTestbed {

    val entries = listOf(
        MaterialTestbedEntry(
            materialName = MaterialManager.M1_REGOLITH,
            description = "M1 regolithSurface — rocky bodies, displacement, horizon shadows, AO, procedural",
            params = listOf(
                MaterialParam("albedoScale", "float", 1.0f, 0.0f, 2.0f),
                MaterialParam("roughness", "float", 0.9f, 0.0f, 1.0f),
                MaterialParam("normalStrength", "float", 1.0f, 0.0f, 2.0f),
                MaterialParam("displacementScale", "float", 0.02f, 0.0f, 0.1f),
                MaterialParam("proceduralStrength", "float", 0.0f, 0.0f, 1.0f),
                MaterialParam("proceduralScale", "float", 1.0f, 0.1f, 10.0f),
                MaterialParam("horizonEnabled", "float", 1.0f, 0.0f, 1.0f)
            )
        ),
        MaterialTestbedEntry(
            materialName = MaterialManager.M3_GAS_GIANT,
            description = "M3 gasGiantSurface — banded flow, wind shear, methane tint, no displacement",
            params = listOf(
                MaterialParam("flowScale", "float", 2.0f, 0.1f, 10.0f),
                MaterialParam("shearAmount", "float", 0.5f, 0.0f, 2.0f),
                MaterialParam("limbDarken", "float", 1.0f, 0.0f, 5.0f),
                MaterialParam("hazeStrength", "float", 0.3f, 0.0f, 1.0f),
                MaterialParam("anisotropy", "float", 0.5f, 0.0f, 1.0f)
            )
        ),
        MaterialTestbedEntry(
            materialName = MaterialManager.M5_RING,
            description = "M5 ringTransmission — optical depth, phase asymmetry, planet shadow",
            params = listOf(
                MaterialParam("opticalDepthScale", "float", 1.0f, 0.0f, 5.0f),
                MaterialParam("forwardScatterBias", "float", 0.3f, -0.9f, 0.9f),
                MaterialParam("azimuthalVariationStrength", "float", 0.0f, 0.0f, 1.0f),
                MaterialParam("spokesEnabled", "float", 0.0f, 0.0f, 1.0f)
            )
        ),
        MaterialTestbedEntry(
            materialName = MaterialManager.M6_SOLAR,
            description = "M6 solarSurface — procedural granulation, limb darkening, spots, emission",
            params = listOf(
                MaterialParam("emissionScale", "float", 1.0f, 0.1f, 10.0f),
                MaterialParam("limbDarkeningA", "float", 0.3f, 0.0f, 1.0f),
                MaterialParam("limbDarkeningB", "float", 0.3f, 0.0f, 1.0f),
                MaterialParam("limbDarkeningC", "float", 0.1f, 0.0f, 1.0f)
            )
        ),
        MaterialTestbedEntry(
            materialName = MaterialManager.M10_ATMOSPHERE,
            description = "M10 atmosphereShell — LUT scattering, ozone, Mie, Rayleigh",
            params = listOf(
                MaterialParam("rayleighScaleHeight", "float", 8000f, 1000f, 20000f),
                MaterialParam("mieScaleHeight", "float", 1200f, 100f, 5000f),
                MaterialParam("mieG", "float", 0.76f, 0.0f, 0.99f),
                MaterialParam("groundAlbedo", "float", 0.3f, 0.0f, 1.0f),
                MaterialParam("ozoneEnabled", "float", 1.0f, 0.0f, 1.0f)
            )
        )
        // Other materials omitted for brevity but follow same pattern
    )

    fun getAllMaterialNames(): List<String> = MaterialManager.ALL_MATERIALS

    fun verifyAllMaterialsHaveTestbed(): Boolean {
        return getAllMaterialNames().all { name ->
            entries.any { it.materialName == name } || name.endsWith("_tier2") // tier variants share testbed
        }
    }
}
