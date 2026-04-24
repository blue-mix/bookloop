package com.starry.myne.api.models

import androidx.annotation.Keep

/** Extra info from google books API */
@Keep
data class ExtraInfo(
    val coverImage: String = "",
    val pageCount: Int = 0,
    val description: String = "",
    // Not part of the API response.
    // Used to check if extra info is cached.
    val isCached: Boolean = false
)
