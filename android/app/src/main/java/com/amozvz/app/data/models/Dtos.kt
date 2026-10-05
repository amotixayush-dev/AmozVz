package com.amozvz.app.data.models

data class CleanResponseDto(
    val cleaned_text: String = "",
    val raw_text: String? = null,
    val metrics: DictationMetricsDto? = null,
    val mode: String? = null
)

data class DictationMetricsDto(
    val hesitations_removed_count: Int = 0,
    val corrections_resolved_count: Int = 0,
    val processing_time_ms: Double = 0.0,
    val engine_used: String = ""
)
