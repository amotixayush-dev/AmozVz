# AmozVz Permissions & System Integration Guide

Like **Wispr Flow**, AmozVz integrates deeply with the Android operating system to deliver seamless voice-to-text dictation that works on top of any application.

This document details why each permission is required, how it is used, and how privacy is maintained.

---

## 1. Permission Matrix

| Permission | Android API | Why AmozVz Needs It | How To Grant |
| :--- | :--- | :--- | :--- |
| **Microphone (`RECORD_AUDIO`)** | All APIs | To capture speech audio for real-time transcription. | In-app runtime dialog |
| **App Overlay (`SYSTEM_ALERT_WINDOW`)** | All APIs | To display the floating mic widget over other apps (WhatsApp, Slack, Gmail, etc.). | System Settings > Display over other apps |
| **Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`)** | All APIs | To detect focused input fields and automatically insert polished text into active apps. | System Settings > Accessibility > AmozVz |
| **Post Notifications (`POST_NOTIFICATIONS`)** | API 33+ (Android 13+) | To show ongoing dictation status and quick "Stop" / "Cancel" controls. | In-app runtime dialog |
| **Foreground Service Microphone (`FOREGROUND_SERVICE_MICROPHONE`)** | API 34+ (Android 14+) | Required by Android 14 to record microphone audio while the app is in the background. | Declared in Manifest |
| **Ignore Battery Optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)** | All APIs | Prevents Android Doze mode from killing the floating overlay when idling. | System prompt dialog |
| **Haptic Feedback (`VIBRATE`)** | All APIs | Provides tactile feedback when tapping the floating button to start/stop dictation. | Declared in Manifest |

---

## 2. In-Depth Permission Breakdown

### 🎙️ 1. Microphone Access (`android.permission.RECORD_AUDIO`)
- **Purpose**: Voice capture. Audio is recorded at 16kHz 16-bit PCM for optimal speech-to-text accuracy.
- **Privacy Assurance**: The microphone is **only** active when the user explicitly taps the floating mic button (indicated by a bright red recording ring and a persistent notification). Once recording is stopped, the audio stream is closed immediately.

### 🪟 2. Display Over Other Apps (`android.permission.SYSTEM_ALERT_WINDOW`)
- **Purpose**: Floating overlay widget. Allows the mic pill to stay visible while using WhatsApp, Slack, Gmail, Google Docs, Twitter/X, Chrome, or any note-taking app.
- **Interaction**:
  - The widget is completely draggable and snaps to the nearest screen edge.
  - Can be dismissed or toggled off at any time.

### ⌨️ 3. Accessibility Service (`android.permission.BIND_ACCESSIBILITY_SERVICE`)
- **Purpose**: Universal text insertion.
- **Mechanism**:
  ```kotlin
  val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
  if (node != null && node.isEditable) {
      val args = Bundle().apply {
          putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, cleanedText)
      }
      node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
  }
  ```
- **Fallback**: If the target application does not expose an accessible editable node, AmozVz automatically copies the text to the system clipboard and displays a visual indicator so you can paste it manually.

### 🔔 4. Foreground Service & Notification (`POST_NOTIFICATIONS`)
- **Purpose**: Transparency and control.
- Android requires any background recording process to be clearly visible to the user.
- AmozVz displays an ongoing notification:
  - Title: **AmozVz Listening**
  - Content: *"Tap to stop or speak naturally anywhere..."*
  - Actions: **Stop** | **Cancel**

---

## 3. Step-by-Step Onboarding Flow

When launching AmozVz for the first time:
1. Open AmozVz and tap the **Permissions & Setup** banner.
2. Grant **Microphone Access**.
3. Toggle **Display over other apps** in Android Settings.
4. Enable the **AmozVz Dictation Injection** Accessibility Service.
5. Tap **Activate Floating Mic Overlay**.
6. Switch to any app and tap the floating button to dictate naturally!
