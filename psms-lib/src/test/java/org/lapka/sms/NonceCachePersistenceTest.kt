package org.lapka.sms

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class NonceCachePersistenceTest {

    private fun nonce(vararg bytes: Byte) = bytes

    @Test
    fun `saveTo and loadFrom roundtrip preserves nonces`() {
        val cache = NonceCache()
        val n1 = nonce(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val n2 = nonce(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120)
        cache.add(n1)
        cache.add(n2)

        val out = ByteArrayOutputStream()
        cache.saveTo(out)

        val loaded = NonceCache()
        loaded.loadFrom(ByteArrayInputStream(out.toByteArray()))

        assertEquals(2, loaded.size())
        assertTrue(loaded.contains(n1))
        assertTrue(loaded.contains(n2))
    }

    @Test
    fun `loadFrom empty stream does not crash`() {
        val cache = NonceCache()
        cache.loadFrom(ByteArrayInputStream(ByteArray(0)))
        assertEquals(0, cache.size())
    }

    @Test
    fun `loadFrom corrupted data does not crash`() {
        val cache = NonceCache()
        cache.loadFrom(ByteArrayInputStream(byteArrayOf(0, 1, 2, 3, 4, 5)))
        assertEquals(0, cache.size())
    }

    @Test
    fun `loadFrom invalid magic is ignored`() {
        val buf = ByteBuffer.allocate(12)
        buf.putInt(0xDEADBEEF.toInt()) // bad magic
        buf.putInt(1) // count
        buf.putInt(4) // nonce len
        val cache = NonceCache()
        cache.loadFrom(ByteArrayInputStream(buf.array()))
        assertEquals(0, cache.size())
    }

    @Test
    fun `loadFrom merges into existing cache`() {
        val cache = NonceCache()
        val existing = nonce(1, 2, 3)
        cache.add(existing)

        val other = NonceCache()
        val n1 = nonce(4, 5, 6)
        other.add(n1)
        val out = ByteArrayOutputStream()
        other.saveTo(out)

        cache.loadFrom(ByteArrayInputStream(out.toByteArray()))
        assertEquals(2, cache.size())
        assertTrue(cache.contains(existing))
        assertTrue(cache.contains(n1))
    }

    @Test
    fun `loadFrom skips duplicate nonces`() {
        val cache = NonceCache()
        val n1 = nonce(1, 2, 3)
        cache.add(n1)

        val other = NonceCache()
        other.add(n1)
        val out = ByteArrayOutputStream()
        other.saveTo(out)

        cache.loadFrom(ByteArrayInputStream(out.toByteArray()))
        assertEquals(1, cache.size())
    }

    @Test
    fun `loadFrom v1 format without timestamps`() {
        // Build v1 format manually: magic_v1(4) + count(4) + [nonceLen(4) + nonceBytes]...
        val nonce = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val buf = ByteBuffer.allocate(4 + 4 + 4 + nonce.size)
        buf.putInt(0x4E43_0001) // MAGIC_V1
        buf.putInt(1) // count
        buf.putInt(nonce.size)
        buf.put(nonce)

        val cache = NonceCache()
        cache.loadFrom(ByteArrayInputStream(buf.array()))
        assertEquals(1, cache.size())
        assertTrue(cache.contains(nonce))
    }

    @Test
    fun `loadFrom rejects expired entries`() {
        // Build v2 with timestamp far in the past
        val nonce = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val buf = ByteBuffer.allocate(4 + 4 + 4 + nonce.size + 8)
        buf.putInt(0x4E43_0002) // MAGIC_V2
        buf.putInt(1) // count
        buf.putInt(nonce.size)
        buf.put(nonce)
        buf.putLong(1000L) // timestamp from 1970 — way expired

        val cache = NonceCache()
        cache.loadFrom(ByteArrayInputStream(buf.array()))
        assertEquals(0, cache.size())
    }

    @Test
    fun `saveTo then loadFrom preserves order for eviction`() {
        val cache = NonceCache(maxSize = 3)
        val n1 = nonce(1)
        val n2 = nonce(2)
        val n3 = nonce(3)
        cache.add(n1)
        cache.add(n2)
        cache.add(n3)

        val out = ByteArrayOutputStream()
        cache.saveTo(out)

        val loaded = NonceCache(maxSize = 3)
        loaded.loadFrom(ByteArrayInputStream(out.toByteArray()))

        // Adding a 4th should evict the oldest (n1)
        loaded.add(nonce(4))
        assertEquals(3, loaded.size())
        assertFalse(loaded.contains(n1))
        assertTrue(loaded.contains(n2))
        assertTrue(loaded.contains(n3))
        assertTrue(loaded.contains(nonce(4)))
    }

    @Test
    fun `loadFrom truncated data does not crash`() {
        // Valid header but truncated nonce data
        val buf = ByteBuffer.allocate(12)
        buf.putInt(0x4E43_0002) // MAGIC_V2
        buf.putInt(1) // count = 1
        buf.putInt(100) // nonceLen = 100, but no more data

        val cache = NonceCache()
        cache.loadFrom(ByteArrayInputStream(buf.array()))
        assertEquals(0, cache.size())
    }

    @Test
    fun `loadFrom negative count is rejected`() {
        val buf = ByteBuffer.allocate(8)
        buf.putInt(0x4E43_0002) // MAGIC_V2
        buf.putInt(-1) // negative count

        val cache = NonceCache()
        cache.loadFrom(ByteArrayInputStream(buf.array()))
        assertEquals(0, cache.size())
    }

    @Test
    fun `saveTo empty cache produces valid data`() {
        val cache = NonceCache()
        val out = ByteArrayOutputStream()
        cache.saveTo(out)

        val loaded = NonceCache()
        loaded.loadFrom(ByteArrayInputStream(out.toByteArray()))
        assertEquals(0, loaded.size())
    }
}
