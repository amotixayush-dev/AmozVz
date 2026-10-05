# 🎙️ AmozVz v1.0.0 — Open-Source Voice-to-Text AI Dictation Agent

*Inspired by [Wispr Flow](https://play.google.com/store/apps/details?id=com.wispr.flowapp)*

We are excited to announce the initial open-source release of **AmozVz (v1.0.0)**!

AmozVz is an intelligent voice-to-text dictation agent for Android that transforms natural, messy speech into polished, punctuated, and ready-to-use text across any app on your phone.

---

## 📦 What's New in v1.0.0

- 🪟 **Floating Mic Overlay Widget (`SYSTEM_ALERT_WINDOW`)**:
  - Draggable, edge-snapping floating button that stays over any app (WhatsApp, Slack, Gmail, Google Docs, Notes, Telegram).
  - Tap to record, speak naturally, tap again to polish and auto-inject.
- ⚡ **Universal In-App Text Injection (`AccessibilityService`)**:
  - Automatically detects the focused editable input field in your active app and inserts the cleaned text via `ACTION_SET_TEXT` / `ACTION_PASTE`.
- 🧠 **Speech Hesitation & Filler Removal**:
  - Automatically filters vocal fillers like *"um"*, *"uh"*, *"er"*, *"ah"*, *"you know"*, *"kind of"*, *"basically"*, and repeated stutters.
- 🔄 **Self-Correction & Speech Changes**:
  - Understands when you change your mind mid-sentence (*"scratch that"*, *"no wait"*, *"wait actually"*, *"make that [X]"*) and resolves the final intended thought seamlessly.
- ✍️ **Natural Punctuation & Formatting**:
  - Inserts punctuation automatically or through spoken commands (*"period"*, *"comma"*, *"question mark"*, *"new line"*).
- 🔔 **Android 14+ Foreground Service & Notification Control**:
  - Compliant with `FOREGROUND_SERVICE_MICROPHONE` and `POST_NOTIFICATIONS` with persistent status and quick Stop/Cancel actions.
- ⌨️ **Voice Input Method (IME)**:
  - Built-in keyboard option for users who prefer dictating directly from their keyboard tray.
- 🔒 **Dual Mode Engine (Offline + Cloud)**:
  - **On-Device Local Filter**: Zero-latency (< 5ms) deterministic NLP cleaner that works 100% offline.
  - **Cloud AI Provider**: Optional integration with Groq Whisper, OpenAI Whisper, Google Gemini, or self-hosted AmozVz FastAPI server.

---

## 📥 Installation

1. Download **`AmozVz-v1.0.0.apk`** from the Assets below.
2. Open the downloaded APK on your Android device (Android 8.0+ / API 26+).
3. If prompted, allow "Install from Unknown Sources".
4. Open **AmozVz** and follow the step-by-step **Permissions & Setup** screen:
   - Enable **Microphone** access.
   - Allow **Display over other apps** (for the floating widget).
   - Turn on the **AmozVz Dictation Injection** Accessibility Service.
   - Allow **Notifications**.
5. Tap **Activate Floating Mic Overlay** and dictate anywhere!

---

## 🛠️ Open-Source Code & Documentation

- GitHub Repository: [https://github.com/amotixayush-dev/AmozVz](https://github.com/amotixayush-dev/AmozVz)
- Architecture Guide: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- Permissions Guide: [docs/PERMISSIONS.md](docs/PERMISSIONS.md)
- Backend REST API: [docs/API.md](docs/API.md)
- License: [MIT License](LICENSE)
