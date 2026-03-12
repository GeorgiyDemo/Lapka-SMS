package com.moez.QKSMS.crypto

import by.cyberpartisan.psms.PSmsEncryptor
import timber.log.Timber

object KSmsEncryptorFactory {
    fun create(): PSmsEncryptor {
        try {
            val stream = PSmsEncryptor::class.java.classLoader!!.getResourceAsStream("words_base.txt")
            Timber.d("KSmsEncryptorFactory: words_base.txt stream = $stream")
        } catch (e: Exception) {
            Timber.e(e, "KSmsEncryptorFactory: failed to check words_base.txt")
        }
        return PSmsEncryptor()
    }
}
