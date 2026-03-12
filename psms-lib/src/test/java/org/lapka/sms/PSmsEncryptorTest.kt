package org.lapka.sms

import org.lapka.sms.encrypted_data_encoder.Scheme
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class PSmsEncryptorTest {

    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

    // --- Encode/Decode roundtrip ---

    @Test
    fun `encode and decode roundtrip - BASE64`() {
        val encryptor = PSmsEncryptor()
        val original = Message("Hello, world!")
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
        assertNull(decoded.channelId)
    }

    @Test
    fun `encode and decode roundtrip - CYRILLIC_BASE64`() {
        val encryptor = PSmsEncryptor()
        val original = Message("Привет, мир!")
        val encoded = encryptor.encode(original, key, Scheme.CYRILLIC_BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.CYRILLIC_BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `encode and decode with channel ID`() {
        val encryptor = PSmsEncryptor()
        val original = Message("Channel message", channelId = 42)
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
        assertEquals(42, decoded.channelId)
    }

    @Test
    fun `encode and decode empty message`() {
        val encryptor = PSmsEncryptor()
        val original = Message("")
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals("", decoded.text)
    }

    @Test
    fun `encode and decode long message`() {
        val encryptor = PSmsEncryptor()
        val longText = "A".repeat(1000)
        val original = Message(longText)
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(longText, decoded.text)
    }

    @Test
    fun `encode and decode unicode message`() {
        val encryptor = PSmsEncryptor()
        val original = Message("\uD83D\uDE00 Emoji and 中文 and العربية")
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    // --- Wrong key ---

    @Test(expected = InvalidDataException::class)
    fun `decode with wrong key throws InvalidDataException`() {
        val encryptor = PSmsEncryptor()
        val encoded = encryptor.encode(Message("secret"), key, Scheme.BASE64.ordinal)

        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val decryptor = PSmsEncryptor()
        decryptor.decode(encoded, wrongKey, Scheme.BASE64.ordinal)
    }

    // --- Tampered ciphertext ---

    @Test(expected = InvalidDataException::class)
    fun `tampered encoded string throws InvalidDataException`() {
        val encryptor = PSmsEncryptor()
        val encoded = encryptor.encode(Message("secret"), key, Scheme.BASE64.ordinal)

        // Modify a character in the middle of the encoded string
        val tampered = encoded.substring(0, encoded.length / 2) +
                (if (encoded[encoded.length / 2] == 'A') 'B' else 'A') +
                encoded.substring(encoded.length / 2 + 1)

        val decryptor = PSmsEncryptor()
        decryptor.decode(tampered, key, Scheme.BASE64.ordinal)
    }

    // --- Each encryption produces different output ---

    @Test
    fun `same message encrypted twice produces different ciphertext`() {
        val msg = Message("same message")
        val enc1 = PSmsEncryptor().encode(msg, key, Scheme.BASE64.ordinal)
        val enc2 = PSmsEncryptor().encode(msg, key, Scheme.BASE64.ordinal)
        assertNotEquals("Ciphertexts should differ due to random nonce", enc1, enc2)
    }

    // --- tryDecode ---

    @Test
    fun `tryDecode returns decrypted message for valid input`() {
        val encryptor = PSmsEncryptor()
        val original = Message("Hello tryDecode")
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `tryDecode returns original text for non-encrypted input`() {
        val plainText = "This is not encrypted"
        val decryptor = PSmsEncryptor()
        val result = decryptor.tryDecode(plainText, key)
        assertEquals(plainText, result.text)
    }

    // --- isEncrypted ---

    @Test
    fun `isEncrypted returns true for encrypted message`() {
        val encryptor = PSmsEncryptor()
        val encoded = encryptor.encode(Message("test"), key, Scheme.BASE64.ordinal)
        assertTrue(PSmsEncryptor().isEncrypted(encoded, key))
    }

    @Test
    fun `isEncrypted returns false for plain text`() {
        assertFalse(PSmsEncryptor().isEncrypted("just a regular sms", key))
    }

    // --- Version compatibility ---

    @Test
    fun `current VERSION is 3`() {
        assertEquals(3, VERSION)
    }

    // --- Large channel ID ---

    @Test
    fun `encode and decode with max channel ID`() {
        val encryptor = PSmsEncryptor()
        val original = Message("test", channelId = Int.MAX_VALUE)
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(Int.MAX_VALUE, decoded.channelId)
    }

    @Test
    fun `encode and decode with negative channel ID`() {
        val encryptor = PSmsEncryptor()
        val original = Message("test", channelId = -1)
        val encoded = encryptor.encode(original, key, Scheme.BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(-1, decoded.channelId)
    }

    // --- Scheme auto-detection via tryDecode ---

    @Test
    fun `tryDecode detects CYRILLIC_BASE64 scheme automatically`() {
        val encryptor = PSmsEncryptor()
        val original = Message("Auto-detect test")
        val encoded = encryptor.encode(original, key, Scheme.CYRILLIC_BASE64.ordinal)

        val decryptor = PSmsEncryptor()
        val decoded = decryptor.tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }
}
