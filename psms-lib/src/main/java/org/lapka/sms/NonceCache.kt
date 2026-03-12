package org.lapka.sms

import java.util.LinkedList

/**
 * In-memory cache of recently seen nonces for anti-replay protection.
 *
 * After GCM decryption succeeds (proving the message is authentic),
 * the nonce is checked against this cache. If it was already seen,
 * the message is rejected as a replay.
 *
 * Thread-safe via synchronized blocks.
 */
class NonceCache(private val maxSize: Int = DEFAULT_MAX_SIZE) {

    companion object {
        const val DEFAULT_MAX_SIZE = 1000

        private val instance = NonceCache()

        fun getDefault(): NonceCache = instance
    }

    private val cache = LinkedList<NonceWrapper>()

    @Synchronized
    fun contains(nonce: ByteArray): Boolean {
        val wrapper = NonceWrapper(nonce)
        return cache.any { it == wrapper }
    }

    @Synchronized
    fun add(nonce: ByteArray) {
        val wrapper = NonceWrapper(nonce)
        if (cache.any { it == wrapper }) return
        cache.addLast(wrapper)
        while (cache.size > maxSize) {
            cache.removeFirst()
        }
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }

    @Synchronized
    fun size(): Int = cache.size

    private class NonceWrapper(private val nonce: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (other !is NonceWrapper) return false
            return nonce.contentEquals(other.nonce)
        }

        override fun hashCode(): Int = nonce.contentHashCode()
    }
}
