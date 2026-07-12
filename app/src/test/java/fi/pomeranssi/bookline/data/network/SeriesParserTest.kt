package fi.pomeranssi.bookline.data.network

import fi.pomeranssi.bookline.domain.model.SeriesEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesParserTest {
    private fun assertParsed(
        title: String,
        expectedTitle: String,
        vararg expectedEntries: SeriesEntry,
    ) {
        val (cleanTitle, entries) = SeriesParser.parseSeriesFromTitle(title)
        assertEquals("clean title for \"$title\"", expectedTitle, cleanTitle)
        assertEquals("entries for \"$title\"", expectedEntries.toList(), entries)
    }

    private fun assertUnchanged(title: String) = assertParsed(title, title)

    private fun entry(
        name: String,
        position: Double,
    ) = SeriesEntry(seriesName = name, position = position)

    // Parenthesized Goodreads format

    @Test
    fun `paren format with comma`() =
        assertParsed(
            "Cold Days (The Dresden Files, #14)",
            "Cold Days",
            entry("The Dresden Files", 14.0),
        )

    @Test
    fun `paren format without comma`() =
        assertParsed(
            "Mountain Man (Mountain Man #1)",
            "Mountain Man",
            entry("Mountain Man", 1.0),
        )

    @Test
    fun `paren format with multiple series`() =
        assertParsed(
            "Thud! (Discworld, #34; City Watch, #7)",
            "Thud!",
            entry("Discworld", 34.0),
            entry("City Watch", 7.0),
        )

    @Test
    fun `paren format with fractional position`() =
        assertParsed(
            "The Last Wish (The Witcher, #0.5)",
            "The Last Wish",
            entry("The Witcher", 0.5),
        )

    @Test
    fun `paren format with extra whitespace`() =
        assertParsed(
            "Words of Radiance  (The  Stormlight   Archive,  #2) ",
            "Words of Radiance",
            entry("The Stormlight Archive", 2.0),
        )

    @Test
    fun `paren format with colon in title`() =
        assertParsed(
            "Mistborn: The Final Empire (Mistborn, #1)",
            "Mistborn: The Final Empire",
            entry("Mistborn", 1.0),
        )

    // Colon / audiobook format

    @Test
    fun `colon format basic`() =
        assertParsed(
            "Storm Over Camelot: Morgan le Fay, Book 3",
            "Storm Over Camelot",
            entry("Morgan le Fay", 3.0),
        )

    @Test
    fun `colon format with hash before number`() =
        assertParsed(
            "Storm Over Camelot: Morgan le Fay, Book #3",
            "Storm Over Camelot",
            entry("Morgan le Fay", 3.0),
        )

    @Test
    fun `colon format with fractional position`() =
        assertParsed(
            "Side Story: Morgan le Fay, Book 2.5",
            "Side Story",
            entry("Morgan le Fay", 2.5),
        )

    @Test
    fun `colon format with apostrophe in series name`() =
        assertParsed(
            "The Restaurant at the End of the Universe: Hitchhiker's Guide to the Galaxy, Book 2",
            "The Restaurant at the End of the Universe",
            entry("Hitchhiker's Guide to the Galaxy", 2.0),
        )

    @Test
    fun `colon format splits at last colon`() =
        assertParsed(
            "Mistborn: The Final Empire: Mistborn, Book 1",
            "Mistborn: The Final Empire",
            entry("Mistborn", 1.0),
        )

    @Test
    fun `colon format with extra whitespace`() =
        assertParsed(
            "Storm Over Camelot:  Morgan  le Fay ,  Book 3 ",
            "Storm Over Camelot",
            entry("Morgan le Fay", 3.0),
        )

    // Both formats in one title

    @Test
    fun `paren and colon format for same series deduplicates`() =
        assertParsed(
            "Storm Over Camelot: Morgan le Fay, Book 3 (Morgan le Fay, #3)",
            "Storm Over Camelot",
            entry("Morgan le Fay", 3.0),
        )

    @Test
    fun `paren and colon format for different series keeps both`() =
        assertParsed(
            "Storm Over Camelot: Morgan le Fay, Book 3 (Camelot Saga, #7)",
            "Storm Over Camelot",
            entry("Camelot Saga", 7.0),
            entry("Morgan le Fay", 3.0),
        )

    // No series info at all

    @Test
    fun `plain title unchanged`() = assertUnchanged("The Great Gatsby")

    @Test
    fun `title with number unchanged`() = assertUnchanged("Fahrenheit 451")

    @Test
    fun `numeric title unchanged`() = assertUnchanged("1984")

    @Test
    fun `title with hyphenated number unchanged`() = assertUnchanged("Catch-22")

    // Parentheses that are not series info

    @Test
    fun `paren edition note without hash unchanged`() = assertUnchanged("To Kill a Mockingbird (Harper Perennial Modern Classics)")

    @Test
    fun `paren movie tie-in unchanged`() = assertUnchanged("Room (Movie Tie-In Edition)")

    @Test
    fun `paren marketing blurb with hash but no series unchanged`() =
        assertUnchanged("I Heart My Little A-Holes (#1 New York Times Bestseller)")

    @Test
    fun `parens in middle of title unchanged`() = assertUnchanged("The (Un)popular Vote")

    @Test
    fun `paren with number but no hash unchanged`() = assertUnchanged("Q & A (2005)")

    // Colons that are not series info

    @Test
    fun `subtitle after colon unchanged`() = assertUnchanged("2001: A Space Odyssey")

    @Test
    fun `book number without series name unchanged`() = assertUnchanged("My Struggle: Book 1")

    @Test
    fun `spelled-out book number unchanged`() = assertUnchanged("Cinder: Book One of the Lunar Chronicles")

    @Test
    fun `plural books unchanged`() = assertUnchanged("1Q84: Books 1-3")

    @Test
    fun `subtitle containing word book unchanged`() = assertUnchanged("The Name of the Wind: The Kingkiller Chronicle: Day One")

    @Test
    fun `descriptive subtitle unchanged`() = assertUnchanged("Salt: A World History")

    @Test
    fun `subtitle with comma but no book number unchanged`() =
        assertUnchanged("Slaughterhouse-Five, or The Children's Crusade: A Duty-Dance with Death")

    // ", Book N" without a colon

    @Test
    fun `classic text with book number but no colon unchanged`() = assertUnchanged("Paradise Lost, Book 1")

    // Near-misses of the colon format

    @Test
    fun `lowercase book keyword unchanged`() = assertUnchanged("Storm Over Camelot: Morgan le Fay, book 3")

    @Test
    fun `text after book number unchanged`() = assertUnchanged("Storm Over Camelot: Morgan le Fay, Book 3 Extras")

    @Test
    fun `book number without digits unchanged`() = assertUnchanged("Storm Over Camelot: Morgan le Fay, Book")

    @Test
    fun `missing series name before comma unchanged`() = assertUnchanged("Storm Over Camelot: , Book 3")

    @Test
    fun `bookend word containing book unchanged`() = assertUnchanged("The Jungle Book")

    @Test
    fun `title ending in word book with number unchanged`() = assertUnchanged("How to Cook Everything: The Basics, Cookbook 2")

    // Malformed / edge inputs

    @Test
    fun `empty title unchanged`() = assertUnchanged("")

    @Test
    fun `paren hash without number unchanged`() = assertUnchanged("Weird Title (Series #)")

    @Test
    fun `unclosed paren unchanged`() = assertUnchanged("Broken Title (Series, #3")

    @Test
    fun `hash number without parens unchanged`() = assertUnchanged("Novel Series #3")
}
