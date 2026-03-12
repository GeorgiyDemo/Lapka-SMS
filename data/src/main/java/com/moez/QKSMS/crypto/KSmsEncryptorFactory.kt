package com.moez.QKSMS.crypto

import org.lapka.sms.PSmsEncryptor

object KSmsEncryptorFactory {
    fun create(): PSmsEncryptor {
        return PSmsEncryptor()
    }
}
