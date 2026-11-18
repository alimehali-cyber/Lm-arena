package com.alijafari.red.astronomy.ui.satellites.core

internal data class ShapeRule(
    val id: String,
    val tier: Int,
    val norad: Set<Int>,
    val names: List<Regex>,
    val categories: Set<SatCategory>,
)

internal object SatShapeRegistry {
    private val byId = shapeRules.flatMap { r -> r.norad.map { it to r } }.toMap()
    private val byCategory = shapeRules.flatMap { r -> r.categories.map { it to r } }.toMap()

    fun resolve(norad: Int, name: String, category: SatCategory): ShapeRule =
        byId[norad]
            ?: shapeRules.firstOrNull { rule -> rule.names.any { it.containsMatchIn(name) } }
            ?: byCategory.getValue(category)
}
