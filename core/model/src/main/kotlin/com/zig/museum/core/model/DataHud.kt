package com.zig.museum.core.model

/**
 * Data HUD and procedural-detail indicator per §5.9 and M5 task 4
 * Driven by object's declared ceiling and resident tile level
 */

data class DataHudState(
    val displayedResolutionMpp: Double, // meters per pixel actually resident
    val ceilingMpp: Double, // object's declared ceiling (finest real data)
    val assetName: String, // e.g., "MESSENGER MDIS BDR"
    val isProceduralBeyond: Boolean, // true if beyond published data
    val isFallback: Boolean, // true if coarser than requested
    val requestedLevel: Int,
    val residentLevel: Int,
    val layerToggles: List<String> = emptyList()
) {
    fun formatEn(): String {
        val resText = when {
            displayedResolutionMpp < 1.0 -> String.format("%.2f m/px", displayedResolutionMpp)
            displayedResolutionMpp < 1000 -> String.format("%.1f m/px", displayedResolutionMpp)
            else -> String.format("%.2f km/px", displayedResolutionMpp / 1000.0)
        }
        val fallback = if (isFallback) " (fallback L$residentLevel)" else ""
        val proc = if (isProceduralBeyond) " — beyond published data, procedural detail" else ""
        return "DATA $resText ($assetName)$fallback$proc"
    }

    fun formatFa(): String {
        val resText = when {
            displayedResolutionMpp < 1.0 -> String.format("%.2f متر/پیکسل", displayedResolutionMpp)
            displayedResolutionMpp < 1000 -> String.format("%.1f متر/پیکسل", displayedResolutionMpp)
            else -> String.format("%.2f کیلومتر/پیکسل", displayedResolutionMpp / 1000.0)
        }
        val proc = if (isProceduralBeyond) " — فراتر از داده‌های منتشر شده، جزئیات رویه‌ای" else ""
        return "داده $resText ($assetName)$proc"
    }

    fun proceduralIndicatorVisible(): Boolean = isProceduralBeyond
}

object DataHud {

    /**
     * Compute HUD state from manifest ground resolution and resident level
     * @param baseResolutionMpp ground resolution at finest level L0
     * @param requestedLevel desired level (0 finest)
     * @param residentLevel actual resident level
     * @param ceilingMpp object's declared ceiling (finest real data)
     */
    fun compute(
        baseResolutionMpp: Double,
        requestedLevel: Int,
        residentLevel: Int,
        ceilingMpp: Double,
        assetName: String,
        layerToggles: List<String> = emptyList()
    ): DataHudState {
        val requestedRes = baseResolutionMpp * (1 shl requestedLevel)
        val residentRes = baseResolutionMpp * (1 shl residentLevel)
        val isFallback = residentLevel > requestedLevel
        val procedural = residentRes > ceilingMpp * 1.1
        return DataHudState(
            displayedResolutionMpp = residentRes,
            ceilingMpp = ceilingMpp,
            assetName = assetName,
            isProceduralBeyond = procedural,
            isFallback = isFallback,
            requestedLevel = requestedLevel,
            residentLevel = residentLevel,
            layerToggles = layerToggles
        )
    }
}
