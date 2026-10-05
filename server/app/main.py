"""
AmozVz AI Agent Server - FastAPI Application.
Exposes voice-to-text and speech cleanup endpoints for Android, Web, and Desktop clients.
"""

import os
from typing import List, Optional
from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .models import CleanRequest, CleanResponse, DictationMode, HealthResponse
from .agent import AmozVzAgent
from .audio_processor import AudioProcessor

app = FastAPI(
    title="AmozVz Voice-to-Text AI Agent API",
    description="Natural voice dictation agent that understands hesitations, speech changes, punctuations, and formatting like Wispr Flow.",
    version="1.0.0",
)

# Enable CORS for cross-origin access (Android emulator, LAN devices, web apps)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

agent = AmozVzAgent()


@app.get("/v1/health", response_model=HealthResponse)
def health_check():
    """Returns agent health status and capabilities."""
    has_api_key = bool(os.getenv("AMOZVZ_API_KEY") or os.getenv("GROQ_API_KEY") or os.getenv("OPENAI_API_KEY"))
    return HealthResponse(
        status="ok",
        version="1.0.0",
        ai_provider_configured=has_api_key,
        supported_modes=[mode.value for mode in DictationMode],
    )


@app.get("/v1/modes")
def get_modes():
    """Returns available dictation modes and explanations."""
    return {
        "modes": [
            {
                "id": DictationMode.FLOW_NATURAL.value,
                "name": "Natural Flow",
                "description": "Standard conversational dictation with hesitations removed and smart punctuation.",
            },
            {
                "id": DictationMode.PROFESSIONAL.value,
                "name": "Professional",
                "description": "Concise, formal phrasing ideal for business emails, reports, and Slack.",
            },
            {
                "id": DictationMode.BULLET_POINTS.value,
                "name": "Bullet Points",
                "description": "Transforms spoken ideas, steps, and lists into structured Markdown bullets.",
            },
            {
                "id": DictationMode.RAW_VERBATIM.value,
                "name": "Raw Verbatim",
                "description": "Preserves every exact word including filler sounds, with basic punctuation.",
            },
        ]
    }


@app.post("/v1/clean", response_model=CleanResponse)
def clean_text(request: CleanRequest):
    """
    Cleans raw transcription text by removing hesitations, resolving self-corrections,
    and applying punctuation and mode-specific formatting.
    """
    try:
        return agent.clean(
            raw_text=request.raw_text,
            mode=request.mode,
            strip_hesitations=request.strip_hesitations,
            resolve_corrections=request.resolve_corrections,
            custom_dict=request.custom_dictionary,
            context=request.context,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/v1/dictate", response_model=CleanResponse)
async def dictate_audio(
    file: UploadFile = File(..., description="Audio file (WAV, MP3, M4A, OGG)"),
    mode: DictationMode = Form(default=DictationMode.FLOW_NATURAL),
    strip_hesitations: bool = Form(default=True),
    resolve_corrections: bool = Form(default=True),
    language: str = Form(default="en"),
    context: Optional[str] = Form(default=None),
):
    """
    Receives an audio recording from the Android app or desktop client,
    transcribes it using Whisper STT, and passes it through the AmozVz Agent
    to return polished, publication-ready text.
    """
    try:
        content = await file.read()
        if not content:
            raise HTTPException(status_code=400, detail="Empty audio file submitted.")

        raw_transcript = AudioProcessor.transcribe(
            audio_bytes=content,
            filename=file.filename or "recording.wav",
            language=language,
        )

        return agent.clean(
            raw_text=raw_transcript,
            mode=mode,
            strip_hesitations=strip_hesitations,
            resolve_corrections=resolve_corrections,
            context=context,
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Audio processing failed: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("server.app.main:app", host="0.0.0.0", port=8000, reload=True)
