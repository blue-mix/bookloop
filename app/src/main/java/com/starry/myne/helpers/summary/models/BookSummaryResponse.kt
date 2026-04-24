package com.starry.myne.helpers.summary.models

import kotlinx.serialization.Serializable

@Serializable
data class BookSummaryResponse(
    val title: String,
    val author: String,
    val summary_points: List<String>,
    val final_summary: String,
    val read_time_hours: String,
    val buy_links: BuyLinks
)

@Serializable
data class BuyLinks(
    val amazon: String,
    val goodreads: String
)
