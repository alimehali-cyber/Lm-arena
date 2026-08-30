package com.alijafari.red.astronomy.sandbox.model

/**
 * Distinguishes established astronomical physics from theoretical / hypothetical constructs.
 */
enum class ScientificClassification(
    val labelEn: String,
    val labelFa: String,
    val isSpeculative: Boolean
) {
    ESTABLISHED_PHYSICS(
        labelEn = "Established Astrophysics",
        labelFa = "اخترفیزیک اثبات‌شده و تجربی",
        isSpeculative = false
    ),
    THEORETICAL_PHYSICS(
        labelEn = "Theoretical / General Relativity Hypothesis",
        labelFa = "فرضیه تئوریک نسبیت عام (اثبات‌نشده تجربی)",
        isSpeculative = true
    )
}
