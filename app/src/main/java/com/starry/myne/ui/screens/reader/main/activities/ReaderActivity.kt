package com.starry.myne.ui.screens.reader.main.activities

import android.content.ContentResolver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.starry.myne.R
import com.starry.myne.helpers.Constants
import com.starry.myne.helpers.toToast
import com.starry.myne.ui.screens.reader.main.composables.ChaptersContent
import com.starry.myne.ui.screens.reader.main.composables.ReaderScreen
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderViewModel
import com.starry.myne.ui.screens.settings.viewmodels.SettingsViewModel
import com.starry.myne.ui.screens.settings.viewmodels.ThemeMode
import custom.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.FileInputStream

@AndroidEntryPoint
class ReaderActivity : AppCompatActivity() {

    private lateinit var settingsViewModel: SettingsViewModel
    private val viewModel: ReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowInsets()

        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            val themeState = settingsViewModel.theme.observeAsState(initial = ThemeMode.Auto)
            val darkTheme = when (themeState.value) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                else -> isSystemInDarkTheme()
            }

            AppTheme(isDarkTheme = darkTheme) {
                val lazyListState = rememberLazyListState()
                val scope = rememberCoroutineScope()

                // Load book (internal/external) + initial scroll positioning.
                val intentData = remember {
                    handleIntent(
                        intent = intent,
                        viewModel = viewModel,
                        contentResolver = contentResolver,
                        scrollToPosition = { index, offset ->
                            scope.launch { lazyListState.scrollToItem(index, offset) }
                        },
                        onError = {
                            getString(R.string.error).toToast(this)
                            finish()
                        }
                    )
                }

                val state = viewModel.state.collectAsState().value
                val ttsUi = viewModel.ttsState.collectAsState().value
                val pendingFocus = viewModel.pendingFocus.collectAsState().value
                // --- Persist reading progress while scrolling ---
                LaunchedEffect(lazyListState, intentData.isExternalFile) {
                    snapshotFlow { lazyListState.firstVisibleItemScrollOffset }.collect { offset ->
                        val idx = lazyListState.firstVisibleItemIndex
                        viewModel.setVisibleChapterIndex(idx)
                        viewModel.setChapterScrollPercent(calculateChapterPercentage(lazyListState))

                        if (!intentData.isExternalFile && intentData.libraryItemId != null) {
                            viewModel.updateReaderProgress(
                                libraryItemId = intentData.libraryItemId,
                                chapterIndex = idx,
                                chapterOffset = offset
                            )
                        }
                    }
                }

                // --- Bind TTS on visible chapter change & try a once-per-chapter auto-resume ---
                // We remember which chapter we already tried auto-resume for to avoid repeating it.
                val lastAutoResumeTriedFor = remember { mutableSetOf<Int>() }
                LaunchedEffect(state.currentChapterIndex, state.chapters) {
                    // Bind current chapter (idempotent)
                    viewModel.ttsBindCurrentChapter()

                    // Auto-resume only once per chapter (and only if user didn’t press Stop)
                    val idx = state.currentChapterIndex
                    if (idx !in lastAutoResumeTriedFor) {
                        lastAutoResumeTriedFor += idx
                        // Only attempt if not already playing and there’s no current locator
                        val ui = viewModel.ttsState.value
                        if (!ui.isPlaying && ui.currentLocator == null) {
                            viewModel.ttsMaybeAutoResume()
                        }
                    }
                }

                // --- Follow TTS when it jumps chapters (ex: Next at end of chapter) ---
                LaunchedEffect(Unit) {
                    viewModel.ttsState.collect { ui ->
                        val loc = ui.currentLocator ?: return@collect
                        if (ui.autoScroll && lazyListState.firstVisibleItemIndex != loc.chapterIndex) {
                            lazyListState.scrollToItem(loc.chapterIndex)
                        }
                    }
                }

                ReaderScreen(
                    viewModel = viewModel,
                    onScrollToChapter = { index -> lazyListState.scrollToItem(index) },
                    chaptersContent = { _ ->
                        ChaptersContent(
                            state = state,
                            lazyListState = lazyListState,
                            onToggleReaderMenu = { viewModel.toggleReaderMenu() },
                            onLookup = viewModel::lookupWord,
                            // ⬇ pass TTS UI so your composable can tint the current sentence
                            ttsUi = ttsUi,
                            onHighlight = { chapterId, paragraphIndex, paragraphText, selection, color ->
                                viewModel.addHighlight(
                                    chapterId = chapterId,
                                    paragraphIndex = paragraphIndex,
                                    paragraphText = paragraphText,
                                    selectedText = selection,
                                    color = color
                                )
                            },
                            pendingFocus = pendingFocus,                       // NEW
                            onPendingFocusConsumed = viewModel::clearPendingFocus  // NEW

                        )
                    }
                )
            }
        }
    }

    // ---------- Window / System UI helpers ----------
    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.hide(WindowInsetsCompat.Type.displayCutout())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun toggleSystemBars(show: Boolean) {
        if (show) showSystemBars() else hideSystemBars()
    }

    private fun showSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.show(WindowInsetsCompat.Type.displayCutout())
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.hide(WindowInsetsCompat.Type.displayCutout())
    }
}

//@AndroidEntryPoint
//class ReaderActivity : AppCompatActivity() {
//
//    private lateinit var settingsViewModel: SettingsViewModel
//    private val viewModel: ReaderViewModel by viewModels()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setupWindowInsets()
//
//        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
//
//        setContent {
//            val themeState = settingsViewModel.theme.observeAsState(initial = ThemeMode.Auto)
//            val darkTheme = when (themeState.value) {
//                ThemeMode.Dark -> true
//                ThemeMode.Light -> false
//                else -> isSystemInDarkTheme()
//            }
//
//            AppTheme(isDarkTheme = darkTheme) {
//                val lazyListState = rememberLazyListState()
//                val scope = rememberCoroutineScope()
//
//                // Handle intent (internal book vs external .epub) and initial scroll.
//                val intentData = remember {
//                    handleIntent(
//                        intent = intent,
//                        viewModel = viewModel,
//                        contentResolver = contentResolver,
//                        scrollToPosition = { index, offset ->
//                            scope.launch { lazyListState.scrollToItem(index, offset) }
//                        },
//                        onError = {
//                            getString(R.string.error).toToast(this)
//                            finish()
//                        }
//                    )
//                }
//
//                val state = viewModel.state.collectAsState().value
//                val ttsUi = viewModel.ttsState.collectAsState().value
//                ReaderScreen(
//                    viewModel = viewModel,
//                    onScrollToChapter = { index -> lazyListState.scrollToItem(index) },
//                    chaptersContent = { _ ->
//                        // Track visible chapter + progress and persist
//                        LaunchedEffect(lazyListState) {
//                            snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
//                                .collect { visibleChapterOffset ->
//                                    val visibleChapterIdx = lazyListState.firstVisibleItemIndex
//                                    viewModel.setVisibleChapterIndex(visibleChapterIdx)
//                                    viewModel.setChapterScrollPercent(
//                                        calculateChapterPercentage(lazyListState)
//                                    )
//                                    if (!intentData.isExternalFile) {
//                                        viewModel.updateReaderProgress(
//                                            libraryItemId = intentData.libraryItemId!!,
//                                            chapterIndex = visibleChapterIdx,
//                                            chapterOffset = visibleChapterOffset
//                                        )
//                                    }
//                                }
//                        }
//                        LaunchedEffect(state.currentChapterIndex, state.chapters) {
//                            val idx = state.currentChapterIndex
//                            val ch  = state.chapters.getOrNull(idx) ?: return@LaunchedEffect
//                            val paragraphs = ch.body.split("\n\n").filter { it.isNotBlank() }
//                            viewModel.ttsBindChapter(
//                                bookId = intentData.libraryItemId,
//                                chapterId = ch.chapterId,
//                                chapterIndex = idx,
//                                paragraphs = paragraphs
//                            )
//                            viewModel.ttsResumeFromSaved()
//                        }
//
//                        LaunchedEffect(Unit) {
//                            viewModel.ttsState.collect { tts ->
//                                val loc = tts.currentLocator ?: return@collect
//                                if (tts.autoScroll && lazyListState.firstVisibleItemIndex != loc.chapterIndex) {
//                                    lazyListState.scrollToItem(loc.chapterIndex)
//                                }
//                            }
//                        }
//                        val state = viewModel.state.collectAsState().value
//                        ChaptersContent(
//                            state = state,
//                            lazyListState = lazyListState,
//                            onToggleReaderMenu = { viewModel.toggleReaderMenu() },
//                            onLookup = viewModel::lookupWord,
//                            ttsUi=ttsUi,
//                            onHighlight = { chapterId, paragraphIndex, paragraphText, selection, color ->
//                                viewModel.addHighlight(
//                                    chapterId = chapterId,
//                                    paragraphIndex = paragraphIndex,
//                                    paragraphText = paragraphText,
//                                    selectedText = selection,
//                                    color = color
//                                )
//                            }
//                        )
//                    }
//                )
//            }
//        }
//    }
//
//    // ---------- Window / System UI helpers ----------
//
//    private fun setupWindowInsets() {
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        val controller = WindowInsetsControllerCompat(window, window.decorView)
//        controller.systemBarsBehavior =
//            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//        controller.hide(WindowInsetsCompat.Type.systemBars())
//        controller.hide(WindowInsetsCompat.Type.displayCutout())
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            window.attributes.layoutInDisplayCutoutMode =
//                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
//        }
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//    }
//
//    private fun toggleSystemBars(show: Boolean) {
//        if (show) showSystemBars() else hideSystemBars()
//    }
//
//    private fun showSystemBars() {
//        val controller = WindowInsetsControllerCompat(window, window.decorView)
//        controller.show(WindowInsetsCompat.Type.systemBars())
//        controller.show(WindowInsetsCompat.Type.displayCutout())
//    }
//
//    private fun hideSystemBars() {
//        val controller = WindowInsetsControllerCompat(window, window.decorView)
//        controller.hide(WindowInsetsCompat.Type.systemBars())
//        controller.hide(WindowInsetsCompat.Type.displayCutout())
//    }
//}
//


// ---------- Intent handling / progress math (unchanged from your


object ReaderConstants {
    const val EXTRA_LIBRARY_ITEM_ID = "reader_book_id"
    const val EXTRA_CHAPTER_IDX = "reader_chapter_index"
    const val DEFAULT_NONE = -100000

}

/**
 * Data class to hold intent information for ReaderActivity.
 *
 * @param libraryItemId Library item id.
 * @param chapterIndex Chapter index.
 * @param isExternalFile Is book opened from external file.
 */
data class IntentData(
    val libraryItemId: Int?, val chapterIndex: Int?, val isExternalFile: Boolean
)

/**
 * Handle intent and load epub book from given id or external file.
 *
 * @param intent Intent to handle.
 * @param viewModel ReaderViewModel to load book.
 * @param contentResolver ContentResolver to open input stream.
 * @param scrollToPosition Function to scroll to specific position.
 * @param onError Function to handle error.
 *
 * @return IntentData object containing book id, chapter index and isExternalBook.
 */
fun handleIntent(
    intent: Intent,
    viewModel: ReaderViewModel,
    contentResolver: ContentResolver,
    scrollToPosition: (index: Int, offset: Int) -> Unit,
    onError: () -> Unit
): IntentData {
    val libraryItemId = intent.extras?.getInt(
        ReaderConstants.EXTRA_LIBRARY_ITEM_ID, ReaderConstants.DEFAULT_NONE
    )
    val chapterIndex = intent.extras?.getInt(
        ReaderConstants.EXTRA_CHAPTER_IDX, ReaderConstants.DEFAULT_NONE
    )
    val isExternalFile = intent.type == Constants.EPUB_MIME_TYPE

    // Internal book
    if (libraryItemId != null && libraryItemId != ReaderConstants.DEFAULT_NONE) {
        // Load epub book from library.
        viewModel.loadEpubBook(libraryItemId = libraryItemId, onLoaded = {
            // if there is saved progress for this book, then scroll to
            // last page at exact position were used had left.
            if (it.hasProgressSaved && chapterIndex == ReaderConstants.DEFAULT_NONE) {
                scrollToPosition(it.lastChapterIndex, it.lastChapterOffset)
            }
        })
        // if user clicked on specific chapter, then scroll to
        // that chapter directly.
        if (chapterIndex != null && chapterIndex != ReaderConstants.DEFAULT_NONE) {
            scrollToPosition(chapterIndex, 0)
        }


    } else if (isExternalFile) {
        // External book from file.
        intent.data?.let {
            contentResolver.openInputStream(it)?.let { ips ->
                viewModel.loadEpubBookExternal(ips as FileInputStream)
            }
        }
    } else {
        onError() // Invalid intent.
    }

    return IntentData(libraryItemId, chapterIndex, isExternalFile)
}

/**
 * Calculate the scroll percentage for the first visible item in a LazyColumn.
 *
 * @param lazyListState The LazyListState of the LazyColumn
 * @return The scroll percentage for the first visible item, or -1 if no item is visible
 */
fun calculateChapterPercentage(lazyListState: LazyListState): Float {
    val firstVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return -1f
    val listHeight =
        lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset

    // Calculate the scroll percentage for the first visible item
    val itemTop = firstVisibleItem.offset.toFloat()
    val itemBottom = itemTop + firstVisibleItem.size.toFloat()

    return if (itemTop >= listHeight || itemBottom <= 0f) {
        1f // Item is completely scrolled out of view
    } else {
        // Calculate the visible portion of the item
        val visiblePortion = if (itemTop < 0f) {
            itemBottom
        } else {
            listHeight - itemTop
        }
        // Calculate the scroll percentage based on the visible portion
        ((1f - visiblePortion / firstVisibleItem.size.toFloat())).coerceIn(0f, 1f)
    }
}

