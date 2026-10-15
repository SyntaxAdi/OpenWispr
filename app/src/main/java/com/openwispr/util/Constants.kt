package com.openwispr.util

object Constants {
    const val DEFAULT_BASE_URL = "https://api.groq.com/openai/v1"
    const val DEFAULT_WHISPER = "whisper-large-v3-turbo"
    const val DEFAULT_LLAMA = "llama-3.1-8b-instant"
    val WHISPER_MODELS = listOf("whisper-large-v3-turbo", "whisper-large-v3")
    val LLAMA_MODELS = listOf("llama-3.1-8b-instant", "llama-3.3-70b-versatile", "llama-3.1-70b-versatile")
    const val DEFAULT_BUBBLE_DP = 56
    const val DEFAULT_MAX_SEC = 60
    const val MIN_BUBBLE_DP = 40
    const val MAX_BUBBLE_DP = 100
    const val MIN_REC_SEC = 15
    const val MAX_REC_SEC = 120
    const val DEFAULT_BUBBLE_OPACITY = 100
    const val MIN_BUBBLE_OPACITY = 20
    const val MAX_BUBBLE_OPACITY = 100
    const val SYSTEM_PROMPT = "You are a speech cleaner. Remove stutters, filler words (uh, um, like, you know, ah, hmm), repetitions, rambling and false starts. Fix grammar and punctuation. Preserve original intent and meaning. Return ONLY the cleaned text, no quotes, no explanation, no prefix like 'Message:', 'Here is the cleaned text:', or similar."

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
}