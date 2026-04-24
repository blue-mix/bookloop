package com.starry.myne.api.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class BookSet(
    @SerialName("count")
    val count: Int = 0,
    @SerialName("next")
    val next: String? = null,
    @SerialName("previous")
    val previous: String? = null,
    @SerialName("results")
    val books: List<Book> = listOf(),
    // For checking if the book is cached or not
    // Not part of the API response.
    val isCached: Boolean = false
)