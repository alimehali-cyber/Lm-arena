package com.zig.museum.core.engine

/**
 * MaterialManager — owns .filamat loading and runtime material building (temporary for M1)
 * Per §5.7: One material per visual family, compiled to .filamat and shipped precompiled via matc.
 * M1 skeleton: M1 regolithSurface albedo + normal, no displacement yet, built offline with matc and loaded from assets.
 *
 * For M1, we use filamat-android runtime MaterialBuilder as temporary placeholder (since matc binary not available in sandbox),
 * and record decision that it will be replaced by offline matc in M4 per DECISIONS.md.
 * API names used must be recorded in DECISIONS.md per §5.7.
 */

object MaterialManager {

    // Material definitions per §5.7
    const val M1_REGOLITH = "regolithSurface"
    const val M2_ICY = "icySurface"
    const val M3_GAS_GIANT = "gasGiantSurface"
    const val M4_CLOUD = "cloudDeck"
    const val M5_RING = "ringTransmission"
    const val M6_SOLAR = "solarSurface"
    const val M7_CORONA = "coronaShell"
    const val M8_NIGHT = "nightLights"
    const val M9_STAR = "starSprite"
    const val M10_ATMOSPHERE = "atmosphereShell"
    const val M11_BLACK_HOLE = "blackHoleLens"
    const val M12_MODEL = "modelSurface"
    const val M13_PATCH = "patchSurface"

    /**
     * M1 skeleton material source (for offline matc compilation)
     * This is the .mat file content that would be compiled via matc to .filamat
     * Per §8.1 regolithSurface: albedo + normal + height + AO + horizon-map shadows + optional displacement
     * For M1 skeleton: albedo + normal only, no displacement yet
     */
    val M1_MAT_SOURCE = """
        material {
            name : regolithSurface,
            shadingModel : lit,
            blending : opaque,
            parameters : [
                { type : sampler2d, name : albedoMap },
                { type : sampler2d, name : normalMap },
                { type : float, name : normalStrength }
            ],
            requires : [ uv0 ],
            variables : [ lightDir ]
        }

        fragment {
            void material(inout MaterialInputs material) {
                prepareMaterial(material);
                // Sample albedo (sRGB, perceptual)
                vec3 albedo = texture(materialParams_albedoMap, getUV0()).rgb;
                material.baseColor.rgb = albedo;
                material.baseColor.a = 1.0;

                // Sample normal map (two-channel, best fidelity)
                // Normal maps are linear, not sRGB
                vec3 normal = texture(materialParams_normalMap, getUV0()).xyz * 2.0 - 1.0;
                normal.xy *= materialParams.normalStrength;
                material.normal = normal;

                material.roughness = 0.9;
                material.metallic = 0.0;
                material.ao = 1.0;
            }
        }
    """.trimIndent()

    /**
     * Runtime material builder for M1 placeholder (uses filamat-android)
     * This violates T2 (no runtime compilation) but is temporary for M1 to get first object on screen.
     * Will be replaced by offline .filamat in M4.
     * Recorded in DECISIONS.md D-020.
     */
    fun buildM1Runtime(engine: Any): Any? {
        // Placeholder: actual implementation requires filamat-android MaterialBuilder
        // We return null for now, real implementation in M1 will use reflection or direct API
        // API names to record in DECISIONS.md per §5.7:
        // - com.google.android.filament.filamat.MaterialBuilder
        // - MaterialBuilder.platform(Platform.MOBILE)
        // - MaterialBuilder.name(String)
        // - MaterialBuilder.shading(Shading.LIT)
        // - MaterialBuilder.uniformParameter(UniformType.SAMPLER_2D, String)
        // - MaterialBuilder.material(String)
        // - MaterialBuilder.optimization(Optimization.NONE)
        // - MaterialBuilder.build(Engine)
        return null
    }

    /**
     * Load .filamat from assets (offline compiled via matc)
     * Per §5.7: keep .mat sources in core/engine/src/main/materials/ and build step in Gradle task graph
     */
    fun loadFilamatFromAssets(assetPath: String): ByteArray? {
        // Placeholder for M1: will read from assets via context.assets.open
        return null
    }
}
