# AmozVz Backend API Reference

The AmozVz backend server exposes endpoints for speech cleanup, Whisper transcription, and real-time dictation polishing.

**Base URL**: `http://localhost:8000` (or `http://10.0.2.2:8000` from Android Emulator)

---

## 1. Clean Text

Cleans raw transcription text by removing hesitations, resolving self-corrections, and applying punctuation and formatting.

**Endpoint**: `POST /v1/clean`  
**Headers**: `Content-Type: application/json`

### Request Body
```json
{
  "raw_text": "um, uh, hello team, let's meet at 2 wait make that 3:30 pm tomorrow, period",
  "mode": "flow_natural",
  "strip_hesitations": true,
  "resolve_corrections": true,
  "custom_dictionary": {
    "k8s": "Kubernetes"
  },
  "context": "chat"
}
```

### Response (`200 OK`)
```json
{
  "cleaned_text": "Hello team, let's meet at 3:30 PM tomorrow.",
  "raw_text": "um, uh, hello team, let's meet at 2 wait make that 3:30 pm tomorrow, period",
  "metrics": {
    "hesitations_removed_count": 2,
    "corrections_resolved_count": 1,
    "processing_time_ms": 3.4,
    "engine_used": "rule_based"
  },
  "mode": "flow_natural"
}
```

---

## 2. Dictate Audio

Accepts an audio recording file (WAV, MP3, M4A, OGG), transcribes it via Whisper, and runs the AmozVz intelligence polisher.

**Endpoint**: `POST /v1/dictate`  
**Headers**: `Content-Type: multipart/form-data`

### Form Parameters
- `file`: Binary audio file (WAV 16kHz mono recommended).
- `mode`: Dictation mode (`flow_natural`, `professional`, `bullet_points`, `raw_verbatim`).
- `strip_hesitations`: `true` / `false`.
- `resolve_corrections`: `true` / `false`.
- `language`: Target language code (default: `en`).

### Response (`200 OK`)
```json
{
  "cleaned_text": "Good morning everyone. The launch is scheduled for Friday.",
  "raw_text": "good morning everyone, uh the launch is scheduled for Friday",
  "metrics": {
    "hesitations_removed_count": 1,
    "corrections_resolved_count": 0,
    "processing_time_ms": 312.5,
    "engine_used": "whisper_large_v3"
  },
  "mode": "flow_natural"
}
```

---

## 3. Health Check

**Endpoint**: `GET /v1/health`

### Response
```json
{
  "status": "ok",
  "version": "1.0.0",
  "ai_provider_configured": true,
  "supported_modes": [
    "flow_natural",
    "professional",
    "bullet_points",
    "raw_verbatim"
  ]
}
```
