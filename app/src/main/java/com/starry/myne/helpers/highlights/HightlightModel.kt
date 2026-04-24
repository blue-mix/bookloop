package com.starry.myne.helpers.highlights

// app/.../reader/highlight/HighlightModels.kt
data class Highlight(
    val chapterId: String,
    val paragraphIndex: Int,
    val start: Int,
    val end: Int,
    val color: HighlightColor
)

enum class HighlightColor { YELLOW, GREEN, BLUE, PINK }
