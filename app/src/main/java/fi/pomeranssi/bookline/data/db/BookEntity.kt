package fi.pomeranssi.bookline.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import fi.pomeranssi.bookline.domain.model.Book
import java.time.LocalDate

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val bookId: String,
    val title: String,
    val authorName: String,
    val isbn: String?,
    val numPages: Int?,
    val bookPublishedYear: Int?,
    val bookDescription: String?,
    val imageUrl: String?,
    val smallImageUrl: String?,
    val mediumImageUrl: String?,
    val largeImageUrl: String?,
    val userRating: Int,
    val averageRating: Double?,
    val userReadAt: Long?,
    val userDateAdded: Long?,
    val userDateCreated: Long?,
    val userShelves: String,
    val userReview: String?,
    val goodreadsUrl: String?,
    val lastSyncedMs: Long = 0,
) {
    fun toDomain(): Book = Book(
        bookId = bookId,
        title = title,
        authorName = authorName,
        isbn = isbn,
        numPages = numPages,
        bookPublishedYear = bookPublishedYear,
        bookDescription = bookDescription,
        imageUrl = imageUrl,
        smallImageUrl = smallImageUrl,
        mediumImageUrl = mediumImageUrl,
        largeImageUrl = largeImageUrl,
        userRating = userRating,
        averageRating = averageRating,
        userReadAt = userReadAt?.let { LocalDate.ofEpochDay(it) },
        userDateAdded = userDateAdded?.let { LocalDate.ofEpochDay(it) },
        userDateCreated = userDateCreated?.let { LocalDate.ofEpochDay(it) },
        userShelves = userShelves.split("|").filter { it.isNotEmpty() },
        userReview = userReview,
        goodreadsUrl = goodreadsUrl,
    )

    companion object {
        fun fromDomain(book: Book, lastSyncedMs: Long = 0): BookEntity = BookEntity(
            bookId = book.bookId,
            title = book.title,
            authorName = book.authorName,
            isbn = book.isbn,
            numPages = book.numPages,
            bookPublishedYear = book.bookPublishedYear,
            bookDescription = book.bookDescription,
            imageUrl = book.imageUrl,
            smallImageUrl = book.smallImageUrl,
            mediumImageUrl = book.mediumImageUrl,
            largeImageUrl = book.largeImageUrl,
            userRating = book.userRating,
            averageRating = book.averageRating,
            userReadAt = book.userReadAt?.toEpochDay(),
            userDateAdded = book.userDateAdded?.toEpochDay(),
            userDateCreated = book.userDateCreated?.toEpochDay(),
            userShelves = if (book.userShelves.isEmpty()) "" else "|${book.userShelves.joinToString("|")}|",
            userReview = book.userReview,
            goodreadsUrl = book.goodreadsUrl,
            lastSyncedMs = lastSyncedMs,
        )
    }
}
