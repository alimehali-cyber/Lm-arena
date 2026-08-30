package com.alijafari.red.astronomy.sandbox.presets

import com.alijafari.red.astronomy.sandbox.model.SandboxBody
import com.alijafari.red.astronomy.sandbox.model.ScientificClassification

/**
 * Data-driven definition of an orbital or theoretical simulation scenario.
 */
data class SandboxPreset(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val subtitleEn: String,
    val subtitleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val classification: ScientificClassification = ScientificClassification.ESTABLISHED_PHYSICS,
    val recommendedBaseTimestepSeconds: Double = 60.0,
    val recommendedTimeSpeedMultiplier: Double = 1.0,
    val cameraTargetDistanceMeters: Double = 3.0e11,
    val bodyFactory: () -> List<SandboxBody>
)
