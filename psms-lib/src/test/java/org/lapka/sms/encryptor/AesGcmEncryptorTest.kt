package org.lapka.sms.encryptor

import org.lapka.sms.InvalidDataException
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class AesGcmEncryptorTest {

    private val encryptor = AesGcmEncryptor()
    private val key256 = ByteArray(32).also { SecureRandom().nextBytes(it) }
    private val key128 = ByteArray(16).also { SecureRandom().nextBytes(it) }

    @Test
    fun `encrypt then decrypt roundtrip - 256 bit key`() {
        val plaintext = "Hello, Partisan-SMS!".toByteArray()
        val encrypted = encryptor.encrypt(key256, plaintext)
        val decrypted = encryptor.decrypt(key256, encrypted)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt then decrypt roundtrip - 128 bit key`() {
        val plaintext = "Test with 128-bit key".toByteArray()
        val encrypted = encryptor.encrypt(key128, plaintext)
        val decrypted = encryptor.decrypt(key128, encrypted)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt then decrypt empty plaintext`() {
        val plaintext = byteArrayOf()
        val encrypted = encryptor.encrypt(key256, plaintext)
        val decrypted = encryptor.decrypt(key256, encrypted)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `ciphertext is nonce plus ciphertext plus tag`() {
        val plaintext = "test".toByteArray()
        val encrypted = encryptor.encrypt(key256, plaintext)
        // 12 (nonce) + 4 (plaintext) + 16 (GCM tag) = 32
        assertEquals(12 + plaintext.size + 16, encrypted.size)
    }

    @Test
    fun `each encryption produces different ciphertext (random nonce)`() {
        val plaintext = "same input".toByteArray()
        val enc1 = encryptor.encrypt(key256, plaintext)
        val enc2 = encryptor.encrypt(key256, plaintext)
        assertFalse("Ciphertexts should differ due to random nonce", enc1.contentEquals(enc2))
    }

    @Test(expected = InvalidDataException::class)
    fun `decrypt with wrong key throws InvalidDataException`() {
        val plaintext = "secret".toByteArray()
        val encrypted = encryptor.encrypt(key256, plaintext)
        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        encryptor.decrypt(wrongKey, encrypted)
    }

    @Test(expected = InvalidDataException::class)
    fun `tampered ciphertext throws InvalidDataException`() {
        val plaintext = "secret".toByteArray()
        val encrypted = encryptor.encrypt(key256, plaintext)
        // Flip a byte in the ciphertext portion (after nonce)
        encrypted[15] = (encrypted[15].toInt() xor 0xFF).toByte()
        encryptor.decrypt(key256, encrypted)
    }

    @Test(expected = InvalidDataException::class)
    fun `too short ciphertext throws InvalidDataException`() {
        // Less than nonce (12) + GCM tag (16) = 28 bytes
        encryptor.decrypt(key256, ByteArray(27))
    }

    @Test(expected = InvalidKeyException::class)
    fun `invalid key size throws InvalidKeyException`() {
        encryptor.encrypt(ByteArray(15), "test".toByteArray())
    }

    @Test
    fun `large plaintext roundtrip`() {
        val plaintext = ByteArray(10_000).also { SecureRandom().nextBytes(it) }
        val encrypted = encryptor.encrypt(key256, plaintext)
        val decrypted = encryptor.decrypt(key256, encrypted)
        assertArrayEquals(plaintext, decrypted)
    }
}
