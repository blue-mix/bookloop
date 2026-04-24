package com.starry.myne.epub.models

import android.graphics.Bitmap
import com.starry.myne.epub.cache.BitmapSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Represents an epub book.
 *
 * @param fileName The name of the epub file.
 * @param title The title of the book.
 * @param author The author of the book.
 * @param language The language code of the book.
 * @param coverImage The cover image of the book.
 * @param chapters The list of chapters in the book.
 * @param images The list of images in the book.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EpubBook @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(1) val fileName: String,
    @ProtoNumber(2) val title: String,
    @ProtoNumber(3) val author: String,
    @ProtoNumber(4) val language: String,
    @ProtoNumber(5) @Serializable(with = BitmapSerializer::class) val coverImage: Bitmap?,
    @ProtoNumber(6) val chapters: List<EpubChapter> = emptyList(),
    @ProtoNumber(7) val images: List<EpubImage> = emptyList()
)