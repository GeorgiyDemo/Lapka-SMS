package org.lapka.sms.encrypted_data_encoder

import org.junit.Assert.*
import org.junit.Test
import org.lapka.sms.InvalidDataException
import java.security.SecureRandom

class Base64EncoderTest {

    private val encoder = Base64()

    @Test
    fun `encode and decode roundtrip`() {
        val data = byteArrayOf(72, 101, 108, 108, 111) // "Hello"
        val encoded = encoder.encode(data)
        val decoded = encoder.decode(encoded)
        assertArrayEquals(data, decoded)
    }

    @Test
    fun `encode produces valid base64 characters`() {
        val data = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = encoder.encode(data)
        val validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
        for (c in encoded) {
            assertTrue("'$c' should be valid base64", validChars.contains(c))
        }
    }

    @Test
    fun `roundtrip with empty data`() {
        val data = byteArrayOf()
        val encoded = encoder.encode(data)
        val decoded = encoder.decode(encoded)
        assertArrayEquals(data, decoded)
    }

    @Test
    fun `roundtrip with single byte all values`() {
        for (b in 0..255) {
            val data = byteArrayOf(b.toByte())
            val decoded = encoder.decode(encoder.encode(data))
            assertArrayEquals("Failed for byte $b", data, decoded)
        }
    }

    @Test
    fun `roundtrip with random data`() {
        val random = SecureRandom()
        repeat(50) {
            val size = random.nextInt(200) + 1
            val data = ByteArray(size).also { random.nextBytes(it) }
            val decoded = encoder.decode(encoder.encode(data))
            assertArrayEquals("Failed for size $size", data, decoded)
        }
    }

    @Test
    fun `decode garbage input does not crash`() {
        // Apache Commons Base64 silently ignores invalid chars
        // and returns whatever it can decode. Verify no crash.
        try {
            encoder.decode("!!!not-base64!!!")
        } catch (_: InvalidDataException) {
            // acceptable
        }
    }

    @Test
    fun `encoded output has no newlines`() {
        val data = ByteArray(200).also { SecureRandom().nextBytes(it) }
        val encoded = encoder.encode(data)
        assertFalse("Should not contain newlines", encoded.contains("\n"))
        assertFalse("Should not contain carriage returns", encoded.contains("\r"))
    }

    @Test
    fun `hasFrontPadding returns false`() {
        assertFalse(encoder.hasFrontPadding())
    }

    @Test
    fun `known value encoding`() {
        // "Hello" = SGVsbG8=
        val data = "Hello".toByteArray(Charsets.US_ASCII)
        val encoded = encoder.encode(data)
        assertEquals("SGVsbG8=", encoded)
    }
}
