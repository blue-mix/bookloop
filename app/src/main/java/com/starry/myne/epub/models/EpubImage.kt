package com.starry.myne.epub.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Represents an image in an epub book.
 *
 * @param absPath The absolute path of the image.
 * @param image The image data.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EpubImage @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(1) val absPath: String,
    @ProtoNumber(2) val image: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EpubImage

        if (absPath != other.absPath) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = absPath.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}