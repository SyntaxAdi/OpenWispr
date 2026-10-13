package com.openwispr.data

import android.content.Context
import android.content.SharedPreferences
import com.openwispr.util.Constants

class SettingsRepository(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("openwispr_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(v) = sp.edit().putString("api_key", v).apply()

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
}