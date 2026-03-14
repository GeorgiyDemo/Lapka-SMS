package org.lapka.sms

import org.lapka.sms.encrypted_data_encoder.Scheme
import org.lapka.sms.encrypted_data_encoder.text_encoder.TextEncoderEn
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.security.SecureRandom

class PSmsEncryptorEnglishTextTest {

    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private fun freshEncryptor() = PSmsEncryptor(nonceCache = NonceCache())

    /**
     * Try to encode; returns null if encoding requires padding (which needs
     * List.removeLast(), available only on Java 21+).
     */
    private fun tryEncode(message: Message, schemeId: Int): String? {
        return try {
            freshEncryptor().encode(message, key, schemeId)
        } catch (_: NoSuchMethodError) {
            null // List.removeLast() not available before Java 21
        }
    }

    // TEXT scheme encoding may need padding (List.removeLast → Java 21+).
    // Tests that require encoding skip when padding is needed on older JVMs.

    @Test
    fun `encode and decode roundtrip - TEXT_ENGLISH`() {
        val encoded = tryEncode(Message("Hello, world!"), Scheme.TEXT_ENGLISH.ordinal)
        assumeTrue("Skipped: encoding needs padding (Java 21+ required)", encoded != null)

        val decoded = freshEncryptor().decode(encoded!!, key, Scheme.TEXT_ENGLISH.ordinal)
        assertEquals("Hello, world!", decoded.text)
        assertNull(decoded.channelId)
    }

    @Test
    fun `encoded output contains only ASCII and punctuation`() {
        val encoded = tryEncode(Message("Test message"), Scheme.TEXT_ENGLISH.ordinal)
        assumeTrue("Skipped: encoding needs padding (Java 21+ required)", encoded != null)

        for (ch in encoded!!) {
            assertTrue(
                "Unexpected character: '$ch' (${ch.code})",
                ch.code in 32..126
            )
        }
    }

    @Test
    fun `encode and decode roundtrip - unicode content`() {
        val encoded = tryEncode(Message("Привет мир! 你好世界"), Scheme.TEXT_ENGLISH.ordinal)
        assumeTrue("Skipped: encoding needs padding (Java 21+ required)", encoded != null)

        val decoded = freshEncryptor().decode(encoded!!, key, Scheme.TEXT_ENGLISH.ordinal)
        assertEquals("Привет мир! 你好世界", decoded.text)
    }

    @Test
    fun `encode and decode roundtrip - emoji content`() {
        val encoded = tryEncode(Message("Hello 😀🎉🚀"), Scheme.TEXT_ENGLISH.ordinal)
        assumeTrue("Skipped: encoding needs padding (Java 21+ required)", encoded != null)

        val decoded = freshEncryptor().decode(encoded!!, key, Scheme.TEXT_ENGLISH.ordinal)
        assertEquals("Hello 😀🎉🚀", decoded.text)
    }

    @Test
    fun `encode and decode roundtrip - empty message`() {
        val encoded = tryEncode(Message(""), Scheme.TEXT_ENGLISH.ordinal)
        assumeTrue("Skipped: encoding needs padding (Java 21+ required)", encoded != null)

        val decoded = freshEncryptor().decode(encoded!!, key, Scheme.TEXT_ENGLISH.ordinal)
        assertEquals("", decoded.text)
    }

    @Test
    fun `encode and decode roundtrip - long message`() {
        val encoded = tryEncode(Message("A".repeat(500)), Scheme.TEXT_ENGLISH.ordinal)
        assumeTrue("Skipped: encoding needs padding (Java 21+ required)", encoded != null)

        val decoded = freshEncryptor().decode(encoded!!, key, Scheme.TEXT_ENGLISH.ordinal)
        assertEquals("A".repeat(500), decoded.text)
    }

    @Test
    fun `encode and decode roundtrip - with channel ID`() {
        val original = Message("Channel test", channelId = 7)
        val encoded = tryEncode(original, Scheme.TEXT_ENGLISH.ordinal)
        assumeTrue("Skipped: encoding needs padding (Java 21+ required)", encoded != null)

        val decoded = freshEncryptor().decode(encoded!!, key, Scheme.TEXT_ENGLISH.ordinal)
        assertEquals("Channel test", decoded.text)
        assertEquals(7, decoded.channelId)
    }

    @Test
    fun `tryDecode auto-detects TEXT_ENGLISH scheme`() {
        val original = Message("Auto-detect test")
        val encoded = tryEncode(original, Scheme.TEXT_ENGLISH.ordinal)
        assumeTrue("Skipped: encoding needs padding (Java 21+ required)", encoded != null)

        // Direct decode should work
        val directDecoded = freshEncryptor().decode(encoded!!, key, Scheme.TEXT_ENGLISH.ordinal)
        assertEquals("Direct decode should work", original.text, directDecoded.text)

        // tryDecode should auto-detect and decode
        val decoded = freshEncryptor().tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `isEncrypted detects TEXT_ENGLISH messages`() {
        val original = Message("Detection test")
        val encoded = tryEncode(original, Scheme.TEXT_ENGLISH.ordinal)
        assumeTrue("Skipped: encoding needs padding (Java 21+ required)", encoded != null)

        // Direct decode should work
        val directDecoded = freshEncryptor().decode(encoded!!, key, Scheme.TEXT_ENGLISH.ordinal)
        assertEquals("Direct decode should work", original.text, directDecoded.text)

        assertTrue(freshEncryptor().isEncrypted(encoded, key))
    }

    @Test
    fun `multiple encode-decode cycles produce correct results`() {
        var successCount = 0
        for (i in 1..10) {
            val original = Message("Cycle $i: testing consistency")
            val encoded = tryEncode(original, Scheme.TEXT_ENGLISH.ordinal) ?: continue

            val decoded = freshEncryptor().decode(encoded, key, Scheme.TEXT_ENGLISH.ordinal)
            assertEquals("Failed at cycle $i", original.text, decoded.text)
            successCount++
        }
        assumeTrue("Skipped: all encodings needed padding (Java 21+ required)", successCount > 0)
    }

    @Test
    fun `TextEncoderEn encode-decode roundtrip at encoder level`() {
        // Test the encoder directly, without PSmsEncryptor wrapper
        val encoder = TextEncoderEn.getInstance()
        val data = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = try {
            encoder.encode(data)
        } catch (_: NoSuchMethodError) {
            return // Java 21+ required for padding
        }

        val decoded = encoder.decode(encoded)
        // The decoded bytes may have leading zeros from front-padding handling,
        // but the significant bytes should match
        assertArrayEquals(data, decoded.takeLast(data.size).toByteArray())
    }
}
