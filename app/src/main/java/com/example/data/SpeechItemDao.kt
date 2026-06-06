package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeechItemDao {
    @Query("SELECT * FROM speech_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<SpeechItem>>

    @Query("SELECT * FROM speech_items WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<SpeechItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeechItem(item: SpeechItem): Long

    @Query("DELETE FROM speech_items WHERE id = :id")
    suspend fun deleteSpeechItemById(id: Int)

    @Query("SELECT * FROM speech_items WHERE id = :id LIMIT 1")
    fun getItemById(id: Int): Flow<SpeechItem?>

    @Query("UPDATE speech_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("SELECT * FROM speech_items WHERE title LIKE :query OR content LIKE :query ORDER BY timestamp DESC")
    fun searchItems(query: String): Flow<List<SpeechItem>>
}
