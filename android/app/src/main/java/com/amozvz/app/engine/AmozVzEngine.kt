package com.amozvz.app.engine

import android.content.Context
import com.amozvz.app.data.PreferencesManager
import com.amozvz.app.data.models.AIProvider
import com.amozvz.app.data.models.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AmozVzEngine(private val context: Context) {

    private val prefs = PreferencesManager(context)
    private val cloudClient = CloudAgentClient()

    suspend fun processSpeechText(rawText: String): TranscriptionResult = withContext(Dispatchers.Default) {
        val config = prefs.getEngineConfig()

        if (config.provider == AIProvider.ON_DEVICE) {
            val result = HesitationFilter.clean(
                rawText = rawText,
                mode = config.mode,
                stripHesitations = config.stripHesitations,
                resolveCorrections = config.resolveCorrections,
                customDictionary = config.customDictionary
            )
            prefs.saveHistoryItem(result)
            return@withContext result
        }

        try {
            val cloudResult = cloudClient.cleanText(rawText, config)
            prefs.saveHistoryItem(cloudResult)
            cloudResult
        } catch (e: Exception) {
            // Graceful fallback to on-device filter on any network/cloud failure
            val fallback = HesitationFilter.clean(
                rawText = rawText,
                mode = config.mode,
                stripHesitations = config.stripHesitations,
                resolveCorrections = config.resolveCorrections,
                customDictionary = config.customDictionary
            ).copy(engineUsed = "on_device_fallback")

            prefs.saveHistoryItem(fallback)
            fallback
        }
    }

    suspend fun processAudioFile(audioFile: File): TranscriptionResult = withContext(Dispatchers.IO) {
        val config = prefs.getEngineConfig()

        if (config.provider != AIProvider.ON_DEVICE) {
            try {
                val cloudResult = cloudClient.transcribeAndCleanAudio(audioFile, config)
                prefs.saveHistoryItem(cloudResult)
                return@withContext cloudResult
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Default / fallback on device result
        val fallback = HesitationFilter.clean(
            rawText = "Audio recorded successfully.",
            mode = config.mode
        )
        prefs.saveHistoryItem(fallback)
        fallback
    }
}
