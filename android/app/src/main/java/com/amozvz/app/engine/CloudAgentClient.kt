package com.amozvz.app.engine

import com.amozvz.app.data.models.CleanResponseDto
import com.amozvz.app.data.models.EngineConfig
import com.amozvz.app.data.models.TranscriptionResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class CloudAgentClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun cleanText(rawText: String, config: EngineConfig): TranscriptionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val payload = mapOf(
            "raw_text" to rawText,
            "mode" to config.mode.id,
            "strip_hesitations" to config.stripHesitations,
            "resolve_corrections" to config.resolveCorrections,
            "custom_dictionary" to config.customDictionary
        )

        val requestBody = gson.toJson(payload).toRequestBody(jsonMediaType)
        val endpoint = "${config.serverUrl.trimEnd('/')}/v1/clean"

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(requestBody)

        if (config.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw RuntimeException("AmozVz Server returned error ${response.code}: $responseBody")
        }

        val cleanDto = gson.fromJson(responseBody, CleanResponseDto::class.java)
        val elapsed = System.currentTimeMillis() - startTime

        TranscriptionResult(
            rawText = rawText,
            cleanedText = cleanDto.cleaned_text,
            mode = config.mode,
            hesitationsRemovedCount = cleanDto.metrics?.hesitations_removed_count ?: 0,
            correctionsResolvedCount = cleanDto.metrics?.corrections_resolved_count ?: 0,
            processingTimeMs = elapsed,
            engineUsed = cleanDto.metrics?.engine_used ?: "cloud"
        )
    }

    suspend fun transcribeAndCleanAudio(
        audioFile: File,
        config: EngineConfig
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val endpoint = "${config.serverUrl.trimEnd('/')}/v1/dictate"

        val fileBody = audioFile.asRequestBody("audio/wav".toMediaType())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, fileBody)
            .addFormDataPart("mode", config.mode.id)
            .addFormDataPart("strip_hesitations", config.stripHesitations.toString())
            .addFormDataPart("resolve_corrections", config.resolveCorrections.toString())
            .build()

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(multipartBody)

        if (config.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw RuntimeException("Dictation upload failed ${response.code}: $responseBody")
        }

        val cleanDto = gson.fromJson(responseBody, CleanResponseDto::class.java)
        val elapsed = System.currentTimeMillis() - startTime

        TranscriptionResult(
            rawText = cleanDto.raw_text ?: "",
            cleanedText = cleanDto.cleaned_text,
            mode = config.mode,
            hesitationsRemovedCount = cleanDto.metrics?.hesitations_removed_count ?: 0,
            correctionsResolvedCount = cleanDto.metrics?.corrections_resolved_count ?: 0,
            processingTimeMs = elapsed,
            engineUsed = cleanDto.metrics?.engine_used ?: "cloud_whisper"
        )
    }
}
