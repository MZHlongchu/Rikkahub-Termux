package me.rerere.rikkahub.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class SimpleCacheTest {
    @Test
    fun `evicts oldest entries when maximum size exceeded`() {
        val cache = SimpleCache.builder<String, Int>()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(2)
            .build()

        cache.put("first", 1)
        Thread.sleep(20)
        cache.put("second", 2)
        Thread.sleep(20)
        cache.put("third", 3)

        assertEquals(2, cache.size())
        assertNull(cache.getIfPresent("first"))
        assertEquals(2, cache.getIfPresent("second"))
        assertEquals(3, cache.getIfPresent("third"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maximum size must be greater than zero`() {
        SimpleCache.builder<String, Int>()
            .maximumSize(0)
    }
}
