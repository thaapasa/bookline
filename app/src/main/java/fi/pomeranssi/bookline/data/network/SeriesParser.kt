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
    private val ENTRY_REGEX = Regex("""^\s*(.+?),?\s*#(\d+(?:\.\d+)?)\s*$""")

    /**
     * Parse the series entries from a book title.
     * Returns a pair of (clean title without series suffix, list of series entries).
     * If no series info is found, the original title is returned with an empty list.
     */
    fun parseSeriesFromTitle(title: String): Pair<String, List<SeriesEntry>> {
        val match = SERIES_SUFFIX_REGEX.find(title) ?: return title to emptyList()

        val cleanTitle = title.substring(0, match.range.first).trim()
        val seriesPart = match.groupValues[1]

        val entries = seriesPart.split(";").mapNotNull { part ->
            val entryMatch = ENTRY_REGEX.matchEntire(part) ?: return@mapNotNull null
            val seriesName = entryMatch.groupValues[1].trim()
            val position = entryMatch.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
            SeriesEntry(seriesName = seriesName, position = position)
        }

        return cleanTitle to entries
    }
}
