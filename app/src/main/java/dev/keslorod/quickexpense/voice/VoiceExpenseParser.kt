package dev.keslorod.quickexpense.voice

import dev.keslorod.quickexpense.ui.quickinput.Option
import java.util.Locale

enum class VoiceParseConfidence {
    /** Amount found and every other word in the phrase was accounted for (matched a
     * category/merchant/source, or was a recognized filler like a currency name) — safe to
     * offer auto-save. */
    HIGH,
    /** Amount found, but at least one word wasn't explained (unmatched, or ambiguous between
     * several candidates) — fields get filled with whatever was understood, but nothing is
     * saved automatically. */
    PARTIAL,
    /** No amount found at all. */
    NONE
}

data class VoiceParseResult(
    val amountCents: Long?,
    val category: Option?,
    val merchant: Option?,
    val source: Option?,
    val confidence: VoiceParseConfidence,
    /** Runs of adjacent words nothing matched, each joined back into one phrase — the
     * recognizer splits even a single foreign brand/compound name into separate space-separated
     * tokens (e.g. "уради сам", "вуди хаус"), so grouping by adjacency keeps those as one
     * candidate instead of offering their pieces as unrelated leftovers. Surfaced so the caller
     * can offer "use as merchant/category/source?" for each phrase instead of discarding it. */
    val leftoverPhrases: List<String>
)

/**
 * Turns one free-form spoken sentence like "двести пятьдесят кофе Лидл" into an amount plus
 * best-guess category/merchant/source, by pulling the first number out and matching whatever
 * words are left against known names. Never invents anything: a word that matches more than one
 * candidate is left unmatched rather than guessed at, and a match only needs the candidate's name
 * to appear anywhere in the transcript — not a whole-phrase match. Word order doesn't matter:
 * the amount and every name are searched for across the whole phrase, not by position.
 */
object VoiceExpenseParser {

    // Words that don't name anything and shouldn't count against "did we understand everything
    // you said" — currency names across the app's three languages, plus a few connectors people
    // naturally say around an amount ("двести за кофе", "two fifty for coffee").
    private val fillerWords = setOf(
        // ru
        "рубль", "рубля", "рублей", "руб", "динар", "динара", "динаров", "евро", "доллар",
        "доллара", "долларов", "за", "на", "в", "и",
        // sr
        "dinar", "dinara", "din", "evro", "evra", "za", "u", "i",
        // en
        "dollar", "dollars", "euro", "euros", "for", "at", "in", "and"
    )

    private val numberRegex = Regex("""\d+(?:[.,]\d+)?""")
    private val punctuationRegex = Regex("""[.,!?;:]""")

    fun parse(
        transcript: String,
        categories: List<Option>,
        merchants: List<Option>,
        sources: List<Option> = emptyList()
    ): VoiceParseResult {
        val normalized = punctuationRegex.replace(transcript.trim().lowercase(Locale.getDefault()), "")
        if (normalized.isBlank()) return VoiceParseResult(null, null, null, null, VoiceParseConfidence.NONE, emptyList())

        val numberMatch = numberRegex.find(normalized)
        val amountCents = numberMatch?.value?.replace(',', '.')?.let(::toCents)

        // Drop the matched number before word-matching so a merchant name that happens to
        // contain digits (e.g. a "5ka"-style chain) isn't eaten by the number extraction.
        val textWithoutNumber = numberMatch?.let { normalized.removeRange(it.range) } ?: normalized
        val words = textWithoutNumber.split(Regex("\\s+")).filter { it.isNotBlank() }

        val (matchedCategory, categoryWords) = findBestMatch(textWithoutNumber, categories)
        val (matchedMerchant, merchantWords) = findBestMatch(textWithoutNumber, merchants)
        val (matchedSource, sourceWords) = findBestMatch(textWithoutNumber, sources)

        val consumedWords = categoryWords + merchantWords + sourceWords
        val leftoverPhrases = groupIntoPhrases(words, consumedWords)

        val confidence = when {
            amountCents == null || amountCents <= 0L -> VoiceParseConfidence.NONE
            leftoverPhrases.isEmpty() -> VoiceParseConfidence.HIGH
            else -> VoiceParseConfidence.PARTIAL
        }

        return VoiceParseResult(amountCents, matchedCategory, matchedMerchant, matchedSource, confidence, leftoverPhrases)
    }

    /**
     * Parses every alternative the recognizer offered and keeps whichever produced the most
     * confident result — a lower-ranked alternative sometimes spells a brand name the way it's
     * actually stored even when the recognizer's top guess mangles it. Ties (including "none of
     * them matched anything") fall back to the recognizer's own ranking: the first alternative.
     */
    fun parseBest(
        transcripts: List<String>,
        categories: List<Option>,
        merchants: List<Option>,
        sources: List<Option> = emptyList()
    ): VoiceParseResult? {
        if (transcripts.isEmpty()) return null
        return transcripts
            .map { parse(it, categories, merchants, sources) }
            .maxByOrNull { it.score() }
    }

    private fun VoiceParseResult.score(): Int = when {
        confidence == VoiceParseConfidence.HIGH -> 3
        confidence == VoiceParseConfidence.PARTIAL && (category != null || merchant != null || source != null) -> 2
        (amountCents ?: 0L) > 0L -> 1
        else -> 0
    }

    private fun toCents(numStr: String): Long? {
        val parts = numStr.split('.', limit = 2)
        val major = parts[0].toLongOrNull() ?: return null
        val minor = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2).toLongOrNull() ?: 0L
        return major * 100 + minor
    }

    /**
     * The single unambiguous candidate whose name appears in [text], plus the words of its name
     * (for marking them "explained") — or null and no words if nothing matches, or more than one
     * candidate does. An ambiguous mention is left for the user to resolve by hand rather than
     * guessed at.
     */
    private fun findBestMatch(text: String, candidates: List<Option>): Pair<Option?, Set<String>> {
        val matches = candidates.filter { candidate ->
            candidate.label.isNotBlank() && text.contains(candidate.label.lowercase(Locale.getDefault()))
        }
        val best = matches.singleOrNull() ?: return null to emptySet()
        val nameWords = best.label.lowercase(Locale.getDefault()).split(Regex("\\s+")).toSet()
        return best to nameWords
    }

    /**
     * Walks [words] in order and joins each run of consecutive words that are neither
     * [consumedWords] nor filler back into a single space-joined phrase — turning e.g.
     * ["уради", "сам"] into ["уради сам"] instead of two unrelated leftovers, as long as they
     * sit next to each other in the transcript.
     */
    private fun groupIntoPhrases(words: List<String>, consumedWords: Set<String>): List<String> {
        val phrases = mutableListOf<String>()
        var currentRun = mutableListOf<String>()
        for (word in words) {
            if (word in consumedWords || word in fillerWords) {
                if (currentRun.isNotEmpty()) {
                    phrases += currentRun.joinToString(" ")
                    currentRun = mutableListOf()
                }
            } else {
                currentRun += word
            }
        }
        if (currentRun.isNotEmpty()) phrases += currentRun.joinToString(" ")
        return phrases
    }
}
