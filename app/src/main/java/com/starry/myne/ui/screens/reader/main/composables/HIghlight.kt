package com.starry.myne.ui.screens.reader.main.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.starry.myne.epub.models.EpubChapter
import com.starry.myne.helpers.highlights.Highlight
import com.starry.myne.helpers.highlights.HighlightColor
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderScreenState
import custom.AppTheme
import custom.components.Icon
import custom.components.IconButton
import custom.components.IconButtonVariant
import custom.components.Surface
import custom.components.Text

@Composable
fun HighlightsSheet(
    state: ReaderScreenState,
    onNavigateTo: (chapterIndex: Int, paragraphIndex: Int) -> Unit,
    onDelete: (chapterId: String, highlight: Highlight) -> Unit,
    onChangeColor: (chapterId: String, highlight: Highlight, newColor: HighlightColor) -> Unit
) {
    val items = remember(state.highlightsByChapter, state.chapters) {
        buildHighlightItems(state)
    }

    if (items.isEmpty()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No highlights yet",
                style = AppTheme.typography.body2,
                color = AppTheme.colors.onSurface
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Optional chapter headers (sorted by chapter index)
        val grouped = items.groupBy { it.chapterIndex }.toSortedMap()
        grouped.forEach { (_, chapterItems) ->
            item(key = "header_${chapterItems.first().chapterIndex}") {
                SheetHeader(chapterItems.first().chapterTitle)
            }
            items(
                items = chapterItems,
                key = { it.key }
            ) { row ->
                HighlightRow(
                    row = row,
                    onClick = { onNavigateTo(row.chapterIndex, row.highlight.paragraphIndex) },
                    onDelete = { onDelete(row.chapterId, row.highlight) },
                    onPickColor = { c -> onChangeColor(row.chapterId, row.highlight, c) }
                )
            }
            item(key = "sp_${chapterItems.first().chapterIndex}") { Spacer(Modifier.height(6.dp)) }
        }
    }
}

// ---------- Row --------------------------------------------------------------

@Composable
private fun HighlightRow(
    row: HighlightRowUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPickColor: (HighlightColor) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val colorDot = row.highlight.color.asColor()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        color = AppTheme.colors.surface,
        contentColor = AppTheme.colors.onSurface
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Color dot (opens menu)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(colorDot)
                    .clickable { menuOpen = true }
            )

            Spacer(Modifier.width(12.dp))

            // Excerpt + meta
            Column(Modifier.weight(1f)) {
                Text(
                    row.excerptStyled,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "¶${row.highlight.paragraphIndex + 1}",
                    style = AppTheme.typography.label3,
                    color = AppTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Delete
            IconButton(variant = IconButtonVariant.Ghost, onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = AppTheme.colors.onSurface
                )
            }

            // Color picker menu
            DropdownMenu(
                containerColor = AppTheme.colors.surface,
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }) {
                HighlightColor.entries.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.name.lowercase().replaceFirstChar { it.titlecase() }) },
                        onClick = {
                            menuOpen = false
                            onPickColor(c)
                        },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(c.asColor())
                            )
                        }
                    )
                }
            }
        }
    }
}

// ---------- Small pieces -----------------------------------------------------

@Composable
private fun SheetHeader(title: String) {
    Text(
        text = title.ifBlank { "Untitled chapter" },
        style = AppTheme.typography.label1,
        color = AppTheme.colors.onSurface.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp, start = 6.dp, end = 6.dp)
    )
}

// Flatten ReaderScreenState.highlightsByChapter → UI rows
private fun buildHighlightItems(state: ReaderScreenState): List<HighlightRowUi> {
    val chapters = state.chapters
    val list = mutableListOf<HighlightRowUi>()

    state.highlightsByChapter.forEach { (chapterId, highlights) ->
        val chapterIndex = chapters.indexOfFirst { it.chapterId == chapterId }
        if (chapterIndex == -1) return@forEach
        val chapter: EpubChapter = chapters[chapterIndex]
        val paragraphs = chapter.body.split("\n\n")

        highlights.forEach { h ->
            val paragraph = paragraphs.getOrNull(h.paragraphIndex).orEmpty()
            if (paragraph.isBlank()) return@forEach

            // Safe bounds
            val s = h.start.coerceIn(0, paragraph.length)
            val e = h.end.coerceIn(s, paragraph.length)

            val (snippet, rangeInSnippet) = makeSnippet(paragraph, s, e)
            val excerptStyled = buildExcerpt(snippet, rangeInSnippet, h.color.asColor())

            list += HighlightRowUi(
                key = "${chapterId}_${h.paragraphIndex}_${h.start}_${h.end}_${h.color}",
                chapterId = chapterId,
                chapterIndex = chapterIndex,
                chapterTitle = chapter.title,
                highlight = h,
                excerptStyled = excerptStyled
            )
        }
    }
    // Sort by chapter, then paragraph
    return list.sortedWith(
        compareBy<HighlightRowUi> { it.chapterIndex }.thenBy { it.highlight.paragraphIndex }
            .thenBy { it.highlight.start }
    )
}

// Build a short excerpt around the selection with ellipses and return the local range
private fun makeSnippet(full: String, start: Int, end: Int, pad: Int = 45): Pair<String, IntRange> {
    val left = (start - pad).coerceAtLeast(0)
    val right = (end + pad).coerceAtMost(full.length)
    val headEllipsis = if (left > 0) "…" else ""
    val tailEllipsis = if (right < full.length) "…" else ""
    val snippet = headEllipsis + full.substring(left, right) + tailEllipsis
    val localStart = headEllipsis.length + (start - left)
    val localEnd = headEllipsis.length + (end - left)
    return snippet to (localStart until localEnd)
}

// Paint the selected segment in the excerpt
private fun buildExcerpt(snippet: String, range: IntRange, color: Color): AnnotatedString {
    return buildAnnotatedString {
        append(snippet)
        val s = range.first.coerceIn(0, snippet.length)
        val e = range.last.coerceIn(0, snippet.length)
        if (s < e) addStyle(SpanStyle(background = color.copy(alpha = 0.28f)), s, e)
    }
}

// UI model for each row
private data class HighlightRowUi(
    val key: String,
    val chapterId: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val highlight: Highlight,
    val excerptStyled: AnnotatedString
)

// Map your enum to colors that match your reading highlight palette
private fun HighlightColor.asColor(): Color = when (this) {
    HighlightColor.YELLOW -> Color(0xFFFFB703)
    HighlightColor.GREEN -> Color(0xFF00C853)
    HighlightColor.BLUE -> Color(0xFF42A5F5)
    HighlightColor.PINK -> Color(0xFFE91E63)
}
//
//@Composable
//fun HighlightsSheet(
//    state: ReaderScreenState,
//    onNavigateTo: (chapterIndex: Int, paragraphIndex: Int) -> Unit,
//    onDelete: (chapterId: String, highlight: Highlight) -> Unit,
//    onClearChapter: (chapterId: String) -> Unit,
//    onClearAll: () -> Unit
//) {
//    val chapterIndexById = remember(state.chapters) {
//        state.chapters.withIndex().associate { it.value.chapterId to it.index }
//    }
//
//    // Flatten & sort highlights by chapter, paragraph, start
//    data class RowItem(
//        val chapterId: String,
//        val chapterIndex: Int,
//        val chapterTitle: String,
//        val paragraphIndex: Int,
//        val snippet: String,
//        val color: HighlightColor,
//        val model: Highlight
//    )
//
//    val itemsByChapter = remember(state.highlightsByChapter, state.chapters) {
//        state.highlightsByChapter
//            .mapNotNull { (chapterId, list) ->
//                val ci = chapterIndexById[chapterId] ?: return@mapNotNull null
//                val ch = state.chapters.getOrNull(ci) ?: return@mapNotNull null
//                val paragraphs = ch.body.split("\n\n").filter { it.isNotBlank() }
//                val rows = list.mapNotNull { h ->
//                    val pText = paragraphs.getOrNull(h.paragraphIndex) ?: return@mapNotNull null
//                    val s = h.start.coerceIn(0, pText.length)
//                    val e = h.end.coerceIn(s, pText.length)
//                    RowItem(
//                        chapterId = chapterId,
//                        chapterIndex = ci,
//                        chapterTitle = ch.title,
//                        paragraphIndex = h.paragraphIndex,
//                        snippet = buildSnippet(pText, s, e),
//                        color = h.color,
//                        model = h
//                    )
//                }.sortedWith(
//                    compareBy<RowItem> { it.chapterIndex }
//                        .thenBy { it.paragraphIndex }
//                        .thenBy { it.model.start }
//                )
//                ci to rows
//            }
//            .sortedBy { it.first } // sort by chapter index
//            .toMap(linkedMapOf())
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 10.dp)
//    ) {
//        // Header
//        Row(
//            Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                "Highlights",
//                style = AppTheme.typography.h3,
//                color = AppTheme.colors.onBackground
//            )
//            if (itemsByChapter.values.any { it.isNotEmpty() }) {
//                TextButton(onClick = onClearAll) { Text("Clear all") }
//            }
//        }
//
//        Spacer(Modifier.height(6.dp))
//
//        if (itemsByChapter.isEmpty() || itemsByChapter.values.all { it.isEmpty() }) {
//            Text(
//                "No highlights yet.\nSelect text in the reader and choose a color.",
//                style = AppTheme.typography.body2,
//                color = AppTheme.colors.onSurface.copy(alpha = 0.7f),
//                modifier = Modifier.padding(vertical = 8.dp)
//            )
//            return@Column
//        }
//
//        // List
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 12.dp)
//        ) {
//            itemsByChapter.forEach { (chapterIndex, rows) ->
//                val chapterId = state.chapters[chapterIndex].chapterId
//                item(key = "header-$chapterIndex") {
//                    Row(
//                        Modifier
//                            .fillMaxWidth()
//                            .padding(top = 10.dp, bottom = 6.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            state.chapters[chapterIndex].title,
//                            style = AppTheme.typography.label1,
//                            color = AppTheme.colors.onBackground.copy(alpha = 0.8f),
//                            maxLines = 2
//                        )
//                        TextButton(onClick = { onClearChapter(chapterId) }) {
//                            Text("Clear chapter")
//                        }
//                    }
//                }
//
//                items(
//                    rows.size,
//                    key = { i -> "hl-$chapterIndex-$i-${rows[i].model.hashCode()}" }
//                ) { i ->
//                    val r = rows[i]
//                    HighlightRow(
//                        snippet = r.snippet,
//                        color = r.color,
//                        onClick = { onNavigateTo(r.chapterIndex, r.paragraphIndex) },
//                        onDelete = { onDelete(r.chapterId, r.model) }
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun HighlightRow(
//    snippet: String,
//    color: HighlightColor,
//    onClick: () -> Unit,
//    onDelete: () -> Unit
//) {
//    val chipColor = when (color) {
//        HighlightColor.YELLOW -> Color(0xFFFFB703)
//        HighlightColor.GREEN  -> Color(0xFF00C853)
//        HighlightColor.BLUE   -> Color(0xFF42A5F5)
//        HighlightColor.PINK   -> Color(0xFFE91E63)
//    }.copy(alpha = 0.35f)
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(14.dp))
//            .background(AppTheme.colors.surface)
//            .padding(horizontal = 12.dp, vertical = 10.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // color swatch
//        Box(
//            Modifier
//                .size(18.dp)
//                .clip(RoundedCornerShape(6.dp))
//                .background(chipColor)
//        )
//
//        Spacer(Modifier.width(10.dp))
//
//        // snippet (tap to navigate)
//        Text(
//            text = snippet,
//            style = AppTheme.typography.body2,
//            color = AppTheme.colors.onSurface,
//            modifier = Modifier
//                .weight(1f)
//                .padding(end = 10.dp).clickable(onClick = onClick)
//        )
//
//        // delete
//        IconButton(
//            variant = IconButtonVariant.Ghost,
//            onClick = onDelete
//        ) {
//            Icon(
//                imageVector = Icons.Rounded.Delete,
//                contentDescription = "Delete highlight",
//                tint = AppTheme.colors.onSurface
//            )
//        }
//    }
//
//    Spacer(Modifier.height(8.dp))
//}

private fun buildSnippet(p: String, s: Int, e: Int, pad: Int = 48): String {
    val start = (s - pad).coerceAtLeast(0)
    val end = (e + pad).coerceAtMost(p.length)
    val prefix = if (start > 0) "… " else ""
    val suffix = if (end < p.length) " …" else ""
    return (prefix + p.substring(start, end).trim() + suffix)
}
