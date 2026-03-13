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
 * O(1) lookup via HashSet, FIFO eviction via LinkedList.
 */
class NonceCache(private val maxSize: Int = DEFAULT_MAX_SIZE) {

    companion object {
        const val DEFAULT_MAX_SIZE = 1000

        private val instance = NonceCache()

        fun getDefault(): NonceCache = instance
    }

    private val order = LinkedList<NonceWrapper>()
    private val set = HashSet<NonceWrapper>()

    @Synchronized
    fun contains(nonce: ByteArray): Boolean {
        return set.contains(NonceWrapper(nonce))
    }

    @Synchronized
    fun add(nonce: ByteArray) {
        val wrapper = NonceWrapper(nonce)
        if (set.contains(wrapper)) return
        order.addLast(wrapper)
        set.add(wrapper)
        while (order.size > maxSize) {
            val evicted = order.removeFirst()
            set.remove(evicted)
        }
    }

    @Synchronized
    fun clear() {
        order.clear()
        set.clear()
    }

    @Synchronized
    fun size(): Int = set.size

    private class NonceWrapper(private val nonce: ByteArray) {
        private val hash = nonce.contentHashCode()

        override fun equals(other: Any?): Boolean {
            if (other !is NonceWrapper) return false
            return nonce.contentEquals(other.nonce)
        }

        override fun hashCode(): Int = hash
    }
}
