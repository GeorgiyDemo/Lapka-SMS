package org.lapka.sms.encryptor

import org.lapka.sms.InvalidDataException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryptor.
 *
 * - 12-byte nonce: [timestamp (4B)] [random (8B)]
 * - 96-bit GCM authentication tag (integrity + authenticity)
 * - Optional AAD (Associated Authenticated Data)
 *
 * Wire format: [nonce (12 bytes)] [ciphertext + GCM tag (12 bytes)]
 */
class AesGcmEncryptor : Encryptor {

    companion object {
        const val GCM_NONCE_LENGTH = 12
        private const val GCM_TAG_BITS = 96
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private const val TIMESTAMP_SIZE = 4

        fun buildNonceWithTimestamp(timestamp: Int): ByteArray {
            val nonce = ByteArray(GCM_NONCE_LENGTH)
            nonce[0] = (timestamp shr 0).toByte()
            nonce[1] = (timestamp shr 8).toByte()
            nonce[2] = (timestamp shr 16).toByte()
            nonce[3] = (timestamp shr 24).toByte()
            val random = ByteArray(GCM_NONCE_LENGTH - TIMESTAMP_SIZE)
            SecureRandom().nextBytes(random)
            System.arraycopy(random, 0, nonce, TIMESTAMP_SIZE, random.size)
            return nonce
        }

        fun extractTimestampFromNonce(nonce: ByteArray): Int {
            return (nonce[3].toInt() shl 24) or
                    ((nonce[2].toInt() and 0xFF) shl 16) or
                    ((nonce[1].toInt() and 0xFF) shl 8) or
                    (nonce[0].toInt() and 0xFF)
        }
    }

    override fun encrypt(key: ByteArray, plainData: ByteArray, aad: ByteArray?): ByteArray {
        return encrypt(key, plainData, aad, timestamp = null)
    }

    fun encrypt(key: ByteArray, plainData: ByteArray, aad: ByteArray? = null, timestamp: Int? = null): ByteArray {
        validateKey(key)
        val nonce = if (timestamp != null) {
            buildNonceWithTimestamp(timestamp)
        } else {
            buildNonceWithTimestamp((System.currentTimeMillis() / 1000).toInt())
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        if (aad != null) cipher.updateAAD(aad)
        return nonce + cipher.doFinal(plainData)
    }

    override fun decrypt(key: ByteArray, encryptedData: ByteArray, aad: ByteArray?): ByteArray {
        validateKey(key)
        if (encryptedData.size < GCM_NONCE_LENGTH + GCM_TAG_BYTES) throw InvalidDataException()
        val nonce = encryptedData.sliceArray(0 until GCM_NONCE_LENGTH)
        val ciphertextWithTag = encryptedData.sliceArray(GCM_NONCE_LENGTH until encryptedData.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        if (aad != null) cipher.updateAAD(aad)
        try {
            return cipher.doFinal(ciphertextWithTag)
        } catch (e: GeneralSecurityException) {
            throw InvalidDataException()
        }
    }

    private fun validateKey(key: ByteArray) {
        if (key.size !in intArrayOf(16, 24, 32)) throw InvalidKeyException()
    }
}
