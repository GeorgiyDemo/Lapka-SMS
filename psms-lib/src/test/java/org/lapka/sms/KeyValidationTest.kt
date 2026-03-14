package org.lapka.sms

import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

/**
 * Tests key validation logic matching KeySettingsPresenter.validateKey().
 * A valid key is a non-empty Base64 string that decodes to 16, 24, or 32 bytes.
 */
class KeyValidationTest {

    private fun validateKey(text: String): Boolean {
        try {
            if (text.isEmpty()) return false
            val data = java.util.Base64.getDecoder().decode(text)
            return data.size == 16 || data.size == 24 || data.size == 32
        } catch (_: IllegalArgumentException) {
            return false
        }
    }

    @Test
    fun `empty string is invalid`() {
        assertFalse(validateKey(""))
    }

    @Test
    fun `valid AES-128 key (16 bytes)`() {
        val key = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val encoded = java.util.Base64.getEncoder().withoutPadding().encodeToString(key)
        assertTrue(validateKey(encoded))
    }

    @Test
    fun `valid AES-192 key (24 bytes)`() {
        val key = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val encoded = java.util.Base64.getEncoder().encodeToString(key)
        assertTrue(validateKey(encoded))
    }

    @Test
    fun `valid AES-256 key (32 bytes)`() {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = java.util.Base64.getEncoder().encodeToString(key)
        assertTrue(validateKey(encoded))
    }

    @Test
    fun `wrong key size 8 bytes is invalid`() {
        val key = ByteArray(8).also { SecureRandom().nextBytes(it) }
        val encoded = java.util.Base64.getEncoder().encodeToString(key)
        assertFalse(validateKey(encoded))
    }

    @Test
    fun `wrong key size 15 bytes is invalid`() {
        val key = ByteArray(15).also { SecureRandom().nextBytes(it) }
        val encoded = java.util.Base64.getEncoder().encodeToString(key)
        assertFalse(validateKey(encoded))
    }

    @Test
    fun `wrong key size 33 bytes is invalid`() {
        val key = ByteArray(33).also { SecureRandom().nextBytes(it) }
        val encoded = java.util.Base64.getEncoder().encodeToString(key)
        assertFalse(validateKey(encoded))
    }

    @Test
    fun `not base64 string is invalid`() {
        assertFalse(validateKey("это не base64!"))
    }

    @Test
    fun `random text is invalid`() {
        assertFalse(validateKey("hello world"))
    }

    @Test
    fun `whitespace only is invalid`() {
        assertFalse(validateKey("   "))
    }

    @Test
    fun `valid base64 but wrong size (1 byte) is invalid`() {
        val encoded = java.util.Base64.getEncoder().encodeToString(byteArrayOf(42))
        assertFalse(validateKey(encoded))
    }

    @Test
    fun `KeyGenerator AES-256 produces valid key`() {
        val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key = keyGen.generateKey()
        val encoded = java.util.Base64.getEncoder().encodeToString(key.encoded)
        assertTrue(validateKey(encoded))
    }

    @Test
    fun `base64 with padding is valid`() {
        // 16 bytes encodes to 24 base64 chars with padding
        val key = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val encoded = java.util.Base64.getEncoder().encodeToString(key)
        assertTrue(encoded.endsWith("=") || encoded.length == 24)
        assertTrue(validateKey(encoded))
    }
}
