package com.starry.myne.api

import android.content.Context
import com.starry.myne.BuildConfig
import com.starry.myne.api.models.BookSet
import com.starry.myne.api.models.ExtraInfo
import com.starry.myne.helpers.PreferenceUtil
import com.starry.myne.helpers.book.BookLanguage
import com.starry.myne.helpers.summary.models.BookSummaryResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BookAPI(context: Context, private val preferenceUtil: PreferenceUtil) {

   private val baseApiUrl ="https://myne.abyx.in/books/"
    private val googleBooksUrl = "https://www.googleapis.com/books/v1/volumes"

    private val googleApiKey =
        BuildConfig.GOOGLE_API_KEY ?: "AIzaSyBCaXx-U0sbEpGVPWylSggC4RaR4gCGkVE" // Backup API key

    private val okHttpClient by lazy {
        // Create an OkHttpClient with a cache and a network interceptor.
        val okHttpBuilder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .cache(Cache(File(context.cacheDir, "http-cache"), CacheInterceptor.CACHE_SIZE))
            .addNetworkInterceptor(CacheInterceptor())

        // Add logging interceptor if in debug mode.
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpBuilder.addInterceptor(logging).build()
        }
        // Finally build the OkHttpClient.
        okHttpBuilder.build()
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAllBooks(
        page: Long,
        bookLanguage: BookLanguage = BookLanguage.AllBooks
    ): Result<BookSet> {
        var url = "${baseApiUrl}?page=$page"
        if (bookLanguage != BookLanguage.AllBooks) {
            url += "&languages=${bookLanguage.isoCode}"
        }
        val request = Request.Builder().get().url(url).build()
        return makeApiRequest(request)
    }

    suspend fun searchBooks(query: String): Result<BookSet> {
        val encodedString = withContext(Dispatchers.IO) {
            URLEncoder.encode(query, "UTF-8")
        }
        val request = Request.Builder().get().url("${baseApiUrl}?search=$encodedString").build()
        return makeApiRequest(request)
    }

    suspend fun getBookById(bookId: String): Result<BookSet> {
        val request = Request.Builder().get().url("${baseApiUrl}?ids=$bookId").build()
        return makeApiRequest(request)
    }

    suspend fun getBooksByCategory(
        category: String,
        page: Long,
        bookLanguage: BookLanguage = BookLanguage.AllBooks
    ): Result<BookSet> {
        var url = "${baseApiUrl}?page=$page&topic=$category"
        if (bookLanguage != BookLanguage.AllBooks) {
            url += "&languages=${bookLanguage.isoCode}"
        }
        val request = Request.Builder().get().url(url).build()
        return makeApiRequest(request)
    }

    // Helper function to make API requests.
    private suspend fun makeApiRequest(request: Request): Result<BookSet> =
        suspendCoroutine { continuation ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(Result.failure(exception = e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        continuation.resume(
                            Result.success(
                                json.decodeFromString(
                                    BookSet.serializer(),
                                    response.body!!.string()
                                ).copy(isCached = response.cacheResponse != null)
                            )
                        )
                    }
                }
            })
        }

    suspend fun getExtraInfo(bookName: String): ExtraInfo? = suspendCoroutine { continuation ->

        if (!preferenceUtil.getBoolean(PreferenceUtil.USE_GOOGLE_API_BOOL, true)) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        val encodedName = URLEncoder.encode(bookName, "UTF-8")
        val url = "${googleBooksUrl}?q=$encodedName&startIndex=0&maxResults=1&key=$googleApiKey"
        val request = Request.Builder().get().url(url).build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(null)
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    continuation.resume(
                        parseExtraInfoJson(
                            response.body!!.string(),
                            response.cacheResponse != null
                        )
                    )
                }
            }
        })
    }

    // Helper function to parse extra info JSON.
    private fun parseExtraInfoJson(jsonString: String, isCached: Boolean): ExtraInfo? {
        return runCatching {
            val jsonObj = JSONObject(jsonString)
            val totalItems = jsonObj.optInt("totalItems", 0)
            if (totalItems != 0) {
                jsonObj.optJSONArray("items")
                    ?.optJSONObject(0)
                    ?.optJSONObject("volumeInfo")
                    ?.let { volumeInfo ->
                        val coverImage = volumeInfo
                            .optJSONObject("imageLinks")
                            ?.optString("thumbnail", "")
                            ?.replace("http://", "https://") ?: ""
                        val pageCount = volumeInfo.optInt("pageCount", 0)
                        val description = volumeInfo.optString("description", "")

                        ExtraInfo(
                            coverImage = coverImage,
                            pageCount = pageCount,
                            description = description,
                            isCached = isCached
                        )
                    }
            } else {
                null
            }
        }.getOrNull()
    }

    suspend fun getBookSummaryFromGemini(bookTitle: String): Result<BookSummaryResponse> =
        suspendCancellableCoroutine { continuation ->

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=AIzaSyDL6cbz7XZ7dDwEmzLrQyznu_8C1XhNtZU"

            // Define schema for structured output
            val schema = """
            {
              "type": "OBJECT",
              "properties": {
                "title": { "type": "STRING" },
                "author": { "type": "STRING" },
                "summary_points": { "type": "ARRAY", "items": { "type": "STRING" } },
                "final_summary": { "type": "STRING" },
                "read_time_hours": { "type": "STRING" },
                "buy_links": {
                  "type": "OBJECT",
                  "properties": {
                    "amazon": { "type": "STRING" },
                    "goodreads": { "type": "STRING" }
                  }
                }
              }
            }
        """.trimIndent()

            val jsonBody = """
            {
              "contents": [{
                "parts": [{
                  "text": "Provide a structured book summary for '$bookTitle'."
                }]
              }],
              "generationConfig": {
                "responseMimeType": "application/json",
                "responseSchema": $schema
              }
            }
        """.trimIndent()

            val request = Request.Builder()
                .url(url)
                .post(
                    okhttp3.RequestBody.create(
                        "application/json".toMediaTypeOrNull(), jsonBody
                    )
                )
                .build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            val raw = response.body!!.string()
                            // Extract the text from Gemini's candidate response
                            val jsonObj = JSONObject(raw)
                            val text = jsonObj
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")

                            val bookSummary = json.decodeFromString(
                                BookSummaryResponse.serializer(), text
                            )

                            continuation.resume(Result.success(bookSummary))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    }
                }
            })
        }

}