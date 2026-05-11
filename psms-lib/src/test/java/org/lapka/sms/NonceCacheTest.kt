package org.lapka.sms

import org.junit.Assert.*
import org.junit.Test

class NonceCacheTest {

    @Test
    fun `empty cache does not contain any nonce`() {
        val cache = NonceCache()
        assertFalse(cache.contains(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `added nonce is found`() {
        val cache = NonceCache()
        val nonce = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        cache.add(nonce)
        assertTrue(cache.contains(nonce))
    }

    @Test
    fun `different nonce is not found`() {
        val cache = NonceCache()
        cache.add(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        assertFalse(cache.contains(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 99)))
    }

    @Test
    fun `evicts oldest when max size exceeded`() {
        val cache = NonceCache(maxSize = 3)
        val n1 = byteArrayOf(1)
        val n2 = byteArrayOf(2)
        val n3 = byteArrayOf(3)
        val n4 = byteArrayOf(4)

        cache.add(n1)
        cache.add(n2)
        cache.add(n3)
        assertEquals(3, cache.size())

        cache.add(n4)
        assertEquals(3, cache.size())
        assertFalse("Oldest nonce should be evicted", cache.contains(n1))
        assertTrue(cache.contains(n2))
        assertTrue(cache.contains(n3))
        assertTrue(cache.contains(n4))
    }

    @Test
    fun `duplicate add does not increase size`() {
        val cache = NonceCache()
        val nonce = byteArrayOf(1, 2, 3)
        cache.add(nonce)
        cache.add(nonce)
        assertEquals(1, cache.size())
    }

    @Test
    fun `clear removes all entries`() {
        val cache = NonceCache()
        cache.add(byteArrayOf(1))
        cache.add(byteArrayOf(2))
        cache.clear()
        assertEquals(0, cache.size())
        assertFalse(cache.contains(byteArrayOf(1)))
    }
}
