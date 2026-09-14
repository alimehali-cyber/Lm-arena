package com.zig.museum.core.data

/**
 * Budgets per §15.2
 * Resident GPU textures budget in bytes, upload budget per frame, frame budget ms.
 */
data class TierBudget(
    val maxResidentBytes: Long,
    val maxUploadsPerFrame: Int,
    val targetP95Ms: Float,
    val maxP95Ms: Float
)

enum class QualityTier(val id: Int) {
    TIER_0(0),
    TIER_1(1),
    TIER_2(2),
    TIER_3(3)
}

object TileStoreConfig {
    // Per §15.2
    val budgets: Map<QualityTier, TierBudget> = mapOf(
        QualityTier.TIER_0 to TierBudget(
            maxResidentBytes = (1.5 * 1024 * 1024 * 1024).toLong(), // 1.5 GB
            maxUploadsPerFrame = 2,
            targetP95Ms = 16.6f,
            maxP95Ms = 16.6f
        ),
        QualityTier.TIER_1 to TierBudget(
            maxResidentBytes = (900 * 1024 * 1024).toLong(),
            maxUploadsPerFrame = 1,
            targetP95Ms = 16.6f,
            maxP95Ms = 16.6f
        ),
        QualityTier.TIER_2 to TierBudget(
            maxResidentBytes = (500 * 1024 * 1024).toLong(),
            maxUploadsPerFrame = 1,
            targetP95Ms = 33f,
            maxP95Ms = 33f
        ),
        QualityTier.TIER_3 to TierBudget(
            maxResidentBytes = (250 * 1024 * 1024).toLong(),
            maxUploadsPerFrame = 1,
            targetP95Ms = 33f,
            maxP95Ms = 33f
        )
    )

    const val WORKER_THREADS = 3 // per §7.3
    const val TILE_SIZE = 512 // per §6.4
    const val APRON = 4 // per §6.4
    const val MAX_TILE_DIMENSION = 8192 // per T4, beyond must be tiled
}
