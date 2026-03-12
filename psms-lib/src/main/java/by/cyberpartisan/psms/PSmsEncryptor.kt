package by.cyberpartisan.psms

import by.cyberpartisan.psms.encrypted_data_encoder.EncryptedDataEncoder
import by.cyberpartisan.psms.encrypted_data_encoder.EncryptedDataEncoderFactory
import by.cyberpartisan.psms.encrypted_data_encoder.EncryptedDataEncoderFactoryImpl
import by.cyberpartisan.psms.encrypted_data_encoder.Scheme
import by.cyberpartisan.psms.encryptor.AesGcmEncryptor
import by.cyberpartisan.psms.encryptor.Encryptor
import by.cyberpartisan.psms.plain_data_encoder.Mode
import by.cyberpartisan.psms.plain_data_encoder.PlainDataEncoder
import by.cyberpartisan.psms.plain_data_encoder.PlainDataEncoderFactory
import by.cyberpartisan.psms.plain_data_encoder.PlainDataEncoderFactoryImpl
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HASH_SIZE = 2
private const val CHANNEL_ID_SIZE = 4
const val VERSION = 2
private const val TIMESTAMP_SIZE = 4
private const val PADDING_BLOCK_SIZE = 16
private const val MAX_MESSAGE_AGE_SECONDS = 48 * 3600L
private const val MAX_FUTURE_SECONDS = 300L

private val ENC_KEY_INFO = "k-sms-v2-enc".toByteArray()
private val MAC_KEY_INFO = "k-sms-v2-mac".toByteArray()

class PSmsEncryptor(
    private val plainDataEncoderFactory: PlainDataEncoderFactory = PlainDataEncoderFactoryImpl(),
    private val encryptedDataEncoderFactory: EncryptedDataEncoderFactory = EncryptedDataEncoderFactoryImpl(),
    private val encryptor: Encryptor = AesGcmEncryptor()
) {
    private var plainDataEncoder: PlainDataEncoder? = null
    private var encryptedDataEncoder: EncryptedDataEncoder? = null

    private fun deriveKeys(masterKey: ByteArray): Pair<ByteArray, ByteArray> {
        val encKey = Hkdf.deriveKey(masterKey, ENC_KEY_INFO, 32)
        val macKey = Hkdf.deriveKey(masterKey, MAC_KEY_INFO, 32)
        return encKey to macKey
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun currentTimestamp(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }

    private fun validateTimestamp(timestamp: Int) {
        val now = System.currentTimeMillis() / 1000
        val ts = timestamp.toLong() and 0xFFFFFFFFL
        if (ts > now + MAX_FUTURE_SECONDS) throw InvalidDataException("Message timestamp is in the future")
        if (ts < now - MAX_MESSAGE_AGE_SECONDS) throw InvalidDataException("Message is too old")
    }

    private fun addPadding(data: ByteArray): ByteArray {
        val padLen = PADDING_BLOCK_SIZE - (data.size % PADDING_BLOCK_SIZE)
        return data + ByteArray(padLen) { padLen.toByte() }
    }

    private fun removePadding(data: ByteArray): ByteArray {
        if (data.isEmpty()) throw InvalidDataException("Empty padded data")
        val padLen = data.last().toInt() and 0xFF
        if (padLen < 1 || padLen > PADDING_BLOCK_SIZE) throw InvalidDataException("Invalid padding")
        if (data.size < padLen) throw InvalidDataException("Invalid padding length")
        for (i in data.size - padLen until data.size) {
            if ((data[i].toInt() and 0xFF) != padLen) throw InvalidDataException("Invalid padding bytes")
        }
        return data.copyOf(data.size - padLen)
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

    private fun pack(data: ByteArray, macKey: ByteArray, channelId: Int?): ByteArray {
        val encoder = plainDataEncoder!!
        val metaInfo = MetaInfo(encoder.mode, VERSION, channelId != null)
        val channelIdBytes = if (channelId != null) intToByteArray(channelId) else byteArrayOf()
        val timestamp = intToByteArray(currentTimestamp())
        val payload = data + channelIdBytes + timestamp
        val packed = payload + byteArrayOf(metaInfo.toByte()) + hmacSha256(macKey, payload).copyOf(HASH_SIZE)
        return addPadding(packed)
    }

    private fun unpack(data: ByteArray, macKey: ByteArray): Triple<Int?, ByteArray, Int> {
        val unpadded = removePadding(data)
        if (unpadded.size < HASH_SIZE + 1 + TIMESTAMP_SIZE) throw InvalidDataException()

        val endPosition = unpadded.size - HASH_SIZE - 1
        val payload = unpadded.copyOf(endPosition)
        val metaInfoByte = unpadded[endPosition]
        val hashFromMessage = unpadded.copyOfRange(unpadded.size - HASH_SIZE, unpadded.size)
        val calculatedHash = hmacSha256(macKey, payload).copyOf(HASH_SIZE)

        if (!MessageDigest.isEqual(hashFromMessage, calculatedHash)) throw InvalidDataException()

        val metaInfo = MetaInfo.parse(metaInfoByte)
        if (metaInfo.version > VERSION) throw InvalidVersionException()
        if (metaInfo.isChannel && payload.size < CHANNEL_ID_SIZE + TIMESTAMP_SIZE) throw InvalidDataException()

        plainDataEncoder = plainDataEncoderFactory.create(metaInfo.mode)

        val timestampOffset = payload.size - TIMESTAMP_SIZE
        val timestamp = byteArrayToInt(payload.copyOfRange(timestampOffset, timestampOffset + TIMESTAMP_SIZE))

        val dataEnd = if (metaInfo.isChannel) timestampOffset - CHANNEL_ID_SIZE else timestampOffset
        val channelId = if (metaInfo.isChannel) {
            byteArrayToInt(payload.copyOfRange(dataEnd, dataEnd + CHANNEL_ID_SIZE))
        } else null
        val textBytes = payload.copyOf(dataEnd)

        return Triple(channelId, textBytes, timestamp)
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
        val (encKey, macKey) = deriveKeys(key)
        this.plainDataEncoder = plainDataEncoder
        this.encryptedDataEncoder = encryptedDataEncoderFactory.create(encryptionSchemeId)
        val encoded = plainDataEncoder.encode(message.text)
        val packed = pack(encoded, macKey, message.channelId)
        val encrypted = encryptor.encrypt(encKey, packed)
        return encryptedDataEncoder!!.encode(encrypted)
    }

    // --- Decode ---

    fun decode(str: String, key: ByteArray, encryptionSchemeId: Int): Message {
        val (encKey, macKey) = deriveKeys(key)
        encryptedDataEncoder = encryptedDataEncoderFactory.create(encryptionSchemeId)
        var raw = encryptedDataEncoder!!.decode(str)
        if (encryptedDataEncoder!!.hasFrontPadding()) {
            while (true) {
                try {
                    return decodeRaw(raw, encKey, macKey)
                } catch (_: InvalidDataException) {
                    raw = raw.sliceArray(1 until raw.size)
                    if (raw.isEmpty()) throw InvalidDataException()
                }
            }
        }
        return decodeRaw(raw, encKey, macKey)
    }

    private fun decodeRaw(raw: ByteArray, encKey: ByteArray, macKey: ByteArray): Message {
        val decrypted = encryptor.decrypt(encKey, raw)
        val (channelId, textBytes, timestamp) = unpack(decrypted, macKey)
        validateTimestamp(timestamp)
        return Message(plainDataEncoder!!.decode(textBytes), channelId)
    }

    // --- Utility ---

    fun isEncrypted(str: String, key: ByteArray): Boolean {
        for (scheme in Scheme.values()) {
            try {
                decode(str, key, scheme.ordinal)
                return true
            } catch (_: InvalidDataException) { }
        }
        return false
    }

    fun tryDecode(str: String, key: ByteArray): Message {
        for (scheme in Scheme.values()) {
            try {
                return decode(str, key, scheme.ordinal)
            } catch (_: InvalidDataException) { }
        }
        return Message(str)
    }
}
