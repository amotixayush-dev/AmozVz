#!/usr/bin/env python3
"""
AmozVz Command Line Interface.
Dictate voice or clean speech text directly from the terminal.
"""

import sys
import argparse
from app.agent import AmozVzAgent
from app.models import DictationMode


def main():
    parser = argparse.ArgumentParser(
        description="AmozVz: AI Voice-to-Text Dictation Agent (Hesitation & Self-Correction Cleaner)"
    )
    parser.add_argument("text", nargs="?", default=None, help="Raw speech text to clean (or omit to read from stdin)")
    parser.add_argument(
        "--mode",
        choices=[m.value for m in DictationMode],
        default=DictationMode.FLOW_NATURAL.value,
        help="Dictation polishing mode",
    )
    parser.add_argument("--no-hesitations", action="store_true", help="Do not strip hesitations/fillers")
    parser.add_argument("--no-corrections", action="store_true", help="Do not resolve self-corrections")
    parser.add_argument("--context", type=str, default=None, help="Target context (e.g. email, slack, code)")

    args = parser.parse_args()

    input_text = args.text
    if not input_text:
        if not sys.stdin.isatty():
            input_text = sys.stdin.read().strip()
        else:
            print("Enter raw speech (Ctrl+D to finish):")
            input_text = sys.stdin.read().strip()

    if not input_text:
        print("Error: No text provided.", file=sys.stderr)
        sys.exit(1)

    agent = AmozVzAgent()
    response = agent.clean(
        raw_text=input_text,
        mode=DictationMode(args.mode),
        strip_hesitations=not args.no_hesitations,
        resolve_corrections=not args.no_corrections,
        context=args.context,
    )

    print("\n--- AmozVz Polished Output ---")
    print(response.cleaned_text)
    print("------------------------------")
    print(
        f"Metrics: {response.metrics.hesitations_removed_count} fillers removed, "
        f"{response.metrics.corrections_resolved_count} corrections resolved, "
        f"{response.metrics.processing_time_ms}ms ({response.metrics.engine_used})\n"
    )


if __name__ == "__main__":
    main()
