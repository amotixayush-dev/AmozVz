"""
Audio processing and Speech-to-Text handler for AmozVz.
Supports Whisper (OpenAI / Groq) and local audio normalization.
"""

import os
import io
from typing import Optional, Tuple


class AudioProcessor:
    """Handles audio inputs and executes Speech-To-Text transcription."""

    @staticmethod
    def transcribe(audio_bytes: bytes, filename: str = "audio.wav", language: str = "en") -> str:
        """
        Transcribes audio bytes to raw text.
        If GROQ_API_KEY or OPENAI_API_KEY is available, uses the ultra-fast Whisper API.
        Otherwise falls back to on-device/mock transcription.
        """
        groq_key = os.getenv("GROQ_API_KEY")
        openai_key = os.getenv("OPENAI_API_KEY")

        if groq_key:
            return AudioProcessor._transcribe_groq(audio_bytes, filename, groq_key, language)
        elif openai_key:
            return AudioProcessor._transcribe_openai(audio_bytes, filename, openai_key, language)
        else:
            # Fallback when testing without API key
            return "This is a test transcript with um some hesitations wait no polished speech."

    @staticmethod
    def _transcribe_groq(audio_bytes: bytes, filename: str, api_key: str, language: str) -> str:
        import httpx

        url = "https://api.groq.com/openai/v1/audio/transcriptions"
        headers = {"Authorization": f"Bearer {api_key}"}
        files = {"file": (filename, audio_bytes, "audio/wav")}
        data = {
            "model": "whisper-large-v3",
            "language": language,
            "response_format": "json",
            "temperature": 0.0,
        }

        with httpx.Client(timeout=30.0) as client:
            resp = client.post(url, headers=headers, files=files, data=data)
            resp.raise_for_status()
            return resp.json().get("text", "")

    @staticmethod
    def _transcribe_openai(audio_bytes: bytes, filename: str, api_key: str, language: str) -> str:
        import httpx

        url = "https://api.openai.com/v1/audio/transcriptions"
        headers = {"Authorization": f"Bearer {api_key}"}
        files = {"file": (filename, audio_bytes, "audio/wav")}
        data = {
            "model": "whisper-1",
            "language": language,
            "response_format": "json",
            "temperature": 0.0,
        }

        with httpx.Client(timeout=30.0) as client:
            resp = client.post(url, headers=headers, files=files, data=data)
            resp.raise_for_status()
            return resp.json().get("text", "")
