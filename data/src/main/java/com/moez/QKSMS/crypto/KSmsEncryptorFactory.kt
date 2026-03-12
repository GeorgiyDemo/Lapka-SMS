package com.moez.QKSMS.crypto

import by.cyberpartisan.psms.PSmsEncryptor

object KSmsEncryptorFactory {
    fun create(): PSmsEncryptor = PSmsEncryptor()
}
