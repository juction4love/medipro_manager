package com.medipro.manager.core.search

class SearchResultCache<T>(private val maxSize: Int = SearchScore.CACHE_MAX_ENTRIES) {
    private val map = object : LinkedHashMap<String, T>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, T>?): Boolean =
            size > maxSize
    }

    @Synchronized
    fun get(key: String): T? = map[key]

    @Synchronized
    fun put(key: String, value: T) {
        map[key] = value
    }

    @Synchronized
    fun clear() = map.clear()
}
