package com.alijafari.red.astronomy.astro_engine

/**
 * A small thread-safe least-recently-used cache for **pure** computations.
 *
 * Only use it where the cached value is a pure function of the key (and the key holds every input),
 * so a cached value is bit-identical to a fresh one. Data-class keys compare Doubles with
 * `Double.compare`, i.e. exactly (0.0 and -0.0 are different keys).
 *
 * The computation runs outside the lock. Two threads that miss the same key at the same moment may
 * both compute it; that is harmless for a pure computation, and the first value stored wins, so
 * every caller ends up holding the same instance.
 */
internal class BoundedCache<K : Any, V : Any>(private val maxEntries: Int) {

    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxEntries
    }

    fun getOrCompute(key: K, compute: () -> V): V {
        synchronized(map) { map[key] }?.let { return it }
        val value = compute()
        return synchronized(map) { map.getOrPut(key) { value } }
    }

    val size: Int
        get() = synchronized(map) { map.size }

    fun clear() {
        synchronized(map) { map.clear() }
    }
}
