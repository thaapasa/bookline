package fi.pomeranssi.bookline.ui.detail

import androidx.lifecycle.ViewModel
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.domain.model.Book
import kotlinx.coroutines.flow.Flow

class BookDetailViewModel(
    private val bookRepository: BookRepository,
    bookId: String,
) : ViewModel() {

    val book: Flow<Book?> = bookRepository.observeBook(bookId)
}
