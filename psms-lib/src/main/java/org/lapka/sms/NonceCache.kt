package org.lapka.sms

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.LinkedList

/**
 * In-memory cache of recently seen nonces for anti-replay protection.
 *
 * After GCM decryption succeeds (proving the message is authentic),
 * the nonce is checked against this cache. If it was already seen,
 * the message is rejected as a replay.
 *
 * Thread-safe via synchronized blocks.
 * O(1) lookup via HashSet, eviction by both age (TTL) and capacity (maxSize).
 *
 * Supports persistence via [saveTo] / [loadFrom] so the cache
 * survives application restarts.
 */
class NonceCache(
    private val maxSize: Int = DEFAULT_MAX_SIZE,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS
) {

    companion object {
        const val DEFAULT_MAX_SIZE = 1000
        const val DEFAULT_TTL_MILLIS = 7 * 24 * 3600 * 1000L // 7 days
        private const val MAGIC_V2 = 0x4E43_0002 // "NC" + version 2
        private const val MAGIC_V1 = 0x4E43_0001 // "NC" + version 1

        private val instance = NonceCache()

        fun getDefault(): NonceCache = instance
    }

    private val order = LinkedList<TimestampedNonce>()
    private val set = HashSet<NonceWrapper>()

    @Synchronized
    fun contains(nonce: ByteArray): Boolean {
        return set.contains(NonceWrapper(nonce))
    }

    @Synchronized
    fun add(nonce: ByteArray) {
        val wrapper = NonceWrapper(nonce)
        if (set.contains(wrapper)) return
        evictExpired()
        order.addLast(TimestampedNonce(wrapper, System.currentTimeMillis()))
        set.add(wrapper)
        while (order.size > maxSize) {
            val evicted = order.removeFirst()
            set.remove(evicted.wrapper)
        }
    }

    @Synchronized
    fun clear() {
        order.clear()
        set.clear()
    }

    @Synchronized
    fun size(): Int = set.size

    private fun evictExpired() {
        val cutoff = System.currentTimeMillis() - ttlMillis
        while (order.isNotEmpty() && order.first.addedAt < cutoff) {
            val evicted = order.removeFirst()
            set.remove(evicted.wrapper)
        }
    }

    /**
     * Serialize the cache to an output stream.
     * Format v2: [magic(4)] [count(4)] [nonceLen(4) nonceBytes addedAt(8)]...
     */
    @Synchronized
    fun saveTo(output: OutputStream) {
        evictExpired()
        val buf = ByteBuffer.allocate(12 + order.size * 28) // estimate
        buf.putInt(MAGIC_V2)
        buf.putInt(order.size)
        for (entry in order) {
            val bytes = entry.wrapper.bytes()
            buf.putInt(bytes.size)
            buf.put(bytes)
            buf.putLong(entry.addedAt)
        }
        buf.flip()
        val arr = ByteArray(buf.remaining())
        buf.get(arr)
        output.write(arr)
        output.flush()
    }

    /**
     * Deserialize and merge nonces from an input stream.
     * Supports both v1 (no timestamps) and v2 (with timestamps) formats.
     * Silently ignores malformed data.
     */
    @Synchronized
    fun loadFrom(input: InputStream) {
        try {
            val data = input.readBytes()
            if (data.size < 8) return
            val buf = ByteBuffer.wrap(data)
            val magic = buf.getInt()
            val hasTimestamps = when (magic) {
                MAGIC_V2 -> true
                MAGIC_V1 -> false
                else -> return
            }
            val count = buf.getInt()
            if (count < 0 || count > maxSize) return
            val now = System.currentTimeMillis()
            for (i in 0 until count) {
                if (buf.remaining() < 4) return
                val len = buf.getInt()
                if (len <= 0 || len > 64 || buf.remaining() < len) return
                val nonce = ByteArray(len)
                buf.get(nonce)
                val addedAt = if (hasTimestamps) {
                    if (buf.remaining() < 8) return
                    buf.getLong()
                } else {
                    now
                }
                if (now - addedAt < ttlMillis) {
                    val wrapper = NonceWrapper(nonce)
                    if (!set.contains(wrapper)) {
                        order.addLast(TimestampedNonce(wrapper, addedAt))
                        set.add(wrapper)
                    }
                }
            }
        } catch (_: Exception) {
            // Corrupted file – ignore silently, cache starts empty
        }
    }

    private class TimestampedNonce(val wrapper: NonceWrapper, val addedAt: Long)

    private class NonceWrapper(private val nonce: ByteArray) {
        private val hash = nonce.contentHashCode()

        fun bytes(): ByteArray = nonce.copyOf()

        override fun equals(other: Any?): Boolean {
            if (other !is NonceWrapper) return false
            return nonce.contentEquals(other.nonce)
        }

        override fun hashCode(): Int = hash
    }
}
