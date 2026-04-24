package com.starry.myne.helpers

/**
 * A class that helps in paginating data from a remote source.
 *
 * @param Page The type of the page.
 * @param BookSet The type of the data set.
 * @property initialPage The initial page to start from.
 * @property onLoadUpdated A lambda that is called when the loading state is updated.
 * @property onRequest A suspend function that is called to request data from the remote source.
 * @property getNextPage A suspend function that is called to get the next page.
 * @property onError A suspend function that is called when an error occurs.
 * @property onSuccess A suspend function that is called when the data is successfully loaded.
 */
class Paginator<Page, BookSet>(
    private val initialPage: Page,
    private val onLoadUpdated: (Boolean) -> Unit,
    private val onRequest: suspend (nextPage: Page) -> Result<BookSet>,
    private val getNextPage: suspend (BookSet) -> Page,
    private val onError: suspend (Throwable?) -> Unit,
    private val onSuccess: suspend (item: BookSet, newPage: Page) -> Unit
) {

    private var currentPage = initialPage
    private var isMakingRequest = false

    /**
     * Loads the next items from the remote source.
     */
    suspend fun loadNextItems() {
        if (isMakingRequest) {
            return
        }
        isMakingRequest = true
        onLoadUpdated(true)
        val result = onRequest(currentPage)
        isMakingRequest = false
        val bookSet = result.getOrElse {
            onError(it)
            onLoadUpdated(false)
            return
        }
        currentPage = getNextPage(bookSet)
        onSuccess(bookSet, currentPage)
        onLoadUpdated(false)
    }

    /**
     * Resets the paginator to the initial state.
     */
    fun reset() {
        currentPage = initialPage
    }
}