# AmozVz Permissions & Privacy Architecture

**AmozVz** is built with a **Privacy-First** design. It does not request any sensitive permissions, screen capture, or accessibility keystroke monitoring.

---

## 1. Permission Matrix

| Permission | Android Identifier | Why AmozVz Needs It | Is It Sensitive? |
| :--- | :--- | :--- | :--- |
| **Microphone** | `android.permission.RECORD_AUDIO` | Captures your voice when you tap the Speak button. | Standard Runtime |
| **Notifications** | `android.permission.POST_NOTIFICATIONS` | Shows recording status and Stop action while dictating. | Standard Runtime |
| **Foreground Service** | `android.permission.FOREGROUND_SERVICE` | Keeps audio capture active during dictation. | Normal |
| **Foreground Mic** | `android.permission.FOREGROUND_SERVICE_MICROPHONE` | Android 14 requirement for microphone audio recording. | Normal |
| **Internet** | `android.permission.INTERNET` | Optional cloud AI inference (Groq / OpenAI / Gemini). | Normal |
| **Vibrate** | `android.permission.VIBRATE` | Tactile feedback when starting or stopping dictation. | Normal |

---

## 2. Zero Sensitive Data Access Guarantee

Unlike apps that require Accessibility Services or Screen Overlays, AmozVz guarantees:
- ❌ **NO Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`)**: AmozVz does **not** inspect your screen, passwords, credit card numbers, or app contents.
- ❌ **NO Screen Overlay (`SYSTEM_ALERT_WINDOW`)**: AmozVz does **not** draw over other apps or hijack screen taps.
- ❌ **NO Battery Optimization Bypass (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**: AmozVz respects Android system power policies.
- ❌ **NO Keystroke Tracking**: AmozVz only receives audio when you explicitly tap Speak.

---

## 3. How Text Is Inserted Without Accessibility

1. **Instant Auto-Copy**: When dictation finishes, the cleaned, punctuated text is instantly copied to your clipboard. A toast notification confirms it's ready. You simply tap **Paste** in WhatsApp, Slack, Gmail, or Notes.
2. **1-Tap Quick Share**: Tap the **Share** button on the dictation result to immediately send your text to any app via the native Android Share sheet.
3. **Voice Keyboard (IME)**: You can optionally enable AmozVz as an input method. Standard Android keyboards type directly into active inputs without requiring any accessibility or sensitive screen permissions.
