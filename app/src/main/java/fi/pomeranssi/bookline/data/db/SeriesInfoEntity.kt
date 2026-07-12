package fi.pomeranssi.bookline.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent series metadata. Stores the user-facing display name and the
 * set of original parsed names (from book titles) that map to this series.
 *
 * [parsedNames] is pipe-delimited with leading and trailing pipes, e.g.
 * `"|Merchant Princes|The Merchant Princes|"`, so that exact lookup can use
 * `LIKE '%|name|%'`.
 */
@Entity(tableName = "series_info")
data class SeriesInfoEntity(
    @PrimaryKey val displayName: String,
    val parsedNames: String,
) {
    /** Returns the set of parsed names stored in this entity. */
    fun parsedNameSet(): Set<String> = parsedNames.split("|").filter { it.isNotBlank() }.toSet()

    companion object {
        /** Encodes a set of names into the pipe-delimited storage format. */
        fun encodeParsedNames(names: Set<String>): String = names.joinToString(separator = "|", prefix = "|", postfix = "|")

        /** Creates a new entity for a freshly-discovered series. */
        fun forNewSeries(name: String): SeriesInfoEntity = SeriesInfoEntity(displayName = name, parsedNames = "|$name|")
    }
}
