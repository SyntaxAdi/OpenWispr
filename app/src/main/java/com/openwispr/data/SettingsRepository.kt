package com.openwispr.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.openwispr.util.Constants
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SettingsRepository(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("openwispr_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() {
            val enc = sp.getString("api_key_enc", null)
            if (enc != null) {
                val decrypted = decrypt(enc)
                if (decrypted != null) return decrypted
            }
            // Auto-migrate legacy plaintext key if present
            val legacy = sp.getString("api_key", null)
            if (!legacy.isNullOrBlank()) {
                val encrypted = encrypt(legacy)
                if (encrypted != null) {
                    sp.edit().putString("api_key_enc", encrypted).remove("api_key").apply()
                }
                return legacy
            }
            return ""
        }
        set(v) {
            val trimmed = v.trim()
            if (trimmed.isEmpty()) {
                sp.edit().remove("api_key_enc").remove("api_key").apply()
                return
            }
            val encrypted = encrypt(trimmed)
            if (encrypted != null) {
                sp.edit().putString("api_key_enc", encrypted).remove("api_key").apply()
            } else {
                // Fallback for non-AndroidKeyStore environments
                sp.edit().putString("api_key", trimmed).apply()
            }
        }

    var baseUrl: String
        get() = sp.getString("base_url", Constants.DEFAULT_BASE_URL) ?: Constants.DEFAULT_BASE_URL
        set(v) = sp.edit().putString("base_url", v).apply()

    var whisper: String
        get() = sp.getString("whisper_model", Constants.DEFAULT_WHISPER) ?: Constants.DEFAULT_WHISPER
        set(v) = sp.edit().putString("whisper_model", v).apply()

    var llama: String
        get() = sp.getString("llama_model", Constants.DEFAULT_LLAMA) ?: Constants.DEFAULT_LLAMA
        set(v) = sp.edit().putString("llama_model", v).apply()

    var bubbleDp: Int
        get() = sp.getInt("bubble_dp", Constants.DEFAULT_BUBBLE_DP)
        set(v) = sp.edit().putInt("bubble_dp", v.coerceIn(Constants.MIN_BUBBLE_DP, Constants.MAX_BUBBLE_DP)).apply()

    var bubbleOpacity: Int
        get() = sp.getInt("bubble_opacity", Constants.DEFAULT_BUBBLE_OPACITY)
        set(v) = sp.edit().putInt("bubble_opacity", v.coerceIn(Constants.MIN_BUBBLE_OPACITY, Constants.MAX_BUBBLE_OPACITY)).apply()

    var maxSec: Int
        get() = sp.getInt("max_sec", Constants.DEFAULT_MAX_SEC)
        set(v) = sp.edit().putInt("max_sec", v.coerceIn(Constants.MIN_REC_SEC, Constants.MAX_REC_SEC)).apply()

    var bubbleX: Int
        get() = sp.getInt("bubble_x", Int.MIN_VALUE)
        private set(v) = sp.edit().putInt("bubble_x", v).apply()

    var bubbleY: Int
        get() = sp.getInt("bubble_y", Int.MIN_VALUE)
        private set(v) = sp.edit().putInt("bubble_y", v).apply()

    fun setBubblePos(x: Int, y: Int) {
        sp.edit().putInt("bubble_x", x).putInt("bubble_y", y).apply()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "openwispr_credential_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12

        private fun getOrCreateSecretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                return keyGenerator.generateKey()
            }
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        fun encrypt(plainText: String): String? {
            if (plainText.isEmpty()) return ""
            return try {
                val key = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val iv = cipher.iv
                val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                Base64.getEncoder().encodeToString(iv + cipherText)
            } catch (_: Throwable) {
                null
            }
        }

        fun decrypt(encryptedBase64: String): String? {
            if (encryptedBase64.isEmpty()) return ""
            return try {
                val combined = Base64.getDecoder().decode(encryptedBase64)
                if (combined.size <= IV_LENGTH) return null
                val key = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val iv = combined.copyOfRange(0, IV_LENGTH)
                val cipherText = combined.copyOfRange(IV_LENGTH, combined.size)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
                String(cipher.doFinal(cipherText), Charsets.UTF_8)
            } catch (_: Throwable) {
                null
            }
        }
    }
}