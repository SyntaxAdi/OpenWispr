package com.openwispr.util

object Constants {
    const val DEFAULT_BASE_URL = "https://api.groq.com/openai/v1"
    const val DEFAULT_WHISPER = "whisper-large-v3-turbo"
    const val DEFAULT_LLAMA = "openai/gpt-oss-20b"
    val WHISPER_MODELS = listOf("whisper-large-v3-turbo", "whisper-large-v3")
    val LLAMA_MODELS = listOf("openai/gpt-oss-20b", "openai/gpt-oss-120b", "qwen/qwen3.8-27b", "allam-2-7b")
    const val DEFAULT_BUBBLE_DP = 56
    const val DEFAULT_MAX_SEC = 60
    const val MIN_BUBBLE_DP = 40
    const val MAX_BUBBLE_DP = 100
    const val MIN_REC_SEC = 15
    const val MAX_REC_SEC = 600
    const val DEFAULT_BUBBLE_OPACITY = 100
    const val MIN_BUBBLE_OPACITY = 20
    const val MAX_BUBBLE_OPACITY = 100
    const val SYSTEM_PROMPT = "You are a speech cleaner. Remove stutters, filler words (uh, um, like, you know, ah, hmm), repetitions, rambling and false starts. Fix grammar and punctuation. Preserve original intent and meaning. Return ONLY the cleaned text, no quotes, no explanation, no prefix like 'Message:', 'Here is the cleaned text:', or similar."

    const val FORMALITY_FORMAL_PROMPT = "You are a speech cleaner and professional business editor. Remove stutters, filler words (uh, um, like, you know, ah, hmm), repetitions, rambling and false starts. Always use proper capitalization and full punctuation (periods, commas, semicolons, question marks). Transform the tone into formal, highly polished, grammatically impeccable business English suitable for professional correspondence. Return ONLY the cleaned text, no quotes, no explanation, no prefix."

    const val FORMALITY_FRIENDLY_PROMPT = "You are a speech cleaner and friendly chat editor. Remove stutters, filler words (uh, um, like, you know, ah, hmm), repetitions, rambling and false starts. Use strictly NO capitalization (convert all text to lowercase, no capital letters anywhere) and strictly NO punctuation marks (no periods, no commas, no exclamation marks, no question marks, no hyphens, no semicolons). Output must be completely lowercase with zero punctuation, warm, friendly, and approachable. Return ONLY the cleaned text, no quotes, no explanation, no prefix."

    const val FORMALITY_CASUAL_PROMPT = "You are a speech cleaner and casual messaging editor. Remove stutters, filler words (uh, um, like, you know, ah, hmm), repetitions, rambling and false starts. Use standard capitalization (capitalize sentence beginnings and proper nouns/I), but use minimal/less punctuation (avoid rigid ending periods, avoid unnecessary commas or semicolons, keep it natural and conversational like modern messaging). Keep the tone very casual, relaxed, natural, and informal for everyday messaging. Return ONLY the cleaned text, no quotes, no explanation, no prefix."

    private val PREFIX_RE = Regex("""^(\s*(Message|Cleaned(\s*text)?|Transcription|Output|Result|Refined)\s*[:\-–—]\s*)+""", RegexOption.IGNORE_CASE)
    private val PREAMBLE_RE = Regex("""^(Here is|Here's|Sure,?\s*here is) (the )?(cleaned|transcribed|corrected|refined).*?:\s*""", RegexOption.IGNORE_CASE)

    /** Strip LLM-added prefixes like "Message:", "Cleaned text:" and surrounding quotes. */
    fun sanitizeOutput(text: String): String {
        var t = text.trim()
        t = PREFIX_RE.replace(t, "")
        t = PREAMBLE_RE.replace(t, "")
        // Trim surrounding quotes / markdown fences
        t = t.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```").removeSuffix("```").trim()
            // remove optional language tag line
            if (t.startsWith("text") || t.startsWith("plain")) t = t.substringAfter("\n").trim()
        }
        return t.removeSurrounding("\"").removeSurrounding("'").removeSurrounding("“").removeSurrounding("”").trim()
    }

    /** Format long model identifiers into clean, human-readable monospace badges (e.g. "openai/gpt-oss-20b" -> "GPT-OSS 20B"). */
    fun formatShortModelName(model: String): String {
        val clean = model.substringAfter('/')
        return when {
            clean.contains("gpt-oss-20b", ignoreCase = true) -> "GPT-OSS 20B"
            clean.contains("gpt-oss-120b", ignoreCase = true) -> "GPT-OSS 120B"
            clean.contains("qwen", ignoreCase = true) && clean.contains("27b", ignoreCase = true) -> "QWEN 27B"
            clean.contains("allam", ignoreCase = true) -> "ALLAM 7B"
            clean.contains("70b", ignoreCase = true) -> "LLAMA 70B"
            clean.contains("8b", ignoreCase = true) -> "LLAMA 8B"
            clean.contains("turbo", ignoreCase = true) -> "WHISPER TURBO"
            clean.contains("whisper", ignoreCase = true) -> "WHISPER V3"
            clean.isNotBlank() -> clean.uppercase().take(14)
            else -> "DEFAULT"
        }
    }
}