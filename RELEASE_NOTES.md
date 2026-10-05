# 🎙️ AmozVz v1.0.2 — Advanced Voice-to-Text Post-Processor

**AmozVz v1.0.2** brings a full implementation of the **Advanced Voice-to-Text Post-Processor**, strictly enforcing natural speech restructuring, self-correction resolution, and context-adaptive formatting, alongside a brand new **Dual-Input Interface (Live Voice + Paste Speech)**.

---

## ⚡ The 6 Strict Post-Processing Rules

AmozVz takes raw, unedited, spoken-word speech transcription and converts it into clean, beautifully formatted written text while strictly preserving your original intent and meaning:

1. 🚫 **Remove All Filler Words**:
   - Strips `"um"`, `"uh"`, `"like"`, `"you know"`, `"actually"`, `"I mean"`, `"basically"`, `"literally"`, and repeated vocal stutters.
2. 🔄 **Resolve Self-Corrections & False Starts Seamlessly**:
   - Naturally resolves mid-sentence corrections:
     - *"Let's meet at 4, wait, no, 5 PM"* ➔ **"Let's meet at 5:00 PM."**
     - *"Send the invoice to Sarah, scratch that, send it to Michael"* ➔ **"Send it to Michael."**
3. ✍️ **Natural Grammar, Capitalization, and Punctuation**:
   - Applies proper sentence casing, comma spacing, period termination, and spoken punctuation symbols.
4. 📋 **Auto-Structure Output**:
   - Automatically detects step-by-step instructions or lists and formats them as clean bullet points or numbered lists (`1. Step one`, `2. Step two`).
5. 🎯 **Adapt Formatting to Context**:
   - **`💻 Code`**: Wraps snippets into markdown code blocks.
   - **`✉️ Email`**: Formats salutations and professional sign-offs.
   - **`💬 Chat`**: Keeps messages concise and conversational.
   - **`📝 Lists`**: Structures multi-item thoughts into readable bullet points.
   - **`✨ Auto`**: Dynamically chooses the optimal structure based on speech content.
6. 🔒 **Zero Preamble or Commentary**:
   - Returns **ONLY** the finalized text. No introductory remarks like *"Here is your cleaned text:"* or robotic filler.

---

## 📱 What's New in the Android App

- 🎛️ **Dual-Input Mode**:
  - **🎙️ Voice Dictation**: One-tap recording with real-time waveform visualization and on-device speech-to-text.
  - **📝 Paste Speech**: Paste raw transcripts from Whisper, voice memos, or audio files and post-process them with one tap.
- 🛡️ **Zero Sensitive Data Access (Clean Install Guarantee)**:
  - No `BIND_ACCESSIBILITY_SERVICE` (no screen recording / keylogging).
  - No `SYSTEM_ALERT_WINDOW` (no draw-over-apps).
  - No `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
  - Installs cleanly across all Android 8.0+ devices without Play Protect blocks or browser security warnings.
- 🔏 **V2 Signed Release**:
  - Validated and signed using standard Android APK Signature Scheme v2.
- 📋 **One-Tap Actions**:
  - Instant auto-copy to clipboard.
  - Native Android Share sheet to send text directly to WhatsApp, Slack, Gmail, Telegram, Notion, etc.

---

## 📥 Direct Download & Installation

| Asset | Size | Signature | Minimum Android |
|---|---|---|---|
| **[`AmozVz-v1.0.2.apk`](https://github.com/amotixayush-dev/AmozVz/releases/download/v1.0.2/AmozVz-v1.0.2.apk)** | **11 MB** | **v2 Signed** | **Android 8.0+ (API 26)** |

1. Download **[`AmozVz-v1.0.2.apk`](https://github.com/amotixayush-dev/AmozVz/releases/download/v1.0.2/AmozVz-v1.0.2.apk)** directly to your device.
2. Open your Downloads and tap the APK to install.
3. Start speaking or paste messy speech notes to experience instant, human-grade post-processing!
