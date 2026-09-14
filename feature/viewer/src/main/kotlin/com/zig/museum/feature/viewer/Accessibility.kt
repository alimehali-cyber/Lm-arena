package com.zig.museum.feature.viewer

/**
 * Accessibility, localisation scaffolding, deep links / shareable state per M12 tasks 1-3
 * Accessibility: content descriptions for every control, scalable text, contrast checks, reduced-motion mode
 * Localisation scaffolding: English complete; strings externalised
 * Deep links / shareable state: show me Apollo 17, Sun in 304
 */

data class AccessibilityInfo(
    val contentDescription: String,
    val contentDescriptionFa: String,
    val scalableText: Boolean = true,
    val contrastRatio: Float = 4.5f, // WCAG AA
    val reducedMotionAlternative: String? = null
)

object AccessibilityRegistry {

    val controls = mapOf(
        "rotation_speed" to AccessibilityInfo(
            contentDescription = "Rotation speed control: hold, 1x, 60x, 3600x",
            contentDescriptionFa = "کنترل سرعت چرخش: نگه‌داشتن، 1 برابر، 60 برابر، 3600 برابر",
            scalableText = true,
            contrastRatio = 7.0f,
            reducedMotionAlternative = "Instant rotation change without animation"
        ),
        "sun_direction" to AccessibilityInfo(
            contentDescription = "Sun direction control: azimuth and elevation",
            contentDescriptionFa = "کنترل جهت خورشید: آزیموت و ارتفاع",
            scalableText = true,
            contrastRatio = 7.0f,
            reducedMotionAlternative = "Instant sun direction change"
        ),
        "layer_toggle" to AccessibilityInfo(
            contentDescription = "Layer toggle",
            contentDescriptionFa = "تغییر لایه",
            scalableText = true,
            contrastRatio = 4.5f
        ),
        "camera_preset" to AccessibilityInfo(
            contentDescription = "Camera preset: full disk, pole-on, terminator, hero region",
            contentDescriptionFa = "پیش‌تنظیم دوربین: دیسک کامل، قطب، سایه‌مرز، ناحیه قهرمان",
            scalableText = true,
            contrastRatio = 4.5f,
            reducedMotionAlternative = "Instant camera move without smooth interpolation"
        ),
        "tap_to_focus" to AccessibilityInfo(
            contentDescription = "Tap to focus on surface",
            contentDescriptionFa = "ضربه برای تمرکز روی سطح",
            scalableText = true,
            contrastRatio = 4.5f,
            reducedMotionAlternative = "Instant focus without smooth recentre"
        ),
        "data_hud" to AccessibilityInfo(
            contentDescription = "Data resolution indicator",
            contentDescriptionFa = "نشانگر وضوح داده",
            scalableText = true,
            contrastRatio = 7.0f
        ),
        "credits" to AccessibilityInfo(
            contentDescription = "Sources and credits",
            contentDescriptionFa = "منابع و اعتبارات",
            scalableText = true,
            contrastRatio = 7.0f
        )
    )

    fun reducedMotionEnabled(): Boolean {
        // Check system setting for reduced motion
        return false // placeholder, real would check AccessibilityManager or user setting
    }
}

data class DeepLink(
    val uri: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val objectId: String,
    val preset: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val layer: String? = null
)

object DeepLinkRegistry {

    val links = listOf(
        DeepLink(
            uri = "zig://museum/moon?preset=apollo11",
            descriptionEn = "Show me Apollo 11",
            descriptionFa = "آپولو 11 را نشان بده",
            objectId = "moon",
            preset = "apollo11",
            lat = 0.67416,
            lon = 23.47314
        ),
        DeepLink(
            uri = "zig://museum/moon?preset=apollo17",
            descriptionEn = "Show me Apollo 17",
            descriptionFa = "آپولو 17 را نشان بده",
            objectId = "moon",
            preset = "apollo17",
            lat = 20.1908,
            lon = 30.7717
        ),
        DeepLink(
            uri = "zig://museum/sun?mode=304&labelled=true",
            descriptionEn = "Sun in 304",
            descriptionFa = "خورشید در 304",
            objectId = "sun",
            layer = "aia_304"
        ),
        DeepLink(
            uri = "zig://museum/earth?preset=himalaya&month=7",
            descriptionEn = "Earth Himalaya July",
            descriptionFa = "زمین هیمالیا ژوئیه",
            objectId = "earth",
            preset = "himalaya"
        ),
        DeepLink(
            uri = "zig://museum/saturn?ring=true&sun=terminator",
            descriptionEn = "Saturn rings at terminator",
            descriptionFa = "حلقه‌های زحل در سایه‌مرز",
            objectId = "saturn"
        ),
        DeepLink(
            uri = "zig://museum/blackhole?mode=physically_correct&tier=tier0",
            descriptionEn = "Black hole physically correct tier0",
            descriptionFa = "سیاه‌چاله صحیح فیزیکی سطح 0",
            objectId = "blackhole"
        )
    )

    fun parse(uri: String): DeepLink? {
        return links.find { it.uri == uri }
    }
}
