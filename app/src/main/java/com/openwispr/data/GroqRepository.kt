package com.openwispr.data

import com.openwispr.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

sealed class GroqResult<out T> {
    data class Success<T>(val data: T) : GroqResult<T>()
    data class Error(val message: String) : GroqResult<Nothing>()
}

class GroqRepository {

    companion object {
        fun validateEndpointUrl(baseUrl: String): String {
            val trimmed = baseUrl.trim().trimEnd('/')
            val url = try { URL(trimmed) } catch (e: Exception) {
                throw IllegalArgumentException("Invalid endpoint URL: ${e.message}")
            }
            if (!url.protocol.equals("https", ignoreCase = true)) {
                throw IllegalArgumentException("Insecure endpoint rejected: HTTPS protocol is strictly required")
            }
            return trimmed
        }

        fun parseTextModels(body: String): List<String> {
            val arr = try { JSONObject(body).optJSONArray("data") } catch (_: Exception) { null } ?: return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val active = obj.optBoolean("active", true)
                val outMods = obj.optJSONArray("output_modalities")?.let { jArr ->
                    (0 until jArr.length()).map { jArr.getString(it) }
                } ?: listOf("text")
                val inMods = obj.optJSONArray("input_modalities")?.let { jArr ->
                    (0 until jArr.length()).map { jArr.getString(it) }
                } ?: listOf("text")

                if (active && "text" in outMods && "text" in inMods &&
                    !id.contains("guard") && !id.contains("whisper") && !id.contains("orpheus")
                ) {
                    list.add(id)
                }
            }
            return list
        }
    }

    suspend fun validateKey(baseUrl: String, apiKey: String): GroqResult<List<String>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = try {
                validateEndpointUrl(baseUrl)
            } catch (e: IllegalArgumentException) {
                return@withContext GroqResult.Error(e.message ?: "Invalid endpoint")
            }
            val url = URL("$endpoint/models")
            val c = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 12000; readTimeout = 12000
            }
            val code = c.responseCode
            val body = readBody(c)
            c.disconnect()
            if (code in 200..299) {
                GroqResult.Success(parseTextModels(body))
            } else {
                GroqResult.Error("HTTP $code: ${extractErr(body)}")
            }
        } catch (e: Exception) { GroqResult.Error(e.message ?: "Network error") }
    }

    suspend fun transcribe(baseUrl: String, apiKey: String, whisperModel: String, audioFile: File): GroqResult<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = try {
                validateEndpointUrl(baseUrl)
            } catch (e: IllegalArgumentException) {
                return@withContext GroqResult.Error(e.message ?: "Invalid endpoint")
            }
            val boundary = "----Wispr${System.currentTimeMillis()}"
            val url = URL("$endpoint/audio/transcriptions")
            val c = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connectTimeout = 30000; readTimeout = 60000
            }
            DataOutputStream(c.outputStream).use { out ->
                writeForm(out, boundary, "model", whisperModel)
                writeForm(out, boundary, "response_format", "json")
                // language omitted -> auto-detect
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${audioFile.name}\"\r\n")
                out.writeBytes("Content-Type: audio/m4a\r\n\r\n")
                FileInputStream(audioFile).use { it.copyTo(out) }
                out.writeBytes("\r\n--$boundary--\r\n")
            }
            val code = c.responseCode
            val body = readBody(c)
            c.disconnect()
            if (code in 200..299) {
                val text = try { JSONObject(body).optString("text", "") } catch (_: Exception) { "" }
                val sanitized = Constants.sanitizeOutput(text)
                if (sanitized.isBlank()) GroqResult.Error("Empty transcription") else GroqResult.Success(sanitized)
            } else GroqResult.Error("Transcribe HTTP $code: ${extractErr(body)}")
        } catch (e: Exception) { GroqResult.Error(e.message ?: "Transcribe failed") }
    }

    suspend fun refine(
        baseUrl: String,
        apiKey: String,
        llamaModel: String,
        rawText: String,
        systemPrompt: String = Constants.SYSTEM_PROMPT,
        temperature: Double = 0.2
    ): GroqResult<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = try {
                validateEndpointUrl(baseUrl)
            } catch (e: IllegalArgumentException) {
                return@withContext GroqResult.Error(e.message ?: "Invalid endpoint")
            }
            val url = URL("$endpoint/chat/completions")
            val c = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000; readTimeout = 30000
            }
            val payload = JSONObject().apply {
                put("model", llamaModel)
                put("temperature", temperature)
                put("max_tokens", 2048)
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", systemPrompt.ifBlank { Constants.SYSTEM_PROMPT }) })
                    put(JSONObject().apply { put("role", "user"); put("content", rawText) })
                })
            }.toString()
            c.outputStream.use { it.write(payload.toByteArray()) }
            val code = c.responseCode
            val body = readBody(c)
            c.disconnect()
            if (code in 200..299) {
                val cleanRaw = try {
                    JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
                } catch (_: Exception) { "" }
                val clean = Constants.sanitizeOutput(cleanRaw)
                // fallback to sanitized raw if LLM returned empty/prefix-only
                GroqResult.Success(if (clean.isBlank()) Constants.sanitizeOutput(rawText) else clean)
            } else {
                val err = extractErr(body)
                if (code == 404 && llamaModel != Constants.DEFAULT_LLAMA &&
                    (err.contains("does not exist") || err.contains("access"))
                ) {
                    return@withContext refine(baseUrl, apiKey, Constants.DEFAULT_LLAMA, rawText, systemPrompt, temperature)
                }
                GroqResult.Error("Refine HTTP $code: $err")
            }
        } catch (e: Exception) { GroqResult.Error(e.message ?: "Refine failed") }
    }

    private fun writeForm(out: DataOutputStream, b: String, name: String, value: String) {
        out.writeBytes("--$b\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        out.writeBytes("$value\r\n")
    }
    private fun readBody(c: HttpURLConnection): String = try {
        val s = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        s?.bufferedReader()?.readText() ?: ""
    } catch (_: Exception) { "" }
    private fun extractErr(body: String): String = try {
        val o = JSONObject(body)
        o.optJSONObject("error")?.optString("message") ?: body.take(300)
    } catch (_: Exception) { body.take(300) }
}