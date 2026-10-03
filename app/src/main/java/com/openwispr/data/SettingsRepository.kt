package com.openwispr.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.openwispr.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore(name = "openwispr_prefs")

class SettingsRepository(private val context: Context) {
    companion object {
        private val KEY_API = stringPreferencesKey("api_key")
        private val KEY_BASE = stringPreferencesKey("base_url")
        private val KEY_WHISPER = stringPreferencesKey("whisper_model")
        private val KEY_LLAMA = stringPreferencesKey("llama_model")
        private val KEY_BUBBLE = intPreferencesKey("bubble_dp")
        private val KEY_MAX_SEC = intPreferencesKey("max_sec")
        private val KEY_BUBBLE_X = intPreferencesKey("bubble_x")
        private val KEY_BUBBLE_Y = intPreferencesKey("bubble_y")
    }

    val apiKeyFlow: Flow<String> = context.ds.data.map { it[KEY_API] ?: "" }
    val baseUrlFlow: Flow<String> = context.ds.data.map { it[KEY_BASE] ?: Constants.DEFAULT_BASE_URL }
    val whisperFlow: Flow<String> = context.ds.data.map { it[KEY_WHISPER] ?: Constants.DEFAULT_WHISPER }
    val llamaFlow: Flow<String> = context.ds.data.map { it[KEY_LLAMA] ?: Constants.DEFAULT_LLAMA }
    val bubbleDpFlow: Flow<Int> = context.ds.data.map { it[KEY_BUBBLE] ?: Constants.DEFAULT_BUBBLE_DP }
    val maxSecFlow: Flow<Int> = context.ds.data.map { it[KEY_MAX_SEC] ?: Constants.DEFAULT_MAX_SEC }
    val bubbleXFlow: Flow<Int> = context.ds.data.map { it[KEY_BUBBLE_X] ?: Int.MIN_VALUE }
    val bubbleYFlow: Flow<Int> = context.ds.data.map { it[KEY_BUBBLE_Y] ?: Int.MIN_VALUE }

    suspend fun setApiKey(v: String) { context.ds.edit { it[KEY_API] = v } }
    suspend fun setBaseUrl(v: String) { context.ds.edit { it[KEY_BASE] = v } }
    suspend fun setWhisper(v: String) { context.ds.edit { it[KEY_WHISPER] = v } }
    suspend fun setLlama(v: String) { context.ds.edit { it[KEY_LLAMA] = v } }
    suspend fun setBubbleDp(v: Int) { context.ds.edit { it[KEY_BUBBLE] = v.coerceIn(Constants.MIN_BUBBLE_DP, Constants.MAX_BUBBLE_DP) } }
    suspend fun setMaxSec(v: Int) { context.ds.edit { it[KEY_MAX_SEC] = v.coerceIn(Constants.MIN_REC_SEC, Constants.MAX_REC_SEC) } }
    suspend fun setBubblePos(x: Int, y: Int) { context.ds.edit { it[KEY_BUBBLE_X] = x; it[KEY_BUBBLE_Y] = y } }
}