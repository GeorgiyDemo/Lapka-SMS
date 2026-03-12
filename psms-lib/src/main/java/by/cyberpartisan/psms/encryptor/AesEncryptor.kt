package by.cyberpartisan.psms.encryptor

import by.cyberpartisan.psms.InvalidDataException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Legacy AES-CFB encryptor (kept for backward compatibility / reference).
 *
 * WARNING: This implementation has known security issues:
 * - IV is only 4 random bytes repeated 4x (should be 16 unique bytes)
 * - CFB mode provides no authentication (vulnerable to bit-flipping)
 * - Use AesGcmEncryptor instead for new messages
 */
class AesEncryptor : Encryptor {

    companion object {
        private const val IV_SIZE = 4
    }

    override fun encrypt(key: ByteArray, plainData: ByteArray): ByteArray {
        validateKey(key)
        val ivSrc = ByteArray(IV_SIZE).also { Random.nextBytes(it) }
        val iv = ivSrc + ivSrc + ivSrc + ivSrc

        val cipher = Cipher.getInstance("AES/CFB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(plainData) + ivSrc
    }

    override fun decrypt(key: ByteArray, encryptedData: ByteArray): ByteArray {
        validateKey(key)
        if (encryptedData.size < IV_SIZE) throw InvalidDataException()

        val payload = encryptedData.sliceArray(0 until encryptedData.size - IV_SIZE)
        val ivSrc = encryptedData.sliceArray(encryptedData.size - IV_SIZE until encryptedData.size)
        val iv = ivSrc + ivSrc + ivSrc + ivSrc

        val cipher = Cipher.getInstance("AES/CFB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(payload)
    }

    private fun validateKey(key: ByteArray) {
        if (key.size !in intArrayOf(16, 24, 32)) throw InvalidKeyException()
    }
}
