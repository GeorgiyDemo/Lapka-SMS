package org.lapka.sms.encryptor

interface Encryptor {
    fun encrypt(key: ByteArray, plainData: ByteArray): ByteArray
    fun decrypt(key: ByteArray, encryptedData: ByteArray): ByteArray
}
