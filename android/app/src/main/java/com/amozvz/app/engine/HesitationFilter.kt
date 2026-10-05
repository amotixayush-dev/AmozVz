package com.amozvz.app.engine

import com.amozvz.app.data.models.DictationMode
import com.amozvz.app.data.models.TranscriptionResult
import java.util.regex.Pattern

/**
 * Advanced Voice-to-Text Post-Processor.
 *
 * Strictly follows the 6 Post-Processing Rules:
 * 1. Remove all filler words ("um," "uh," "like," "you know," "actually," "I mean").
 * 2. Resolve self-corrections and false starts seamlessly.
 * 3. Apply natural grammar, capitalization, and punctuation.
 * 4. Auto-structure the output: clean bullet points or numbered lists; paragraphs for distinct thoughts.
 * 5. Adapt formatting to context: code blocks, casual chat, or professional email.
 * 6. Return ONLY the finalized text (no preambles or commentary).
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
        "\\bactually\\b",
        "\\bI\\s+mean\\b",
        "\\bbasically\\b",
        "\\bliterally\\b",
        "\\bso\\s+yeah\\b"
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
        // e.g.: "Let's meet at 4, wait, no, 5 PM" -> "Let's meet at 5:00 PM"
        Pattern.compile("(?:^|(?<=[.!?\\s]))(?<before>.+?)\\s*,?\\s*(?:wait,?\\s*no|no,?\\s*wait|wait,?\\s*actually|actually,?\\s*wait)\\s*,?\\s*(?<after>.+)$", Pattern.CASE_INSENSITIVE),
        // "scratch that, [replacement]" or "never mind that, [replacement]"
        Pattern.compile("(?:^|(?<=[.!?\\s]))(?<before>.+?)\\s*,?\\s*(?:scratch\\s+that|never\\s+mind(?:\\s+that)?)\\s*,?\\s*(?<after>.+)$", Pattern.CASE_INSENSITIVE),
        // "wait make that [replacement]" or "make that [replacement]"
        Pattern.compile("(?:^|(?<=[.!?\\s]))(?<before>.+?)\\s*,?\\s*(?:wait\\s+make\\s+that|make\\s+that)\\s*,?\\s*(?<after>.+)$", Pattern.CASE_INSENSITIVE),
        // "or rather, [replacement]" or "correction, [replacement]"
        Pattern.compile("(?:^|(?<=[.!?\\s]))(?<before>.+?)\\s*,?\\s*(?:or\\s+rather|correction)\\s*,?\\s*(?<after>.+)$", Pattern.CASE_INSENSITIVE)
    )

    fun clean(
        rawText: String,
        mode: DictationMode = DictationMode.AUTO,
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

        // 1. Resolve self-corrections and false starts seamlessly (Rule 2)
        if (resolveCorrections) {
            val (correctedText, cCount) = resolveSpeechCorrections(text)
            text = correctedText
            correctionsCount = cCount
        }

        // 2. Convert spoken voice punctuation commands
        text = applySpokenPunctuation(text)

        // 3. Remove all filler words (Rule 1)
        if (stripHesitations) {
            val (dehesitatedText, hCount) = stripVocalHesitations(text)
            text = dehesitatedText
            hesitationsCount = hCount
        }

        // 4. Remove stutter repetitions
        text = removeStutters(text)

        // 5. Apply custom vocabulary substitutions
        if (customDictionary.isNotEmpty()) {
            for ((key, value) in customDictionary) {
                val pattern = Pattern.compile("\\b" + Pattern.quote(key) + "\\b", Pattern.CASE_INSENSITIVE)
                text = pattern.matcher(text).replaceAll(value)
            }
        }

        // 6. Format time expressions (e.g., "5 PM" -> "5:00 PM")
        text = normalizeTimeExpressions(text)

        // 7. Adapt formatting to context & auto-structure (Rules 4 & 5)
        text = formatByContext(text, mode)

        // 8. Apply natural grammar, capitalization, and punctuation (Rule 3)
        text = normalizeGrammarAndSpacing(text)

        val processingTime = System.currentTimeMillis() - startTime

        return TranscriptionResult(
            rawText = rawText,
            cleanedText = text,
            mode = mode,
            hesitationsRemovedCount = hesitationsCount,
            correctionsResolvedCount = correctionsCount,
            processingTimeMs = processingTime,
            engineUsed = "advanced_post_processor"
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

                // Check for target parameter substitution (e.g. "at 4, wait, no, 5 PM")
                if (afterWords.isNotEmpty() && beforeWords.isNotEmpty()) {
                    val lastBeforeWord = beforeWords.last().trimEnd(',', '.', '?', '!')
                    val afterFirstWord = afterWords.first().trimEnd(',', '.', '?', '!')

                    // If replacing a number or time fragment (e.g. "4" -> "5 PM")
                    val replaceCount = if (lastBeforeWord.matches(Regex("\\d+(:\\d+)?")) && afterFirstWord.matches(Regex("\\d+(:\\d+)?"))) {
                        1
                    } else if (afterWords.size <= 3 && beforeWords.size >= afterWords.size) {
                        afterWords.size
                    } else {
                        afterWords.size.coerceAtMost(beforeWords.size)
                    }

                    val keptBefore = beforeWords.dropLast(replaceCount).joinToString(" ").trimEnd(',')
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

        // Clean filler "like" before adjacent fillers consume boundary commas
        val likePatterns = listOf(
            Pattern.compile("(?:^|[\\s,])like\\s*,", Pattern.CASE_INSENSITIVE),
            Pattern.compile(",\\s*like(?:\\s*,|\\s+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:^|[.!?\\n])\\s*like\\s+(?=(?:we|I|you|they|it|he|she|this|that|what|how|why|when|where|there)\\b)", Pattern.CASE_INSENSITIVE)
        )
        for (lp in likePatterns) {
            val matcher = lp.matcher(result)
            while (matcher.find()) {
                count++
            }
            result = lp.matcher(result).replaceAll(" ")
        }

        for (filler in FILLER_WORDS) {
            val pattern = Pattern.compile("(?:,\\s*)?$filler(?:\\s*,)?", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(result)
            while (matcher.find()) {
                count++
            }
            result = pattern.matcher(result).replaceAll(" ")
        }

        // Clean leftover double commas or orphan punctuation
        result = result.replace(Regex(",\\s*,+"), ",")
        result = result.replace(Regex("^[\\s,]+"), "")
        result = result.replace(Regex("\\s+,\\s*"), ", ")

        return Pair(result, count)
    }

    private fun removeStutters(text: String): String {
        var result = text.replace(Regex("(?i)\\b(\\w+)\\s+\\1\\b"), "$1")
        result = result.replace(Regex("(?i)\\b(\\w+)\\s+\\1\\b"), "$1")
        result = result.replace(Regex("(?i)\\b[a-zA-Z]{1,2}-\\b"), "")
        return result
    }

    private fun normalizeTimeExpressions(text: String): String {
        // Formats "5 PM" or "5 pm" to "5:00 PM"
        val timeRegex = Regex("\\b(\\d{1,2})\\s*(AM|PM|am|pm)\\b")
        return timeRegex.replace(text) { match ->
            val hour = match.groupValues[1]
            val meridiem = match.groupValues[2].uppercase()
            "$hour:00 $meridiem"
        }
    }

    private fun formatByContext(text: String, mode: DictationMode): String {
        var result = text

        // Code context or auto-detected code keywords
        val isCode = mode == DictationMode.CODE ||
                (mode == DictationMode.AUTO && text.contains(Regex("\\b(function|def |class |const |val |var |import |return |SELECT |FROM |public static)\\b", RegexOption.IGNORE_CASE)))

        if (isCode) {
            return if (!result.startsWith("```")) {
                "```\n$result\n```"
            } else {
                result
            }
        }

        // List structure: bullet points or numbered lists (Rule 4)
        val isListMode = mode == DictationMode.LISTS ||
                (mode == DictationMode.AUTO && (
                    text.contains(Regex("\\b(step 1|step 2|first|second|third|finally|item 1|item 2)\\b", RegexOption.IGNORE_CASE)) ||
                    text.split(Regex("[.!?]\\s+")).size >= 3 && text.contains(Regex("\\b(buy|todo|task|check|add|fix)\\b", RegexOption.IGNORE_CASE))
                ))

        if (isListMode) {
            val lines = mutableListOf<String>()
            val numberedMatch = text.contains(Regex("\\b(first|second|third|step 1|step 2|1\\.|2\\.)\\b", RegexOption.IGNORE_CASE))
            val segments = text.split(Regex("(?<=[.!?])\\s+|\\s+(?:first|second|third|next|finally|also|then)\\s+", RegexOption.IGNORE_CASE))
            
            var index = 1
            for (seg in segments) {
                var trimmed = seg.trim().trimStart('-', '*', '•', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ')').trim()
                // Strip leading transition ordinals (First, Second, Third, Next, Finally, Also, Then, Step X)
                trimmed = trimmed.replace(Regex("(?i)^(first|second|third|fourth|fifth|next|finally|also|then|step\\s*\\d+)\\s*,?\\s*"), "")
                if (trimmed.isNotEmpty()) {
                    val capitalized = trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    if (numberedMatch) {
                        lines.add("$index. $capitalized")
                        index++
                    } else {
                        lines.add("- $capitalized")
                    }
                }
            }
            if (lines.isNotEmpty()) return lines.joinToString("\n")
        }

        // Email context (Rule 5)
        if (mode == DictationMode.EMAIL) {
            result = formatAsEmail(result)
        }

        return result
    }

    private fun formatAsEmail(text: String): String {
        var emailText = text
        // Format common email greeting
        emailText = emailText.replace(Regex("(?i)\\b(hi|hello|dear)\\s+([a-zA-Z]+)(?:\\s*,|\\s+comma)?"), "$1 $2,\n\n")
        // Format common email sign-off
        emailText = emailText.replace(Regex("(?i)\\b(best regards|warm regards|thanks|thank you|sincerely|cheers)(?:\\s*,|\\s+comma)?\\s*([a-zA-Z\\s]*)$"), "\n\n$1,\n$2")
        return emailText
    }

    private fun normalizeGrammarAndSpacing(text: String): String {
        // If code block, don't mangle spacing
        if (text.startsWith("```") && text.endsWith("```")) {
            return text
        }

        var res = text.replace(Regex("[ \\t]+"), " ")
        res = res.replace(Regex("\\s*\\n\\s*"), "\n")
        res = res.replace(Regex("\\n{3,}"), "\n\n")

        // Remove space before punctuation
        res = res.replace(Regex("\\s+([,.:;?!])"), "$1")

        // Resolve conflicting punctuation
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

        // Capitalize pronoun "I"
        res = res.replace(Regex("\\bi\\b"), "I")
        res = res.replace(Regex("\\bi'([a-z]+)\\b"), "I'$1")

        // Ensure closing punctuation if not a list or code
        res = res.trim()
        if (res.isNotEmpty() && !res.endsWithAny(listOf(".", "!", "?", "\n", "\"", "'", "-", "```"))) {
            if (!res.startsWith("-") && !res.matches(Regex("^\\d+\\..*"))) {
                res += "."
            }
        }

        return res
    }

    private fun String.endsWithAny(suffixes: List<String>): Boolean {
        return suffixes.any { this.endsWith(it) }
    }
}
