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
 */
object SeriesParser {

    private val SERIES_SUFFIX_REGEX = Regex("""\s*\(([^)]*#[^)]*)\)\s*$""")

    /**
     * Matches individual series entries like "The Dresden Files, #14" or "Mountain Man #1".
     * Uses #N as the anchor and captures everything before it as the series name,
     * stopping at semicolons or previous #N entries.
     */
    private val ENTRY_REGEX = Regex("""([\w][^#;]*?),?\s*#(\d+(?:\.\d+)?)""")

    private val WHITESPACE_REGEX = Regex("""\s+""")

    /**
     * Parse the series entries from a book title.
     * Returns a pair of (clean title without series suffix, list of series entries).
     * If no series info is found, the original title is returned with an empty list.
     */
    fun parseSeriesFromTitle(title: String): Pair<String, List<SeriesEntry>> {
        val match = SERIES_SUFFIX_REGEX.find(title) ?: return title to emptyList()

        val cleanTitle = title.substring(0, match.range.first).trim()
        val seriesPart = match.groupValues[1]

        val entries = ENTRY_REGEX.findAll(seriesPart).map { entryMatch ->
            val seriesName = entryMatch.groupValues[1].trim().trimStart(',').trim()
                .replace(WHITESPACE_REGEX, " ")
            val position = entryMatch.groupValues[2].toDoubleOrNull()
            if (seriesName.isNotEmpty() && position != null) {
                SeriesEntry(seriesName = seriesName, position = position)
            } else {
                null
            }
        }.filterNotNull().toList()

        return cleanTitle to entries
    }
}
