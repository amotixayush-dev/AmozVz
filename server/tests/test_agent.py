"""
Unit tests for AmozVz Advanced Voice-to-Text Post-Processor.
Validates the 6 strict post-processing rules.
"""

import unittest
from app.agent import RuleBasedHesitationCleaner, AmozVzAgent
from app.models import DictationMode


class TestAmozVzAgent(unittest.TestCase):

    def setUp(self):
        self.agent = AmozVzAgent(provider="rule_based")

    def test_rule1_remove_all_filler_words(self):
        """Rule 1: Remove all filler words (um, uh, like, you know, actually, I mean)."""
        raw = "Um, uh, like, you know, we should actually start, I mean basically now."
        resp = self.agent.clean(raw, mode=DictationMode.AUTO)

        self.assertNotIn("um", resp.cleaned_text.lower())
        self.assertNotIn("uh", resp.cleaned_text.lower())
        self.assertNotIn("you know", resp.cleaned_text.lower())
        self.assertNotIn("actually", resp.cleaned_text.lower())
        self.assertNotIn("i mean", resp.cleaned_text.lower())
        self.assertNotIn("basically", resp.cleaned_text.lower())
        self.assertIn("We should start", resp.cleaned_text)

    def test_rule2_resolve_self_corrections_example(self):
        """Rule 2: Resolve self-corrections (e.g. 'Let's meet at 4, wait, no, 5 PM' -> 'Let's meet at 5:00 PM')."""
        raw = "Let's meet at 4, wait, no, 5 PM"
        resp = self.agent.clean(raw, mode=DictationMode.AUTO)

        self.assertNotIn("4", resp.cleaned_text)
        self.assertIn("5:00 PM", resp.cleaned_text)
        self.assertEqual("Let's meet at 5:00 PM.", resp.cleaned_text)

    def test_rule2_scratch_that(self):
        """Rule 2: 'scratch that' speech change correction."""
        raw = "Send the invoice to Sarah, scratch that, send it to Michael."
        resp = self.agent.clean(raw, mode=DictationMode.AUTO)

        self.assertNotIn("Sarah", resp.cleaned_text)
        self.assertIn("Michael", resp.cleaned_text)
        self.assertEqual("Send it to Michael.", resp.cleaned_text)

    def test_rule3_apply_natural_grammar_and_punctuation(self):
        """Rule 3: Natural grammar, capitalization, and punctuation."""
        raw = "hello everyone how are you today period i hope you are having a wonderful week"
        resp = self.agent.clean(raw, mode=DictationMode.AUTO)

        self.assertTrue(resp.cleaned_text.startswith("Hello"))
        self.assertIn("I hope", resp.cleaned_text)
        self.assertTrue(resp.cleaned_text.endswith("."))

    def test_rule4_auto_structure_lists(self):
        """Rule 4: Auto-structure lists into clean bullet points or numbered lists."""
        raw = "First buy milk. Second buy eggs. Third buy bread."
        resp = self.agent.clean(raw, mode=DictationMode.LISTS)

        lines = resp.cleaned_text.strip().split("\n")
        self.assertTrue(len(lines) >= 3)
        self.assertTrue(any("Buy milk" in line for line in lines))
        self.assertTrue(any("Buy eggs" in line for line in lines))
        self.assertTrue(any("Buy bread" in line for line in lines))

    def test_rule5_context_code_formatting(self):
        """Rule 5: Adapt formatting to context (code blocks for code)."""
        raw = "def calculate_sum(a, b): return a + b"
        resp = self.agent.clean(raw, mode=DictationMode.CODE)

        self.assertTrue(resp.cleaned_text.startswith("```"))
        self.assertTrue(resp.cleaned_text.endswith("```"))
        self.assertIn("def calculate_sum", resp.cleaned_text)

    def test_rule6_no_preamble_or_commentary(self):
        """Rule 6: Return ONLY the finalized text without conversational filler."""
        raw = "Um, this is our finalized text."
        resp = self.agent.clean(raw, mode=DictationMode.AUTO)

        self.assertFalse(resp.cleaned_text.startswith("Here is"))
        self.assertFalse(resp.cleaned_text.startswith("Cleaned:"))
        self.assertEqual("This is our finalized text.", resp.cleaned_text)


if __name__ == "__main__":
    unittest.main()
