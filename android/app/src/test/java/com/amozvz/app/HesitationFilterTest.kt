package com.amozvz.app

import com.amozvz.app.data.models.DictationMode
import com.amozvz.app.engine.HesitationFilter
import org.junit.Assert.*
import org.junit.Test

class HesitationFilterTest {

    @Test
    fun testHesitationRemoval() {
        val raw = "Um, uh, hello team, ah, we are, you know, basically ready to begin."
        val result = HesitationFilter.clean(raw, DictationMode.FLOW_NATURAL)

        assertFalse(result.cleanedText.contains("um", ignoreCase = true))
        assertFalse(result.cleanedText.contains("uh", ignoreCase = true))
        assertFalse(result.cleanedText.contains("you know", ignoreCase = true))
        assertFalse(result.cleanedText.contains("basically", ignoreCase = true))
        assertTrue(result.cleanedText.startsWith("Hello team"))
        assertTrue(result.cleanedText.contains("ready to begin"))
        assertTrue(result.hesitationsRemovedCount > 0)
    }

    @Test
    fun testStutterRepetitionRemoval() {
        val raw = "I I think we we should test th-the feature."
        val result = HesitationFilter.clean(raw, DictationMode.FLOW_NATURAL)

        assertEquals("I think we should test the feature.", result.cleanedText)
    }

    @Test
    fun testSelfCorrectionScratchThat() {
        val raw = "Send the email to Sarah, scratch that, send it to Alex."
        val result = HesitationFilter.clean(raw, DictationMode.FLOW_NATURAL)

        assertFalse(result.cleanedText.contains("Sarah"))
        assertTrue(result.cleanedText.contains("Alex"))
        assertEquals("Send it to Alex.", result.cleanedText)
    }

    @Test
    fun testSelfCorrectionWaitMakeThat() {
        val raw = "Let's meet at 2:00 wait make that 3:30 PM tomorrow."
        val result = HesitationFilter.clean(raw, DictationMode.FLOW_NATURAL)

        assertFalse(result.cleanedText.contains("2:00"))
        assertTrue(result.cleanedText.contains("3:30 PM"))
    }

    @Test
    fun testSpokenPunctuationConversion() {
        val raw = "Hello Alice comma how are you question mark I am doing great period"
        val result = HesitationFilter.clean(raw, DictationMode.FLOW_NATURAL)

        assertEquals("Hello Alice, how are you? I am doing great.", result.cleanedText)
    }

    @Test
    fun testBulletPointsMode() {
        val raw = "Buy groceries. Call mom. Finish homework."
        val result = HesitationFilter.clean(raw, DictationMode.BULLET_POINTS)

        val lines = result.cleanedText.lines()
        assertTrue(lines.all { it.startsWith("- ") })
        assertTrue(result.cleanedText.contains("Buy groceries"))
        assertTrue(result.cleanedText.contains("Call mom"))
        assertTrue(result.cleanedText.contains("Finish homework"))
    }

    @Test
    fun testRawVerbatimKeepsFillers() {
        val raw = "Um, this is uh verbatim."
        val result = HesitationFilter.clean(raw, DictationMode.RAW_VERBATIM)

        assertTrue(result.cleanedText.contains("Um"))
        assertTrue(result.cleanedText.contains("uh"))
    }

    @Test
    fun testCustomDictionary() {
        val raw = "We deploy to k8s using amoz vz."
        val dict = mapOf("k8s" to "Kubernetes", "amoz vz" to "AmozVz")
        val result = HesitationFilter.clean(raw, customDictionary = dict)

        assertTrue(result.cleanedText.contains("Kubernetes"))
        assertTrue(result.cleanedText.contains("AmozVz"))
    }
}
