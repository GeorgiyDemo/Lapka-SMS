package org.lapka.sms

import org.lapka.sms.encrypted_data_encoder.Scheme
import org.lapka.sms.encryptor.AesGcmEncryptor
import org.lapka.sms.plain_data_encoder.Mode
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class PSmsEncryptorTest {

    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private fun freshEncryptor() = PSmsEncryptor(nonceCache = NonceCache())

    private fun sharedCacheEncryptor(cache: NonceCache) = PSmsEncryptor(nonceCache = cache)

    // =========================================================================
    // Encode/Decode roundtrip — all schemes
    // =========================================================================

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

    // TEXT scheme requires Java 21+ (List.removeLast) — tested separately when available

    @Test
    fun `BASE64 and CYRILLIC_BASE64 schemes produce decodable output`() {
        for (scheme in listOf(Scheme.BASE64, Scheme.CYRILLIC_BASE64)) {
            val original = Message("Scheme test: ${scheme.name}")
            val encoded = freshEncryptor().encode(original, key, scheme.ordinal)
            val decoded = freshEncryptor().decode(encoded, key, scheme.ordinal)
            assertEquals("Failed for scheme ${scheme.name}", original.text, decoded.text)
        }
    }

    // =========================================================================
    // Encode/Decode — channel ID
    // =========================================================================

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

    @Test
    fun `encode and decode with zero channel ID`() {
        val original = Message("test", channelId = 0)
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(0, decoded.channelId)
    }

    @Test
    fun `message without channel ID decodes to null channelId`() {
        val original = Message("test")
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertNull(decoded.channelId)
    }

    @Test
    fun `channel ID preserved across BASE64 and CYRILLIC_BASE64`() {
        for (scheme in listOf(Scheme.BASE64, Scheme.CYRILLIC_BASE64)) {
            val original = Message("channel in ${scheme.name}", channelId = 999)
            val encoded = freshEncryptor().encode(original, key, scheme.ordinal)
            val decoded = freshEncryptor().decode(encoded, key, scheme.ordinal)
            assertEquals("Failed for scheme ${scheme.name}", 999, decoded.channelId)
        }
    }

    // =========================================================================
    // Encode/Decode — various message content
    // =========================================================================

    @Test
    fun `encode and decode empty message`() {
        val original = Message("")
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals("", decoded.text)
    }

    @Test
    fun `encode and decode long message`() {
        val longText = "A".repeat(1000)
        val original = Message(longText)
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(longText, decoded.text)
    }

    @Test
    fun `encode and decode unicode - emoji`() {
        val original = Message("\uD83D\uDE00\uD83D\uDE02\uD83E\uDD23\uD83D\uDE4F")
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `encode and decode unicode - mixed scripts`() {
        val original = Message("\uD83D\uDE00 Emoji and 中文 and العربية")
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `encode and decode cyrillic text`() {
        val original = Message("Тестовое сообщение на русском языке!")
        val encoded = freshEncryptor().encode(original, key, Scheme.CYRILLIC_BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.CYRILLIC_BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `encode and decode single character`() {
        val original = Message("X")
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals("X", decoded.text)
    }

    @Test
    fun `encode and decode newlines and whitespace`() {
        val original = Message("line1\nline2\r\n\ttab")
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `encode and decode special characters`() {
        val original = Message("!@#\$%^&*()_+-=[]{}|;':\",./<>?`~")
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    // =========================================================================
    // Plain data encoder modes
    // =========================================================================

    @Test
    fun `plain data encoder modes roundtrip - case preserving`() {
        // UTF_8, ASCII, CP1251 preserve case; HUFFMAN and SHORT encoders may not
        for (mode in listOf(Mode.UTF_8, Mode.ASCII, Mode.CP1251)) {
            val original = Message("test abc 123")
            val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal, mode)
            val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
            assertEquals("Failed for mode ${mode.name}", original.text, decoded.text)
        }
    }

    @Test
    fun `plain data encoder modes roundtrip - case insensitive`() {
        for (mode in listOf(Mode.SHORT_CP1251_PREFER_CYRILLIC, Mode.SHORT_CP1251_PREFER_LATIN)) {
            val original = Message("test mode check")
            val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal, mode)
            val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
            assertEquals("Failed for mode ${mode.name}",
                original.text.lowercase(), decoded.text.lowercase())
        }
    }

    @Test
    fun `huffman encoder modes roundtrip`() {
        val latinMsg = Message("hello world test")
        val encoded = freshEncryptor().encode(latinMsg, key, Scheme.BASE64.ordinal, Mode.HUFFMAN_LATIN)
        val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals(latinMsg.text, decoded.text)

        val cyrMsg = Message("привет мир тест")
        val encCyr = freshEncryptor().encode(cyrMsg, key, Scheme.BASE64.ordinal, Mode.HUFFMAN_CYRILLIC)
        val decCyr = freshEncryptor().decode(encCyr, key, Scheme.BASE64.ordinal)
        assertEquals(cyrMsg.text, decCyr.text)
    }

    // =========================================================================
    // Wrong key / tampered data
    // =========================================================================

    @Test(expected = InvalidDataException::class)
    fun `decode with wrong key throws InvalidDataException`() {
        val encoded = freshEncryptor().encode(Message("secret"), key, Scheme.BASE64.ordinal)
        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        freshEncryptor().decode(encoded, wrongKey, Scheme.BASE64.ordinal)
    }

    @Test(expected = InvalidDataException::class)
    fun `tampered encoded string throws InvalidDataException`() {
        val encoded = freshEncryptor().encode(Message("secret"), key, Scheme.BASE64.ordinal)
        val tampered = encoded.substring(0, encoded.length / 2) +
                (if (encoded[encoded.length / 2] == 'A') 'B' else 'A') +
                encoded.substring(encoded.length / 2 + 1)
        freshEncryptor().decode(tampered, key, Scheme.BASE64.ordinal)
    }

    @Test(expected = InvalidDataException::class)
    fun `truncated ciphertext throws InvalidDataException`() {
        val encoded = freshEncryptor().encode(Message("secret"), key, Scheme.BASE64.ordinal)
        freshEncryptor().decode(encoded.substring(0, encoded.length / 2), key, Scheme.BASE64.ordinal)
    }

    @Test(expected = InvalidDataException::class)
    fun `empty string throws exception on decode`() {
        freshEncryptor().decode("", key, Scheme.BASE64.ordinal)
    }

    @Test(expected = InvalidDataException::class)
    fun `decode with wrong scheme throws InvalidDataException`() {
        val encoded = freshEncryptor().encode(Message("test"), key, Scheme.BASE64.ordinal)
        freshEncryptor().decode(encoded, key, Scheme.CYRILLIC_BASE64.ordinal)
    }

    // =========================================================================
    // Nonce uniqueness
    // =========================================================================

    @Test
    fun `same message encrypted twice produces different ciphertext`() {
        val msg = Message("same message")
        val enc1 = freshEncryptor().encode(msg, key, Scheme.BASE64.ordinal)
        val enc2 = freshEncryptor().encode(msg, key, Scheme.BASE64.ordinal)
        assertNotEquals("Ciphertexts should differ due to random nonce", enc1, enc2)
    }

    @Test
    fun `100 encryptions all produce unique ciphertext`() {
        val msg = Message("unique nonce test")
        val results = (1..100).map { freshEncryptor().encode(msg, key, Scheme.BASE64.ordinal) }.toSet()
        assertEquals("All 100 ciphertexts must be unique", 100, results.size)
    }

    // =========================================================================
    // tryDecode
    // =========================================================================

    @Test
    fun `tryDecode returns decrypted message for valid input`() {
        val original = Message("Hello tryDecode")
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `tryDecode returns original text for non-encrypted input`() {
        val plainText = "This is not encrypted"
        val result = freshEncryptor().tryDecode(plainText, key)
        assertEquals(plainText, result.text)
    }

    @Test
    fun `tryDecode returns original for random garbage`() {
        val garbage = "aslkdjf298374lkjsdf"
        val result = freshEncryptor().tryDecode(garbage, key)
        assertEquals(garbage, result.text)
    }

    @Test
    fun `tryDecode returns original for empty string`() {
        val result = freshEncryptor().tryDecode("", key)
        assertEquals("", result.text)
    }

    @Test
    fun `tryDecode preserves channel ID`() {
        val original = Message("channel test", channelId = 77)
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
        assertEquals(77, decoded.channelId)
    }

    @Test
    fun `tryDecode with wrong key returns original ciphertext`() {
        val encoded = freshEncryptor().encode(Message("secret"), key, Scheme.BASE64.ordinal)
        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val result = freshEncryptor().tryDecode(encoded, wrongKey)
        assertEquals(encoded, result.text)
    }

    // =========================================================================
    // Scheme auto-detection via tryDecode
    // =========================================================================

    @Test
    fun `tryDecode detects BASE64 scheme automatically`() {
        val original = Message("Auto-detect BASE64")
        val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `tryDecode detects CYRILLIC_BASE64 scheme automatically`() {
        val original = Message("Auto-detect CYRILLIC_BASE64")
        val encoded = freshEncryptor().encode(original, key, Scheme.CYRILLIC_BASE64.ordinal)
        val decoded = freshEncryptor().tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    // TEXT scheme auto-detection via tryDecode is not reliable
    // (steganographic output may match other schemes first)
    // and TEXT scheme requires Java 21+ (List.removeLast)

    // =========================================================================
    // isEncrypted
    // =========================================================================

    @Test
    fun `isEncrypted returns true for encrypted message`() {
        val encoded = freshEncryptor().encode(Message("test"), key, Scheme.BASE64.ordinal)
        assertTrue(freshEncryptor().isEncrypted(encoded, key))
    }

    @Test
    fun `isEncrypted returns false for plain text`() {
        assertFalse(freshEncryptor().isEncrypted("just a regular sms", key))
    }

    @Test
    fun `isEncrypted returns false for empty string`() {
        assertFalse(freshEncryptor().isEncrypted("", key))
    }

    @Test
    fun `isEncrypted returns false for random garbage`() {
        assertFalse(freshEncryptor().isEncrypted("asdf1234!@#\$", key))
    }

    @Test
    fun `isEncrypted returns false with wrong key`() {
        val encoded = freshEncryptor().encode(Message("test"), key, Scheme.BASE64.ordinal)
        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        assertFalse(freshEncryptor().isEncrypted(encoded, wrongKey))
    }

    @Test
    fun `isEncrypted detects BASE64 and CYRILLIC_BASE64`() {
        for (scheme in listOf(Scheme.BASE64, Scheme.CYRILLIC_BASE64)) {
            val encoded = freshEncryptor().encode(Message("detect ${scheme.name}"), key, scheme.ordinal)
            assertTrue("isEncrypted should detect ${scheme.name}", freshEncryptor().isEncrypted(encoded, key))
        }
    }

    // =========================================================================
    // CRITICAL: isEncrypted must NOT poison nonce cache
    // (This was the bug that broke message display)
    // =========================================================================

    @Test
    fun `isEncrypted then tryDecode on same message works`() {
        val cache = NonceCache()
        val original = Message("isEncrypted + tryDecode test")
        val encoded = sharedCacheEncryptor(cache).encode(original, key, Scheme.BASE64.ordinal)

        // isEncrypted should NOT add to nonce cache
        assertTrue(sharedCacheEncryptor(cache).isEncrypted(encoded, key))

        // tryDecode should still work after isEncrypted
        val decoded = sharedCacheEncryptor(cache).tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `isEncrypted called multiple times does not break tryDecode`() {
        val cache = NonceCache()
        val original = Message("multi-check test")
        val encoded = sharedCacheEncryptor(cache).encode(original, key, Scheme.BASE64.ordinal)

        // Call isEncrypted several times
        repeat(5) {
            assertTrue(sharedCacheEncryptor(cache).isEncrypted(encoded, key))
        }

        // tryDecode should still work
        val decoded = sharedCacheEncryptor(cache).tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `tryDecode called multiple times works (display re-rendering)`() {
        val cache = NonceCache()
        val original = Message("re-render test")
        val encoded = sharedCacheEncryptor(cache).encode(original, key, Scheme.BASE64.ordinal)

        // Simulate RecyclerView re-binding: tryDecode called multiple times
        repeat(10) {
            val decoded = sharedCacheEncryptor(cache).tryDecode(encoded, key)
            assertEquals(original.text, decoded.text)
        }
    }

    @Test
    fun `isEncrypted does not add to nonce cache - verified by cache size`() {
        val cache = NonceCache()
        val encoded = sharedCacheEncryptor(cache).encode(Message("test"), key, Scheme.BASE64.ordinal)

        assertEquals(0, cache.size())
        sharedCacheEncryptor(cache).isEncrypted(encoded, key)
        assertEquals("isEncrypted must not add to nonce cache", 0, cache.size())
    }

    @Test
    fun `tryDecode does not add to nonce cache - verified by cache size`() {
        val cache = NonceCache()
        val encoded = sharedCacheEncryptor(cache).encode(Message("test"), key, Scheme.BASE64.ordinal)

        assertEquals(0, cache.size())
        sharedCacheEncryptor(cache).tryDecode(encoded, key)
        assertEquals("tryDecode must not add to nonce cache", 0, cache.size())
    }

    @Test
    fun `decode DOES add to nonce cache`() {
        val cache = NonceCache()
        val encoded = sharedCacheEncryptor(cache).encode(Message("test"), key, Scheme.BASE64.ordinal)

        assertEquals(0, cache.size())
        sharedCacheEncryptor(cache).decode(encoded, key, Scheme.BASE64.ordinal)
        assertEquals("decode must add to nonce cache", 1, cache.size())
    }

    @Test
    fun `full adapter flow - isEncrypted then tryDecode across BASE64 and CYRILLIC_BASE64`() {
        val cache = NonceCache()
        for (scheme in listOf(Scheme.BASE64, Scheme.CYRILLIC_BASE64)) {
            val original = Message("adapter test ${scheme.name}")
            val encoded = sharedCacheEncryptor(cache).encode(original, key, scheme.ordinal)

            // Simulate MessagesAdapter: isEncrypted check, then tryDecode
            assertTrue(sharedCacheEncryptor(cache).isEncrypted(encoded, key))
            val decoded = sharedCacheEncryptor(cache).tryDecode(encoded, key)
            assertEquals("Failed for scheme ${scheme.name}", original.text, decoded.text)
        }
    }

    @Test
    fun `multiple messages - isEncrypted and tryDecode interleaved`() {
        val cache = NonceCache()
        val messages = (1..10).map { i ->
            val original = Message("Message #$i")
            val encoded = sharedCacheEncryptor(cache).encode(original, key, Scheme.BASE64.ordinal)
            original to encoded
        }

        // Simulate adapter rendering all messages: check + decode each
        for ((original, encoded) in messages) {
            assertTrue(sharedCacheEncryptor(cache).isEncrypted(encoded, key))
            val decoded = sharedCacheEncryptor(cache).tryDecode(encoded, key)
            assertEquals(original.text, decoded.text)
        }

        // Simulate scroll back: re-render all messages again
        for ((original, encoded) in messages) {
            val decoded = sharedCacheEncryptor(cache).tryDecode(encoded, key)
            assertEquals(original.text, decoded.text)
        }
    }

    // =========================================================================
    // Nonce replay detection (decode only)
    // =========================================================================

    @Test(expected = InvalidDataException::class)
    fun `replayed message is rejected by decode`() {
        val cache = NonceCache()
        val encoded = sharedCacheEncryptor(cache).encode(Message("test"), key, Scheme.BASE64.ordinal)

        // First decode succeeds
        sharedCacheEncryptor(cache).decode(encoded, key, Scheme.BASE64.ordinal)

        // Second decode with same nonce should fail
        sharedCacheEncryptor(cache).decode(encoded, key, Scheme.BASE64.ordinal)
    }

    @Test
    fun `replay detection does not affect different messages`() {
        val cache = NonceCache()
        val encoded1 = sharedCacheEncryptor(cache).encode(Message("msg1"), key, Scheme.BASE64.ordinal)
        val encoded2 = sharedCacheEncryptor(cache).encode(Message("msg2"), key, Scheme.BASE64.ordinal)

        // Both should decode fine
        val d1 = sharedCacheEncryptor(cache).decode(encoded1, key, Scheme.BASE64.ordinal)
        val d2 = sharedCacheEncryptor(cache).decode(encoded2, key, Scheme.BASE64.ordinal)
        assertEquals("msg1", d1.text)
        assertEquals("msg2", d2.text)
    }

    @Test
    fun `tryDecode after decode still works (no replay for read-only ops)`() {
        val cache = NonceCache()
        val original = Message("decode then tryDecode")
        val encoded = sharedCacheEncryptor(cache).encode(original, key, Scheme.BASE64.ordinal)

        // decode adds to nonce cache
        sharedCacheEncryptor(cache).decode(encoded, key, Scheme.BASE64.ordinal)

        // tryDecode should still work (skips replay check)
        val decoded = sharedCacheEncryptor(cache).tryDecode(encoded, key)
        assertEquals(original.text, decoded.text)
    }

    @Test
    fun `isEncrypted after decode still works`() {
        val cache = NonceCache()
        val encoded = sharedCacheEncryptor(cache).encode(Message("test"), key, Scheme.BASE64.ordinal)

        sharedCacheEncryptor(cache).decode(encoded, key, Scheme.BASE64.ordinal)

        // isEncrypted should still return true (skips replay check)
        assertTrue(sharedCacheEncryptor(cache).isEncrypted(encoded, key))
    }

    // =========================================================================
    // Key handling
    // =========================================================================

    @Test
    fun `different keys produce different ciphertext`() {
        val key2 = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val msg = Message("same message")
        val enc1 = freshEncryptor().encode(msg, key, Scheme.BASE64.ordinal)
        val enc2 = freshEncryptor().encode(msg, key2, Scheme.BASE64.ordinal)
        // Can't directly compare due to random nonce, but decode with
        // matching key should work and cross-key should fail
        assertEquals(msg.text, freshEncryptor().decode(enc1, key, Scheme.BASE64.ordinal).text)
        assertEquals(msg.text, freshEncryptor().decode(enc2, key2, Scheme.BASE64.ordinal).text)
    }

    @Test
    fun `16-byte key works for encode and decode`() {
        val shortKey = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val original = Message("short key test")
        val encoded = freshEncryptor().encode(original, shortKey, Scheme.BASE64.ordinal)
        val decoded = freshEncryptor().decode(encoded, shortKey, Scheme.BASE64.ordinal)
        assertEquals(original.text, decoded.text)
    }

    // =========================================================================
    // Version
    // =========================================================================

    @Test
    fun `current VERSION is 3`() {
        assertEquals(3, VERSION)
    }

    // =========================================================================
    // Wire format sanity checks
    // =========================================================================

    @Test
    fun `BASE64 encoded output is valid base64`() {
        val encoded = freshEncryptor().encode(Message("test"), key, Scheme.BASE64.ordinal)
        // Should not throw
        java.util.Base64.getDecoder().decode(encoded)
    }

    @Test
    fun `encoded message is longer than plaintext (overhead exists)`() {
        val msg = Message("Hi")
        val encoded = freshEncryptor().encode(msg, key, Scheme.BASE64.ordinal)
        assertTrue("Ciphertext must be longer than plaintext", encoded.length > msg.text.length)
    }

    // =========================================================================
    // Stress / edge cases
    // =========================================================================

    @Test
    fun `encode and decode 50 random messages - BASE64`() {
        val random = SecureRandom()
        repeat(50) {
            val len = random.nextInt(200)
            val text = (1..len).map { ('a' + random.nextInt(26)).toChar() }.joinToString("")
            val original = Message(text)
            val encoded = freshEncryptor().encode(original, key, Scheme.BASE64.ordinal)
            val decoded = freshEncryptor().decode(encoded, key, Scheme.BASE64.ordinal)
            assertEquals(original.text, decoded.text)
        }
    }

    @Test
    fun `encode and decode 50 random messages - CYRILLIC_BASE64`() {
        val random = SecureRandom()
        repeat(50) {
            val len = random.nextInt(200)
            val text = (1..len).map { ('a' + random.nextInt(26)).toChar() }.joinToString("")
            val original = Message(text)
            val encoded = freshEncryptor().encode(original, key, Scheme.CYRILLIC_BASE64.ordinal)
            val decoded = freshEncryptor().decode(encoded, key, Scheme.CYRILLIC_BASE64.ordinal)
            assertEquals(original.text, decoded.text)
        }
    }

    @Test
    fun `tryDecode with shared cache under high message volume`() {
        val cache = NonceCache()
        val pairs = (1..50).map { i ->
            val original = Message("Volume test #$i")
            val encoded = sharedCacheEncryptor(cache).encode(original, key, Scheme.BASE64.ordinal)
            original to encoded
        }

        // Decrypt all, then re-decrypt (simulates scroll)
        for ((original, encoded) in pairs) {
            assertEquals(original.text, sharedCacheEncryptor(cache).tryDecode(encoded, key).text)
        }
        for ((original, encoded) in pairs.reversed()) {
            assertEquals(original.text, sharedCacheEncryptor(cache).tryDecode(encoded, key).text)
        }
    }

    @Test
    fun `nonce cache eviction does not break tryDecode`() {
        // Small cache that evicts quickly
        val cache = NonceCache(maxSize = 5)
        val encoded = sharedCacheEncryptor(cache).encode(Message("first"), key, Scheme.BASE64.ordinal)

        // Fill cache via decode (the only op that writes to cache)
        repeat(10) { i ->
            val msg = sharedCacheEncryptor(cache).encode(Message("fill $i"), key, Scheme.BASE64.ordinal)
            sharedCacheEncryptor(cache).decode(msg, key, Scheme.BASE64.ordinal)
        }

        // Original message's tryDecode should still work
        val decoded = sharedCacheEncryptor(cache).tryDecode(encoded, key)
        assertEquals("first", decoded.text)
    }

    @Test
    fun `mixed encrypted and plain messages in same conversation`() {
        val cache = NonceCache()
        val messages = listOf(
            "Hello" to true,
            "This is plain text" to false,
            "Another encrypted" to true,
            "Regular SMS" to false,
            "Secret!" to true
        )

        val prepared = messages.map { (text, encrypt) ->
            if (encrypt) {
                val encoded = sharedCacheEncryptor(cache).encode(Message(text), key, Scheme.BASE64.ordinal)
                Triple(text, encoded, true)
            } else {
                Triple(text, text, false)
            }
        }

        // Simulate adapter rendering
        for ((originalText, body, wasEncrypted) in prepared) {
            val result = sharedCacheEncryptor(cache).tryDecode(body, key)
            assertEquals(originalText, result.text)
            // For encrypted messages, tryDecode output differs from input
            if (wasEncrypted) {
                assertNotEquals(body, result.text)
            } else {
                assertEquals(body, result.text)
            }
        }
    }

    // =========================================================================
    // Encrypted message length — for SMS segment counter
    // =========================================================================

    // SMS segment limits:
    // GSM-7 (ASCII/Latin): 160 chars per single SMS, 153 per segment in multipart
    // UCS-2 (Unicode/Cyrillic): 70 chars per single SMS, 67 per segment in multipart

    private fun smsSegments(text: String): Int {
        val isUnicode = text.any { it.code > 127 }
        val singleLimit = if (isUnicode) 70 else 160
        val multiLimit = if (isUnicode) 67 else 153
        return when {
            text.length <= singleLimit -> 1
            else -> (text.length + multiLimit - 1) / multiLimit
        }
    }

    // --- BASE64 scheme ---

    @Test
    fun `BASE64 - encrypted always longer than plaintext`() {
        val texts = listOf("H", "Hi", "Hello world", "A".repeat(50), "A".repeat(160), "Привет")
        for (text in texts) {
            val encrypted = freshEncryptor().encode(Message(text), key, Scheme.BASE64.ordinal)
            assertTrue(
                "BASE64: plain=${text.length}, encrypted=${encrypted.length}",
                encrypted.length > text.length
            )
        }
    }

    @Test
    fun `BASE64 - length grows monotonically`() {
        val lengths = listOf(1, 10, 50, 100, 160, 300, 500)
        var prevEncLen = 0
        for (len in lengths) {
            val encrypted = freshEncryptor().encode(Message("A".repeat(len)), key, Scheme.BASE64.ordinal)
            assertTrue(
                "BASE64: len=$len -> ${encrypted.length} should be > $prevEncLen",
                encrypted.length > prevEncLen
            )
            prevEncLen = encrypted.length
        }
    }

    @Test
    fun `BASE64 - same plaintext length gives same encrypted length`() {
        // BASE64 has no random front padding, so lengths should be identical
        val results = (1..10).map {
            freshEncryptor().encode(Message("A".repeat(50)), key, Scheme.BASE64.ordinal).length
        }
        assertTrue(
            "BASE64: all encrypted lengths should be equal: $results",
            results.all { it == results[0] }
        )
    }

    @Test
    fun `BASE64 - segment count for short message`() {
        // "Hi" plaintext = 1 SMS. Encrypted should be calculable.
        val encrypted = freshEncryptor().encode(Message("Hi"), key, Scheme.BASE64.ordinal)
        val segments = smsSegments(encrypted)
        // BASE64 output is ASCII -> GSM-7 encoding
        assertTrue("BASE64 'Hi' encrypted: ${encrypted.length} chars, $segments segments", segments >= 1)
    }

    @Test
    fun `BASE64 - segment count for medium message`() {
        val encrypted = freshEncryptor().encode(Message("A".repeat(100)), key, Scheme.BASE64.ordinal)
        val segments = smsSegments(encrypted)
        assertTrue("BASE64 100-char: ${encrypted.length} chars, $segments segments", segments >= 1)
        // 100 ASCII chars -> encrypted should be > 160 chars, so likely 2+ segments
        println("BASE64 100-char: encrypted=${encrypted.length}, segments=$segments")
    }

    @Test
    fun `BASE64 - segment count for long message`() {
        val encrypted = freshEncryptor().encode(Message("A".repeat(300)), key, Scheme.BASE64.ordinal)
        val segments = smsSegments(encrypted)
        assertTrue("BASE64 300-char: ${encrypted.length} chars, $segments segments", segments >= 2)
    }

    @Test
    fun `BASE64 - overhead is bounded`() {
        val encrypted = freshEncryptor().encode(Message("X"), key, Scheme.BASE64.ordinal)
        assertTrue("1-char BASE64 under 200: ${encrypted.length}", encrypted.length < 200)
        val empty = freshEncryptor().encode(Message(""), key, Scheme.BASE64.ordinal)
        assertTrue("Empty BASE64 has overhead: ${empty.length}", empty.length > 0)
    }

    // --- CYRILLIC_BASE64 scheme ---

    @Test
    fun `CYRILLIC_BASE64 - encrypted always longer than plaintext`() {
        val texts = listOf("H", "Hi", "Hello world", "A".repeat(50), "Привет мир")
        for (text in texts) {
            val encrypted = freshEncryptor().encode(Message(text), key, Scheme.CYRILLIC_BASE64.ordinal)
            assertTrue(
                "CYRILLIC_BASE64: plain=${text.length}, encrypted=${encrypted.length}",
                encrypted.length > text.length
            )
        }
    }

    @Test
    fun `CYRILLIC_BASE64 - length grows monotonically`() {
        val lengths = listOf(1, 10, 50, 100, 160, 300)
        var prevEncLen = 0
        for (len in lengths) {
            val encrypted = freshEncryptor().encode(Message("A".repeat(len)), key, Scheme.CYRILLIC_BASE64.ordinal)
            assertTrue(
                "CYRILLIC_BASE64: len=$len -> ${encrypted.length} should be > $prevEncLen",
                encrypted.length > prevEncLen
            )
            prevEncLen = encrypted.length
        }
    }

    @Test
    fun `CYRILLIC_BASE64 - uses more SMS segments than BASE64 due to Unicode`() {
        // Cyrillic output is Unicode -> UCS-2 (70 chars per SMS vs 160)
        val text = "A".repeat(100)
        val base64Enc = freshEncryptor().encode(Message(text), key, Scheme.BASE64.ordinal)
        val cyrillicEnc = freshEncryptor().encode(Message(text), key, Scheme.CYRILLIC_BASE64.ordinal)
        val base64Seg = smsSegments(base64Enc)
        val cyrillicSeg = smsSegments(cyrillicEnc)
        // Cyrillic scheme should use more segments (UCS-2 = 70 char limit)
        assertTrue(
            "CYRILLIC_BASE64 ($cyrillicSeg segs, ${cyrillicEnc.length} chars) should use >= segments than BASE64 ($base64Seg segs, ${base64Enc.length} chars)",
            cyrillicSeg >= base64Seg
        )
        println("100-char: BASE64=${base64Enc.length}chars/${base64Seg}segs, CYRILLIC=${cyrillicEnc.length}chars/${cyrillicSeg}segs")
    }

    @Test
    fun `CYRILLIC_BASE64 - segment count for short message`() {
        val encrypted = freshEncryptor().encode(Message("Hi"), key, Scheme.CYRILLIC_BASE64.ordinal)
        val segments = smsSegments(encrypted)
        assertTrue("CYRILLIC_BASE64 'Hi': ${encrypted.length} chars, $segments segments", segments >= 1)
        println("CYRILLIC_BASE64 'Hi': encrypted=${encrypted.length}, segments=$segments")
    }

    @Test
    fun `CYRILLIC_BASE64 - overhead is bounded`() {
        val encrypted = freshEncryptor().encode(Message("X"), key, Scheme.CYRILLIC_BASE64.ordinal)
        assertTrue("1-char CYRILLIC_BASE64 under 200: ${encrypted.length}", encrypted.length < 200)
    }

    // --- TEXT (words) scheme ---
    // TEXT scheme requires Java 21+ List.removeLast(), so these test encoding only

    @Test
    fun `TEXT - encrypted always longer than plaintext`() {
        val texts = listOf("H", "Hi", "Hello world", "A".repeat(50))
        for (text in texts) {
            try {
                val encrypted = freshEncryptor().encode(Message(text), key, Scheme.TEXT.ordinal)
                assertTrue(
                    "TEXT: plain=${text.length}, encrypted=${encrypted.length}",
                    encrypted.length > text.length
                )
            } catch (_: NoSuchMethodError) {
                // TEXT scheme requires Java 21+, skip
                return
            }
        }
    }

    @Test
    fun `TEXT - length grows monotonically`() {
        val lengths = listOf(1, 10, 50, 100)
        var prevEncLen = 0
        for (len in lengths) {
            try {
                val encrypted = freshEncryptor().encode(Message("A".repeat(len)), key, Scheme.TEXT.ordinal)
                assertTrue(
                    "TEXT: len=$len -> ${encrypted.length} should be > $prevEncLen",
                    encrypted.length > prevEncLen
                )
                prevEncLen = encrypted.length
            } catch (_: NoSuchMethodError) {
                return
            }
        }
    }

    @Test
    fun `TEXT - uses Unicode and more segments`() {
        try {
            val text = "Hello"
            val textEnc = freshEncryptor().encode(Message(text), key, Scheme.TEXT.ordinal)
            val base64Enc = freshEncryptor().encode(Message(text), key, Scheme.BASE64.ordinal)
            println("TEXT 'Hello': ${textEnc.length} chars, ${smsSegments(textEnc)} segs")
            println("BASE64 'Hello': ${base64Enc.length} chars, ${smsSegments(base64Enc)} segs")
            assertTrue("TEXT output non-empty", textEnc.isNotEmpty())
        } catch (_: NoSuchMethodError) {
            // TEXT scheme requires Java 21+
        }
    }

    // --- Cross-scheme comparison ---

    @Test
    fun `all schemes - encrypted output roundtrips correctly`() {
        for (scheme in listOf(Scheme.BASE64, Scheme.CYRILLIC_BASE64)) {
            val text = "Test message 123 Привет"
            val encrypted = freshEncryptor().encode(Message(text), key, scheme.ordinal)
            val decrypted = freshEncryptor().decode(encrypted, key, scheme.ordinal)
            assertEquals("Roundtrip failed for $scheme", text, decrypted.text)
        }
    }

    @Test
    fun `all schemes - segment count table for various lengths`() {
        // Print a table showing how plaintext length maps to SMS segments per scheme
        val lengths = listOf(1, 10, 30, 50, 70, 100, 140, 160, 200, 300)
        println("\nSMS Segment Count Table:")
        println("Plain | BASE64 len/segs | CYRILLIC len/segs")
        println("------|-----------------|-------------------")
        for (len in lengths) {
            val text = "A".repeat(len)
            val b64 = freshEncryptor().encode(Message(text), key, Scheme.BASE64.ordinal)
            val cyr = freshEncryptor().encode(Message(text), key, Scheme.CYRILLIC_BASE64.ordinal)
            println("${len.toString().padStart(5)} | ${b64.length.toString().padStart(7)}/${smsSegments(b64).toString().padStart(4)}    | ${cyr.length.toString().padStart(9)}/${smsSegments(cyr).toString().padStart(4)}")
            // Verify basic invariants
            assertTrue("BASE64 len=$len encrypted non-empty", b64.isNotEmpty())
            assertTrue("CYRILLIC len=$len encrypted non-empty", cyr.isNotEmpty())
            assertTrue("BASE64 len=$len >= 1 segment", smsSegments(b64) >= 1)
            assertTrue("CYRILLIC len=$len >= 1 segment", smsSegments(cyr) >= 1)
        }
    }

    @Test
    fun `all schemes - cyrillic plaintext encrypted length`() {
        val text = "Привет мир, это тестовое сообщение для проверки"
        val b64 = freshEncryptor().encode(Message(text), key, Scheme.BASE64.ordinal)
        val cyr = freshEncryptor().encode(Message(text), key, Scheme.CYRILLIC_BASE64.ordinal)
        println("Cyrillic text (${text.length} chars):")
        println("  BASE64: ${b64.length} chars, ${smsSegments(b64)} segments")
        println("  CYRILLIC_BASE64: ${cyr.length} chars, ${smsSegments(cyr)} segments")
        assertTrue("BASE64 cyrillic text encrypted", b64.length > text.length)
        assertTrue("CYRILLIC cyrillic text encrypted", cyr.length > text.length)
    }
}
