package fi.pomeranssi.bookline.ui.common

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Shared date formatters for display across the app. */
object DateFormatters {
    /**
     * User-facing date format in the current locale, e.g. "Jan 15, 2025" (en)
     * or "15.1.2025" (fi). Resolved per call because the formatter captures
     * the default locale at creation time and the app locale can change.
     */
    val displayDate: DateTimeFormatter
        get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
}
