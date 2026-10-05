package com.amozvz.app

import com.amozvz.app.data.models.DictationMode
import com.amozvz.app.engine.HesitationFilter
import org.junit.Assert.*
import org.junit.Test

class HesitationFilterTest {

    @Test
    fun testRule1_RemoveAllFillerWords() {
        val raw = "Um, uh, like, we should you know actually test this, I mean basically now."
        val result = HesitationFilter.clean(raw)

        assertFalse(result.cleanedText.contains("um", ignoreCase = true))
        assertFalse(result.cleanedText.contains("uh", ignoreCase = true))
        assertFalse(result.cleanedText.contains("you know", ignoreCase = true))
        assertFalse(result.cleanedText.contains("actually", ignoreCase = true))
        assertFalse(result.cleanedText.contains("I mean", ignoreCase = true))
        assertFalse(result.cleanedText.contains("basically", ignoreCase = true))
        assertTrue(result.cleanedText.startsWith("We should test this"))
    }

    @Test
    fun testRule2_ResolveSelfCorrectionsExample() {
        // Exact example from user prompt: "Let's meet at 4, wait, no, 5 PM" -> "Let's meet at 5:00 PM"
        val raw = "Let's meet at 4, wait, no, 5 PM"
        val result = HesitationFilter.clean(raw)

        assertFalse(result.cleanedText.contains("4"))
        assertTrue(result.cleanedText.contains("5:00 PM"))
        assertEquals("Let's meet at 5:00 PM.", result.cleanedText)
    }

    @Test
    fun testRule2_ScratchThatCorrection() {
        val raw = "Send the document to Sarah, scratch that, send it to Alex."
        val result = HesitationFilter.clean(raw)

        assertFalse(result.cleanedText.contains("Sarah"))
        assertTrue(result.cleanedText.contains("Alex"))
        assertEquals("Send it to Alex.", result.cleanedText)
    }

    @Test
    fun testRule3_GrammarCapitalizationAndPunctuation() {
        val raw = "hello everyone how are you today period i hope you are doing well"
        val result = HesitationFilter.clean(raw)

        assertTrue(result.cleanedText.startsWith("Hello"))
        assertTrue(result.cleanedText.contains("I hope"))
        assertTrue(result.cleanedText.endsWith("."))
    }

    @Test
    fun testRule4_AutoStructureLists() {
        val raw = "First buy milk. Second buy eggs. Third buy bread."
        val result = HesitationFilter.clean(raw, mode = DictationMode.LISTS)

        val lines = result.cleanedText.lines()
        assertTrue(lines.size >= 3)
        assertTrue(result.cleanedText.contains("Buy milk"))
        assertTrue(result.cleanedText.contains("Buy eggs"))
        assertTrue(result.cleanedText.contains("Buy bread"))
    }

    @Test
    fun testRule5_CodeContextFormatting() {
        val raw = "def calculate_total(items): return sum(items)"
        val result = HesitationFilter.clean(raw, mode = DictationMode.CODE)

        assertTrue(result.cleanedText.startsWith("```"))
        assertTrue(result.cleanedText.endsWith("```"))
        assertTrue(result.cleanedText.contains("def calculate_total"))
    }

    @Test
    fun testRule6_NoPreambleOrCommentary() {
        val raw = "Um, this is a test transcription."
        val result = HesitationFilter.clean(raw)

        assertFalse(result.cleanedText.contains("Here is"))
        assertFalse(result.cleanedText.contains("Cleaned:"))
        assertEquals("This is a test transcription.", result.cleanedText)
    }
}
