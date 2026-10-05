package com.amozvz.app.data.models

enum class DictationMode(val id: String, val displayName: String, val description: String) {
    AUTO(
        id = "auto",
        displayName = "✨ Auto",
        description = "Automatically detects structure, lists, paragraphs, and tone."
    ),
    EMAIL(
        id = "email",
        displayName = "✉️ Email",
        description = "Professional format with proper salutation, body paragraphs, and sign-off."
    ),
    CHAT(
        id = "chat",
        displayName = "💬 Casual Chat",
        description = "Concise and natural phrasing for WhatsApp, Slack, and messaging."
    ),
    CODE(
        id = "code",
        displayName = "💻 Code",
        description = "Formats technical dictation and programming syntax into clean code blocks."
    ),
    LISTS(
        id = "lists",
        displayName = "📝 Lists",
        description = "Auto-structures steps, items, and tasks into clean bullet points or numbered lists."
    );

    companion object {
        fun fromId(id: String): DictationMode = entries.firstOrNull { it.id == id } ?: AUTO
    }
}
