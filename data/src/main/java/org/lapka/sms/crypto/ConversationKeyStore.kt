package org.lapka.sms.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps conversation encryption keys with an Android Keystore master key (AES-256-GCM).
 *
 * Storage format for wrapped keys: "kw1:" + Base64(IV‖ciphertext‖GCM-tag).
 * Legacy (unwrapped) keys are plain Base64-encoded raw key bytes with no prefix.
 *
 * Defence-in-depth: even if the Realm database encryption is bypassed,
 * conversation keys remain protected by hardware-backed Keystore.
 */
object ConversationKeyStore {

    private const val KEYSTORE_ALIAS = "lapka_conversation_key_wrapper"
    private const val WRAPPED_PREFIX = "kw1:"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128

    /**
     * Wrap a raw-key Base64 string for safe storage in Realm.
     * Returns a prefixed string that [unwrapKeyBytes] can decode.
     */
    fun wrapKey(rawKeyBase64: String): String {
        val rawKeyBytes = Base64.decode(rawKeyBase64, Base64.DEFAULT)
        val keystoreKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)
        val encrypted = cipher.doFinal(rawKeyBytes)
        val iv = cipher.iv
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return WRAPPED_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Recover raw key bytes from a stored (possibly wrapped) string.
     * Handles both wrapped ("kw1:…") and legacy plain Base64 keys.
     */
    fun unwrapKeyBytes(storedKey: String): ByteArray {
        if (storedKey.startsWith(WRAPPED_PREFIX)) {
            val combined = Base64.decode(storedKey.removePrefix(WRAPPED_PREFIX), Base64.NO_WRAP)
            val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
            val encrypted = combined.sliceArray(GCM_IV_LENGTH until combined.size)
            val keystoreKey = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            return cipher.doFinal(encrypted)
        }
        // Legacy unwrapped key – decode plain Base64
        return Base64.decode(storedKey, Base64.DEFAULT)
    }

    /** True if the stored key is already wrapped with Keystore. */
    fun isWrapped(storedKey: String): Boolean = storedKey.startsWith(WRAPPED_PREFIX)

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
