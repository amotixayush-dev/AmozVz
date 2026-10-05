package com.amozvz.app.engine

import com.amozvz.app.data.models.DictationMode
import com.amozvz.app.data.models.TranscriptionResult
import java.util.regex.Pattern

/**
 * AmozVz High-Performance Hesitation and Speech-Correction Filter.
 * Formats natural spoken audio into clean, punctuated, ready-to-paste text like Wispr Flow.
 */
object HesitationFilter {

    private val FILLER_WORDS = listOf(
        "\\buh+\\b",
        "\\bum+\\b",
        "\\ber+\\b",
        "\\bah+\\b",
        "\\bhmm+\\b",
        "\\byou\\s+know\\b",
        "\\bkind\\s+of\\b",
        "\\bsort\\s+of\\b",
        "\\bbasically\\b",
        "\\bliterally\\b",
        "\\bso\\s+yeah\\b",
        "\\bI\\s+mean\\b"
    )

    private val PUNCTUATION_COMMANDS = listOf(
        Pair(Pattern.compile("\\b(new\\s+paragraph)\\b", Pattern.CASE_INSENSITIVE), "\n\n"),
        Pair(Pattern.compile("\\b(new\\s+line)\\b", Pattern.CASE_INSENSITIVE), "\n"),
        Pair(Pattern.compile("\\b(period|full\\s+stop)\\b", Pattern.CASE_INSENSITIVE), "."),
        Pair(Pattern.compile("\\b(comma)\\b", Pattern.CASE_INSENSITIVE), ","),
        Pair(Pattern.compile("\\b(question\\s+mark)\\b", Pattern.CASE_INSENSITIVE), "?"),
        Pair(Pattern.compile("\\b(exclamation\\s+mark|exclamation\\s+point)\\b", Pattern.CASE_INSENSITIVE), "!"),
        Pair(Pattern.compile("\\b(colon)\\b", Pattern.CASE_INSENSITIVE), ":"),
        Pair(Pattern.compile("\\b(semi-colon|semicolon)\\b", Pattern.CASE_INSENSITIVE), ";"),
        Pair(Pattern.compile("\\b(hyphen|dash)\\b", Pattern.CASE_INSENSITIVE), " - "),
        Pair(Pattern.compile("\\b(open\\s+quote|start\\s+quote)\\b", Pattern.CASE_INSENSITIVE), " \""),
        Pair(Pattern.compile("\\b(close\\s+quote|end\\s+quote)\\b", Pattern.CASE_INSENSITIVE), "\" "),
        Pair(Pattern.compile("\\b(bullet\\s+point)\\b", Pattern.CASE_INSENSITIVE), "\n- ")
    )

    private val CORRECTION_PATTERNS = listOf(
        // "scratch that, [replacement]"
        Pattern.compile("(?:^|(?<=[.!?\\s]))(?<before>.+?)\\s*,?\\s*(?:scratch\\s+that|never\\s+mind(?:\\s+that)?)\\s*,?\\s*(?<after>.+)$", Pattern.CASE_INSENSITIVE),
        // "wait no, [replacement]"
        Pattern.compile("(?:^|(?<=[.!?\\s]))(?<before>.+?)\\s*,?\\s*(?:wait\\s+no|no\\s+wait|wait\\s+actually|actually\\s+wait)\\s*,?\\s*(?<after>.+)$", Pattern.CASE_INSENSITIVE),
        // "[target] actually [replacement]"
        Pattern.compile("(?:^|(?<=[.!?\\s]))(?<before>.+?)\\s*,?\\s*(?:actually|no\\s+I\\s+meant|or\\s+rather|correction)\\s*,?\\s*(?<after>.+)$", Pattern.CASE_INSENSITIVE),
        // "wait make that [replacement]"
        Pattern.compile("(?:^|(?<=[.!?\\s]))(?<before>.+?)\\s*,?\\s*(?:wait\\s+make\\s+that|make\\s+that)\\s*,?\\s*(?<after>.+)$", Pattern.CASE_INSENSITIVE)
    )

    fun clean(
        rawText: String,
        mode: DictationMode = DictationMode.FLOW_NATURAL,
        stripHesitations: Boolean = true,
        resolveCorrections: Boolean = true,
        customDictionary: Map<String, String> = emptyMap()
    ): TranscriptionResult {
        val startTime = System.currentTimeMillis()
        var text = rawText.trim()
        if (text.isEmpty()) {
            return TranscriptionResult(
                rawText = rawText,
                cleanedText = "",
                mode = mode
            )
        }

        var hesitationsCount = 0
        var correctionsCount = 0

        // 1. Resolve self-corrections / speech changes
        if (resolveCorrections && mode != DictationMode.RAW_VERBATIM) {
            val (correctedText, cCount) = resolveSpeechCorrections(text)
            text = correctedText
            correctionsCount = cCount
        }

        // 2. Convert spoken voice punctuation commands
        text = applySpokenPunctuation(text)

        // 3. Strip vocal hesitations and filler sounds
        if (stripHesitations && mode != DictationMode.RAW_VERBATIM) {
            val (dehesitatedText, hCount) = stripVocalHesitations(text)
            text = dehesitatedText
            hesitationsCount = hCount
        }

        // 4. Remove stutter repetition
        if (mode != DictationMode.RAW_VERBATIM) {
            text = removeStutters(text)
        }

        // 5. Apply custom vocabulary substitutions
        if (customDictionary.isNotEmpty()) {
            for ((key, value) in customDictionary) {
                val pattern = Pattern.compile("\\b" + Pattern.quote(key) + "\\b", Pattern.CASE_INSENSITIVE)
                text = pattern.matcher(text).replaceAll(value)
            }
        }

        // 6. Format according to mode
        text = formatByMode(text, mode)

        // 7. Grammar, punctuation spacing and capitalization normalization
        text = normalizeGrammarAndSpacing(text)

        val processingTime = System.currentTimeMillis() - startTime

        return TranscriptionResult(
            rawText = rawText,
            cleanedText = text,
            mode = mode,
            hesitationsRemovedCount = hesitationsCount,
            correctionsResolvedCount = correctionsCount,
            processingTimeMs = processingTime,
            engineUsed = "on_device"
        )
    }

    private fun resolveSpeechCorrections(text: String): Pair<String, Int> {
        var result = text
        var count = 0

        for (pattern in CORRECTION_PATTERNS) {
            val matcher = pattern.matcher(result)
            if (matcher.find()) {
                val before = matcher.group("before")?.trim() ?: ""
                val after = matcher.group("after")?.trim() ?: ""

                val beforeWords = before.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val afterWords = after.split("\\s+".toRegex()).filter { it.isNotBlank() }

                if (afterWords.size <= 3 && beforeWords.size >= afterWords.size) {
                    val keptBefore = beforeWords.dropLast(afterWords.size).joinToString(" ")
                    result = if (keptBefore.isNotBlank()) "$keptBefore $after" else after
                } else {
                    result = after
                }
                count++
                break
            }
        }

        return Pair(result, count)
    }

    private fun applySpokenPunctuation(text: String): String {
        var result = text
        for ((pattern, replacement) in PUNCTUATION_COMMANDS) {
            result = pattern.matcher(result).replaceAll(replacement)
        }
        return result
    }

    private fun stripVocalHesitations(text: String): Pair<String, Int> {
        var result = text
        var count = 0

        for (filler in FILLER_WORDS) {
            val pattern = Pattern.compile("(?:,\\s*)?$filler(?:\\s*,)?", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(result)
            while (matcher.find()) {
                count++
            }
            result = pattern.matcher(result).replaceAll(" ")
        }

        // Clean filler "like" flanked by commas: ", like,"
        val likePattern = Pattern.compile("(?:,\\s*like\\s*,|\\s+like\\s*,|,\\s*like\\s+)", Pattern.CASE_INSENSITIVE)
        val likeMatcher = likePattern.matcher(result)
        while (likeMatcher.find()) {
            count++
        }
        result = likePattern.matcher(result).replaceAll(" ")

        // Clean duplicate commas or leading punctuation
        result = result.replace(Regex(",\\s*,+"), ",")
        result = result.replace(Regex("^[\\s,]+"), "")
        result = result.replace(Regex("\\s+,\\s*"), ", ")

        return Pair(result, count)
    }

    private fun removeStutters(text: String): String {
        // Repeated identical words e.g. "we we", "the the"
        var result = text.replace(Regex("(?i)\\b(\\w+)\\s+\\1\\b"), "$1")
        result = result.replace(Regex("(?i)\\b(\\w+)\\s+\\1\\b"), "$1")

        // Hyphenated syllable stammers: "th-the", "w-what"
        result = result.replace(Regex("(?i)\\b[a-zA-Z]{1,2}-\\b"), "")
        return result
    }

    private fun formatByMode(text: String, mode: DictationMode): String {
        return when (mode) {
            DictationMode.BULLET_POINTS -> {
                val lines = mutableListOf<String>()
                val segments = text.split(Regex("(?<=[.!?])\\s+|\\s+(?:first|second|third|next|finally|also)\\s+", RegexOption.IGNORE_CASE))
                for (seg in segments) {
                    val trimmed = seg.trim().trimStart('-', '*', '•').trim()
                    if (trimmed.isNotEmpty()) {
                        val capitalized = trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        lines.add("- $capitalized")
                    }
                }
                if (lines.isNotEmpty()) lines.joinToString("\n") else text
            }
            else -> text
        }
    }

    private fun normalizeGrammarAndSpacing(text: String): String {
        var res = text.replace(Regex("[ \\t]+"), " ")
        res = res.replace(Regex("\\s*\\n\\s*"), "\n")
        res = res.replace(Regex("\\n{3,}"), "\n\n")

        // Remove space before punctuation
        res = res.replace(Regex("\\s+([,.:;?!])"), "$1")

        // Resolve conflicting punctuation e.g. ", ." -> "."
        res = res.replace(Regex(",\\s*([.?!])"), "$1")

        // Ensure single space after punctuation
        res = res.replace(Regex("([,.:;?!])(?=[^\\s\\n\\d\"'])"), "$1 ")

        // Capitalize sentences
        val sentenceRegex = Regex("(^|[.!?\\n]\\s*)([a-z])")
        res = sentenceRegex.replace(res) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val letter = matchResult.groupValues[2]
            prefix + letter.uppercase()
        }

        // Capitalize standalone pronoun "I" and contractions
        res = res.replace(Regex("\\bi\\b"), "I")
        res = res.replace(Regex("\\bi'([a-z]+)\\b"), "I'$1")

        // Ensure closing period if missing and not a list
        res = res.trim()
        if (res.isNotEmpty() && !res.endsWithAny(listOf(".", "!", "?", "\n", "\"", "'", "-"))) {
            if (!res.startsWith("-")) {
                res += "."
            }
        }

        return res
    }

    private fun String.endsWithAny(suffixes: List<String>): Boolean {
        return suffixes.any { this.endsWith(it) }
    }
}
