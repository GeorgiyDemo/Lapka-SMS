package org.lapka.sms.encrypted_data_encoder

import org.lapka.sms.InvalidDataException

class Base64 : EncryptedDataEncoder {

    override fun hasFrontPadding(): Boolean = false

    override fun encode(data: ByteArray): String {
        return try {
            org.apache.commons.codec.binary.Base64.encodeBase64String(data).replace("\r\n", "").replace("\n", "")
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
        }
    }

    override fun decode(str: String): ByteArray {
        return try {
            org.apache.commons.codec.binary.Base64.decodeBase64(str)
        } catch (_: Throwable) {
            try {
                android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
            } catch (_: IllegalArgumentException) {
                throw InvalidDataException("Not base64 string.")
            }
        }
    }
}
