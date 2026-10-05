package com.amozvz.app.data.models

enum class AIProvider(val id: String, val displayName: String) {
    ON_DEVICE("on_device", "On-Device Engine (Zero Cloud / Offline)"),
    AMOZVZ_SERVER("amozvz_server", "AmozVz AI Server"),
    GROQ_WHISPER("groq_whisper", "Groq Whisper + Llama 3"),
    OPENAI("openai", "OpenAI Whisper + GPT-4o-mini")
}

data class EngineConfig(
    val provider: AIProvider = AIProvider.ON_DEVICE,
    val serverUrl: String = "http://10.0.2.2:8000",
    val apiKey: String = "",
    val stripHesitations: Boolean = true,
    val resolveCorrections: Boolean = true,
    val mode: DictationMode = DictationMode.FLOW_NATURAL,
    val customDictionary: Map<String, String> = emptyMap()
)
