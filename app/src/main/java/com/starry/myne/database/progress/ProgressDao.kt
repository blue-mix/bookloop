package com.starry.myne.database.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(progressData: ProgressData)

    @Query("DELETE FROM reader_table WHERE library_item_id = :libraryItemId")
    fun delete(libraryItemId: Int)

    @Update
    fun update(progressData: ProgressData)

    @Query("SELECT * FROM reader_table WHERE library_item_id = :libraryItemId")
    fun getReaderData(libraryItemId: Int): ProgressData?

    @Query("SELECT * FROM reader_table")
    fun getAllReaderItems(): List<ProgressData>

    @Query("SELECT * FROM reader_table WHERE library_item_id = :libraryItemId")
    fun getReaderDataAsFlow(libraryItemId: Int): Flow<ProgressData?>
}