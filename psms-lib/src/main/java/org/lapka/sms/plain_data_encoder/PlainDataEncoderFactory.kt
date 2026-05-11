package org.lapka.sms.plain_data_encoder

interface PlainDataEncoderFactory {
    fun create(mode: Int): PlainDataEncoder
    fun create(mode: Mode): PlainDataEncoder
    fun createBestEncoder(s: String): PlainDataEncoder
}
