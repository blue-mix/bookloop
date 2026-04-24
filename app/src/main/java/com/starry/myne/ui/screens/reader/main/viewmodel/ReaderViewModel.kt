package com.starry.myne.ui.screens.reader.main.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starry.myne.database.library.LibraryDao
import com.starry.myne.database.progress.ProgressDao
import com.starry.myne.database.progress.ProgressData
import com.starry.myne.epub.EpubParser
import com.starry.myne.epub.models.EpubChapter
import com.starry.myne.epub.models.EpubImage
import com.starry.myne.helpers.PreferenceUtil
import com.starry.myne.helpers.dictionary.DictionaryRepository
import com.starry.myne.helpers.highlights.Highlight
import com.starry.myne.helpers.highlights.HighlightColor
import com.starry.myne.helpers.tts.TtsEngine
import com.starry.myne.helpers.tts.TtsLocator
import com.starry.myne.helpers.tts.TtsUiState
import com.starry.myne.ui.screens.reader.main.dictionary.DictionaryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import javax.inject.Inject

data class ReaderScreenState(
    val isLoading: Boolean = true,
    val shouldShowLoader: Boolean = false,
    val showReaderMenu: Boolean = false,
    val currentChapterIndex: Int = 0,
    val currentChapter: EpubChapter = EpubChapter("", "", "", ""),
    val chapterScrollPercent: Float = 0f,
    // Book data
    val title: String = "",
    val chapters: List<EpubChapter> = emptyList(),
    val images: List<EpubImage> = emptyList(),
    // Reader data
    val hasProgressSaved: Boolean = false,
    val lastChapterIndex: Int = 0,
    val lastChapterOffset: Int = 0,
    // Typography
    val fontSize: Int = 100,
    val fontFamily: ReaderFont = ReaderFont.System,
    // Highlights
    val highlightsByChapter: Map<String, List<Highlight>> = emptyMap(),
    // Dictionary
    val lastLookup: String? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val progressDao: ProgressDao,
    private val preferenceUtil: PreferenceUtil,
    private val epubParser: EpubParser,
    private val dictionaryRepo: DictionaryRepository,
    private val ttsEngine: TtsEngine
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderScreenState())
    val state: StateFlow<ReaderScreenState> = _state.asStateFlow()

    private val _dictionary = MutableStateFlow<DictionaryUiState>(DictionaryUiState.Idle)
    val dictionary = _dictionary.asStateFlow()
    private var lookupJob: Job? = null

    val ttsState: StateFlow<TtsUiState> = ttsEngine.ui

    // Track which book is loaded (null for external files)
    private var loadedBookId: Int? = null

    // ReaderViewModel.kt
    private val _pendingFocus = MutableStateFlow<Pair<Int, Int>?>(null)
    val pendingFocus = _pendingFocus.asStateFlow()

    fun requestFocus(chapterIndex: Int, paragraphIndex: Int) {
        _pendingFocus.value = chapterIndex to paragraphIndex
    }

    fun clearPendingFocus() {
        _pendingFocus.value = null
    }


    // ---------- TTS: public API wrappers ----------

    /** Call this whenever the visible chapter changes (or on initial load). */
    fun ttsBindCurrentChapter() {
        val idx = state.value.currentChapterIndex
        val ch = state.value.chapters.getOrNull(idx) ?: return
        val paragraphs = ch.body.split("\n\n").filter { it.isNotBlank() }
        ttsEngine.bindChapter(
            bookId = loadedBookId,
            chapterId = ch.chapterId,
            chapterIndex = idx,
            paragraphs = paragraphs
        )
    }

    /** Try resuming from saved locator (only if engine policy allows). */
    fun ttsMaybeAutoResume(): Boolean {
        return if (ttsEngine.shouldAutoResume()) ttsEngine.resumeFromSaved() else false
    }

    /** Start at the first speakable sentence of the current chapter. */
    fun ttsStartFromChapterBeginning() {
        val idx = state.value.currentChapterIndex
        val ch = state.value.chapters.getOrNull(idx) ?: return
        val paragraphs = ch.body.split("\n\n").filter { it.isNotBlank() }

        // Ensure bound (idempotent)
        ttsEngine.bindChapter(loadedBookId, ch.chapterId, idx, paragraphs)

        // Find first non-empty paragraph and kick off reading by selecting start=0
        val firstSpeakable = paragraphs.indexOfFirst { p -> p.any { it.isLetterOrDigit() } }
        if (firstSpeakable >= 0) {
            ttsEngine.readSelection(firstSpeakable, 0, 0)
        }
    }

    fun ttsResumeFromSaved(): Boolean = ttsEngine.resumeFromSaved()
    fun ttsSpeakFrom(locator: TtsLocator) = ttsEngine.speakFrom(locator)
    fun ttsReadSelection(paragraphIndex: Int, start: Int, end: Int) =
        ttsEngine.readSelection(paragraphIndex, start, end)

    fun ttsPlayPauseToggle() {
        val ui = ttsState.value
        if (ui.isPlaying) {
            ttsEngine.pause()
        } else {
            when {
                ui.currentLocator != null -> ttsEngine.speakFrom(ui.currentLocator!!)
                ttsEngine.shouldAutoResume() && ttsEngine.resumeFromSaved() -> Unit
                else -> ttsStartFromChapterBeginning()
            }
        }
    }

    fun ttsStop() = ttsEngine.stop()
    fun ttsNext() = ttsEngine.nextSentence()
    fun ttsPrev() = ttsEngine.previousSentence()
    fun ttsSetSpeed(v: Float) = ttsEngine.setSpeed(v)
    fun ttsSetPitch(v: Float) = ttsEngine.setPitch(v)
    fun ttsSetAutoScroll(enabled: Boolean) = ttsEngine.setAutoScroll(enabled)
    fun ttsSpeakText(text: String) = ttsEngine.speakText(text)
    fun ttsShouldAutoResume(): Boolean = ttsEngine.shouldAutoResume()

    override fun onCleared() {
        // Don’t fully shutdown the singleton engine; just pause so we keep locator & prefs.
        ttsEngine.pause()
        super.onCleared()
    }

    // ---------- Init / fonts ----------

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                fontSize = preferenceUtil.getInt(PreferenceUtil.READER_FONT_SIZE_INT, 100),
                fontFamily = ReaderFont.getFontById(
                    preferenceUtil.getString(
                        PreferenceUtil.READER_FONT_STYLE_STR,
                        ReaderFont.System.id
                    )!!
                )
            )
            // Keep currentChapter in sync
            _state.collect {
                if (_state.value.isLoading) return@collect
                if (_state.value.chapters.isEmpty()) return@collect
                _state.value = _state.value.copy(
                    currentChapter = _state.value.chapters[_state.value.currentChapterIndex]
                )
            }
        }
    }

    // ---------- Dictionary ----------

    fun lookupWord(word: String) {
        if (word.isBlank()) return
        lookupJob?.cancel()
        _dictionary.value = DictionaryUiState.Loading
        viewModelScope.launch {
            dictionaryRepo.lookup(word)
                .onSuccess {
                    _dictionary.value = DictionaryUiState.Success(it)
                    _state.value = _state.value.copy(lastLookup = word)
                }
                .onFailure {
                    _dictionary.value = DictionaryUiState.Error(it.message ?: "Lookup failed")
                }
        }
    }

    fun clearDictionary() {
        _dictionary.value = DictionaryUiState.Idle
    }

    // ---------- Highlights (unchanged) ----------

    private var currentHighlightKey: String = ""

    private fun setHighlightKeyForInternal(libraryItemId: Int) {
        currentHighlightKey = "book_$libraryItemId"
    }

    private fun setHighlightKeyForExternal(title: String) {
        currentHighlightKey = "external_${title.hashCode()}"
    }

    // ---- Change highlight color ----
    fun changeHighlightColor(chapterId: String, highlight: Highlight, newColor: HighlightColor) {
        val cur = _state.value.highlightsByChapter.toMutableMap()
        val list = (cur[chapterId]?.toMutableList()) ?: return

        val idx = list.indexOfFirst {
            it.paragraphIndex == highlight.paragraphIndex &&
                    it.start == highlight.start && it.end == highlight.end
        }
        if (idx >= 0) {
            list[idx] = list[idx].copy(color = newColor)
            cur[chapterId] = list
            _state.value = _state.value.copy(highlightsByChapter = cur)
            persistHighlights(cur)
        }
    }

    fun addHighlight(
        chapterId: String,
        paragraphIndex: Int,
        paragraphText: String,
        selectedText: String,
        color: HighlightColor
    ) {
        val start = paragraphText.indexOf(selectedText)
        if (start < 0) return
        val end = start + selectedText.length
        val h = Highlight(
            chapterId,
            paragraphIndex = paragraphIndex,
            start = start,
            end = end,
            color = color
        )

        val cur = _state.value.highlightsByChapter.toMutableMap()
        val list = (cur[chapterId]?.toMutableList() ?: mutableListOf()).apply { add(h) }
        cur[chapterId] = list
        _state.value = _state.value.copy(highlightsByChapter = cur)

        persistHighlights(cur)
    }

    fun removeHighlight(chapterId: String, highlight: Highlight) {
        val cur = _state.value.highlightsByChapter.toMutableMap()
        val list = (cur[chapterId]?.toMutableList() ?: mutableListOf()).apply { remove(highlight) }
        cur[chapterId] = list
        _state.value = _state.value.copy(highlightsByChapter = cur)
        persistHighlights(cur)
    }

    private fun persistHighlights(map: Map<String, List<Highlight>>) {
        if (currentHighlightKey.isEmpty()) return
        val arr = JSONArray()
        map.forEach { (chapterId, list) ->
            list.forEach { h ->
                val o = JSONObject()
                o.put("chapterId", chapterId)
                o.put("paragraphIndex", h.paragraphIndex)
                o.put("start", h.start)
                o.put("end", h.end)
                o.put("color", h.color.name)
                arr.put(o)
            }
        }
        preferenceUtil.putString("hl_$currentHighlightKey", arr.toString())
    }

    private fun loadHighlights(): Map<String, List<Highlight>> {
        if (currentHighlightKey.isEmpty()) return emptyMap()
        val json = preferenceUtil.getString("hl_$currentHighlightKey", null) ?: return emptyMap()
        val arr = JSONArray(json)
        val out = mutableMapOf<String, MutableList<Highlight>>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val chapterId = o.getString("chapterId")
            val h = Highlight(
                chapterId = chapterId,
                paragraphIndex = o.getInt("paragraphIndex"),
                start = o.getInt("start"),
                end = o.getInt("end"),
                color = HighlightColor.valueOf(o.getString("color"))
            )
            out.getOrPut(chapterId) { mutableListOf() }.add(h)
        }
        return out
    }

    // ---------- Book loading / progress ----------

    fun loadEpubBook(libraryItemId: Int, onLoaded: (ReaderScreenState) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val libraryItem = libraryDao.getItemById(libraryItemId)
            loadedBookId = libraryItemId                           // set for TTS
            setHighlightKeyForInternal(libraryItemId)
            val loadedHighlights = loadHighlights()

            _state.value = _state.value.copy(
                shouldShowLoader = !epubParser.isBookCached(libraryItem!!.filePath)
            )

            val readerData = progressDao.getReaderData(libraryItemId)
            var shouldUseToc = !libraryItem.isExternalBook
            if (shouldUseToc && epubParser.peekLanguage(libraryItem.filePath) == "zh") {
                Log.d("ReaderDetailViewModel", "Parsing book without toc for Chinese book.")
                shouldUseToc = false
            }
            val epubBook = epubParser.createEpubBook(libraryItem.filePath, shouldUseToc)

            _state.value = _state.value.copy(
                title = libraryItem.title,
                chapters = epubBook.chapters,
                images = epubBook.images,
                hasProgressSaved = readerData != null,
                lastChapterIndex = readerData?.lastChapterIndex ?: 0,
                lastChapterOffset = readerData?.lastChapterOffset ?: 0,
                currentChapterIndex = readerData?.lastChapterIndex ?: 0,
                highlightsByChapter = loadedHighlights
            )
            onLoaded(state.value)
            if (state.value.shouldShowLoader) delay(200L)
            _state.value = _state.value.copy(isLoading = false, shouldShowLoader = false)
        }
    }

    fun loadEpubBookExternal(fileStream: FileInputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            fileStream.use { fis ->
                val epubBook = epubParser.createEpubBook(fis, shouldUseToc = false)
                loadedBookId = null                                 // external
                setHighlightKeyForExternal(epubBook.title)
                val loadedHighlights = loadHighlights()

                _state.value = _state.value.copy(
                    title = epubBook.title,
                    chapters = epubBook.chapters,
                    images = epubBook.images,
                    hasProgressSaved = false,
                    lastChapterIndex = 0,
                    lastChapterOffset = 0,
                    currentChapterIndex = 0,
                    highlightsByChapter = loadedHighlights
                )
                delay(200L)
                _state.value = _state.value.copy(isLoading = false, shouldShowLoader = false)
            }
        }
    }

    fun updateReaderProgress(libraryItemId: Int, chapterIndex: Int, chapterOffset: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val isLastChapter = chapterIndex == state.value.chapters.size - 1
            val hasSavedProgress = state.value.hasProgressSaved
            val progressData = progressDao.getReaderData(libraryItemId)

            when {
                hasSavedProgress && !isLastChapter -> {
                    progressData?.let {
                        val updatedProgress = it.copy(
                            lastChapterIndex = chapterIndex,
                            lastChapterOffset = chapterOffset,
                            lastReadTime = System.currentTimeMillis()
                        )
                        updatedProgress.id = it.id
                        progressDao.update(updatedProgress)
                    }
                }

                isLastChapter -> {
                    progressData?.let { progressDao.delete(it.libraryItemId) }
                }

                else -> {
                    progressDao.insert(
                        ProgressData(
                            libraryItemId = libraryItemId,
                            lastChapterIndex = chapterIndex,
                            lastChapterOffset = chapterOffset,
                            lastReadTime = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    // ---------- UI helpers ----------

    fun setChapterScrollPercent(percent: Float) {
        _state.value = _state.value.copy(chapterScrollPercent = percent)
    }

    fun setVisibleChapterIndex(index: Int) {
        _state.value = _state.value.copy(currentChapterIndex = index)
    }

    fun toggleReaderMenu() {
        _state.value = _state.value.copy(showReaderMenu = !state.value.showReaderMenu)
    }

    fun hideReaderInfo() {
        _state.value = _state.value.copy(showReaderMenu = false)
    }

    fun setFontFamily(font: ReaderFont) {
        preferenceUtil.putString(PreferenceUtil.READER_FONT_STYLE_STR, font.id)
        _state.value = _state.value.copy(fontFamily = font)
    }

    fun setFontSize(newValue: Int) {
        preferenceUtil.putInt(PreferenceUtil.READER_FONT_SIZE_INT, newValue)
        _state.value = _state.value.copy(fontSize = newValue)
    }
}

//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.starry.myne.database.library.LibraryDao
//import com.starry.myne.database.progress.ProgressDao
//import com.starry.myne.database.progress.ProgressData
//import com.starry.myne.epub.EpubParser
//import com.starry.myne.epub.models.EpubChapter
//import com.starry.myne.epub.models.EpubImage
//import com.starry.myne.helpers.PreferenceUtil
//import com.starry.myne.helpers.dictionary.DictionaryRepository
//import com.starry.myne.helpers.highlights.Highlight
//import com.starry.myne.helpers.highlights.HighlightColor
//import com.starry.myne.helpers.tts.TtsEngine
//import com.starry.myne.helpers.tts.TtsLocator
//import com.starry.myne.helpers.tts.TtsUiState
//import com.starry.myne.ui.screens.reader.main.dictionary.DictionaryUiState
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import org.json.JSONArray
//import org.json.JSONObject
//import java.io.FileInputStream
//import javax.inject.Inject
//
//
//data class ReaderScreenState(
//    // Screen state
//    val isLoading: Boolean = true,
//    val shouldShowLoader: Boolean = false,
//    val showReaderMenu: Boolean = false,
//    val currentChapterIndex: Int = 0,
//    val currentChapter: EpubChapter = EpubChapter("", "", "", ""),
//    val chapterScrollPercent: Float = 0f,
//    // Book data
//    val title: String = "",
//    val chapters: List<EpubChapter> = emptyList(),
//    val images: List<EpubImage> = emptyList(),
//    // Reader data
//    val hasProgressSaved: Boolean = false,
//    val lastChapterIndex: Int = 0,
//    val lastChapterOffset: Int = 0,
//    // Typography
//    val fontSize: Int = 100,
//    val fontFamily: ReaderFont = ReaderFont.System,
//    // in ReaderScreenState
//    val highlightsByChapter: Map<String, List<Highlight>> = emptyMap(),
//// + optionally keep the last looked up word
//    val lastLookup: String? = null
//
//)
//
//@HiltViewModel
//class ReaderViewModel @Inject constructor(
//    private val libraryDao: LibraryDao,
//    private val progressDao: ProgressDao,
//    private val preferenceUtil: PreferenceUtil,
//    private val epubParser: EpubParser,
//    private val dictionaryRepo: DictionaryRepository,
//    private val ttsEngine: TtsEngine
//) : ViewModel() {
//
//    // Mutable state flow to update the state.
//    private val _state = MutableStateFlow(ReaderScreenState())
//    val state = _state.asStateFlow()
//
//    private val _dictionary = MutableStateFlow<DictionaryUiState>(DictionaryUiState.Idle)
//    val dictionary = _dictionary.asStateFlow()
//    private var lookupJob: Job? = null
//
//    val ttsState: StateFlow<TtsUiState> = ttsEngine.ui
//
//
//    fun ttsBindChapter(bookId: Int?, chapterId: String, chapterIndex: Int, paragraphs: List<String>) {
//        ttsEngine.bindChapter(bookId, chapterId, chapterIndex, paragraphs)
//    }
//
//    fun ttsResumeFromSaved(): Boolean = ttsEngine.resumeFromSaved()
//    fun ttsSpeakFrom(locator: TtsLocator) = ttsEngine.speakFrom(locator)
//    fun ttsReadSelection(paragraphIndex: Int, start: Int, end: Int) = ttsEngine.readSelection(paragraphIndex, start, end)
//
//    fun ttsPlayPauseToggle() {
//        if (ttsState.value.isPlaying) ttsEngine.pause() else {
//            // if we have a locator, resume; else try saved
//            val cur = ttsState.value.currentLocator
//            if (cur != null) ttsEngine.speakFrom(cur) else ttsEngine.resumeFromSaved()
//        }
//    }
//    fun ttsStop() = ttsEngine.stop()
//    fun ttsNext() = ttsEngine.nextSentence()
//    fun ttsPrev() = ttsEngine.previousSentence()
//
//    fun ttsSetSpeed(v: Float) = ttsEngine.setSpeed(v)
//    fun ttsSetPitch(v: Float) = ttsEngine.setPitch(v)
//    fun ttsSetAutoScroll(enabled: Boolean) = ttsEngine.setAutoScroll(enabled)
//    fun ttsSpeakText(text: String) = ttsEngine.speakText(text) // one-liner wrapper
//
//    override fun onCleared() {
//        ttsEngine.pause()
//        super.onCleared()
//    }
//    init {
//        viewModelScope.launch {
//            _state.value = _state.value.copy(
//                fontSize = preferenceUtil.getInt(PreferenceUtil.READER_FONT_SIZE_INT, 100)
//            )
//            _state.value = _state.value.copy(
//                fontFamily = ReaderFont.getFontById(
//                    preferenceUtil.getString(
//                        PreferenceUtil.READER_FONT_STYLE_STR,
//                        ReaderFont.System.id
//                    )!!
//                )
//            )
//            // Collect the state to update the current chapter.
//            _state.collect {
//                if (state.value.isLoading) return@collect
//                if (state.value.chapters.isEmpty()) return@collect
//                _state.value = _state.value.copy(
//                    currentChapter = _state.value.chapters[_state.value.currentChapterIndex]
//                )
//            }
//        }
//    }
//
//    fun lookupWord(word: String) {
//        if (word.isBlank()) return
//        lookupJob?.cancel()
//        _dictionary.value = DictionaryUiState.Loading
//        viewModelScope.launch {
//            dictionaryRepo.lookup(word)
//                .onSuccess {
//                    _dictionary.value = DictionaryUiState.Success(it)
//                    _state.value = _state.value.copy(lastLookup = word)
//                }
//                .onFailure {
//                    _dictionary.value = DictionaryUiState.Error(it.message ?: "Lookup failed")
//                }
//        }
//    }
//
//    fun clearDictionary() {
//        _dictionary.value = DictionaryUiState.Idle
//    }
//    // Keep a stable key for this book (internal: libraryItemId; external: title fallback)
//    private var currentHighlightKey: String = ""
//
//    // set this when loading a book
//    private fun setHighlightKeyForInternal(libraryItemId: Int) {
//        currentHighlightKey = "book_$libraryItemId"
//    }
//    private fun setHighlightKeyForExternal(title: String) {
//        currentHighlightKey = "external_${title.hashCode()}" // fallback; improve with file hash if available
//    }
//    fun addHighlight(
//        chapterId: String,
//        paragraphIndex: Int,
//        paragraphText: String,
//        selectedText: String,
//        color: HighlightColor
//    ) {
//        val start = paragraphText.indexOf(selectedText)
//        if (start < 0) return
//        val end = start + selectedText.length
//        val h = Highlight(chapterId,paragraphIndex = paragraphIndex, start = start, end = end, color = color)
//
//        val cur = _state.value.highlightsByChapter.toMutableMap()
//        val list = (cur[chapterId]?.toMutableList() ?: mutableListOf()).apply { add(h) }
//        cur[chapterId] = list
//        _state.value = _state.value.copy(highlightsByChapter = cur)
//
//        persistHighlights(cur)
//    }
//
//    fun removeHighlight(chapterId: String, highlight: Highlight) {
//        val cur = _state.value.highlightsByChapter.toMutableMap()
//        val list = (cur[chapterId]?.toMutableList() ?: mutableListOf()).apply { remove(highlight) }
//        cur[chapterId] = list
//        _state.value = _state.value.copy(highlightsByChapter = cur)
//        persistHighlights(cur)
//    }
//
//    private fun persistHighlights(map: Map<String, List<Highlight>>) {
//        if (currentHighlightKey.isEmpty()) return
//        val arr = JSONArray()
//        map.forEach { (chapterId, list) ->
//            list.forEach { h ->
//                val o = JSONObject()
//                o.put("chapterId", chapterId)
//                o.put("paragraphIndex", h.paragraphIndex)
//                o.put("start", h.start)
//                o.put("end", h.end)
//                o.put("color", h.color.name)
//                arr.put(o)
//            }
//        }
//        preferenceUtil.putString("hl_$currentHighlightKey", arr.toString())
//    }
//
//    private fun loadHighlights(): Map<String, List<Highlight>> {
//        if (currentHighlightKey.isEmpty()) return emptyMap()
//        val json = preferenceUtil.getString("hl_$currentHighlightKey", null) ?: return emptyMap()
//        val arr = JSONArray(json)
//        val out = mutableMapOf<String, MutableList<Highlight>>()
//        for (i in 0 until arr.length()) {
//            val o = arr.getJSONObject(i)
//            val chapterId = o.getString("chapterId")
//            val h = Highlight(chapterId= chapterId,
//                paragraphIndex = o.getInt("paragraphIndex"),
//                start = o.getInt("start"),
//                end = o.getInt("end"),
//                color = HighlightColor.valueOf(o.getString("color"))
//            )
//            out.getOrPut(chapterId) { mutableListOf() }.add(h)
//        }
//        return out
//    }
//
//    fun loadEpubBook(libraryItemId: Int, onLoaded: (ReaderScreenState) -> Unit) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val libraryItem = libraryDao.getItemById(libraryItemId)
//            setHighlightKeyForInternal(libraryItemId)
//            val loadedHighlights = loadHighlights()
//
//            _state.value = _state.value.copy(
//                shouldShowLoader = !epubParser.isBookCached(libraryItem!!.filePath)
//            )
//            val readerData = progressDao.getReaderData(libraryItemId)
//            // Only use toc if the book is not an external book.
//            var shouldUseToc = !libraryItem.isExternalBook
//            // Gutenberg for some reason don't include proper navMap for chinese books
//            // in toc file, so we need to parse the book based on spine, instead of toc.
//            // This is special case for Chinese books.
//            if (shouldUseToc && epubParser.peekLanguage(libraryItem.filePath) == "zh") {
//                Log.d("ReaderDetailViewModel", "Parsing book without toc for Chinese book.")
//                shouldUseToc = false
//            }
//            var epubBook = epubParser.createEpubBook(libraryItem.filePath, shouldUseToc)
//
//            _state.value = _state.value.copy(
//                title = libraryItem.title,
//                chapters = epubBook.chapters,
//                images = epubBook.images,
//                hasProgressSaved = readerData != null,
//                lastChapterIndex = readerData?.lastChapterIndex ?: 0,
//                lastChapterOffset = readerData?.lastChapterOffset ?: 0,
//                currentChapterIndex = readerData?.lastChapterIndex ?: 0,
//                highlightsByChapter = loadedHighlights
//            )
//            onLoaded(state.value)
//            // Added some delay to avoid choppy animation.
//            if (state.value.shouldShowLoader) {
//                delay(200L)
//            }
//            _state.value = _state.value.copy(
//                isLoading = false,
//                shouldShowLoader = false
//            )
//        }
//    }
//
//    fun loadEpubBookExternal(fileStream: FileInputStream) {
//        viewModelScope.launch(Dispatchers.IO) {
//            fileStream.use { fis ->
//                // parse and create epub book
//                val epubBook = epubParser.createEpubBook(fis, shouldUseToc = false)
//                setHighlightKeyForExternal(epubBook.title)
//                val loadedHighlights = loadHighlights()
//                _state.value = _state.value.copy(
//                    title = epubBook.title,
//                    chapters = epubBook.chapters,
//                    images = epubBook.images,
//                    hasProgressSaved = false,
//                    lastChapterIndex = 0,
//                    lastChapterOffset = 0,
//                    currentChapterIndex = 0,
//                            highlightsByChapter = loadedHighlights
//                )
//                // Added some delay to avoid choppy animation.
//                delay(200L)
//                _state.value = _state.value.copy(
//                    isLoading = false,
//                    shouldShowLoader = false
//                )
//            }
//        }
//    }
//
//    fun updateReaderProgress(libraryItemId: Int, chapterIndex: Int, chapterOffset: Int) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val isLastChapter = chapterIndex == state.value.chapters.size - 1
//            val hasSavedProgress = state.value.hasProgressSaved
//            val progressData = progressDao.getReaderData(libraryItemId)
//
//            when {
//                // Update progress for existing book
//                hasSavedProgress && !isLastChapter -> {
//                    progressData?.let {
//                        val updatedProgress = it.copy(
//                            lastChapterIndex = chapterIndex,
//                            lastChapterOffset = chapterOffset,
//                            lastReadTime = System.currentTimeMillis()
//                        )
//                        updatedProgress.id = it.id
//                        progressDao.update(updatedProgress)
//                    }
//                }
//
//                isLastChapter -> {
//                    // Delete progress for completed book
//                    progressData?.let { progressDao.delete(it.libraryItemId) }
//                }
//
//                else -> {
//                    // Insert new progress for new book
//                    progressDao.insert(
//                        ProgressData(
//                            libraryItemId = libraryItemId,
//                            lastChapterIndex = chapterIndex,
//                            lastChapterOffset = chapterOffset,
//                            lastReadTime = System.currentTimeMillis()
//                        )
//                    )
//                }
//            }
//        }
//    }
//
//
//    fun setChapterScrollPercent(percent: Float) {
//        _state.value = _state.value.copy(chapterScrollPercent = percent)
//    }
//
//    fun setVisibleChapterIndex(index: Int) {
//        _state.value = _state.value.copy(currentChapterIndex = index)
//    }
//
//    fun toggleReaderMenu() {
//        _state.value = _state.value.copy(showReaderMenu = !state.value.showReaderMenu)
//    }
//
//    fun hideReaderInfo() {
//        _state.value = _state.value.copy(showReaderMenu = false)
//    }
//
//    fun setFontFamily(font: ReaderFont) {
//        preferenceUtil.putString(PreferenceUtil.READER_FONT_STYLE_STR, font.id)
//        _state.value = _state.value.copy(fontFamily = font)
//    }
//
//    fun setFontSize(newValue: Int) {
//        preferenceUtil.putInt(PreferenceUtil.READER_FONT_SIZE_INT, newValue)
//        _state.value = _state.value.copy(fontSize = newValue)
//    }
//
//}