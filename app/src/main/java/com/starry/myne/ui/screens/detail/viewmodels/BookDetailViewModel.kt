package com.starry.myne.ui.screens.detail.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starry.myne.api.BookAPI
import com.starry.myne.api.models.Book
import com.starry.myne.api.models.BookSet
import com.starry.myne.api.models.ExtraInfo
import com.starry.myne.database.library.LibraryDao
import com.starry.myne.database.library.LibraryItem
import com.starry.myne.helpers.Constants
import com.starry.myne.helpers.PreferenceUtil
import com.starry.myne.helpers.book.BookDownloader
import com.starry.myne.helpers.book.BookUtils
import com.starry.myne.helpers.summary.models.BookSummaryResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BookDetailScreenState(
    val isLoading: Boolean = true,
    val bookSet: BookSet = BookSet(0, null, null, emptyList()),
    val extraInfo: ExtraInfo = ExtraInfo(),
    val bookLibraryItem: LibraryItem? = null,
    val bookSummary: BookSummaryResponse? = null, // 👈 NEW
    val error: String? = null
)


@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val bookAPI: BookAPI,
    val libraryDao: LibraryDao,
    val bookDownloader: BookDownloader,
    private val preferenceUtil: PreferenceUtil
) : ViewModel() {
    var state by mutableStateOf(BookDetailScreenState())
        private set

    fun getInternalReaderSetting() = preferenceUtil.getBoolean(
        PreferenceUtil.INTERNAL_READER_BOOL, true
    )

    fun getBookDetails(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // This method can be called multiple times. like if
                // request fails and user clicks on retry button.
                // So, we are setting isLoading to true before making
                // the request to show the loading indicator in the UI.
                state = state.copy(isLoading = true)
                val bookSet = bookAPI.getBookById(bookId).getOrNull()!!
                val bookTitle = bookSet.books.first().title
                val extraInfo = bookAPI.getExtraInfo(bookSet.books.first().title)
                // ✅ fetch Gemini summary
                val summaryResult = bookAPI.getBookSummaryFromGemini(bookTitle)
                val summary = summaryResult.getOrNull()

                // If API response is cached, it will not show the loading
                // indicator. So, we are adding a delay to show the loading
                // indicator. This is just for better UX.
                if (bookSet.isCached) delay(400)
                state = if (extraInfo != null) {
                    state.copy(bookSet = bookSet, extraInfo = extraInfo)
                } else {
                    state.copy(bookSet = bookSet)
                }
                state = state.copy(
                    bookLibraryItem = libraryDao.getItemByBookId(bookId.toInt()),
                    bookSummary = summary,
                    isLoading = false,
                    error = null
                )
            } catch (exc: Exception) {
                state = state.copy(
                    error = exc.localizedMessage ?: Constants.UNKNOWN_ERR,
                    isLoading = false
                )
            }
        }
    }

    fun reFetchLibraryItem(bookId: Int, onComplete: (LibraryItem) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val libraryItem = libraryDao.getItemByBookId(bookId)
            state = state.copy(bookLibraryItem = libraryItem)
            libraryItem?.let { withContext(Dispatchers.Main) { onComplete(libraryItem) } }
        }
    }

    fun downloadBook(
        book: Book, downloadProgressListener: (Float, Int) -> Unit
    ) {
        bookDownloader.downloadBook(
            book = book,
            downloadProgressListener = downloadProgressListener,
            onDownloadSuccess = { filePath ->
                insertIntoDB(book = book, filePath = filePath)
                state = state.copy(bookLibraryItem = libraryDao.getItemByBookId(book.id))
            }
        )
    }

    private fun insertIntoDB(book: Book, filePath: String) {
        val libraryItem = LibraryItem(
            bookId = book.id,
            title = book.title,
            authors = BookUtils.getAuthorsAsString(book.authors),
            filePath = filePath,
            createdAt = System.currentTimeMillis()
        )
        libraryDao.insert(libraryItem)
    }
}