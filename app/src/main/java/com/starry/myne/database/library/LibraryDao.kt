package com.starry.myne.database.library

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LibraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(libraryItem: LibraryItem)

    @Delete
    fun delete(libraryItem: LibraryItem)

    @Query("SELECT * FROM book_library ORDER BY created_at DESC")
    fun getAllItems(): LiveData<List<LibraryItem>>

    @Query("SELECT * FROM book_library WHERE id = :id")
    fun getItemById(id: Int): LibraryItem?

    @Query("SELECT * FROM book_library WHERE book_id = :bookId")
    fun getItemByBookId(bookId: Int): LibraryItem?
}