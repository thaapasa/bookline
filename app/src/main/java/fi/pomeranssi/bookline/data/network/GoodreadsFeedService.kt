package fi.pomeranssi.bookline.data.network

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
    suspend fun fetch(feedUrl: String, page: Int = 1): InputStream = withContext(Dispatchers.IO) {
        val pagedUrl = appendPage(feedUrl, page)
        val connection = URI(pagedUrl).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw GoodreadsFeedException(
                "Feed returned HTTP ${connection.responseCode}: ${connection.responseMessage}",
            )
        }

        connection.inputStream
    }

    companion object {
        private const val TIMEOUT_MS = 15_000

        /**
         * Append or replace the `page` query parameter in the feed URL.
         */
        internal fun appendPage(feedUrl: String, page: Int): String {
            val uri = URI(feedUrl)
            val params = uri.rawQuery
                ?.split("&")
                ?.filter { !it.startsWith("page=") }
                ?: emptyList()
            val newParams = (params + "page=$page").joinToString("&")
            return URI(
                uri.scheme, uri.authority, uri.path, newParams, uri.fragment,
            ).toString()
        }
    }
}

class GoodreadsFeedException(message: String) : Exception(message)

