"""
Pydantic data models for AmozVz Voice-to-Text AI Agent.
"""

from enum import Enum
from typing import Dict, List, Optional
from pydantic import BaseModel, Field


class DictationMode(str, Enum):
    AUTO = "auto"
    EMAIL = "email"
    CHAT = "chat"
    CODE = "code"
    LISTS = "lists"


class CleanRequest(BaseModel):
    raw_text: str = Field(..., description="Raw transcribed text containing speech hesitations, false starts, etc.")
    mode: DictationMode = Field(default=DictationMode.AUTO, description="Dictation polishing mode")
    strip_hesitations: bool = Field(default=True, description="Whether to filter filler words (um, uh, like)")
    resolve_corrections: bool = Field(default=True, description="Whether to resolve self-corrections/changes")
    custom_dictionary: Optional[Dict[str, str]] = Field(default=None, description="Custom word/phrase replacements")
    context: Optional[str] = Field(default=None, description="Optional target app context (e.g., 'email', 'chat', 'code')")


class DictationMetrics(BaseModel):
    hesitations_removed_count: int = Field(default=0, description="Number of filler words filtered out")
    corrections_resolved_count: int = Field(default=0, description="Number of self-corrections resolved")
    processing_time_ms: float = Field(default=0.0, description="Latency in milliseconds")
    engine_used: str = Field(default="rule_based", description="Engine used (rule_based, llm, groq, openai)")


class CleanResponse(BaseModel):
    cleaned_text: str = Field(..., description="Polished, punctuated, and corrected text ready to type/paste")
    raw_text: str = Field(..., description="Original raw transcript")
    metrics: DictationMetrics = Field(..., description="Processing metrics")
    mode: DictationMode = Field(..., description="Applied dictation mode")


class HealthResponse(BaseModel):
    status: str
    version: str
    ai_provider_configured: bool
    supported_modes: List[str]
