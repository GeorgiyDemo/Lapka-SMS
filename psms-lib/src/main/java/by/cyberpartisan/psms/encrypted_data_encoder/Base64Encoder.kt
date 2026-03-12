package by.cyberpartisan.psms.encrypted_data_encoder

import by.cyberpartisan.psms.InvalidDataException

class Base64 : EncryptedDataEncoder {

    override fun hasFrontPadding(): Boolean = false

    override fun encode(data: ByteArray): String {
        return try {
            org.apache.commons.codec.binary.Base64.encodeBase64String(data)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)
        }
    }

    override fun decode(str: String): ByteArray {
        try {
            org.apache.commons.codec.binary.Base64.isBase64(str)
        } catch (_: Throwable) {
            return try {
                android.util.Base64.decode(str, android.util.Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                throw InvalidDataException("Not base64 string.")
            }
        }
        if (!org.apache.commons.codec.binary.Base64.isBase64(str)) {
            throw InvalidDataException("Not base64 string.")
        }
        return org.apache.commons.codec.binary.Base64.decodeBase64(str)
    }
}
