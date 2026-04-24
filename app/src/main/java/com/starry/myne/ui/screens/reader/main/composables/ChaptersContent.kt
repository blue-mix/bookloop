package com.starry.myne.ui.screens.reader.main.composables

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.starry.myne.R
import com.starry.myne.epub.BookTextMapper
import com.starry.myne.epub.models.EpubChapter
import com.starry.myne.helpers.highlights.Highlight
import com.starry.myne.helpers.highlights.HighlightColor
import com.starry.myne.helpers.toToast
import com.starry.myne.helpers.tts.TtsUiState
import com.starry.myne.ui.common.MyneSelectionContainer
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderScreenState
import custom.AppTheme
import custom.components.HorizontalDivider
import custom.components.Text
import kotlinx.coroutines.delay

@Immutable
data class HighlightPalette(
    val yellow: Color,
    val green: Color,
    val blue: Color,
    val pink: Color
)

@Composable
fun rememberHighlightPalette(): HighlightPalette {
    return remember {
        // You can swap primary for anything else if you want perfect theming
        HighlightPalette(
            yellow = Color(0xFFFFB703).copy(alpha = 0.22f),
            green = Color(0xFF00C853).copy(alpha = 0.22f),
            blue = Color(0xFF42A5F5).copy(alpha = 0.22f),
            pink = Color(0xFFE91E63).copy(alpha = 0.22f)
        )
    }
}

private fun chunkText(text: String): List<String> =
    text.splitToSequence("\n\n").filter { it.isNotBlank() }.toList()

private fun selectionToLookupWord(raw: String): String =
    raw.trim()
        .replace(Regex("\\s+"), " ")
        .trim('\"', '\'', '(', ')', '[', ']', '{', '}', '.', ',', ';', '!', '?', ':')

private fun shareText(ctx: Context, s: String) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, s)
        type = "text/plain"
    }
    try {
        ctx.startActivity(Intent.createChooser(intent, null))
    } catch (e: ActivityNotFoundException) {
        ctx.getString(R.string.no_app_to_handle_content).toToast(ctx)
    }
}

private fun webSearch(ctx: Context, s: String) {
    val intent = Intent().apply {
        action = Intent.ACTION_WEB_SEARCH
        putExtra(SearchManager.QUERY, s)
    }
    try {
        ctx.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        ctx.getString(R.string.no_app_to_handle_content).toToast(ctx)
    }
}

private fun openTranslate(ctx: Context, s: String) {
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://translate.google.com/?sl=auto&tl=en&text=$s"))
    try {
        ctx.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        ctx.getString(R.string.no_app_to_handle_content).toToast(ctx)
    }
}

fun buildAnnotatedWithHighlights(
    text: String,
    highlights: List<Highlight>,
    palette: HighlightPalette,
    ttsSpan: IntRange?,          // <-- NEW (paragraph-local start..end)
    ttsBg: Color                 // <-- NEW
): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        // 1) user highlights first
        highlights.forEach { h ->
            if (h.start in text.indices && h.end in 1..text.length && h.start < h.end) {
                val bg = when (h.color) {
                    HighlightColor.YELLOW -> palette.yellow
                    HighlightColor.GREEN -> palette.green
                    HighlightColor.BLUE -> palette.blue
                    HighlightColor.PINK -> palette.pink
                }
                addStyle(SpanStyle(background = bg), start = h.start, end = h.end)
            }
        }
        // 2) TTS sentence highlight on top
        ttsSpan?.let { r ->
            val s = r.first.coerceIn(0, text.length)
            val e = r.last.coerceIn(0, text.length)
            if (s < e) addStyle(SpanStyle(background = ttsBg), start = s, end = e)
        }
    }
}

@Composable
fun ChaptersContent(
    state: ReaderScreenState,
    lazyListState: LazyListState,
    onToggleReaderMenu: () -> Unit,
    onLookup: (String) -> Unit,
    onHighlight: (chapterId: String, paragraphIndex: Int, paragraphText: String, selection: String, color: HighlightColor) -> Unit,
    ttsUi: TtsUiState,
    pendingFocus: Pair<Int, Int>?,                  // NEW
    onPendingFocusConsumed: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState
    ) {
        items(
            count = state.chapters.size,
            key = { index -> state.chapters[index].chapterId }
        ) { index ->
            val chapter = state.chapters[index]
            ChapterLazyItemItem(
                chapterIndex = index,
                chapter = chapter,
                state = state,
                onClick = onToggleReaderMenu,
                onLookup = onLookup,
                ttsUi = ttsUi,
                onHighlight = { paragraphIndex, paragraphText, selection, color ->
                    onHighlight(chapter.chapterId, paragraphIndex, paragraphText, selection, color)
                },
                pendingFocus = pendingFocus,             // ← pass focus
                onPendingFocusConsumed = onPendingFocusConsumed // NEW
            )
        }
    }
}


@Composable
private fun ChapterLazyItemItem(
    chapterIndex: Int,
    chapter: EpubChapter,
    state: ReaderScreenState,
    onClick: () -> Unit,
    onLookup: (String) -> Unit,
    pendingFocus: Pair<Int, Int>?,                // NEW
    onPendingFocusConsumed: () -> Unit,
    ttsUi: TtsUiState,
    onHighlight: (paragraphIndex: Int, paragraphText: String, selection: String, color: HighlightColor) -> Unit
) {
    val context = LocalContext.current
    val textToolbar = LocalTextToolbar.current   // ⬅ used in #2
    val paragraphs = remember { chunkText(chapter.body) }

    val targetFontSize = remember(state.fontSize) {
        (state.fontSize / 5) * 0.9f
    }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = AppTheme.colors.elevation.copy(0.22f),          // cursor/handle color
        backgroundColor = AppTheme.colors.elevation.copy(0.22f),   // highlight color
    )
    val fontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = tween(durationMillis = 300),
        label = "fontSize"
    )
    Spacer(modifier = Modifier.height(12.dp))

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        MyneSelectionContainer(
            onCopyRequested = { /* your toast for <= S_V2 if you want */ },
            onShareRequested = { shareText(context, it) },
            onWebSearchRequested = { webSearch(context, it) },
            onTranslateRequested = { openTranslate(context, it) },
            onDictionaryRequested = { selection ->
                val word = selectionToLookupWord(selection)
                if (word.isNotEmpty()) onLookup(word)
            },
            onHighlightRequested = { selection, color ->
                val spans = computeCrossParagraphSpans(paragraphs, selection)
                if (spans.isEmpty()) {
                    context.getString(R.string.error).toToast(context)
                    return@MyneSelectionContainer
                }
                spans.forEach { hit ->
                    val pIdx = hit.paragraphIndex
                    val pText = paragraphs[pIdx]
                    val sub = pText.substring(hit.start, hit.end)
                    onHighlight(pIdx, pText, sub, color)
                }
            }

        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val up = waitForUpOrCancellation()
                            if (up != null && down.id == up.id) {
                                textToolbar.hide()   // ⬅ hide ActionMode
                                onClick()
                            }
                        }
                    }
            ) {
                paragraphs.forEachIndexed { pIndex, paraRaw ->
                    val imgEntry = BookTextMapper.ImgEntry.fromXMLString(paraRaw)
                    if (imgEntry != null) {
                        val image = state.images.find { it.absPath == imgEntry.path }
                        image?.let {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(image.image)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 6.dp)
                            )
                        }
                    } else {
                        val paragraphText = paraRaw.trimEnd()
                        val highlightsForParagraph = state.highlightsByChapter[chapter.chapterId]
                            .orEmpty()
                            .filter { it.paragraphIndex == pIndex }

                        val locator = ttsUi.currentLocator
                        val ttsSpanForParagraph: IntRange? =
                            if (locator != null
                                && locator.chapterIndex == chapterIndex
                                && locator.paragraphIndex == pIndex
                                && locator.start in 0..paragraphText.length
                                && locator.end in 0..paragraphText.length
                                && locator.start < locator.end
                            ) {
                                locator.start until locator.end
                            } else null
                        val ttsBg = AppTheme.colors.primary.copy(alpha = 0.28f)
                        val palette = rememberHighlightPalette()

                        // ----- BringIntoView + flash -----
                        val bringer = remember { BringIntoViewRequester() }
                        var flash by remember { mutableStateOf(false) }
                        val flashColor by animateColorAsState(
                            if (flash) AppTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                            label = "flashColor"
                        )

                        LaunchedEffect(pendingFocus, state.currentChapterIndex, pIndex) {
                            val pf = pendingFocus
                            if (pf != null &&
                                pf.first == state.currentChapterIndex &&
                                pf.second == pIndex
                            ) {
                                // Ensure layout is ready, then bring into view
                                bringer.bringIntoView()
                                // Flash briefly
                                flash = true
                                delay(450)
                                flash = false
                                onPendingFocusConsumed()
                            }
                        }

                        val annotated = remember(
                            paragraphText,
                            highlightsForParagraph,
                            ttsSpanForParagraph,
                            ttsBg
                        ) {
                            buildAnnotatedWithHighlights(
                                paragraphText,
                                highlightsForParagraph,
                                palette,
                                ttsSpanForParagraph,
                                ttsBg

                            )
                        }
                        Text(
                            text = annotated,
                            fontSize = fontSize.sp,
                            lineHeight = 1.3.em,
                            fontFamily = state.fontFamily.fontFamily,
                            modifier = Modifier
                                .padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
                                .background(flashColor)              // flash layer
                                .bringIntoViewRequester(bringer),    // <— hook requester
                            color = AppTheme.colors.onBackground
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 2.dp,
            color = AppTheme.colors.onBackground.copy(alpha = 0.2f)
        )
    }
}

private fun computeCrossParagraphSpans(
    paragraphs: List<String>,
    selectedRaw: String
): List<Hit> {
    // Try multiple plausible joiners (what Android could insert between Text nodes)
    val joiners = arrayOf(" ", "\n", "\n\n", "")
    for (joiner in joiners) {
        val spans = computeWithJoiner(paragraphs, selectedRaw, joiner)
        if (spans.isNotEmpty()) return spans
    }
    return emptyList()
}

private fun computeWithJoiner(
    paragraphs: List<String>,
    selectedRaw: String,
    joiner: String
): List<Hit> {
    if (selectedRaw.isBlank()) return emptyList()

    // Build "original global" with a specific joiner
    val bases = IntArray(paragraphs.size)
    val globalOrig = buildString {
        paragraphs.forEachIndexed { i, p ->
            bases[i] = length
            append(p)
            if (i != paragraphs.lastIndex) append(joiner)
        }
    }

    // Normalize whitespace consistently (collapse runs → single space)
    val (globalNorm, norm2orig) = normalizeWithMap(globalOrig)
    val selectedNorm = normalizeNoMap(selectedRaw).trim()

    if (selectedNorm.isEmpty()) return emptyList()

    val normStart = globalNorm.indexOf(selectedNorm)
    if (normStart < 0) return emptyList()

    val normEnd = normStart + selectedNorm.length

    // Map normalized indices back to original-global indices. For end, map to
    // the char after the last normalized char (best effort across whitespace).
    val gStart = norm2orig.getOrElse(normStart) { 0 }
    val gEnd = if (normEnd > 0) {
        val lastOrig = norm2orig.getOrElse(normEnd - 1) { globalOrig.length - 1 }
        (lastOrig + 1).coerceAtMost(globalOrig.length)
    } else 0

    // Intersect [gStart,gEnd) with each paragraph's [base, base+len) in ORIGINAL global space
    val hits = mutableListOf<Hit>()
    paragraphs.forEachIndexed { i, p ->
        val base = bases[i]
        val pend = base + p.length
        val s = maxOf(gStart, base)
        val e = minOf(gEnd, pend)
        if (s < e) {
            hits.add(Hit(paragraphIndex = i, start = s - base, end = e - base))
        }
    }
    return hits
}

/** Collapse all Unicode whitespace to a single ' ' and provide a mapping back to original indices. */
private fun normalizeWithMap(src: String): Pair<String, IntArray> {
    val out = StringBuilder(src.length)
    val map = ArrayList<Int>(src.length)
    var i = 0
    var lastWasWs = false
    while (i < src.length) {
        val ch = src[i]
        val isWs = ch.isWhitespace()
        if (isWs) {
            // collapse run
            var j = i
            while (j < src.length && src[j].isWhitespace()) j++
            if (!lastWasWs) {
                out.append(' ')
                map.add(i) // map the single normalized space to the first original index of the run
                lastWasWs = true
            }
            i = j
        } else {
            out.append(ch)
            map.add(i)
            lastWasWs = false
            i++
        }
    }
    // Build int array
    val norm2orig = IntArray(map.size)
    for (k in map.indices) norm2orig[k] = map[k]
    return out.toString() to norm2orig
}

/** Normalize without mapping (for the selection string). */
private fun normalizeNoMap(src: String): String {
    if (src.isEmpty()) return src
    val out = StringBuilder(src.length)
    var i = 0
    var lastWasWs = false
    while (i < src.length) {
        val ch = src[i]
        val isWs = ch.isWhitespace() || ch == '\u00A0' // NBSP
        if (isWs) {
            // collapse run
            while (i < src.length && (src[i].isWhitespace() || src[i] == '\u00A0')) i++
            if (!lastWasWs) {
                out.append(' ')
                lastWasWs = true
            }
        } else {
            out.append(ch)
            lastWasWs = false
            i++
        }
    }
    return out.toString()
}


private fun Char.isWhitespace(): Boolean = this <= ' ' || Character.isWhitespace(this)

private data class Hit(val paragraphIndex: Int, val start: Int, val end: Int)

