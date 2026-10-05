package com.amozvz.app.data

import android.content.Context
import android.content.SharedPreferences
import com.amozvz.app.data.models.AIProvider
import com.amozvz.app.data.models.DictationMode
import com.amozvz.app.data.models.EngineConfig
import com.amozvz.app.data.models.TranscriptionResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    var isOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    var dictationMode: DictationMode
        get() = DictationMode.fromId(prefs.getString(KEY_DICTATION_MODE, DictationMode.FLOW_NATURAL.id) ?: DictationMode.FLOW_NATURAL.id)
        set(value) = prefs.edit().putString(KEY_DICTATION_MODE, value.id).apply()

    var aiProvider: AIProvider
        get() {
            val id = prefs.getString(KEY_AI_PROVIDER, AIProvider.ON_DEVICE.id) ?: AIProvider.ON_DEVICE.id
            return AIProvider.entries.firstOrNull { it.id == id } ?: AIProvider.ON_DEVICE
        }
        set(value) = prefs.edit().putString(KEY_AI_PROVIDER, value.id).apply()

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var stripHesitations: Boolean
        get() = prefs.getBoolean(KEY_STRIP_HESITATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_STRIP_HESITATIONS, value).apply()

    var resolveCorrections: Boolean
        get() = prefs.getBoolean(KEY_RESOLVE_CORRECTIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_RESOLVE_CORRECTIONS, value).apply()

    var customDictionary: Map<String, String>
        get() {
            val json = prefs.getString(KEY_CUSTOM_DICTIONARY, null) ?: return emptyMap()
            val type = object : TypeToken<Map<String, String>>() {}.type
            return try {
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit().putString(KEY_CUSTOM_DICTIONARY, json).apply()
        }

    fun getEngineConfig(): EngineConfig {
        return EngineConfig(
            provider = aiProvider,
            serverUrl = serverUrl,
            apiKey = apiKey,
            stripHesitations = stripHesitations,
            resolveCorrections = resolveCorrections,
            mode = dictationMode,
            customDictionary = customDictionary
        )
    }

    fun saveHistoryItem(result: TranscriptionResult) {
        val history = getHistory().toMutableList()
        history.add(0, result)
        // Keep last 50 items
        val trimmed = if (history.size > 50) history.subList(0, 50) else history
        val json = gson.toJson(trimmed)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    fun getHistory(): List<TranscriptionResult> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<TranscriptionResult>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val PREFS_NAME = "amozvz_settings"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_DICTATION_MODE = "dictation_mode"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_STRIP_HESITATIONS = "strip_hesitations"
        private const val KEY_RESOLVE_CORRECTIONS = "resolve_corrections"
        private const val KEY_CUSTOM_DICTIONARY = "custom_dictionary"
        private const val KEY_HISTORY = "transcription_history"
    }
}
