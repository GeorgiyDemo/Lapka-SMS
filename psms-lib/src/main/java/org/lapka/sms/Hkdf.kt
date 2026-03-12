package org.lapka.sms

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HKDF (HMAC-based Key Derivation Function) per RFC 5869.
 * Used to derive separate encryption and MAC keys from a single master key.
 */
object Hkdf {

    private const val HASH_LEN = 32 // SHA-256 output
    private val SALT = "k-sms-hkdf-v2".toByteArray()

    fun deriveKey(ikm: ByteArray, info: ByteArray, length: Int = 32): ByteArray {
        val prk = extract(ikm)
        return expand(prk, info, length)
    }

    private fun extract(ikm: ByteArray): ByteArray {
        return hmacSha256(SALT, ikm)
    }

    private fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length in 1..255 * HASH_LEN)
        var t = byteArrayOf()
        var okm = byteArrayOf()
        var counter: Byte = 1
        while (okm.size < length) {
            t = hmacSha256(prk, t + info + byteArrayOf(counter))
            okm += t
            counter++
        }
        return okm.copyOf(length)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
