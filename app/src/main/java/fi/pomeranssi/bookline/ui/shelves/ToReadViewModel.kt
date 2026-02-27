package fi.pomeranssi.bookline.ui.shelves

import androidx.lifecycle.ViewModel
import fi.pomeranssi.bookline.data.repository.BookRepository
import fi.pomeranssi.bookline.domain.model.Book
import kotlinx.coroutines.flow.Flow

class ToReadViewModel(
    bookRepository: BookRepository,
) : ViewModel() {

    val books: Flow<List<Book>> = bookRepository.observeToReadBooks()
}
