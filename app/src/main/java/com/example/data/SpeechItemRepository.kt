package com.example.data

import kotlinx.coroutines.flow.Flow

class SpeechItemRepository(private val dao: SpeechItemDao) {
    val allItems: Flow<List<SpeechItem>> = dao.getAllItems()
    val favorites: Flow<List<SpeechItem>> = dao.getFavorites()

    fun getItemById(id: Int): Flow<SpeechItem?> {
        return dao.getItemById(id)
    }

    suspend fun insert(item: SpeechItem): Long {
        return dao.insertSpeechItem(item)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteSpeechItemById(id)
    }

    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean) {
        dao.updateFavoriteStatus(id, isFavorite)
    }

    fun searchItems(query: String): Flow<List<SpeechItem>> {
        val formattedQuery = "%$query%"
        return dao.searchItems(formattedQuery)
    }
}
