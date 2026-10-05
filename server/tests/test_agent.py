"""
Unit tests for AmozVz AI Voice-to-Text Agent.
Validates hesitations, self-corrections, speech changes, punctuations, and modes.
"""

import unittest
from app.agent import RuleBasedHesitationCleaner, AmozVzAgent
from app.models import DictationMode


class TestAmozVzAgent(unittest.TestCase):

    def setUp(self):
        self.agent = AmozVzAgent(provider="rule_based")

    def test_hesitations_removal(self):
        """Tests that vocal hesitations and filler sounds are cleanly stripped."""
        raw = "Um, uh, hello everyone, ah, we are, you know, basically ready to start."
        resp = self.agent.clean(raw, mode=DictationMode.FLOW_NATURAL)
        
        self.assertNotIn("um", resp.cleaned_text.lower())
        self.assertNotIn("uh", resp.cleaned_text.lower())
        self.assertNotIn("you know", resp.cleaned_text.lower())
        self.assertNotIn("basically", resp.cleaned_text.lower())
        self.assertIn("Hello everyone", resp.cleaned_text)
        self.assertIn("ready to start", resp.cleaned_text)
        self.assertGreater(resp.metrics.hesitations_removed_count, 0)

    def test_stutter_repetition_removal(self):
        """Tests that stuttered consecutive words and syllable repetitions are cleaned."""
        raw = "I I think we we should schedule th-the appointment."
        resp = self.agent.clean(raw, mode=DictationMode.FLOW_NATURAL)
        
        self.assertEqual(resp.cleaned_text, "I think we should schedule the appointment.")

    def test_self_correction_scratch_that(self):
        """Tests 'scratch that' speech change correction."""
        raw = "Send the invoice to Sarah, scratch that, send it to Michael."
        resp = self.agent.clean(raw, mode=DictationMode.FLOW_NATURAL)
        
        self.assertNotIn("Sarah", resp.cleaned_text)
        self.assertIn("Michael", resp.cleaned_text)
        self.assertEqual(resp.cleaned_text, "Send it to Michael.")

    def test_self_correction_wait_make_that(self):
        """Tests time/target correction like 'meet at 2 wait make that 3:30 PM'."""
        raw = "Let's meet at 2:00 wait make that 3:30 PM tomorrow."
        resp = self.agent.clean(raw, mode=DictationMode.FLOW_NATURAL)
        
        self.assertNotIn("2:00", resp.cleaned_text)
        self.assertIn("3:30 PM", resp.cleaned_text)

    def test_spoken_punctuation_conversion(self):
        """Tests spoken voice punctuation commands (period, comma, question mark)."""
        raw = "Hello John comma how are you doing today question mark I hope everything is great period"
        resp = self.agent.clean(raw, mode=DictationMode.FLOW_NATURAL)
        
        self.assertEqual(resp.cleaned_text, "Hello John, how are you doing today? I hope everything is great.")

    def test_bullet_points_mode(self):
        """Tests bullet point mode formatting."""
        raw = "Buy apples. Buy oranges. Buy milk."
        resp = self.agent.clean(raw, mode=DictationMode.BULLET_POINTS)
        
        lines = resp.cleaned_text.strip().split("\n")
        self.assertTrue(all(line.startswith("- ") for line in lines))
        self.assertIn("Buy apples", resp.cleaned_text)
        self.assertIn("Buy oranges", resp.cleaned_text)
        self.assertIn("Buy milk", resp.cleaned_text)

    def test_raw_verbatim_mode(self):
        """Tests that raw verbatim keeps filler words untouched."""
        raw = "Um, this is uh verbatim test."
        resp = self.agent.clean(raw, mode=DictationMode.RAW_VERBATIM)
        
        self.assertIn("Um", resp.cleaned_text)
        self.assertIn("uh", resp.cleaned_text)

    def test_custom_dictionary(self):
        """Tests replacement of user-defined jargon or names."""
        raw = "We are deploying to k8s using amoz vz."
        custom_dict = {"k8s": "Kubernetes", "amoz vz": "AmozVz"}
        resp = self.agent.clean(raw, custom_dict=custom_dict)
        
        self.assertIn("Kubernetes", resp.cleaned_text)
        self.assertIn("AmozVz", resp.cleaned_text)

    def test_capitalization_and_periods(self):
        """Tests sentence casing and closing punctuation."""
        raw = "this is sentence one. this is sentence two"
        resp = self.agent.clean(raw)
        
        self.assertTrue(resp.cleaned_text.startswith("This"))
        self.assertIn("This is sentence two.", resp.cleaned_text)


if __name__ == "__main__":
    unittest.main()
