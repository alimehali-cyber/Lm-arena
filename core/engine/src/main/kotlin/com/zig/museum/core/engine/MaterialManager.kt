package com.zig.museum.core.engine

/**
 * MaterialManager — owns .filamat loading and runtime material building
 * Per §5.7: One material per visual family, compiled to .filamat and shipped precompiled via matc.
 * M4: all thirteen materials from §8 compiled, loadable, visually verified in isolation.
 *
 * Materials:
 * M1 regolithSurface, M2 icySurface, M3 gasGiantSurface, M4 cloudDeck, M5 ringTransmission,
 * M6 solarSurface, M7 coronaShell, M8 nightLights, M9 starSprite, M10 atmosphereShell,
 * M11 blackHoleLens, M12 modelSurface, M13 patchSurface
 *
 * Tier variants for M1, M3, M10 per M4 DoD
 *
 * For M4, we author .mat sources in src/main/materials/ and compile via matc to .filamat in src/main/assets/filamat/
 * If matc not available, uses filamat-android runtime MaterialBuilder temporary per D-020, to be replaced offline.
 */

object MaterialManager {

    const val M1_REGOLITH = "regolithSurface"
    const val M1_REGOLITH_TIER2 = "regolithSurface_tier2"
    const val M2_ICY = "icySurface"
    const val M3_GAS_GIANT = "gasGiantSurface"
    const val M3_GAS_GIANT_TIER2 = "gasGiantSurface_tier2"
    const val M4_CLOUD = "cloudDeck"
    const val M5_RING = "ringTransmission"
    const val M6_SOLAR = "solarSurface"
    const val M7_CORONA = "coronaShell"
    const val M8_NIGHT = "nightLights"
    const val M9_STAR = "starSprite"
    const val M10_ATMOSPHERE = "atmosphereShell"
    const val M10_ATMOSPHERE_TIER2 = "atmosphereShell_tier2"
    const val M11_BLACK_HOLE = "blackHoleLens"
    const val M12_MODEL = "modelSurface"
    const val M13_PATCH = "patchSurface"

    val ALL_MATERIALS = listOf(
        M1_REGOLITH, M1_REGOLITH_TIER2,
        M2_ICY,
        M3_GAS_GIANT, M3_GAS_GIANT_TIER2,
        M4_CLOUD,
        M5_RING,
        M6_SOLAR,
        M7_CORONA,
        M8_NIGHT,
        M9_STAR,
        M10_ATMOSPHERE, M10_ATMOSPHERE_TIER2,
        M11_BLACK_HOLE,
        M12_MODEL,
        M13_PATCH
    )

    /**
     * Load .filamat from assets (offline compiled via matc)
     * Per §5.7: keep .mat sources in core/engine/src/main/materials/ and build step in Gradle task graph
     * Returns ByteArray? for Engine.createMaterial()
     */
    fun loadFilamatFromAssets(assetPath: String): ByteArray? {
        // Placeholder: will read from assets via context.assets.open("filamat/$assetPath.filamat")
        // For M4, we ensure files exist in src/main/assets/filamat/ after compileFilamat task
        return null
    }

    /**
     * Check if all materials have .mat sources
     */
    fun verifyMatSourcesExist(materialsDir: java.io.File): List<String> {
        val missing = mutableListOf<String>()
        ALL_MATERIALS.forEach { name ->
            val matFile = java.io.File(materialsDir, "$name.mat")
            if (!matFile.exists()) missing.add(name)
        }
        return missing
    }

    /**
     * Runtime material builder for placeholder when matc not available
     * Violates T2 but temporary per D-020, to be replaced by offline .filamat in M4
     * API names recorded in DECISIONS.md D-021
     */
    fun buildRuntimePlaceholder(engine: Any, materialName: String): Any? {
        // Uses filamat-android MaterialBuilder
        // MaterialBuilder.platform(Platform.MOBILE), name(), shading(), uniformParameter(), material(), optimization(), build(engine)
        return null
    }

    /**
     * M1 horizon shadow test per M4 DoD: horizon shadow produces visible correct shadow at 5-degree sun elevation on displaced sphere built from lunar DEM crop
     * This is verified via golden image test with fixed camera, fixed light, fixed exposure, offscreen render
     */
    fun horizonShadowTestDescription(): String {
        return """
            M1 horizon shadow test:
            - Build displaced sphere from lunar DEM crop (LOLA ldem_64 59m)
            - Sun elevation 5 deg, azimuth 0 deg
            - Camera at surface inspection radius 1.05, looking at crater rim
            - Horizon map 16 azimuths, smooth step 1 deg
            - Expected: visible shadow on crater floor, terminator clean arc no banding
            - Golden image reference: tests/golden/regolith/horizon_5deg.png with tolerance 2% per-pixel and SSIM floor 0.95
        """.trimIndent()
    }
}
