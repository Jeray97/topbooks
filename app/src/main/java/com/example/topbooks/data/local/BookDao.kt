package com.example.topbooks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBookById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR authors LIKE '%' || :query || '%' ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun searchBooks(query: String, limit: Int = 20): List<BookEntity>

    @Query("SELECT * FROM books ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getRecentBooks(limit: Int = 50): List<BookEntity>

    @Query("SELECT * FROM books ORDER BY cachedAt DESC LIMIT :limit")
    fun observeRecentBooks(limit: Int = 50): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: String)

    @Query("DELETE FROM books WHERE cachedAt < :olderThan")
    suspend fun deleteOldBooks(olderThan: Long)

    @Query("DELETE FROM books")
    suspend fun clearAll()
}
