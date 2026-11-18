package com.alijafari.red.astronomy.ui.satellites.core

import java.text.Normalizer
import java.util.Locale

internal enum class SatCategory {
    STATION,
    SCIENCE,
    STARLINK,
    OTHER_CONSTELLATION,
    NAVIGATION,
    WEATHER_EO,
    COMMS,
    AMATEUR_CUBESAT,
    DEBRIS,
    OTHER;

    companion object {
        fun fromGroup(group: String): SatCategory =
            when (group.lowercase(Locale.ROOT)) {
                "stations" -> STATION
                "science" -> SCIENCE
                "starlink" -> STARLINK
                "oneweb",
                "iridium",
                "iridium-next",
                "globalstar" -> OTHER_CONSTELLATION
                "gnss",
                "gps-ops",
                "glo-ops",
                "galileo",
                "beidou" -> NAVIGATION
                "weather",
                "noaa",
                "resource",
                "earth-resources",
                "goes" -> WEATHER_EO
                "geo",
                "satnogs-comms",
                "intelsat",
                "ses" -> COMMS
                "amateur",
                "cubesat" -> AMATEUR_CUBESAT
                "debris" -> DEBRIS
                else -> OTHER
            }
    }
}

internal enum class Orbit {
    LEO,
    MEO,
    GEO,
}

internal enum class SatSort {
    HORIZON,
    NAME,
    ANGLE,
    NEXT_PASS,
    BRIGHTNESS,
}

internal data class Craft(
    val id: String,
    val norad: Int,
    val name: String,
    val persian: String,
    val cospar: String,
    val category: SatCategory,
    val nakedEye: Boolean,
    val orbit: Orbit?,
    val aliases: List<String> = emptyList(),
)

internal object SearchNormalizer {
    private val marks = Regex("\\p{M}+")
    private val spaces = Regex("[^\\p{L}\\p{N}]+")

    fun normalize(text: String): String {
        val s = Normalizer.normalize(text, Normalizer.Form.NFKD).lowercase(Locale.ROOT).replace(marks, "")
        return buildString(s.length) {
                for (c in s) when (c) {
                    'ـ' -> Unit
                    'ي',
                    'ى' -> append('ی')
                    'ك' -> append('ک')
                    'ة' -> append('ه')
                    in '۰'..'۹' -> append('0' + (c - '۰'))
                    in '٠'..'٩' -> append('0' + (c - '٠'))
                    else -> append(c)
                }
            }
            .replace(spaces, " ")
            .trim()
    }
}

internal class SearchIndex(catalogue: List<Craft>) {
    private val entries = catalogue.map { c ->
        c to
            SearchNormalizer.normalize(
                    (listOf(c.name, c.persian, c.norad.toString(), c.cospar) + c.aliases).joinToString(" "),
                )
                .split(' ')
    }

    fun search(raw: String): List<Craft> {
        val terms = SearchNormalizer.normalize(raw).split(' ')
        return entries.filter { (_, words) -> terms.all { term -> words.any { it.startsWith(term) } } }.map { it.first }
    }
}

internal data class Rules(
    val categories: Set<SatCategory> = emptySet(),
    val nameContains: String = "",
    val orbit: Orbit? = null,
    val nakedEyeOnly: Boolean = false,
) {
    fun matches(c: Craft): Boolean =
        (categories.isEmpty() || c.category in categories) &&
            (!nakedEyeOnly || c.nakedEye) &&
            (orbit == null || c.orbit == orbit) &&
            (nameContains.isBlank() ||
                SearchNormalizer.normalize(c.name + " " + c.persian).contains(SearchNormalizer.normalize(nameContains)))
}

internal data class CollectionDef(
    val id: String,
    val name: String,
    val glyph: Int = 0,
    val members: Set<String> = emptySet(),
    val rules: Rules? = null,
)

internal data class Library(
    val custom: List<CollectionDef> = emptyList(),
    val order: List<String> = builtIns,
    val hidden: Set<String> = hiddenInitially,
    val favorites: Set<String> = emptySet(),
    val active: String = ALL,
    val sort: SatSort = SatSort.HORIZON,
    val recent: List<String> = emptyList(),
) {
    fun accepts(id: String, c: Craft, above: Boolean): Boolean =
        when (id) {
            ALL -> true
            VISIBLE -> above
            FAVORITES -> c.id in favorites
            else ->
                if (id == c.category.name) true
                else custom.firstOrNull { it.id == id }?.let { it.rules?.matches(c) ?: (c.id in it.members) } ?: false
        }

    fun toggleFavorite(id: String) = copy(favorites = if (id in favorites) favorites - id else favorites + id)

    fun delete(id: String) =
        if (custom.none { it.id == id }) this
        else
            copy(
                custom = custom.filterNot { it.id == id },
                order = order - id,
                hidden = hidden - id,
                active = if (active == id) ALL else active,
            )

    companion object {
        const val ALL = "all"
        const val VISIBLE = "visible"
        const val FAVORITES = "favorites"
        val builtIns =
            listOf(ALL, VISIBLE, FAVORITES) +
                listOf(
                        SatCategory.STATION,
                        SatCategory.SCIENCE,
                        SatCategory.STARLINK,
                        SatCategory.NAVIGATION,
                        SatCategory.WEATHER_EO,
                        SatCategory.OTHER_CONSTELLATION,
                        SatCategory.COMMS,
                        SatCategory.AMATEUR_CUBESAT,
                        SatCategory.DEBRIS,
                        SatCategory.OTHER,
                    )
                    .map { it.name }
        val hiddenInitially = builtIns.drop(8).toSet()
    }
}
