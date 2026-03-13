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
    fun `ciphertext is nonce plus ciphertext plus 128-bit tag`() {
        val plaintext = "test".toByteArray()
        val encrypted = encryptor.encrypt(key256, plaintext)
        // 12 (nonce) + 4 (plaintext) + 16 (128-bit GCM tag) = 32
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
        val encrypted = encryptor.encrypt(key256, "secret".toByteArray())
        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        encryptor.decrypt(wrongKey, encrypted)
    }

    @Test(expected = InvalidDataException::class)
    fun `tampered ciphertext throws InvalidDataException`() {
        val plaintext = "secret".toByteArray()
        val encrypted = encryptor.encrypt(key256, plaintext)
        encrypted[15] = (encrypted[15].toInt() xor 0xFF).toByte()
        encryptor.decrypt(key256, encrypted)
    }

    @Test(expected = InvalidDataException::class)
    fun `too short ciphertext throws InvalidDataException`() {
        // Less than nonce (12) + 128-bit GCM tag (16) = 28 bytes
        encryptor.decrypt(key256, ByteArray(27))
    }

    @Test
    fun `nonce contains timestamp`() {
        val timestamp = (System.currentTimeMillis() / 1000).toInt()
        val encrypted = encryptor.encrypt(key256, "test".toByteArray(), timestamp = timestamp)
        val nonce = encrypted.sliceArray(0 until 12)
        assertEquals(timestamp, AesGcmEncryptor.extractTimestampFromNonce(nonce))
    }

    @Test
    fun `encrypt with AAD then decrypt with same AAD`() {
        val plaintext = "test".toByteArray()
        val aad = byteArrayOf(0x42)
        val encrypted = encryptor.encrypt(key256, plaintext, aad)
        val decrypted = encryptor.decrypt(key256, encrypted, aad)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test(expected = InvalidDataException::class)
    fun `decrypt with wrong AAD throws InvalidDataException`() {
        val encrypted = encryptor.encrypt(key256, "test".toByteArray(), byteArrayOf(0x42))
        encryptor.decrypt(key256, encrypted, byteArrayOf(0x43))
    }

    @Test
    fun `large plaintext roundtrip`() {
        val plaintext = ByteArray(10_000).also { SecureRandom().nextBytes(it) }
        val encrypted = encryptor.encrypt(key256, plaintext)
        val decrypted = encryptor.decrypt(key256, encrypted)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test(expected = InvalidKeyException::class)
    fun `invalid key size throws InvalidKeyException`() {
        encryptor.encrypt(ByteArray(15), "test".toByteArray())
    }

    @Test
    fun `buildNonceWithTimestamp produces 12-byte nonce`() {
        val nonce = AesGcmEncryptor.buildNonceWithTimestamp(12345)
        assertEquals(12, nonce.size)
    }

    @Test
    fun `extractTimestampFromNonce roundtrip`() {
        val ts = 1700000000
        val nonce = AesGcmEncryptor.buildNonceWithTimestamp(ts)
        assertEquals(ts, AesGcmEncryptor.extractTimestampFromNonce(nonce))
    }

    @Test
    fun `buildNonceWithTimestamp different calls produce different nonces`() {
        val ts = 12345
        val n1 = AesGcmEncryptor.buildNonceWithTimestamp(ts)
        val n2 = AesGcmEncryptor.buildNonceWithTimestamp(ts)
        assertEquals(
            AesGcmEncryptor.extractTimestampFromNonce(n1),
            AesGcmEncryptor.extractTimestampFromNonce(n2)
        )
        assertFalse("Random parts should differ", n1.contentEquals(n2))
    }
}
