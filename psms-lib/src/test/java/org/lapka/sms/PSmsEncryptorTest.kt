package org.lapka.sms

import org.lapka.sms.encrypted_data_encoder.Scheme
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class PSmsEncryptorTest {

    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private fun freshEncryptor() = PSmsEncryptor(nonceCache = NonceCache())

    // --- Encode/Decode roundtrip ---

    @Test
    fun `encode and decode roundtrip - BASE64`() {
        val encryptor = freshEncryptor()
        val original = Message("Hello, world!")
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
        assertNull(decoded.channelId)
    }

    @Test
    fun `encode and decode roundtrip - CYRILLIC_BASE64`() {
        val encryptor = freshEncryptor()
        val original = Message("Привет, мир!")
        val encoded = encryptor.encode(original, key, Scheme.CYRILLIC_BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.CYRILLIC_BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `encode and decode with channel ID`() {
        val encryptor = freshEncryptor()
        val original = Message("Channel message", channelId = 42)
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
        assertEquals(42, decoded.channelId)
    }

    @Test
    fun `encode and decode empty message`() {
        val encryptor = freshEncryptor()
        val original = Message("")
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals("", decoded.text)
    }

    @Test
    fun `encode and decode long message`() {
        val encryptor = freshEncryptor()
        val longText = "A".repeat(1000)
        val original = Message(longText)
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(longText, decoded.text)
    }

    @Test
    fun `encode and decode unicode message`() {
        val encryptor = freshEncryptor()
        val original = Message("\uD83D\uDE00 Emoji and 中文 and العربية")
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    // --- Wrong key ---

    @Test(expected = InvalidDataException::class)
    fun `decode with wrong key throws InvalidDataException`() {
        val encryptor = freshEncryptor()
        val encoded = encryptor.encode(Message("secret"), key, Scheme.BASE64.ordinal)

        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val decryptor = freshEncryptor()
        decryptor.decode(encoded, wrongKey, Scheme.BASE64.ordinal)
    }

    // --- Tampered ciphertext ---

    @Test(expected = InvalidDataException::class)
    fun `tampered encoded string throws InvalidDataException`() {
        val encryptor = freshEncryptor()
        val encoded = encryptor.encode(Message("secret"), key, Scheme.BASE64.ordinal)

        val tampered = encoded.substring(0, encoded.length / 2) +
                (if (encoded[encoded.length / 2] == 'A') 'B' else 'A') +
                encoded.substring(encoded.length / 2 + 1)

        val decryptor = freshEncryptor()
        decryptor.decode(tampered, key, Scheme.BASE64.ordinal)
    }

    // --- Each encryption produces different output ---

    @Test
    fun `same message encrypted twice produces different ciphertext`() {
        val msg = Message("same message")
        val enc1 = freshEncryptor().encode(msg, key, Scheme.BASE64.ordinal)
        val enc2 = freshEncryptor().encode(msg, key, Scheme.BASE64.ordinal)
        assertNotEquals("Ciphertexts should differ due to random nonce", enc1, enc2)
    }

    // --- tryDecode ---

    @Test
    fun `tryDecode returns decrypted message for valid input`() {
        val encryptor = freshEncryptor()
        val original = Message("Hello tryDecode")
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `tryDecode returns original text for non-encrypted input`() {
        val plainText = "This is not encrypted"
        val decryptor = freshEncryptor()
        val result = decryptor.tryDecode(plainText, key)
        assertEquals(plainText, result.text)
    }

    // --- isEncrypted ---

    @Test
    fun `isEncrypted returns true for encrypted message`() {
        val encryptor = freshEncryptor()
        val encoded = encryptor.encode(Message("test"), key, Scheme.BASE64.ordinal)
        assertTrue(freshEncryptor().isEncrypted(encoded, key))
    }

    @Test
    fun `isEncrypted returns false for plain text`() {
        assertFalse(freshEncryptor().isEncrypted("just a regular sms", key))
    }

    // --- Version ---

    @Test
    fun `current VERSION is 3`() {
        assertEquals(3, VERSION)
    }

    // --- Large channel ID ---

    @Test
    fun `encode and decode with max channel ID`() {
        val encryptor = freshEncryptor()
        val original = Message("test", channelId = Int.MAX_VALUE)
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(Int.MAX_VALUE, decoded.channelId)
    }

    @Test
    fun `encode and decode with negative channel ID`() {
        val encryptor = freshEncryptor()
        val original = Message("test", channelId = -1)
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(-1, decoded.channelId)
    }

    // --- Scheme auto-detection via tryDecode ---

    @Test
    fun `tryDecode detects CYRILLIC_BASE64 scheme automatically`() {
        val encryptor = freshEncryptor()
        val original = Message("Auto-detect test")
        val encoded = encryptor.encode(original, key, Scheme.CYRILLIC_BASE64.ordinal)

        val decryptor = freshEncryptor()
        val decoded = decryptor.tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    // --- Nonce replay detection ---

    @Test(expected = InvalidDataException::class)
    fun `replayed message is rejected`() {
        val cache = NonceCache()
        val encryptor = PSmsEncryptor(nonceCache = cache)
        val encoded = encryptor.encode(Message("test"), key, Scheme.BASE64.ordinal)

        // First decode succeeds
        val decryptor1 = PSmsEncryptor(nonceCache = cache)
        decryptor1.decode(encoded, key, Scheme.BASE64.ordinal)

        // Second decode with same nonce should fail
        val decryptor2 = PSmsEncryptor(nonceCache = cache)
        decryptor2.decode(encoded, key, Scheme.BASE64.ordinal)
    }
}
