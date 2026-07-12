package fi.pomeranssi.bookline.data.network

import fi.pomeranssi.bookline.domain.model.SeriesEntry

/**
 * Parses series information from Goodreads book titles.
 *
 * Goodreads encodes series membership in the title as a parenthesized suffix:
 * - `"Cold Days (The Dresden Files, #14)"`
 * - `"Mountain Man (Mountain Man #1)"`
 * - `"Thud! (Discworld, #34; City Watch, #7)"`
 * - `"The Last Wish (The Witcher, #0.5)"`
 *
 * Some titles (typically imported from audiobook listings) instead use a colon suffix:
 * - `"Storm Over Camelot: Morgan le Fay, Book 3"`
 * - `"The Restaurant at the End of the Universe: Hitchhiker's Guide to the Galaxy, Book #2"`
 */
object SeriesParser {
    private val SERIES_SUFFIX_REGEX = Regex("""\s*\(([^)]*#[^)]*)\)\s*$""")

    /**
     * Matches individual series entries like "The Dresden Files, #14" or "Mountain Man #1".
     * Uses #N as the anchor and captures everything before it as the series name,
     * stopping at semicolons or previous #N entries.
     */
    private val ENTRY_REGEX = Regex("""([\w][^#;]*?),?\s*#(\d+(?:\.\d+)?)""")

    /**
     * Matches colon-style suffixes like ": Morgan le Fay, Book 3" at the end of a title.
     * The title prefix is matched greedily so the split happens at the last colon,
     * allowing colons in the title itself. The series name may not contain
     * colons, semicolons, commas, parentheses, or '#' — keeping the match narrow to
     * avoid mangling ordinary titles. "Book" is case-sensitive and the number may
     * have a leading '#' and a decimal part.
     */
    private val COLON_SUFFIX_REGEX = Regex("""^(.+):\s*([^:;,()#]+),\s*Book\s+#?(\d+(?:\.\d+)?)\s*$""")

    private val WHITESPACE_REGEX = Regex("""\s+""")

    /**
     * Parse the series entries from a book title.
     * Returns a pair of (clean title without series suffix, list of series entries).
     * If no series info is found, the original title is returned with an empty list.
     */
    fun parseSeriesFromTitle(title: String): Pair<String, List<SeriesEntry>> {
        val (parenTitle, parenEntries) = parseParenthesizedSuffix(title)
        val (cleanTitle, colonEntry) = parseColonSuffix(parenTitle)

        val entries =
            if (colonEntry != null && parenEntries.none { it.seriesName.equals(colonEntry.seriesName, ignoreCase = true) }) {
                parenEntries + colonEntry
            } else {
                parenEntries
            }

        return cleanTitle to entries
    }

    private fun parseParenthesizedSuffix(title: String): Pair<String, List<SeriesEntry>> {
        val match = SERIES_SUFFIX_REGEX.find(title) ?: return title to emptyList()

        val seriesPart = match.groupValues[1]
        val entries =
            ENTRY_REGEX
                .findAll(seriesPart)
                .mapNotNull { entryMatch ->
                    val seriesName = normalizeName(entryMatch.groupValues[1].trimStart(','))
                    val position = entryMatch.groupValues[2].toDoubleOrNull()
                    if (seriesName.isNotEmpty() && position != null) {
                        SeriesEntry(seriesName = seriesName, position = position)
                    } else {
                        null
                    }
                }.toList()

        // If nothing inside the parentheses parsed as a series entry, the suffix is
        // part of the actual title (e.g. "(#1 New York Times Bestseller)") — keep it.
        if (entries.isEmpty()) return title to emptyList()

        return title.substring(0, match.range.first).trim() to entries
    }

    private fun parseColonSuffix(title: String): Pair<String, SeriesEntry?> {
        val match = COLON_SUFFIX_REGEX.find(title) ?: return title to null

        val cleanTitle = match.groupValues[1].trim()
        val seriesName = normalizeName(match.groupValues[2])
        val position = match.groupValues[3].toDoubleOrNull()
        if (cleanTitle.isEmpty() || seriesName.isEmpty() || position == null) return title to null

        return cleanTitle to SeriesEntry(seriesName = seriesName, position = position)
    }

    private fun normalizeName(name: String): String = name.trim().replace(WHITESPACE_REGEX, " ")
}
