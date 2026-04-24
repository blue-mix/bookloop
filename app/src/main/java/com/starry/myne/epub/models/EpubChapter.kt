package com.starry.myne.epub.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Represents a chapter in an epub book.
 *
 * @param absPath The absolute path of the chapter.
 * @param title The title of the chapter.
 * @param body The body of the chapter.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EpubChapter @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(1) val chapterId: String,
    @ProtoNumber(2) val absPath: String,
    @ProtoNumber(3) val title: String,
    @ProtoNumber(4) val body: String
)