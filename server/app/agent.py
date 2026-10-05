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
    r"\bbasically\b",
    r"\bliterally\b",
    r"\bso\s+yeah\b",
    r"\bI\s+mean\b",
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
# e.g.: "meet at 2 wait actually 3:30" -> replaces target
CORRECTION_PATTERNS = [
    # "scratch that, [replacement]"
    re.compile(r"(?:^|(?<=[.!?,\s]))(?P<before>.+?)\s*,?\s*(?:scratch\s+that|never\s+mind(?:\s+that)?)\s*,?\s*(?P<after>.+)$", re.IGNORECASE),
    # "wait no, [replacement]" / "no wait, [replacement]"
    re.compile(r"(?:^|(?<=[.!?,\s]))(?P<before>.+?)\s*,?\s*(?:wait\s+no|no\s+wait|wait\s+actually|actually\s+wait)\s*,?\s*(?P<after>.+)$", re.IGNORECASE),
    # "[target] actually [replacement]" (e.g. "at 4 actually at 5")
    re.compile(r"(?:^|(?<=[.!?,\s]))(?P<before>.+?)\s*,?\s*(?:actually|no\s+I\s+meant|or\s+rather|correction)\s*,?\s*(?P<after>.+)$", re.IGNORECASE),
    # "wait make that [replacement]"
    re.compile(r"(?:^|(?<=[.!?,\s]))(?P<before>.+?)\s*,?\s*(?:wait\s+make\s+that|make\s+that)\s*,?\s*(?P<after>.+)$", re.IGNORECASE),
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
        mode: DictationMode = DictationMode.FLOW_NATURAL,
    ) -> Tuple[str, DictationMetrics]:
        start_time = time.perf_counter()
        raw = text.strip()
        if not raw:
            return "", DictationMetrics(processing_time_ms=0.0, engine_used="rule_based")

        cleaned = raw
        hesitations_count = 0
        corrections_count = 0

        # 1. Resolve self-corrections / speech changes if enabled
        if resolve_corrections and mode != DictationMode.RAW_VERBATIM:
            cleaned, corrections_count = cls._resolve_corrections(cleaned)

        # 2. Convert explicit spoken punctuation commands
        cleaned = cls._apply_spoken_punctuation(cleaned)

        # 3. Strip hesitations & filler words if enabled
        if strip_hesitations and mode != DictationMode.RAW_VERBATIM:
            cleaned, hesitations_count = cls._strip_hesitations(cleaned)

        # 4. Remove stutter repetition (e.g., "I I think", "the the meeting")
        if mode != DictationMode.RAW_VERBATIM:
            cleaned = cls._remove_stutters(cleaned)

        # 5. Apply custom dictionary substitutions
        if custom_dict:
            for source, replacement in custom_dict.items():
                pattern = re.compile(r"\b" + re.escape(source) + r"\b", re.IGNORECASE)
                cleaned = pattern.sub(replacement, cleaned)

        # 6. Apply formatting based on mode
        cleaned = cls._format_by_mode(cleaned, mode)

        # 7. Normalize punctuation spacing & sentence capitalization
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
                
                # If before and after are clauses, inspect if before has a matching predicate or if it was a complete replacement
                # E.g.: "meet tomorrow at 2 wait make that 3:30 pm"
                # If after is a fragment (e.g., "3:30 pm" or "Tuesday"), replace the last token or phrase in before.
                after_words = after.split()
                before_words = before.split()
                
                # Check for direct word/fragment substitution
                if len(after_words) <= 3 and len(before_words) >= len(after_words):
                    # Replace the ending tokens of before with after
                    kept_before = " ".join(before_words[:-len(after_words)])
                    if kept_before:
                        result = f"{kept_before} {after}"
                    else:
                        result = after
                else:
                    # Full clause replacement: replace previous false-start clause
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

        # Replace vocalized fillers
        for filler in FILLER_WORDS:
            # Match filler optionally flanked by commas/punctuation e.g. ", um," or "um,"
            filler_pattern = re.compile(r"(?:,\s*)?" + filler + r"(?:\s*,)?", re.IGNORECASE)
            matches = filler_pattern.findall(res)
            if matches:
                count += len(matches)
                res = filler_pattern.sub(" ", res)

        # Replace filler "like" when surrounded by commas or at start of phrase e.g. "I was, like, so happy"
        like_filler_pattern = re.compile(r"(?:,\s*like\s*,|\s+like\s*,|,\s*like\s+)", re.IGNORECASE)
        like_matches = like_filler_pattern.findall(res)
        if like_matches:
            count += len(like_matches)
            res = like_filler_pattern.sub(" ", res)

        # Clean orphaned double commas, leading commas, or weird spacing
        res = re.sub(r",\s*,+", ",", res)
        res = re.sub(r"^[,\s]+", "", res)
        res = re.sub(r"\s+,\s*", ", ", res)

        return res, count

    @classmethod
    def _remove_stutters(cls, text: str) -> str:
        # Matches word repeated consecutively: "we we", "the the", "I I"
        repetition_pattern = re.compile(r"\b(\w+)\s+\1\b", re.IGNORECASE)
        # Apply twice in case of triple repetitions ("I I I")
        res = repetition_pattern.sub(r"\1", text)
        res = repetition_pattern.sub(r"\1", res)
        
        # Matches hyphenated stutter syllables: "w-what", "th-the"
        hyphen_stutter = re.compile(r"\b[a-zA-Z]{1,2}-\b", re.IGNORECASE)
        res = hyphen_stutter.sub("", res)
        return res

    @classmethod
    def _format_by_mode(cls, text: str, mode: DictationMode) -> str:
        if mode == DictationMode.BULLET_POINTS:
            # Split by period or semicolon or list keywords (first, second, also, next)
            lines = []
            raw_segments = re.split(r"(?<=[.!?])\s+|\s+(?:first|second|third|next|finally|also)\s+", text, flags=re.IGNORECASE)
            for seg in raw_segments:
                seg = seg.strip().lstrip("-*•").strip()
                if seg:
                    # Ensure first char is capitalized
                    seg = seg[0].upper() + seg[1:] if len(seg) > 1 else seg.upper()
                    lines.append(f"- {seg}")
            return "\n".join(lines) if lines else text

        return text

    @classmethod
    def _normalize_grammar_and_spacing(cls, text: str) -> str:
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

        # Add closing punctuation if completely missing and not bullet mode
        if res and not res.endswith((".", "!", "?", "\n", '"', "'", "-")):
            if not res.startswith("-"):
                res += "."

        return res.strip()


AMOZVZ_AGENT_SYSTEM_PROMPT = """You are AmozVz, an elite real-time voice-to-text dictation agent inspired by Wispr Flow.
Your job is to transform raw, imperfect spoken transcriptions into clean, natural, punctuated, and ready-to-use written text.

Guidelines:
1. HESITATIONS & FILLER WORDS:
   - Strip vocal fillers: "um", "uh", "er", "ah", "like" (when used as filler), "you know", "kind of", "sort of", "basically".
   - Keep intentional words (e.g. "I like apple" must keep "like").

2. SELF-CORRECTIONS & SPEECH CHANGES:
   - Speech often includes false starts or changes of mind (e.g., "Let's meet at 3 wait make that 4:30 pm" -> "Let's meet at 4:30 PM.").
   - "Send this to Alice scratch that send to Bob and CC Alice" -> "Send this to Bob and CC Alice."
   - Always output the FINAL intended meaning seamlessly.

3. PUNCTUATION & CAPITALIZATION:
   - Insert natural commas, periods, question marks, and paragraphs.
   - Respect explicit verbal commands like "period", "comma", "new line", "question mark".
   - Capitalize proper nouns, acronyms, and sentence starts.

4. DICTATION MODES:
   - flow_natural (default): Clean, conversational, natural flow.
   - professional: Formal, concise, business-appropriate.
   - bullet_points: Markdown bullet list (- item).
   - raw_verbatim: Exact transcription without removing fillers.

CRITICAL INSTRUCTION:
Output ONLY the final cleaned text. Do NOT explain, do NOT provide commentary, and do NOT wrap in quotes.
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
        mode: DictationMode = DictationMode.FLOW_NATURAL,
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
