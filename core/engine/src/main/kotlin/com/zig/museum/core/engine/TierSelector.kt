package com.zig.museum.core.engine

/**
 * Automatic tier selection from device capability and thermal status, with Settings override per M11 task 1
 * Device matrix: minimum two Adreno, two Mali, one Xclipse class device if obtainable; record driver strings and any artifacts
 * Benchmark path for every object at every tier: CSV of frame times, resident bytes, peak temperatures where available
 * Fix all budget violations; where fix impossible, record exception in DECISIONS.md with measurements
 */

enum class GpuFamily { ADRENO, MALI, XCLIPSE, OTHER }

data class DeviceCapability(
    val gpuFamily: GpuFamily,
    val gpuModel: String,
    val driverString: String,
    val totalMemoryMB: Int,
    val supportsAstc: Boolean,
    val supportsHalfFloat: Boolean,
    val maxTextureSize: Int
)

data class ThermalStatus(
    val temperatureC: Float,
    val throttling: Boolean,
    val thermalLevel: Int // 0 cool, 1 warm, 2 hot, 3 critical
)

enum class QualityTier(val budgetMB: Int, val frameBudgetMs: Float) {
    TIER0(1536, 16.6f), // 60fps flagship
    TIER1(900, 16.6f),
    TIER2(500, 33.3f), // 30fps mid-range
    TIER3(250, 33.3f)
}

object TierSelector {

    fun selectTier(
        capability: DeviceCapability,
        thermal: ThermalStatus,
        userOverride: QualityTier? = null
    ): QualityTier {
        if (userOverride != null) return userOverride

        // Thermal collapse: if hot, drop tier
        if (thermal.thermalLevel >= 3) return QualityTier.TIER3
        if (thermal.thermalLevel == 2) return QualityTier.TIER2

        // Device capability based selection
        return when (capability.gpuFamily) {
            GpuFamily.ADRENO -> {
                when {
                    capability.totalMemoryMB >= 8000 && capability.maxTextureSize >= 16384 -> QualityTier.TIER0
                    capability.totalMemoryMB >= 4000 -> QualityTier.TIER1
                    capability.totalMemoryMB >= 2000 -> QualityTier.TIER2
                    else -> QualityTier.TIER3
                }
            }
            GpuFamily.MALI -> {
                when {
                    capability.totalMemoryMB >= 8000 -> QualityTier.TIER0
                    capability.totalMemoryMB >= 4000 -> QualityTier.TIER1
                    else -> QualityTier.TIER2
                }
            }
            GpuFamily.XCLIPSE -> QualityTier.TIER0
            GpuFamily.OTHER -> QualityTier.TIER2
        }
    }

    fun benchmarkCsvHeader(): String = "objectId,tier,frameTimeMs,p50Ms,p95Ms,maxMs,fps,residentBytesMB,peakTempC,thermalLevel,gpuFamily,gpuModel,driver"

    fun benchmarkCsvRow(
        objectId: String,
        tier: QualityTier,
        frameTimeMs: Float,
        p50Ms: Float,
        p95Ms: Float,
        maxMs: Float,
        residentBytesMB: Float,
        peakTempC: Float,
        thermal: ThermalStatus,
        capability: DeviceCapability
    ): String {
        val fps = 1000f / frameTimeMs
        return "$objectId,${tier.name},$frameTimeMs,$p50Ms,$p95Ms,$maxMs,$fps,$residentBytesMB,$peakTempC,${thermal.thermalLevel},${capability.gpuFamily},${capability.gpuModel},${capability.driverString}"
    }

    fun deviceMatrix(): List<DeviceCapability> {
        return listOf(
            DeviceCapability(GpuFamily.ADRENO, "Adreno 750", "OpenGL ES 3.2 V@0600.0", 12000, true, true, 16384),
            DeviceCapability(GpuFamily.ADRENO, "Adreno 740", "OpenGL ES 3.2 V@0590.0", 8000, true, true, 16384),
            DeviceCapability(GpuFamily.MALI, "Mali-G720", "OpenGL ES 3.2 v1.r44p0", 12000, true, true, 8192),
            DeviceCapability(GpuFamily.MALI, "Mali-G710", "OpenGL ES 3.2 v1.r40p0", 8000, true, true, 8192),
            DeviceCapability(GpuFamily.XCLIPSE, "Xclipse 940", "OpenGL ES 3.2", 12000, true, true, 16384)
        )
    }
}
