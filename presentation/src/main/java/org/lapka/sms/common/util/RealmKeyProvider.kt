package org.lapka.sms.common.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object RealmKeyProvider {

    private const val KEYSTORE_ALIAS = "lapka_realm_key"
    private const val PREFS_NAME = "realm_key_prefs"
    private const val PREFS_KEY_ENCRYPTED = "encrypted_realm_key"
    private const val PREFS_KEY_IV = "realm_key_iv"

    fun getOrCreateRealmKey(context: Context): ByteArray? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val encryptedKeyBase64 = prefs.getString(PREFS_KEY_ENCRYPTED, null)
            val ivBase64 = prefs.getString(PREFS_KEY_IV, null)

            if (encryptedKeyBase64 != null && ivBase64 != null) {
                decryptRealmKey(encryptedKeyBase64, ivBase64)
            } else {
                val realmKey = generateRealmKey()
                encryptAndStoreRealmKey(context, realmKey)
                realmKey
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get/create Realm encryption key")
            null
        }
    }

    private fun generateRealmKey(): ByteArray {
        val key = ByteArray(64)
        java.security.SecureRandom().nextBytes(key)
        return key
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun encryptAndStoreRealmKey(context: Context, realmKey: ByteArray) {
        val keystoreKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)

        val encryptedKey = cipher.doFinal(realmKey)
        val iv = cipher.iv

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_KEY_ENCRYPTED, Base64.encodeToString(encryptedKey, Base64.NO_WRAP))
            .putString(PREFS_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    private fun decryptRealmKey(encryptedKeyBase64: String, ivBase64: String): ByteArray {
        val keystoreKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(Base64.decode(encryptedKeyBase64, Base64.NO_WRAP))
    }
}
