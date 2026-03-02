package fi.pomeranssi.bookline.ui.common

import java.time.format.DateTimeFormatter

/** Shared date formatters for display across the app. */
object DateFormatters {
    /** User-facing date format, e.g. "Jan 15, 2025". */
    val displayDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
}
