package com.openwispr.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.openwispr.util.Constants
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.Base64
import java.util.Locale
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
                // Purge corrupt or un-decryptable ciphertext from invalidated keystore
                logW("OpenWispr:Settings", "Purging corrupt or invalidated encrypted key")
                sp.edit().remove("api_key_enc").apply()
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
                // Fallback for non-AndroidKeyStore environments: save plaintext, purge enc
                sp.edit().putString("api_key", trimmed).remove("api_key_enc").apply()
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

    var customBaseSystemPrompt: String
        get() = sp.getString("custom_system_prompt", Constants.SYSTEM_PROMPT) ?: Constants.SYSTEM_PROMPT
        set(v) = sp.edit().putString("custom_system_prompt", v.trim()).apply()

    fun resetBaseSystemPrompt() {
        sp.edit().putString("custom_system_prompt", Constants.SYSTEM_PROMPT).apply()
    }

    var globalFormalityMode: FormalityMode
        get() {
            val name = sp.getString("global_formality_mode", FormalityMode.DEFAULT.name)
            return try { FormalityMode.valueOf(name ?: FormalityMode.DEFAULT.name) } catch (_: Exception) { FormalityMode.DEFAULT }
        }
        set(v) = sp.edit().putString("global_formality_mode", v.name).apply()

    var activeProfileId: String?
        get() = sp.getString("active_profile_id", null)
        set(v) = sp.edit().putString("active_profile_id", v).apply()

    fun getPromptProfiles(): List<PromptProfile> {
        val raw = sp.getString("prompt_profiles_json", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PromptProfile>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PromptProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        promptText = obj.getString("promptText")
                    )
                )
            }
            list
        } catch (_: Exception) { emptyList() }
    }

    fun savePromptProfile(profile: PromptProfile, makeActive: Boolean = true) {
        val current = getPromptProfiles().toMutableList()
        val idx = current.indexOfFirst { it.id == profile.id }
        if (idx >= 0) {
            current[idx] = profile
        } else {
            current.add(profile)
        }
        val arr = JSONArray()
        current.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("promptText", p.promptText)
            })
        }
        sp.edit().putString("prompt_profiles_json", arr.toString()).apply()
        if (makeActive || activeProfileId == null) {
            activeProfileId = profile.id
        }
    }

    fun deletePromptProfile(profileId: String) {
        val current = getPromptProfiles().filterNot { it.id == profileId }
        val arr = JSONArray()
        current.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("promptText", p.promptText)
            })
        }
        sp.edit().putString("prompt_profiles_json", arr.toString()).apply()
        if (activeProfileId == profileId) {
            activeProfileId = current.firstOrNull()?.id
        }
    }

    fun getAppPromptRules(): Map<String, AppPromptRule> {
        val raw = sp.getString("app_prompt_rules_json", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            val map = mutableMapOf<String, AppPromptRule>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val mode = try {
                    FormalityMode.valueOf(obj.getString("formalityMode"))
                } catch (_: Exception) { FormalityMode.DEFAULT }
                val rule = AppPromptRule(
                    packageName = obj.getString("packageName"),
                    appName = obj.optString("appName", obj.getString("packageName")),
                    formalityMode = mode,
                    customProfileId = if (obj.has("customProfileId") && !obj.isNull("customProfileId")) {
                        obj.getString("customProfileId").takeIf { it.isNotBlank() }
                    } else null
                )
                map[rule.packageName] = rule
            }
            map
        } catch (_: Exception) { emptyMap() }
    }

    fun saveAppPromptRule(rule: AppPromptRule) {
        val current = getAppPromptRules().toMutableMap()
        current[rule.packageName] = rule
        saveAppRulesList(current.values)
    }

    fun deleteAppPromptRule(packageName: String) {
        val current = getAppPromptRules().toMutableMap()
        current.remove(packageName)
        saveAppRulesList(current.values)
    }

    private fun saveAppRulesList(rules: Collection<AppPromptRule>) {
        val arr = JSONArray()
        rules.forEach { r ->
            arr.put(JSONObject().apply {
                put("packageName", r.packageName)
                put("appName", r.appName)
                put("formalityMode", r.formalityMode.name)
                r.customProfileId?.let { put("customProfileId", it) }
            })
        }
        sp.edit().putString("app_prompt_rules_json", arr.toString()).apply()
    }

    fun getEffectiveFormalityMode(packageName: String?): FormalityMode {
        val rule = packageName?.let { getAppPromptRules()[it] }
        return rule?.formalityMode ?: globalFormalityMode
    }

    fun applyFormalityStyling(text: String, mode: FormalityMode): String {
        return Companion.applyFormalityStyling(text, mode)
    }

    fun getEffectiveSystemPrompt(packageName: String?): String {
        val rule = packageName?.let { getAppPromptRules()[it] }
        val mode = rule?.formalityMode ?: globalFormalityMode
        val customId = rule?.customProfileId ?: activeProfileId

        return when (mode) {
            FormalityMode.DEFAULT -> customBaseSystemPrompt
            FormalityMode.FORMAL -> Constants.FORMALITY_FORMAL_PROMPT
            FormalityMode.FRIENDLY -> Constants.FORMALITY_FRIENDLY_PROMPT
            FormalityMode.CASUAL -> Constants.FORMALITY_CASUAL_PROMPT
            FormalityMode.CUSTOM -> {
                val profiles = getPromptProfiles()
                val selected = profiles.firstOrNull { it.id == customId } ?: profiles.firstOrNull()
                selected?.promptText?.takeIf { it.isNotBlank() } ?: customBaseSystemPrompt
            }
        }
    }

    fun getPromptForMode(mode: FormalityMode): String {
        return when (mode) {
            FormalityMode.DEFAULT -> customBaseSystemPrompt
            FormalityMode.FORMAL -> Constants.FORMALITY_FORMAL_PROMPT
            FormalityMode.FRIENDLY -> Constants.FORMALITY_FRIENDLY_PROMPT
            FormalityMode.CASUAL -> Constants.FORMALITY_CASUAL_PROMPT
            FormalityMode.CUSTOM -> {
                val profiles = getPromptProfiles()
                val selected = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull()
                selected?.promptText?.takeIf { it.isNotBlank() } ?: customBaseSystemPrompt
            }
        }
    }

    fun getActiveCustomProfileName(): String? {
        val profiles = getPromptProfiles()
        return (profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull())?.name
    }

    companion object {
        fun applyFormalityStyling(text: String, mode: FormalityMode): String {
            return when (mode) {
                FormalityMode.FORMAL -> {
                    var res = text.trim()
                    if (res.isNotEmpty() && res[0].isLowerCase()) {
                        res = res.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
                    if (res.isNotEmpty() && !res.last().toString().matches(Regex("""[.!?\"']"""))) {
                        res = "$res."
                    }
                    res
                }
                FormalityMode.CASUAL -> {
                    var res = text.trim()
                    if (res.isNotEmpty() && res[0].isLowerCase()) {
                        res = res.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
                    // Strip rigid trailing single period for casual chat feel
                    if (res.endsWith(".") && !res.endsWith("..")) {
                        res = res.dropLast(1).trim()
                    }
                    res
                }
                FormalityMode.FRIENDLY -> {
                    // Strictly no caps + no punctuations
                    text.lowercase(Locale.getDefault())
                        .replace(Regex("""[.,!?;:\"'()\\-_]"""), "")
                        .replace(Regex("""\s+"""), " ")
                        .trim()
                }
                else -> text.trim()
            }
        }

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "openwispr_credential_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12

        private fun getOrCreateSecretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = try {
                keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            } catch (_: Exception) {
                null
            }
            if (existing != null) return existing

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

        private fun logW(tag: String, msg: String) {
            try { android.util.Log.w(tag, msg) } catch (_: Throwable) {}
        }

        fun encrypt(plainText: String): String? {
            if (plainText.isEmpty()) return ""
            return try {
                val key = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val iv = cipher.iv
                val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                val encoded = Base64.getEncoder().encodeToString(iv + cipherText)
                val verified = decrypt(encoded)
                if (verified == plainText) encoded else null
            } catch (t: Throwable) {
                logW("OpenWispr:Settings", "KeyStore encrypt failed: ${t.message}")
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
            } catch (t: Throwable) {
                logW("OpenWispr:Settings", "KeyStore decrypt failed: ${t.message}")
                null
            }
        }
    }
}

enum class FormalityMode(val displayName: String, val description: String) {
    DEFAULT("Default", "Standard speech cleaner (preserves original tone)"),
    FORMAL("Formal", "Polished business English (caps + punctuations)"),
    CASUAL("Casual", "Relaxed messaging (caps + less punctuation)"),
    FRIENDLY("Friendly", "Warm chat (no caps + no punctuations)"),
    CUSTOM("Custom Prompt", "Uses selected custom prompt profile")
}

data class PromptProfile(
    val id: String,
    val name: String,
    val promptText: String
)

data class AppPromptRule(
    val packageName: String,
    val appName: String,
    val formalityMode: FormalityMode,
    val customProfileId: String? = null
)