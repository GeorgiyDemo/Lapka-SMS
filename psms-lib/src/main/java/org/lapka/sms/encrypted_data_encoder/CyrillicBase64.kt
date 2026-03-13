package org.lapka.sms.encrypted_data_encoder

import org.lapka.sms.InvalidDataException

class CyrillicBase64 : EncryptedDataEncoder {

    private val cyrillic = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя"
    private val latin = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="

    override fun hasFrontPadding(): Boolean = false

    override fun encode(data: ByteArray): String {
        val base64 = Base64().encode(data)
        return buildString(base64.length) {
            for (c in base64) {
                if (c.isWhitespace()) continue
                val idx = latin.indexOf(c)
                if (idx < 0) throw InvalidDataException("Unexpected Base64 character: '$c'")
                append(cyrillic[idx])
            }
        }
    }

    override fun decode(str: String): ByteArray {
        val base64 = buildString(str.length) {
            for (c in str) {
                val idx = cyrillic.indexOf(c)
                if (idx < 0) throw InvalidDataException("string is not in valid Cyrillic Base64 scheme")
                append(latin[idx])
            }
        }
        return Base64().decode(base64)
    }
}
