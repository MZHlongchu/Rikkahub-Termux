package me.rerere.rikkahub.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * A simple thread-safe cache implementation with expiration support.
 * This is a lightweight alternative to Guava Cache to avoid concurrency issues.
 */
class SimpleCache<K, V>(
    private val expireAfterWriteMillis: Long,
    private val maximumSize: Int?
) {
    private data class CacheEntry<V>(
        val value: V,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(expireAfterWriteMillis: Long): Boolean {
            return System.currentTimeMillis() - timestamp > expireAfterWriteMillis
        }
    }

    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val sizeLimitLock = Any()

    fun getIfPresent(key: K): V? {
        val entry = cache[key] ?: return null
        return if (entry.isExpired(expireAfterWriteMillis)) {
            cache.remove(key)
            null
        } else {
            entry.value
        }
    }

    fun put(key: K, value: V) {
        cache[key] = CacheEntry(value)
        enforceMaximumSizeIfNeeded()
    }

    fun invalidate(key: K) {
        cache.remove(key)
    }

    fun invalidateAll() {
        cache.clear()
    }

    fun cleanUp() {
        cache.entries.removeIf { it.value.isExpired(expireAfterWriteMillis) }
    }

    fun size(): Int = cache.size

    private fun enforceMaximumSizeIfNeeded() {
        val maxSize = maximumSize ?: return
        if (cache.size <= maxSize) return
        synchronized(sizeLimitLock) {
            cache.entries.removeIf { it.value.isExpired(expireAfterWriteMillis) }
            while (cache.size > maxSize) {
                val oldest = cache.entries.minByOrNull { it.value.timestamp } ?: break
                cache.remove(oldest.key)
            }
        }
    }

    companion object {
        fun <K, V> builder() = Builder<K, V>()
    }

    class Builder<K, V> {
        private var expireAfterWriteMillis: Long = Long.MAX_VALUE
        private var maximumSize: Int? = null

        fun expireAfterWrite(duration: Long, unit: TimeUnit): Builder<K, V> {
            expireAfterWriteMillis = unit.toMillis(duration)
            return this
        }

        fun maximumSize(size: Int): Builder<K, V> {
            require(size > 0) { "maximumSize must be greater than 0" }
            maximumSize = size
            return this
        }

        fun build(): SimpleCache<K, V> {
            return SimpleCache(expireAfterWriteMillis, maximumSize)
        }
    }
}
