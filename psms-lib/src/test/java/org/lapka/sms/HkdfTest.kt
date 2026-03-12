package org.lapka.sms

import org.junit.Assert.*
import org.junit.Test

class HkdfTest {

    @Test
    fun `deriveKey returns correct length`() {
        val ikm = "master-key".toByteArray()
        val info = "test-info".toByteArray()

        assertEquals(32, Hkdf.deriveKey(ikm, info, 32).size)
        assertEquals(16, Hkdf.deriveKey(ikm, info, 16).size)
        assertEquals(64, Hkdf.deriveKey(ikm, info, 64).size)
    }

    @Test
    fun `same inputs produce same output (deterministic)`() {
        val ikm = "master-key".toByteArray()
        val info = "test-info".toByteArray()

        val key1 = Hkdf.deriveKey(ikm, info, 32)
        val key2 = Hkdf.deriveKey(ikm, info, 32)
        assertArrayEquals(key1, key2)
    }

    @Test
    fun `different info produces different keys`() {
        val ikm = "master-key".toByteArray()
        val key1 = Hkdf.deriveKey(ikm, "enc".toByteArray(), 32)
        val key2 = Hkdf.deriveKey(ikm, "mac".toByteArray(), 32)
        assertFalse("Keys with different info must differ", key1.contentEquals(key2))
    }

    @Test
    fun `different ikm produces different keys`() {
        val info = "test-info".toByteArray()
        val key1 = Hkdf.deriveKey("key-a".toByteArray(), info, 32)
        val key2 = Hkdf.deriveKey("key-b".toByteArray(), info, 32)
        assertFalse("Keys with different IKM must differ", key1.contentEquals(key2))
    }

    @Test
    fun `derive key with minimum length`() {
        val key = Hkdf.deriveKey("ikm".toByteArray(), "info".toByteArray(), 1)
        assertEquals(1, key.size)
    }

    @Test
    fun `derive key longer than hash output (multi-block expand)`() {
        val key = Hkdf.deriveKey("ikm".toByteArray(), "info".toByteArray(), 100)
        assertEquals(100, key.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `length 0 throws`() {
        Hkdf.deriveKey("ikm".toByteArray(), "info".toByteArray(), 0)
    }

    @Test
    fun `enc and mac key info from PSmsEncryptor produce different keys`() {
        val masterKey = "test-master-key-32-bytes!!!!!!!!".toByteArray()
        val encKey = Hkdf.deriveKey(masterKey, "k-sms-v2-enc".toByteArray(), 32)
        val macKey = Hkdf.deriveKey(masterKey, "k-sms-v2-mac".toByteArray(), 32)
        assertFalse("Enc and MAC keys must differ", encKey.contentEquals(macKey))
    }
}
