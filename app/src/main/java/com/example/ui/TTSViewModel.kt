package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SpeechItem
import com.example.data.SpeechItemRepository
import com.example.tts.TextToSpeechManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class TTSViewModel(
    application: Application,
    private val repository: SpeechItemRepository
) : AndroidViewModel(application) {

    private val ttsManager = TextToSpeechManager(application)

    // UI state for inputs
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _inputTitle = MutableStateFlow("")
    val inputTitle: StateFlow<String> = _inputTitle.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(Locale("ar"))
    val selectedLanguage: StateFlow<Locale> = _selectedLanguage.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    // Flag for filter tab (History vs Favorites)
    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites.asStateFlow()

    // Bind ttsManager states directly
    val isPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val currentSentenceIndex: StateFlow<Int> = ttsManager.currentSentenceIndex
    val sentences: StateFlow<List<String>> = ttsManager.sentences
    val isTtsInitialized: StateFlow<Boolean> = ttsManager.isInitialized

    // Dynamic list of languages from the engine
    private val _supportedLanguages = MutableStateFlow<List<Locale>>(emptyList())
    val supportedLanguages: StateFlow<List<Locale>> = _supportedLanguages.asStateFlow()

    // Saved speech items
    val savedItems: StateFlow<List<SpeechItem>> = combine(
        _searchQuery,
        _showOnlyFavorites,
        repository.allItems
    ) { query, onlyFavs, items ->
        var list = items
        if (onlyFavs) {
            list = list.filter { it.isFavorite }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Currently selected/active SpeechItem from history (if loaded)
    private val _activeItemId = MutableStateFlow<Int?>(null)
    val activeItemId: StateFlow<Int?> = _activeItemId.asStateFlow()

    init {
        // Fetch supported languages when TTS initializes
        viewModelScope.launch {
            ttsManager.isInitialized.collect { ready ->
                if (ready) {
                    _supportedLanguages.value = ttsManager.getAvailableLanguages()
                }
            }
        }
    }

    fun setInputText(text: String) {
        _inputText.value = text
        ttsManager.setText(text)
    }

    fun setInputTitle(title: String) {
        _inputTitle.value = title
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowOnlyFavorites(enabled: Boolean) {
        _showOnlyFavorites.value = enabled
    }

    fun setSpeed(value: Float) {
        _speed.value = value
        ttsManager.setSpeechRate(value)
    }

    fun setPitch(value: Float) {
        _pitch.value = value
        ttsManager.setPitch(value)
    }

    fun setLanguage(locale: Locale) {
        _selectedLanguage.value = locale
        ttsManager.setLanguage(locale)
    }

    fun selectSentence(index: Int) {
        ttsManager.setSentenceIndex(index)
    }

    // Controls
    fun playTTS() {
        if (_inputText.value.isBlank()) return
        // Initialize active reading text in compiler if it differs
        ttsManager.setText(_inputText.value)
        ttsManager.play()
    }

    fun pauseTTS() {
        ttsManager.pause()
    }

    fun stopTTS() {
        ttsManager.stop()
    }

    // Local DB Operations
    fun saveCurrentText(title: String = "") {
        val textToSave = _inputText.value
        if (textToSave.isBlank()) return

        val finalTitle = title.trim().ifEmpty {
            val length = minOf(textToSave.length, 25)
            textToSave.substring(0, length) + "..."
        }

        viewModelScope.launch {
            val currentId = _activeItemId.value
            val newItem = SpeechItem(
                id = currentId ?: 0,
                title = finalTitle,
                content = textToSave,
                pitch = _pitch.value,
                speed = _speed.value,
                languageCode = _selectedLanguage.value.language,
                timestamp = System.currentTimeMillis()
            )
            val newId = repository.insert(newItem)
            if (currentId == null) {
                _activeItemId.value = newId.toInt()
            }
        }
    }

    fun deleteItem(item: SpeechItem) {
        viewModelScope.launch {
            if (_activeItemId.value == item.id) {
                _activeItemId.value = null
                _inputTitle.value = ""
                _inputText.value = ""
                ttsManager.stop()
                ttsManager.setText("")
            }
            repository.deleteById(item.id)
        }
    }

    fun toggleFavorite(item: SpeechItem) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(item.id, !item.isFavorite)
        }
    }

    fun loadSpeechItem(item: SpeechItem) {
        _activeItemId.value = item.id
        _inputTitle.value = item.title
        _inputText.value = item.content
        _pitch.value = item.pitch
        _speed.value = item.speed
        
        val itemLocale = Locale(item.languageCode)
        _selectedLanguage.value = itemLocale

        // Apply parameters to speech engine
        ttsManager.stop()
        ttsManager.setPitch(item.pitch)
        ttsManager.setSpeechRate(item.speed)
        ttsManager.setLanguage(itemLocale)
        ttsManager.setText(item.content)
    }

    fun createNewDocument() {
        ttsManager.stop()
        _activeItemId.value = null
        _inputTitle.value = ""
        _inputText.value = ""
        ttsManager.setText("")
    }

    // Open/Import text file
    fun importTextFromUri(context: Context, uri: Uri, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val fileName = getFileNameFromUri(context, uri)
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line).append("\n")
                            line = reader.readLine()
                        }
                    }
                }
                val extractedText = stringBuilder.toString().trim()
                if (extractedText.isNotEmpty()) {
                    _inputText.value = extractedText
                    val titleWithoutExt = fileName.substringBeforeLast(".")
                    _inputTitle.value = titleWithoutExt
                    _activeItemId.value = null // treat as new unsaved item originally
                    ttsManager.setText(extractedText)
                    
                    // Auto save import to history
                    saveCurrentText(titleWithoutExt)
                    
                    onSuccess()
                } else {
                    onError()
                }
            } catch (e: Exception) {
                Log.e("TTSViewModel", "Error reading file URI $uri", e)
                onError()
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = "ملف_نصي.txt"
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } else if (uri.scheme == "file") {
            uri.path?.let { path ->
                val file = java.io.File(path)
                name = file.name
            }
        }
        return name
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

class TTSViewModelFactory(
    private val application: Application,
    private val repository: SpeechItemRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TTSViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TTSViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
