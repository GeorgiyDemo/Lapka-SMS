package org.lapka.sms.plain_data_encoder

interface PlainDataEncoder {
    fun encode(s: String): ByteArray
    fun decode(data: ByteArray): String
    val mode: Int
}
