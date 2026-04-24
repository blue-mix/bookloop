package com.starry.myne.helpers.dictionary

import DictionaryEntry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepository @Inject constructor(
    private val client: HttpClient
) {
    private val cache = mutableMapOf<String, DictionaryEntry>()

    suspend fun lookup(input: String): Result<DictionaryEntry> = runCatching {
        val word = sanitize(input)
        cache[word]?.let { return Result.success(it) }

        val list = client
            .get("https://api.dictionaryapi.dev/api/v2/entries/en/$word")
            .body<List<DictionaryEntry>>()

        val entry = list.firstOrNull() ?: error("No definition for “$word”.")
        cache[word] = entry
        entry
    }

    private fun sanitize(s: String) = s.lowercase().trim()
        .trim('\'', '"', '(', ')', '[', ']', '{', '}', '.', ',', ';', '!', '?', ':')
}
