package fi.pomeranssi.bookline.data.network

import fi.pomeranssi.bookline.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

/**
 * Fetches the raw RSS feed from a Goodreads URL.
 *
 * Uses plain [HttpURLConnection] to avoid pulling in an HTTP library for
 * a single GET request. Can be swapped for Ktor/OkHttp later if needed.
 */
class GoodreadsFeedService {
    /**
     * Download a single page of the RSS feed and return its [InputStream].
     * Caller is responsible for closing the stream.
     *
     * @throws java.io.IOException on network errors.
     */
    suspend fun fetch(
        feedUrl: String,
        page: Int = 1,
    ): InputStream =
        withContext(Dispatchers.IO) {
            val pagedUrl = appendPage(feedUrl, page)
            val connection = URI(pagedUrl).toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw GoodreadsFeedException(
                        "Feed returned HTTP ${connection.responseCode}: ${connection.responseMessage}",
                    )
                }

                connection.inputStream
            } catch (t: Throwable) {
                // Release the socket on any error path; the success path hands the
                // stream to the caller, who is responsible for closing it.
                connection.disconnect()
                throw t
            }
        }

    companion object {
        private const val TIMEOUT_MS = 15_000

        /**
         * Amazon's Agent Terms (Conditions of Use) require automated
         * clients to self-identify with an "Agent/[name]" token in the
         * User-Agent header.
         */
        private val USER_AGENT = "Bookline/${BuildConfig.VERSION_NAME} Agent/Bookline"

        /**
         * Build the feed URL for a given page.
         * Keeps only the `key` parameter from the original URL and adds
         * `order=isbn` for stable pagination ordering.
         */
        internal fun appendPage(
            feedUrl: String,
            page: Int,
        ): String {
            val uri = URI(feedUrl)
            val key =
                uri.rawQuery
                    ?.split("&")
                    ?.firstOrNull { it.startsWith("key=") }
            val params = listOfNotNull(key, "order=isbn", "page=$page").joinToString("&")
            return URI(
                uri.scheme,
                uri.authority,
                uri.path,
                params,
                uri.fragment,
            ).toString()
        }
    }
}

class GoodreadsFeedException(
    message: String,
) : Exception(message)
