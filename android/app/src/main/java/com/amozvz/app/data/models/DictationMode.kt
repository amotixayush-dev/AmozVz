package com.amozvz.app.data.models

enum class DictationMode(val id: String, val displayName: String, val description: String) {
    FLOW_NATURAL(
        id = "flow_natural",
        displayName = "Natural Flow",
        description = "Conversational dictation with hesitations removed and smart punctuation."
    ),
    PROFESSIONAL(
        id = "professional",
        displayName = "Professional",
        description = "Concise and formal phrasing ideal for business emails, reports, and Slack."
    ),
    BULLET_POINTS(
        id = "bullet_points",
        displayName = "Bullet Points",
        description = "Transforms spoken thoughts, steps, and lists into structured bullet points."
    ),
    RAW_VERBATIM(
        id = "raw_verbatim",
        displayName = "Raw Verbatim",
        description = "Preserves every exact word including filler sounds, with basic punctuation."
    );

    companion object {
        fun fromId(id: String): DictationMode = entries.firstOrNull { it.id == id } ?: FLOW_NATURAL
    }
}
