package com.starry.myne.helpers.tts

data class TtsLocator(
    val bookId: Int?,            // nullable for external books
    val chapterId: String,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val start: Int,              // char start in paragraph
    val end: Int                 // char end (exclusive) in paragraph
)

