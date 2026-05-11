package org.lapka.sms.encrypted_data_encoder

import org.lapka.sms.encrypted_data_encoder.text_encoder.TextEncoder
import org.lapka.sms.encrypted_data_encoder.text_encoder.TextEncoderEn

class EncryptedDataEncoderFactoryImpl : EncryptedDataEncoderFactory {
    override fun create(schemeId: Int): EncryptedDataEncoder {
        return when (schemeId) {
            Scheme.BASE64.ordinal -> Base64()
            Scheme.CYRILLIC_BASE64.ordinal -> CyrillicBase64()
            Scheme.TEXT.ordinal -> TextEncoder.Companion.getInstance()
            Scheme.TEXT_ENGLISH.ordinal -> TextEncoderEn.getInstance()
            else -> Base64()
        }
    }
}
