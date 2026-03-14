package org.lapka.sms.crypto

import org.lapka.sms.PSmsEncryptor

object KSmsEncryptorFactory {
    fun create(): PSmsEncryptor {
        return PSmsEncryptor()
    }
}
