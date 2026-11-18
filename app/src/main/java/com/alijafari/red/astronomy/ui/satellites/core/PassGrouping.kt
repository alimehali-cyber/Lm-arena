package com.alijafari.red.astronomy.ui.satellites.core

internal object PassGrouping {
    /** Stable start-time order. The caller supplies the observer's sunset/sunrise key, not UTC midnight. */
    fun <T> nights(sorted: List<T>, key: (T) -> Long) = sorted.groupBy(key)

    fun <T> rows(night: List<T>, category: (T) -> SatCategory, favorite: (T) -> Boolean): List<List<T>> {
        val eligible = night.filterNot {
            favorite(it) || category(it) in setOf(SatCategory.STATION, SatCategory.SCIENCE)
        }
        val bundles = eligible.groupBy(category).filterValues { it.size >= 4 }
        val emitted = mutableSetOf<SatCategory>()
        return buildList {
            for (p in night) {
                val c = category(p)
                val group = bundles[c]
                if (group == null || favorite(p)) add(listOf(p)) else if (emitted.add(c)) add(group)
            }
        }
    }

    fun <T> spotlight(items: List<T>, now: Long, start: (T) -> Long, end: (T) -> Long, score: (T) -> Int): T? {
        val good = items.filter { end(it) > now && score(it) >= 55 }.sortedBy(start)
        val first = good.firstOrNull() ?: return null
        if (start(first) <= now || score(first) >= 65) return first
        return good.firstOrNull { score(it) >= 65 && start(it) - start(first) <= 21_600_000L } ?: first
    }
}
