package org.lapka.sms

import org.lapka.sms.encrypted_data_encoder.EncryptedDataEncoder
import org.lapka.sms.encrypted_data_encoder.EncryptedDataEncoderFactory
import org.lapka.sms.encrypted_data_encoder.EncryptedDataEncoderFactoryImpl
import org.lapka.sms.encrypted_data_encoder.Scheme
import org.lapka.sms.encryptor.AesGcmEncryptor
import org.lapka.sms.plain_data_encoder.Mode
import org.lapka.sms.plain_data_encoder.PlainDataEncoder
import org.lapka.sms.plain_data_encoder.PlainDataEncoderFactory
import org.lapka.sms.plain_data_encoder.PlainDataEncoderFactoryImpl

private const val CHANNEL_ID_SIZE = 4
const val VERSION = 3
private const val MAX_FRONT_PADDING_STRIP = 256
private const val MAX_MESSAGE_AGE_SECONDS = 24 * 3600L
private const val MAX_FUTURE_SECONDS = 300L

private val ENC_KEY_INFO = "k-sms-v2-enc".toByteArray()

class PSmsEncryptor(
    private val plainDataEncoderFactory: PlainDataEncoderFactory = PlainDataEncoderFactoryImpl(),
    private val encryptedDataEncoderFactory: EncryptedDataEncoderFactory = EncryptedDataEncoderFactoryImpl(),
    private val aesGcmEncryptor: AesGcmEncryptor = AesGcmEncryptor(),
    private val nonceCache: NonceCache = NonceCache.getDefault()
) {
    private var plainDataEncoder: PlainDataEncoder? = null
    private var encryptedDataEncoder: EncryptedDataEncoder? = null

    private fun deriveEncKey(masterKey: ByteArray): ByteArray {
        return Hkdf.deriveKey(masterKey, ENC_KEY_INFO, 32)
    }

    private fun validateTimestamp(timestamp: Int) {
        val now = System.currentTimeMillis() / 1000
        val ts = timestamp.toLong() and 0xFFFFFFFFL
        if (ts > now + MAX_FUTURE_SECONDS) throw InvalidDataException("Message timestamp is in the future")
        if (ts < now - MAX_MESSAGE_AGE_SECONDS) throw InvalidDataException("Message is too old")
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 0).toByte(),
            (value shr 8).toByte(),
            (value shr 16).toByte(),
            (value shr 24).toByte()
        )
    }

    private fun byteArrayToInt(data: ByteArray): Int {
        return (data[3].toInt() shl 24) or
                ((data[2].toInt() and 0xFF) shl 16) or
                ((data[1].toInt() and 0xFF) shl 8) or
                (data[0].toInt() and 0xFF)
    }

    private fun pack(data: ByteArray, channelId: Int?): ByteArray {
        val encoder = plainDataEncoder!!
        val metaInfo = MetaInfo(encoder.mode, VERSION, channelId != null)
        val channelIdBytes = if (channelId != null) intToByteArray(channelId) else byteArrayOf()
        return data + channelIdBytes + byteArrayOf(metaInfo.toByte())
    }

    private fun unpack(data: ByteArray): Triple<Int?, ByteArray, MetaInfo> {
        if (data.isEmpty()) throw InvalidDataException("Empty data")

        val metaInfoByte = data[data.size - 1]
        val metaInfo = MetaInfo.parse(metaInfoByte)
        if (metaInfo.version > VERSION) throw InvalidVersionException()

        val payloadEnd = data.size - 1

        if (metaInfo.isChannel && payloadEnd < CHANNEL_ID_SIZE) throw InvalidDataException()

        plainDataEncoder = plainDataEncoderFactory.create(metaInfo.mode)

        val dataEnd = if (metaInfo.isChannel) payloadEnd - CHANNEL_ID_SIZE else payloadEnd
        val channelId = if (metaInfo.isChannel) {
            byteArrayToInt(data.copyOfRange(dataEnd, dataEnd + CHANNEL_ID_SIZE))
        } else null
        val textBytes = data.copyOf(dataEnd)

        return Triple(channelId, textBytes, metaInfo)
    }

    // --- Encode ---

    fun encode(message: Message, key: ByteArray, encryptionSchemeId: Int): String {
        return encode(message, key, encryptionSchemeId, plainDataEncoderFactory.createBestEncoder(message.text))
    }

    fun encode(message: Message, key: ByteArray, encryptionSchemeId: Int, plainDataEncoderMode: Mode): String {
        return encode(message, key, encryptionSchemeId, plainDataEncoderFactory.create(plainDataEncoderMode))
    }

    fun encode(message: Message, key: ByteArray, encryptionSchemeId: Int, plainDataEncoderMode: Int): String {
        return encode(message, key, encryptionSchemeId, plainDataEncoderFactory.create(plainDataEncoderMode))
    }

    fun encode(message: Message, key: ByteArray, encryptionSchemeId: Int, plainDataEncoder: PlainDataEncoder): String {
        val encKey = deriveEncKey(key)
        this.plainDataEncoder = plainDataEncoder
        this.encryptedDataEncoder = encryptedDataEncoderFactory.create(encryptionSchemeId)
        val encoded = plainDataEncoder.encode(message.text)
        val packed = pack(encoded, message.channelId)
        val encrypted = aesGcmEncryptor.encrypt(encKey, packed)
        return encryptedDataEncoder!!.encode(encrypted)
    }

    // --- Decode ---

    fun decode(str: String, key: ByteArray, encryptionSchemeId: Int): Message {
        return decodeInternal(str, key, encryptionSchemeId, checkReplay = true)
    }

    private fun decodeInternal(str: String, key: ByteArray, encryptionSchemeId: Int, checkReplay: Boolean): Message {
        val encKey = deriveEncKey(key)
        encryptedDataEncoder = encryptedDataEncoderFactory.create(encryptionSchemeId)
        var raw = encryptedDataEncoder!!.decode(str)
        if (encryptedDataEncoder!!.hasFrontPadding()) {
            var stripped = 0
            while (true) {
                try {
                    return decodeRaw(raw, encKey, checkReplay)
                } catch (_: InvalidDataException) {
                    raw = raw.sliceArray(1 until raw.size)
                    stripped++
                    if (raw.isEmpty() || stripped >= MAX_FRONT_PADDING_STRIP) throw InvalidDataException()
                }
            }
        }
        return decodeRaw(raw, encKey, checkReplay)
    }

    private fun decodeRaw(raw: ByteArray, encKey: ByteArray, checkReplay: Boolean = true): Message {
        if (raw.size < AesGcmEncryptor.GCM_NONCE_LENGTH + AesGcmEncryptor.GCM_TAG_BYTES) {
            throw InvalidDataException()
        }

        // Extract nonce to get timestamp before decryption (saves CPU on old messages)
        val nonce = raw.sliceArray(0 until AesGcmEncryptor.GCM_NONCE_LENGTH)
        val timestamp = AesGcmEncryptor.extractTimestampFromNonce(nonce)
        validateTimestamp(timestamp)

        // GCM decryption (authenticates entire payload including MetaInfo)
        val decrypted = aesGcmEncryptor.decrypt(encKey, raw)

        if (decrypted.isEmpty()) throw InvalidDataException("Empty decrypted data")

        if (checkReplay) {
            // Check for replay (only after GCM auth succeeds — prevents cache poisoning)
            if (nonceCache.contains(nonce)) {
                throw InvalidDataException("Replayed message (duplicate nonce)")
            }
            nonceCache.add(nonce)
        }

        val (channelId, textBytes, _) = unpack(decrypted)

        return Message(plainDataEncoder!!.decode(textBytes), channelId)
    }

    // --- Utility ---

    /**
     * Quick pre-check: steg decode + min length + timestamp validation.
     * Returns decoded bytes if plausible, null if definitely not encrypted.
     * Avoids expensive GCM decryption on obviously non-encrypted messages.
     */
    private fun quickCheck(str: String, schemeId: Int): ByteArray? {
        val encoder = encryptedDataEncoderFactory.create(schemeId)
        val raw = try {
            encoder.decode(str)
        } catch (_: Exception) {
            return null
        }
        val minSize = AesGcmEncryptor.GCM_NONCE_LENGTH + AesGcmEncryptor.GCM_TAG_BYTES + 1 // +1 for MetaInfo
        if (raw.size < minSize) return null
        val timestamp = AesGcmEncryptor.extractTimestampFromNonce(raw)
        val now = System.currentTimeMillis() / 1000
        val ts = timestamp.toLong() and 0xFFFFFFFFL
        if (ts > now + MAX_FUTURE_SECONDS || ts < now - MAX_MESSAGE_AGE_SECONDS) return null
        return raw
    }

    fun isEncrypted(str: String, key: ByteArray): Boolean {
        for (scheme in Scheme.values()) {
            if (quickCheck(str, scheme.ordinal) == null) continue
            try {
                // Skip replay check — isEncrypted is a read-only probe
                // that must not poison the nonce cache
                decodeInternal(str, key, scheme.ordinal, checkReplay = false)
                return true
            } catch (_: InvalidDataException) {
            }
        }
        return false
    }

    fun tryDecode(str: String, key: ByteArray): Message {
        for (scheme in Scheme.values()) {
            if (quickCheck(str, scheme.ordinal) == null) continue
            try {
                // Skip replay check — tryDecode is used for display/read-back,
                // not for incoming message processing
                return decodeInternal(str, key, scheme.ordinal, checkReplay = false)
            } catch (_: InvalidDataException) {
            }
        }
        return Message(str)
    }
}
