package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speech_items")
data class SpeechItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val languageCode: String = "ar",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
