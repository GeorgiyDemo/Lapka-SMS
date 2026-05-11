package org.lapka.sms.encrypted_data_encoder

interface EncryptedDataEncoderFactory {
    fun create(schemeId: Int): EncryptedDataEncoder
}
