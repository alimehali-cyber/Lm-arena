package com.zig.museum.core.data

/**
 * Maps resident tile levels to HUD display per §5.9 and M3 DoD.
 * HUD's displayed resolution tracks level actually resident.
 */
object DataHudMapper {
    data class HudState(
        val displayedResolutionMpp: Double, // meters per pixel
        val requestedLevel: Int,
        val residentLevel: Int,
        val isFallback: Boolean,
        val proceduralBeyond: Boolean,
        val assetName: String
    )

    /**
     * Compute HUD state from manifest ground resolution and resident level.
     * @param baseResolutionMpp ground resolution at finest level (L0)
     * @param requestedLevel desired level (0 finest)
     * @param residentLevel actual resident level (may be coarser if fallback)
     * @param ceilingMpp object's declared ceiling (finest real data)
     */
    fun compute(
        baseResolutionMpp: Double,
        requestedLevel: Int,
        residentLevel: Int,
        ceilingMpp: Double,
        assetName: String
    ): HudState {
        // Each level halves resolution (coarser = *2)
        val requestedRes = baseResolutionMpp * (1 shl requestedLevel)
        val residentRes = baseResolutionMpp * (1 shl residentLevel)
        val isFallback = residentLevel > requestedLevel
        val procedural = residentRes > ceilingMpp * 1.1 // beyond published data
        return HudState(
            displayedResolutionMpp = residentRes,
            requestedLevel = requestedLevel,
            residentLevel = residentLevel,
            isFallback = isFallback,
            proceduralBeyond = procedural,
            assetName = assetName
        )
    }

    fun format(hud: HudState): String {
        val resText = when {
            hud.displayedResolutionMpp < 1.0 -> String.format("%.2f m/px", hud.displayedResolutionMpp)
            hud.displayedResolutionMpp < 1000 -> String.format("%.1f m/px", hud.displayedResolutionMpp)
            else -> String.format("%.2f km/px", hud.displayedResolutionMpp / 1000.0)
        }
        val fallback = if (hud.isFallback) " (fallback L${hud.residentLevel})" else ""
        val proc = if (hud.proceduralBeyond) " — beyond published data, procedural detail" else ""
        return "DATA $resText (${hud.assetName})$fallback$proc"
    }
}
