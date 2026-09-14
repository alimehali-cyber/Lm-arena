package com.zig.museum.core.model

/**
 * Camera presets per object (full disk, pole-on, terminator, hero region) per M5 task 6
 */

data class CameraPreset(
    val id: String,
    val labelEn: String,
    val labelFa: String,
    val radius: Float, // object radii
    val yawDeg: Float,
    val pitchDeg: Float,
    val targetX: Float = 0f,
    val targetY: Float = 0f,
    val targetZ: Float = 0f
)

object CameraPresets {

    val common = listOf(
        CameraPreset("full_disk", "Full Disk", "دیسک کامل", 2.5f, 0f, 0f),
        CameraPreset("pole_on", "Pole-On", "قطب‌نما", 2.5f, 0f, 89f),
        CameraPreset("terminator", "Terminator", "پایانه", 2.5f, 90f, 0f)
    )

    val perObject: Map<String, List<CameraPreset>> = mapOf(
        "moon" to (common + listOf(
            CameraPreset("apollo11", "Apollo 11", "آپولو ۱۱", 1.05f, 23.47f, 0.67f, 0.1f, 0.01f, 0.05f),
            CameraPreset("tycho", "Tycho Crater", "دهانه تایکو", 1.05f, -11.36f, -43.31f),
            CameraPreset("hadley", "Hadley Rille", "شیار هادلی", 1.05f, 3.0f, 26.0f)
        )),
        "mars" to (common + listOf(
            CameraPreset("olympus", "Olympus Mons", "کوه المپ", 1.05f, -133f, 18.6f),
            CameraPreset("valles", "Valles Marineris", "دره مارینر", 1.05f, -59f, -14f)
        )),
        "earth" to (common + listOf(
            CameraPreset("himalaya", "Himalaya", "هیمالیا", 1.2f, 86f, 28f),
            CameraPreset("sahara", "Sahara", "صحرای بزرگ", 1.2f, 13f, 23f)
        ))
    )

    fun forObject(objectId: String): List<CameraPreset> {
        return perObject[objectId.lowercase()] ?: common
    }
}
