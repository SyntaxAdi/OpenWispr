package com.openwispr.util

import java.util.Locale

data class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String = ""
)

object LanguageManager {
    val AUTO_DETECT = LanguageOption(
        code = "auto",
        displayName = "Detect Language (Auto)",
        nativeName = "Auto-Detect"
    )

    val NO_TRANSLATION = LanguageOption(
        code = "off",
        displayName = "Original (No Translation)",
        nativeName = "Original"
    )

    /**
     * Dynamically generated from standard library java.util.Locale.
     * Zero hardcoded language preset arrays.
     */
    val allLanguages: List<LanguageOption> by lazy {
        Locale.getISOLanguages()
            .map { code ->
                val locale = Locale(code)
                val englishName = locale.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
                }
                val nativeName = locale.getDisplayLanguage(locale).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
                LanguageOption(
                    code = code,
                    displayName = englishName,
                    nativeName = nativeName
                )
            }
            .filter { it.displayName.isNotBlank() && it.displayName != it.code }
            .distinctBy { it.displayName }
            .sortedBy { it.displayName }
    }

    /** Spoken languages: Auto-detect always pinned on top. */
    fun getSpokenLanguages(): List<LanguageOption> = listOf(AUTO_DETECT) + allLanguages

    /** Target translation languages: Original (No Translation) always pinned on top. */
    fun getTargetLanguages(): List<LanguageOption> = listOf(NO_TRANSLATION) + allLanguages

    fun getDisplayName(code: String): String {
        return when (code) {
            "auto" -> AUTO_DETECT.displayName
            "off" -> NO_TRANSLATION.displayName
            else -> allLanguages.firstOrNull { it.code.equals(code, ignoreCase = true) }?.displayName ?: code
        }
    }
}
