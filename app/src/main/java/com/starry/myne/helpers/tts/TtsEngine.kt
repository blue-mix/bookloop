package com.starry.myne.helpers.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.BreakIterator
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton


data class TtsUiState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val autoScroll: Boolean = true,
    val currentLocator: TtsLocator? = null,
    val resumeOnReenter: Boolean = true
)

@Singleton
class TtsEngine @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private var autoAdvance: Boolean = false  // NEW

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _ui = MutableStateFlow(TtsUiState())
    val ui: StateFlow<TtsUiState> = _ui

    // Bound chapter text
    private var bookId: Int? = null
    private var chapterId: String = ""
    private var chapterIndex: Int = 0
    private var paragraphs: List<String> = emptyList()
    private var sentenceRangesByParagraph: List<List<IntRange>> = emptyList()

    private var tts: TextToSpeech? = null
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        parseUtteranceId(utteranceId)?.let { loc ->
                            scope.launch {
                                _ui.value = _ui.value.copy(
                                    currentLocator = loc,
                                    isPlaying = true,
                                    isReady = true
                                )
                            }
                            persistLocator(loc)
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        // Only auto-advance when we’re actively playing.
                        if (autoAdvance) {
                            scope.launch { speakNextInternal() }
                        } else {
                            scope.launch { _ui.value = _ui.value.copy(isPlaying = false) }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch { _ui.value = _ui.value.copy(isPlaying = false) }
                    }
                })
                scope.launch { _ui.value = _ui.value.copy(isReady = true) }
            }
        }

        setSpeed(1.0f)
        setPitch(1.0f)
    }
//
//    private fun initTts() {
//        tts = TextToSpeech(appContext) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                tts?.language = Locale.US
//                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//                    override fun onStart(utteranceId: String?) {
//                        parseUtteranceId(utteranceId)?.let { loc ->
//                            scope.launch { _ui.value = _ui.value.copy(currentLocator = loc, isPlaying = true, isReady = true) }
//                            persistLocator(loc)
//                        }
//                    }
//                    override fun onDone(utteranceId: String?) {
//                        // auto-advance
//                        scope.launch { speakNextInternal() }
//                    }
//                    @Deprecated("Deprecated in Java")
//                    override fun onError(utteranceId: String?) {
//                        scope.launch { _ui.value = _ui.value.copy(isPlaying = false) }
//                    }
//                })
//                scope.launch { _ui.value = _ui.value.copy(isReady = true) }
//            }
//        }
//
//        // Defaults
//        setSpeed(1.0f)
//        setPitch(1.0f)
//    }

    // ---------- Public API ----------

    /** Bind current chapter’s text and indices. Must be called on chapter change. */
    fun bindChapter(bookId: Int?, chapterId: String, chapterIndex: Int, paragraphs: List<String>) {
        this.bookId = bookId
        this.chapterId = chapterId
        this.chapterIndex = chapterIndex
        this.paragraphs = paragraphs
        this.sentenceRangesByParagraph = paragraphs.map { sentenceRanges(it) }
    }

    /** Reads the currently selected range (paragraph-bound). */
    fun readSelection(paragraphIndex: Int, start: Int, end: Int) {
        if (!ensureReady()) return
        val ranges = sentenceRangesByParagraph.getOrNull(paragraphIndex).orEmpty()
        val firstSentenceIdx = ranges.indexOfFirst { start in it }.let { if (it == -1) 0 else it }
        val r = ranges.getOrNull(firstSentenceIdx) ?: return
        val loc = TtsLocator(
            bookId, chapterId, chapterIndex,
            paragraphIndex, firstSentenceIdx, r.first, r.last + 1
        )
        speakFrom(loc)
    }

    /** Resume from saved locator if the bound chapter matches. */
    fun resumeFromSaved(): Boolean {
        val saved = restoreLocator() ?: return false
        if (saved.chapterIndex != chapterIndex || saved.chapterId != chapterId) return false
        speakFrom(saved)
        return true
    }

    fun speakText(text: String) {
        if (text.isNotBlank()) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "DICT_WORD")
    }

    fun speakFrom(locator: TtsLocator) {
        if (!ensureReady()) return
        requestAudioFocus() ?: return
        tts?.stop()
        autoAdvance = true                         // NEW: we’re in play mode
        scope.launch { _ui.value = _ui.value.copy(isPlaying = true) }
        speakSingleSentence(locator)               // NEW
    }

    private fun speakSingleSentence(loc: TtsLocator) {  // NEW
        val ranges = sentenceRangesByParagraph.getOrNull(loc.paragraphIndex).orEmpty()
        val r = ranges.getOrNull(loc.sentenceIndexInParagraph) ?: return
        val text = paragraphs[loc.paragraphIndex].substring(r.first, r.last + 1)
        val uid = makeUtteranceId(
            loc.copy(start = r.first, end = r.last + 1)
        )
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, uid)
    }

    private var resumeOnReenter: Boolean = true

    fun setResumeOnReenter(v: Boolean) {
        resumeOnReenter = v
        _ui.update { it.copy(resumeOnReenter = v) }
    }

    fun shouldAutoResume(): Boolean = resumeOnReenter

    fun pause() {
        tts?.stop()
        abandonAudioFocus()
        autoAdvance = false                         // NEW
        setResumeOnReenter(true)                    // Paused → OK to auto-resume later
        scope.launch { _ui.value = _ui.value.copy(isPlaying = false) }
    }

    fun stop() {
        tts?.stop()
        abandonAudioFocus()
        autoAdvance = false                         // NEW
        setResumeOnReenter(false)                   // Stopped → do NOT auto-resume
        scope.launch {
            _ui.value = _ui.value.copy(isPlaying = false, currentLocator = null)
        }
    }

    fun setSpeed(value: Float) {
        val v = value.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(v)
        scope.launch { _ui.value = _ui.value.copy(speed = v) }
    }

    fun setPitch(value: Float) {
        val v = value.coerceIn(0.5f, 2.0f)
        tts?.setPitch(v)
        scope.launch { _ui.value = _ui.value.copy(pitch = v) }
    }

    fun setAutoScroll(enabled: Boolean) {
        scope.launch { _ui.value = _ui.value.copy(autoScroll = enabled) }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }

    fun nextSentence() {
        scope.launch { speakNextInternal(manual = true) }
    }

    fun previousSentence() {
        scope.launch { speakPrevInternal() }
    }

    private fun speakFromInternal(start: TtsLocator) {
        var loc = start
        // queue current + remainder sentences
        var pi = loc.paragraphIndex
        var si = loc.sentenceIndexInParagraph
        val builder = mutableListOf<Pair<String, String>>() // text to utteranceId

        while (pi < paragraphs.size) {
            val ranges = sentenceRangesByParagraph[pi]
            while (si < ranges.size) {
                val r = ranges[si]
                val text = paragraphs[pi].substring(r.first, r.last + 1)
                val uid = makeUtteranceId(
                    TtsLocator(
                        bookId,
                        chapterId,
                        chapterIndex,
                        pi,
                        si,
                        r.first,
                        r.last + 1
                    )
                )
                builder.add(text to uid)
                si++
            }
            pi++; si = 0
        }
        // Speak queue (first flush, remainder enqueue)
        builder.forEachIndexed { idx, (text, uid) ->
            val q = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, q, null, uid)
        }
    }


    private fun speakNextInternal(manual: Boolean = false) {
        val cur = _ui.value.currentLocator ?: return
        val p = cur.paragraphIndex
        val s = cur.sentenceIndexInParagraph

        val rangesHere = sentenceRangesByParagraph.getOrNull(p).orEmpty()
        when {
            s + 1 < rangesHere.size -> {
                val r = rangesHere[s + 1]
                speakSingleSentence(
                    cur.copy(
                        sentenceIndexInParagraph = s + 1,
                        start = r.first, end = r.last + 1
                    )
                )
            }

            else -> {
                // Find the first non-empty next paragraph
                var np = p + 1
                while (np < paragraphs.size && sentenceRangesByParagraph[np].isEmpty()) np++
                if (np < paragraphs.size) {
                    val nextRanges = sentenceRangesByParagraph[np]
                    val r0 = nextRanges[0]
                    speakSingleSentence(
                        cur.copy(
                            paragraphIndex = np,
                            sentenceIndexInParagraph = 0,
                            start = r0.first, end = r0.last + 1
                        )
                    )
                } else {
                    // End of chapter
                    stop()
                }
            }
        }
    }

    private fun speakPrevInternal() {
        val cur = _ui.value.currentLocator ?: return
        val p = cur.paragraphIndex
        val s = cur.sentenceIndexInParagraph

        when {
            s - 1 >= 0 -> {
                val r = sentenceRangesByParagraph[p][s - 1]
                speakSingleSentence(
                    cur.copy(
                        sentenceIndexInParagraph = s - 1,
                        start = r.first, end = r.last + 1
                    )
                )
            }

            else -> {
                // Find previous non-empty paragraph
                var pp = p - 1
                while (pp >= 0 && sentenceRangesByParagraph[pp].isEmpty()) pp--
                if (pp >= 0) {
                    val prevRanges = sentenceRangesByParagraph[pp]
                    val lastIdx = prevRanges.lastIndex
                    val r = prevRanges[lastIdx]
                    speakSingleSentence(
                        cur.copy(
                            paragraphIndex = pp,
                            sentenceIndexInParagraph = lastIdx,
                            start = r.first, end = r.last + 1
                        )
                    )
                }
            }
        }
    }


    private fun ensureReady() = _ui.value.isReady && paragraphs.isNotEmpty()

    /** Clean, correct sentence ranges: [start, endInclusive]. */
    private fun sentenceRanges(paragraph: String): List<IntRange> {
        return try {
            val it = BreakIterator.getSentenceInstance(tts?.language ?: Locale.US)
            it.setText(paragraph)
            val ranges = ArrayList<IntRange>()
            var start = it.first()
            var end = it.next()
            while (end != BreakIterator.DONE) {
                var s = start
                var e = end
                while (s < e && paragraph[s].isWhitespace()) s++
                while (e > s && paragraph[e - 1].isWhitespace()) e--
                if (e > s) ranges.add(s..(e - 1))   // inclusive end
                start = end
                end = it.next()
            }
            ranges
        } catch (_: Throwable) {
            // Fallback: naive split by sentence terminators, inclusive end
            val rx = Regex("(?<=[.!?])\\s+")
            val out = mutableListOf<IntRange>()
            var base = 0
            paragraph.split(rx).forEach { seg ->
                val trimmed = seg.trim()
                if (trimmed.isNotEmpty()) {
                    val start = paragraph.indexOf(trimmed, base)
                    val end = start + trimmed.length
                    out.add(start..(end - 1))
                    base = end
                } else {
                    base += seg.length
                }
            }
            out
        }
    }

    private fun makeUtteranceId(loc: TtsLocator) =
        "tts|${loc.bookId ?: -1}|${loc.chapterIndex}|${loc.paragraphIndex}|${loc.sentenceIndexInParagraph}|${loc.start}|${loc.end}|${loc.chapterId}"

    private fun parseUtteranceId(id: String?): TtsLocator? {
        if (id == null || !id.startsWith("tts|")) return null
        val parts = id.split('|')
        return try {
            TtsLocator(
                bookId = parts[1].toInt().takeIf { it >= 0 },
                chapterIndex = parts[2].toInt(),
                paragraphIndex = parts[3].toInt(),
                sentenceIndexInParagraph = parts[4].toInt(),
                start = parts[5].toInt(),
                end = parts[6].toInt(),
                chapterId = parts.subList(7, parts.size)
                    .joinToString("|") // chapterId may contain '|'
            )
        } catch (_: Throwable) {
            null
        }
    }

    // ---------- Audio focus ----------

    private fun requestAudioFocus(): Unit? {
        val attrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        return if (Build.VERSION.SDK_INT >= 26) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setOnAudioFocusChangeListener { change ->
                    if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause()
                }
                .setAudioAttributes(attrs)
                .build()
            focusRequest = req
            val res = audioManager.requestAudioFocus(req)
            if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) Unit else null
        } else {
            @Suppress("DEPRECATION")
            val res = audioManager.requestAudioFocus(
                { change ->
                    if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause()
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) Unit else null
        }
    }

    private fun abandonAudioFocus() {
        focusRequest?.let {
            if (Build.VERSION.SDK_INT >= 26) audioManager.abandonAudioFocusRequest(it)
        }
    }

    // ---------- Persistence (PreferenceUtil-like simple store) ----------

    private fun persistLocator(loc: TtsLocator) {
        val prefs = appContext.getSharedPreferences("tts_locator", Context.MODE_PRIVATE)
        val key = "book_${loc.bookId ?: -1}_chapter_${loc.chapterId}"
        val value = listOf(
            loc.chapterIndex,
            loc.paragraphIndex,
            loc.sentenceIndexInParagraph,
            loc.start,
            loc.end
        ).joinToString(",")
        prefs.edit().putString(key, value).apply()
    }

    private fun restoreLocator(): TtsLocator? {
        val prefs = appContext.getSharedPreferences("tts_locator", Context.MODE_PRIVATE)
        val key = "book_${bookId ?: -1}_chapter_${chapterId}"
        val str = prefs.getString(key, null) ?: return null
        val parts = str.split(',')
        return try {
            val pi = parts[1].toInt()
            val si = parts[2].toInt()
            val st = parts[3].toInt()
            val en = parts[4].toInt()
            TtsLocator(bookId, chapterId, chapterIndex, pi, si, st, en)
        } catch (_: Throwable) {
            null
        }
    }
}
