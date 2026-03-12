package org.lapka.sms.encryptor

interface Encryptor {
    fun encrypt(key: ByteArray, plainData: ByteArray, aad: ByteArray? = null): ByteArray
    fun decrypt(key: ByteArray, encryptedData: ByteArray, aad: ByteArray? = null): ByteArray
}
