package com.zig.museum.core.model

/**
 * Layer toggles per object (data-driven from registry) per M5 task 3
 */

data class LayerToggle(
    val id: String,
    val labelEn: String,
    val labelFa: String,
    val defaultEnabled: Boolean,
    val category: LayerCategory
)

enum class LayerCategory {
    DATA, // e.g., albedo, DEM, normal
    OVERLAY, // e.g., horizon shadows, atmosphere, clouds
    PROCEDURAL // e.g., micro-detail, procedural beyond data
}

object LayerToggles {

    // Per-object layer definitions
    val perObject: Map<String, List<LayerToggle>> = mapOf(
        "moon" to listOf(
            LayerToggle("wac_morphology", "WAC Morphology", "مورفولوژی WAC", true, LayerCategory.DATA),
            LayerToggle("lola_dem", "LOLA DEM", "DEM لولا", false, LayerCategory.DATA),
            LayerToggle("normal_map", "Normal Map", "نقشه نرمال", true, LayerCategory.DATA),
            LayerToggle("horizon_shadows", "Horizon Shadows", "سایه‌های افق", true, LayerCategory.OVERLAY),
            LayerToggle("ao", "Ambient Occlusion", "انسداد محیطی", true, LayerCategory.OVERLAY),
            LayerToggle("procedural", "Procedural Detail", "جزئیات رویه‌ای", false, LayerCategory.PROCEDURAL)
        ),
        "earth" to listOf(
            LayerToggle("bmng", "Blue Marble", "مرمر آبی", true, LayerCategory.DATA),
            LayerToggle("night_lights", "Night Lights", "نورهای شب", true, LayerCategory.OVERLAY),
            LayerToggle("clouds", "Clouds", "ابرها", true, LayerCategory.OVERLAY),
            LayerToggle("atmosphere", "Atmosphere", "جو", true, LayerCategory.OVERLAY),
            LayerToggle("bathymetry", "Bathymetry", "عمق‌سنجی", false, LayerCategory.DATA),
            LayerToggle("landsat", "Landsat Hero", "قهرمان لندست", false, LayerCategory.DATA)
        ),
        "mars" to listOf(
            LayerToggle("ctx_mosaic", "CTX Mosaic", "موزاییک CTX", true, LayerCategory.DATA),
            LayerToggle("hirise_patches", "HiRISE Patches", "تکه‌های HiRISE", false, LayerCategory.DATA),
            LayerToggle("dem", "DEM", "DEM", false, LayerCategory.DATA),
            LayerToggle("atmosphere", "Atmosphere", "جو", true, LayerCategory.OVERLAY),
            LayerToggle("horizon_shadows", "Horizon Shadows", "سایه‌های افق", true, LayerCategory.OVERLAY)
        )
        // Other objects use generic layers
    )

    fun forObject(objectId: String): List<LayerToggle> {
        return perObject[objectId.lowercase()] ?: listOf(
            LayerToggle("albedo", "Albedo", "آلبیدو", true, LayerCategory.DATA),
            LayerToggle("normal", "Normal", "نرمال", true, LayerCategory.DATA),
            LayerToggle("atmosphere", "Atmosphere", "جو", true, LayerCategory.OVERLAY)
        )
    }
}
