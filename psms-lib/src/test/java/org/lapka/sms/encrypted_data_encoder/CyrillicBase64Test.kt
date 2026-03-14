package org.lapka.sms.encrypted_data_encoder

import org.junit.Assert.*
import org.junit.Test
import org.lapka.sms.InvalidDataException
import java.security.SecureRandom

class CyrillicBase64Test {

    private val encoder = CyrillicBase64()

    @Test
    fun `encode and decode roundtrip`() {
        val data = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val encoded = encoder.encode(data)
        val decoded = encoder.decode(encoded)
        assertArrayEquals(data, decoded)
    }

    @Test
    fun `encoded output contains only cyrillic characters`() {
        val data = byteArrayOf(0, 127, -128, -1, 64, 32, 16, 8)
        val encoded = encoder.encode(data)
        val cyrillic = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя"
        for (c in encoded) {
            assertTrue("Character '$c' should be in cyrillic alphabet", cyrillic.contains(c))
        }
    }

    @Test
    fun `encoded output contains no latin characters`() {
        val data = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = encoder.encode(data)
        for (c in encoded) {
            assertFalse("Should not contain latin char '$c'", c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9')
        }
    }

    @Test
    fun `decode invalid character throws InvalidDataException`() {
        try {
            encoder.decode("Hello")
            fail("Should throw InvalidDataException")
        } catch (_: InvalidDataException) {
            // expected
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
    fun `roundtrip with single byte`() {
        for (b in 0..255) {
            val data = byteArrayOf(b.toByte())
            val encoded = encoder.encode(data)
            val decoded = encoder.decode(encoded)
            assertArrayEquals("Failed for byte $b", data, decoded)
        }
    }

    @Test
    fun `roundtrip with random data`() {
        val random = SecureRandom()
        repeat(50) {
            val size = random.nextInt(100) + 1
            val data = ByteArray(size).also { random.nextBytes(it) }
            val encoded = encoder.encode(data)
            val decoded = encoder.decode(encoded)
            assertArrayEquals("Failed for random data of size $size", data, decoded)
        }
    }

    @Test
    fun `hasFrontPadding returns false`() {
        assertFalse(encoder.hasFrontPadding())
    }

    @Test
    fun `cyrillic and latin alphabets have same length`() {
        // This is an invariant the encoder depends on
        val cyrillic = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя"
        val latin = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
        assertEquals(cyrillic.length, latin.length)
    }

    @Test
    fun `encode produces different output than standard base64`() {
        val data = byteArrayOf(72, 101, 108, 108, 111) // "Hello"
        val cyrillicEncoded = encoder.encode(data)
        val base64Encoded = Base64().encode(data)
        assertNotEquals(cyrillicEncoded, base64Encoded)
    }
}
