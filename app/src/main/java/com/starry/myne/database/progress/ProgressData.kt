package com.starry.myne.database.progress

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * Data class for storing the reading progress of a library item (book).
 *
 * @param libraryItemId The ID of the library item.
 * @param lastChapterIndex The index of the last chapter read.
 * @param lastChapterOffset The offset of the last chapter read.
 * @param lastReadTime The time when the last chapter was read.
 */
@Entity(tableName = "reader_table")
data class ProgressData(
    @ColumnInfo(name = "library_item_id") val libraryItemId: Int,
    @ColumnInfo(name = "last_chapter_index") val lastChapterIndex: Int,
    @ColumnInfo(name = "last_chapter_offset") val lastChapterOffset: Int,
    // Added in database schema version 5
    @ColumnInfo(
        name = "last_read_time",
        defaultValue = "0"
    ) val lastReadTime: Long = System.currentTimeMillis()
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    fun getProgressPercent(totalChapters: Int) =
        String.format(
            locale = Locale.US,
            format = "%.2f",
            ((lastChapterIndex + 1).toFloat() / totalChapters.toFloat()) * 100f
        )
}