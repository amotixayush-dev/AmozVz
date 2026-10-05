package com.amozvz.app.data.models

data class TranscriptionResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rawText: String,
    val cleanedText: String,
    val mode: DictationMode,
    val hesitationsRemovedCount: Int = 0,
    val correctionsResolvedCount: Int = 0,
    val processingTimeMs: Long = 0,
    val engineUsed: String = "on_device",
    val timestamp: Long = System.currentTimeMillis()
)
