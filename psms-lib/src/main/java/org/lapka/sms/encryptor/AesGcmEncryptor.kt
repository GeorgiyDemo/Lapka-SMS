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
 * - 12-byte random nonce (96 bits, recommended for GCM)
 * - 128-bit GCM authentication tag (integrity + authenticity)
 * - Uses Android's javax.crypto (hardware-accelerated, FIPS-validated)
 *
 * Wire format: [nonce (12 bytes)] [ciphertext + GCM tag (16 bytes)]
 */
class AesGcmEncryptor : Encryptor {

    companion object {
        private const val GCM_NONCE_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
    }

    override fun encrypt(key: ByteArray, plainData: ByteArray): ByteArray {
        validateKey(key)
        val nonce = ByteArray(GCM_NONCE_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return nonce + cipher.doFinal(plainData)
    }

    override fun decrypt(key: ByteArray, encryptedData: ByteArray): ByteArray {
        validateKey(key)
        if (encryptedData.size < GCM_NONCE_LENGTH + GCM_TAG_BYTES) throw InvalidDataException()
        val nonce = encryptedData.sliceArray(0 until GCM_NONCE_LENGTH)
        val ciphertextWithTag = encryptedData.sliceArray(GCM_NONCE_LENGTH until encryptedData.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
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
