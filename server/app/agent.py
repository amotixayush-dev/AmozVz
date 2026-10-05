"""
AmozVz AI Agent: Natural Speech-to-Polished-Text Engine.
Understands hesitations, self-corrections, speech changes, punctuations, and formatting like Wispr Flow.
"""

import os
import re
import time
from typing import Dict, List, Optional, Tuple
from .models import DictationMode, DictationMetrics, CleanResponse

# Common filler words and vocalized hesitations
FILLER_WORDS = [
    r"\buh+\b",
    r"\bum+\b",
    r"\ber+\b",
    r"\bah+\b",
    r"\bhmm+\b",
    r"\bmhm+\b",
    r"\byou\s+know\b",
    r"\bkind\s+of\b",
    r"\bsort\s+of\b",
    r"\bactually\b",
    r"\bI\s+mean\b",
    r"\bbasically\b",
    r"\bliterally\b",
    r"\bso\s+yeah\b",
]

# Spoken punctuation commands to symbols
PUNCTUATION_COMMANDS: List[Tuple[re.Pattern, str]] = [
    (re.compile(r"\b(new\s+paragraph)\b", re.IGNORECASE), "\n\n"),
    (re.compile(r"\b(new\s+line)\b", re.IGNORECASE), "\n"),
    (re.compile(r"\b(period|full\s+stop)\b", re.IGNORECASE), "."),
    (re.compile(r"\b(comma)\b", re.IGNORECASE), ","),
    (re.compile(r"\b(question\s+mark)\b", re.IGNORECASE), "?"),
    (re.compile(r"\b(exclamation\s+mark|exclamation\s+point)\b", re.IGNORECASE), "!"),
    (re.compile(r"\b(colon)\b", re.IGNORECASE), ":"),
    (re.compile(r"\b(semi-colon|semicolon)\b", re.IGNORECASE), ";"),
    (re.compile(r"\b(hyphen|dash)\b", re.IGNORECASE), " - "),
    (re.compile(r"\b(open\s+quote|start\s+quote)\b", re.IGNORECASE), ' "'),
    (re.compile(r"\b(close\s+quote|end\s+quote)\b", re.IGNORECASE), '" '),
    (re.compile(r"\b(bullet\s+point)\b", re.IGNORECASE), "\n- "),
]

# Self-correction trigger patterns
# e.g.: "Let's meet at 4, wait, no, 5 PM" -> "Let's meet at 5:00 PM"
CORRECTION_PATTERNS = [
    # "wait no, [replacement]" / "no wait, [replacement]"
    re.compile(r"(?:^|(?<=[.!?\s]))(?P<before>.+?)\s*,?\s*(?:wait,?\s*no|no,?\s*wait|wait,?\s*actually|actually,?\s*wait)\s*,?\s*(?P<after>.+)$", re.IGNORECASE),
    # "scratch that, [replacement]" or "never mind that, [replacement]"
    re.compile(r"(?:^|(?<=[.!?\s]))(?P<before>.+?)\s*,?\s*(?:scratch\s+that|never\s+mind(?:\s+that)?)\s*,?\s*(?P<after>.+)$", re.IGNORECASE),
    # "wait make that [replacement]" or "make that [replacement]"
    re.compile(r"(?:^|(?<=[.!?\s]))(?P<before>.+?)\s*,?\s*(?:wait\s+make\s+that|make\s+that)\s*,?\s*(?P<after>.+)$", re.IGNORECASE),
    # "or rather, [replacement]" or "correction, [replacement]"
    re.compile(r"(?:^|(?<=[.!?\s]))(?P<before>.+?)\s*,?\s*(?:or\s+rather|correction)\s*,?\s*(?P<after>.+)$", re.IGNORECASE),
]


class RuleBasedHesitationCleaner:
    """
    High-performance, zero-latency rule-based natural speech polisher.
    Runs entirely on-device or on local server without requiring external API keys.
    """

    @classmethod
    def clean(
        cls,
        text: str,
        strip_hesitations: bool = True,
        resolve_corrections: bool = True,
        custom_dict: Optional[Dict[str, str]] = None,
        mode: DictationMode = DictationMode.AUTO,
    ) -> Tuple[str, DictationMetrics]:
        start_time = time.perf_counter()
        raw = text.strip()
        if not raw:
            return "", DictationMetrics(processing_time_ms=0.0, engine_used="rule_based")

        cleaned = raw
        hesitations_count = 0
        corrections_count = 0

        # 1. Resolve self-corrections / speech changes if enabled
        if resolve_corrections:
            cleaned, corrections_count = cls._resolve_corrections(cleaned)

        # 2. Convert explicit spoken punctuation commands
        cleaned = cls._apply_spoken_punctuation(cleaned)

        # 3. Strip hesitations & filler words if enabled
        if strip_hesitations:
            cleaned, hesitations_count = cls._strip_hesitations(cleaned)

        # 4. Remove stutter repetition (e.g., "I I think", "the the meeting")
        cleaned = cls._remove_stutters(cleaned)

        # 5. Apply custom dictionary substitutions
        if custom_dict:
            for source, replacement in custom_dict.items():
                pattern = re.compile(r"\b" + re.escape(source) + r"\b", re.IGNORECASE)
                cleaned = pattern.sub(replacement, cleaned)

        # 6. Normalize time expressions (e.g. 5 PM -> 5:00 PM)
        cleaned = cls._normalize_time_expressions(cleaned)

        # 7. Apply formatting based on mode
        cleaned = cls._format_by_mode(cleaned, mode)

        # 8. Normalize punctuation spacing & sentence capitalization
        cleaned = cls._normalize_grammar_and_spacing(cleaned)

        elapsed_ms = (time.perf_counter() - start_time) * 1000.0
        metrics = DictationMetrics(
            hesitations_removed_count=hesitations_count,
            corrections_resolved_count=corrections_count,
            processing_time_ms=round(elapsed_ms, 2),
            engine_used="rule_based",
        )
        return cleaned, metrics

    @classmethod
    def _resolve_corrections(cls, text: str) -> Tuple[str, int]:
        result = text
        count = 0
        
        for pattern in CORRECTION_PATTERNS:
            match = pattern.search(result)
            if match:
                before = match.group("before").strip()
                after = match.group("after").strip()
                
                after_words = after.split()
                before_words = before.split()
                
                if after_words and before_words:
                    last_before = before_words[-1].rstrip(".,?!")
                    first_after = after_words[0].rstrip(".,?!")
                    
                    if re.match(r"^\d+(:?\d+)?$", last_before) and re.match(r"^\d+(:?\d+)?$", first_after):
                        replace_count = 1
                    elif len(after_words) <= 3 and len(before_words) >= len(after_words):
                        replace_count = len(after_words)
                    else:
                        replace_count = min(len(after_words), len(before_words))
                        
                    kept_before = " ".join(before_words[:-replace_count]).rstrip(",")
                    result = f"{kept_before} {after}" if kept_before else after
                else:
                    result = after
                    
                count += 1
                break

        return result, count

    @classmethod
    def _apply_spoken_punctuation(cls, text: str) -> str:
        res = text
        for pattern, replacement in PUNCTUATION_COMMANDS:
            res = pattern.sub(replacement, res)
        return res

    @classmethod
    def _strip_hesitations(cls, text: str) -> Tuple[str, int]:
        res = text
        count = 0

        # Clean filler "like" before adjacent fillers consume boundary commas
        like_patterns = [
            re.compile(r"(?:^|[\s,])like\s*,", re.IGNORECASE),
            re.compile(r",\s*like(?:\s*,|\s+)", re.IGNORECASE),
            re.compile(r"(?:^|[.!?\n])\s*like\s+(?=(?:we|I|you|they|it|he|she|this|that|what|how|why|when|where|there)\b)", re.IGNORECASE),
        ]
        for lp in like_patterns:
            matches = lp.findall(res)
            if matches:
                count += len(matches)
                res = lp.sub(" ", res)

        # Replace vocalized fillers
        for filler in FILLER_WORDS:
            filler_pattern = re.compile(r"(?:,\s*)?" + filler + r"(?:\s*,)?", re.IGNORECASE)
            matches = filler_pattern.findall(res)
            if matches:
                count += len(matches)
                res = filler_pattern.sub(" ", res)

        # Clean orphaned double commas, leading commas, or weird spacing
        res = re.sub(r",\s*,+", ",", res)
        res = re.sub(r"^[,\s]+", "", res)
        res = re.sub(r"\s+,\s*", ", ", res)

        return res, count

    @classmethod
    def _remove_stutters(cls, text: str) -> str:
        repetition_pattern = re.compile(r"\b(\w+)\s+\1\b", re.IGNORECASE)
        res = repetition_pattern.sub(r"\1", text)
        res = repetition_pattern.sub(r"\1", res)
        hyphen_stutter = re.compile(r"\b[a-zA-Z]{1,2}-\b", re.IGNORECASE)
        res = hyphen_stutter.sub("", res)
        return res

    @classmethod
    def _normalize_time_expressions(cls, text: str) -> str:
        time_regex = re.compile(r"\b(\d{1,2})\s*(AM|PM|am|pm)\b")
        return time_regex.sub(lambda m: f"{m.group(1)}:00 {m.group(2).upper()}", text)

    @classmethod
    def _format_by_mode(cls, text: str, mode: DictationMode) -> str:
        # Code mode or code keywords
        is_code = mode == DictationMode.CODE or (
            mode == DictationMode.AUTO and bool(re.search(r"\b(def |class |import |function|const |var |SELECT |FROM )\b", text, re.IGNORECASE))
        )
        if is_code:
            return f"```\n{text}\n```" if not text.startswith("```") else text

        # List mode (Rule 4)
        is_list = mode == DictationMode.LISTS or (
            mode == DictationMode.AUTO and bool(re.search(r"\b(step 1|step 2|first|second|third|item 1|item 2)\b", text, re.IGNORECASE))
        )
        if is_list:
            numbered_match = bool(re.search(r"\b(first|second|third|step 1|step 2|1\.|2\.)\b", text, re.IGNORECASE))
            lines = []
            raw_segments = re.split(r"(?<=[.!?])\s+|\s+(?:first|second|third|next|finally|also|then)\s+", text, flags=re.IGNORECASE)
            idx = 1
            for seg in raw_segments:
                seg = seg.strip().lstrip("-*•0123456789.)").strip()
                seg = re.sub(r"^(?:first|second|third|fourth|fifth|next|finally|also|then|step\s*\d+)\s*,?\s*", "", seg, flags=re.IGNORECASE)
                if seg:
                    seg = seg[0].upper() + seg[1:] if len(seg) > 1 else seg.upper()
                    if numbered_match:
                        lines.append(f"{idx}. {seg}")
                        idx += 1
                    else:
                        lines.append(f"- {seg}")
            return "\n".join(lines) if lines else text

        # Email context (Rule 5)
        if mode == DictationMode.EMAIL:
            text = re.sub(r"\b(hi|hello|dear)\s+([a-zA-Z]+)(?:\s*,|\s+comma)?", r"\1 \2,\n\n", text, flags=re.IGNORECASE)
            text = re.sub(r"\b(best regards|warm regards|thanks|thank you|sincerely|cheers)(?:\s*,|\s+comma)?\s*([a-zA-Z\s]*)$", r"\n\n\1,\n\2", text, flags=re.IGNORECASE)

        return text

    @classmethod
    def _normalize_grammar_and_spacing(cls, text: str) -> str:
        # If code block, preserve as-is
        if text.startswith("```") and text.endswith("```"):
            return text

        # Normalize multiple spaces and whitespace around newlines
        res = re.sub(r"[ \t]+", " ", text)
        res = re.sub(r"\s*\n\s*", "\n", res)
        res = re.sub(r"\n{3,}", "\n\n", res)

        # Remove spaces before punctuation (e.g., "hello , world ." -> "hello, world.")
        res = re.sub(r"\s+([,.:;?!])", r"\1", res)

        # Resolve conflicting adjacent punctuation (e.g., ", ." -> ".")
        res = re.sub(r",\s*([.?!])", r"\1", res)

        # Ensure single space after punctuation (unless followed by newline or end of string)
        res = re.sub(r"([,.:;?!])(?=[^\s\n\d\"'])", r"\1 ", res)

        # Capitalize sentences (start of text, after . ! ? or newline)
        def capitalize_sentence(match):
            prefix = match.group(1)
            char = match.group(2)
            return prefix + char.upper()

        res = re.sub(r"(^|[.!?\n]\s*)([a-z])", capitalize_sentence, res.strip())

        # Capitalize standalone pronoun "I" and contractions
        res = re.sub(r"\bi\b", "I", res)
        res = re.sub(r"\bi'([a-z]+)\b", r"I'\1", res)

        # Add closing punctuation if completely missing and not bullet or code mode
        if res and not res.endswith((".", "!", "?", "\n", '"', "'", "-", "`")):
            if not res.startswith("-"):
                res += "."

        return res.strip()


AMOZVZ_AGENT_SYSTEM_PROMPT = """You are an advanced voice-to-text post-processor.

Your task is to take raw, unedited, spoken-word speech transcription and convert it into clean, beautifully formatted written text while strictly preserving the speaker's original intent and meaning.

Follow these strict rules:
1. Remove all filler words (e.g., "um," "uh," "like," "you know," "actually," "I mean").
2. Resolve self-corrections and false starts seamlessly (e.g., "Let's meet at 4, wait, no, 5 PM" should become "Let's meet at 5:00 PM").
3. Apply natural grammar, capitalization, and punctuation.
4. Auto-structure the output: if the speaker lists steps or items, format them as clean bullet points or numbered lists; use paragraphs for distinct thoughts.
5. Adapt formatting to context: if the user mentions code, format code blocks properly; if it's casual chat, keep it concise; if it's an email, format it professionally.
6. Do NOT add preamble, commentary, or conversational filler (e.g., do not say "Here is your cleaned text:"). Return ONLY the finalized text.
"""


class AmozVzAgent:
    """
    Main AmozVz Agent Controller.
    Dispatches to Cloud LLM (OpenAI/Groq/Gemini/Ollama) if available, or seamlessly uses RuleBasedHesitationCleaner.
    """

    def __init__(self, api_key: Optional[str] = None, provider: str = "auto", base_url: Optional[str] = None):
        self.api_key = api_key or os.getenv("AMOZVZ_API_KEY") or os.getenv("GROQ_API_KEY") or os.getenv("OPENAI_API_KEY")
        self.provider = provider
        self.base_url = base_url

    def clean(
        self,
        raw_text: str,
        mode: DictationMode = DictationMode.AUTO,
        strip_hesitations: bool = True,
        resolve_corrections: bool = True,
        custom_dict: Optional[Dict[str, str]] = None,
        context: Optional[str] = None,
    ) -> CleanResponse:
        # Check if LLM agent can be used
        if self.api_key and self.provider != "rule_based":
            try:
                return self._clean_with_llm(
                    raw_text, mode, strip_hesitations, resolve_corrections, custom_dict, context
                )
            except Exception as e:
                # Graceful fallback to rule-based engine on any network or API issue
                print(f"[AmozVzAgent] LLM provider error ({e}), falling back to local rule-based cleaner.")

        cleaned_text, metrics = RuleBasedHesitationCleaner.clean(
            text=raw_text,
            strip_hesitations=strip_hesitations,
            resolve_corrections=resolve_corrections,
            custom_dict=custom_dict,
            mode=mode,
        )

        return CleanResponse(
            cleaned_text=cleaned_text,
            raw_text=raw_text,
            metrics=metrics,
            mode=mode,
        )

    def _clean_with_llm(
        self,
        raw_text: str,
        mode: DictationMode,
        strip_hesitations: bool,
        resolve_corrections: bool,
        custom_dict: Optional[Dict[str, str]],
        context: Optional[str],
    ) -> CleanResponse:
        import httpx

        start_time = time.perf_counter()
        
        # Determine provider and endpoint
        groq_key = os.getenv("GROQ_API_KEY")
        openai_key = os.getenv("OPENAI_API_KEY")
        
        endpoint = "https://api.groq.com/openai/v1/chat/completions" if groq_key else "https://api.openai.com/v1/chat/completions"
        key = groq_key or openai_key or self.api_key
        model = "llama-3.3-70b-versatile" if groq_key else "gpt-4o-mini"
        if self.base_url:
            endpoint = f"{self.base_url.rstrip('/')}/chat/completions"

        user_prompt = f"Mode: {mode.value}\n"
        if custom_dict:
            user_prompt += f"Custom Dictionary: {custom_dict}\n"
        if context:
            user_prompt += f"Target App / Context: {context}\n"
        user_prompt += f"\nRaw Speech:\n{raw_text}"

        headers = {
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": model,
            "messages": [
                {"role": "system", "content": AMOZVZ_AGENT_SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
            "temperature": 0.2,
            "max_tokens": 1024,
        }

        with httpx.Client(timeout=8.0) as client:
            resp = client.post(endpoint, headers=headers, json=payload)
            resp.raise_for_status()
            data = resp.json()
            cleaned_text = data["choices"][0]["message"]["content"].strip()

        elapsed_ms = (time.perf_counter() - start_time) * 1000.0
        metrics = DictationMetrics(
            hesitations_removed_count=0,
            corrections_resolved_count=0,
            processing_time_ms=round(elapsed_ms, 2),
            engine_used=f"llm_{model}",
        )

        return CleanResponse(
            cleaned_text=cleaned_text,
            raw_text=raw_text,
            metrics=metrics,
            mode=mode,
        )
